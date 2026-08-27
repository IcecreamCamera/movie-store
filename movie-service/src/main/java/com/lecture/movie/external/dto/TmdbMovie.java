package com.lecture.movie.external.dto;

import com.lecture.movie.entity.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** TMDB /search/movie 결과 1건을 도메인이 쓰기 좋게 변환한 값 */
@Getter
@Builder
@AllArgsConstructor
public class TmdbMovie {
    private final Long tmdbId;
    private final String originalTitle;
    private final String overview;
    private final Genre genre;
    private final String genreIds;
    private final String posterUrl;
    private final BigDecimal voteAverage;
}
