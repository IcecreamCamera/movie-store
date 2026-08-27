package com.lecture.booking.service;

import com.lecture.booking.dto.BookingDto;
import com.lecture.booking.entity.Booking;
import com.lecture.booking.kafka.BookingKafkaProducer;
import com.lecture.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private MovieServiceClient movieServiceClient;
    private PaymentServiceClient paymentServiceClient;
    private BookingKafkaProducer kafkaProducer;
    private BookingWriteService writeService;
    private BookingService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        movieServiceClient = mock(MovieServiceClient.class);
        paymentServiceClient = mock(PaymentServiceClient.class);
        kafkaProducer = mock(BookingKafkaProducer.class);
        writeService = mock(BookingWriteService.class);
        service = new BookingService(bookingRepository, movieServiceClient,
                paymentServiceClient, kafkaProducer, writeService);
    }

    private PaymentServiceClient.PaymentResult completedResult() {
        PaymentServiceClient.PaymentResult result = mock(PaymentServiceClient.PaymentResult.class);
        when(result.getStatus()).thenReturn("COMPLETED");
        return result;
    }

    @Test
    void 예매금액은_단가에_매수를_곱한_값이다() {
        when(movieServiceClient.existsMovie(1L)).thenReturn(true);
        when(movieServiceClient.getPrice(1L)).thenReturn(new BigDecimal("14000.00"));
        when(writeService.createPendingBooking(eq(1L), eq(1L), eq(3), any()))
                .thenReturn(Booking.builder()
                        .id(100L).userId(1L).movieId(1L).quantity(3)
                        .amount(new BigDecimal("42000.00"))
                        .build());
        PaymentServiceClient.PaymentResult ok = completedResult();
        when(paymentServiceClient.requestPayment(any(), any(), any(), any())).thenReturn(ok);

        BookingDto.BookingResponse response = service.book(1L,
                BookingDto.BookRequest.builder().movieId(1L).quantity(3).build());

        assertEquals(new BigDecimal("42000.00"), response.getAmount());
        verify(paymentServiceClient).requestPayment(1L, 100L, 1L, new BigDecimal("42000.00"));
    }

    @Test
    void 존재하지_않는_영화는_예매할_수_없다() {
        when(movieServiceClient.existsMovie(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.book(1L,
                BookingDto.BookRequest.builder().movieId(999L).quantity(1).build()));

        verify(paymentServiceClient, never()).requestPayment(any(), any(), any(), any());
    }

    @Test
    void 같은_영화를_두_번_예매할_수_있다() {
        when(movieServiceClient.existsMovie(1L)).thenReturn(true);
        when(movieServiceClient.getPrice(1L)).thenReturn(new BigDecimal("14000.00"));
        when(writeService.createPendingBooking(eq(1L), eq(1L), eq(1), any()))
                .thenReturn(Booking.builder().id(101L).userId(1L).movieId(1L).quantity(1)
                        .amount(new BigDecimal("14000.00")).build())
                .thenReturn(Booking.builder().id(102L).userId(1L).movieId(1L).quantity(1)
                        .amount(new BigDecimal("14000.00")).build());
        PaymentServiceClient.PaymentResult ok = completedResult();
        when(paymentServiceClient.requestPayment(any(), any(), any(), any())).thenReturn(ok);

        BookingDto.BookRequest request =
                BookingDto.BookRequest.builder().movieId(1L).quantity(1).build();

        assertEquals(101L, service.book(1L, request).getId());
        assertEquals(102L, service.book(1L, request).getId());
    }

    @Test
    void 결제가_실패하면_예매를_취소하고_예외를_던진다() {
        when(movieServiceClient.existsMovie(1L)).thenReturn(true);
        when(movieServiceClient.getPrice(1L)).thenReturn(new BigDecimal("14000.00"));
        when(writeService.createPendingBooking(eq(1L), eq(1L), eq(1), any()))
                .thenReturn(Booking.builder()
                        .id(200L).userId(1L).movieId(1L).quantity(1)
                        .amount(new BigDecimal("14000.00"))
                        .build());
        PaymentServiceClient.PaymentResult failed = mock(PaymentServiceClient.PaymentResult.class);
        when(failed.getStatus()).thenReturn("FAILED");
        when(paymentServiceClient.requestPayment(any(), any(), any(), any())).thenReturn(failed);

        assertThrows(IllegalStateException.class, () -> service.book(1L,
                BookingDto.BookRequest.builder().movieId(1L).quantity(1).build()));

        verify(writeService).cancelBooking(200L);
    }

    @Test
    void 결제_요청이_예외를_던지면_예매를_취소하고_예외를_전파한다() {
        when(movieServiceClient.existsMovie(1L)).thenReturn(true);
        when(movieServiceClient.getPrice(1L)).thenReturn(new BigDecimal("14000.00"));
        when(writeService.createPendingBooking(eq(1L), eq(1L), eq(1), any()))
                .thenReturn(Booking.builder()
                        .id(300L).userId(1L).movieId(1L).quantity(1)
                        .amount(new BigDecimal("14000.00"))
                        .build());
        when(paymentServiceClient.requestPayment(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Payment Service 연결 실패"));

        assertThrows(RuntimeException.class, () -> service.book(1L,
                BookingDto.BookRequest.builder().movieId(1L).quantity(1).build()));

        verify(writeService).cancelBooking(300L);
    }

    @Test
    void bookingId로_예매를_확정하고_이벤트를_발행한다() {
        Booking booking = Booking.builder()
                .id(100L).userId(1L).movieId(7L).quantity(2)
                .amount(new BigDecimal("28000.00"))
                .build();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(movieServiceClient.getMovie(7L)).thenReturn(Map.of("genre", "ACTION"));

        service.confirmBooking(100L);

        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
        verify(movieServiceClient).increaseBookingCount(7L);
        verify(kafkaProducer).publishBookingCompleted(argThat(
                event -> event.getBookingId().equals(100L)
                        && event.getMovieId().equals(7L)
                        && "ACTION".equals(event.getGenre())));
    }
}
