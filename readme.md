# movie-store

영화 예매 MSA 데모 플랫폼. KOBIS 박스오피스와 TMDB 메타데이터를 수집해 홈화면에 뿌리고, 예매하면 결제·확정까지 이벤트 체인으로 이어진다.

> The following practice code is intended for educational purposes only. For contact : audit@korea.ac.kr, Sungryel Lim Ph.D
>
> This practice code is not a completed commercial version but has been developed for educational purposes; supplementation is required depending on the deployment objective for use as a commercial service.

---

## 시스템 구성

```
                        [Vue Frontend :3000]
                                 │  Bearer JWT
                                 ▼
                      [API Gateway :8080]
                   JWT 검증 → X-User-Id 헤더 주입
                     Eureka 로 서비스 탐색
                                 │
     ┌───────────┬───────────────┼───────────────┬───────────────┐
     ▼           ▼               ▼               ▼               ▼
[user :8081] [movie :8082] [booking :8083] [payment :8084] [recommend :8085]
     │           │               │               │
     │           │  KOBIS +      │               │
     │           │  TMDB 수집     │               │
     └───────────┴──[MariaDB lecture_db :3379]───┘
                                 │
                        [Kafka :9092]
                  payment.completed / booking.completed
                                 │
                    [Eureka :8761]  [Auth Server :9000]
```

| 서비스 | 컨테이너 | 포트 | 빌드 |
|---|---|---|---|
| API Gateway | `lecture-gateway` | 8080 | 소스 빌드 |
| User Service | `lecture-user` | 8081 | 소스 빌드 |
| Movie Service | `lecture-movie` | 8082 | 소스 빌드 |
| Booking Service | `lecture-booking` | 8083 | 소스 빌드 |
| Payment Service | `lecture-payment` | 8084 | 소스 빌드 |
| Recommend Service (FastAPI) | `lecture-recommend` | 8085 | 소스 빌드 |
| Eureka Server | `lecture-eureka` | 8761 | 소스 빌드 |
| **Auth Server** | `lecture-auth` | 9000 | **사전 빌드 이미지** |
| MariaDB | `lecturedb` | 3379 → 3306 | 공식 이미지 |
| Kafka (KRaft) | `lecture-kafka` | 9092 | 공식 이미지 |
| Frontend | `lecture-frontend` | 3000 → 80 | 소스 빌드 |

`auth-server`만 저장소에 소스가 없다. 나머지는 전부 소스에서 빌드된다.

---

## 데이터 모델

```
users ──┬─< bookings >─── movies ──< boxoffice_rankings
        │       │            │
        └─────< payments >───┘
```

| 테이블 | 설명 |
|---|---|
| `users` | 회원 |
| `movies` | KOBIS 영화코드(`movie_cd`) 기준 캐시. TMDB에서 장르·포스터·줄거리 보강 |
| `boxoffice_rankings` | 일간/주간 순위 스냅샷. `UNIQUE(rank_type, target_date, rank_no)` |
| `bookings` | 예매. **같은 영화를 여러 번 예매할 수 있어 `UNIQUE(user_id, movie_id)`가 없다** |
| `payments` | 결제. `booking_id`가 NOT NULL + UNIQUE — 예매 1건당 결제 1건 |

`payments.booking_id`가 이 설계의 핵심이다. 구 강의 플랫폼은 `UNIQUE(user_id, course_id)` 덕분에 결제를 `(userId, courseId)`로 특정할 수 있었지만, 영화는 재예매가 가능해 그 제약을 없앴다. `booking_id` 없이는 같은 영화를 두 번 예매했을 때 두 번째 결제가 첫 번째 예매를 확정시킨다.

---

## 시작하기

### 1. API 키 준비

`movie-service`가 KOBIS와 TMDB를 호출한다. 프로젝트 루트에 `.env`를 만든다 (`.gitignore` 대상이라 커밋되지 않는다).

```bash
cat > .env <<'EOF'
KOBIS_API_KEY=발급받은_KOBIS_키
TMDB_API_KEY=발급받은_TMDB_키
EOF
```

- KOBIS: https://www.kobis.or.kr/kobisopenapi/
- TMDB: https://www.themoviedb.org/settings/api

키가 없어도 스택은 뜬다. 박스오피스 조회 시점에만 실패한다.

### 2. Auth Server 이미지 로드

```bash
docker load -i infra-images.tar
docker images | grep msa-lecture   # msa-lecture/auth-server:1.0 확인
```

### 3. 기동

```bash
docker compose up -d --build
```

> **⚠️ 기존 볼륨이 있다면 반드시 `docker compose down -v` 를 먼저 실행할 것**
>
> `ddl-auto: update` 는 컬럼을 **추가만 하고 지우지 않는다.** 구 도메인(강의) 시절의 볼륨을 그대로 쓰면 `payments` 테이블에 `course_id BIGINT NOT NULL` 이 남아 있어 결제 insert 가 다음 오류로 실패한다.
>
> ```
> Field 'course_id' doesn't have a default value
> ```
>
> 이미 그 상태라면: `docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db -e "ALTER TABLE payments DROP COLUMN course_id;"`

기동 순서는 `depends_on` 으로 강제된다:

```
MariaDB / Kafka → Eureka → Auth Server → Gateway + 서비스들 → Recommend → Frontend
```

### 4. 확인

```bash
docker compose ps                 # 전체 상태
open http://localhost:8761        # Eureka 등록 현황
curl http://localhost:8080/actuator/health
```

---

## 동작 확인

게이트웨이(8080)를 거치는 대부분의 경로는 JWT 가 필요하다. 토큰 없이 확인하려면 서비스 포트로 직접 호출한다.

```bash
# 박스오피스 수집 (첫 호출에서 KOBIS + TMDB 를 실제로 호출한다)
curl -s "http://localhost:8082/api/movies/boxoffice?type=DAILY" | jq

# 회원 생성 — bookings.user_id 가 FK 라 사용자가 먼저 있어야 한다
curl -s -X POST http://localhost:8081/api/users/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"테스트","email":"test@example.com","password":"password123","role":"STUDENT"}'

# 예매 — movies.id 는 수집할 때마다 새로 발번되므로 실제 값을 조회해서 쓴다
MID=$(docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db -N \
  -e "SELECT id FROM movies ORDER BY id LIMIT 1;" | tr -d '[:space:]')
UID_=$(docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db -N \
  -e "SELECT id FROM users LIMIT 1;" | tr -d '[:space:]')

curl -s -X POST http://localhost:8083/api/bookings \
  -H 'Content-Type: application/json' -H "X-User-Id: $UID_" \
  -d "{\"movieId\": $MID, \"quantity\": 2}"

# 결과 확인 — CONFIRMED / COMPLETED 가 되어야 정상
docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db -e \
  "SELECT b.id, b.status, p.status AS payment, m.booking_count
   FROM bookings b LEFT JOIN payments p ON p.booking_id=b.id
   LEFT JOIN movies m ON m.id=b.movie_id ORDER BY b.id DESC LIMIT 3;"
```

---

## 주요 흐름

### 박스오피스 (lazy 캐시)

```
GET /api/movies/boxoffice?type=DAILY|WEEKLY
  └ boxoffice_rankings 에 해당 기준일 스냅샷이 있으면 그대로 반환 (오픈API 호출 없음)
    └ 없으면 KOBIS 조회 → movies upsert → TMDB 보강 → 스냅샷 저장
```

- `targetDt` 는 항상 집계가 끝난 과거 날짜다. 일간은 어제, 주간은 직전 일요일.
- TMDB 보강은 `tmdb_id` 가 아직 없는 영화에만 1회 수행한다. 같은 영화가 며칠간 순위에 머물러도 TMDB 호출은 영화당 한 번이다.
- KOBIS 는 장르를 주지 않아 TMDB 가 필수다. 음식 추천이 장르에 의존한다.

### 예매

```
POST /api/bookings { movieId, quantity }
  1) movie-service : 영화 존재 확인 + 티켓 단가 조회
  2) bookings INSERT (PENDING, amount = price × quantity) — 별도 트랜잭션으로 즉시 커밋
  3) payment-service : /internal/request { userId, bookingId, movieId, amount }
  4) payments INSERT → 모의 승인 → COMPLETED
  5) Kafka  payment.completed { paymentId, bookingId, userId, movieId }
  6) booking-service 소비 → bookings.status = CONFIRMED
                          → movies.booking_count 원자적 증가
  7) Kafka  booking.completed { bookingId, userId, movieId, genre }
```

2번이 별도 트랜잭션인 이유: 결제가 빠르면 `payment.completed` 가 바깥 트랜잭션 커밋보다 먼저 도착할 수 있고, 그때 booking 행이 없으면 확정에 실패한다.

결제 요청이 실패하거나 거절되면 PENDING 예매는 자동으로 CANCELLED 된다.

---

## Kafka 토픽

| 토픽 | 발행 | 소비 | 페이로드 |
|---|---|---|---|
| `payment.completed` | payment-service | booking-service | `paymentId, bookingId, userId, movieId, status` |
| `booking.completed` | booking-service | recommend-service | `bookingId, userId, movieId, genre` |

---

## 인증

```
1. 프론트 → GET /oauth2/authorize (client_id, redirect_uri, scope)
2. auth-server 로그인 폼 → 로그인
3. 302 → {redirect_uri}/callback?code=...
4. 프론트 → POST /oauth2/token (Basic web-client:web-secret, grant_type=authorization_code)
5. access_token(JWT) 수신
6. 이후 요청에 Bearer 토큰 → Gateway 가 검증 후 X-User-Id / X-User-Email / X-User-Role 주입
```

각 서비스는 자체 인증 로직 없이 `@RequestHeader("X-User-Id")` 로 사용자를 식별한다. 게이트웨이는 이 헤더들을 **덮어쓴다** — 클라이언트가 직접 보낸 값은 무시된다.

게이트웨이 공개 경로: `/oauth2/**`, `/login`, `/logout`, `/actuator/**`, `/api-docs/**`, `/swagger-ui/**`, `/api/users/register`, `/api/users/login`

---

## 로그

```bash
docker compose logs -f                      # 전체
docker compose logs -f movie-service        # 개별
```

서비스명: `mariadb` `kafka` `eureka-server` `auth-server` `api-gateway` `user-service` `movie-service` `booking-service` `payment-service` `recommend-service` `frontend-web`

---

## 프론트엔드

컨테이너로 이미 3000 포트에 뜬다. 로컬에서 직접 돌리려면:

```bash
cd vue_frontend_demo/vue-frontend   # compose 가 빌드하는 실제 프론트엔드
npm install
npm run dev
```

브라우저: http://localhost:3000

---

## 종료

```bash
docker compose down        # 컨테이너만
docker compose down -v     # 볼륨(DB 데이터)까지 — 스키마가 바뀐 뒤에는 이쪽
```

---

## 트러블슈팅

**컨테이너 이름 충돌** (`The container name "/lecture-kafka" is already in use`)
다른 compose 프로젝트가 같은 `container_name` 을 쓰고 있다. 정지된 잔여 컨테이너를 지운다:
```bash
docker rm lecture-kafka lecture-eureka lecture-auth   # 필요한 것만
```

**빌드 중 `429 Too Many Requests`**
Maven Central 이 rate limit 을 건 상태다. 각 모듈의 `build.gradle` 과 `settings.gradle` 에 Google 이 운영하는 Maven Central 공식 미러가 이미 설정되어 있으므로 대개 그대로 통과한다. 그래도 실패하면 잠시 후 재시도한다.

**박스오피스가 비어 있음**
`.env` 의 `KOBIS_API_KEY` / `TMDB_API_KEY` 를 확인한다. `movies` 에 행은 있는데 `genre` 가 전부 `OTHER` 면 TMDB 키 문제이거나 제목 매칭 실패다:
```bash
docker compose logs movie-service | grep TmdbClient
```

**예매가 PENDING 에서 멈춤**
`payment.completed` 가 도착하지 않은 것이다. Kafka 와 payment-service 로그를 본다:
```bash
docker compose logs payment-service | grep "payment.completed"
docker compose logs booking-service | grep -E "payment.completed|예매 확정"
```

---

## 부록 — 추가·수정 파일 맵

강의 원본 템플릿(`course-service` / `enrollment-service` 도메인, 커밋 `c780ba5`) 대비 이 저장소에서 실제로 손댄 파일이다. 동일한 목록은 아래 명령으로 다시 뽑을 수 있다.

```bash
git diff --name-status c780ba5 HEAD     # 파일별 상태
git diff --stat c780ba5 HEAD | tail -1  # 187 files changed, 8284 insertions(+), 1895 deletions(-)
```

구분 표기: **신규** 새로 만든 파일 · **수정** 기존 파일 변경 · **이동** 도메인 리네임으로 경로가 바뀐 파일(내용도 함께 수정됨) · **삭제** 구 도메인 잔재

### 디렉토리 요약

| 디렉토리 | 상태 | 무엇을 했나 |
|---|---|---|
| `api-gateway/` | 신규 서비스 | JWT 검증 + 라우팅 게이트웨이를 새로 작성 (원본에 없던 모듈) |
| `movie-service/` | `course-service/` 리네임 + 대폭 확장 | KOBIS/TMDB 연동, 박스오피스 lazy 캐시 |
| `booking-service/` | `enrollment-service/` 리네임 + 재작성 | 예매 오케스트레이션, Kafka 확정 처리 |
| `payment-service/` | 수정 | 결제 키를 `courseId` → `bookingId` + `movieId` 로 교체 |
| `recommend-service/` | 대폭 수정 | 수강 추천 → 영화 장르 기반 간식 추천(LLM) |
| `user-service/`, `eureka-server/` | 빌드 설정만 수정 | Maven Central 미러 추가 |
| `init-db/` | 수정 | 영화 예매 도메인 DDL 로 전면 교체 |
| `vue_frontend_demo/vue-frontend/` | 대폭 수정 | 실제 서비스 프론트엔드 (compose 가 빌드하는 대상) |
| `vue-frontend/` | 손대지 않음 | 강의 원본 프론트엔드 사본. 빌드/배포에 쓰이지 않는다 |
| `course-service/`, `enrollment-service/` | 삭제 | 리네임되어 사라진 구 모듈 |

### 루트

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `docker-compose.yml` | 수정 | `course`/`enrollment` 서비스를 `movie`/`booking` 으로 교체, `api-gateway` 추가, KOBIS·TMDB·LLM 키를 `.env` 에서 주입, 프론트 빌드 컨텍스트를 `./vue_frontend_demo/vue-frontend` 로 변경 |
| `init-db/01_init.sql` | 수정 | `courses`/`enrollments` → `movies`/`boxoffice_rankings`/`bookings` 로 교체. `payments` 에 `booking_id`(NOT NULL + UNIQUE)·`movie_id` 추가, `course_id` 제거. `bookings` 는 재예매 허용을 위해 `UNIQUE(user_id, movie_id)` 를 두지 않음 |
| `.gitignore` | 수정 | `.env`(`!.env.example` 예외), Python/Node/Gradle 산출물, `infra-images*.tar` 등 대용량 바이너리, `docs/superpowers/*` 작업 산출물 제외 |
| `package.json` | 신규 | 루트에서 프론트를 실행하는 위임 스크립트 (`dev` → `vue-frontend`, `dev:demo` → `vue_frontend_demo/vue-frontend`) |
| `.claude/launch.json` | 신규 | 프론트 두 벌(3000 / 5173)에 대한 실행 설정 |
| `readme.md` | 수정 | 강의 도메인 → 영화 예매 아키텍처 기준으로 재작성 |

### `api-gateway/` — 신규 서비스

원본 `course-service` 의 Gradle 골격만 복사해 새로 작성했다.

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `src/main/java/com/lecture/gateway/ApiGatewayApplication.java` | 신규 | Spring Cloud Gateway 진입점 |
| `src/main/java/com/lecture/gateway/config/SecurityConfig.java` | 신규 | JWT Resource Server 설정. 공개 경로(`/oauth2/**`, `/login`, 회원가입 등)와 `GET /api/movies/**` 는 permitAll, `/api/*/internal/**` 는 외부에서 denyAll |
| `src/main/java/com/lecture/gateway/filter/JwtAuthenticationFilter.java` | 신규 | JWT 클레임에서 사용자 정보를 뽑아 `X-User-Id` / `X-User-Email` / `X-User-Role` 로 **덮어써서** 주입 (클레임이 없으면 헤더를 제거해 클라이언트 위조 차단) |
| `src/main/java/com/lecture/gateway/filter/LoggingFilter.java` | 신규 | 요청/응답 로깅 글로벌 필터 |
| `src/main/resources/application.yml` | 신규 | 서비스별 라우트 정의, Eureka discovery locator, 루트(`/`) → OAuth2 인가 요청 리다이렉트, `DedupeResponseHeader` 로 CORS 헤더 중복 제거 |
| `build.gradle`, `settings.gradle`, `Dockerfile`, `gradle/**` | 신규/이동 | 게이트웨이 의존성(gateway, oauth2-resource-server, eureka-client)과 빌드 골격 |

### `movie-service/` — `course-service/` 리네임

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `src/main/java/com/lecture/movie/entity/Movie.java` | 신규 | KOBIS `movie_cd` 기준 영화 캐시 엔티티. TMDB 메타(장르·포스터·평점) + `price`, `booking_count` |
| `src/main/java/com/lecture/movie/entity/Genre.java` | 신규 | TMDB genre_id → 내부 장르 enum 매핑 |
| `src/main/java/com/lecture/movie/entity/BoxofficeRanking.java` | 신규 | 일간/주간 순위 스냅샷. `UNIQUE(rank_type, target_date, rank_no)` |
| `src/main/java/com/lecture/movie/external/KobisClient.java` | 신규 | KOBIS 박스오피스 API 클라이언트 |
| `src/main/java/com/lecture/movie/external/TmdbClient.java` | 신규 | TMDB 검색 클라이언트 (장르·포스터·줄거리를 1-call 로 보강) |
| `src/main/java/com/lecture/movie/external/dto/*.java` | 신규 | `KobisBoxofficeItem`, `KobisMovieItem`, `TmdbMovie` 응답 DTO |
| `src/main/java/com/lecture/movie/config/OpenApiProperties.java` | 신규 | KOBIS/TMDB 키·URL 바인딩 |
| `src/main/java/com/lecture/movie/service/BoxofficeSyncService.java` | 신규 | 스냅샷이 없을 때만 KOBIS 조회 → `movies` upsert → TMDB 보강 → 스냅샷 저장 (lazy 캐시) |
| `src/main/java/com/lecture/movie/service/MovieService.java` | 신규 | 목록/검색/장르별 조회, 내부용 존재 확인·예매수 증가 |
| `src/main/java/com/lecture/movie/controller/MovieController.java` | 신규 | `GET /api/movies`, `/boxoffice`, `/search`, `/{id}`, `/genre/{genre}` + 내부용 `/internal/exists/{id}`, `/internal/{id}`, `POST /internal/{id}/booking-count` |
| `src/main/java/com/lecture/movie/dto/MovieDto.java` | 신규 | 응답 래퍼 및 `MovieResponse` / `BoxofficeResponse` |
| `src/main/java/com/lecture/movie/repository/*.java` | 신규 | `MovieRepository`, `BoxofficeRankingRepository` |
| `src/main/java/com/lecture/movie/config/{JpaConfig,SecurityConfig,GlobalExceptionHandler}.java` | 이동 | `course` 패키지에서 이동. SecurityConfig 는 공개 경로 갱신 |
| `src/main/resources/application.yml` | 이동 | 서비스명·포트(8082)·KOBIS/TMDB 설정 반영 |
| `src/test/java/com/lecture/movie/**` | 신규 | `GenreTest`, `KobisClientTest`, `TmdbClientTest`, `BoxofficeSyncServiceTest`, `MovieServiceTest` |
| `build.gradle`, `settings.gradle`, `Dockerfile`, `gradle/**` | 이동/신규 | 모듈명 변경 + Maven Central 미러 추가 |
| `~~course-service/**~~` | 삭제 | `Course`, `CourseController`, `CourseService`, `CourseRepository`, `CourseDto` 등 구 도메인 전부 |

### `booking-service/` — `enrollment-service/` 리네임

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `src/main/java/com/lecture/booking/entity/Booking.java` | 이동 | `Enrollment` → `Booking`. `movieId`, `quantity`, `amount`, `PENDING/CONFIRMED/CANCELLED` 상태 |
| `src/main/java/com/lecture/booking/service/BookingService.java` | 신규 | 예매 오케스트레이션 — 영화 확인 → 예매 생성 → 결제 요청 → 실패 시 PENDING 자동 취소 |
| `src/main/java/com/lecture/booking/service/BookingWriteService.java` | 신규 | 예매 INSERT 를 **별도 트랜잭션**으로 즉시 커밋 (결제 완료 이벤트가 바깥 트랜잭션 커밋보다 먼저 도착하는 경합 방지) |
| `src/main/java/com/lecture/booking/service/MovieServiceClient.java` | 신규 | movie-service 호출 — 영화 존재 확인, 티켓 단가 조회, 예매수 증가 |
| `src/main/java/com/lecture/booking/service/PaymentServiceClient.java` | 이동 | 결제 요청 페이로드를 `bookingId` + `movieId` 계약으로 변경 |
| `src/main/java/com/lecture/booking/kafka/BookingKafkaConsumer.java` | 신규 | `payment.completed` 소비 → 예매 CONFIRMED 확정 |
| `src/main/java/com/lecture/booking/kafka/BookingKafkaProducer.java` | 신규 | `booking.completed` 발행 (`bookingId, userId, movieId, genre`). 발행 실패가 확정 트랜잭션을 롤백시키지 않도록 격리 |
| `src/main/java/com/lecture/booking/kafka/KafkaEvent.java` | 신규 | 이벤트 페이로드 정의 |
| `src/main/java/com/lecture/booking/controller/BookingController.java` | 신규 | `POST /api/bookings`, `GET /api/bookings/my`, `/user/{userId}`, 내부용 `/internal/history/{userId}` |
| `src/main/java/com/lecture/booking/dto/BookingDto.java` | 신규 | 요청·응답 DTO 및 예매 이력 응답 |
| `src/main/java/com/lecture/booking/repository/BookingRepository.java` | 신규 | 사용자별 예매 조회 |
| `src/main/java/com/lecture/booking/config/**` | 이동/신규 | `JpaConfig`·`KafkaConfig`·`SecurityConfig`·`WebClientConfig` 이동, `GlobalExceptionHandler` 재작성 |
| `src/test/java/com/lecture/booking/**` | 신규 | `BookingServiceTest`, `BookingKafkaConsumerTest` |
| `~~enrollment-service/**~~` | 삭제 | `Enrollment*` 클래스, `CourseServiceClient` 등 구 도메인 전부 |

### `payment-service/` — 결제 키 교체

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `src/main/java/com/lecture/payment/entity/Payment.java` | 수정 | `course_id` 제거 → `booking_id`(NOT NULL) + `movie_id` 추가 |
| `src/main/java/com/lecture/payment/dto/PaymentDto.java` | 수정 | 요청/응답 전 구간에서 `courseId` → `bookingId`/`movieId` |
| `src/main/java/com/lecture/payment/repository/PaymentRepository.java` | 수정 | `findByUserIdAndCourseId` → `findByBookingId` (재예매가 가능해 `(userId, movieId)` 로는 결제를 특정할 수 없음) |
| `src/main/java/com/lecture/payment/service/PaymentService.java` | 수정 | 결제 저장·로그를 새 키 기준으로, `payment.completed` 에 `bookingId` 포함 |
| `src/main/java/com/lecture/payment/kafka/PaymentKafkaProducer.java` | 수정 | `PaymentCompletedEvent` 에 `bookingId` 추가, `courseId` → `movieId` |
| `build.gradle`, `settings.gradle` | 수정 | Maven Central 미러 추가 |

### `recommend-service/` (FastAPI) — 간식 추천으로 전환

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `app/llm/client.py` | 신규 | OpenAI 호환 chat completions 클라이언트(DeepSeek 등). markdown fence·trailing comma 를 보정하는 JSON 파서 포함 |
| `app/llm/prompt.py` | 신규 | context injection 프롬프트 빌더 — 음식 카탈로그 + 장르별 감정/미각 프로파일 + 논문 근거를 주입 |
| `app/model/movie_data.py` | 신규 | 장르 라벨, 장르→맛 매핑(`GENRE_TASTE_MAP`), 장르별 감정 프로파일 |
| `app/model/foods.json` | 신규 | 영화관 간식 카탈로그 (맛·카테고리·가격·인기도) |
| `app/client/movie_client.py` | 신규 | `GET /api/movies/internal/{movieId}` 로 장르 조회 (래핑/비래핑 응답 모두 대응) |
| `app/service/recommend_service.py` | 수정 | 추천 로직 전면 교체 — 장르 조회 → LLM 추천 → 실패 시 규칙 기반 폴백 |
| `app/kafka/consumer.py` | 수정 | `booking.completed` 이벤트 계약(`bookingId, userId, movieId, genre`)으로 매핑 |
| `app/router/recommend_router.py` | 수정 | `GET /api/recommend/{userId}?movie_id=&limit=` |
| `app/model/schemas.py`, `app/config/settings.py`, `main.py` | 수정 | 영화/LLM 스키마 및 `MOVIE_SERVICE_URL`·LLM 설정 추가 |
| `README.md` | 신규 | 추천 서비스 단독 문서 |
| `.gitignore` | 신규 / `.env` 삭제 | 키가 담긴 `.env` 를 추적 해제 |
| `~~app/client/course_client.py~~`, `~~app/client/enrollment_client.py~~` | 삭제 | 구 도메인 클라이언트 |

### `user-service/`, `eureka-server/`

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `build.gradle`, `settings.gradle` (양쪽 모두) | 수정 | 빌드 중 `429 Too Many Requests` 를 피하려고 Google 이 운영하는 Maven Central 미러를 `repositories` / `pluginManagement` 최우선으로 추가 |

### `vue_frontend_demo/vue-frontend/` — 실제 프론트엔드

> compose 가 빌드하는 프론트는 이쪽이다. 루트의 `vue-frontend/` 는 강의 원본 사본이라 손대지 않았다.

| 경로 | 구분 | 변경 내용 |
|---|---|---|
| `src/api/index.js` | 신규 | axios 인스턴스 — 토큰 자동 첨부, 401 처리 |
| `src/api/auth.js` | 신규 | OAuth2 인가 코드 ↔ 토큰 교환(CLIENT_SECRET_BASIC), `/api/users/me` |
| `src/api/movie.js` | 신규 | 영화 목록·박스오피스·검색·장르별 조회 |
| `src/api/booking.js` | 신규 | 예매 생성, 내 예매 목록 |
| `src/api/payment.js` | 신규 | 사용자 결제 내역 |
| `src/api/recommend.js` | 신규 | 간식 추천 호출 (`movie_id` 스네이크 케이스) |
| `src/lib/genre.js` | 신규 | 백엔드 Genre enum ↔ 한글 라벨 매핑 |
| `src/store/auth.js` | 신규 | 로그인 상태·토큰(sessionStorage) 관리, 회원가입 |
| `src/store/bookings.js` | 신규 | 예매·결제 내역 화면 상태 |
| `src/views/LoginView.vue`, `RegisterView.vue`, `CallbackView.vue` | 신규 | 앱 내 로그인 폼 / 회원가입 / OAuth2 콜백 처리 |
| `src/views/MovieListView.vue`, `MyPageView.vue`, `PaymentsView.vue`, `SnacksView.vue` | 신규 | 영화 목록·마이페이지·결제 내역·간식 추천 화면 |
| `src/views/HomeView.vue`, `RankingView.vue`, `MovieDetailView.vue` | 수정 | 목업 데이터 → 실제 API 연동. 포스터 없는 영화는 노출 제외 |
| `src/components/BookingDialog.vue`, `HeroCarousel.vue`, `SnackCard.vue` | 신규 | 예매 다이얼로그, 메인 캐러셀, 간식 카드 |
| `src/components/AppHeader.vue`, `AppFooter.vue` | 수정 | 로그인 상태 반영 및 라우트 갱신 |
| `src/data/movies.js` (수정), `reviews.js`·`snacks.js` (신규) | 수정/신규 | 폴백용 정적 데이터 |
| `src/router/index.js` | 수정 | 신규 화면 라우트 및 인증 가드 |
| `vite.config.js` | 수정 | 포트 3000 고정, `/api`·`/oauth2`·`/login`·`/logout`·`/userinfo` 를 게이트웨이로 프록시(동일 출처 유지) |
| `Dockerfile`, `nginx.conf` | 신규 | 빌드 후 nginx 서빙. SPA history 폴백 + 인증 경로 프록시. `/login` 은 GET → SPA, POST → auth-server 로 분기 |
| `.env.example` | 신규 | `VITE_API_BASE_URL`, `VITE_CLIENT_ID/SECRET`, `VITE_REDIRECT_URI` 샘플 |
| `package.json`, `package-lock.json`, `index.html`, `.gitignore`, `src/assets/styles/globals.css` | 수정 | 의존성(axios, pinia, tailwind) 및 전역 스타일 |
