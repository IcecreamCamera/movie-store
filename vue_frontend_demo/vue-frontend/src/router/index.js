import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue')
  },
  {
    path: '/booking',
    name: 'Booking',
    component: () => import('@/views/MovieListView.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/MovieListView.vue')
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('@/views/RankingView.vue')
  },
  {
    path: '/snacks',
    name: 'Snacks',
    component: () => import('@/views/SnacksView.vue')
  },
  {
    path: '/payments',
    name: 'Payments',
    component: () => import('@/views/PaymentsView.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue')
  },
  {
    path: '/mypage',
    name: 'MyPage',
    component: () => import('@/views/MyPageView.vue')
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

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to) {
    if (to.hash) return { el: to.hash, top: HEADER_OFFSET, behavior: 'smooth' }
    return { top: 0 }
  }
})

export default router
