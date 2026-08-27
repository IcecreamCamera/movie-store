package com.lecture.movie.controller;

import com.lecture.movie.dto.MovieDto;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * GET /api/movies/boxoffice?type=DAILY|WEEKLY - 홈화면 박스오피스
     * 스냅샷이 없으면 KOBIS/TMDB를 호출해 채운 뒤 반환한다.
     */
    @GetMapping("/boxoffice")
    public ResponseEntity<MovieDto.ApiResponse<MovieDto.BoxofficeResponse>> getBoxoffice(
            @RequestParam(defaultValue = "DAILY") RankType type) {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getBoxoffice(type))
        );
    }

    /** GET /api/movies - 전체 영화 목록 */
    @GetMapping
    public ResponseEntity<MovieDto.ApiResponse<List<MovieDto.MovieResponse>>> getAllMovies() {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getAllMovies())
        );
    }

    /**
     * GET /api/movies/search?q={검색어} - 영화 이름 검색 (KOBIS 오픈API)
     * 검색 결과는 movies 에 upsert 되어 바로 예매(booking)에 쓸 수 있다.
     */
    @GetMapping("/search")
    public ResponseEntity<MovieDto.ApiResponse<List<MovieDto.MovieResponse>>> searchMovies(
            @RequestParam("q") String q) {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.searchMovies(q))
        );
    }

    /** GET /api/movies/{id} - 영화 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDto.ApiResponse<MovieDto.MovieResponse>> getMovie(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getMovie(id))
        );
    }

    /** GET /api/movies/genre/{genre} - 장르별 목록 */
    @GetMapping("/genre/{genre}")
    public ResponseEntity<MovieDto.ApiResponse<List<MovieDto.MovieResponse>>> getMoviesByGenre(
            @PathVariable Genre genre) {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getMoviesByGenre(genre))
        );
    }

    /** GET /api/movies/internal/exists/{id} - 영화 존재 여부 (booking-service 호출) */
    @GetMapping("/internal/exists/{id}")
    public ResponseEntity<Boolean> existsMovie(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.existsMovie(id));
    }

    /**
     * GET /api/movies/internal/{id} - 영화 정보 (booking / recommend 호출)
     * 래퍼 없이 MovieResponse를 그대로 반환한다.
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<MovieDto.MovieResponse> getMovieInternal(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovie(id));
    }

    /** POST /api/movies/internal/{id}/booking-count - 예매 수 증가 (booking-service 호출) */
    @PostMapping("/internal/{id}/booking-count")
    public ResponseEntity<Void> increaseBookingCount(@PathVariable Long id) {
        movieService.increaseBookingCount(id);
        return ResponseEntity.ok().build();
    }
}
