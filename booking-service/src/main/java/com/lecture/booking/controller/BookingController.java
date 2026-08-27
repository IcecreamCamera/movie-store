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
     * POST /api/bookings - 예매
     * Gateway가 주입한 X-User-Id 헤더로 사용자를 식별한다.
     */
    @PostMapping
    public ResponseEntity<BookingDto.ApiResponse<BookingDto.BookingResponse>> book(
            @Valid @RequestBody BookingDto.BookRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        BookingDto.BookingResponse response = bookingService.book(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingDto.ApiResponse.success(response));
    }

    /** GET /api/bookings/my - 내 예매 목록 */
    @GetMapping("/my")
    public ResponseEntity<BookingDto.ApiResponse<List<BookingDto.BookingResponse>>> getMyBookings(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(
                BookingDto.ApiResponse.success(bookingService.getBookingsByUser(userId))
        );
    }

    /** GET /api/bookings/user/{userId} - 특정 사용자 예매 목록 */
    @GetMapping("/user/{userId}")
    public ResponseEntity<BookingDto.ApiResponse<List<BookingDto.BookingResponse>>> getBookings(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                BookingDto.ApiResponse.success(bookingService.getBookingsByUser(userId))
        );
    }

    /**
     * GET /api/bookings/internal/history/{userId} - 예매 이력 (Recommend Service용)
     * 래퍼 없이 반환한다.
     */
    @GetMapping("/internal/history/{userId}")
    public ResponseEntity<BookingDto.BookingHistoryResponse> getBookingHistory(
            @PathVariable Long userId) {

        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }
}
