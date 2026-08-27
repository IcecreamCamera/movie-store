<template>
  <div class="min-h-screen bg-background">
    <AppHeader :current-page="isSearch ? 'search' : 'booking'" @search="onSearch" />

    <!-- 페이지 헤더 -->
    <div class="bg-surface border-b border-hairline">
      <div class="max-w-7xl mx-auto px-8 lg:px-16 py-6">
        <div class="flex items-center gap-3">
          <component :is="isSearch ? Search : Ticket" class="h-6 w-6 text-brand" />
          <h1 class="text-2xl font-bold text-foreground">
            {{ isSearch ? `'${query}' 검색 결과` : '예매하기' }}
          </h1>
        </div>
        <p class="text-dim mt-2">
          {{ isSearch
            ? `${results.length}편을 찾았어요. 골라서 바로 예매할 수 있어요.`
            : '보고 싶은 영화를 고르면 바로 예매할 수 있어요.' }}
        </p>
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-8 lg:px-16 py-8">
      <!-- 장르 필터 -->
      <div class="flex flex-wrap items-center gap-2 mb-8">
        <button
          v-for="g in genreOptions"
          :key="g"
          class="rounded-full px-4 py-1.5 text-sm transition-colors cursor-pointer"
          :class="g === genre
            ? 'bg-brand text-[#1A1408] font-medium'
            : 'border border-hairline text-dim hover:text-foreground hover:border-brand/40'"
          @click="genre = g"
        >{{ g }}</button>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="py-20 text-center">
        <p class="text-dim text-lg">불러오는 중...</p>
      </div>

      <!-- 에러 -->
      <div v-else-if="errorMessage" class="py-20 text-center">
        <p class="text-dim text-lg mb-2">영화 목록을 불러오지 못했어요.</p>
        <p class="text-faint text-sm">{{ errorMessage }}</p>
      </div>

      <!-- 결과 없음 -->
      <div v-else-if="results.length === 0" class="py-20 text-center">
        <p class="text-dim text-lg mb-2">조건에 맞는 영화가 없어요.</p>
        <p class="text-faint text-sm">다른 검색어나 장르로 찾아보세요.</p>
      </div>

      <!-- 목록 -->
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
        <div
          v-for="movie in results"
          :key="movie.id"
          class="group cursor-pointer"
          @click="openMovie(movie)"
        >
          <div class="aspect-[2/3] rounded-lg overflow-hidden relative mb-3 transition-transform duration-300 group-hover:scale-105">
            <ImageWithFallback
              :src="movie.poster"
              :alt="movie.title"
              class="w-full h-full object-cover"
            />
            <div
              class="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end justify-center pb-4"
            >
              <span class="rounded-full bg-brand text-[#1A1408] text-sm font-semibold px-4 py-1.5">
                예매하기
              </span>
            </div>
          </div>
          <h3 class="font-semibold text-foreground text-sm line-clamp-1 group-hover:text-brand transition-colors">
            {{ movie.title }}
          </h3>
          <div class="flex items-center gap-2 text-xs text-dim mt-1">
            <Star class="h-3 w-3 text-yellow-400 fill-current" />
            <span>{{ movie.rating.toFixed(1) }}</span>
            <span class="text-faint">·</span>
            <span>{{ movie.genre }}</span>
          </div>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Ticket, Star } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import * as movieApi from '@/api/movie.js'
import { GENRE_OPTIONS, genreLabel, genreCode } from '@/lib/genre.js'

const route = useRoute()
const router = useRouter()

const ALL = '전체'
const genre = ref(ALL)

const loading = ref(false)
const errorMessage = ref('')
const movies = ref([])

const isSearch = computed(() => route.name === 'Search')
const query = computed(() => String(route.query.q ?? '').trim())

const genreOptions = computed(() => [ALL, ...GENRE_OPTIONS.map(genreLabel)])

function mapMovie(m) {
  return {
    id: m.id,
    title: m.title,
    poster: m.posterUrl || '',
    genre: genreLabel(m.genre),
    year: m.openDt ? new Date(m.openDt).getFullYear() : undefined,
    rating: Number(m.voteAverage ?? 0),
    director: m.director || '',
    actors: m.actors || ''
  }
}

// GET /api/movies (전체) / GET /api/movies/genre/{genre} (장르별) / GET /api/movies/search?q= (검색, Search 라우트 전용)
async function fetchMovies() {
  loading.value = true
  errorMessage.value = ''
  try {
    let res
    if (isSearch.value) {
      if (!query.value) {
        movies.value = []
        return
      }
      res = await movieApi.search(query.value)
    } else if (genre.value !== ALL) {
      res = await movieApi.byGenre(genreCode(genre.value))
    } else {
      res = await movieApi.list()
    }
    const data = res?.data?.data ?? res?.data ?? []
    movies.value = Array.isArray(data) ? data.map(mapMovie) : []
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message || err?.message || '영화 목록을 불러오지 못했습니다.'
    movies.value = []
  } finally {
    loading.value = false
  }
}

// 검색 결과 위에서도 장르 칩으로 한 번 더 걸러볼 수 있게 둔다 (서버 응답에 대한 클라이언트 필터).
const results = computed(() => {
  if (isSearch.value && genre.value !== ALL) {
    return movies.value.filter((m) => m.genre === genre.value)
  }
  return movies.value
})

// 검색어가 바뀌면 장르 필터는 풀어준다. 결과가 0이 되는 걸 막기 위해서.
watch(query, () => { genre.value = ALL })

watch([isSearch, query, genre], fetchMovies, { immediate: true })

function onSearch(q) {
  router.push({ name: 'Search', query: { q } })
}

function openMovie(movie) {
  router.push(`/movies/${movie.id}`)
}
</script>
