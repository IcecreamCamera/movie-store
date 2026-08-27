# 배포 가이드 (Deployment)

맥(macOS)에서 **Cloudflare Tunnel**을 이용해 로컬 서비스(`localhost`)를 외부에 공유하는 방법을 이 프로젝트(movie-store)에 맞게 정리한 문서입니다.

> 개요: **로컬에서 앱 실행** → **`cloudflared` 터널 실행** → 생성된 `https://xxx.trycloudflare.com` 주소 공유.
> 외부 도메인·포트포워딩 불필요. 터널이 실행 중일 때만 접속 가능.

---

## 0. 사전 준비 (맥 기준)

- **Docker Desktop** (백엔드 실행용)
- **Node.js** (프론트 dev 서버용, vue-frontend)
- **cloudflared** — Cloudflare 공식 바이너리

```bash
# Apple Silicon(M1/M2/M3/M4/M5) 확인
uname -m          # → arm64 (Apple Silicon)

# cloudflared 다운로드/실행 권한
cd ~/Downloads
tar -xzf cloudflared-darwin-arm64.tgz
chmod +x cloudflared
# 보안 차단 시:
xattr -d com.apple.quarantine ./cloudflared
```

---

## 1. 리포 클론

```bash
git clone https://github.com/IcecreamCamera/movie-store.git
cd movie-store
```

- `main` 브랜치가 기본 배포 대상.
- **추천 피처** 포함 버전은 `feat/recommend-llm` 브랜치:
  ```bash
  git checkout feat/recommend-llm
  ```

---

## 2. 환경변수 설정

```bash
cp .env.example .env
# .env 에 아래 필수 값 입력:
#   KOBIS_API_KEY, TMDB_API_KEY   (영화 데이터 수집 - 없으면 movie 수집 안 됨)
#   LLM_API_KEY                   (간식 추천 LLM - 없으면 rule-based 로만 동작)
```

---

## 3. 앱 실행 (백엔드 + 프론트)

### 3-1. 백엔드 (Docker Compose)
movie-service/booking-service 등 Java 서비스는 소스에서 빌드됩니다 (팀이 추가한 **Google Maven 미러**로 429 우회).

```bash
# 전체 백엔드 빌드 + 실행
docker compose up -d --build

# (또는) 이미 빌드된 이미지가 있으면 재빌드 없이
docker compose up -d --no-build --pull never
```

- `api-gateway :8080`, `auth-server :9000`, `user/movie/booking/payment :8081~8084`, `recommend-service :8085`
- `mariadb :3379(→3306)`, `kafka :9092`, `eureka :8761`

### 3-2. 프론트엔드 (Vite dev 서버, :3000)
```bash
cd vue-frontend
npm install
npm run dev        # → http://localhost:3000
```
(dev 서버가 `/api`, `/oauth2` 등을 `localhost:8080`(게이트웨이)로 프록시)

---

## 4. 정상 동작 확인

```bash
# 게이트웨이 (백엔드)
curl http://localhost:8080/api/courses          # 또는 movie API
curl http://localhost:8080/api/recommend/genres # 추천 장르

# 프론트
curl http://localhost:3000
```
브라우저에서 `http://localhost:3000` 접속 → 회원가입/로그인 → 영화·간식 추천 화면 확인.

---

## 5. Cloudflare Tunnel 로 외부 공유

```bash
./cloudflared tunnel --url http://localhost:3000
```
성공 시 터미널에 다음과 같은 외부 주소가 표시됩니다.
```
https://xxxxx.trycloudflare.com
```
이 주소를 공유하면 외부에서 접속 가능합니다.

**참고**
- cloudflared가 실행되는 동안만 외부 접속 가능.
- 터미널 종료 or `Ctrl+C` → 공유 주소도 만료.
- 외부 접속이 안 되면, 노출할 대상을 프론트(`:3000`)가 아니라 백엔드 게이트웨이(`:8080`)로 바꿔 시도:
  ```bash
  ./cloudflared tunnel --url http://localhost:8080
  ```

---

## 6. 참고(프로젝트 특이사항)

- **DB 데이터**: `movies`/`users` 등은 `lecture_db`(mariadb, volume `mariadb_data`)에 저장. 초기엔 movie 수집 필요(`KOBIS/TMDB_API_KEY`) 또는 수동 시드.
- **추천 피처**: `LLM_API_KEY` 없으면 rule-based 로만 추천(`recommendedFoods` + `basedOnGenre`, reason 없음).
- **JWT 인증**: `/api/recommend/{user_id}` 등 대부분 API는 `auth-server` 토큰 필요. 프론트에서 로그인하면 자동 발급됨.
