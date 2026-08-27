import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue')
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('@/views/RankingView.vue')
  },
  {
    path: '/movies/:id',
    name: 'MovieDetail',
    component: () => import('@/views/MovieDetailView.vue')
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

// 스티키 헤더(h-20 = 80px)에 섹션 제목이 가리지 않도록 두는 여유값
const HEADER_OFFSET = 96

// 라우터가 스크롤을 트리거하지 않는 경우(같은 위치로 재이동)에 직접 쓰는 헬퍼
export function scrollToSection(hash) {
  const el = hash ? document.querySelector(hash) : null
  if (!el) return
  window.scrollTo({
    top: el.getBoundingClientRect().top + window.scrollY - HEADER_OFFSET,
    behavior: 'smooth'
  })
}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to) {
    if (to.hash) return { el: to.hash, top: HEADER_OFFSET, behavior: 'smooth' }
    return { top: 0 }
  }
})

export default router
