package com.lecture.booking.kafka;

import com.lecture.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingKafkaConsumer {

    private final BookingService bookingService;

    /**
     * payment.completed 이벤트 수신
     * → 예매 상태 PENDING → ACTIVE 로 변경
     * → booking.completed 이벤트 발행 (→ Recommend Service)
     *
     * payment-service 쪽은 JsonSerializer + type header 미포함으로 이벤트를 발행하므로,
     * 여기서는 특정 DTO 타입으로 바로 받지 않고 Map<String, Object> 로 받아 처리한다.
     *
     * 영화 ID 키는 movieId / courseId 둘 다 허용한다.
     * payment-service가 아직 movie 용어로 리네이밍되기 전이라 courseId로 발행되고 있어,
     * 마이그레이션 기간 동안 양쪽을 모두 받아야 하기 때문이다.
     * TODO: payment-service 리네이밍 완료 후 courseId 폴백 제거
     */
    @KafkaListener(
            topics = "${kafka.topic.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info("[Kafka Consumer] payment.completed raw event 수신: {}", event);

        try {
            Object userIdValue = event.get("userId");
            Object movieIdValue = event.get("movieId");
            if (movieIdValue == null) {
                movieIdValue = event.get("courseId");
            }

            if (userIdValue == null || movieIdValue == null) {
                throw new IllegalArgumentException("Kafka 이벤트에 userId 또는 movieId가 없습니다.");
            }

            Long userId = ((Number) userIdValue).longValue();
            Long movieId = ((Number) movieIdValue).longValue();

            log.info("[Kafka Consumer] payment.completed 파싱 완료 - userId: {}, movieId: {}",
                    userId, movieId);

            bookingService.activateBooking(userId, movieId);

            log.info("[Kafka Consumer] 예매 확정 완료 - userId: {}, movieId: {}",
                    userId, movieId);

        } catch (Exception e) {
            log.error("[Kafka Consumer] 예매 확정 실패 - event: {}, error: {}",
                    event, e.getMessage(), e);
        }
    }
}
