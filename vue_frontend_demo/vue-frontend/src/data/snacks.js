// 매점 간식 추천 — 임시 더미 데이터
//
// ⚠️ 이 파일은 자리만 잡아둔 것입니다.
// 장르 → 관객 감정 → 미각 선호(짠맛/단맛) → 메뉴 매핑의 실제 근거는
// 팀 논문 정리 문서(MD)가 도착하면 그때 확정합니다.
// 그때 바꿀 것: mood / taste / reason 값. 화면 구조는 그대로 두면 됩니다.

export const SNACK_PLACEHOLDER_NOTE = '감정–미각 매핑 근거 문서 도착 전 임시 값'

export const snackRecommendations = [
  {
    id: 'snack-caramel',
    name: '카라멜 팝콘',
    taste: '단맛',
    mood: '긴장 해소',
    price: 5500,
    reason: '[근거 문구 — 매핑 문서 도착 후 교체]'
  },
  {
    id: 'snack-nacho',
    name: '나쵸 & 치즈',
    taste: '짠맛',
    mood: '각성 유지',
    price: 6000,
    reason: '[근거 문구 — 매핑 문서 도착 후 교체]'
  },
  {
    id: 'snack-butter',
    name: '버터 팝콘',
    taste: '짠맛',
    mood: '몰입',
    price: 5000,
    reason: '[근거 문구 — 매핑 문서 도착 후 교체]'
  },
  {
    id: 'snack-choco',
    name: '초코 스낵',
    taste: '단맛',
    mood: '여운',
    price: 4500,
    reason: '[근거 문구 — 매핑 문서 도착 후 교체]'
  }
]

// 홈 화면 배너에 노출할 추천 흐름 (표지와 동일한 체인)
export const recommendChain = ['영화 장르', '관객 감정', '미각 선호', '매점 간식']
