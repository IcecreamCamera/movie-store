<template>
  <header class="bg-black/95 backdrop-blur-sm sticky top-0 z-50 border-b border-gray-800">
    <div class="max-w-7xl mx-auto px-8 lg:px-16">
      <div class="flex items-center justify-between h-16">
        <!-- 로고 -->
        <div class="flex items-center">
          <button
            class="text-2xl font-bold text-red-600 hover:text-red-500 transition-colors cursor-pointer"
            @click="go('home')"
          >
            MovieSSG
          </button>
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
