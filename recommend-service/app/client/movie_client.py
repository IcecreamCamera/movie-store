# -*- coding: utf-8 -*-
import logging

import httpx

from app.config.settings import settings

logger = logging.getLogger(__name__)


class MovieServiceClient:
    """
    Movie Service REST 클라이언트 — movieId → 장르(genre) 조회.
    최종 계약(Notion): GET /api/movies/internal/{movieId} → MovieResponse{ ..., genre }
    """

    def __init__(self):
        self.base_url = settings.movie_service_url

    async def get_movie_genre(self, movie_id: int):
        url = f"{self.base_url}/api/movies/internal/{movie_id}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                resp = await client.get(url)
                resp.raise_for_status()
                data = resp.json()
        except httpx.HTTPError as e:
            logger.warning(f"[MovieClient] 영화 조회 실패 - movieId: {movie_id}, error: {e}")
            return None

        if isinstance(genre, str):
            return genre.strip().upper()
        return None
        return genre


movie_client = MovieServiceClient()
