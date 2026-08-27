package com.lecture.movie.service;

import com.lecture.movie.dto.MovieDto;
import com.lecture.movie.entity.BoxofficeRanking;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.external.KobisClient;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final BoxofficeRankingRepository rankingRepository;
    private final BoxofficeSyncService syncService;

    /**
     * 박스오피스 조회 (홈화면).
     * 스냅샷이 있으면 그대로 쓰고, 없을 때만 오픈API를 호출한다.
     */
    public MovieDto.BoxofficeResponse getBoxoffice(RankType rankType) {
        LocalDate targetDate = KobisClient.resolveTargetDate(rankType, LocalDate.now());

        List<BoxofficeRanking> rankings =
                rankingRepository.findByRankTypeAndTargetDateOrderByRankNoAsc(rankType, targetDate);

        if (rankings.isEmpty()) {
            log.info("[MovieService] 스냅샷 없음, 오픈API 수집 - {} {}", rankType, targetDate);
            rankings = syncService.sync(rankType, targetDate);
        }

        List<Long> movieIds = rankings.stream()
                .map(BoxofficeRanking::getMovieId)
                .collect(Collectors.toList());

        Map<Long, Movie> movieMap = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        List<MovieDto.BoxofficeItem> items = rankings.stream()
                .filter(r -> movieMap.containsKey(r.getMovieId()))
                .map(r -> MovieDto.BoxofficeItem.builder()
                        .rankNo(r.getRankNo())
                        .rankInten(r.getRankInten())
                        .audienceCnt(r.getAudienceCnt())
                        .movie(MovieDto.MovieResponse.from(movieMap.get(r.getMovieId())))
                        .build())
                .collect(Collectors.toList());

        return MovieDto.BoxofficeResponse.builder()
                .rankType(rankType.name())
                .targetDate(targetDate)
                .items(items)
                .build();
    }

    public MovieDto.MovieResponse getMovie(Long id) {
        return MovieDto.MovieResponse.from(findMovieById(id));
    }

    public List<MovieDto.MovieResponse> getAllMovies() {
        return movieRepository.findByStatus(Movie.Status.ACTIVE).stream()
                .map(MovieDto.MovieResponse::from)
                .collect(Collectors.toList());
    }

    public List<MovieDto.MovieResponse> getMoviesByGenre(Genre genre) {
        return movieRepository.findByGenreAndStatus(genre, Movie.Status.ACTIVE).stream()
                .map(MovieDto.MovieResponse::from)
                .collect(Collectors.toList());
    }

    /** booking-service 동기 호출용 */
    public boolean existsMovie(Long id) {
        return movieRepository.existsById(id);
    }

    /** 예매 확정 시 booking-service가 호출 */
    @Transactional
    public void increaseBookingCount(Long movieId) {
        findMovieById(movieId).increaseBookingCount();
    }

    private Movie findMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다: " + id));
    }
}
