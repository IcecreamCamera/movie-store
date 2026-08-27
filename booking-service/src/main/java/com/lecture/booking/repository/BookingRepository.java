package com.lecture.booking.repository;

import com.lecture.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, Booking.Status status);

    Optional<Booking> findByUserIdAndMovieId(Long userId, Long movieId);

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    // 예매 확정(ACTIVE)된 영화 ID 목록 - 추천 서비스용
    List<Booking> findByUserIdAndStatusIn(Long userId, List<Booking.Status> statuses);
}
