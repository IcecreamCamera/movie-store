# -*- coding: utf-8 -*-
import logging

from fastapi import APIRouter, Depends, Query

from app.config.security import verify_token
from app.model.movie_data import MOVIE_GENRES
from app.model.schemas import RecommendResponse
from app.service.recommend_service import recommend_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/recommend", tags=["recommend"])


@router.get("/genres", include_in_schema=False)
async def list_genres():
    """지원 장르 목록 (TMDB enum, OTHER 제외)"""
    return {"genres": [g for g in MOVIE_GENRES if g != "OTHER"]}


@router.get("/health", include_in_schema=False)
async def health_check():
    return {"status": "UP", "service": "recommend-service"}


@router.get("/{user_id}", response_model=RecommendResponse)
async def get_food_recommendations(
    user_id: int,
    movie_id: int = Query(..., description="영화 ID (장르 해석용)"),
    limit: int = Query(3, ge=1, le=5, description="추천 음식 개수"),
    token_payload: dict = Depends(verify_token),
):
    """
    GET /api/recommend/{user_id}?movie_id=123&limit=3

    Notion API 명세: userId(경로) + movieId(쿼리) → recommendedFoods, basedOnGenre, message.
    - movie_service에서 장르(TMDB genre enum) 조회 → 장르 기반 음식(food) 추천
    - primary: LLM context injection, fallback: GENRE_TASTE_MAP rule-based
    """
    logger.info(f"[Router] 음식 추천 요청 - user_id: {user_id}, movie_id: {movie_id}, limit: {limit}")
    return await recommend_service.get_food_recommendations(user_id, movie_id, limit)
