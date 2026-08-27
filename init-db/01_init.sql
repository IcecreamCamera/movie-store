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
