<template>
  <div class="min-h-screen bg-background">
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
            <h2 class="text-xl lg:text-2xl font-medium text-dim">당신만을 위한 추천</h2>
            <BaseButton
              variant="ghost"
              class="text-dim hover:text-white font-medium"
              @click="router.push('/ranking')"
            >
              전체 보기 <ChevronRight class="h-4 w-4 ml-1" />
            </BaseButton>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-hairline mb-6"></div>

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
            <h2 class="text-xl lg:text-2xl font-medium text-dim">최신 영화</h2>
            <BaseButton variant="ghost" class="text-dim hover:text-white font-medium">
              더보기 <ChevronRight class="h-4 w-4 ml-1" />
            </BaseButton>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-hairline mb-6"></div>

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
                  <BaseBadge class="bg-brand text-[#1A1408] text-xs">NEW</BaseBadge>
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
            <h2 class="text-xl lg:text-2xl font-medium text-dim">이번주 인기 순위</h2>
            <BaseButton
              variant="ghost"
              class="text-dim hover:text-white font-medium"
              @click="router.push('/ranking')"
            >
              전체 순위 <ChevronRight class="h-4 w-4 ml-1" />
            </BaseButton>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-hairline mb-6"></div>

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
                  class="absolute top-2 left-2 bg-brand text-[#1A1408] w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm"
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

        <!-- 4. 매점 간식 추천 (데이터 연동 전 자리) -->
        <!-- id: 헤더의 '오늘의 간식'이 여기로 스크롤한다 -->
        <div id="snacks" class="scroll-mt-24">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl lg:text-2xl font-medium text-dim">오늘의 간식 추천</h2>
            <BaseBadge class="border-brand/40 bg-brand-dim text-brand">데이터 연동 예정</BaseBadge>
          </div>
          <div class="w-full h-px bg-hairline mb-6"></div>

          <div class="rounded-xl bg-surface border border-hairline p-6">
            <p class="text-dim mb-6">
              예매하신 영화의 <span class="text-brand">장르</span>에 맞춰 매점 간식을 골라드립니다.
            </p>

            <!-- 간식 카드 -->
            <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <div
                v-for="snack in snackRecommendations"
                :key="snack.id"
                class="rounded-lg bg-surface-2 border border-hairline p-4 flex flex-col gap-3"
              >
                <div class="flex items-center justify-between gap-2">
                  <span class="font-semibold text-foreground">{{ snack.name }}</span>
                  <span class="shrink-0 rounded px-2 py-0.5 text-xs bg-brand-dim text-brand">{{ snack.taste }}</span>
                </div>
                <div class="mt-auto text-sm text-brand">{{ snack.price.toLocaleString() }}원</div>
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
import { Star, ChevronRight } from '@lucide/vue'
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
  topMovies
} from '@/data/movies'
import { snackRecommendations } from '@/data/snacks'

const router = useRouter()

function openMovie(movie) {
  router.push(`/movies/${movie.id}`)
}

function rankColor(index) {
  return index === 0 ? 'bg-brand text-[#1A1408]' : index === 1 ? 'bg-[#C9C3BA] text-[#1A1408]' : 'bg-[#9A7B4F]'
}
</script>
