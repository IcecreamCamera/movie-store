# vue-frontend

Figma Make로 만든 영화 트렌드 사이트 디자인을 Vue 3로 옮긴 뒤, 팀 서비스명 **오도독**으로 바꾼 프론트엔드입니다.

## 스택

- Vue 3 (`<script setup>`, JavaScript)
- Vite 6
- Vue Router 4
- Tailwind CSS v4 (`@tailwindcss/vite`)
- `@lucide/vue` (원본의 lucide-react 대응)
- `tailwind-merge` (shadcn의 `cn()` 헬퍼 대응)

## 실행

```bash
npm install
npm run dev      # http://localhost:5173
npm run build
```

## 구조

```
src/
  assets/styles/globals.css   # Figma Make 원본의 디자인 토큰
  lib/cn.js                   # 클래스 충돌 병합 헬퍼
  components/
    AppHeader.vue             # 원본 components/Header.tsx
    AppFooter.vue             # 원본 components/Footer.tsx
    ImageWithFallback.vue     # 원본 components/figma/ImageWithFallback.tsx
    ui/BaseButton.vue         # shadcn/ui Button 최소 구현
    ui/BaseBadge.vue          # shadcn/ui Badge 최소 구현
    ui/BaseInput.vue          # shadcn/ui Input 최소 구현
  views/
    HomeView.vue              # 원본 App.tsx (히어로 + 4개 섹션)
    MovieDetailView.vue       # 원본 components/MovieDetailPage.tsx
    RankingView.vue           # 원본 components/RankingPage.tsx
  data/movies.js              # 원본 App.tsx의 목 데이터
  router/index.js
```

## 라우트

| 경로 | 화면 |
| --- | --- |
| `/` | 홈 |
| `/ranking` | 영화 랭킹 |
| `/movies/:id` | 영화 상세 |

## 원본과 다른 점

- 원본은 `useState`로 페이지를 갈아끼웠지만, 여기서는 vue-router를 씁니다.
- 원본은 목 데이터의 개봉연도/장르/평점을 `Math.random()`으로 만들어 새로고침마다 값이 바뀌었습니다.
  화면이 흔들리지 않도록 시드 기반 의사난수로 고정했습니다.
- 아직 옮기지 않은 화면: 검색(SearchPage), 리뷰(ReviewPage), 영화목록(MovieListPage), 관리자(AdminPage).
  헤더의 "검색"·"리뷰" 메뉴는 라우트가 생기기 전까지 동작하지 않습니다.
- 포스터 이미지는 원본과 동일하게 Unsplash 외부 URL을 씁니다.
