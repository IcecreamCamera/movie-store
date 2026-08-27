# -*- coding: utf-8 -*-
import json
import logging
from pathlib import Path
from typing import List, Optional, Tuple

from app.client.movie_client import movie_client
from app.config.settings import settings
from app.kafka.consumer import user_recent_context
from app.llm.client import llm_client
from app.llm.prompt import build_recommend_messages
from app.model.movie_data import GENRE_TASTE_MAP, MOVIE_GENRES
from app.model.schemas import Food, RecommendedFood, RecommendResponse

logger = logging.getLogger(__name__)

_FOODS_PATH = Path(__file__).resolve().parent.parent / "model" / "foods.json"


class RecommendService:
    """
    영화 장르 기반 음식(food) 추천 서비스.

    알고리즘 신호:
    1) LLM context injection (primary) - 장르 정보 + 음식 정보를 모두 서술한 프롬프트 주입.
    2) Rule-based (fallback) - GENRE_TASTE_MAP 기반 가중 점수.

    Notion API 명세: GET /api/recommend/{user_id}?movie_id=
    → recommendedFoods[]: FoodItem{ id, name, imageUrl, price, taste, category }, basedOnGenre, message
    """

    def _load_foods(self) -> List[Food]:
        return [Food(**f) for f in json.loads(_FOODS_PATH.read_text(encoding="utf-8"))]

    def _user_context(self, user_id: Optional[int]) -> Optional[str]:
        if user_id is None:
            return None
        recent = user_recent_context.get(user_id)
        if recent:
            return f"최근 이용 영화 ID: {', '.join(str(m) for m in recent)}"
        return None

    async def _resolve_genre(self, movie_id: int) -> str:
        """movieId → 장르 해석 (movie-service 조회, 실패/없으면 기본값)"""
        genre = None
        if movie_id is not None:
            try:
                genre = await movie_client.get_movie_genre(movie_id)
            except Exception as e:
                logger.warning(f"[RecommendService] 장르 해석 오류: {e}")
        if not genre or genre not in MOVIE_GENRES:
            logger.warning(
                f"[RecommendService] movieId={movie_id} 장르 없음 → default '{settings.default_movie_genre}' 사용"
            )
            return settings.default_movie_genre
        return genre

    async def get_food_recommendations(
        self,
        user_id: int,
        movie_id: int,
        limit: Optional[int] = None,
    ) -> RecommendResponse:
        limit = limit or settings.llm_max_snacks
        genre = await self._resolve_genre(movie_id)
        foods = self._load_foods()
        user_context = self._user_context(user_id)

        if settings.llm_api_key:
            try:
                picks, message = await self._llm_recommend(genre, foods, limit, user_context)
                return self._response(user_id, movie_id, genre, picks, message)
            except Exception as e:
                logger.warning(f"[RecommendService] LLM 추천 실패, rule-based fallback: {e}")

        picks, message = self._rule_based_recommend(genre, foods, limit)
        return self._response(user_id, movie_id, genre, picks, message)

    def _response(
        self,
        user_id: int,
        movie_id: int,
        genre: str,
        picks: List[RecommendedFood],
        message: str,
    ) -> RecommendResponse:
        return RecommendResponse(
            userId=user_id,
            movieId=movie_id,
            recommendedFoods=picks,
            basedOnGenre=genre,
            message=message,
        )

    async def _llm_recommend(
        self,
        genre: str,
        foods: List[Food],
        limit: int,
        user_context: Optional[str],
    ) -> Tuple[List[RecommendedFood], str]:
        messages = build_recommend_messages(
            genre, [f.model_dump() for f in foods], limit, user_context or "없음"
        )
        data = await llm_client.chat_json(messages)
        items = data.get("recommendedFoods", [])

        by_id = {f.id: f for f in foods}
        picks: List[RecommendedFood] = []
        for item in items:
            if not isinstance(item, dict) or "id" not in item:
                continue
            food = by_id.get(int(item["id"]))
            if food is None:
                continue
            picks.append(
                RecommendedFood(
                    id=food.id,
                    name=food.name,
                    imageUrl=food.imageUrl,
                    price=food.price,
                    taste=food.taste,
                    category=food.category,
                    reason=item.get("reason"),
                )
            )
            if len(picks) >= limit:
                break

        if not picks:
            raise ValueError("LLM 응답에 유효한 음식이 없습니다")

        return picks, f"{genre} 장르 기반 LLM 추천입니다"

    def _rule_based_recommend(
        self,
        genre: str,
        foods: List[Food],
        limit: int,
    ) -> Tuple[List[RecommendedFood], str]:
        target = set(GENRE_TASTE_MAP.get(genre, ["salty", "sweet"]))
        scored = sorted(
            foods,
            key=lambda f: (len(set(f.taste) & target) * 2 + f.popularity * 0.1),
            reverse=True,
        )
        picks = scored[:limit]

        # 안전빵: salty+sweet 하프앤하프 음식 1개 포함
        pick_ids = {f.id for f in picks}
        safe = next((f for f in foods if set(f.taste) == {"salty", "sweet"}), None)
        if safe is not None and safe.id not in pick_ids and len(picks) == limit:
            picks[-1] = safe

        recommended = [
            RecommendedFood(
                id=f.id, name=f.name, imageUrl=f.imageUrl,
                price=f.price, taste=f.taste, category=f.category,
            )
            for f in picks
        ]
        return recommended, f"{genre} 장르 기반 규칙 추천입니다"


recommend_service = RecommendService()
