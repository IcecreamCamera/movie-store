import api from './index.js'

// 예매 생성. 사용자 식별은 게이트웨이가 토큰에서 뽑은 X-User-Id 헤더로 이뤄진다.
export function create({ movieId, quantity }) {
  return api.post('/api/bookings', { movieId, quantity })
}

// 내 예매 목록
export function my() {
  return api.get('/api/bookings/my')
}
