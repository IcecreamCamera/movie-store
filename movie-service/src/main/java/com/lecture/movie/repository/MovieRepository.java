package com.lecture.movie.repository;

import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    /** 오픈API upsert 기준 조회 */
    Optional<Movie> findByMovieCd(String movieCd);

    List<Movie> findByStatus(Movie.Status status);

    List<Movie> findByGenreAndStatus(Genre genre, Movie.Status status);

    /** 동시 확정 시 lost-update 를 막기 위한 원자적 증가 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Movie m SET m.bookingCount = m.bookingCount + 1 WHERE m.id = :id")
    int incrementBookingCount(@Param("id") Long id);
}
