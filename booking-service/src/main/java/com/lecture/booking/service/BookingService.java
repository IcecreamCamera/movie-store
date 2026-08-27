package com.lecture.booking.service;

import com.lecture.booking.dto.BookingDto;
import com.lecture.booking.entity.Booking;
import com.lecture.booking.kafka.BookingKafkaProducer;
import com.lecture.booking.kafka.KafkaEvent;
import com.lecture.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MovieServiceClient movieServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final BookingKafkaProducer kafkaProducer;
    private final BookingWriteService bookingWriteService;

    /**
     * 영화 예매 전체 흐름
     * 1. 영화 존재 확인
     * 2. 중복 예매 확인
     * 3. Booking 생성 및 즉시 커밋 (PENDING)
     * 4. 결제 요청
     */
    public BookingDto.BookingResponse book(Long userId, Long movieId) {
        if (!movieServiceClient.existsMovie(movieId)) {
            throw new IllegalArgumentException("존재하지 않는 영화입니다: " + movieId);
        }

        if (bookingRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new IllegalArgumentException("이미 예매한 영화입니다");
        }

        Booking booking = bookingWriteService.createPendingBooking(userId, movieId);

        // TODO: 금액이 하드코딩되어 있음. movie-service에서 가격을 받아와 전달해야 함
        paymentServiceClient.requestPayment(userId, movieId, BigDecimal.valueOf(99000));

        log.info("[BookingService] 예매 완료 (결제 대기) - bookingId: {}", booking.getId());
        return BookingDto.BookingResponse.from(booking);
    }

    /**
     * 예매 확정
     */
    @Transactional
    public void activateBooking(Long userId, Long movieId) {
        Booking booking = bookingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "예매 정보를 찾을 수 없습니다 - userId: " + userId + ", movieId: " + movieId));

        booking.activate();

        movieServiceClient.increaseBookingCount(movieId);

        kafkaProducer.publishBookingCompleted(
                KafkaEvent.BookingCompletedEvent.builder()
                        .bookingId(booking.getId())
                        .userId(userId)
                        .movieId(movieId)
                        .build()
        );

        log.info("[BookingService] 예매 확정 완료 - bookingId: {}", booking.getId());
    }

    /**
     * 사용자 예매 목록 조회
     * - movie-service에서 영화 상세 정보를 붙여서 반환
     */
    public List<BookingDto.BookingResponse> getBookingsByUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);

        return bookings.stream()
                .map(booking -> {
                    Map<String, Object> movieInfo = movieServiceClient.getMovie(booking.getMovieId());

                    BookingDto.MovieSummary movieSummary = BookingDto.MovieSummary.builder()
                            .id(toLong(movieInfo.get("id")))
                            .title((String) movieInfo.get("title"))
                            .description((String) movieInfo.get("description"))
                            .genre(normalizeGenre((String) movieInfo.get("genre")))
                            .price(toInteger(movieInfo.get("price")))
                            .poster(
                                    firstNonNull(
                                            (String) movieInfo.get("poster"),
                                            (String) movieInfo.get("posterUrl"),
                                            (String) movieInfo.get("thumbnail")
                                    )
                            )
                            .director(
                                    firstNonNull(
                                            (String) movieInfo.get("director"),
                                            (String) movieInfo.get("directorName"),
                                            (String) movieInfo.get("director_name")
                                    )
                            )
                            .bookingCount(toInteger(
                                    firstNonNullObject(
                                            movieInfo.get("bookingCount"),
                                            movieInfo.get("booking_count")
                                    )
                            ))
                            .build();

                    return BookingDto.BookingResponse.from(booking, movieSummary);
                })
                .collect(Collectors.toList());
    }

    /**
     * 예매 이력 조회 - 추천 서비스용
     */
    public BookingDto.BookingHistoryResponse getBookingHistory(Long userId) {
        List<Long> activeMovieIds = bookingRepository
                .findByUserIdAndStatus(userId, Booking.Status.ACTIVE)
                .stream()
                .map(Booking::getMovieId)
                .collect(Collectors.toList());

        return BookingDto.BookingHistoryResponse.builder()
                .userId(userId)
                .activeMovieIds(activeMovieIds)
                .build();
    }

    /**
     * 장르 표기 정규화.
     *
     * 리팩토링 전에는 강의 카테고리(BACKEND → 백엔드 등)를 한글로 매핑했으나,
     * movie-service의 genre enum이 아직 확정되지 않아 현재는 원본 값을 그대로 통과시킨다.
     * TODO: genre enum 확정 후 매핑 규칙 작성
     */
    private String normalizeGenre(String genre) {
        return genre;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Object firstNonNullObject(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
