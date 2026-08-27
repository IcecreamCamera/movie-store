package com.lecture.booking.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.booking-completed}")
    private String bookingCompletedTopic;

    /** booking.completed 발행 → Recommend Service가 음식 추천 갱신 */
    public void publishBookingCompleted(KafkaEvent.BookingCompletedEvent event) {
        log.info("[Kafka Producer] booking.completed 발행 - bookingId: {}, userId: {}, movieId: {}, genre: {}",
                event.getBookingId(), event.getUserId(), event.getMovieId(), event.getGenre());

        kafkaTemplate.send(bookingCompletedTopic, String.valueOf(event.getUserId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka Producer] booking.completed 발행 실패: {}", ex.getMessage());
                    } else {
                        log.info("[Kafka Producer] booking.completed 발행 성공 - offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
