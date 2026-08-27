package com.lecture.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 영화 예매.
 * 강의 수강(enrollments)과 달리 UNIQUE(user_id, movie_id) 제약이 없다.
 * 같은 영화를 여러 번 예매할 수 있으므로 결제는 booking_id 로 매칭한다.
 */
@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /** 예매 시점의 price * quantity 스냅샷 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,    // 예매 생성, 결제 대기
        CONFIRMED,  // 결제 완료, 예매 확정
        CANCELLED   // 취소
    }

    public void confirm() {
        this.status = Status.CONFIRMED;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }
}
