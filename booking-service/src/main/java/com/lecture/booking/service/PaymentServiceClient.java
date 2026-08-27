package com.lecture.booking.service;

import com.fasterxml.jackson.annotation.JsonProperty;
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
     * Payment Service: 결제 요청 (동기 REST)
     */
    public PaymentResult requestPayment(Long userId, Long movieId, BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest(userId, movieId, amount);

            PaymentResult result = webClientBuilder.build()
                    .post()
                    .uri("http://payment-service:8084/api/payments/internal/request")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResult.class)
                    .block();

            log.info("[PaymentServiceClient] 결제 요청 완료 - userId: {}, movieId: {}, result: {}",
                    userId, movieId, result != null ? result.getStatus() : "null");

            return result;
        } catch (Exception e) {
            log.error("[PaymentServiceClient] 결제 요청 실패 - userId: {}, movieId: {}, error: {}",
                    userId, movieId, e.getMessage(), e);
            throw new RuntimeException("Payment Service 연결 실패");
        }
    }

    @Getter
    @NoArgsConstructor
    static class PaymentRequest {
        private Long userId;

        /*
         * API 명세서상 /api/payments/internal/request 의 body는 미변경 대상이라
         * 와이어 필드명은 courseId 를 유지한다. (payment-service의 DTO가 courseId로 바인딩)
         * TODO: payment-service 리네이밍 완료 후 @JsonProperty 제거
         */
        @JsonProperty("courseId")
        private Long movieId;

        private BigDecimal amount;

        PaymentRequest(Long userId, Long movieId, BigDecimal amount) {
            this.userId = userId;
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
