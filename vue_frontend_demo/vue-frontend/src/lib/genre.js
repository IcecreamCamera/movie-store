// 백엔드 Genre enum(TMDB 기준, 영문) <-> 화면 표시용 한글 라벨 매핑.
// movie-service의 GET /api/movies/genre/{genre}는 이 enum 이름(ACTION, DRAMA...)을 그대로 받는다.

export const GENRE_LABELS = {
  ACTION: '액션',
  ADVENTURE: '모험',
  ANIMATION: '애니메이션',
  COMEDY: '코미디',
  CRIME: '범죄',
  DOCUMENTARY: '다큐멘터리',
  DRAMA: '드라마',
  FAMILY: '가족',
  FANTASY: '판타지',
  HISTORY: '역사',
  HORROR: '공포',
  MUSIC: '음악',
  MYSTERY: '미스터리',
  ROMANCE: '로맨스',
  SCIENCE_FICTION: 'SF',
  TV_MOVIE: 'TV 영화',
  THRILLER: '스릴러',
  WAR: '전쟁',
  WESTERN: '서부',
  OTHER: '기타'
}

// 화면에 노출할 장르 필터 옵션 (전체 제외). 표시 순서 고정.
export const GENRE_OPTIONS = Object.keys(GENRE_LABELS)

// 백엔드 enum 코드 -> 한글 라벨
export function genreLabel(code) {
  return GENRE_LABELS[code] || code || '기타'
}

// 한글 라벨 -> 백엔드 enum 코드
export function genreCode(label) {
  const entry = Object.entries(GENRE_LABELS).find(([, v]) => v === label)
  return entry ? entry[0] : label
}
