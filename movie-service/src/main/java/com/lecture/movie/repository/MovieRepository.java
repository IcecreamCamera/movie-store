package com.lecture.movie.repository;

import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    /** 오픈API upsert 기준 조회 */
    Optional<Movie> findByMovieCd(String movieCd);

    List<Movie> findByStatus(Movie.Status status);

    List<Movie> findByGenreAndStatus(Genre genre, Movie.Status status);
}
