package com.lecture.booking.kafka;

import lombok.*;

public class KafkaEvent {

    /**
     * Payment Service → Booking Service
     * 결제 완료 이벤트. bookingId 로 예매를 특정한다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentCompletedEvent {
        private Long paymentId;
        private Long bookingId;
        private Long userId;
        private Long movieId;
        private String status; // COMPLETED
    }

    /**
     * Booking Service → Recommend Service
     * 예매 확정 이벤트. genre 를 실어 추천 서비스의 재조회를 줄인다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingCompletedEvent {
        private Long bookingId;
        private Long userId;
        private Long movieId;
        private String genre;
    }
}
