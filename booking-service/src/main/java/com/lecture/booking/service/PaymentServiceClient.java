package com.lecture.booking.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final WebClient.Builder webClientBuilder;

    /**
     * 결제 요청 (동기 REST).
     * bookingId 를 반드시 함께 보낸다 - payment.completed 이벤트가 이 값으로 예매를 특정한다.
     */
    public PaymentResult requestPayment(Long userId, Long bookingId, Long movieId, BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest(userId, bookingId, movieId, amount);

            PaymentResult result = webClientBuilder.build()
                    .post()
                    .uri("http://payment-service:8084/api/payments/internal/request")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResult.class)
                    .block();

            log.info("[PaymentServiceClient] 결제 요청 완료 - bookingId: {}, movieId: {}, result: {}",
                    bookingId, movieId, result != null ? result.getStatus() : "null");

            return result;
        } catch (Exception e) {
            log.error("[PaymentServiceClient] 결제 요청 실패 - bookingId: {}, error: {}",
                    bookingId, e.getMessage(), e);
            throw new RuntimeException("Payment Service 연결 실패");
        }
    }

    @Getter
    @NoArgsConstructor
    static class PaymentRequest {
        private Long userId;
        private Long bookingId;
        private Long movieId;
        private BigDecimal amount;

        PaymentRequest(Long userId, Long bookingId, Long movieId, BigDecimal amount) {
            this.userId = userId;
            this.bookingId = bookingId;
            this.movieId = movieId;
            this.amount = amount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PaymentResult {
        private Long paymentId;
        private String status; // COMPLETED / FAILED
    }
}
