# 추천 로직 (Recommend Logic)

> recommend-service의 추천 알고리즘을 설명한다. (영화 장르 기반 음식 추천)

---

## 1. 전체 흐름

```
GET /api/recommend/{user_id}?movie_id={id}&limit={n}   (JWT)
        │
        ▼
① 장르 해석     movie_id → movie-service(/api/movies/internal/{id}) → genre(TMDB enum)
        │        └ 실패/미지 → DEFAULT_MOVIE_GENRE(DRAMA) 폴백
        ▼
② 신호 구성     genre → { 미각(taste) 프로파일, 감정·각성 설명, 논문 근거 }  + 음식 카탈로그
        │
        ▼
③ 추천           [LLM context injection]  ▸ 성공 → reason 포함
                  └ 실패/키없음 → [Rule-based 가중점수]  ▸ reason 없음
        ▼
④ 응답          { userId, movieId, recommendedFoods[]: FoodItem, basedOnGenre, message }
```

---

## 2. 입력 신호 (genre → taste)

핵심 가설: **영화 장르가 유발하는 감정/각성 상태 → 선호 미각(taste) → 음식**.

`GENRE_TASTE_MAP`이 장르를 미각 집합으로 매핑합니다 (논문의 latent feature(violence/arousal/valence/tension)를 수동 매핑).

| 장르 그룹 | 미각(taste) | 논문 근거 |
|---|---|---|
| ACTION / CRIME / HORROR / THRILLER / WAR (고각성·긴장) | `salty, fatty, spicy` | Mattara 2018 (짠 71.4% / 지방 62%) |
| MYSTERY / SCIENCE_FICTION (긴장+호기심) | `salty, savory, spicy` | 동일 축, 몰입감 |
| DRAMA / FAMILY / ROMANCE / HISTORY / ANIMATION (이완·위안) | `sweet, creamy` | SagePub 2021 · EJCN (로맨스→단맛, P=0.019) |
| COMEDY / ADVENTURE / FANTASY / MUSIC (기쁨·흥분) | `sour, sweet` | 감정-미각 매핑 (긍정 고각성→신·바삭) |
| DOCUMENTARY / TV_MOVIE / WESTERN / OTHER (중립) | `salty, sweet` | 하프앤하프 안전빵 |

장르 enum: TMDB 공식 장르 20종 (`ACTION … WESTERN, OTHER`). `app/model/movie_data.py`.

---

## 3. 추천 알고리즘 (2경로)

### ① LLM context injection (primary)
`app/llm/prompt.py`가 장르·음식·근거를 **자연어로 모두 서술**해 주입.

```
시스템: 지식기반 영화관 음식 추천 시스템. 반드시 유효한 JSON만 출력.
사용자:
  영화 장르: {label}              # 예: 액션
  장르 분위기: {emotion}           # 예: 긴장·각성이 높고 위험·폭력적인 어두운 분위기
  미각 프로파일: {taste}           # 예: salty, fatty, spicy
  논문 근거: {PAPER_NOTE}         # Mattara 2018 / EJCN(P=0.019) / Wiley
  사용자 맥락: {user_context}      # 최근 이용 영화 ID (Kafka)
  [음식 카탈로그] 16개             # id/이름/taste/category/price/popularity
  → 가장 어울리는 N개, 각 reason을 논문 근거와 연결
  → JSON: {"recommendedFoods": [{"id": 1, "reason": "..."}]}
```

- `llm_client`(DeepSeek, OpenAI-compatible) 호출: `response_format=json_object`, `max_tokens=2048`, `temperature=0.4`.
- 응답의 `id`들로 카탈로그에서 음식을 매칭 → `reason`(근거 문장) 포함.
- **유효한 음식이 없으면 예외 → rule-based 폴백.**

### ② Rule-based 가중점수 (fallback)
결정적이고 설명가능하며 재현성을 확보.

```
score = |snack.taste ∩ GENRE_TASTE_MAP[genre]| × 2  +  popularity × 0.1
```

- 미각 교집합 크기를 우선하되 인기(popularity)로 보정.
- 상위 `limit`개 선택 + **안전빵**: `taste == {salty, sweet}`(하프앤하프) 음식 1개 강제 포함.
- `reason` 없음(규칙 기반).

**선택 기준**: `LLM_API_KEY`가 있고 호출 성공 시 LLM(①), 실패/키 없음 → rule-based(②).  
즉 **LLM이 주력, 규칙이 안전망**.

---

## 4. 개인화 맥락 (user_context)

- `booking.completed`(Kafka) → `user_recent_context[userId]`(in-memory `deque(maxlen=20)`)에 **최근 이용 영화 ID** 적립.
- 프롬프트에 `"최근 이용 영화 ID: 1, 5, 12"` 형태로 주입.
- 의도: 사용자별 개인화 신호(→ 추후 posterior). **현재는 영화 ID만 전달**되어 영향은 약함.

---

## 5. 응답 스키마 (`RecommendResponse`)

```json
{
  "userId": 1,
  "movieId": 1,
  "recommendedFoods": [
    {
      "id": 1, "name": "솔티팝콘", "imageUrl": "https://...",
      "price": 5000, "taste": ["salty"], "category": "팝콘",
      "reason": "짠맛은 긴장·각성 높은 액션 영화에서 선호되는 미각으로, 논문에서 짠맛 선호 71.4%를 지지합니다."
    }
  ],
  "basedOnGenre": "ACTION",
  "message": "ACTION 장르 기반 LLM 추천입니다"
}
```

- `FoodItem`: `id, name, imageUrl, price, taste[], category, reason`.
- `basedOnGenre`: 추천에 사용된 장르(TMDB enum name).

---

## 6. 데이터 소스

| 항목 | 파일 | 내용 |
|---|---|---|
| 장르 | `app/model/movie_data.py` | TMDB 공식 장르 20종 + `GENRE_TASTE_MAP` / `GENRE_EMOTION` / `GENRE_LABEL` |
| 음식 | `app/model/foods.json` | 16개 (id/name/imageUrl/price/taste/category/popularity) |
| 논문 근거(prior) | `app/llm/prompt.py`의 `PAPER_NOTE` | Mattara 2018, SagePub 2021, Nature EJCN(447명, P=0.019), Wiley 1750-3841 |

---

## 7. 엣지 / 폴백 케이스

| 상황 | 동작 |
|---|---|
| movie-service 미기동 / 장르 없음 | `DEFAULT_MOVIE_GENRE`(DRAMA) 폴백 |
| `movie_id` 장르가 enum에 없음 | 동일하게 폴백 |
| LLM 키 없음 / 호출 실패 / JSON 파싱 실패 | **rule-based** 폴백 |
| LLM이 유효한 id 없이 응답 | 예외 → rule-based |
| 사용자 이력(Kafka) 없음 | `user_context = 없음` |

---

## 8. 알려진 한계 / 후속 계획

- **논문 prior 의존**: 실제 구매 데이터가 없어 논문 실험 결과를 prior로 사용. 실험실 선호 ≠ 실제 구매.
- **개인화 약함**: `user_context`가 영화 ID만 전달되어 영향이 미미. 이벤트에 포함된 `genre`를 저장해 장르 맥락으로 주입하면 개선.
- **후속(Phase 2)**: 구매 로그(`snack_orders`) 수집 → `P(snack | genre)` **posterior**(베이지안)로 prior를 보정.
- **movie-service 필요**: `movie_id → 장르` 해석은 movie-service가 떠 있어야 정상.
