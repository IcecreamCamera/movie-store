package com.lecture.movie.external;

import com.lecture.movie.config.OpenApiProperties;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.external.dto.TmdbMovie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClient {

    private final WebClient.Builder webClientBuilder;
    private final OpenApiProperties properties;

    /**
     * 제목으로 TMDB를 검색한다.
     * 1차: 개봉연도 필터 포함 (정확도 우선)
     * 2차: 결과가 없으면 연도를 빼고 재검색
     * 검색 응답에 genre_ids/overview/poster_path/vote_average 가 모두 있으므로
     * /movie/{id} 상세 호출은 하지 않는다.
     */
    public Optional<TmdbMovie> search(String title, Integer releaseYear) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        List<Map<String, Object>> results = doSearch(title, releaseYear);
        if (results.isEmpty() && releaseYear != null) {
            log.debug("[TmdbClient] 연도 필터 결과 없음, 연도 제외 재검색 - title: {}", title);
            results = doSearch(title, null);
        }

        if (results.isEmpty()) {
            log.warn("[TmdbClient] 검색 결과 없음 - title: {}, year: {}", title, releaseYear);
            return Optional.empty();
        }

        // 포스터가 있는 첫 번째 결과를 우선 채택하고, 없으면 첫 결과를 쓴다
        Map<String, Object> chosen = results.stream()
                .filter(r -> r.get("poster_path") != null)
                .findFirst()
                .orElse(results.get(0));

        return Optional.of(toTmdbMovie(chosen));
    }

    private List<Map<String, Object>> doSearch(String title, Integer releaseYear) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(properties.getTmdb().getBaseUrl() + "/search/movie")
                .queryParam("api_key", properties.getTmdb().getKey())
                .queryParam("query", title)
                .queryParam("language", properties.getTmdb().getLanguage())
                .queryParam("include_adult", false);

        if (releaseYear != null) {
            uri.queryParam("primary_release_year", releaseYear);
        }

        try {
            Map<String, Object> body = webClientBuilder.build()
                    .get()
                    .uri(uri.encode().build().toUri())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (body == null || !(body.get("results") instanceof List<?> rawResults)) {
                return List.of();
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Object raw : rawResults) {
                if (raw instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) map;
                    results.add(typed);
                }
            }
            return results;

        } catch (Exception e) {
            log.error("[TmdbClient] 검색 실패 - title: {}, error: {}", title, e.getMessage());
            return List.of();
        }
    }

    /** 검색 결과 1건을 TmdbMovie로 변환한다 (테스트를 위해 package-private) */
    TmdbMovie toTmdbMovie(Map<String, Object> result) {
        List<Integer> genreIdList = extractGenreIds(result.get("genre_ids"));

        return TmdbMovie.builder()
                .tmdbId(toLong(result.get("id")))
                .originalTitle(toStringOrNull(result.get("original_title")))
                .overview(toStringOrNull(result.get("overview")))
                .genre(Genre.fromTmdbIds(genreIdList))
                .genreIds(genreIdList.isEmpty() ? null : genreIdList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .posterUrl(buildPosterUrl(toStringOrNull(result.get("poster_path"))))
                .voteAverage(toVoteAverage(result.get("vote_average")))
                .build();
    }

    private List<Integer> extractGenreIds(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (Object raw : rawList) {
            if (raw instanceof Number number) {
                ids.add(number.intValue());
            }
        }
        return ids;
    }

    private String buildPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return properties.getTmdb().getImageBaseUrl() + posterPath;
    }

    /** DB 컬럼이 DECIMAL(3,1) 이므로 소수 첫째 자리로 반올림한다 */
    private BigDecimal toVoteAverage(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        return BigDecimal.valueOf(number.doubleValue()).setScale(1, RoundingMode.HALF_UP);
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String toStringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString();
        return s.isBlank() ? null : s;
    }
}
