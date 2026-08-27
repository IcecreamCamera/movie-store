package com.lecture.movie.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Movie {

    public static final BigDecimal DEFAULT_PRICE = new BigDecimal("14000.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** KOBIS 영화코드 - 오픈API upsert 기준 키 */
    @Column(name = "movie_cd", nullable = false, unique = true, length = 20)
    private String movieCd;

    /** TMDB movie id - 제목 매칭 실패 시 null */
    @Column(name = "tmdb_id")
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Genre genre = Genre.OTHER;

    /** TMDB genre_ids 원본 CSV (예: "28,12,878") */
    @Column(name = "genre_ids", length = 100)
    private String genreIds;

    @Column(name = "open_dt")
    private LocalDate openDt;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "vote_average", precision = 3, scale = 1)
    private BigDecimal voteAverage;

    /** KOBIS 누적 관객수 */
    @Column(name = "audience_acc", nullable = false)
    @Builder.Default
    private Long audienceAcc = 0L;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = DEFAULT_PRICE;

    @Column(name = "booking_count", nullable = false)
    @Builder.Default
    private Integer bookingCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        ACTIVE, INACTIVE
    }

    public void increaseBookingCount() {
        this.bookingCount++;
    }

    /** KOBIS 재수집 시 갱신되는 값 */
    public void updateBoxofficeStats(Long audienceAcc) {
        if (audienceAcc != null) {
            this.audienceAcc = audienceAcc;
        }
    }

    /** TMDB 보강 - tmdbId 가 null 인 영화에 대해 1회만 호출된다 */
    public void applyTmdb(Long tmdbId, String originalTitle, String description,
                          Genre genre, String genreIds, String posterUrl, BigDecimal voteAverage) {
        this.tmdbId = tmdbId;
        this.originalTitle = originalTitle;
        this.description = description;
        this.genre = genre != null ? genre : Genre.OTHER;
        this.genreIds = genreIds;
        this.posterUrl = posterUrl;
        this.voteAverage = voteAverage;
    }
}
