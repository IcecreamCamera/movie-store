package com.lecture.movie.service;

import com.lecture.movie.entity.BoxofficeRanking;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.external.KobisClient;
import com.lecture.movie.external.TmdbClient;
import com.lecture.movie.external.dto.KobisBoxofficeItem;
import com.lecture.movie.external.dto.KobisMovieItem;
import com.lecture.movie.external.dto.TmdbMovie;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 박스오피스 lazy 수집.
 * 요청 시점에 해당 (rankType, targetDate) 스냅샷이 없으면 오픈API를 호출해 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxofficeSyncService {

    private final MovieRepository movieRepository;
    private final BoxofficeRankingRepository rankingRepository;
    private final KobisClient kobisClient;
    private final TmdbClient tmdbClient;

    /**
     * KOBIS 조회 → movies upsert → 랭킹 스냅샷 재작성.
     * 같은 기준일을 다시 수집하면 기존 스냅샷을 지우고 새로 넣는다 (UNIQUE 충돌 방지).
     *
     * 호출부인 MovieService.getBoxoffice() 는 @Transactional(readOnly = true) 조회 트랜잭션
     * 안에서 이 메서드를 호출한다. 전파가 REQUIRED면 이 동기화가 그 readOnly 트랜잭션에 합류해
     * flush 가 되지 않거나(FlushMode.MANUAL) JDBC 커넥션이 read-only로 잠겨 INSERT/UPDATE가
     * 조용히 무효화될 수 있다. REQUIRES_NEW 로 별도 쓰기 트랜잭션을 열어 그 위험을 분리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<BoxofficeRanking> sync(RankType rankType, LocalDate targetDate) {
        List<KobisBoxofficeItem> items = kobisClient.fetch(rankType, targetDate);
        if (items.isEmpty()) {
            log.warn("[BoxofficeSync] KOBIS 결과 없음 - {} {}", rankType, targetDate);
            return List.of();
        }

        rankingRepository.deleteByRankTypeAndTargetDate(rankType, targetDate);

        List<BoxofficeRanking> rankings = new ArrayList<>();
        for (KobisBoxofficeItem item : items) {
            Movie movie = upsertMovie(item);
            rankings.add(rankingRepository.save(
                    BoxofficeRanking.builder()
                            .movieId(movie.getId())
                            .rankType(rankType)
                            .targetDate(targetDate)
                            .rankNo(item.getRankNo())
                            .audienceCnt(item.getAudiCnt())
                            .rankInten(item.getRankInten())
                            .fetchedAt(LocalDateTime.now())
                            .build()));
        }

        log.info("[BoxofficeSync] 수집 완료 - {} {} ({}건)", rankType, targetDate, rankings.size());
        return rankings;
    }

    /**
     * movieCd 기준 upsert.
     * TMDB 보강은 tmdbId 가 아직 없는 영화에 대해서만 1회 수행한다.
     *
     * sync() 내부에서 같은 빈의 self-invocation으로 호출되므로 프록시를 타지 않는다.
     * 별도 @Transactional을 붙여도 무효하므로 붙이지 않는다 — 항상 sync()의
     * REQUIRES_NEW 트랜잭션 안에서 실행된다.
     */
    public Movie upsertMovie(KobisBoxofficeItem item) {
        Movie movie = movieRepository.findByMovieCd(item.getMovieCd())
                .orElseGet(() -> Movie.builder()
                        .movieCd(item.getMovieCd())
                        .title(item.getMovieNm())
                        .openDt(item.getOpenDt())
                        .build());

        movie.updateBoxofficeStats(item.getAudiAcc());

        if (movie.getTmdbId() == null) {
            enrichWithTmdb(movie, item);
        }

        return movieRepository.save(movie);
    }

    private void enrichWithTmdb(Movie movie, KobisBoxofficeItem item) {
        Integer releaseYear = item.getOpenDt() != null ? item.getOpenDt().getYear() : null;
        enrichWithTmdb(movie, item.getMovieNm(), releaseYear, item.getMovieCd());
    }

    /**
     * 검색(searchMovieList) 결과를 movies 에 upsert 한다.
     * 박스오피스 수치가 없으므로 audienceAcc 는 기본값(0)을 그대로 둔다.
     *
     * MovieService.searchMovies() 는 @Transactional(propagation = Propagation.NOT_SUPPORTED) 로
     * 주변 읽기 트랜잭션을 없애 호출하므로 sync() 와 동일한 REQUIRES_NEW 격리 이유가 적용되지는
     * 않지만, 이 메서드 자체가 여러 건을 upsert 하는 하나의 단위이므로 REQUIRES_NEW 로 묶어
     * 부분 실패 시에도 독립적인 쓰기 트랜잭션을 보장한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Movie> upsertSearchResults(List<KobisMovieItem> items) {
        List<Movie> result = new ArrayList<>();
        for (KobisMovieItem item : items) {
            Movie movie = movieRepository.findByMovieCd(item.getMovieCd())
                    .orElseGet(() -> Movie.builder()
                            .movieCd(item.getMovieCd())
                            .title(item.getMovieNm())
                            .originalTitle(item.getMovieNmEn())
                            .openDt(item.getOpenDt())
                            .build());

            if (movie.getTmdbId() == null) {
                Integer releaseYear = item.getOpenDt() != null ? item.getOpenDt().getYear() : null;
                enrichWithTmdb(movie, item.getMovieNm(), releaseYear, item.getMovieCd());
            }

            result.add(movieRepository.save(movie));
        }
        return result;
    }

    private void enrichWithTmdb(Movie movie, String title, Integer releaseYear, String movieCd) {
        Optional<TmdbMovie> found = tmdbClient.search(title, releaseYear);
        if (found.isEmpty()) {
            log.warn("[BoxofficeSync] TMDB 매칭 실패 - movieCd: {}, title: {}", movieCd, title);
            return;
        }

        TmdbMovie tmdb = found.get();
        movie.applyTmdb(
                tmdb.getTmdbId(),
                tmdb.getOriginalTitle(),
                tmdb.getOverview(),
                tmdb.getGenre(),
                tmdb.getGenreIds(),
                tmdb.getPosterUrl(),
                tmdb.getVoteAverage());

        log.info("[BoxofficeSync] TMDB 보강 완료 - movieCd: {}, tmdbId: {}, genre: {}",
                movieCd, tmdb.getTmdbId(), tmdb.getGenre());
    }
}
