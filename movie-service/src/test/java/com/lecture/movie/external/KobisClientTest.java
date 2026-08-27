package com.lecture.movie.external;

import com.lecture.movie.entity.BoxofficeRanking.RankType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KobisClientTest {

    @Test
    void 일간은_어제_날짜를_쓴다() {
        LocalDate today = LocalDate.of(2026, 8, 27); // 목요일
        assertEquals(LocalDate.of(2026, 8, 26),
                KobisClient.resolveTargetDate(RankType.DAILY, today));
    }

    @Test
    void 주간은_직전_일요일을_쓴다() {
        // 2026-08-27 은 목요일 -> 직전 일요일은 2026-08-23
        LocalDate thursday = LocalDate.of(2026, 8, 27);
        assertEquals(LocalDate.of(2026, 8, 23),
                KobisClient.resolveTargetDate(RankType.WEEKLY, thursday));
    }

    @Test
    void 주간_기준일이_월요일이면_바로_전날_일요일이다() {
        LocalDate monday = LocalDate.of(2026, 8, 24);
        assertEquals(LocalDate.of(2026, 8, 23),
                KobisClient.resolveTargetDate(RankType.WEEKLY, monday));
    }

    @Test
    void 주간_기준일이_일요일이면_한_주_전_일요일이다() {
        LocalDate sunday = LocalDate.of(2026, 8, 23);
        assertEquals(LocalDate.of(2026, 8, 16),
                KobisClient.resolveTargetDate(RankType.WEEKLY, sunday));
    }
}
