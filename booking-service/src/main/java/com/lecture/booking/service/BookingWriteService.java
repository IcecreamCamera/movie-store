package com.lecture.booking.service;

import com.lecture.booking.entity.Booking;
import com.lecture.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingWriteService {

    private final BookingRepository bookingRepository;

    /**
     * PENDING 예매를 독립 트랜잭션으로 즉시 커밋한다.
     * 결제 요청보다 먼저 커밋되어야, payment.completed 이벤트가 도착했을 때
     * booking 행을 조회할 수 있다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking createPendingBooking(Long userId, Long movieId, Integer quantity, BigDecimal amount) {
        Booking booking = bookingRepository.save(
                Booking.builder()
                        .userId(userId)
                        .movieId(movieId)
                        .quantity(quantity)
                        .amount(amount)
                        .build()
        );

        log.info("[BookingWriteService] PENDING 예매 생성 - bookingId: {}, userId: {}, movieId: {}, amount: {}",
                booking.getId(), userId, movieId, amount);

        return booking;
    }
}
