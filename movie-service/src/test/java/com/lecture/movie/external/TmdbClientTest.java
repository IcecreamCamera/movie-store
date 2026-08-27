package com.lecture.movie.external;

import com.lecture.movie.config.OpenApiProperties;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.external.dto.TmdbMovie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TmdbClientTest {

    private TmdbClient client;

    @BeforeEach
    void setUp() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.getTmdb().setBaseUrl("https://api.themoviedb.org/3");
        properties.getTmdb().setImageBaseUrl("https://image.tmdb.org/t/p/w500");
        properties.getTmdb().setKey("dummy");
        client = new TmdbClient(WebClient.builder(), properties);
    }

    @Test
    void 검색결과를_TmdbMovie로_변환한다() {
        Map<String, Object> result = Map.of(
                "id", 838209,
                "original_title", "파묘",
                "overview", "거액의 의뢰를 받은 무당",
                "genre_ids", List.of(9648, 27, 53),
                "poster_path", "/tw0i3kkmOTjDjGFZTLHKhoeXVvA.jpg",
                "vote_average", 7.648
        );

        TmdbMovie movie = client.toTmdbMovie(result);

        assertEquals(838209L, movie.getTmdbId());
        assertEquals("파묘", movie.getOriginalTitle());
        assertEquals("거액의 의뢰를 받은 무당", movie.getOverview());
        assertEquals(Genre.MYSTERY, movie.getGenre());
        assertEquals("9648,27,53", movie.getGenreIds());
        assertEquals("https://image.tmdb.org/t/p/w500/tw0i3kkmOTjDjGFZTLHKhoeXVvA.jpg",
                movie.getPosterUrl());
        assertEquals(new BigDecimal("7.6"), movie.getVoteAverage());
    }

    @Test
    void 포스터가_없으면_posterUrl은_null이다() {
        Map<String, Object> result = Map.of(
                "id", 1,
                "genre_ids", List.of(28)
        );

        TmdbMovie movie = client.toTmdbMovie(result);

        assertNull(movie.getPosterUrl());
        assertEquals(Genre.ACTION, movie.getGenre());
    }

    @Test
    void 장르가_없으면_OTHER다() {
        Map<String, Object> result = Map.of("id", 2);

        TmdbMovie movie = client.toTmdbMovie(result);

        assertEquals(Genre.OTHER, movie.getGenre());
        assertNull(movie.getGenreIds());
    }
}
