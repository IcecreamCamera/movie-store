import api from './index.js'
import { genreLabel } from '@/lib/genre.js'

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

// API 영화(MovieResponse) -> 화면 표시용 뷰 모델. 여러 화면이 같은 필드 이름(mock 시절 이름)을
// 쓰도록 여기서 한 번에 매핑한다. 다른 화면들도 각자 지역 mapMovie를 갖고 있지만
// 이 함수가 그 기준이 되므로 나중에 하나씩 이 함수로 옮겨도 된다.
//
// 필드 대응:
// - poster/backdrop: API는 posterUrl 하나만 준다. 별도 backdrop이 없어 poster를 그대로 재사용한다.
// - year: openDt("2019-05-30") 문자열에서 연도만 뽑는다. openDt가 없으면 null.
// - rating: voteAverage가 없으면 0으로 채운다(다른 화면과 동일한 관례). 실제 값이 없다는 사실은
//   잃지만, 템플릿의 `.toFixed(1)` 호출이 항상 안전해진다.
// - genre: 백엔드 enum(SCIENCE_FICTION 등)을 한글 라벨로 바꾼다 (src/lib/genre.js의 GENRE_LABELS).
// - description: 없으면 null. 화면에서 빈 문단을 그리지 않고 감춰야 한다.
// - runtime/director/actors: 두 API 어디에서도 주지 않는 값이라 아예 넣지 않는다.
//   TMDB 상세 조회를 의도적으로 하지 않기 때문이며, 화면은 이 값이 없을 때 렌더링을 감춰야 한다.
export function toViewMovie(m = {}) {
  const year = m.openDt ? new Date(m.openDt).getFullYear() : null
  return {
    id: m.id,
    title: m.title,
    poster: m.posterUrl || '',
    backdrop: m.posterUrl || '',
    year: Number.isFinite(year) ? year : null,
    rating: Number(m.voteAverage ?? 0),
    genre: genreLabel(m.genre),
    description: m.description || null
  }
}

// posterUrl이 없는 영화(검색으로 들어와 TMDB 매칭에 실패한 항목)를 판별한다.
// 목록/그리드 화면에서만 걸러내는 데 쓰고, 상세 화면(/movies/{id})에는 적용하지 않는다.
export function hasPoster(m) {
  return Boolean(m?.posterUrl)
}
