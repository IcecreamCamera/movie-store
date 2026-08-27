# -*- coding: utf-8 -*-
"""
LLM context injection 프롬프트 빌더.

"음식 정보(food 카탈로그) + 장르 정보(감정/각성 프로파일 + 논문 근거)"를
모두 자연어로 서술해 주입하는 것이 이 추천의 핵심 신호(signal)이다.
근거 출처: Notion "추천 알고리즘 근거 (논문)" 페이지의 5편 논문.
"""

from app.model.movie_data import GENRE_LABEL, GENRE_TASTE_MAP, GENRE_EMOTION

# 논문 근거 요약 (prior/heuristic) — 프롬프트에 항상 주입
PAPER_NOTE = (
    "논문 근거: Mattara et al. 2018(폭력·긴장 영화 → 짠·기름진 음식 선호, 짠 71.4% / 지방 62%), "
    "Nature EJCN(447명, 공포→긴장·불안↑ / 로맨스→이완·졸림, 단 음식 선호 유의 P=0.019), "
    "Wiley 1750-3841(단맛→긍정, 쓴맛→분노·혐오, 짠맛·신맛→복합 감정)"
)


def build_recommend_messages(
    genre: str,
    foods: list[dict],
    limit: int,
    user_context: str = "없음",
) -> list[dict]:
    """장르 → food 추천 프롬프트 메시지 목록 생성."""
    emotion = GENRE_EMOTION.get(genre, "")
    taste = GENRE_TASTE_MAP.get(genre, ["salty", "sweet"])
    label = GENRE_LABEL.get(genre, genre)

    food_lines = "\n".join(
        f"- id:{f['id']} {f['name']} "
        f"(taste: {', '.join(f['taste'])}, category: {f['category']}, price: {f['price']}원, popularity: {f['popularity']})"
        for f in foods
    )

    system = (
        "너는 지식기반(knowledge-based) 영화관 음식 추천 시스템이다. "
        "영화 장르의 감정·각성 프로파일과 논문 근거에 따라 어울리는 음식(food)을 고르고, "
        "그 이유를 논문 근거와 연결해 설명한다. "
        "출력은 반드시 유효한 JSON 객체 하나만, 불필요한 텍스트 없이."
    )

    user = f"""영화 장르: {label}
장르 분위기: {emotion}
장르의 미각 프로파일(참고): {', '.join(taste)}
논문 근거: {PAPER_NOTE}
사용자 최근 이용 맥락: {user_context}

[음식(food) 카탈로그]
{food_lines}

위 장르와 논문 근거를 근거로, 가장 잘 어울리는 음식 {limit}개를 추천해줘.
각 음식은 id로 지정하고, "왜 이 음식이 어울리는지"를 논문 근거 문장과 연결해
각 reason은 60자 이내, 한 문장(한국어)으로 간결히 설명해줘.

응답은 다음 JSON 형식 그대로만 출력해줘:
{{"recommendedFoods": [{{"id": 1, "reason": "..."}}]}}
"""

    return [
        {"role": "system", "content": system},
        {"role": "user", "content": user},
    ]
