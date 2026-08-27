package com.lecture.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieServiceClient {

    private final WebClient.Builder webClientBuilder;

    /**
     * Movie Service: 영화 존재 여부 확인 (동기 REST)
     */
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
     * Movie Service: 영화 상세 조회
     * - 내 예매 목록 응답에 movie 정보를 붙일 때 사용
     * - movie-service 쪽에 GET /api/movies/internal/{id} 엔드포인트가 있어야 함
     */
    public Map<String, Object> getMovie(Long movieId) {
        try {
            Map<String, Object> responseBody = webClientBuilder.build()
                    .get()
                    .uri("http://movie-service/api/movies/internal/{id}", movieId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (responseBody == null) {
                throw new RuntimeException("Movie Service 응답 본문이 비어 있습니다.");
            }

            log.info("[MovieServiceClient] 영화 상세 조회 성공 - movieId: {}", movieId);
            log.debug("[MovieServiceClient] 영화 상세 응답 - movieId: {}, body: {}", movieId, responseBody);

            /*
             * 응답 형태가 다음 둘 중 하나일 수 있으므로 둘 다 처리
             *
             * 1) 래퍼 응답
             * {
             *   "success": true,
             *   "message": "성공",
             *   "data": { ...movie fields... }
             * }
             *
             * 2) 바로 영화 객체 반환
             * {
             *   "id": 1,
             *   "title": "...",
             *   ...
             * }
             */
            Object data = responseBody.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> movieMap = (Map<String, Object>) dataMap;
                return movieMap;
            }

            return responseBody;
        } catch (Exception e) {
            log.error("[MovieServiceClient] 영화 상세 조회 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
            throw new RuntimeException("Movie Service 영화 상세 조회 실패");
        }
    }

    /**
     * Movie Service: 예매 인원 수 증가 (예매 확정 시 호출)
     */
    public void increaseBookingCount(Long movieId) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri("http://movie-service/api/movies/internal/{id}/booking-count", movieId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("[MovieServiceClient] 예매 인원 수 증가 완료 - movieId: {}", movieId);
        } catch (Exception e) {
            log.error("[MovieServiceClient] 예매 인원 수 증가 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
        }
    }
}
