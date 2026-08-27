package com.lecture.booking.controller;

import com.lecture.booking.dto.BookingDto;
import com.lecture.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /bookings - 영화 예매
     * Gateway에서 X-User-Id 헤더로 사용자 ID 전달
     */
    @PostMapping
    public ResponseEntity<BookingDto.ApiResponse<BookingDto.BookingResponse>> book(
            @Valid @RequestBody BookingDto.BookRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        BookingDto.BookingResponse response =
                bookingService.book(userId, request.getMovieId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingDto.ApiResponse.success(response));
    }

    /**
     * GET /bookings/my - 내 예매 목록 조회
     * Gateway가 전달한 X-User-Id 헤더를 사용
     */
    @GetMapping("/my")
    public ResponseEntity<BookingDto.ApiResponse<List<BookingDto.BookingResponse>>> getMyBookings(
            @RequestHeader("X-User-Id") Long userId) {

        List<BookingDto.BookingResponse> response =
                bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(BookingDto.ApiResponse.success(response));
    }

    /**
     * GET /bookings/user/{userId} - 특정 사용자 예매 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<BookingDto.ApiResponse<List<BookingDto.BookingResponse>>> getBookings(
            @PathVariable Long userId) {

        List<BookingDto.BookingResponse> response =
                bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(BookingDto.ApiResponse.success(response));
    }

    /**
     * GET /bookings/internal/history/{userId} - 예매 이력 조회 (Recommend Service용)
     */
    @GetMapping("/internal/history/{userId}")
    public ResponseEntity<BookingDto.BookingHistoryResponse> getBookingHistory(
            @PathVariable Long userId) {

        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }
}
