# -*- coding: utf-8 -*-
import json
import logging
import threading
from collections import defaultdict, deque

from kafka import KafkaConsumer

from app.config.settings import settings

logger = logging.getLogger(__name__)

# user_id -> 최근 이용한 movieId 목록 (in-memory, maxlen 20)
# Kafka 이벤트로 채워지며, LLM 프롬프트의 "사용자 맥락"으로 주입된다.
user_recent_context = defaultdict(lambda: deque(maxlen=20))


class EnrollmentCompletedConsumer:
    """
    Kafka Consumer: enrollment.completed 수신 — 영화 도메인으로 매핑해 활용.

    제약: Kafka 서버/프로듀서 소스는 수정 불가 → 현재 recommend-service 내에서
    이벤트의 의미만 매핑한다.
      - enrollmentId -> usageId (영화 이용 id)
      - courseId      -> movieId  (이용한 영화 id)
    유저의 최근 이용 영화 ID 목록을 in-memory로 유지해 추천 프롬프트에 주입
    (doc1의 prior + user feedback → posterior 확장의 단초 / 실습 수준: 로그 처리).
    """

    def __init__(self):
        self.topic = settings.kafka_topic_enrollment_completed
        self.consumer = None
        self._running = False
        self._thread = None

    def start(self):
        """별도 스레드로 Kafka Consumer 시작"""
        self._running = True
        self._thread = threading.Thread(target=self._consume, daemon=True)
        self._thread.start()
        logger.info(f"[KafkaConsumer] 시작 - topic: {self.topic}")

    def stop(self):
        self._running = False
        if self.consumer:
            self.consumer.close()

    def _consume(self):
        try:
            self.consumer = KafkaConsumer(
                self.topic,
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.kafka_consumer_group_id,
                auto_offset_reset="earliest",
                enable_auto_commit=True,
                value_deserializer=lambda m: json.loads(m.decode("utf-8")),
                consumer_timeout_ms=1000,
            )

            while self._running:
                for message in self.consumer:
                    if not self._running:
                        break
                    self._handle_message(message.value)

        except Exception as e:
            logger.error(f"[KafkaConsumer] 오류 발생: {e}")
        finally:
            if self.consumer:
                self.consumer.close()

    def _handle_message(self, event: dict):
        """
        enrollment.completed 이벤트 → 영화 도메인 의미로 매핑.
        - 유저 최근 이용 영화 ID 컨텍스트 갱신
        - 실습 포인트: 여기서 추천 캐시 갱신/재계산 로직 추가 가능
        """
        try:
            usage_id = event.get("enrollmentId")
            user_id = event.get("userId")
            movie_id = event.get("courseId")  # 영화 도메인: courseId == movieId

            logger.info(
                f"[KafkaConsumer] movie.usage.completed 수신 - "
                f"usageId: {usage_id}, userId: {user_id}, movieId: {movie_id}"
            )

            if user_id is not None and movie_id is not None:
                user_recent_context[user_id].append(movie_id)

        except Exception as e:
            logger.error(f"[KafkaConsumer] 메시지 처리 실패: {e}, event: {event}")


enrollment_consumer = EnrollmentCompletedConsumer()
