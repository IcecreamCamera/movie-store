package com.lecture.booking.kafka;

import lombok.*;

/**
 * Kafka 이벤트 메시지 DTO
 */
public class KafkaEvent {

    /**
     * Payment Service → Booking Service
     * 결제 완료 이벤트 수신
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentCompletedEvent {
        private Long paymentId;
        private Long userId;
        private Long movieId;
        private String status; // COMPLETED
    }

    /**
     * Booking Service → Recommend Service
     * 예매 확정 완료 이벤트 발행
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingCompletedEvent {
        private Long bookingId;
        private Long userId;
        private Long movieId;
    }
}
