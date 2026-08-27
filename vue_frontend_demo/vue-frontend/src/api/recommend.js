import api from './index.js'

// 영화 기반 간식 추천 (recommend-service).
// movie_id는 스네이크 케이스이며 필수, limit은 1~5(기본 3)이다.
// JWT가 필요하며 axios 인스턴스가 Authorization 헤더를 자동으로 붙인다.
// GET /api/recommend/{userId}?movie_id={movieId}&limit={limit}
export function recommend(userId, movieId, limit) {
  const params = { movie_id: movieId }
  if (limit) params.limit = limit
  return api.get(`/api/recommend/${userId}`, { params })
}
