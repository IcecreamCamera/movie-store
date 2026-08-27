package com.lecture.booking.dto;

import com.lecture.booking.entity.Booking;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingDto {

    /** 예매 요청 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookRequest {

        @NotNull(message = "영화 ID는 필수입니다")
        private Long movieId;

        @Min(value = 1, message = "예매 매수는 1 이상이어야 합니다")
        @Builder.Default
        private Integer quantity = 1;
    }

    /** 예매 목록 표시용 영화 요약 (movie-service에서 조회) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovieSummary {
        private Long id;
        private String title;
        private String genre;
        private String posterUrl;
        private BigDecimal price;
        private String openDt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingResponse {
        private Long id;
        private Long userId;
        private Long movieId;
        private Integer quantity;
        private BigDecimal amount;
        private Booking.Status status;
        private LocalDateTime createdAt;
        private MovieSummary movie;

        public static BookingResponse from(Booking booking) {
            return BookingResponse.builder()
                    .id(booking.getId())
                    .userId(booking.getUserId())
                    .movieId(booking.getMovieId())
                    .quantity(booking.getQuantity())
                    .amount(booking.getAmount())
                    .status(booking.getStatus())
                    .createdAt(booking.getCreatedAt())
                    .build();
        }

        public static BookingResponse from(Booking booking, MovieSummary movie) {
            return BookingResponse.builder()
                    .id(booking.getId())
                    .userId(booking.getUserId())
                    .movieId(booking.getMovieId())
                    .quantity(booking.getQuantity())
                    .amount(booking.getAmount())
                    .status(booking.getStatus())
                    .createdAt(booking.getCreatedAt())
                    .movie(movie)
                    .build();
        }
    }

    /** 추천 서비스용: 확정된 예매의 영화 ID 목록 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingHistoryResponse {
        private Long userId;
        private List<Long> movieIds;
    }

    /** 공통 API 응답 래퍼 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
