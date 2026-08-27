// 영화별 리뷰 목데이터
//
// 리뷰는 API 명세서에 엔드포인트가 없습니다. 화면 구성을 보여주기 위한 더미입니다.
// 리뷰 기능을 실제로 넣게 되면 백엔드에 목록/작성 API를 요청해야 합니다.

const NICKNAMES = [
  '팝콘러버', '심야극장', '주말관객', '영화광', '조용한관람객',
  '첫줄선호', '뒷자리파', '자막파', '재관람러', '시네필'
]

const COMMENTS = [
  { score: 5, text: '기대 이상이었어요. 끝나고 한참 여운이 남았습니다.' },
  { score: 4, text: '배우들 연기가 좋았어요. 후반부가 조금 늘어지는 느낌은 있었습니다.' },
  { score: 5, text: '오랜만에 극장에서 제대로 몰입했어요. 사운드가 특히 좋았습니다.' },
  { score: 3, text: '나쁘진 않은데 예고편이 더 재밌었던 것 같아요.' },
  { score: 4, text: '스토리가 탄탄합니다. 같이 본 친구도 만족했어요.' },
  { score: 5, text: '연출이 인상적이었어요. 한 번 더 볼 생각입니다.' },
  { score: 4, text: '중반까지는 평범한데 마지막 20분이 다 살렸어요.' },
  { score: 2, text: '개인적으로는 조금 지루했습니다. 취향을 타는 작품 같아요.' },
  { score: 4, text: '영상미가 좋아서 큰 화면으로 보길 잘했어요.' },
  { score: 5, text: '가족들과 봤는데 다들 좋아했습니다.' }
]

// 제목 → 안정적인 정수. 같은 영화는 항상 같은 리뷰가 나옵니다.
function hash(str) {
  let h = 2166136261
  for (let i = 0; i < String(str).length; i++) {
    h ^= String(str).charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

/** 영화 한 편의 리뷰 목록 */
export function reviewsFor(movie, count = 4) {
  const n = hash(movie?.title ?? '')
  return Array.from({ length: count }, (_, i) => {
    const c = COMMENTS[(n + i * 3) % COMMENTS.length]
    const daysAgo = ((n >> (i + 1)) % 30) + 1
    const d = new Date()
    d.setDate(d.getDate() - daysAgo)
    return {
      id: `${movie?.id ?? 'm'}-r${i}`,
      nickname: NICKNAMES[(n + i * 7) % NICKNAMES.length],
      score: c.score,
      text: c.text,
      date: d
    }
  })
}

/** 별점 분포 (5점 → 1점) */
export function scoreBreakdown(reviews) {
  const counts = [0, 0, 0, 0, 0]
  reviews.forEach((r) => { counts[5 - r.score] += 1 })
  const total = reviews.length || 1
  return counts.map((c, i) => ({ score: 5 - i, count: c, pct: Math.round((c / total) * 100) }))
}
