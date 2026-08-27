package com.lecture.movie.service;

import com.lecture.movie.dto.MovieDto;
import com.lecture.movie.entity.BoxofficeRanking;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MovieServiceTest {

    private MovieRepository movieRepository;
    private BoxofficeRankingRepository rankingRepository;
    private BoxofficeSyncService syncService;
    private MovieService service;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        rankingRepository = mock(BoxofficeRankingRepository.class);
        syncService = mock(BoxofficeSyncService.class);
        service = new MovieService(movieRepository, rankingRepository, syncService);
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
        Movie m = movie();
        when(movieRepository.findById(1L)).thenReturn(Optional.of(m));

        service.increaseBookingCount(1L);

        assertEquals(1, m.getBookingCount());
    }
}
