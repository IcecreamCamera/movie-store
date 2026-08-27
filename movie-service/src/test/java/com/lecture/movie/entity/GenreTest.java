package com.lecture.movie.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenreTest {

    @Test
    void tmdbId로_장르를_찾는다() {
        assertEquals(Genre.ACTION, Genre.fromTmdbId(28));
        assertEquals(Genre.SCIENCE_FICTION, Genre.fromTmdbId(878));
        assertEquals(Genre.MYSTERY, Genre.fromTmdbId(9648));
    }

    @Test
    void 알_수_없는_tmdbId는_OTHER다() {
        assertEquals(Genre.OTHER, Genre.fromTmdbId(99999));
        assertEquals(Genre.OTHER, Genre.fromTmdbId(null));
    }

    @Test
    void 장르_배열에서_첫_번째_유효_장르를_고른다() {
        assertEquals(Genre.MYSTERY, Genre.fromTmdbIds(List.of(9648, 27, 53)));
    }

    @Test
    void 장르_배열이_비었거나_전부_미지면_OTHER다() {
        assertEquals(Genre.OTHER, Genre.fromTmdbIds(List.of()));
        assertEquals(Genre.OTHER, Genre.fromTmdbIds(null));
        assertEquals(Genre.OTHER, Genre.fromTmdbIds(List.of(99999)));
    }
}
