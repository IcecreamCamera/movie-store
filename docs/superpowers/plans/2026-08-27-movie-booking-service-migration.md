# movie-service / booking-service 전환 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `course-service`와 `enrollment-service`를 영화 예매 도메인(`movie-service`, `booking-service`)으로 전환하고, KOBIS 박스오피스 + TMDB 메타데이터 수집 기능을 이식한다.

**Architecture:** 기존 MSA 구조(Eureka + Gateway + WebClient 동기 호출 + Kafka 이벤트 체인)를 그대로 유지한 채 도메인만 교체한다. movie-service가 KOBIS/TMDB 오픈API를 lazy 방식으로 수집해 `movies` + `boxoffice_rankings`에 캐시하고, booking-service는 예매 생성 → 결제 요청 → `payment.completed` 수신 → 예매 확정 → `booking.completed` 발행 흐름을 담당한다.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Cloud 2024.0.0, Spring Data JPA, MariaDB 11.2, Spring Kafka, WebClient, Lombok, JUnit 5 + Mockito

**Spec:** `docs/superpowers/specs/2026-08-26-movie-store-schema-design.md`

---

## Global Constraints

- **담당 범위**: `course-service`(→`movie-service`), `enrollment-service`(→`booking-service`) 두 컨테이너만. `user-service` / `payment-service` / `recommend-service` / `vue-frontend`는 **다른 팀원 담당이므로 이 계획에서 수정하지 않는다.**
- **Java 21**, Spring Boot **3.4.5**, Spring Cloud **2024.0.0** (기존 `build.gradle` 값 유지)
- **패키지 규칙**: `com.lecture.movie.*`, `com.lecture.booking.*`
- **DB**: 단일 `lecture_db` 공유, `ddl-auto: update`
- **API 키는 절대 소스에 하드코딩하지 않는다.** `application.yml`은 `${KOBIS_API_KEY}` / `${TMDB_API_KEY}` 플레이스홀더만 두고 실제 값은 `docker-compose.yml`의 `environment` 또는 셸 환경변수로 주입한다.
- **테스트 실행 시 반드시 `--tests` 필터를 쓴다.** 기존 `*ApplicationTests`는 `@SpringBootTest`라 MariaDB가 떠 있어야 통과한다. 이 계획의 단위 테스트는 Spring 컨텍스트 없이 도는 순수 JUnit/Mockito 테스트다.
- **TMDB는 `/search/movie` 한 번만 호출한다.** 검색 응답에 `genre_ids`, `overview`, `poster_path`, `vote_average`, `original_title`이 모두 들어 있다. `/movie/{id}` 상세 호출은 하지 않으며, 따라서 `running_time`은 수집하지 않는다.
- **KOBIS `targetDt`는 항상 집계가 끝난 과거 날짜**를 쓴다. DAILY=어제, WEEKLY=직전 일요일.

### 다른 팀원과 조율이 필요한 계약 변경 (Task 0에서 먼저 공유)

| 대상 | 변경 | 영향 |
|---|---|---|
| payment-service | `POST /api/payments/internal/request` Body가 `{ userId, courseId, amount }` → `{ userId, bookingId, movieId, amount }` | 결제 담당자 |
| payment-service | `payment.completed` 이벤트에 `bookingId` 추가 필수 | 결제 담당자 |
| recommend-service | `enrollment.completed` → `booking.completed` 토픽명 변경, `COURSE_SERVICE_URL` → `MOVIE_SERVICE_URL` | 추천 담당자 |
| api-gateway | `/api/courses/**` → `/api/movies/**`, `/api/enrollments/**` → `/api/bookings/**` | 게이트웨이 이미지 담당자 |
| docker-compose | 서비스명 `course-service`→`movie-service`, `enrollment-service`→`booking-service` | 전원 |

---

## File Structure

### movie-service (구 course-service)

| 경로 | 책임 |
|---|---|
| `entity/Genre.java` | TMDB 장르 enum + tmdbId 역매핑 |
| `entity/Movie.java` | 영화 캐시 엔티티 |
| `entity/BoxofficeRanking.java` | 일간/주간 순위 스냅샷 엔티티 |
| `repository/MovieRepository.java` | 영화 조회/upsert 조회 |
| `repository/BoxofficeRankingRepository.java` | 순위 조회 |
| `config/OpenApiProperties.java` | KOBIS/TMDB 설정 바인딩 |
| `external/KobisClient.java` | KOBIS 호출 + targetDt 계산 + 응답 파싱 |
| `external/TmdbClient.java` | TMDB 검색 1-call + 연도 폴백 |
| `external/dto/KobisBoxofficeItem.java` | KOBIS 응답 1행 |
| `external/dto/TmdbMovie.java` | TMDB 검색 결과 1건 |
| `service/BoxofficeSyncService.java` | lazy 수집: KOBIS→TMDB→upsert→랭킹 insert |
| `service/MovieService.java` | 조회 API 비즈니스 로직 |
| `dto/MovieDto.java` | 요청/응답 DTO |
| `controller/MovieController.java` | REST 엔드포인트 |

### booking-service (구 enrollment-service)

| 경로 | 책임 |
|---|---|
| `entity/Booking.java` | 예매 엔티티 |
| `repository/BookingRepository.java` | 예매 조회 |
| `dto/BookingDto.java` | 요청/응답 DTO |
| `service/MovieServiceClient.java` | movie-service 동기 호출 |
| `service/PaymentServiceClient.java` | payment-service 동기 호출 |
| `service/BookingWriteService.java` | PENDING 예매 독립 트랜잭션 생성 |
| `service/BookingService.java` | 예매 흐름 오케스트레이션 |
| `kafka/KafkaEvent.java` | 이벤트 DTO |
| `kafka/BookingKafkaConsumer.java` | `payment.completed` 소비 |
| `kafka/BookingKafkaProducer.java` | `booking.completed` 발행 |
| `controller/BookingController.java` | REST 엔드포인트 |

---

## Task 0: 계약 변경 공유

**Files:** 없음 (커뮤니케이션)

- [ ] **Step 1: 위 "다른 팀원과 조율이 필요한 계약 변경" 표를 팀에 공유하고 합의**

특히 `payment.completed`에 `bookingId`가 실리지 않으면 booking-service가 예매를 확정할 수 없다. Task 14~16이 이 합의에 의존한다. 합의 전이면 Task 1~13까지는 그대로 진행 가능하다.

---

## Task 1: DB 스키마 교체

**Files:**
- Modify: `init-db/01_init.sql` (전체 교체)

**Interfaces:**
- Produces: `users`, `movies`, `boxoffice_rankings`, `bookings`, `payments` 테이블. 이후 모든 엔티티가 이 컬럼명을 따른다.

- [ ] **Step 1: `init-db/01_init.sql`을 아래 내용으로 전체 교체**

```sql
-- 영화 예매 플랫폼 초기 DDL
-- Spring JPA ddl-auto: update 로도 생성되지만
-- 명시적 DDL로 테이블 선후 관계를 문서화

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    role        VARCHAR(20)     NOT NULL COMMENT 'MEMBER | ADMIN',
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- KOBIS 박스오피스 + TMDB 메타데이터를 합친 영화 캐시
CREATE TABLE IF NOT EXISTS movies (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    movie_cd       VARCHAR(20)    NOT NULL COMMENT 'KOBIS 영화코드',
    tmdb_id        BIGINT         NULL     COMMENT 'TMDB movie id (매칭 실패 시 NULL)',
    title          VARCHAR(255)   NOT NULL COMMENT 'KOBIS movieNm',
    original_title VARCHAR(255)            COMMENT 'TMDB original_title',
    description    TEXT                    COMMENT 'TMDB overview (ko-KR)',
    genre          VARCHAR(30)    NOT NULL DEFAULT 'OTHER' COMMENT 'TMDB 대표 장르',
    genre_ids      VARCHAR(100)            COMMENT 'TMDB genre_ids CSV 원본',
    open_dt        DATE                    COMMENT 'KOBIS openDt',
    poster_url     VARCHAR(500)            COMMENT 'TMDB poster_path 조합 결과',
    vote_average   DECIMAL(3,1)            COMMENT 'TMDB 평점',
    audience_acc   BIGINT         NOT NULL DEFAULT 0 COMMENT 'KOBIS 누적 관객수',
    price          DECIMAL(10,2)  NOT NULL DEFAULT 14000.00 COMMENT '티켓 단가',
    booking_count  INT            NOT NULL DEFAULT 0,
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE',
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_movies_movie_cd (movie_cd),
    KEY idx_movies_tmdb_id (tmdb_id),
    KEY idx_movies_genre (genre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 일간/주간 박스오피스 순위 스냅샷
-- rank 는 MariaDB 10.2+ 예약어이므로 rank_no 사용
CREATE TABLE IF NOT EXISTS boxoffice_rankings (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    movie_id     BIGINT      NOT NULL,
    rank_type    VARCHAR(10) NOT NULL COMMENT 'DAILY | WEEKLY',
    target_date  DATE        NOT NULL COMMENT '집계 기준일 (KOBIS targetDt)',
    rank_no      INT         NOT NULL,
    audience_cnt BIGINT      NOT NULL DEFAULT 0 COMMENT '해당 기간 관객수',
    rank_inten   INT                  COMMENT '전일/전주 대비 등락 (KOBIS rankInten)',
    fetched_at   DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_boxoffice (rank_type, target_date, rank_no),
    KEY idx_boxoffice_lookup (rank_type, target_date),
    CONSTRAINT fk_boxoffice_movie FOREIGN KEY (movie_id) REFERENCES movies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 예매 (구 enrollments)
-- UNIQUE(user_id, movie_id) 없음: 같은 영화를 여러 번 예매할 수 있다
CREATE TABLE IF NOT EXISTS bookings (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    movie_id   BIGINT        NOT NULL,
    quantity   INT           NOT NULL DEFAULT 1 COMMENT '예매 매수',
    amount     DECIMAL(10,2) NOT NULL COMMENT 'price * quantity 스냅샷',
    status     VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | CONFIRMED | CANCELLED',
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_bookings_user (user_id),
    KEY idx_bookings_movie (movie_id),
    CONSTRAINT fk_bookings_user  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT fk_bookings_movie FOREIGN KEY (movie_id) REFERENCES movies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 결제 (예매 1건당 결제 1건)
CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    user_id        BIGINT        NOT NULL,
    booking_id     BIGINT        NOT NULL,
    movie_id       BIGINT        NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | COMPLETED | FAILED | CANCELLED',
    transaction_id VARCHAR(255)  UNIQUE COMMENT '모의 PG 거래 ID (UUID)',
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_payments_booking (booking_id),
    KEY idx_payments_user (user_id),
    CONSTRAINT fk_payments_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_payments_movie   FOREIGN KEY (movie_id)   REFERENCES movies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 볼륨을 지우고 DB만 띄워 DDL이 통과하는지 확인**

```bash
docker compose down -v
docker compose up -d mariadb
sleep 30
docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db -e "SHOW TABLES;"
```

Expected: `users`, `movies`, `boxoffice_rankings`, `bookings`, `payments` 5개가 출력된다.

- [ ] **Step 3: 커밋**

```bash
git add init-db/01_init.sql
git commit -m "feat(db): 영화 예매 도메인 스키마로 DDL 교체"
```

---

## Task 2: course-service → movie-service 리네임

기능 변경 없이 디렉터리·패키지·설정 이름만 바꾸고 빌드가 통과하는 상태를 만든다. 엔티티 내용은 Task 3에서 바꾼다.

**Files:**
- Rename: `course-service/` → `movie-service/`
- Rename: `movie-service/src/main/java/com/lecture/course/` → `.../com/lecture/movie/`
- Modify: `movie-service/settings.gradle`, `movie-service/src/main/resources/application.yml`
- Modify: 모든 `.java` 파일의 `package` / `import` 선언

**Interfaces:**
- Produces: `com.lecture.movie` 패키지 루트, `MovieServiceApplication` 클래스

- [ ] **Step 1: 디렉터리 이동**

```bash
git mv course-service movie-service
git mv movie-service/src/main/java/com/lecture/course movie-service/src/main/java/com/lecture/movie
git mv movie-service/src/test/java/com/lecture/course movie-service/src/test/java/com/lecture/movie
rm -rf movie-service/bin
find movie-service -name .DS_Store -delete
```

`bin/`은 IDE가 만든 컴파일 산출물 사본이라 삭제한다. 남겨두면 옛 패키지가 클래스패스에 섞인다.

- [ ] **Step 2: 패키지 선언 일괄 치환**

```bash
cd movie-service
grep -rl 'com\.lecture\.course' src | xargs sed -i '' 's/com\.lecture\.course/com.lecture.movie/g'
```

- [ ] **Step 3: 애플리케이션 클래스 및 gradle 설정 이름 변경**

```bash
cd movie-service
git mv src/main/java/com/lecture/movie/CourseServiceApplication.java \
       src/main/java/com/lecture/movie/MovieServiceApplication.java
git mv src/test/java/com/lecture/movie/CourseServiceApplicationTests.java \
       src/test/java/com/lecture/movie/MovieServiceApplicationTests.java
sed -i '' 's/CourseServiceApplication/MovieServiceApplication/g' \
       src/main/java/com/lecture/movie/MovieServiceApplication.java \
       src/test/java/com/lecture/movie/MovieServiceApplicationTests.java
echo "rootProject.name = 'movie-service'" > settings.gradle
sed -i '' 's/name: course-service/name: movie-service/' src/main/resources/application.yml
```

- [ ] **Step 4: 컴파일 확인**

Run:
```bash
cd movie-service && ./gradlew compileJava --no-daemon
```
Expected: `BUILD SUCCESSFUL`. 이 시점에는 클래스명이 아직 `Course*`여도 정상이다 — 패키지만 옮긴 상태다.

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "refactor: course-service를 movie-service로 리네임 (패키지 이동만)"
```

---

## Task 3: Genre enum + Movie 엔티티 + MovieRepository

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/entity/Genre.java`
- Create: `movie-service/src/test/java/com/lecture/movie/entity/GenreTest.java`
- Modify: `movie-service/src/main/java/com/lecture/movie/entity/Course.java` → `Movie.java`로 교체
- Modify: `movie-service/src/main/java/com/lecture/movie/repository/CourseRepository.java` → `MovieRepository.java`로 교체

**Interfaces:**
- Produces:
  - `Genre.fromTmdbId(Integer tmdbId) -> Genre` (미지의 id는 `Genre.OTHER`)
  - `Genre.fromTmdbIds(List<Integer> ids) -> Genre` (첫 번째 유효 장르, 없으면 `OTHER`)
  - `Movie` 엔티티: `getId()`, `getMovieCd()`, `getTmdbId()`, `getTitle()`, `getGenre()`, `getPrice()`, `getBookingCount()`, `getStatus()`, `increaseBookingCount()`, `applyTmdb(Long, String, String, Genre, String, String, BigDecimal)`, `updateBoxofficeStats(Long audienceAcc)`
  - `MovieRepository.findByMovieCd(String)`, `findByStatus(Movie.Status)`, `findByGenreAndStatus(Genre, Movie.Status)`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `movie-service/src/test/java/com/lecture/movie/entity/GenreTest.java`:

```java
package com.lecture.movie.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenreTest {

    @Test
    void tmdbId로_장르를_찾는다() {
        assertEquals(Genre.ACTION, Genre.fromTmdbId(28));
        assertEquals(Genre.SCIENCE_FICTION, Genre.fromTmdbId(878));
        assertEquals(Genre.MYSTERY, Genre.fromTmdbId(9648));
    }

    @Test
    void 알_수_없는_tmdbId는_OTHER다() {
        assertEquals(Genre.OTHER, Genre.fromTmdbId(99999));
        assertEquals(Genre.OTHER, Genre.fromTmdbId(null));
    }

    @Test
    void 장르_배열에서_첫_번째_유효_장르를_고른다() {
        assertEquals(Genre.MYSTERY, Genre.fromTmdbIds(List.of(9648, 27, 53)));
    }

    @Test
    void 장르_배열이_비었거나_전부_미지면_OTHER다() {
        assertEquals(Genre.OTHER, Genre.fromTmdbIds(List.of()));
        assertEquals(Genre.OTHER, Genre.fromTmdbIds(null));
        assertEquals(Genre.OTHER, Genre.fromTmdbIds(List.of(99999)));
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.entity.GenreTest" --no-daemon`
Expected: 컴파일 실패 — `cannot find symbol: class Genre`

- [ ] **Step 3: Genre enum 구현**

Create `movie-service/src/main/java/com/lecture/movie/entity/Genre.java`:

```java
package com.lecture.movie.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TMDB 공식 영화 장르.
 * tmdbId 는 TMDB /genre/movie/list 가 반환하는 장르 ID다.
 * OTHER 는 TMDB 매칭 실패 또는 미지의 장르용 폴백이며 tmdbId 0 은 실제로 존재하지 않는다.
 */
public enum Genre {
    ACTION(28),
    ADVENTURE(12),
    ANIMATION(16),
    COMEDY(35),
    CRIME(80),
    DOCUMENTARY(99),
    DRAMA(18),
    FAMILY(10751),
    FANTASY(14),
    HISTORY(36),
    HORROR(27),
    MUSIC(10402),
    MYSTERY(9648),
    ROMANCE(10749),
    SCIENCE_FICTION(878),
    TV_MOVIE(10770),
    THRILLER(53),
    WAR(10752),
    WESTERN(37),
    OTHER(0);

    private static final Map<Integer, Genre> BY_TMDB_ID = Arrays.stream(values())
            .filter(g -> g != OTHER)
            .collect(Collectors.toMap(Genre::getTmdbId, Function.identity()));

    private final int tmdbId;

    Genre(int tmdbId) {
        this.tmdbId = tmdbId;
    }

    public int getTmdbId() {
        return tmdbId;
    }

    public static Genre fromTmdbId(Integer tmdbId) {
        if (tmdbId == null) {
            return OTHER;
        }
        return BY_TMDB_ID.getOrDefault(tmdbId, OTHER);
    }

    /**
     * TMDB genre_ids 배열에서 대표 장르 하나를 고른다.
     * 배열 순서가 TMDB 기준 관련도 순이므로 첫 번째 유효 장르를 채택한다.
     */
    public static Genre fromTmdbIds(List<Integer> tmdbIds) {
        if (tmdbIds == null || tmdbIds.isEmpty()) {
            return OTHER;
        }
        return tmdbIds.stream()
                .map(Genre::fromTmdbId)
                .filter(g -> g != OTHER)
                .findFirst()
                .orElse(OTHER);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.entity.GenreTest" --no-daemon`
Expected: PASS (4 tests)

- [ ] **Step 5: Movie 엔티티 작성**

```bash
cd movie-service
git rm src/main/java/com/lecture/movie/entity/Course.java
```

Create `movie-service/src/main/java/com/lecture/movie/entity/Movie.java`:

```java
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
```

- [ ] **Step 6: MovieRepository 작성**

```bash
cd movie-service
git rm src/main/java/com/lecture/movie/repository/CourseRepository.java
```

Create `movie-service/src/main/java/com/lecture/movie/repository/MovieRepository.java`:

```java
package com.lecture.movie.repository;

import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    /** 오픈API upsert 기준 조회 */
    Optional<Movie> findByMovieCd(String movieCd);

    List<Movie> findByStatus(Movie.Status status);

    List<Movie> findByGenreAndStatus(Genre genre, Movie.Status status);
}
```

- [ ] **Step 7: 컴파일 확인**

Run: `cd movie-service && ./gradlew compileJava --no-daemon`
Expected: `CourseService`, `CourseController`, `CourseDto`가 아직 `Course`를 참조하므로 **컴파일 실패**한다. 이는 예상된 상태이며 Task 10~12에서 해소된다.

임시로 통과시키려면 이 시점에 `CourseService.java`, `CourseController.java`, `CourseDto.java`를 삭제한다 (Task 10~12에서 새로 만든다):

```bash
cd movie-service
git rm src/main/java/com/lecture/movie/service/CourseService.java
git rm src/main/java/com/lecture/movie/controller/CourseController.java
git rm src/main/java/com/lecture/movie/dto/CourseDto.java
./gradlew compileJava --no-daemon
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "feat(movie): Genre enum, Movie 엔티티, MovieRepository 추가"
```

---

## Task 4: BoxofficeRanking 엔티티 + Repository

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/entity/BoxofficeRanking.java`
- Create: `movie-service/src/main/java/com/lecture/movie/repository/BoxofficeRankingRepository.java`

**Interfaces:**
- Consumes: `Movie` (Task 3)
- Produces:
  - `BoxofficeRanking.RankType` enum (`DAILY`, `WEEKLY`)
  - `BoxofficeRankingRepository.findByRankTypeAndTargetDateOrderByRankNoAsc(RankType, LocalDate) -> List<BoxofficeRanking>`
  - `BoxofficeRankingRepository.existsByRankTypeAndTargetDate(RankType, LocalDate) -> boolean`
  - `BoxofficeRankingRepository.deleteByRankTypeAndTargetDate(RankType, LocalDate)`

- [ ] **Step 1: 엔티티 작성**

Create `movie-service/src/main/java/com/lecture/movie/entity/BoxofficeRanking.java`:

```java
package com.lecture.movie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일간/주간 박스오피스 순위 스냅샷.
 * movie 는 지연 로딩하지 않고 movie_id 만 보관한다 (MSA 관례상 서비스 내부지만 조인 최소화).
 */
@Entity
@Table(name = "boxoffice_rankings",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_boxoffice",
               columnNames = {"rank_type", "target_date", "rank_no"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoxofficeRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank_type", nullable = false, length = 10)
    private RankType rankType;

    /** KOBIS targetDt - 집계 기준일 */
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    /** rank 는 MariaDB 예약어라 rank_no 로 매핑한다 */
    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(name = "audience_cnt", nullable = false)
    @Builder.Default
    private Long audienceCnt = 0L;

    /** 전일/전주 대비 순위 등락 (KOBIS rankInten) */
    @Column(name = "rank_inten")
    private Integer rankInten;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    public enum RankType {
        DAILY, WEEKLY
    }
}
```

- [ ] **Step 2: Repository 작성**

Create `movie-service/src/main/java/com/lecture/movie/repository/BoxofficeRankingRepository.java`:

```java
package com.lecture.movie.repository;

import com.lecture.movie.entity.BoxofficeRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BoxofficeRankingRepository extends JpaRepository<BoxofficeRanking, Long> {

    List<BoxofficeRanking> findByRankTypeAndTargetDateOrderByRankNoAsc(
            BoxofficeRanking.RankType rankType, LocalDate targetDate);

    boolean existsByRankTypeAndTargetDate(
            BoxofficeRanking.RankType rankType, LocalDate targetDate);

    void deleteByRankTypeAndTargetDate(
            BoxofficeRanking.RankType rankType, LocalDate targetDate);
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `cd movie-service && ./gradlew compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "feat(movie): 박스오피스 순위 스냅샷 엔티티 추가"
```

---

## Task 5: enrollment-service → booking-service 리네임 + Booking 엔티티

**Files:**
- Rename: `enrollment-service/` → `booking-service/`
- Rename: `.../com/lecture/enrollment/` → `.../com/lecture/booking/`
- Create: `booking-service/src/main/java/com/lecture/booking/entity/Booking.java`
- Create: `booking-service/src/main/java/com/lecture/booking/repository/BookingRepository.java`
- Modify: `booking-service/settings.gradle`, `booking-service/src/main/resources/application.yml`

**Interfaces:**
- Produces:
  - `Booking` 엔티티: `getId()`, `getUserId()`, `getMovieId()`, `getQuantity()`, `getAmount()`, `getStatus()`, `confirm()`, `cancel()`
  - `Booking.Status` enum (`PENDING`, `CONFIRMED`, `CANCELLED`)
  - `BookingRepository.findByUserIdOrderByCreatedAtDesc(Long)`, `findByUserIdAndStatus(Long, Booking.Status)`

- [ ] **Step 1: 디렉터리 이동 및 패키지 치환**

```bash
git mv enrollment-service booking-service
git mv booking-service/src/main/java/com/lecture/enrollment booking-service/src/main/java/com/lecture/booking
git mv booking-service/src/test/java/com/lecture/enrollment booking-service/src/test/java/com/lecture/booking
rm -rf booking-service/bin
find booking-service -name .DS_Store -delete

cd booking-service
grep -rl 'com\.lecture\.enrollment' src | xargs sed -i '' 's/com\.lecture\.enrollment/com.lecture.booking/g'
git mv src/main/java/com/lecture/booking/EnrollmentServiceApplication.java \
       src/main/java/com/lecture/booking/BookingServiceApplication.java
git mv src/test/java/com/lecture/booking/EnrollmentServiceApplicationTests.java \
       src/test/java/com/lecture/booking/BookingServiceApplicationTests.java
sed -i '' 's/EnrollmentServiceApplication/BookingServiceApplication/g' \
       src/main/java/com/lecture/booking/BookingServiceApplication.java \
       src/test/java/com/lecture/booking/BookingServiceApplicationTests.java
echo "rootProject.name = 'booking-service'" > settings.gradle
```

- [ ] **Step 2: 옛 도메인 클래스 제거 (Task 13~16에서 새로 만든다)**

```bash
cd booking-service/src/main/java/com/lecture/booking
git rm entity/Enrollment.java repository/EnrollmentRepository.java dto/EnrollmentDto.java \
       service/EnrollmentService.java service/EnrollmentWriteService.java \
       service/CourseServiceClient.java service/PaymentServiceClient.java \
       controller/EnrollmentController.java \
       kafka/EnrollmentKafkaConsumer.java kafka/EnrollmentKafkaProducer.java kafka/KafkaEvent.java
```

- [ ] **Step 3: Booking 엔티티 작성**

Create `booking-service/src/main/java/com/lecture/booking/entity/Booking.java`:

```java
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
```

- [ ] **Step 4: BookingRepository 작성**

Create `booking-service/src/main/java/com/lecture/booking/repository/BookingRepository.java`:

```java
package com.lecture.booking.repository;

import com.lecture.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, Booking.Status status);
}
```

- [ ] **Step 5: application.yml 갱신**

Modify `booking-service/src/main/resources/application.yml` — 아래 항목만 교체:

```yaml
spring:
  application:
    name: booking-service

  kafka:
    consumer:
      group-id: booking-service

service:
  movie-service:
    url: http://movie-service:8082
  payment-service:
    url: http://payment-service:8084

kafka:
  topic:
    payment-completed: payment.completed
    booking-completed: booking.completed
```

기존 `service.course-service`와 `kafka.topic.enrollment-completed` 항목은 삭제한다.

- [ ] **Step 6: KafkaConfig의 토픽 프로퍼티 참조 수정**

Modify `booking-service/src/main/java/com/lecture/booking/config/KafkaConfig.java`:

```java
package com.lecture.booking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.payment-completed}")
    private String paymentCompletedTopic;

    @Value("${kafka.topic.booking-completed}")
    private String bookingCompletedTopic;

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(paymentCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingCompletedTopic() {
        return TopicBuilder.name(bookingCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

- [ ] **Step 7: 컴파일 확인**

Run: `cd booking-service && ./gradlew compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL` (도메인 클래스를 지웠으므로 config + Application만 남아 통과한다)

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "refactor: enrollment-service를 booking-service로 리네임하고 Booking 엔티티 추가"
```

---

## Task 6: 오픈API 설정 바인딩

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/config/OpenApiProperties.java`
- Modify: `movie-service/src/main/resources/application.yml`
- Modify: `movie-service/src/main/java/com/lecture/movie/MovieServiceApplication.java`

**Interfaces:**
- Produces: `OpenApiProperties`
  - `getKobis().getBaseUrl()`, `getKobis().getKey()`
  - `getTmdb().getBaseUrl()`, `getTmdb().getKey()`, `getTmdb().getImageBaseUrl()`, `getTmdb().getLanguage()`

- [ ] **Step 1: 설정 클래스 작성**

Create `movie-service/src/main/java/com/lecture/movie/config/OpenApiProperties.java`:

```java
package com.lecture.movie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

    private Kobis kobis = new Kobis();
    private Tmdb tmdb = new Tmdb();

    @Getter
    @Setter
    public static class Kobis {
        private String baseUrl;
        private String key;
    }

    @Getter
    @Setter
    public static class Tmdb {
        private String baseUrl;
        private String imageBaseUrl;
        private String key;
        private String language = "ko-KR";
    }
}
```

- [ ] **Step 2: application.yml에 설정 추가**

Modify `movie-service/src/main/resources/application.yml` — 파일 끝에 추가:

```yaml
openapi:
  kobis:
    base-url: http://www.kobis.or.kr/kobisopenapi/webservice/rest
    key: ${KOBIS_API_KEY:}
  tmdb:
    base-url: https://api.themoviedb.org/3
    image-base-url: https://image.tmdb.org/t/p/w500
    key: ${TMDB_API_KEY:}
    language: ko-KR
```

`${KOBIS_API_KEY:}`처럼 기본값을 빈 문자열로 둬서, 키가 없어도 애플리케이션은 기동되고 수집 시점에만 실패하게 한다.

- [ ] **Step 3: 설정 바인딩 활성화**

Modify `movie-service/src/main/java/com/lecture/movie/MovieServiceApplication.java`:

```java
package com.lecture.movie;

import com.lecture.movie.config.OpenApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(OpenApiProperties.class)
public class MovieServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }
}
```

기존 파일에 `@EnableDiscoveryClient`가 없으면 넣지 말고 그대로 두되, `@EnableConfigurationProperties`만 추가한다.

- [ ] **Step 4: 컴파일 확인**

Run: `cd movie-service && ./gradlew compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat(movie): KOBIS/TMDB 오픈API 설정 바인딩 추가"
```

---

## Task 7: KobisClient

`movie_api.py`의 `get_kobis_boxoffice()`를 이식한다. 프로토타입은 주간만 다뤘으나 일간도 지원한다.

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/external/dto/KobisBoxofficeItem.java`
- Create: `movie-service/src/main/java/com/lecture/movie/external/KobisClient.java`
- Create: `movie-service/src/test/java/com/lecture/movie/external/KobisClientTest.java`

**Interfaces:**
- Consumes: `OpenApiProperties` (Task 6), `BoxofficeRanking.RankType` (Task 4)
- Produces:
  - `KobisClient.resolveTargetDate(RankType, LocalDate today) -> LocalDate` (static, 테스트 대상)
  - `KobisClient.fetch(RankType rankType, LocalDate targetDate) -> List<KobisBoxofficeItem>`
  - `KobisBoxofficeItem`: `getRankNo()`, `getRankInten()`, `getMovieCd()`, `getMovieNm()`, `getOpenDt()`, `getAudiCnt()`, `getAudiAcc()`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `movie-service/src/test/java/com/lecture/movie/external/KobisClientTest.java`:

```java
package com.lecture.movie.external;

import com.lecture.movie.entity.BoxofficeRanking.RankType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KobisClientTest {

    @Test
    void 일간은_어제_날짜를_쓴다() {
        LocalDate today = LocalDate.of(2026, 8, 27); // 목요일
        assertEquals(LocalDate.of(2026, 8, 26),
                KobisClient.resolveTargetDate(RankType.DAILY, today));
    }

    @Test
    void 주간은_직전_일요일을_쓴다() {
        // 2026-08-27 은 목요일 -> 직전 일요일은 2026-08-23
        LocalDate thursday = LocalDate.of(2026, 8, 27);
        assertEquals(LocalDate.of(2026, 8, 23),
                KobisClient.resolveTargetDate(RankType.WEEKLY, thursday));
    }

    @Test
    void 주간_기준일이_월요일이면_바로_전날_일요일이다() {
        LocalDate monday = LocalDate.of(2026, 8, 24);
        assertEquals(LocalDate.of(2026, 8, 23),
                KobisClient.resolveTargetDate(RankType.WEEKLY, monday));
    }

    @Test
    void 주간_기준일이_일요일이면_한_주_전_일요일이다() {
        LocalDate sunday = LocalDate.of(2026, 8, 23);
        assertEquals(LocalDate.of(2026, 8, 16),
                KobisClient.resolveTargetDate(RankType.WEEKLY, sunday));
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.external.KobisClientTest" --no-daemon`
Expected: 컴파일 실패 — `cannot find symbol: class KobisClient`

- [ ] **Step 3: 응답 DTO 작성**

Create `movie-service/src/main/java/com/lecture/movie/external/dto/KobisBoxofficeItem.java`:

```java
package com.lecture.movie.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * KOBIS 박스오피스 응답 1행.
 * KOBIS는 모든 필드를 문자열로 주므로 여기서 타입을 확정한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class KobisBoxofficeItem {
    private final Integer rankNo;
    private final Integer rankInten;
    private final String movieCd;
    private final String movieNm;
    private final LocalDate openDt;
    private final Long audiCnt;
    private final Long audiAcc;
}
```

- [ ] **Step 4: KobisClient 구현**

Create `movie-service/src/main/java/com/lecture/movie/external/KobisClient.java`:

```java
package com.lecture.movie.external;

import com.lecture.movie.config.OpenApiProperties;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.external.dto.KobisBoxofficeItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KobisClient {

    private static final DateTimeFormatter TARGET_DT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WebClient.Builder webClientBuilder;
    private final OpenApiProperties properties;

    /**
     * KOBIS는 집계가 끝난 과거 날짜만 조회할 수 있다.
     * - DAILY: 어제
     * - WEEKLY: 직전 일요일 (월~일 주간 집계가 일요일에 마감된다)
     */
    public static LocalDate resolveTargetDate(RankType rankType, LocalDate today) {
        if (rankType == RankType.DAILY) {
            return today.minusDays(1);
        }
        return today.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
    }

    public List<KobisBoxofficeItem> fetch(RankType rankType, LocalDate targetDate) {
        String path = rankType == RankType.DAILY
                ? "/boxoffice/searchDailyBoxOfficeList.json"
                : "/boxoffice/searchWeeklyBoxOfficeList.json";
        String listKey = rankType == RankType.DAILY
                ? "dailyBoxOfficeList"
                : "weeklyBoxOfficeList";

        UriComponentsBuilder uri = UriComponentsBuilder
                .fromHttpUrl(properties.getKobis().getBaseUrl() + path)
                .queryParam("key", properties.getKobis().getKey())
                .queryParam("targetDt", targetDate.format(TARGET_DT));

        if (rankType == RankType.WEEKLY) {
            uri.queryParam("weekGb", "0"); // 0=주간(월~일)
        }

        try {
            Map<String, Object> body = webClientBuilder.build()
                    .get()
                    .uri(uri.build(true).toUri())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (body == null) {
                log.warn("[KobisClient] 응답 본문이 비어 있습니다 - {} {}", rankType, targetDate);
                return List.of();
            }

            Object resultObj = body.get("boxOfficeResult");
            if (!(resultObj instanceof Map<?, ?> result)) {
                log.warn("[KobisClient] boxOfficeResult 없음 - body: {}", body);
                return List.of();
            }

            Object listObj = result.get(listKey);
            if (!(listObj instanceof List<?> rawList)) {
                log.warn("[KobisClient] {} 없음 - result: {}", listKey, result);
                return List.of();
            }

            List<KobisBoxofficeItem> items = new ArrayList<>();
            for (Object rawItem : rawList) {
                if (rawItem instanceof Map<?, ?> item) {
                    items.add(toItem(item));
                }
            }

            log.info("[KobisClient] {} 박스오피스 {}건 조회 - targetDt: {}",
                    rankType, items.size(), targetDate);
            return items;

        } catch (Exception e) {
            log.error("[KobisClient] 조회 실패 - {} {}, error: {}",
                    rankType, targetDate, e.getMessage());
            return List.of();
        }
    }

    private KobisBoxofficeItem toItem(Map<?, ?> item) {
        return KobisBoxofficeItem.builder()
                .rankNo(parseInt(item.get("rank")))
                .rankInten(parseInt(item.get("rankInten")))
                .movieCd(asString(item.get("movieCd")))
                .movieNm(asString(item.get("movieNm")))
                .openDt(parseDate(asString(item.get("openDt"))))
                .audiCnt(parseLong(item.get("audiCnt")))
                .audiAcc(parseLong(item.get("audiAcc")))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer parseInt(Object value) {
        try {
            return value == null ? null : Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? 0L : Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** KOBIS openDt 는 "yyyy-MM-dd" 이며 미개봉작은 빈 문자열이 온다 */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || !value.contains("-")) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.external.KobisClientTest" --no-daemon`
Expected: PASS (4 tests)

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat(movie): KOBIS 박스오피스 클라이언트 이식"
```

---

## Task 8: TmdbClient

`movie_api.py`의 `get_tmdb_poster()`를 이식하되, 포스터만이 아니라 장르·줄거리·평점까지 한 번에 가져온다.

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/external/dto/TmdbMovie.java`
- Create: `movie-service/src/main/java/com/lecture/movie/external/TmdbClient.java`
- Create: `movie-service/src/test/java/com/lecture/movie/external/TmdbClientTest.java`

**Interfaces:**
- Consumes: `OpenApiProperties` (Task 6), `Genre` (Task 3)
- Produces:
  - `TmdbClient.search(String title, Integer releaseYear) -> Optional<TmdbMovie>`
  - `TmdbClient.toTmdbMovie(Map<String, Object> result) -> TmdbMovie` (package-private, 테스트 대상)
  - `TmdbMovie`: `getTmdbId()`, `getOriginalTitle()`, `getOverview()`, `getGenre()`, `getGenreIds()`, `getPosterUrl()`, `getVoteAverage()`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `movie-service/src/test/java/com/lecture/movie/external/TmdbClientTest.java`:

```java
package com.lecture.movie.external;

import com.lecture.movie.config.OpenApiProperties;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.external.dto.TmdbMovie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TmdbClientTest {

    private TmdbClient client;

    @BeforeEach
    void setUp() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.getTmdb().setBaseUrl("https://api.themoviedb.org/3");
        properties.getTmdb().setImageBaseUrl("https://image.tmdb.org/t/p/w500");
        properties.getTmdb().setKey("dummy");
        client = new TmdbClient(WebClient.builder(), properties);
    }

    @Test
    void 검색결과를_TmdbMovie로_변환한다() {
        Map<String, Object> result = Map.of(
                "id", 838209,
                "original_title", "파묘",
                "overview", "거액의 의뢰를 받은 무당",
                "genre_ids", List.of(9648, 27, 53),
                "poster_path", "/tw0i3kkmOTjDjGFZTLHKhoeXVvA.jpg",
                "vote_average", 7.648
        );

        TmdbMovie movie = client.toTmdbMovie(result);

        assertEquals(838209L, movie.getTmdbId());
        assertEquals("파묘", movie.getOriginalTitle());
        assertEquals("거액의 의뢰를 받은 무당", movie.getOverview());
        assertEquals(Genre.MYSTERY, movie.getGenre());
        assertEquals("9648,27,53", movie.getGenreIds());
        assertEquals("https://image.tmdb.org/t/p/w500/tw0i3kkmOTjDjGFZTLHKhoeXVvA.jpg",
                movie.getPosterUrl());
        assertEquals(new BigDecimal("7.6"), movie.getVoteAverage());
    }

    @Test
    void 포스터가_없으면_posterUrl은_null이다() {
        Map<String, Object> result = Map.of(
                "id", 1,
                "genre_ids", List.of(28)
        );

        TmdbMovie movie = client.toTmdbMovie(result);

        assertNull(movie.getPosterUrl());
        assertEquals(Genre.ACTION, movie.getGenre());
    }

    @Test
    void 장르가_없으면_OTHER다() {
        Map<String, Object> result = Map.of("id", 2);

        TmdbMovie movie = client.toTmdbMovie(result);

        assertEquals(Genre.OTHER, movie.getGenre());
        assertNull(movie.getGenreIds());
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.external.TmdbClientTest" --no-daemon`
Expected: 컴파일 실패 — `cannot find symbol: class TmdbClient`

- [ ] **Step 3: TmdbMovie DTO 작성**

Create `movie-service/src/main/java/com/lecture/movie/external/dto/TmdbMovie.java`:

```java
package com.lecture.movie.external.dto;

import com.lecture.movie.entity.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** TMDB /search/movie 결과 1건을 도메인이 쓰기 좋게 변환한 값 */
@Getter
@Builder
@AllArgsConstructor
public class TmdbMovie {
    private final Long tmdbId;
    private final String originalTitle;
    private final String overview;
    private final Genre genre;
    private final String genreIds;
    private final String posterUrl;
    private final BigDecimal voteAverage;
}
```

- [ ] **Step 4: TmdbClient 구현**

Create `movie-service/src/main/java/com/lecture/movie/external/TmdbClient.java`:

```java
package com.lecture.movie.external;

import com.lecture.movie.config.OpenApiProperties;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.external.dto.TmdbMovie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClient {

    private final WebClient.Builder webClientBuilder;
    private final OpenApiProperties properties;

    /**
     * 제목으로 TMDB를 검색한다.
     * 1차: 개봉연도 필터 포함 (정확도 우선)
     * 2차: 결과가 없으면 연도를 빼고 재검색
     * 검색 응답에 genre_ids/overview/poster_path/vote_average 가 모두 있으므로
     * /movie/{id} 상세 호출은 하지 않는다.
     */
    public Optional<TmdbMovie> search(String title, Integer releaseYear) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        List<Map<String, Object>> results = doSearch(title, releaseYear);
        if (results.isEmpty() && releaseYear != null) {
            log.debug("[TmdbClient] 연도 필터 결과 없음, 연도 제외 재검색 - title: {}", title);
            results = doSearch(title, null);
        }

        if (results.isEmpty()) {
            log.warn("[TmdbClient] 검색 결과 없음 - title: {}, year: {}", title, releaseYear);
            return Optional.empty();
        }

        // 포스터가 있는 첫 번째 결과를 우선 채택하고, 없으면 첫 결과를 쓴다
        Map<String, Object> chosen = results.stream()
                .filter(r -> r.get("poster_path") != null)
                .findFirst()
                .orElse(results.get(0));

        return Optional.of(toTmdbMovie(chosen));
    }

    private List<Map<String, Object>> doSearch(String title, Integer releaseYear) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromHttpUrl(properties.getTmdb().getBaseUrl() + "/search/movie")
                .queryParam("api_key", properties.getTmdb().getKey())
                .queryParam("query", title)
                .queryParam("language", properties.getTmdb().getLanguage())
                .queryParam("include_adult", false);

        if (releaseYear != null) {
            uri.queryParam("primary_release_year", releaseYear);
        }

        try {
            Map<String, Object> body = webClientBuilder.build()
                    .get()
                    .uri(uri.encode().build().toUri())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (body == null || !(body.get("results") instanceof List<?> rawResults)) {
                return List.of();
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Object raw : rawResults) {
                if (raw instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) map;
                    results.add(typed);
                }
            }
            return results;

        } catch (Exception e) {
            log.error("[TmdbClient] 검색 실패 - title: {}, error: {}", title, e.getMessage());
            return List.of();
        }
    }

    /** 검색 결과 1건을 TmdbMovie로 변환한다 (테스트를 위해 package-private) */
    TmdbMovie toTmdbMovie(Map<String, Object> result) {
        List<Integer> genreIdList = extractGenreIds(result.get("genre_ids"));

        return TmdbMovie.builder()
                .tmdbId(toLong(result.get("id")))
                .originalTitle(toStringOrNull(result.get("original_title")))
                .overview(toStringOrNull(result.get("overview")))
                .genre(Genre.fromTmdbIds(genreIdList))
                .genreIds(genreIdList.isEmpty() ? null : genreIdList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .posterUrl(buildPosterUrl(toStringOrNull(result.get("poster_path"))))
                .voteAverage(toVoteAverage(result.get("vote_average")))
                .build();
    }

    private List<Integer> extractGenreIds(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (Object raw : rawList) {
            if (raw instanceof Number number) {
                ids.add(number.intValue());
            }
        }
        return ids;
    }

    private String buildPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return properties.getTmdb().getImageBaseUrl() + posterPath;
    }

    /** DB 컬럼이 DECIMAL(3,1) 이므로 소수 첫째 자리로 반올림한다 */
    private BigDecimal toVoteAverage(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        return BigDecimal.valueOf(number.doubleValue()).setScale(1, RoundingMode.HALF_UP);
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String toStringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString();
        return s.isBlank() ? null : s;
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.external.TmdbClientTest" --no-daemon`
Expected: PASS (3 tests)

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat(movie): TMDB 검색 클라이언트 이식 (장르/포스터/줄거리 1-call)"
```

---

## Task 9: BoxofficeSyncService

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/service/BoxofficeSyncService.java`
- Create: `movie-service/src/test/java/com/lecture/movie/service/BoxofficeSyncServiceTest.java`

**Interfaces:**
- Consumes: `KobisClient` (Task 7), `TmdbClient` (Task 8), `MovieRepository` (Task 3), `BoxofficeRankingRepository` (Task 4)
- Produces:
  - `BoxofficeSyncService.sync(RankType rankType, LocalDate targetDate) -> List<BoxofficeRanking>`
  - `BoxofficeSyncService.upsertMovie(KobisBoxofficeItem item) -> Movie`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `movie-service/src/test/java/com/lecture/movie/service/BoxofficeSyncServiceTest.java`:

```java
package com.lecture.movie.service;

import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.external.KobisClient;
import com.lecture.movie.external.TmdbClient;
import com.lecture.movie.external.dto.KobisBoxofficeItem;
import com.lecture.movie.external.dto.TmdbMovie;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BoxofficeSyncServiceTest {

    private MovieRepository movieRepository;
    private BoxofficeRankingRepository rankingRepository;
    private KobisClient kobisClient;
    private TmdbClient tmdbClient;
    private BoxofficeSyncService service;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        rankingRepository = mock(BoxofficeRankingRepository.class);
        kobisClient = mock(KobisClient.class);
        tmdbClient = mock(TmdbClient.class);
        service = new BoxofficeSyncService(
                movieRepository, rankingRepository, kobisClient, tmdbClient);
    }

    private KobisBoxofficeItem item() {
        return KobisBoxofficeItem.builder()
                .rankNo(1)
                .rankInten(0)
                .movieCd("20250654")
                .movieNm("파묘")
                .openDt(LocalDate.of(2024, 2, 22))
                .audiCnt(2566914L)
                .audiAcc(7109333L)
                .build();
    }

    @Test
    void 신규_영화는_TMDB를_호출해_보강한다() {
        when(movieRepository.findByMovieCd("20250654")).thenReturn(Optional.empty());
        when(tmdbClient.search("파묘", 2024)).thenReturn(Optional.of(
                TmdbMovie.builder()
                        .tmdbId(838209L)
                        .originalTitle("파묘")
                        .overview("줄거리")
                        .genre(Genre.MYSTERY)
                        .genreIds("9648,27,53")
                        .posterUrl("https://image.tmdb.org/t/p/w500/x.jpg")
                        .voteAverage(new BigDecimal("7.6"))
                        .build()));
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.upsertMovie(item());

        assertEquals("20250654", movie.getMovieCd());
        assertEquals(838209L, movie.getTmdbId());
        assertEquals(Genre.MYSTERY, movie.getGenre());
        assertEquals(7109333L, movie.getAudienceAcc());
        verify(tmdbClient, times(1)).search("파묘", 2024);
    }

    @Test
    void 이미_tmdbId가_있으면_TMDB를_다시_호출하지_않는다() {
        Movie existing = Movie.builder()
                .id(1L)
                .movieCd("20250654")
                .tmdbId(838209L)
                .title("파묘")
                .genre(Genre.MYSTERY)
                .audienceAcc(1000L)
                .build();
        when(movieRepository.findByMovieCd("20250654")).thenReturn(Optional.of(existing));
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.upsertMovie(item());

        assertEquals(7109333L, movie.getAudienceAcc()); // 관객수는 갱신된다
        verify(tmdbClient, never()).search(anyString(), anyInt());
    }

    @Test
    void TMDB_매칭에_실패하면_OTHER_장르로_남는다() {
        when(movieRepository.findByMovieCd("20250654")).thenReturn(Optional.empty());
        when(tmdbClient.search("파묘", 2024)).thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie movie = service.upsertMovie(item());

        assertNull(movie.getTmdbId());
        assertEquals(Genre.OTHER, movie.getGenre());
        assertEquals("파묘", movie.getTitle());
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.service.BoxofficeSyncServiceTest" --no-daemon`
Expected: 컴파일 실패 — `cannot find symbol: class BoxofficeSyncService`

- [ ] **Step 3: 구현**

Create `movie-service/src/main/java/com/lecture/movie/service/BoxofficeSyncService.java`:

```java
package com.lecture.movie.service;

import com.lecture.movie.entity.BoxofficeRanking;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.external.KobisClient;
import com.lecture.movie.external.TmdbClient;
import com.lecture.movie.external.dto.KobisBoxofficeItem;
import com.lecture.movie.external.dto.TmdbMovie;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 박스오피스 lazy 수집.
 * 요청 시점에 해당 (rankType, targetDate) 스냅샷이 없으면 오픈API를 호출해 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxofficeSyncService {

    private final MovieRepository movieRepository;
    private final BoxofficeRankingRepository rankingRepository;
    private final KobisClient kobisClient;
    private final TmdbClient tmdbClient;

    /**
     * KOBIS 조회 → movies upsert → 랭킹 스냅샷 재작성.
     * 같은 기준일을 다시 수집하면 기존 스냅샷을 지우고 새로 넣는다 (UNIQUE 충돌 방지).
     */
    @Transactional
    public List<BoxofficeRanking> sync(RankType rankType, LocalDate targetDate) {
        List<KobisBoxofficeItem> items = kobisClient.fetch(rankType, targetDate);
        if (items.isEmpty()) {
            log.warn("[BoxofficeSync] KOBIS 결과 없음 - {} {}", rankType, targetDate);
            return List.of();
        }

        rankingRepository.deleteByRankTypeAndTargetDate(rankType, targetDate);

        List<BoxofficeRanking> rankings = new ArrayList<>();
        for (KobisBoxofficeItem item : items) {
            Movie movie = upsertMovie(item);
            rankings.add(rankingRepository.save(
                    BoxofficeRanking.builder()
                            .movieId(movie.getId())
                            .rankType(rankType)
                            .targetDate(targetDate)
                            .rankNo(item.getRankNo())
                            .audienceCnt(item.getAudiCnt())
                            .rankInten(item.getRankInten())
                            .fetchedAt(LocalDateTime.now())
                            .build()));
        }

        log.info("[BoxofficeSync] 수집 완료 - {} {} ({}건)", rankType, targetDate, rankings.size());
        return rankings;
    }

    /**
     * movieCd 기준 upsert.
     * TMDB 보강은 tmdbId 가 아직 없는 영화에 대해서만 1회 수행한다.
     */
    @Transactional
    public Movie upsertMovie(KobisBoxofficeItem item) {
        Movie movie = movieRepository.findByMovieCd(item.getMovieCd())
                .orElseGet(() -> Movie.builder()
                        .movieCd(item.getMovieCd())
                        .title(item.getMovieNm())
                        .openDt(item.getOpenDt())
                        .build());

        movie.updateBoxofficeStats(item.getAudiAcc());

        if (movie.getTmdbId() == null) {
            enrichWithTmdb(movie, item);
        }

        return movieRepository.save(movie);
    }

    private void enrichWithTmdb(Movie movie, KobisBoxofficeItem item) {
        Integer releaseYear = item.getOpenDt() != null ? item.getOpenDt().getYear() : null;

        Optional<TmdbMovie> found = tmdbClient.search(item.getMovieNm(), releaseYear);
        if (found.isEmpty()) {
            log.warn("[BoxofficeSync] TMDB 매칭 실패 - movieCd: {}, title: {}",
                    item.getMovieCd(), item.getMovieNm());
            return;
        }

        TmdbMovie tmdb = found.get();
        movie.applyTmdb(
                tmdb.getTmdbId(),
                tmdb.getOriginalTitle(),
                tmdb.getOverview(),
                tmdb.getGenre(),
                tmdb.getGenreIds(),
                tmdb.getPosterUrl(),
                tmdb.getVoteAverage());

        log.info("[BoxofficeSync] TMDB 보강 완료 - movieCd: {}, tmdbId: {}, genre: {}",
                item.getMovieCd(), tmdb.getTmdbId(), tmdb.getGenre());
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.service.BoxofficeSyncServiceTest" --no-daemon`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat(movie): 박스오피스 lazy 수집 서비스 추가"
```

---

## Task 10: MovieDto

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/dto/MovieDto.java`

**Interfaces:**
- Consumes: `Movie`, `Genre` (Task 3), `BoxofficeRanking` (Task 4)
- Produces:
  - `MovieDto.MovieResponse.from(Movie) -> MovieResponse`
  - `MovieDto.BoxofficeItem` (rankNo, rankInten, audienceCnt, movie)
  - `MovieDto.BoxofficeResponse` (rankType, targetDate, items)
  - `MovieDto.ApiResponse.success(T)` / `MovieDto.ApiResponse.error(String)`

- [ ] **Step 1: 작성**

Create `movie-service/src/main/java/com/lecture/movie/dto/MovieDto.java`:

```java
package com.lecture.movie.dto;

import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MovieDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovieResponse {
        private Long id;
        private String movieCd;
        private Long tmdbId;
        private String title;
        private String originalTitle;
        private String description;
        private Genre genre;
        private String genreIds;
        private LocalDate openDt;
        private String posterUrl;
        private BigDecimal voteAverage;
        private Long audienceAcc;
        private BigDecimal price;
        private Integer bookingCount;
        private Movie.Status status;
        private LocalDateTime createdAt;

        public static MovieResponse from(Movie movie) {
            return MovieResponse.builder()
                    .id(movie.getId())
                    .movieCd(movie.getMovieCd())
                    .tmdbId(movie.getTmdbId())
                    .title(movie.getTitle())
                    .originalTitle(movie.getOriginalTitle())
                    .description(movie.getDescription())
                    .genre(movie.getGenre())
                    .genreIds(movie.getGenreIds())
                    .openDt(movie.getOpenDt())
                    .posterUrl(movie.getPosterUrl())
                    .voteAverage(movie.getVoteAverage())
                    .audienceAcc(movie.getAudienceAcc())
                    .price(movie.getPrice())
                    .bookingCount(movie.getBookingCount())
                    .status(movie.getStatus())
                    .createdAt(movie.getCreatedAt())
                    .build();
        }
    }

    /** 박스오피스 1행: 순위 정보 + 영화 상세 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoxofficeItem {
        private Integer rankNo;
        private Integer rankInten;
        private Long audienceCnt;
        private MovieResponse movie;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoxofficeResponse {
        private String rankType;
        private LocalDate targetDate;
        private List<BoxofficeItem> items;
    }

    /** 공통 API 응답 래퍼 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `cd movie-service && ./gradlew compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add -A
git commit -m "feat(movie): MovieDto 추가"
```

---

## Task 11: MovieService

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/service/MovieService.java`
- Create: `movie-service/src/test/java/com/lecture/movie/service/MovieServiceTest.java`

**Interfaces:**
- Consumes: `MovieRepository`, `BoxofficeRankingRepository`, `BoxofficeSyncService` (Task 9), `MovieDto` (Task 10)
- Produces:
  - `getBoxoffice(RankType) -> MovieDto.BoxofficeResponse`
  - `getMovie(Long) -> MovieDto.MovieResponse`
  - `getAllMovies() -> List<MovieDto.MovieResponse>`
  - `getMoviesByGenre(Genre) -> List<MovieDto.MovieResponse>`
  - `existsMovie(Long) -> boolean`
  - `increaseBookingCount(Long)`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `movie-service/src/test/java/com/lecture/movie/service/MovieServiceTest.java`:

```java
package com.lecture.movie.service;

import com.lecture.movie.dto.MovieDto;
import com.lecture.movie.entity.BoxofficeRanking;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MovieServiceTest {

    private MovieRepository movieRepository;
    private BoxofficeRankingRepository rankingRepository;
    private BoxofficeSyncService syncService;
    private MovieService service;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        rankingRepository = mock(BoxofficeRankingRepository.class);
        syncService = mock(BoxofficeSyncService.class);
        service = new MovieService(movieRepository, rankingRepository, syncService);
    }

    private Movie movie() {
        return Movie.builder()
                .id(1L)
                .movieCd("20250654")
                .title("파묘")
                .genre(Genre.MYSTERY)
                .build();
    }

    private BoxofficeRanking ranking() {
        return BoxofficeRanking.builder()
                .id(10L)
                .movieId(1L)
                .rankType(RankType.DAILY)
                .targetDate(LocalDate.of(2026, 8, 26))
                .rankNo(1)
                .audienceCnt(1000L)
                .rankInten(0)
                .build();
    }

    @Test
    void 스냅샷이_있으면_오픈API를_호출하지_않는다() {
        when(rankingRepository.findByRankTypeAndTargetDateOrderByRankNoAsc(eq(RankType.DAILY), any()))
                .thenReturn(List.of(ranking()));
        when(movieRepository.findAllById(any())).thenReturn(List.of(movie()));

        MovieDto.BoxofficeResponse response = service.getBoxoffice(RankType.DAILY);

        assertEquals(1, response.getItems().size());
        assertEquals("파묘", response.getItems().get(0).getMovie().getTitle());
        verify(syncService, never()).sync(any(), any());
    }

    @Test
    void 스냅샷이_없으면_수집한_뒤_반환한다() {
        when(rankingRepository.findByRankTypeAndTargetDateOrderByRankNoAsc(eq(RankType.DAILY), any()))
                .thenReturn(List.of());
        when(syncService.sync(eq(RankType.DAILY), any())).thenReturn(List.of(ranking()));
        when(movieRepository.findAllById(any())).thenReturn(List.of(movie()));

        MovieDto.BoxofficeResponse response = service.getBoxoffice(RankType.DAILY);

        assertEquals(1, response.getItems().size());
        verify(syncService, times(1)).sync(eq(RankType.DAILY), any());
    }

    @Test
    void 예매수를_증가시킨다() {
        Movie m = movie();
        when(movieRepository.findById(1L)).thenReturn(Optional.of(m));

        service.increaseBookingCount(1L);

        assertEquals(1, m.getBookingCount());
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.service.MovieServiceTest" --no-daemon`
Expected: 컴파일 실패 — `cannot find symbol: class MovieService`

- [ ] **Step 3: 구현**

Create `movie-service/src/main/java/com/lecture/movie/service/MovieService.java`:

```java
package com.lecture.movie.service;

import com.lecture.movie.dto.MovieDto;
import com.lecture.movie.entity.BoxofficeRanking;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.entity.Movie;
import com.lecture.movie.external.KobisClient;
import com.lecture.movie.repository.BoxofficeRankingRepository;
import com.lecture.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final BoxofficeRankingRepository rankingRepository;
    private final BoxofficeSyncService syncService;

    /**
     * 박스오피스 조회 (홈화면).
     * 스냅샷이 있으면 그대로 쓰고, 없을 때만 오픈API를 호출한다.
     */
    public MovieDto.BoxofficeResponse getBoxoffice(RankType rankType) {
        LocalDate targetDate = KobisClient.resolveTargetDate(rankType, LocalDate.now());

        List<BoxofficeRanking> rankings =
                rankingRepository.findByRankTypeAndTargetDateOrderByRankNoAsc(rankType, targetDate);

        if (rankings.isEmpty()) {
            log.info("[MovieService] 스냅샷 없음, 오픈API 수집 - {} {}", rankType, targetDate);
            rankings = syncService.sync(rankType, targetDate);
        }

        List<Long> movieIds = rankings.stream()
                .map(BoxofficeRanking::getMovieId)
                .collect(Collectors.toList());

        Map<Long, Movie> movieMap = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        List<MovieDto.BoxofficeItem> items = rankings.stream()
                .filter(r -> movieMap.containsKey(r.getMovieId()))
                .map(r -> MovieDto.BoxofficeItem.builder()
                        .rankNo(r.getRankNo())
                        .rankInten(r.getRankInten())
                        .audienceCnt(r.getAudienceCnt())
                        .movie(MovieDto.MovieResponse.from(movieMap.get(r.getMovieId())))
                        .build())
                .collect(Collectors.toList());

        return MovieDto.BoxofficeResponse.builder()
                .rankType(rankType.name())
                .targetDate(targetDate)
                .items(items)
                .build();
    }

    public MovieDto.MovieResponse getMovie(Long id) {
        return MovieDto.MovieResponse.from(findMovieById(id));
    }

    public List<MovieDto.MovieResponse> getAllMovies() {
        return movieRepository.findByStatus(Movie.Status.ACTIVE).stream()
                .map(MovieDto.MovieResponse::from)
                .collect(Collectors.toList());
    }

    public List<MovieDto.MovieResponse> getMoviesByGenre(Genre genre) {
        return movieRepository.findByGenreAndStatus(genre, Movie.Status.ACTIVE).stream()
                .map(MovieDto.MovieResponse::from)
                .collect(Collectors.toList());
    }

    /** booking-service 동기 호출용 */
    public boolean existsMovie(Long id) {
        return movieRepository.existsById(id);
    }

    /** 예매 확정 시 booking-service가 호출 */
    @Transactional
    public void increaseBookingCount(Long movieId) {
        findMovieById(movieId).increaseBookingCount();
    }

    private Movie findMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다: " + id));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd movie-service && ./gradlew test --tests "com.lecture.movie.service.MovieServiceTest" --no-daemon`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat(movie): MovieService 추가 (박스오피스 lazy 조회 포함)"
```

---

## Task 12: MovieController + SecurityConfig

**Files:**
- Create: `movie-service/src/main/java/com/lecture/movie/controller/MovieController.java`
- Modify: `movie-service/src/main/java/com/lecture/movie/config/SecurityConfig.java` (주석 블록의 경로만 갱신)

**Interfaces:**
- Consumes: `MovieService` (Task 11), `MovieDto` (Task 10)
- Produces (booking-service와 recommend-service가 의존하는 계약):
  - `GET /api/movies/boxoffice?type=DAILY|WEEKLY` → `ApiResponse<BoxofficeResponse>`
  - `GET /api/movies` → `ApiResponse<List<MovieResponse>>`
  - `GET /api/movies/{id}` → `ApiResponse<MovieResponse>`
  - `GET /api/movies/genre/{genre}` → `ApiResponse<List<MovieResponse>>`
  - `GET /api/movies/internal/exists/{id}` → `Boolean` (래퍼 없음)
  - `GET /api/movies/internal/{id}` → `MovieResponse` (래퍼 없음)
  - `POST /api/movies/internal/{id}/booking-count` → 200, 본문 없음

- [ ] **Step 1: 컨트롤러 작성**

Create `movie-service/src/main/java/com/lecture/movie/controller/MovieController.java`:

```java
package com.lecture.movie.controller;

import com.lecture.movie.dto.MovieDto;
import com.lecture.movie.entity.BoxofficeRanking.RankType;
import com.lecture.movie.entity.Genre;
import com.lecture.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * GET /api/movies/boxoffice?type=DAILY|WEEKLY - 홈화면 박스오피스
     * 스냅샷이 없으면 KOBIS/TMDB를 호출해 채운 뒤 반환한다.
     */
    @GetMapping("/boxoffice")
    public ResponseEntity<MovieDto.ApiResponse<MovieDto.BoxofficeResponse>> getBoxoffice(
            @RequestParam(defaultValue = "DAILY") RankType type) {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getBoxoffice(type))
        );
    }

    /** GET /api/movies - 전체 영화 목록 */
    @GetMapping
    public ResponseEntity<MovieDto.ApiResponse<List<MovieDto.MovieResponse>>> getAllMovies() {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getAllMovies())
        );
    }

    /** GET /api/movies/{id} - 영화 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDto.ApiResponse<MovieDto.MovieResponse>> getMovie(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getMovie(id))
        );
    }

    /** GET /api/movies/genre/{genre} - 장르별 목록 */
    @GetMapping("/genre/{genre}")
    public ResponseEntity<MovieDto.ApiResponse<List<MovieDto.MovieResponse>>> getMoviesByGenre(
            @PathVariable Genre genre) {
        return ResponseEntity.ok(
                MovieDto.ApiResponse.success(movieService.getMoviesByGenre(genre))
        );
    }

    /** GET /api/movies/internal/exists/{id} - 영화 존재 여부 (booking-service 호출) */
    @GetMapping("/internal/exists/{id}")
    public ResponseEntity<Boolean> existsMovie(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.existsMovie(id));
    }

    /**
     * GET /api/movies/internal/{id} - 영화 정보 (booking / recommend 호출)
     * 래퍼 없이 MovieResponse를 그대로 반환한다.
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<MovieDto.MovieResponse> getMovieInternal(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovie(id));
    }

    /** POST /api/movies/internal/{id}/booking-count - 예매 수 증가 (booking-service 호출) */
    @PostMapping("/internal/{id}/booking-count")
    public ResponseEntity<Void> increaseBookingCount(@PathVariable Long id) {
        movieService.increaseBookingCount(id);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: SecurityConfig 주석 블록의 경로 갱신**

Modify `movie-service/src/main/java/com/lecture/movie/config/SecurityConfig.java` — 활성 코드(`anyRequest().permitAll()`)는 그대로 두고, 주석 처리된 블록 안의 경로 문자열만 바꾼다:

```
/api/courses            -> /api/movies
/api/courses/**         -> /api/movies/**
/api/courses/internal/** -> /api/movies/internal/**
```

`ROLE_INSTRUCTOR`로 `POST /api/courses`를 제한하던 줄은 삭제한다 — 영화 등록 엔드포인트가 없어졌다.

- [ ] **Step 3: 전체 단위 테스트 통과 확인**

Run:
```bash
cd movie-service && ./gradlew test \
  --tests "com.lecture.movie.entity.*" \
  --tests "com.lecture.movie.external.*" \
  --tests "com.lecture.movie.service.*" --no-daemon
```
Expected: PASS (17 tests = Genre 4 + KobisClient 4 + TmdbClient 3 + BoxofficeSync 3 + MovieService 3)

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "feat(movie): MovieController 추가 및 보안 경로 갱신"
```

---

## Task 13: BookingDto

**Files:**
- Create: `booking-service/src/main/java/com/lecture/booking/dto/BookingDto.java`

**Interfaces:**
- Consumes: `Booking` (Task 5)
- Produces:
  - `BookingDto.BookRequest`: `getMovieId()`, `getQuantity()`
  - `BookingDto.MovieSummary`: `id, title, genre, posterUrl, price, openDt`
  - `BookingDto.BookingResponse.from(Booking)` / `.from(Booking, MovieSummary)`
  - `BookingDto.BookingHistoryResponse`: `getUserId()`, `getMovieIds()`
  - `BookingDto.ApiResponse.success(T)` / `.error(String)`

- [ ] **Step 1: 작성**

Create `booking-service/src/main/java/com/lecture/booking/dto/BookingDto.java`:

```java
package com.lecture.booking.dto;

import com.lecture.booking.entity.Booking;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingDto {

    /** 예매 요청 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookRequest {

        @NotNull(message = "영화 ID는 필수입니다")
        private Long movieId;

        @Min(value = 1, message = "예매 매수는 1 이상이어야 합니다")
        @Builder.Default
        private Integer quantity = 1;
    }

    /** 예매 목록 표시용 영화 요약 (movie-service에서 조회) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovieSummary {
        private Long id;
        private String title;
        private String genre;
        private String posterUrl;
        private BigDecimal price;
        private String openDt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingResponse {
        private Long id;
        private Long userId;
        private Long movieId;
        private Integer quantity;
        private BigDecimal amount;
        private Booking.Status status;
        private LocalDateTime createdAt;
        private MovieSummary movie;

        public static BookingResponse from(Booking booking) {
            return BookingResponse.builder()
                    .id(booking.getId())
                    .userId(booking.getUserId())
                    .movieId(booking.getMovieId())
                    .quantity(booking.getQuantity())
                    .amount(booking.getAmount())
                    .status(booking.getStatus())
                    .createdAt(booking.getCreatedAt())
                    .build();
        }

        public static BookingResponse from(Booking booking, MovieSummary movie) {
            return BookingResponse.builder()
                    .id(booking.getId())
                    .userId(booking.getUserId())
                    .movieId(booking.getMovieId())
                    .quantity(booking.getQuantity())
                    .amount(booking.getAmount())
                    .status(booking.getStatus())
                    .createdAt(booking.getCreatedAt())
                    .movie(movie)
                    .build();
        }
    }

    /** 추천 서비스용: 확정된 예매의 영화 ID 목록 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingHistoryResponse {
        private Long userId;
        private List<Long> movieIds;
    }

    /** 공통 API 응답 래퍼 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `cd booking-service && ./gradlew compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add -A
git commit -m "feat(booking): BookingDto 추가"
```

---

## Task 14: MovieServiceClient + PaymentServiceClient

**Files:**
- Create: `booking-service/src/main/java/com/lecture/booking/service/MovieServiceClient.java`
- Create: `booking-service/src/main/java/com/lecture/booking/service/PaymentServiceClient.java`

**Interfaces:**
- Produces:
  - `MovieServiceClient.existsMovie(Long) -> boolean`
  - `MovieServiceClient.getMovie(Long) -> Map<String, Object>`
  - `MovieServiceClient.getPrice(Long) -> BigDecimal` (조회 실패 시 `Movie.DEFAULT_PRICE`와 같은 14000)
  - `MovieServiceClient.increaseBookingCount(Long)`
  - `PaymentServiceClient.requestPayment(Long userId, Long bookingId, Long movieId, BigDecimal amount) -> PaymentResult`
  - `PaymentServiceClient.PaymentResult`: `getPaymentId()`, `getStatus()`

- [ ] **Step 1: MovieServiceClient 작성**

Create `booking-service/src/main/java/com/lecture/booking/service/MovieServiceClient.java`:

```java
package com.lecture.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieServiceClient {

    /** movie-service가 price를 주지 못할 때 쓰는 폴백 티켓 단가 */
    private static final BigDecimal FALLBACK_PRICE = new BigDecimal("14000.00");

    private final WebClient.Builder webClientBuilder;

    /** 영화 존재 여부 확인 (동기 REST) */
    public boolean existsMovie(Long movieId) {
        try {
            Boolean exists = webClientBuilder.build()
                    .get()
                    .uri("http://movie-service/api/movies/internal/exists/{id}", movieId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("[MovieServiceClient] 영화 존재 확인 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
            throw new RuntimeException("Movie Service 연결 실패");
        }
    }

    /**
     * 영화 상세 조회.
     * movie-service는 /internal/{id}를 래퍼 없이 반환하지만,
     * 게이트웨이 경유 등으로 래퍼가 씌워질 수 있어 둘 다 처리한다.
     */
    public Map<String, Object> getMovie(Long movieId) {
        try {
            Map<String, Object> body = webClientBuilder.build()
                    .get()
                    .uri("http://movie-service/api/movies/internal/{id}", movieId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (body == null) {
                throw new RuntimeException("Movie Service 응답 본문이 비어 있습니다.");
            }

            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> movieMap = (Map<String, Object>) dataMap;
                return movieMap;
            }
            return body;

        } catch (Exception e) {
            log.error("[MovieServiceClient] 영화 상세 조회 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
            throw new RuntimeException("Movie Service 영화 상세 조회 실패");
        }
    }

    /** 티켓 단가 조회 - 예매 금액 계산에 사용 */
    public BigDecimal getPrice(Long movieId) {
        Object price = getMovie(movieId).get("price");
        if (price instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (price != null) {
            try {
                return new BigDecimal(price.toString());
            } catch (NumberFormatException ignored) {
                // 아래 폴백으로 떨어진다
            }
        }
        log.warn("[MovieServiceClient] price 없음, 폴백 사용 - movieId: {}", movieId);
        return FALLBACK_PRICE;
    }

    /** 예매 확정 시 예매 수 증가 */
    public void increaseBookingCount(Long movieId) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri("http://movie-service/api/movies/internal/{id}/booking-count", movieId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("[MovieServiceClient] 예매 수 증가 완료 - movieId: {}", movieId);
        } catch (Exception e) {
            // 카운터 증가 실패가 예매 확정을 막아서는 안 된다
            log.error("[MovieServiceClient] 예매 수 증가 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: PaymentServiceClient 작성**

Create `booking-service/src/main/java/com/lecture/booking/service/PaymentServiceClient.java`:

```java
package com.lecture.booking.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final WebClient.Builder webClientBuilder;

    /**
     * 결제 요청 (동기 REST).
     * bookingId 를 반드시 함께 보낸다 - payment.completed 이벤트가 이 값으로 예매를 특정한다.
     */
    public PaymentResult requestPayment(Long userId, Long bookingId, Long movieId, BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest(userId, bookingId, movieId, amount);

            PaymentResult result = webClientBuilder.build()
                    .post()
                    .uri("http://payment-service:8084/api/payments/internal/request")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResult.class)
                    .block();

            log.info("[PaymentServiceClient] 결제 요청 완료 - bookingId: {}, movieId: {}, result: {}",
                    bookingId, movieId, result != null ? result.getStatus() : "null");

            return result;
        } catch (Exception e) {
            log.error("[PaymentServiceClient] 결제 요청 실패 - bookingId: {}, error: {}",
                    bookingId, e.getMessage(), e);
            throw new RuntimeException("Payment Service 연결 실패");
        }
    }

    @Getter
    @NoArgsConstructor
    static class PaymentRequest {
        private Long userId;
        private Long bookingId;
        private Long movieId;
        private BigDecimal amount;

        PaymentRequest(Long userId, Long bookingId, Long movieId, BigDecimal amount) {
            this.userId = userId;
            this.bookingId = bookingId;
            this.movieId = movieId;
            this.amount = amount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PaymentResult {
        private Long paymentId;
        private String status; // COMPLETED / FAILED
    }
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `cd booking-service && ./gradlew compileJava --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "feat(booking): movie/payment 서비스 클라이언트 추가 (bookingId 계약 반영)"
```

---

## Task 15: BookingWriteService + BookingService

**Files:**
- Create: `booking-service/src/main/java/com/lecture/booking/service/BookingWriteService.java`
- Create: `booking-service/src/main/java/com/lecture/booking/kafka/KafkaEvent.java`
- Create: `booking-service/src/main/java/com/lecture/booking/kafka/BookingKafkaProducer.java`
- Create: `booking-service/src/main/java/com/lecture/booking/service/BookingService.java`
- Create: `booking-service/src/test/java/com/lecture/booking/service/BookingServiceTest.java`

**Interfaces:**
- Consumes: `BookingRepository` (Task 5), `BookingDto` (Task 13), `MovieServiceClient`, `PaymentServiceClient` (Task 14)
- Produces:
  - `BookingWriteService.createPendingBooking(Long userId, Long movieId, Integer quantity, BigDecimal amount) -> Booking`
  - `BookingService.book(Long userId, BookingDto.BookRequest) -> BookingDto.BookingResponse`
  - `BookingService.confirmBooking(Long bookingId)` ← Kafka consumer가 호출
  - `BookingService.getBookingsByUser(Long userId) -> List<BookingDto.BookingResponse>`
  - `BookingService.getBookingHistory(Long userId) -> BookingDto.BookingHistoryResponse`
  - `KafkaEvent.BookingCompletedEvent`: `bookingId`, `userId`, `movieId`, `genre`
  - `BookingKafkaProducer.publishBookingCompleted(KafkaEvent.BookingCompletedEvent)`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `booking-service/src/test/java/com/lecture/booking/service/BookingServiceTest.java`:

```java
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

    @Test
    void 예매금액은_단가에_매수를_곱한_값이다() {
        when(movieServiceClient.existsMovie(1L)).thenReturn(true);
        when(movieServiceClient.getPrice(1L)).thenReturn(new BigDecimal("14000.00"));
        when(writeService.createPendingBooking(eq(1L), eq(1L), eq(3), any()))
                .thenReturn(Booking.builder()
                        .id(100L).userId(1L).movieId(1L).quantity(3)
                        .amount(new BigDecimal("42000.00"))
                        .build());

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

        BookingDto.BookRequest request =
                BookingDto.BookRequest.builder().movieId(1L).quantity(1).build();

        assertEquals(101L, service.book(1L, request).getId());
        assertEquals(102L, service.book(1L, request).getId());
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
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cd booking-service && ./gradlew test --tests "com.lecture.booking.service.BookingServiceTest" --no-daemon`
Expected: 컴파일 실패 — `cannot find symbol: class BookingService`

- [ ] **Step 3: BookingWriteService 작성**

Create `booking-service/src/main/java/com/lecture/booking/service/BookingWriteService.java`:

```java
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
```

- [ ] **Step 4: KafkaEvent 작성**

Create `booking-service/src/main/java/com/lecture/booking/kafka/KafkaEvent.java`:

```java
package com.lecture.booking.kafka;

import lombok.*;

public class KafkaEvent {

    /**
     * Payment Service → Booking Service
     * 결제 완료 이벤트. bookingId 로 예매를 특정한다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentCompletedEvent {
        private Long paymentId;
        private Long bookingId;
        private Long userId;
        private Long movieId;
        private String status; // COMPLETED
    }

    /**
     * Booking Service → Recommend Service
     * 예매 확정 이벤트. genre 를 실어 추천 서비스의 재조회를 줄인다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingCompletedEvent {
        private Long bookingId;
        private Long userId;
        private Long movieId;
        private String genre;
    }
}
```

- [ ] **Step 5: BookingKafkaProducer 작성**

Create `booking-service/src/main/java/com/lecture/booking/kafka/BookingKafkaProducer.java`:

```java
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
```

- [ ] **Step 6: BookingService 작성**

Create `booking-service/src/main/java/com/lecture/booking/service/BookingService.java`:

```java
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
     * 예매 흐름
     * 1. 영화 존재 확인 (동기)
     * 2. 티켓 단가 조회 후 금액 계산
     * 3. PENDING 예매 생성 및 즉시 커밋
     * 4. 결제 요청 (동기) - bookingId 전달
     *
     * 중복 예매 검사는 하지 않는다. 같은 영화를 여러 번 예매할 수 있다.
     */
    public BookingDto.BookingResponse book(Long userId, BookingDto.BookRequest request) {
        Long movieId = request.getMovieId();
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();

        if (!movieServiceClient.existsMovie(movieId)) {
            throw new IllegalArgumentException("존재하지 않는 영화입니다: " + movieId);
        }

        BigDecimal amount = movieServiceClient.getPrice(movieId)
                .multiply(BigDecimal.valueOf(quantity));

        Booking booking = bookingWriteService.createPendingBooking(userId, movieId, quantity, amount);

        paymentServiceClient.requestPayment(userId, booking.getId(), movieId, amount);

        log.info("[BookingService] 예매 접수 (결제 대기) - bookingId: {}", booking.getId());
        return BookingDto.BookingResponse.from(booking);
    }

    /**
     * 예매 확정 - payment.completed 수신 시 호출된다.
     * bookingId 로 특정하므로 같은 영화의 다른 예매 건에 영향을 주지 않는다.
     */
    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "예매 정보를 찾을 수 없습니다 - bookingId: " + bookingId));

        booking.confirm();

        movieServiceClient.increaseBookingCount(booking.getMovieId());

        String genre = resolveGenre(booking.getMovieId());

        kafkaProducer.publishBookingCompleted(
                KafkaEvent.BookingCompletedEvent.builder()
                        .bookingId(booking.getId())
                        .userId(booking.getUserId())
                        .movieId(booking.getMovieId())
                        .genre(genre)
                        .build()
        );

        log.info("[BookingService] 예매 확정 완료 - bookingId: {}", booking.getId());
    }

    /** 사용자 예매 목록 - movie-service에서 영화 정보를 붙여 반환 */
    public List<BookingDto.BookingResponse> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(booking -> BookingDto.BookingResponse.from(
                        booking, toMovieSummary(booking.getMovieId())))
                .collect(Collectors.toList());
    }

    /** 추천 서비스용: 확정된 예매의 영화 ID 목록 */
    public BookingDto.BookingHistoryResponse getBookingHistory(Long userId) {
        List<Long> movieIds = bookingRepository
                .findByUserIdAndStatus(userId, Booking.Status.CONFIRMED)
                .stream()
                .map(Booking::getMovieId)
                .distinct()
                .collect(Collectors.toList());

        return BookingDto.BookingHistoryResponse.builder()
                .userId(userId)
                .movieIds(movieIds)
                .build();
    }

    private String resolveGenre(Long movieId) {
        try {
            Object genre = movieServiceClient.getMovie(movieId).get("genre");
            return genre != null ? genre.toString() : null;
        } catch (Exception e) {
            log.warn("[BookingService] 장르 조회 실패 - movieId: {}, error: {}", movieId, e.getMessage());
            return null;
        }
    }

    private BookingDto.MovieSummary toMovieSummary(Long movieId) {
        try {
            Map<String, Object> movie = movieServiceClient.getMovie(movieId);
            return BookingDto.MovieSummary.builder()
                    .id(toLong(movie.get("id")))
                    .title(toStringOrNull(movie.get("title")))
                    .genre(toStringOrNull(movie.get("genre")))
                    .posterUrl(toStringOrNull(movie.get("posterUrl")))
                    .price(toBigDecimal(movie.get("price")))
                    .openDt(toStringOrNull(movie.get("openDt")))
                    .build();
        } catch (Exception e) {
            log.warn("[BookingService] 영화 요약 조회 실패 - movieId: {}, error: {}",
                    movieId, e.getMessage());
            return null;
        }
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `cd booking-service && ./gradlew test --tests "com.lecture.booking.service.BookingServiceTest" --no-daemon`
Expected: PASS (4 tests)

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "feat(booking): 예매 오케스트레이션 서비스 추가 (bookingId 기반 확정)"
```

---

## Task 16: BookingKafkaConsumer + BookingController

**Files:**
- Create: `booking-service/src/main/java/com/lecture/booking/kafka/BookingKafkaConsumer.java`
- Create: `booking-service/src/test/java/com/lecture/booking/kafka/BookingKafkaConsumerTest.java`
- Create: `booking-service/src/main/java/com/lecture/booking/controller/BookingController.java`

**Interfaces:**
- Consumes: `BookingService.confirmBooking(Long)` (Task 15)
- Produces:
  - `POST /api/bookings` → `ApiResponse<BookingResponse>`, 201
  - `GET /api/bookings/my` → `ApiResponse<List<BookingResponse>>`
  - `GET /api/bookings/user/{userId}` → `ApiResponse<List<BookingResponse>>`
  - `GET /api/bookings/internal/history/{userId}` → `BookingHistoryResponse` (래퍼 없음)

- [ ] **Step 1: 실패하는 테스트 작성**

Create `booking-service/src/test/java/com/lecture/booking/kafka/BookingKafkaConsumerTest.java`:

```java
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
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cd booking-service && ./gradlew test --tests "com.lecture.booking.kafka.BookingKafkaConsumerTest" --no-daemon`
Expected: 컴파일 실패 — `cannot find symbol: class BookingKafkaConsumer`

- [ ] **Step 3: Consumer 구현**

Create `booking-service/src/main/java/com/lecture/booking/kafka/BookingKafkaConsumer.java`:

```java
package com.lecture.booking.kafka;

import com.lecture.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingKafkaConsumer {

    private final BookingService bookingService;

    /**
     * payment.completed 수신 → 예매 PENDING → CONFIRMED
     *
     * payment-service는 JsonSerializer에 type header를 싣지 않으므로
     * 특정 DTO가 아니라 Map으로 받아 직접 파싱한다.
     */
    @KafkaListener(
            topics = "${kafka.topic.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info("[Kafka Consumer] payment.completed 수신: {}", event);

        try {
            Object bookingIdValue = event.get("bookingId");

            if (bookingIdValue == null) {
                throw new IllegalArgumentException(
                        "payment.completed 이벤트에 bookingId가 없습니다. payment-service 계약을 확인하세요.");
            }

            Long bookingId = ((Number) bookingIdValue).longValue();
            bookingService.confirmBooking(bookingId);

            log.info("[Kafka Consumer] 예매 확정 완료 - bookingId: {}", bookingId);

        } catch (Exception e) {
            // 예외를 삼켜 리스너 컨테이너가 계속 살아 있게 한다
            log.error("[Kafka Consumer] 예매 확정 실패 - event: {}, error: {}",
                    event, e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd booking-service && ./gradlew test --tests "com.lecture.booking.kafka.BookingKafkaConsumerTest" --no-daemon`
Expected: PASS (3 tests)

- [ ] **Step 5: Controller 작성**

Create `booking-service/src/main/java/com/lecture/booking/controller/BookingController.java`:

```java
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
```

- [ ] **Step 6: booking-service 전체 단위 테스트 통과 확인**

Run:
```bash
cd booking-service && ./gradlew test \
  --tests "com.lecture.booking.service.*" \
  --tests "com.lecture.booking.kafka.*" --no-daemon
```
Expected: PASS (7 tests)

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat(booking): Kafka consumer와 BookingController 추가"
```

---

## Task 17: docker-compose 갱신 및 기동 검증

**Files:**
- Modify: `docker-compose.yml`

**Interfaces:**
- Consumes: 모든 이전 task

- [ ] **Step 1: docker-compose.yml에서 course-service 블록을 movie-service로 교체**

```yaml
  movie-service:
    build:
      context: ./movie-service
      dockerfile: Dockerfile
    container_name: lecture-movie
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:mariadb://lecturedb:3306/lecture_db
      - SPRING_DATASOURCE_USERNAME=manager
      - SPRING_DATASOURCE_PASSWORD=SqlDba-1
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://auth-server:9000/oauth2/jwks
      - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-server:9000
      - KOBIS_API_KEY=${KOBIS_API_KEY}
      - TMDB_API_KEY=${TMDB_API_KEY}
    depends_on:
      mariadb:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      auth-server:
        condition: service_healthy
    networks:
      - lecture-net
```

- [ ] **Step 2: enrollment-service 블록을 booking-service로 교체**

```yaml
  booking-service:
    build:
      context: ./booking-service
      dockerfile: Dockerfile
    container_name: lecture-booking
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:mariadb://lecturedb:3306/lecture_db
      - SPRING_DATASOURCE_USERNAME=manager
      - SPRING_DATASOURCE_PASSWORD=SqlDba-1
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://auth-server:9000/oauth2/jwks
      - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-server:9000
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      mariadb:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      auth-server:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - lecture-net
```

- [ ] **Step 3: recommend-service의 의존 서비스명 갱신**

`recommend-service` 블록에서:
```
      - ENROLLMENT_SERVICE_URL=http://booking-service:8083
      - MOVIE_SERVICE_URL=http://movie-service:8082
```
`depends_on`의 `enrollment-service`/`course-service`도 `booking-service`/`movie-service`로 바꾼다. (recommend-service 내부 코드는 담당자가 별도로 수정한다.)

- [ ] **Step 4: API 키를 .env로 주입하고 커밋에서 제외**

```bash
cat > .env <<'EOF'
KOBIS_API_KEY=여기에_KOBIS_키
TMDB_API_KEY=여기에_TMDB_키
EOF

printf '\n.env\n' >> .gitignore
```

`movie_api.py`에 하드코딩된 키를 여기에 옮겨 넣는다. **`.env`는 절대 커밋하지 않는다.**

- [ ] **Step 5: 전체 기동**

```bash
docker compose down -v
docker compose up -d --build mariadb kafka eureka-server auth-server movie-service booking-service
docker compose logs -f movie-service
```
Expected: `Started MovieServiceApplication` 로그, Eureka에 `MOVIE-SERVICE` 등록

- [ ] **Step 6: 박스오피스 수집 확인**

```bash
curl -s "http://localhost:8082/api/movies/boxoffice?type=WEEKLY" | head -c 2000
```
Expected: `success: true`, `items` 배열에 `rankNo`, `movie.title`, `movie.genre`, `movie.posterUrl`이 채워져 있다.

```bash
docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db \
  -e "SELECT movie_cd, title, genre, tmdb_id FROM movies LIMIT 10;"
docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db \
  -e "SELECT rank_type, target_date, rank_no, movie_id FROM boxoffice_rankings ORDER BY rank_no LIMIT 10;"
```
Expected: 영화 행에 `genre`가 `OTHER`가 아닌 값으로 채워진 행이 다수 존재한다. 전부 `OTHER`라면 `TMDB_API_KEY`가 주입되지 않았거나 제목 매칭이 실패한 것이다 — movie-service 로그의 `[TmdbClient] 검색 결과 없음`을 확인한다.

- [ ] **Step 7: 두 번째 호출이 오픈API를 타지 않는지 확인**

```bash
curl -s "http://localhost:8082/api/movies/boxoffice?type=WEEKLY" > /dev/null
docker compose logs --tail=50 movie-service | grep "스냅샷 없음"
```
Expected: 두 번째 호출에서는 `스냅샷 없음` 로그가 추가로 찍히지 않는다.

- [ ] **Step 8: 커밋**

```bash
git add docker-compose.yml .gitignore
git commit -m "chore: movie-service/booking-service로 compose 갱신 및 API 키 환경변수화"
```

---

## 완료 후 남는 작업 (다른 담당자)

이 계획을 모두 마쳐도 아래는 열려 있다. Task 0에서 공유한 계약 변경이 실제로 반영되어야 예매 흐름이 끝까지 동작한다.

1. **payment-service**: `InternalPaymentRequest`에 `bookingId` 추가, `payments` 테이블에 `booking_id`/`movie_id` 반영, `payment.completed`에 `bookingId` 포함
2. **recommend-service**: `booking.completed` 토픽 구독, `/internal/history/{userId}` 응답 필드 `activeCourseIds` → `movieIds`, 장르→음식 매핑 로직
3. **api-gateway**: 라우트 경로 변경 (이미지 재빌드 필요)
4. **vue-frontend**: `/api/courses` → `/api/movies`, `/api/enrollments` → `/api/bookings`, 홈화면 박스오피스 렌더링, 음식 추천 팝업
5. **예매 취소** `DELETE /api/bookings/{id}`: 구현 여부 미결정 (스펙 13장)
