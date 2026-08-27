<template>
  <header class="bg-black/95 backdrop-blur-sm sticky top-0 z-50 border-b border-gray-800">
    <div class="max-w-7xl mx-auto px-8 lg:px-16">
      <div class="flex items-center justify-between h-20">
        <!-- 로고 -->
        <div class="flex items-center">
          <button
            class="odok-logo cursor-pointer transition-colors"
            aria-label="오도독 홈으로"
            @click="go('home')"
          ><span class="odok-ch" style="transform: translateY(0)">오</span><span class="odok-ch" style="transform: translateY(-4px)">도</span><span class="odok-ch odok-ch-last" style="transform: translateY(-8px)">독<svg class="odok-kernel" viewBox="0 0 80 80" aria-hidden="true"><g class="odok-spark" stroke="currentColor" stroke-width="6" stroke-linecap="round" fill="none"><line x1="13" y1="25" x2="21" y2="33" /><line x1="40" y1="9" x2="40" y2="20" /><line x1="67" y1="25" x2="59" y2="33" /></g><g transform="translate(40,52)"><g class="odok-pop"><g transform="scale(0.82)"><circle cx="0" cy="0" r="19" fill="#FFF6E3" /><circle cx="-15" cy="-9" r="14" fill="#FFF6E3" /><circle cx="15" cy="-9" r="14" fill="#FFF6E3" /><circle cx="-11" cy="11" r="13" fill="#FFF6E3" /><circle cx="12" cy="11" r="13" fill="#FFF6E3" /><circle cx="0" cy="-18" r="13" fill="#FFF6E3" /></g></g></g></svg></span></button>
        </div>

        <!-- 네비게이션 메뉴 -->
        <nav class="hidden md:flex items-center space-x-8">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="font-medium transition-colors cursor-pointer"
            :class="isActive(item.key) ? 'text-red-500' : 'text-white/80 hover:text-white'"
            @click="go(item.key)"
          >
            {{ item.label }}
          </button>
        </nav>

        <!-- 검색바 -->
        <div class="flex-1 max-w-md mx-8">
          <form class="relative" @submit.prevent="submitSearch">
            <Search
              class="absolute left-3 top-1/2 transform -translate-y-1/2 text-white/60 h-4 w-4 cursor-pointer hover:text-white transition-colors z-10"
              @click="submitSearch"
            />
            <BaseInput
              v-model="searchQuery"
              type="text"
              placeholder="영화 제목, 감독, 배우를 검색하세요"
              class="pl-10 bg-gray-900/50 border-gray-700 focus:border-red-500 text-white placeholder:text-white/50"
              @keydown.enter="submitSearch"
            />
          </form>
        </div>

        <!-- 사용자 메뉴 -->
        <div class="flex items-center space-x-4">
          <BaseButton variant="ghost" size="icon">
            <Bell class="h-5 w-5 text-white/80 hover:text-white" />
          </BaseButton>
          <BaseButton variant="ghost" size="icon">
            <User class="h-5 w-5 text-white/80 hover:text-white" />
          </BaseButton>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Bell, User } from '@lucide/vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'

const props = defineProps({
  // 'home' | 'movies' | 'ranking' | 'reviews' | 'movie-detail' | 'search'
  currentPage: { type: String, default: 'home' }
})

const emit = defineEmits(['search'])
const router = useRouter()
const searchQuery = ref('')

const navItems = [
  { key: 'home', label: '홈' },
  { key: 'movies', label: '검색' },
  { key: 'ranking', label: '랭킹' },
  { key: 'reviews', label: '리뷰' }
]

function isActive(key) {
  if (key === 'movies') return props.currentPage === 'movies' || props.currentPage === 'search'
  return props.currentPage === key
}

// 검색/리뷰 페이지는 이번 범위에 없으므로 라우트가 있는 항목만 이동한다.
const ROUTES = { home: '/', ranking: '/ranking' }

function go(key) {
  const path = ROUTES[key]
  if (path) router.push(path)
}

function submitSearch() {
  const q = searchQuery.value.trim()
  if (q) emit('search', q)
}
</script>

<style scoped>
/* 로고: 오→도→독 계단식 + 독 위에 팝콘 한 알 */
.odok-logo {
  font-family: inherit;
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.01em;
  color: #FFD65A;
  padding-top: 26px;
  white-space: nowrap;
}
.odok-logo:hover { color: #FFE9A8; }

.odok-ch { display: inline-block; }
.odok-ch-last { position: relative; }

.odok-kernel {
  position: absolute;
  right: -16px;
  top: -17px;
  width: 17px;
  height: 17px;
  overflow: visible;
}

/* 독 안에서 팝! 하고 튀어나와 오른쪽 위로 날아가 자리잡음 */
@keyframes odok-pop {
  0%   { transform: translate(-15px, 16px) scale(0.15) rotate(-50deg); opacity: 0;
         animation-timing-function: cubic-bezier(.12, .9, .25, 1.6); }
  12%  { transform: translate(-9px, 7px)   scale(0.8)  rotate(-14deg); opacity: 1; }
  40%  { transform: translate(4px, -8px)   scale(1.22) rotate(22deg);  opacity: 1; }
  56%  { transform: translate(0, 0)        scale(1)    rotate(8deg);   opacity: 1; }
  86%  { transform: translate(0, 0)        scale(1)    rotate(8deg);   opacity: 1; }
  95%  { transform: translate(0, 0)        scale(1)    rotate(8deg);   opacity: 0; }
  100% { transform: translate(-15px, 16px) scale(0.15) rotate(-50deg); opacity: 0; }
}
/* 튀는 선은 터지는 순간에만 확 퍼졌다 사라짐 */
@keyframes odok-spark {
  0%, 8% { opacity: 0;    transform: scale(0.2); }
  24%    { opacity: 0.95; transform: scale(1.2); }
  46%    { opacity: 0;    transform: scale(1.8); }
  100%   { opacity: 0;    transform: scale(1.8); }
}
.odok-kernel { animation: odok-pop 2.4s ease-out infinite; }
.odok-spark {
  animation: odok-spark 2.4s ease-out infinite;
  transform-box: view-box;
  transform-origin: 40px 52px;
}
</style>
