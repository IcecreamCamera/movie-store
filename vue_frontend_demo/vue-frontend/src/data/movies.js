// Figma Make 원본 App.tsx의 목 데이터를 옮긴 것.
// 원본은 Math.random()으로 year/genre/rating을 만들어 새로고침마다 값이 바뀌었는데,
// 화면이 흔들리지 않도록 시드 기반 의사난수로 고정했다.

const POSTERS = [
  'https://images.unsplash.com/photo-1594181985790-4ad34b333bca?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxrb3JlYW4lMjBtb3ZpZSUyMHBvc3RlcnxlbnwxfHx8fDE3NTY5NjUzMjB8MA&ixlib=rb-4.1.0&q=80&w=1080',
  'https://images.unsplash.com/photo-1753944847480-92f369a5f00e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtb3ZpZSUyMHBvc3RlciUyMGNpbmVtYXxlbnwxfHx8fDE3NTY4ODI5OTN8MA&ixlib=rb-4.1.0&q=80&w=1080',
  'https://images.unsplash.com/photo-1739891251370-05b62a54697b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhY3Rpb24lMjBtb3ZpZSUyMHBvc3RlcnxlbnwxfHx8fDE3NTY4ODI5OTN8MA&ixlib=rb-4.1.0&q=80&w=1080',
  'https://images.unsplash.com/photo-1710988486821-9af47f60d62b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx0aHJpbGxlciUyMG1vdmllJTIwcG9zdGVyfGVufDF8fHx8MTc1Njk2NDMzOXww&ixlib=rb-4.1.0&q=80&w=1080',
  'https://images.unsplash.com/photo-1572700432881-42c60fe8c869?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxkcmFtYSUyMG1vdmllJTIwcG9zdGVyfGVufDF8fHx8MTc1NjkxNjIwOXww&ixlib=rb-4.1.0&q=80&w=1080'
]

export const ACTOR_IMAGE =
  'https://images.unsplash.com/photo-1686245203273-8f3fabb01ea3?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhY3RvciUyMHBvcnRyYWl0JTIwY2luZW1hfGVufDF8fHx8MTc1Njk3MjE5OXww&ixlib=rb-4.1.0&q=80&w=1080'

export const SCENE_IMAGE =
  'https://images.unsplash.com/photo-1563202221-f4eae97e4828?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtb3ZpZSUyMHNjZW5lJTIwY2luZW1hfGVufDF8fHx8MTc1Njk3MjIwM3ww&ixlib=rb-4.1.0&q=80&w=1080'

export const featuredMovie = {
  id: 'featured-1',
  title: '우는 남자',
  director: '이정범',
  actors: '장동건, 김민희, 박성웅',
  poster: POSTERS[0],
  year: 2024,
  genre: '액션/드라마',
  rating: 8.7,
  runtime: 125,
  description:
    '절대 울지 않는 남자의 마지막 눈물을 그린 감동 액션 드라마. 복수와 용서 사이에서 고뇌하는 한 남자의 이야기가 깊은 울림을 준다. 가족을 잃은 슬픔과 분노로 가득한 주인공이 진정한 용서와 구원을 찾아가는 여정을 그린 작품으로, 액션과 드라마가 완벽하게 조화를 이룬다.'
}

const MOVIE_DATA = [
  { title: '기생충', director: '봉준호', actors: '송강호, 이선균, 조여정, 최우식' },
  { title: '올드보이', director: '박찬욱', actors: '최민식, 유지태, 강혜정' },
  { title: '부산행', director: '연상호', actors: '공유, 정유미, 마동석' },
  { title: '신세계', director: '박훈정', actors: '이정재, 황정민, 박성웅' },
  { title: '곡성', director: '나홍진', actors: '곽도원, 황정민, 천우희' },
  { title: '핸드메이든', director: '박찬욱', actors: '김민희, 김태리, 하정우' },
  { title: '버닝', director: '이창동', actors: '유아인, 전종서, 스티븐 연' },
  { title: '택시운전사', director: '장훈', actors: '송강호, 토마스 크레치만' },
  { title: '1987', director: '장준환', actors: '김윤석, 하정우, 유해진' },
  { title: '암살', director: '최동훈', actors: '전지현, 이정재, 하정우' },
  { title: '도둑들', director: '최동훈', actors: '김윤석, 김혜수, 이정재' },
  { title: '극한직업', director: '이병헌', actors: '류승범, 이하늬, 진선규' },
  { title: '베테랑', director: '류승완', actors: '황정민, 유아인, 유해진' },
  { title: '검은사제들', director: '장재현', actors: '김윤석, 강동원, 이솜' },
  { title: '밀정', director: '김지운', actors: '송강호, 공유, 한지민' },
  { title: '아가씨', director: '박찬욱', actors: '김민희, 김태리, 하정우' },
  { title: '마더', director: '봉준호', actors: '김혜자, 원빈, 진구' },
  { title: '살인의 추억', director: '봉준호', actors: '송강호, 김상경, 김뢰하' },
  { title: '괴물', director: '봉준호', actors: '송강호, 변희봉, 박해일' },
  { title: '친구', director: '곽경택', actors: '유오성, 장동건, 서태화' }
]

const GENRES = ['액션', '드라마', '코미디', '스릴러', '로맨스', '호러', 'SF', '범죄']

const DESCRIPTIONS = [
  '계급사회의 모순을 예리하게 파헤친 봉준호 감독의 걸작. 기택 가족과 박 사장 가족 사이의 기생관계를 통해 현대사회의 계급갈등을 그려낸다.',
  '박찬욱 감독의 복수 3부작 중 두 번째 작품. 15년간 감금된 남자의 복수를 그린 충격적인 스릴러.',
  '좀비 바이러스가 창궐한 KTX 안에서 벌어지는 생존 드라마. 인간성에 대한 깊이 있는 성찰을 담았다.',
  '조직의 세계를 사실적으로 그려낸 범죄 드라마. 이정재, 황정민의 열연이 돋보인다.',
  '미스터리 호러의 새로운 지평을 연 나홍진 감독의 대표작. 곡성에 나타난 정체불명의 존재를 둘러싼 이야기.',
  '박찬욱 감독이 선보인 에로틱 스릴러. 일제강점기를 배경으로 한 여성들의 치밀한 계략을 그린다.',
  '이창동 감독의 섬세한 연출이 돋보이는 미스터리 드라마. 무라카미 하루키의 소설을 각색했다.',
  '5.18 광주민주화운동을 배경으로 한 감동적인 드라마. 송강호의 뛰어난 연기가 인상적이다.',
  '1987년 6월 항쟁을 다룬 정치 드라마. 한국 현대사의 중요한 순간을 생생하게 재현했다.',
  '일제강점기 독립군의 암살 작전을 그린 액션 영화. 전지현, 이정재, 하정우의 캐스팅이 화제가 됐다.',
  '홍콩, 마카오, 부산을 오가며 벌어지는 도둑들의 이야기. 스타 캐스팅과 스펙터클한 액션이 볼거리.',
  '마약수사대의 위장 수사를 소재로 한 액션 코미디. 류승범, 이하늬 등의 코믹 연기가 웃음을 준다.',
  '부패한 재벌 3세와 그를 쫓는 강력계 형사의 대결을 그린 액션 영화. 황정민의 카리스마가 돋보인다.',
  '악귀를 쫓는 신부들의 이야기를 그린 오컬트 호러. 김윤석, 강동원의 연기가 인상적이다.',
  '일제강점기 이중스파이의 갈등을 그린 스파이 스릴러. 송강호, 공유의 연기 대결이 볼거리.',
  '조선시대 기생들의 이야기를 그린 에로틱 드라마. 아름다운 영상미와 탄탄한 스토리가 조화를 이룬다.',
  '아들을 둔 어머니의 모성애를 그린 미스터리 드라마. 김혜자의 압도적인 연기가 깊은 인상을 남긴다.',
  '연쇄살인마를 쫓는 형사들의 이야기. 봉준호 감독의 데뷔작으로 한국 영화사에 한 획을 그었다.',
  '한강에 나타난 괴물과 맞서는 가족의 이야기. 환경 문제에 대한 메시지를 담은 작품.',
  '두 친구의 우정과 파국을 그린 한국 느와르의 고전. 부산 사투리와 시대상이 생생하게 살아있다.'
]

// 시드 기반 의사난수 (mulberry32) — 값이 매번 동일하게 재현된다
function seeded(seed) {
  return function () {
    seed |= 0
    seed = (seed + 0x6d2b79f5) | 0
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export const allMovies = MOVIE_DATA.map((m, i) => {
  const rand = seeded(i + 1)
  return {
    id: `movie-${i}`,
    title: m.title,
    director: m.director,
    actors: m.actors,
    poster: POSTERS[i % POSTERS.length],
    year: 2000 + Math.floor(rand() * 25),
    genre: GENRES[Math.floor(rand() * GENRES.length)],
    rating: 7.0 + rand() * 2.5,
    runtime: 90 + Math.floor(rand() * 60),
    description: DESCRIPTIONS[i]
  }
})

// 홈 화면 섹션별 데이터 (원본 App.tsx의 slice 구간 그대로)
export const personalizedTopMovies = allMovies.slice(0, 3).map((m, i) => ({ ...m, rank: i + 1 }))
export const latestMovies = allMovies.slice(6, 12)
export const topMovies = allMovies.slice(12, 17).map((m, i) => ({ ...m, rank: i + 1 }))
export const reviewEventMovies = allMovies.slice(17, 20)

export function findMovieById(id) {
  return allMovies.find((m) => m.id === id) || null
}
