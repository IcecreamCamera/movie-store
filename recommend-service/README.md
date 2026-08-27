# Recommend Service

영화 장르 기반 **음식(food) 추천** 서비스입니다. movie-store MSA에서 `movie_id`(영화)를 받아, 해당 영화의 장르에 어울리는 매점 간식을 추천합니다.

- 스택: **FastAPI** (Python 3.12, uvicorn)
- 이벤트 소비: **Apache Kafka** (`booking.completed` → 사용자 이용 맥락)
- 추천 근거: **논문 기반 knowledge-based prior** + **LLM context injection** (DeepSeek)

---

## 동작 흐름

```
[Vue Frontend]
      │  GET /api/recommend/{user_id}?movie_id={id}   (JWT Bearer)
      ▼
[recommend-service :8085]
   1. movie_id → genre  : movie-service  [GET /api/movies/internal/{id}]
      └─ 해석 실패/없으면 → DEFAULT_MOVIE_GENRE 로 폴백
   2. genre + 음식 카탈로그 + 논문 근거 → LLM 프롬프트 주입 (DeepSeek)
      → 추천 음식 + reason(근거 문장) 생성
   3. LLM 실패/키 미설정 → GENRE_TASTE_MAP rule-based 가중점수
      ▶ 최종 응답: { recommendedFoods[], basedOnGenre, message }
```

`booking.completed` (Kafka) 를 영화 이용 이벤트로 해석해, 사용자의 최근 이용 영화 맥락을 프롬프트에 주입합니다.

---

## API

### `GET /api/recommend/{user_id}?movie_id={movieId}&limit={n}`
- **인증**: `Authorization: Bearer <JWT>` (issuer 검증)
- **입력**: `user_id`(경로), `movie_id`(필수 쿼리, 장르 해석용), `limit`(선택, 1~5, 기본 3)
- **응답**:
```json
{
  "userId": 1,
  "movieId": 1,
  "recommendedFoods": [
    { "id": 1, "name": "솔티팝콘", "imageUrl": "...", "price": 5000,
      "taste": ["salty"], "category": "팝콘",
      "reason": "짠맛은 긴장·각성 높은 액션 영화에서 선호되는 미각으로, 논문에서 짠맛 선호 71.4%를 지지합니다." }
  ],
  "basedOnGenre": "ACTION",
  "message": "ACTION 장르 기반 LLM 추천입니다"
}
```

### 기타
- `GET /api/recommend/genres` — 지원 장르(TMDB 19종, `OTHER` 제외)
- `GET /api/recommend/health` — 헬스체크 `{"status":"UP","service":"recommend-service"}`

---

## 추천 알고리즘

**① LLM context injection (primary)**
장르 정보 + 음식 정보 + 논문 근거를 모두 자연어로 서술해 LLM에 주입합니다.
```
영화 장르 / 장르 분위기(감정·각성) / 미각 프로파일 / 논문 근거 / 사용자 맥락
[음식 카탈로그]
→ "가장 잘 어울리는 음식 N개, 각 reason을 논문 근거와 연결"
→ JSON: {"recommendedFoods":[{"id":1,"reason":"..."}]}
```

**② Rule-based 가중점수 (fallback)**
```
score = |snack.taste ∩ GENRE_TASTE_MAP[genre]| * 2 + popularity * 0.1
```
상위 N개 선택 + 안전빵으로 `salty+sweet`(하프앤하프) 1개 포함. LLM 실패/키 미설정 시 사용.

> 참고: 실제 구매 데이터가 없는 현재는 논문 실험 결과를 **prior/heuristic**으로 사용합니다. 이후 구매 로그를 쌓아 `P(snack|genre)` **posterior**(베이지안)로 보정하는 단계(Phase 2)를 계획.

---

## 데이터

### 장르 (`app/model/movie_data.py`)
- **TMDB 공식 장르** enum(20): `ACTION, ADVENTURE, ANIMATION, COMEDY, CRIME, DOCUMENTARY, DRAMA, FAMILY, FANTASY, HISTORY, HORROR, MUSIC, MYSTERY, ROMANCE, SCIENCE_FICTION, TV_MOVIE, THRILLER, WAR, WESTERN, OTHER`
- `GENRE_TASTE_MAP` — genre → 미각 profile (논문 매핑)
- `GENRE_EMOTION` / `GENRE_LABEL` — 프롬프트 주입용 감정·각성 설명 / 표기명

### 음식 카탈로그 (`app/model/foods.json`)
16개 항목: `id, name, imageUrl, price, taste[], category, popularity`

### 논문 근거 (Notion "추천 알고리즘 근거 (논문)")
| 논문 | 내용 |
|---|---|
| Mattara et al. 2018 | 폭력·긴장 영화 → 짠·기름진 음식 선호 (짠 71.4% / 지방 62%) |
| SagePub 2021 | 로맨스 → 단 음식 선호↑, 공포 → 단맛 선호↓ |
| Nature EJCN (447명) | 공포→긴장·불안↑ / 로맨스→이완·졸림, 단 음식 선호 **P=0.019** |
| Wiley 1750-3841 | 미각→감정 매핑 (단→긍정, 쓴→분노·혐오, 짠·신→복합) |

---

## 설정 (환경변수 / `.env`)

| 변수 | 설명 | 기본 |
|---|---|---|
| `MOVIE_SERVICE_URL` | movie-service 호스트 (장르 해석) | `http://movie-service:8082` |
| `LLM_API_KEY` | DeepSeek API 키 (**필수, `.env`로만 주입, gitignore**) | 빈 값 → LLM 비활성(rule-based만) |
| `LLM_BASE_URL` | LLM API base | `https://api.deepseek.com/v1` |
| `LLM_MODEL` | 모델명 | `deepseek-chat` |
| `LLM_MAX_SNACKS` / `LLM_MAX_TOKENS` | 추천 개수 / 토큰 상한 | `3` / `2048` |
| `DEFAULT_MOVIE_GENRE` | 장르 해석 실패 시 폴백 | `DRAMA` |
| `KAFKA_*` | `booking.completed` 소비자 설정 | — |
| `EUREKA_*` / `JWT_*` | 등록/검증 | — |

> `.env`는 git에서 제외됩니다 (`recommend-service/.gitignore`). 배포 시 `LLM_API_KEY`를 환경변수로 주입해야 LLM 추천이 활성화됩니다.

---

## 실행

### 로컬
```bash
cd recommend-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --port 8085 --reload
```

### 컨테이너 (prebuilt 이미지, 교수님 지침 — 소스 빌드 금지)
```bash
# movie-store 루트에서
docker compose up -d --no-build --pull never
```

---

## 테스트 / 검증

서비스레이어(JWT 불필요)로 바로 확인:
```bash
docker exec -w /app lecture-recommend python -c "
import asyncio
from app.service.recommend_service import recommend_service
async def m():
    r = await recommend_service.get_food_recommendations(1, 1, 3)
    print(r.model_dump_json())
asyncio.run(m())
"
```

전체(HTTP + movie-service 연동) 확인:
1. `movies` 테이블에 테스트 영화 시드 → `docker exec lecturedb mariadb -umanager -pSqlDba-1 lecture_db -e "INSERT INTO movies (movie_cd,title,genre,price,status) VALUES ('V100','테스트액션','ACTION',14000.00,'ACTIVE');"`
2. `curl http://localhost:8082/api/movies/internal/1` → `"genre":"ACTION"`
3. `curl -H "Authorization: Bearer <TOKEN>" "http://localhost:8085/api/recommend/1?movie_id=1&limit=3"`

장르 분기 확인: `ACTION`→짠/매운(솔티팝콘·나쵸), `ROMANCE`→단/크리미(초콜릿타코·카라멜아이스크림).

---

## 구조

```
recommend-service/
├── main.py                    # FastAPI 앱 + lifespan(Eureka 등록, Kafka 소비자)
├── app/
│   ├── config/
│   │   ├── settings.py        # 설정(env)
│   │   └── security.py        # JWT 검증 (verify_token)
│   ├── client/movie_client.py # movie-service → genre 조회
│   ├── kafka/consumer.py      # booking.completed → 이용 맥락 저장
│   ├── llm/
│   │   ├── client.py          # OpenAI-compatible chat (DeepSeek) + JSON 파서
│   │   └── prompt.py          # context injection 프롬프트 + 논문 근거
│   ├── model/
│   │   ├── movie_data.py      # TMDB 장르 enum + taste/emotion 매핑
│   │   ├── foods.json         # 음식 카탈로그
│   │   └── schemas.py         # Food/RecommendedFood/RecommendResponse
│   ├── router/recommend_router.py
│   └── service/recommend_service.py  # LLM(primary) + rule-based(fallback)
└── Dockerfile                 # uvicorn (8085)
```

## 알려진 한계
- `movie_id → 장르` 해석은 movie-service가 떠 있어야 정상. 없으면 `DEFAULT_MOVIE_GENRE` 폴백.
- 실제 구매 데이터가 없어 **논문 prior**에 의존 (실험실 선호 ≠ 실제 구매). 구매 로그 도입 시 posterior 보정 필요.
- 영화별 장르 분기 검증은 movie-service(Java 빌드: Google maven 미러 필요)가 필요.
