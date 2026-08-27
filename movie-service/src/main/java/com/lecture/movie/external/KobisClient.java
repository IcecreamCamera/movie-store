package com.lecture.movie.external;

import com.lecture.movie.config.OpenApiProperties;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.external.dto.KobisBoxofficeItem;
import com.lecture.movie.external.dto.KobisMovieItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KobisClient {

    private static final DateTimeFormatter TARGET_DT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WebClient.Builder webClientBuilder;
    private final OpenApiProperties properties;

    /**
     * KOBIS는 집계가 끝난 과거 날짜만 조회할 수 있다.
     * - DAILY: 어제
     * - WEEKLY: 직전 일요일 (월~일 주간 집계가 일요일에 마감된다)
     */
    public static LocalDate resolveTargetDate(RankType rankType, LocalDate today) {
        if (rankType == RankType.DAILY) {
            return today.minusDays(1);
        }
        return today.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
    }

    public List<KobisBoxofficeItem> fetch(RankType rankType, LocalDate targetDate) {
        String path = rankType == RankType.DAILY
                ? "/boxoffice/searchDailyBoxOfficeList.json"
                : "/boxoffice/searchWeeklyBoxOfficeList.json";
        String listKey = rankType == RankType.DAILY
                ? "dailyBoxOfficeList"
                : "weeklyBoxOfficeList";

        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(properties.getKobis().getBaseUrl() + path)
                .queryParam("key", properties.getKobis().getKey())
                .queryParam("targetDt", targetDate.format(TARGET_DT));

        if (rankType == RankType.WEEKLY) {
            uri.queryParam("weekGb", "0"); // 0=주간(월~일)
        }

        try {
            Map<String, Object> body = webClientBuilder.build()
                    .get()
                    .uri(uri.build(true).toUri())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (body == null) {
                log.warn("[KobisClient] 응답 본문이 비어 있습니다 - {} {}", rankType, targetDate);
                return List.of();
            }

            Object resultObj = body.get("boxOfficeResult");
            if (!(resultObj instanceof Map<?, ?> result)) {
                log.warn("[KobisClient] boxOfficeResult 없음 - body: {}", body);
                return List.of();
            }

            Object listObj = result.get(listKey);
            if (!(listObj instanceof List<?> rawList)) {
                log.warn("[KobisClient] {} 없음 - result: {}", listKey, result);
                return List.of();
            }

            List<KobisBoxofficeItem> items = new ArrayList<>();
            for (Object rawItem : rawList) {
                if (rawItem instanceof Map<?, ?> item) {
                    items.add(toItem(item));
                }
            }

            log.info("[KobisClient] {} 박스오피스 {}건 조회 - targetDt: {}",
                    rankType, items.size(), targetDate);
            return items;

        } catch (Exception e) {
            log.error("[KobisClient] 조회 실패 - {} {}, error: {}",
                    rankType, targetDate, e.getMessage());
            return List.of();
        }
    }

    /**
     * 영화 이름으로 KOBIS 를 검색한다.
     * KOBIS는 부분일치 검색이라 관련 없어 보이는 결과도 섞여 오지만 그대로 반환한다.
     * 검색어는 한글이 대부분이므로 fetch() 와 달리 encode() 로 URL 인코딩한다.
     */
    public List<KobisMovieItem> searchMovies(String query) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(properties.getKobis().getBaseUrl() + "/movie/searchMovieList.json")
                .queryParam("key", properties.getKobis().getKey())
                .queryParam("movieNm", query)
                .queryParam("itemPerPage", 10);

        try {
            Map<String, Object> body = webClientBuilder.build()
                    .get()
                    .uri(uri.encode().build().toUri())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (body == null) {
                log.warn("[KobisClient] 검색 응답 본문이 비어 있습니다 - query: {}", query);
                return List.of();
            }

            Object resultObj = body.get("movieListResult");
            if (!(resultObj instanceof Map<?, ?> result)) {
                log.warn("[KobisClient] movieListResult 없음 - body: {}", body);
                return List.of();
            }

            Object listObj = result.get("movieList");
            if (!(listObj instanceof List<?> rawList)) {
                log.warn("[KobisClient] movieList 없음 - result: {}", result);
                return List.of();
            }

            List<KobisMovieItem> items = new ArrayList<>();
            for (Object rawItem : rawList) {
                if (rawItem instanceof Map<?, ?> item) {
                    items.add(toSearchItem(item));
                }
            }

            log.info("[KobisClient] 검색 {}건 조회 - query: {}", items.size(), query);
            return items;

        } catch (Exception e) {
            log.error("[KobisClient] 검색 실패 - query: {}, error: {}", query, e.getMessage());
            return List.of();
        }
    }

    private KobisMovieItem toSearchItem(Map<?, ?> item) {
        return KobisMovieItem.builder()
                .movieCd(asString(item.get("movieCd")))
                .movieNm(asString(item.get("movieNm")))
                .movieNmEn(asString(item.get("movieNmEn")))
                .openDt(parseCompactDate(asString(item.get("openDt"))))
                .genreNm(asString(item.get("repGenreNm")))
                .directorName(firstDirectorName(item.get("directors")))
                .build();
    }

    private String firstDirectorName(Object value) {
        if (!(value instanceof List<?> rawList) || rawList.isEmpty()) {
            return null;
        }
        Object first = rawList.get(0);
        if (!(first instanceof Map<?, ?> director)) {
            return null;
        }
        return asString(director.get("peopleNm"));
    }

    private KobisBoxofficeItem toItem(Map<?, ?> item) {
        return KobisBoxofficeItem.builder()
                .rankNo(parseInt(item.get("rank")))
                .rankInten(parseInt(item.get("rankInten")))
                .movieCd(asString(item.get("movieCd")))
                .movieNm(asString(item.get("movieNm")))
                .openDt(parseDate(asString(item.get("openDt"))))
                .audiCnt(parseLong(item.get("audiCnt")))
                .audiAcc(parseLong(item.get("audiAcc")))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer parseInt(Object value) {
        try {
            return value == null ? null : Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? 0L : Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** KOBIS openDt 는 "yyyy-MM-dd" 이며 미개봉작은 빈 문자열이 온다 */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || !value.contains("-")) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 영화 검색(searchMovieList) 응답의 openDt 는 "yyyyMMdd" (하이픈 없음)이며
     * 미개봉작은 빈 문자열이 온다. 테스트를 위해 package-private.
     */
    LocalDate parseCompactDate(String value) {
        if (value == null || value.isBlank() || value.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(value, TARGET_DT);
        } catch (Exception e) {
            return null;
        }
    }
}
