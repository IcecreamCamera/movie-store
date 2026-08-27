<template>
  <div class="min-h-screen bg-background">
    <!-- 공통 헤더 -->
    <AppHeader current-page="home" />

    <!-- 메인 콘텐츠 -->
    <main class="relative">
      <!-- 히어로 섹션 (캐러셀) : GET /api/movies/boxoffice?type=WEEKLY 상위 5편 -->
      <div v-if="boxofficeLoading" class="h-[70vh] flex items-center justify-center">
        <p class="text-dim text-lg">불러오는 중...</p>
      </div>
      <div v-else-if="boxofficeError" class="h-[70vh] flex flex-col items-center justify-center gap-2">
        <p class="text-dim text-lg">박스오피스 정보를 불러오지 못했어요.</p>
        <p class="text-faint text-sm">{{ boxofficeError }}</p>
      </div>
      <div v-else-if="featuredMovies.length === 0" class="h-[70vh] flex items-center justify-center">
        <p class="text-dim text-lg">박스오피스 데이터가 아직 없어요.</p>
      </div>
      <HeroCarousel v-else :movies="featuredMovies" @select="openMovie" />

      <!-- 섹션들 -->
      <div class="max-w-7xl mx-auto px-8 lg:px-16 pt-[100px] space-y-[100px] pb-16">
        <!-- 1. 맞춤형 추천 랭킹 TOP3 -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl lg:text-2xl font-medium text-dim">{{ personalizedTitle }}</h2>
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

          <div v-if="personalizedLoading" class="py-10 text-center">
            <p class="text-dim">불러오는 중...</p>
          </div>
          <div v-else-if="personalizedError" class="py-10 text-center">
            <p class="text-dim mb-1">추천 정보를 불러오지 못했어요.</p>
            <p class="text-faint text-sm">{{ personalizedError }}</p>
          </div>
          <div v-else-if="personalizedTopMovies.length === 0" class="py-10 text-center">
            <p class="text-dim">아직 추천할 영화가 없어요.</p>
          </div>
          <div v-else class="flex gap-4 overflow-x-auto scrollbar-hide pb-4">
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
                      <BaseBadge class="bg-white/20 text-white text-xs">{{ personalizedBadge }}</BaseBadge>
                    </div>
                    <h3 class="text-white font-bold text-lg mb-1 line-clamp-1">{{ movie.title }}</h3>
                    <div class="flex items-center gap-2 text-white/80 text-sm">
                      <Star class="h-4 w-4 text-yellow-400 fill-current" />
                      <span>{{ movie.rating.toFixed(1) }}</span>
                      <template v-if="movie.year">
                        <span>•</span>
                        <span>{{ movie.year }}년</span>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 2. 최신 영화 추천리스트 : GET /api/movies, openDt 내림차순 상위 6편 -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl lg:text-2xl font-medium text-dim">최신 영화</h2>
            <BaseButton variant="ghost" class="text-dim hover:text-white font-medium">
              더보기 <ChevronRight class="h-4 w-4 ml-1" />
            </BaseButton>
          </div>
          <!-- 밑줄 추가 -->
          <div class="w-full h-px bg-hairline mb-6"></div>

          <div v-if="latestLoading" class="py-10 text-center">
            <p class="text-dim">불러오는 중...</p>
          </div>
          <div v-else-if="latestError" class="py-10 text-center">
            <p class="text-dim mb-1">최신 영화를 불러오지 못했어요.</p>
            <p class="text-faint text-sm">{{ latestError }}</p>
          </div>
          <div v-else-if="latestMovies.length === 0" class="py-10 text-center">
            <p class="text-dim">표시할 영화가 없어요.</p>
          </div>
          <div v-else class="flex gap-4 overflow-x-auto scrollbar-hide pb-4">
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

        <!-- 3. 이번주 우수작 TOP5 : boxoffice WEEKLY 응답을 히어로와 함께 재사용, rankNo를 순위로 표시 -->
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

          <div v-if="boxofficeLoading" class="py-10 text-center">
            <p class="text-dim">불러오는 중...</p>
          </div>
          <div v-else-if="boxofficeError" class="py-10 text-center">
            <p class="text-dim mb-1">박스오피스 정보를 불러오지 못했어요.</p>
            <p class="text-faint text-sm">{{ boxofficeError }}</p>
          </div>
          <div v-else-if="topMovies.length === 0" class="py-10 text-center">
            <p class="text-dim">박스오피스 데이터가 아직 없어요.</p>
          </div>
          <div v-else class="flex gap-4 overflow-x-auto scrollbar-hide pb-4">
            <div
              v-for="movie in topMovies"
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
                  {{ movie.rankNo }}
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
      </div>
    </main>

    <!-- 공통 푸터 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Star, ChevronRight } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import HeroCarousel from '@/components/HeroCarousel.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import * as movieApi from '@/api/movie.js'
import * as bookingApi from '@/api/booking.js'
import { isLoggedIn } from '@/store/auth'
import { genreLabel } from '@/lib/genre.js'

const router = useRouter()

function openMovie(movie) {
  router.push(`/movies/${movie.id}`)
}

function rankColor(index) {
  return index === 0 ? 'bg-brand text-[#1A1408]' : index === 1 ? 'bg-[#C9C3BA] text-[#1A1408]' : 'bg-[#9A7B4F]'
}

// ── 1) 히어로 + 이번주 인기 순위 : GET /api/movies/boxoffice?type=WEEKLY 를 한 번만 호출해 공유한다 ──
const boxofficeLoading = ref(false)
const boxofficeError = ref('')
const featuredMovies = ref([])
const topMovies = ref([])

async function fetchBoxoffice() {
  boxofficeLoading.value = true
  boxofficeError.value = ''
  try {
    const res = await movieApi.boxoffice('WEEKLY')
    const data = res?.data?.data ?? res?.data
    const items = Array.isArray(data?.items) ? data.items : []
    // 포스터 없는 영화(검색으로 들어와 TMDB 매칭 실패)는 목록에서 제외한다.
    const withPoster = items.filter((it) => movieApi.hasPoster(it.movie))

    featuredMovies.value = withPoster.slice(0, 5).map((it) => movieApi.toViewMovie(it.movie))
    topMovies.value = withPoster
      .slice(0, 10)
      .map((it) => ({ ...movieApi.toViewMovie(it.movie), rankNo: it.rankNo }))
  } catch (err) {
    boxofficeError.value =
      err?.response?.data?.message || err?.message || '박스오피스 정보를 불러오지 못했습니다.'
    featuredMovies.value = []
    topMovies.value = []
  } finally {
    boxofficeLoading.value = false
  }
}

// ── 2) 최신 영화 : GET /api/movies, openDt 내림차순 상위 6편 ──
const latestLoading = ref(false)
const latestError = ref('')
const latestMovies = ref([])

async function fetchLatest() {
  latestLoading.value = true
  latestError.value = ''
  try {
    const res = await movieApi.list()
    const data = res?.data?.data ?? res?.data ?? []
    const list = Array.isArray(data) ? data : []
    latestMovies.value = list
      .filter(movieApi.hasPoster)
      .slice()
      .sort((a, b) => new Date(b.openDt ?? 0) - new Date(a.openDt ?? 0))
      .slice(0, 6)
      .map(movieApi.toViewMovie)
  } catch (err) {
    latestError.value =
      err?.response?.data?.message || err?.message || '최신 영화 정보를 불러오지 못했습니다.'
    latestMovies.value = []
  } finally {
    latestLoading.value = false
  }
}

// ── 3) "당신만을 위한 추천" ──
// 추천 API가 없다(recommend-service는 간식 추천으로 이관됨). 그래서:
//  - 로그인 + 예매 이력이 있으면: 가장 최근 예매한 영화의 장르로 GET /api/movies/genre/{genre}를
//    조회해 그 영화만 뺀 나머지를 최대 3편 보여준다. 라벨도 "{장르} 좋아하시죠?"처럼 정직하게 붙인다.
//  - 그 외(비로그인/예매 이력 없음/실패)에는 GET /api/movies/boxoffice?type=DAILY 상위 3편을
//    일반 인기 추천으로 보여준다.
const personalizedLoading = ref(false)
const personalizedError = ref('')
const personalizedTopMovies = ref([])
const personalizedLabel = ref('당신만을 위한 추천')
const personalizedBadge = ref('맞춤 추천')

const personalizedTitle = computed(() => personalizedLabel.value)

async function fetchGeneralRecommendation() {
  try {
    const res = await movieApi.boxoffice('DAILY')
    const data = res?.data?.data ?? res?.data
    const items = Array.isArray(data?.items) ? data.items : []
    personalizedTopMovies.value = items
      .filter((it) => movieApi.hasPoster(it.movie))
      .slice(0, 3)
      .map((it) => movieApi.toViewMovie(it.movie))
    personalizedLabel.value = '지금 가장 인기 있는 영화'
    personalizedBadge.value = '인기 급상승'
  } catch (err) {
    personalizedTopMovies.value = []
    personalizedLabel.value = '지금 가장 인기 있는 영화'
    personalizedError.value =
      err?.response?.data?.message || err?.message || '추천 정보를 불러오지 못했습니다.'
  }
}

async function fetchPersonalized() {
  personalizedLoading.value = true
  personalizedError.value = ''
  try {
    if (isLoggedIn.value) {
      const myRes = await bookingApi.my()
      const bookings = myRes?.data?.data ?? myRes?.data ?? []
      const list = Array.isArray(bookings) ? bookings : []
      if (list.length > 0) {
        const recent = [...list].sort(
          (a, b) => new Date(b.createdAt ?? 0) - new Date(a.createdAt ?? 0)
        )[0]
        const recentMovie = recent?.movie
        if (recentMovie?.genre) {
          const genreRes = await movieApi.byGenre(recentMovie.genre)
          const data = genreRes?.data?.data ?? genreRes?.data ?? []
          const genreList = Array.isArray(data) ? data : []
          personalizedTopMovies.value = genreList
            .filter(movieApi.hasPoster)
            .filter((m) => m.id !== recentMovie.id)
            .slice(0, 3)
            .map(movieApi.toViewMovie)
          personalizedLabel.value = `${genreLabel(recentMovie.genre)} 좋아하시죠?`
          personalizedBadge.value = '맞춤 추천'
          return
        }
      }
    }
    await fetchGeneralRecommendation()
  } catch (err) {
    await fetchGeneralRecommendation()
  } finally {
    personalizedLoading.value = false
  }
}

onMounted(() => {
  fetchBoxoffice()
  fetchLatest()
  fetchPersonalized()
})
</script>
