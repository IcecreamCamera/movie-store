import api from './index.js'

// 전체 영화 목록
export function list() {
  return api.get('/api/movies')
}

// 장르별 목록. genre는 백엔드 Genre enum 값(ACTION, DRAMA 등)이어야 한다.
export function byGenre(genre) {
  return api.get(`/api/movies/genre/${encodeURIComponent(genre)}`)
}

// 영화 상세
export function detail(id) {
  return api.get(`/api/movies/${id}`)
}

// 영화 이름 검색. KOBIS 오픈 API로 조회 후 DB에 upsert되어 바로 예매 가능하다.
// axios가 params를 URLSearchParams로 직렬화하며 한글 등은 자동으로 percent-encode된다.
export function search(q) {
  return api.get('/api/movies/search', { params: { q } })
}

// 박스오피스 랭킹. type: 'DAILY' | 'WEEKLY'
export function boxoffice(type) {
  return api.get('/api/movies/boxoffice', { params: { type } })
}
