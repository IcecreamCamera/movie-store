package com.lecture.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieServiceClient {

    /** movie-service가 price를 주지 못할 때 쓰는 폴백 티켓 단가 */
    private static final BigDecimal FALLBACK_PRICE = new BigDecimal("14000.00");

    private final WebClient.Builder webClientBuilder;

    /** 영화 존재 여부 확인 (동기 REST) */
    public boolean existsMovie(Long movieId) {
        try {
            Boolean exists = webClientBuilder.build()
                    .get()
                    .uri("http://movie-service/api/movies/internal/exists/{id}", movieId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("[MovieServiceClient] 영화 존재 확인 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
            throw new RuntimeException("Movie Service 연결 실패");
        }
    }

    /**
     * 영화 상세 조회.
     * movie-service는 /internal/{id}를 래퍼 없이 반환하지만,
     * 게이트웨이 경유 등으로 래퍼가 씌워질 수 있어 둘 다 처리한다.
     */
    public Map<String, Object> getMovie(Long movieId) {
        try {
            Map<String, Object> body = webClientBuilder.build()
                    .get()
                    .uri("http://movie-service/api/movies/internal/{id}", movieId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (body == null) {
                throw new RuntimeException("Movie Service 응답 본문이 비어 있습니다.");
            }

            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> movieMap = (Map<String, Object>) dataMap;
                return movieMap;
            }
            return body;

        } catch (Exception e) {
            log.error("[MovieServiceClient] 영화 상세 조회 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
            throw new RuntimeException("Movie Service 영화 상세 조회 실패");
        }
    }

    /** 티켓 단가 조회 - 예매 금액 계산에 사용 */
    public BigDecimal getPrice(Long movieId) {
        Object price = getMovie(movieId).get("price");
        if (price instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (price != null) {
            try {
                return new BigDecimal(price.toString());
            } catch (NumberFormatException ignored) {
                // 아래 폴백으로 떨어진다
            }
        }
        log.warn("[MovieServiceClient] price 없음, 폴백 사용 - movieId: {}", movieId);
        return FALLBACK_PRICE;
    }

    /** 예매 확정 시 예매 수 증가 */
    public void increaseBookingCount(Long movieId) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri("http://movie-service/api/movies/internal/{id}/booking-count", movieId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("[MovieServiceClient] 예매 수 증가 완료 - movieId: {}", movieId);
        } catch (Exception e) {
            // 카운터 증가 실패가 예매 확정을 막아서는 안 된다
            log.error("[MovieServiceClient] 예매 수 증가 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
        }
    }
}
