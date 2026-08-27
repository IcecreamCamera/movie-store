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
      <section v-if="latest" class="mb-12">
        <h2 class="text-lg font-semibold text-foreground mb-1">최근 예매하신 영화 기준</h2>
        <p class="text-dim text-sm mb-5">
          <span class="text-foreground">{{ latest.movieTitle }}</span>
          <span class="text-faint"> · </span>
          <span class="text-brand">{{ latest.genre }}</span>
        </p>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <SnackCard v-for="s in latestSnacks" :key="s.id" :snack="s" />
        </div>
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Popcorn } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import SnackCard from '@/components/SnackCard.vue'
import { recommendByGenre, mappedGenres as genres, allSnacks, SNACK_PLACEHOLDER_NOTE } from '@/data/snacks'
import { bookings } from '@/store/bookings'

const router = useRouter()

const picked = ref('액션')

// TODO: GET /api/recommend/{userId} 로 교체 (movieId 없이 호출하면 이력 기반).
const latest = computed(() => bookings.value[0] ?? null)
const latestSnacks = computed(() => recommendByGenre(latest.value?.genre))
const pickedSnacks = computed(() => recommendByGenre(picked.value))

function onSearch(q) {
  router.push({ name: 'Search', query: { q } })
}
</script>
