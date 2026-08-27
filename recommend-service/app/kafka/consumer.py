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


class BookingCompletedConsumer:
    """
    Kafka Consumer: booking.completed 수신 — 예매 확정 이벤트를 영화 이용 맥락으로 사용.

    제약: Kafka 브로커/프로듀서(booking-service)는 이미지이거나 기존 로직이라 수정 불가
    → recommend-service 내에서 이벤트 계약(BookingCompletedEvent)에 맞춰 매핑만 한다.
      - bookingId -> usageId           (예매/이용 id)
      - movieId   -> 이용한 영화 id    (장르는 genre 필드로 함께 수신)
    유저의 최근 이용 영화 ID 목록을 in-memory로 유지해 추천 프롬프트에 주입한다.
    """

    def __init__(self):
        self.topic = settings.kafka_topic_booking_completed
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
        booking.completed 이벤트(BookingCompletedEvent) → 영화 이용 맥락으로 매핑.
        - bookingId / userId / movieId / genre 수신
        - 유저 최근 이용 영화 ID 컨텍스트 갱신
        - 실습 포인트: 여기서 추천 캐시 갱신/재계산 로직 추가 가능
        """
        try:
            booking_id = event.get("bookingId")
            user_id = event.get("userId")
            movie_id = event.get("movieId")  # 예매 확정된 영화 id
            genre = event.get("genre")       # 예매 시 해석된 장르 (재조회 감소)

            logger.info(
                f"[KafkaConsumer] booking.completed 수신 - "
                f"bookingId: {booking_id}, userId: {user_id}, movieId: {movie_id}, genre: {genre}"
            )

            if user_id is not None and movie_id is not None:
                user_recent_context[user_id].append(movie_id)

        except Exception as e:
            logger.error(f"[KafkaConsumer] 메시지 처리 실패: {e}, event: {event}")


booking_consumer = BookingCompletedConsumer()
