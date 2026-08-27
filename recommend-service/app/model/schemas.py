# -*- coding: utf-8 -*-
from typing import List, Optional
from pydantic import BaseModel


class Food(BaseModel):
    """food 카탈로그 항목 (foods.json, 내부 로드용)"""
    id: int
    name: str
    imageUrl: str = ""
    price: int
    taste: List[str] = []
    category: str = ""
    popularity: int = 0


class RecommendedFood(BaseModel):
    """추천 결과 food (Notion API 명세: FoodItem{ id, name, imageUrl, price, taste, category })"""
    id: int
    name: str
    imageUrl: str = ""
    price: int
    taste: List[str] = []
    category: str = ""
    reason: Optional[str] = None


class RecommendResponse(BaseModel):
    """GET /api/recommend/{user_id}?movie_id= — 추천 응답"""
    userId: int
    movieId: int
    recommendedFoods: List[RecommendedFood]
    basedOnGenre: str
    message: Optional[str] = None
