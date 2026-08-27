package com.lecture.booking.kafka;

import com.lecture.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class BookingKafkaConsumerTest {

    private BookingService bookingService;
    private BookingKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        bookingService = mock(BookingService.class);
        consumer = new BookingKafkaConsumer(bookingService);
    }

    @Test
    void bookingId를_추출해_예매를_확정한다() {
        Map<String, Object> event = new HashMap<>();
        event.put("paymentId", 5);
        event.put("bookingId", 100);
        event.put("userId", 1);
        event.put("movieId", 7);
        event.put("status", "COMPLETED");

        consumer.handlePaymentCompleted(event);

        verify(bookingService).confirmBooking(100L);
    }

    @Test
    void bookingId가_문자열로_와도_확정한다() {
        Map<String, Object> event = new HashMap<>();
        event.put("bookingId", "100");

        consumer.handlePaymentCompleted(event);

        verify(bookingService).confirmBooking(100L);
    }

    @Test
    void bookingId가_없으면_확정하지_않는다() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1);
        event.put("movieId", 7);

        consumer.handlePaymentCompleted(event);

        verify(bookingService, never()).confirmBooking(any());
    }

    @Test
    void 확정_중_예외가_나도_리스너는_죽지_않는다() {
        Map<String, Object> event = new HashMap<>();
        event.put("bookingId", 999);
        doThrow(new IllegalArgumentException("없음")).when(bookingService).confirmBooking(999L);

        consumer.handlePaymentCompleted(event); // 예외가 밖으로 나오면 테스트 실패

        verify(bookingService).confirmBooking(999L);
    }
}
