<template>
  <div class="min-h-screen bg-background">
    <AppHeader current-page="snacks" @search="onSearch" />

    <div class="bg-surface border-b border-hairline">
      <div class="max-w-7xl mx-auto px-8 lg:px-16 py-6">
        <div class="flex items-center gap-3">
          <Popcorn class="h-6 w-6 text-brand" />
          <h1 class="text-2xl font-bold text-foreground">간식 추천</h1>
        </div>
        <p class="text-dim mt-2">영화 장르에 어울리는 매점 간식을 골라드려요.</p>
      </div>
    </div>

    <div class="max-w-5xl mx-auto px-8 lg:px-16 py-8">
      <!-- 최근 예매 기준 추천 -->
      <section class="mb-12">
        <!-- 로딩 -->
        <div v-if="historyLoading" class="py-6 text-dim text-sm">추천을 불러오는 중...</div>

        <!-- 예매 이력 기반 추천 -->
        <template v-else-if="!historyFallback">
          <h2 class="text-lg font-semibold text-foreground mb-1">최근 예매하신 영화 기준</h2>
          <p class="text-dim text-sm mb-5">
            <span class="text-foreground">{{ historyMovieTitle }}</span>
            <span class="text-faint"> · </span>
            <span class="text-brand">{{ historyGenreLabel }}</span>
          </p>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <SnackCard v-for="s in historySnacks" :key="s.id" :snack="s" />
          </div>
        </template>

        <!-- 폴백: 로그인 안 함 / 예매 이력 없음 / 추천 실패 -->
        <template v-else>
          <h2 class="text-lg font-semibold text-foreground mb-1">일반 추천 간식</h2>
          <p class="text-dim text-sm mb-5">예매 이력이 없어 일반적으로 인기 있는 간식을 보여드려요.</p>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <SnackCard v-for="s in fallbackSnacks" :key="s.id" :snack="s" />
          </div>
        </template>
      </section>

      <!-- 장르별로 둘러보기 -->
      <section>
        <h2 class="text-lg font-semibold text-foreground mb-1">장르별로 둘러보기</h2>
        <p class="text-dim text-sm mb-5">궁금한 장르를 눌러보세요.</p>

        <div class="flex flex-wrap items-center gap-2 mb-6">
          <button
            v-for="g in genres"
            :key="g"
            class="rounded-full px-4 py-1.5 text-sm transition-colors cursor-pointer"
            :class="g === picked
              ? 'bg-brand text-[#1A1408] font-medium'
              : 'border border-hairline text-dim hover:text-foreground hover:border-brand/40'"
            @click="picked = g"
          >{{ g }}</button>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <SnackCard v-for="s in pickedSnacks" :key="s.id" :snack="s" />
        </div>
      </section>

      <!-- 전체 메뉴 -->
      <section class="mt-12">
        <h2 class="text-lg font-semibold text-foreground mb-1">전체 메뉴</h2>
        <p class="text-dim text-sm mb-5">매점에서 파는 {{ allSnacks.length }}가지예요.</p>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <SnackCard v-for="s in allSnacks" :key="s.id" :snack="s" />
        </div>
      </section>

      <p class="text-faint text-xs mt-10">※ {{ SNACK_PLACEHOLDER_NOTE }}</p>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Popcorn } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import SnackCard from '@/components/SnackCard.vue'
import { recommendByGenre, mappedGenres as genres, allSnacks, SNACK_PLACEHOLDER_NOTE } from '@/data/snacks'
import * as bookingApi from '@/api/booking.js'
import * as recommendApi from '@/api/recommend.js'
import { user, isLoggedIn } from '@/store/auth'
import { genreLabel } from '@/lib/genre.js'

const router = useRouter()

const picked = ref('액션')

const historyLoading = ref(false)
const historyFallback = ref(false)
const historyMovieTitle = ref('')
const historyGenreLabel = ref('')
const historySnacks = ref([])

// recommend-service의 Food -> 화면에서 쓰는 모양으로 정리한다 (description 없음, taste는 배열).
function mapFood(f) {
  return {
    id: f.id,
    name: f.name,
    price: Number(f.price ?? 0),
    taste: Array.isArray(f.taste) ? f.taste.join(', ') : (f.taste ?? ''),
    reason: f.reason || null
  }
}

// movie_id가 필수라 "이력 기반" 추천은 더 이상 movieId 없이 부를 수 없다.
// 대신 내 예매 이력(GET /api/bookings/my) 중 가장 최근 예매의 movieId로
// GET /api/recommend/{userId}?movie_id={movieId}&limit=5 를 호출해 같은 의도를 구현한다.
// 로그인 안 함 / 예매 이력 없음 / 호출 실패 -> snacks.js 카탈로그로 대체.
async function loadHistoryRecommendation() {
  historyLoading.value = true
  historyFallback.value = false
  try {
    if (!isLoggedIn.value || !user.value?.id) {
      throw new Error('로그인이 필요합니다.')
    }

    const res = await bookingApi.my()
    const list = res?.data?.data ?? res?.data ?? []
    const sorted = [...(Array.isArray(list) ? list : [])].sort(
      (a, b) => new Date(b.createdAt ?? 0) - new Date(a.createdAt ?? 0)
    )
    const latestBooking = sorted[0]
    if (!latestBooking?.movieId) {
      throw new Error('예매 이력이 없습니다.')
    }

    const recRes = await recommendApi.recommend(user.value.id, latestBooking.movieId, 5)
    const data = recRes?.data?.data ?? recRes?.data
    const foods = Array.isArray(data?.recommendedFoods) ? data.recommendedFoods : []
    if (foods.length === 0) {
      throw new Error('추천 결과가 없습니다.')
    }

    historyMovieTitle.value = latestBooking.movie?.title ?? ''
    historyGenreLabel.value = data?.basedOnGenre
      ? genreLabel(data.basedOnGenre)
      : genreLabel(latestBooking.movie?.genre)
    historySnacks.value = foods.map(mapFood)
  } catch {
    historyFallback.value = true
  } finally {
    historyLoading.value = false
  }
}

onMounted(loadHistoryRecommendation)

// 폴백 화면에서 쓰는 일반 추천 세트 (예매 이력이 없을 때 기본으로 노출하는 것과 동일한 세트).
const fallbackSnacks = computed(() => allSnacks.slice(0, 8))

const pickedSnacks = computed(() => recommendByGenre(picked.value))

function onSearch(q) {
  router.push({ name: 'Search', query: { q } })
}
</script>
