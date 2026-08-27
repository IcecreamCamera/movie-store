package com.lecture.movie.service;

import com.lecture.movie.dto.MovieDto;
import com.lecture.movie.entity.BoxofficeRanking;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.external.KobisClient;
import com.lecture.movie.external.dto.KobisMovieItem;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MovieServiceTest {

    private MovieRepository movieRepository;
    private BoxofficeRankingRepository rankingRepository;
    private BoxofficeSyncService syncService;
    private KobisClient kobisClient;
    private MovieService service;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        rankingRepository = mock(BoxofficeRankingRepository.class);
        syncService = mock(BoxofficeSyncService.class);
        kobisClient = mock(KobisClient.class);
        service = new MovieService(movieRepository, rankingRepository, syncService, kobisClient);
    }

    private Movie movie() {
        return Movie.builder()
                .id(1L)
                .movieCd("20250654")
                .title("파묘")
                .genre(Genre.MYSTERY)
                .build();
    }

    private BoxofficeRanking ranking() {
        return BoxofficeRanking.builder()
                .id(10L)
                .movieId(1L)
                .rankType(RankType.DAILY)
                .targetDate(LocalDate.of(2026, 8, 26))
                .rankNo(1)
                .audienceCnt(1000L)
                .rankInten(0)
                .build();
    }

    @Test
    void 스냅샷이_있으면_오픈API를_호출하지_않는다() {
        when(rankingRepository.findByRankTypeAndTargetDateOrderByRankNoAsc(eq(RankType.DAILY), any()))
                .thenReturn(List.of(ranking()));
        when(movieRepository.findAllById(any())).thenReturn(List.of(movie()));

        MovieDto.BoxofficeResponse response = service.getBoxoffice(RankType.DAILY);

        assertEquals(1, response.getItems().size());
        assertEquals("파묘", response.getItems().get(0).getMovie().getTitle());
        verify(syncService, never()).sync(any(), any());
    }

    @Test
    void 스냅샷이_없으면_수집한_뒤_반환한다() {
        when(rankingRepository.findByRankTypeAndTargetDateOrderByRankNoAsc(eq(RankType.DAILY), any()))
                .thenReturn(List.of());
        when(syncService.sync(eq(RankType.DAILY), any())).thenReturn(List.of(ranking()));
        when(movieRepository.findAllById(any())).thenReturn(List.of(movie()));

        MovieDto.BoxofficeResponse response = service.getBoxoffice(RankType.DAILY);

        assertEquals(1, response.getItems().size());
        verify(syncService, times(1)).sync(eq(RankType.DAILY), any());
    }

    @Test
    void 예매수를_증가시킨다() {
        when(movieRepository.incrementBookingCount(1L)).thenReturn(1);

        service.increaseBookingCount(1L);

        verify(movieRepository).incrementBookingCount(1L);
    }

    @Test
    void 존재하지_않는_영화의_예매수_증가는_예외를_던진다() {
        when(movieRepository.incrementBookingCount(999L)).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> service.increaseBookingCount(999L));
    }

    @Test
    void 검색어가_비면_KOBIS를_호출하지_않는다() {
        List<MovieDto.MovieResponse> emptyResult = service.searchMovies("");
        List<MovieDto.MovieResponse> nullResult = service.searchMovies(null);

        assertTrue(emptyResult.isEmpty());
        assertTrue(nullResult.isEmpty());
        verify(kobisClient, never()).searchMovies(any());
    }

    @Test
    void 검색_결과를_upsert하고_응답으로_변환한다() {
        KobisMovieItem item1 = KobisMovieItem.builder()
                .movieCd("20183782")
                .movieNm("기생충")
                .movieNmEn("PARASITE")
                .openDt(LocalDate.of(2019, 5, 30))
                .genreNm("드라마")
                .directorName("봉준호")
                .build();
        KobisMovieItem item2 = KobisMovieItem.builder()
                .movieCd("20200001")
                .movieNm("마약 기생충")
                .movieNmEn(null)
                .openDt(null)
                .genreNm("드라마")
                .directorName(null)
                .build();
        when(kobisClient.searchMovies("기생충")).thenReturn(List.of(item1, item2));

        Movie movie1 = Movie.builder()
                .id(1L)
                .movieCd("20183782")
                .title("기생충")
                .genre(Genre.DRAMA)
                .build();
        Movie movie2 = Movie.builder()
                .id(2L)
                .movieCd("20200001")
                .title("마약 기생충")
                .genre(Genre.DRAMA)
                .build();
        when(syncService.upsertSearchResults(List.of(item1, item2)))
                .thenReturn(List.of(movie1, movie2));

        List<MovieDto.MovieResponse> response = service.searchMovies("기생충");

        assertEquals(2, response.size());
        assertEquals("기생충", response.get(0).getTitle());
        assertEquals("마약 기생충", response.get(1).getTitle());
    }
}
