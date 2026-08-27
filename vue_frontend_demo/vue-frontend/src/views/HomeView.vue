<template>
  <div class="min-h-screen bg-white">
    <!-- 공통 헤더 -->
    <AppHeader current-page="home" />

    <!-- 메인 콘텐츠 -->
    <main class="relative">
      <!-- 히어로 섹션 (Netflix 스타일) — 자동으로 옆으로 넘어가는 캐러셀 -->
      <HeroCarousel :movies="featuredMovies" @select="openMovie" />

      <!-- 섹션들 -->
      <div class="max-w-7xl mx-auto px-8 lg:px-16 pt-[100px] space-y-[100px] pb-16">
        <!-- 1. 맞춤형 추천 랭킹 TOP3 -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl lg:text-2xl font-medium text-gray-600">당신만을 위한 추천</h2>
            <BaseButton
              variant="ghost"
              class="text-gray-600 hover:text-black font-medium"
              @click="router.push('/ranking')"
            >
              전체 보기 <ChevronRight class="h-4 w-4 ml-1" />
            </BaseButton>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-gray-200 mb-6"></div>

          <div class="flex gap-4 overflow-x-auto scrollbar-hide pb-4">
            <div
              v-for="(movie, index) in personalizedTopMovies"
              :key="movie.id"
              class="group cursor-pointer flex-shrink-0 relative"
              @click="openMovie(movie)"
            >
              <div
                class="w-80 aspect-[16/9] rounded-lg overflow-hidden relative transition-transform duration-300 group-hover:scale-105"
              >
                <ImageWithFallback
                  :src="movie.poster"
                  :alt="movie.title"
                  class="w-full h-full object-cover"
                />
                <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent">
                  <div class="absolute bottom-4 left-4 right-4">
                    <div class="flex items-center gap-2 mb-2">
                      <div
                        class="w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm"
                        :class="rankColor(index)"
                      >
                        {{ index + 1 }}
                      </div>
                      <BaseBadge class="bg-white/20 text-white text-xs">맞춤 추천</BaseBadge>
                    </div>
                    <h3 class="text-white font-bold text-lg mb-1 line-clamp-1">{{ movie.title }}</h3>
                    <div class="flex items-center gap-2 text-white/80 text-sm">
                      <Star class="h-4 w-4 text-yellow-400 fill-current" />
                      <span>{{ movie.rating.toFixed(1) }}</span>
                      <span>•</span>
                      <span>{{ movie.year }}년</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 2. 최신 영화 추천리스트 -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl lg:text-2xl font-medium text-gray-600">최신 영화</h2>
            <BaseButton variant="ghost" class="text-gray-600 hover:text-black font-medium">
              더보기 <ChevronRight class="h-4 w-4 ml-1" />
            </BaseButton>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-gray-200 mb-6"></div>

          <div class="flex gap-4 overflow-x-auto scrollbar-hide pb-4">
            <div
              v-for="movie in latestMovies"
              :key="movie.id"
              class="group cursor-pointer flex-shrink-0"
              @click="openMovie(movie)"
            >
              <div
                class="w-48 aspect-[2/3] rounded-lg overflow-hidden relative transition-transform duration-300 group-hover:scale-105"
              >
                <ImageWithFallback
                  :src="movie.poster"
                  :alt="movie.title"
                  class="w-full h-full object-cover"
                />
                <div class="absolute top-2 right-2">
                  <BaseBadge class="bg-blue-600 text-white text-xs">NEW</BaseBadge>
                </div>
                <div
                  class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <div class="absolute bottom-3 left-3 right-3">
                    <h4 class="text-white font-semibold text-sm mb-1 line-clamp-2">{{ movie.title }}</h4>
                    <div class="flex items-center gap-1 text-white/80 text-xs">
                      <Star class="h-3 w-3 text-yellow-400 fill-current" />
                      <span>{{ movie.rating.toFixed(1) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 3. 이번주 우수작 TOP5 -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl lg:text-2xl font-medium text-gray-600">이번주 인기 순위</h2>
            <BaseButton
              variant="ghost"
              class="text-gray-600 hover:text-black font-medium"
              @click="router.push('/ranking')"
            >
              전체 순위 <ChevronRight class="h-4 w-4 ml-1" />
            </BaseButton>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-gray-200 mb-6"></div>

          <div class="flex gap-4 overflow-x-auto scrollbar-hide pb-4">
            <div
              v-for="(movie, index) in topMovies"
              :key="movie.id"
              class="group cursor-pointer flex-shrink-0 relative"
              @click="openMovie(movie)"
            >
              <div
                class="w-48 aspect-[2/3] rounded-lg overflow-hidden relative transition-transform duration-300 group-hover:scale-105"
              >
                <ImageWithFallback
                  :src="movie.poster"
                  :alt="movie.title"
                  class="w-full h-full object-cover"
                />
                <div
                  class="absolute top-2 left-2 bg-red-600 text-white w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm"
                >
                  {{ index + 1 }}
                </div>
                <div
                  class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <div class="absolute bottom-3 left-3 right-3">
                    <h4 class="text-white font-semibold text-sm mb-1 line-clamp-2">{{ movie.title }}</h4>
                    <div class="flex items-center gap-1 text-white/80 text-xs">
                      <Star class="h-3 w-3 text-yellow-400 fill-current" />
                      <span>{{ movie.rating.toFixed(1) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. 영화 리뷰 이벤트 -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl lg:text-2xl font-medium text-gray-600">리뷰 이벤트</h2>
            <BaseBadge class="bg-purple-600 text-white">진행중</BaseBadge>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-gray-200 mb-6"></div>

          <div class="bg-gray-100 rounded-xl p-6">
            <p class="text-gray-700 mb-6">영화 리뷰를 작성하고 특별한 혜택을 받아보세요!</p>
            <div class="flex gap-4 overflow-x-auto scrollbar-hide pb-4">
              <div
                v-for="movie in reviewEventMovies"
                :key="movie.id"
                class="group cursor-pointer flex-shrink-0"
                @click="openMovie(movie)"
              >
                <div
                  class="w-48 aspect-[2/3] rounded-lg overflow-hidden relative transition-transform duration-300 group-hover:scale-105"
                >
                  <ImageWithFallback
                    :src="movie.poster"
                    :alt="movie.title"
                    class="w-full h-full object-cover"
                  />
                  <div
                    class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <div class="absolute bottom-3 left-3 right-3">
                      <h4 class="text-white font-semibold text-sm mb-1 line-clamp-2">{{ movie.title }}</h4>
                      <div class="flex items-center gap-1 text-white/80 text-xs mb-2">
                        <Star class="h-3 w-3 text-yellow-400 fill-current" />
                        <span>{{ movie.rating.toFixed(1) }}</span>
                      </div>
                      <BaseButton size="sm" class="w-full bg-purple-600 hover:bg-purple-700 text-xs">
                        <MessageSquare class="h-3 w-3 mr-1" />
                        리뷰 작성
                      </BaseButton>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 공통 푸터 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Star, ChevronRight, MessageSquare } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import HeroCarousel from '@/components/HeroCarousel.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import {
  featuredMovies,
  personalizedTopMovies,
  latestMovies,
  topMovies,
  reviewEventMovies
} from '@/data/movies'

const router = useRouter()

function openMovie(movie) {
  router.push(`/movies/${movie.id}`)
}

function rankColor(index) {
  return index === 0 ? 'bg-red-600' : index === 1 ? 'bg-orange-600' : 'bg-yellow-600'
}
</script>
