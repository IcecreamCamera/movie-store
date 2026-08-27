package com.lecture.movie.repository;

import com.lecture.movie.entity.BoxofficeRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BoxofficeRankingRepository extends JpaRepository<BoxofficeRanking, Long> {

    List<BoxofficeRanking> findByRankTypeAndTargetDateOrderByRankNoAsc(
            BoxofficeRanking.RankType rankType, LocalDate targetDate);

    boolean existsByRankTypeAndTargetDate(
            BoxofficeRanking.RankType rankType, LocalDate targetDate);

    void deleteByRankTypeAndTargetDate(
            BoxofficeRanking.RankType rankType, LocalDate targetDate);
}
