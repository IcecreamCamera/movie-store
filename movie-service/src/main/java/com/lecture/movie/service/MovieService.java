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
import org.springframework.transaction.annotation.Propagation;
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
     *
     * 클래스 레벨 @Transactional(readOnly = true) 를 이 메서드에서만 NOT_SUPPORTED 로 무효화한다.
     * MariaDB 기본 격리수준(REPEATABLE READ)에서는 트랜잭션 시작 시점에 스냅샷이 고정되는데,
     * syncService.sync() 는 REQUIRES_NEW 로 별도 트랜잭션에 커밋한다. 이 메서드가 주변 읽기
     * 트랜잭션을 유지한 채 그 스냅샷을 계속 쓰면, sync() 커밋 이후 실행되는
     * movieRepository.findAllById() 가 방금 삽입된 Movie 행을 보지 못해 items 가 비거나
     * 일부만 채워진다. 트랜잭션을 두지 않으면 각 리포지토리 호출이 각자의 짧은 트랜잭션에서
     * 커밋 직후의 최신 스냅샷을 읽으므로 이 문제가 사라진다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
