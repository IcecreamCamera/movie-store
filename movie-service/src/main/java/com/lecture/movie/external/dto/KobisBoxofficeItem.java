package com.lecture.movie.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * KOBIS 박스오피스 응답 1행.
 * KOBIS는 모든 필드를 문자열로 주므로 여기서 타입을 확정한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class KobisBoxofficeItem {
    private final Integer rankNo;
    private final Integer rankInten;
    private final String movieCd;
    private final String movieNm;
    private final LocalDate openDt;
    private final Long audiCnt;
    private final Long audiAcc;
}
