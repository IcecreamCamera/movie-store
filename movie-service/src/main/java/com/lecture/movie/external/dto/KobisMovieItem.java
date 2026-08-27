package com.lecture.movie.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * KOBIS 영화 검색(searchMovieList) 응답 1행.
 * KOBIS는 모든 필드를 문자열로 주므로 여기서 타입을 확정한다.
 * 박스오피스 응답과 달리 audiCnt/audiAcc/rank 가 없다.
 */
@Getter
@Builder
@AllArgsConstructor
public class KobisMovieItem {
    private final String movieCd;
    private final String movieNm;
    private final String movieNmEn;
    private final LocalDate openDt;
    private final String genreNm;
    private final String directorName;
}
