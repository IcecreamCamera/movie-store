<template>
  <header class="bg-black/95 backdrop-blur-sm sticky top-0 z-50 border-b border-gray-800">
    <!-- 아이콘을 창 오른쪽 끝에 붙이려고 바 내용은 전체 폭을 쓴다. 로고 위치는 기존과 동일. -->
    <div class="w-full pl-8 lg:pl-16 pr-6 lg:pr-8">
      <div class="flex items-center gap-6 lg:gap-8 h-20">
        <!-- 로고 -->
        <div class="flex items-center shrink-0">
          <button
            class="odok-logo cursor-pointer transition-colors"
            aria-label="오도독 홈으로"
            @click="go('home')"
          ><span class="odok-ch" style="transform: translateY(0)">오</span><span class="odok-ch" style="transform: translateY(-4px)">도</span><span class="odok-ch odok-ch-last" style="transform: translateY(-8px)">독<svg class="odok-kernel" viewBox="0 0 80 80" aria-hidden="true"><g class="odok-spark" stroke="currentColor" stroke-width="6" stroke-linecap="round" fill="none"><line x1="13" y1="25" x2="21" y2="33" /><line x1="40" y1="9" x2="40" y2="20" /><line x1="67" y1="25" x2="59" y2="33" /></g><g transform="translate(40,52)"><g class="odok-pop"><g transform="scale(0.82)"><circle cx="0" cy="0" r="19" fill="#FFF6E3" /><circle cx="-15" cy="-9" r="14" fill="#FFF6E3" /><circle cx="15" cy="-9" r="14" fill="#FFF6E3" /><circle cx="-11" cy="11" r="13" fill="#FFF6E3" /><circle cx="12" cy="11" r="13" fill="#FFF6E3" /><circle cx="0" cy="-18" r="13" fill="#FFF6E3" /></g></g></g></svg></span></button>
        </div>

        <!-- 네비게이션 메뉴 -->
        <nav class="hidden lg:flex items-center gap-6 shrink-0">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="font-medium transition-colors cursor-pointer"
            :class="isActive(item.key) ? 'text-white font-semibold' : 'text-white/55 hover:text-white/90'"
            @click="go(item.key)"
          >
            {{ item.label }}
          </button>
        </nav>

        <!-- 검색바 -->
        <div class="flex-1 min-w-0 max-w-md">
          <form class="relative" @submit.prevent="submitSearch">
            <Search
              class="absolute left-3 top-1/2 transform -translate-y-1/2 text-white/60 h-4 w-4 cursor-pointer hover:text-white transition-colors z-10"
              @click="submitSearch"
            />
            <BaseInput
              v-model="searchQuery"
              type="text"
              placeholder="영화를 검색하세요"
              class="pl-10 bg-gray-900/50 border-gray-700 focus:border-brand text-white placeholder:text-white/50"
              @keydown.enter="submitSearch"
            />
          </form>
        </div>

        <!-- 사용자 메뉴 -->
        <div class="flex items-center gap-2 shrink-0 ml-auto">
          <BaseButton variant="ghost" size="icon">
            <Bell class="h-5 w-5 text-white/80 hover:text-white" />
          </BaseButton>
          <BaseButton variant="ghost" size="icon" aria-label="마이페이지" @click="router.push('/mypage')">
            <User class="h-5 w-5 text-white/80 hover:text-white" />
          </BaseButton>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Bell, User } from '@lucide/vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'

const props = defineProps({
  // 'home' | 'ranking' | 'movie-detail'
  currentPage: { type: String, default: 'home' }
})

const emit = defineEmits(['search'])
const router = useRouter()
const route = useRoute()
const searchQuery = ref('')

const navItems = [
  { key: 'home', label: '홈' },
  { key: 'booking', label: '예매' },
  { key: 'ranking', label: '랭킹' },
  { key: 'snacks', label: '간식추천' }
]

function isActive(key) {
  if (key === 'booking') return props.currentPage === 'booking' || props.currentPage === 'search'
  return props.currentPage === key
}

const ROUTES = {
  home: '/',
  booking: '/booking',
  ranking: '/ranking',
  snacks: '/snacks'
}

function go(key) {
  const target = ROUTES[key]
  if (!target) return

  // 이미 같은 화면이면 라우터가 아무것도 하지 않으므로 맨 위로 올려준다.
  if (router.resolve(target).fullPath === route.fullPath) {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }

  router.push(target)
}

function submitSearch() {
  const q = searchQuery.value.trim()
  if (!q) return
  emit('search', q)
  router.push({ name: 'Search', query: { q } })
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
