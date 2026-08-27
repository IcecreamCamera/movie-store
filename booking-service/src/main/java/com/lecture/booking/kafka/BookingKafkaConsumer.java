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
     * payment.completed 수신 → 예매 PENDING → CONFIRMED
     *
     * payment-service는 JsonSerializer에 type header를 싣지 않으므로
     * 특정 DTO가 아니라 Map으로 받아 직접 파싱한다.
     */
    @KafkaListener(
            topics = "${kafka.topic.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info("[Kafka Consumer] payment.completed 수신: {}", event);

        try {
            Object bookingIdValue = event.get("bookingId");

            if (bookingIdValue == null) {
                throw new IllegalArgumentException(
                        "payment.completed 이벤트에 bookingId가 없습니다. payment-service 계약을 확인하세요.");
            }

            Long bookingId = ((Number) bookingIdValue).longValue();
            bookingService.confirmBooking(bookingId);

            log.info("[Kafka Consumer] 예매 확정 완료 - bookingId: {}", bookingId);

        } catch (Exception e) {
            // 예외를 삼켜 리스너 컨테이너가 계속 살아 있게 한다
            log.error("[Kafka Consumer] 예매 확정 실패 - event: {}, error: {}",
                    event, e.getMessage(), e);
        }
    }
}
