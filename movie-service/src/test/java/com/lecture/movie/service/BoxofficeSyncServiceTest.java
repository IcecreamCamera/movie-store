package com.lecture.movie.service;

import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.external.KobisClient;
import com.lecture.movie.external.TmdbClient;
import com.lecture.movie.external.dto.KobisBoxofficeItem;
import com.lecture.movie.external.dto.TmdbMovie;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BoxofficeSyncServiceTest {

    private MovieRepository movieRepository;
    private BoxofficeRankingRepository rankingRepository;
    private KobisClient kobisClient;
    private TmdbClient tmdbClient;
    private BoxofficeSyncService service;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        rankingRepository = mock(BoxofficeRankingRepository.class);
        kobisClient = mock(KobisClient.class);
        tmdbClient = mock(TmdbClient.class);
        service = new BoxofficeSyncService(
                movieRepository, rankingRepository, kobisClient, tmdbClient);
    }

    private KobisBoxofficeItem item() {
        return KobisBoxofficeItem.builder()
                .rankNo(1)
                .rankInten(0)
                .movieCd("20250654")
                .movieNm("파묘")
                .openDt(LocalDate.of(2024, 2, 22))
                .audiCnt(2566914L)
                .audiAcc(7109333L)
                .build();
    }

    @Test
    void 신규_영화는_TMDB를_호출해_보강한다() {
        when(movieRepository.findByMovieCd("20250654")).thenReturn(Optional.empty());
        when(tmdbClient.search("파묘", 2024)).thenReturn(Optional.of(
                TmdbMovie.builder()
                        .tmdbId(838209L)
                        .originalTitle("파묘")
                        .overview("줄거리")
                        .genre(Genre.MYSTERY)
                        .genreIds("9648,27,53")
                        .posterUrl("https://image.tmdb.org/t/p/w500/x.jpg")
                        .voteAverage(new BigDecimal("7.6"))
                        .build()));
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.upsertMovie(item());

        assertEquals("20250654", movie.getMovieCd());
        assertEquals(838209L, movie.getTmdbId());
        assertEquals(Genre.MYSTERY, movie.getGenre());
        assertEquals(7109333L, movie.getAudienceAcc());
        verify(tmdbClient, times(1)).search("파묘", 2024);
    }

    @Test
    void 이미_tmdbId가_있으면_TMDB를_다시_호출하지_않는다() {
        Movie existing = Movie.builder()
                .id(1L)
                .movieCd("20250654")
                .tmdbId(838209L)
                .title("파묘")
                .genre(Genre.MYSTERY)
                .audienceAcc(1000L)
                .build();
        when(movieRepository.findByMovieCd("20250654")).thenReturn(Optional.of(existing));
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.upsertMovie(item());

        assertEquals(7109333L, movie.getAudienceAcc()); // 관객수는 갱신된다
        verify(tmdbClient, never()).search(anyString(), anyInt());
    }

    @Test
    void TMDB_매칭에_실패하면_OTHER_장르로_남는다() {
        when(movieRepository.findByMovieCd("20250654")).thenReturn(Optional.empty());
        when(tmdbClient.search("파묘", 2024)).thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.upsertMovie(item());

        assertNull(movie.getTmdbId());
        assertEquals(Genre.OTHER, movie.getGenre());
        assertEquals("파묘", movie.getTitle());
    }
}
