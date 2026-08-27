package com.lecture.booking.service;

import com.lecture.booking.entity.Booking;
import com.lecture.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingWriteService {

    private final BookingRepository bookingRepository;

    /**
     * 반드시 독립 트랜잭션으로 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking createPendingBooking(Long userId, Long movieId) {

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .userId(userId)
                        .movieId(movieId)
                        .build()
        );

        log.info("[BookingWriteService] PENDING booking 생성 완료 - bookingId: {}, userId: {}, movieId: {}",
                booking.getId(), userId, movieId);

        return booking;
    }
}
