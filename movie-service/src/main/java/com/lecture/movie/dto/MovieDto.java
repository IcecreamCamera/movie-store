package com.lecture.movie.dto;

import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MovieDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovieResponse {
        private Long id;
        private String movieCd;
        private Long tmdbId;
        private String title;
        private String originalTitle;
        private String description;
        private Genre genre;
        private String genreIds;
        private LocalDate openDt;
        private String posterUrl;
        private BigDecimal voteAverage;
        private Long audienceAcc;
        private BigDecimal price;
        private Integer bookingCount;
        private Movie.Status status;
        private LocalDateTime createdAt;

        public static MovieResponse from(Movie movie) {
            return MovieResponse.builder()
                    .id(movie.getId())
                    .movieCd(movie.getMovieCd())
                    .tmdbId(movie.getTmdbId())
                    .title(movie.getTitle())
                    .originalTitle(movie.getOriginalTitle())
                    .description(movie.getDescription())
                    .genre(movie.getGenre())
                    .genreIds(movie.getGenreIds())
                    .openDt(movie.getOpenDt())
                    .posterUrl(movie.getPosterUrl())
                    .voteAverage(movie.getVoteAverage())
                    .audienceAcc(movie.getAudienceAcc())
                    .price(movie.getPrice())
                    .bookingCount(movie.getBookingCount())
                    .status(movie.getStatus())
                    .createdAt(movie.getCreatedAt())
                    .build();
        }
    }

    /** 박스오피스 1행: 순위 정보 + 영화 상세 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoxofficeItem {
        private Integer rankNo;
        private Integer rankInten;
        private Long audienceCnt;
        private MovieResponse movie;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoxofficeResponse {
        private String rankType;
        private LocalDate targetDate;
        private List<BoxofficeItem> items;
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
