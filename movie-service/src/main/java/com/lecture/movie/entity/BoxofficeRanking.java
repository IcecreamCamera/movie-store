package com.lecture.movie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일간/주간 박스오피스 순위 스냅샷.
 * movie 는 지연 로딩하지 않고 movie_id 만 보관한다 (MSA 관례상 서비스 내부지만 조인 최소화).
 */
@Entity
@Table(name = "boxoffice_rankings",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_boxoffice",
               columnNames = {"rank_type", "target_date", "rank_no"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoxofficeRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank_type", nullable = false, length = 10)
    private RankType rankType;

    /** KOBIS targetDt - 집계 기준일 */
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    /** rank 는 MariaDB 예약어라 rank_no 로 매핑한다 */
    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(name = "audience_cnt", nullable = false)
    @Builder.Default
    private Long audienceCnt = 0L;

    /** 전일/전주 대비 순위 등락 (KOBIS rankInten) */
    @Column(name = "rank_inten")
    private Integer rankInten;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    public enum RankType {
        DAILY, WEEKLY
    }
}
