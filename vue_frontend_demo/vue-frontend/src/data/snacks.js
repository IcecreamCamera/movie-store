// 매점 간식 추천 — 임시 더미 데이터
//
// ⚠️ 자리만 잡아둔 것입니다.
// 장르 → 관객 감정 → 미각 선호(짠맛/단맛) → 메뉴 매핑의 실제 근거는
// 팀 논문 정리 문서(MD)가 도착하면 확정합니다.
//
// 백엔드 연동 시 이 파일을 지우고
// GET /api/recommend/{userId}?movieId={movieId} 응답으로 교체하세요.
// (응답 명세: STEP 6. API 명세서 → /api/recommend/{user_id})

export const SNACK_PLACEHOLDER_NOTE = '감정–미각 매핑 근거 문서 도착 전 임시 값'

// 실제 멀티플렉스 매점에서 파는 품목 위주로 구성했습니다.
// taste    : 단맛 | 짠맛   (팀 핵심 로직의 미각 축)
// category : SNACK | DRINK | COMBO
const C = {
  // ── 팝콘
  popcornOriginal: { name: '오리지널 팝콘', taste: '짠맛', category: 'SNACK', price: 5000 },
  popcornButter: { name: '버터 팝콘', taste: '짠맛', category: 'SNACK', price: 5000 },
  popcornCaramel: { name: '카라멜 팝콘', taste: '단맛', category: 'SNACK', price: 5500 },
  popcornCheese: { name: '치즈 팝콘', taste: '짠맛', category: 'SNACK', price: 5500 },
  popcornOnion: { name: '어니언 팝콘', taste: '짠맛', category: 'SNACK', price: 5500 },
  popcornGarlic: { name: '갈릭 팝콘', taste: '짠맛', category: 'SNACK', price: 5800 },
  popcornChoco: { name: '초코 팝콘', taste: '단맛', category: 'SNACK', price: 5800 },

  // ── 짭짤한 스낵
  nachoCheese: { name: '나쵸 & 치즈', taste: '짠맛', category: 'SNACK', price: 6000 },
  nachoSalsa: { name: '나쵸 & 살사', taste: '짠맛', category: 'SNACK', price: 6000 },
  fries: { name: '감자튀김', taste: '짠맛', category: 'SNACK', price: 5000 },
  cheeseStick: { name: '치즈스틱', taste: '짠맛', category: 'SNACK', price: 5500 },
  onionRing: { name: '어니언링', taste: '짠맛', category: 'SNACK', price: 5500 },
  popcornChicken: { name: '팝콘치킨', taste: '짠맛', category: 'SNACK', price: 7500 },
  chickenTender: { name: '치킨텐더', taste: '짠맛', category: 'SNACK', price: 8000 },
  hotdog: { name: '핫도그', taste: '짠맛', category: 'SNACK', price: 6500 },
  sausage: { name: '통소시지', taste: '짠맛', category: 'SNACK', price: 5000 },
  pizzaSlice: { name: '피자 한 조각', taste: '짠맛', category: 'SNACK', price: 6500 },

  // ── 건어물
  squidSpicy: { name: '매운 오징어', taste: '짠맛', category: 'SNACK', price: 5500 },
  squidButter: { name: '버터구이 오징어', taste: '짠맛', category: 'SNACK', price: 6000 },
  jerky: { name: '육포', taste: '짠맛', category: 'SNACK', price: 7000 },

  // ── 단것
  churros: { name: '츄러스', taste: '단맛', category: 'SNACK', price: 5000 },
  iceCream: { name: '아이스크림', taste: '단맛', category: 'SNACK', price: 4000 },
  chocoSnack: { name: '초코 스낵', taste: '단맛', category: 'SNACK', price: 4500 },
  waffle: { name: '와플', taste: '단맛', category: 'SNACK', price: 5500 },
  cookie: { name: '쿠키', taste: '단맛', category: 'SNACK', price: 3500 },
  gummy: { name: '젤리', taste: '단맛', category: 'SNACK', price: 3000 },

  // ── 음료
  cola: { name: '콜라', taste: '단맛', category: 'DRINK', price: 3000 },
  colaZero: { name: '제로콜라', taste: '단맛', category: 'DRINK', price: 3000 },
  cider: { name: '사이다', taste: '단맛', category: 'DRINK', price: 3000 },
  icedTea: { name: '아이스티', taste: '단맛', category: 'DRINK', price: 3500 },
  slush: { name: '슬러시', taste: '단맛', category: 'DRINK', price: 4000 },
  water: { name: '생수', taste: '짠맛', category: 'DRINK', price: 1500 },

  // ── 콤보
  comboClassic: { name: '팝콘 콤보 (팝콘+콜라)', taste: '짠맛', category: 'COMBO', price: 9000 },
  comboSweet: { name: '스위트 콤보 (카라멜팝콘+아이스티)', taste: '단맛', category: 'COMBO', price: 9500 },
  comboHot: { name: '핫 콤보 (핫도그+콜라)', taste: '짠맛', category: 'COMBO', price: 9500 }
}

// key를 id로 부여
const CATALOG = Object.fromEntries(
  Object.entries(C).map(([id, v]) => [id, { id, ...v }])
)

export const allSnacks = Object.values(CATALOG)

// 장르 → 간식. "Open API" 문서 §11 매핑 초안을 목데이터 장르명(한글)에 맞춘 것.
// 각성도가 높은 장르에는 짠맛, 긴장 후 이완이 필요한 장르에는 단맛을 배치했습니다.
const BY_GENRE = {
  액션: ['hotdog', 'nachoCheese', 'popcornChicken', 'cola', 'comboHot'],
  스릴러: ['nachoSalsa', 'jerky', 'popcornGarlic', 'colaZero'],
  범죄: ['squidSpicy', 'jerky', 'squidButter', 'colaZero'],
  호러: ['chocoSnack', 'popcornCaramel', 'iceCream', 'slush'],
  코미디: ['churros', 'iceCream', 'popcornCheese', 'cider', 'comboSweet'],
  로맨스: ['popcornCaramel', 'waffle', 'cheeseStick', 'icedTea', 'comboSweet'],
  드라마: ['popcornCaramel', 'cheeseStick', 'cookie', 'icedTea'],
  SF: ['popcornButter', 'fries', 'onionRing', 'cola'],
  판타지: ['popcornChoco', 'churros', 'gummy', 'slush'],
  애니메이션: ['popcornCaramel', 'gummy', 'iceCream', 'cider'],
  다큐: ['popcornOriginal', 'cookie', 'water'],
  전쟁: ['sausage', 'jerky', 'pizzaSlice', 'cola']
}

const DEFAULT_SET = ['comboClassic', 'popcornOriginal', 'cola']

/**
 * 영화 장르에 맞는 간식을 고릅니다.
 * '액션/드라마'처럼 복합 장르는 앞쪽을 대표 장르로 씁니다.
 * 매핑에 없는 장르는 기본 세트로 폴백합니다. (백엔드 §11 규칙과 동일)
 */
export function recommendByGenre(genre, limit = 4) {
  const primary = String(genre ?? '').split('/')[0].trim()
  const ids = BY_GENRE[primary] ?? DEFAULT_SET
  return ids.slice(0, limit).map((id) => CATALOG[id])
}

/** 매핑이 정의된 장르 목록 (간식추천 화면의 장르 칩) */
export const mappedGenres = Object.keys(BY_GENRE)

// 홈 화면 상시 노출용 기본 세트 (예매 이력이 없을 때 백엔드가 주는 값에 해당)
export const snackRecommendations = DEFAULT_SET.map((id) => CATALOG[id]).concat(
  CATALOG.popcornCaramel
)
