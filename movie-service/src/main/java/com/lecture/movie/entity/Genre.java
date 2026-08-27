package com.lecture.movie.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TMDB 공식 영화 장르.
 * tmdbId 는 TMDB /genre/movie/list 가 반환하는 장르 ID다.
 * OTHER 는 TMDB 매칭 실패 또는 미지의 장르용 폴백이며 tmdbId 0 은 실제로 존재하지 않는다.
 */
public enum Genre {
    ACTION(28),
    ADVENTURE(12),
    ANIMATION(16),
    COMEDY(35),
    CRIME(80),
    DOCUMENTARY(99),
    DRAMA(18),
    FAMILY(10751),
    FANTASY(14),
    HISTORY(36),
    HORROR(27),
    MUSIC(10402),
    MYSTERY(9648),
    ROMANCE(10749),
    SCIENCE_FICTION(878),
    TV_MOVIE(10770),
    THRILLER(53),
    WAR(10752),
    WESTERN(37),
    OTHER(0);

    private static final Map<Integer, Genre> BY_TMDB_ID = Arrays.stream(values())
            .filter(g -> g != OTHER)
            .collect(Collectors.toMap(Genre::getTmdbId, Function.identity()));

    private final int tmdbId;

    Genre(int tmdbId) {
        this.tmdbId = tmdbId;
    }

    public int getTmdbId() {
        return tmdbId;
    }

    public static Genre fromTmdbId(Integer tmdbId) {
        if (tmdbId == null) {
            return OTHER;
        }
        return BY_TMDB_ID.getOrDefault(tmdbId, OTHER);
    }

    /**
     * TMDB genre_ids 배열에서 대표 장르 하나를 고른다.
     * 배열 순서가 TMDB 기준 관련도 순이므로 첫 번째 유효 장르를 채택한다.
     */
    public static Genre fromTmdbIds(List<Integer> tmdbIds) {
        if (tmdbIds == null || tmdbIds.isEmpty()) {
            return OTHER;
        }
        return tmdbIds.stream()
                .map(Genre::fromTmdbId)
                .filter(g -> g != OTHER)
                .findFirst()
                .orElse(OTHER);
    }
}
