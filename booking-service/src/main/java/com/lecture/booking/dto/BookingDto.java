package com.lecture.booking.dto;

import com.lecture.booking.entity.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class BookingDto {

    // 예매 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookRequest {
        @NotNull(message = "영화 ID는 필수입니다")
        private Long movieId;
    }

    // 영화 요약 정보 (내 예매 목록 표시용)
    // TODO: movie-service 응답 스키마 확정 시 필드/키 재확인 (API 명세서 §2 "스키마 정의 시 별도 작성 예정")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovieSummary {
        private Long id;
        private String title;
        private String description;
        private String genre;
        private Integer price;
        private String poster;
        private String director;
        private Integer bookingCount;
    }

    // 예매 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingResponse {
        private Long id;
        private Long userId;
        private Long movieId;
        private Booking.Status status;
        private LocalDateTime createdAt;

        // 추가
        private MovieSummary movie;

        public static BookingResponse from(Booking booking) {
            return BookingResponse.builder()
                    .id(booking.getId())
                    .userId(booking.getUserId())
                    .movieId(booking.getMovieId())
                    .status(booking.getStatus())
                    .createdAt(booking.getCreatedAt())
                    .build();
        }

        public static BookingResponse from(Booking booking, MovieSummary movie) {
            return BookingResponse.builder()
                    .id(booking.getId())
                    .userId(booking.getUserId())
                    .movieId(booking.getMovieId())
                    .status(booking.getStatus())
                    .createdAt(booking.getCreatedAt())
                    .movie(movie)
                    .build();
        }
    }

    // 추천 서비스용: 예매 이력 조회 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingHistoryResponse {
        private Long userId;
        private List<Long> activeMovieIds;
    }

    // 공통 API 응답 래퍼
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
