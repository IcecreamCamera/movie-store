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

    /**
     * 결제 실패한 예매를 독립 트랜잭션으로 취소한다.
     * createPendingBooking 이 REQUIRES_NEW 로 이미 커밋한 행이므로,
     * 호출자의 트랜잭션과 무관하게 별도로 상태를 되돌려야 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelBooking(Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.cancel();
            log.info("[BookingWriteService] 예매 취소 - bookingId: {}", bookingId);
        });
    }
}
