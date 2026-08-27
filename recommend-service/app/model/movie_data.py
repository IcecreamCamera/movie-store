# -*- coding: utf-8 -*-
"""
영화 장르 enum + 논문 기반 heuristic 프로파일.

출처:
- movie-store DDL(movies.genre): TMDB 공식 장르 `Genre` enum name
  (ACTION, ADVENTURE, ..., SCIENCE_FICTION, ..., OTHER) — store `movies.genre` 로 직렬화됨
- Notion "추천 알고리즘 근거 (논문)" / "KMDB 장르 → 감정 → 미각 매핑 테이블":
  감정/각성 latent feature(violence/arousal/valence/tension) 기반의 genre → taste heuristic prior

용도:
- movie-service가 반환한 genre(TMDB enum name) 검증
- LLM context injection 프롬프트에 "장르 설명" 주입
- LLM 실패/키 미설정 시 rule-based fallback (`GENRE_TASTE_MAP`)
"""

# TMDB 공식 장르 enum name (movie-store Genre.java)
MOVIE_GENRES = [
    "ACTION", "ADVENTURE", "ANIMATION", "COMEDY", "CRIME", "DOCUMENTARY",
    "DRAMA", "FAMILY", "FANTASY", "HISTORY", "HORROR", "MUSIC", "MYSTERY",
    "ROMANCE", "SCIENCE_FICTION", "TV_MOVIE", "THRILLER", "WAR", "WESTERN", "OTHER",
]

# 통용 가능한 taste 집합 (food.taste 과 매칭)
TASTE_KEYS = ["salty", "sweet", "spicy", "sour", "savory", "creamy", "fatty", "crispy"]

# 한국어 표시명 (프롬프트/응답 표기용)
GENRE_LABEL = {
    "ACTION": "액션", "ADVENTURE": "어드벤처", "ANIMATION": "애니메이션",
    "COMEDY": "코메디", "CRIME": "범죄", "DOCUMENTARY": "다큐멘터리",
    "DRAMA": "드라마", "FAMILY": "가족", "FANTASY": "판타지", "HISTORY": "역사",
    "HORROR": "공포", "MUSIC": "뮤직", "MYSTERY": "미스터리", "ROMANCE": "멜로/로맨스",
    "SCIENCE_FICTION": "SF", "TV_MOVIE": "TV영화", "THRILLER": "스릴러",
    "WAR": "전쟁", "WESTERN": "서부", "OTHER": "기타",
}

# ── heuristic prior: genre → taste (논문 기반) ────────────────────────────────
# 고각성/긴장/폭력 → 짠·기름·매운 (Mattara 2018, Nature EJCN 긴장/불안)
_HIGH_AROUSAL = ["ACTION", "CRIME", "HORROR", "THRILLER", "WAR"]
# 긴장 + 호기심 혼합
_TENSION_CURIOSITY = ["MYSTERY", "SCIENCE_FICTION"]
# 저각성/이완/위안 → 단·크리미 (SagePub 로맨스>공포 단맛, EJCN 로맨스=졸림·이완 p=0.019)
_LOW_AROUSAL = ["DRAMA", "FAMILY", "ROMANCE", "HISTORY", "ANIMATION"]
# 기쁨/흥분/유쾌 → 신·단·바삭 (Medium: 기쁨/흥분 → 신 사탕·시트러스·바삭)
_JOY = ["COMEDY", "ADVENTURE", "FANTASY", "MUSIC"]
# 중립 → salty+sweet 하프앤하프 fallback
_NEUTRAL = ["DOCUMENTARY", "TV_MOVIE", "WESTERN", "OTHER"]

_GENRE_TASTE_TABLE = {
    **{g: ["salty", "fatty", "spicy"] for g in _HIGH_AROUSAL},
    **{g: ["salty", "savory", "spicy"] for g in _TENSION_CURIOSITY},
    **{g: ["sweet", "creamy"] for g in _LOW_AROUSAL},
    **{g: ["sour", "sweet"] for g in _JOY},
    **{g: ["salty", "sweet"] for g in _NEUTRAL},
}

GENRE_TASTE_MAP = {g: _GENRE_TASTE_TABLE[g] for g in MOVIE_GENRES}

# ── genre → 감정/각성 프로필 (LLM 프롬프트 주입용 설명) ──────────────────────
_GENRE_EMOTION = {
    **{g: "긴장·각성이 높고 위험·폭력적인 어두운 분위기" for g in _HIGH_AROUSAL},
    **{g: "긴장감과 호기심이 혼합된 미스터리한 몰입의 분위기" for g in _TENSION_CURIOSITY},
    **{g: "따뜻하고 이완되며 위안이 되는 차분한 분위기" for g in _LOW_AROUSAL},
    **{g: "기쁘고 밝으며 흥분되는 유쾌한 분위기" for g in _JOY},
    **{g: "중립적이고 혼합된 차분한 분위기" for g in _NEUTRAL},
}

GENRE_EMOTION = {g: _GENRE_EMOTION[g] for g in MOVIE_GENRES}

# LLM 실패 시 사용하는 기본 fallback taste (하프앤하프 안전빵)
DEFAULT_TARGET_TASTE = ["salty", "sweet"]
