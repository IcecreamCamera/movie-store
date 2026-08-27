<template>
  <div class="min-h-screen bg-background">
    <!-- 공통 헤더 -->
    <AppHeader current-page="ranking" />

    <!-- 페이지 제목 -->
    <div class="bg-surface border-b border-hairline">
      <div class="max-w-7xl mx-auto px-8 lg:px-16 py-6">
        <div class="flex items-center gap-3">
          <TrendingUp class="h-6 w-6 text-brand" />
          <h1 class="text-2xl font-bold text-foreground">영화 랭킹</h1>
        </div>
        <p class="text-dim mt-2">실시간 업데이트되는 영화 순위를 확인하세요</p>
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-8 lg:px-16 py-8">
      <!-- 로딩 -->
      <div v-if="loading" class="py-20 text-center">
        <p class="text-dim text-lg">불러오는 중...</p>
      </div>

      <!-- 에러 -->
      <div v-else-if="errorMessage" class="py-20 text-center">
        <p class="text-dim text-lg mb-2">박스오피스 정보를 불러오지 못했어요.</p>
        <p class="text-faint text-sm">{{ errorMessage }}</p>
      </div>

      <!-- 데이터 없음 -->
      <div v-else-if="rankedMovies.length < 2" class="py-20 text-center">
        <p class="text-dim text-lg mb-2">박스오피스 데이터가 아직 없어요.</p>
      </div>

      <template v-else>
      <!-- VS 섹션 - 1위 vs 2위 -->
      <div class="mb-12">
        <div class="bg-surface/80 backdrop-blur-sm rounded-2xl p-8 shadow-lg border border-hairline">
          <div class="text-center mb-8">
            <h2 class="text-3xl font-bold text-foreground mb-3">최고 평점 대결</h2>
            <p class="text-dim text-lg">이번 주 최고 평점 영화들의 투표 현황</p>
            <div class="mt-4">
              <p class="text-faint">총 {{ totalVotes.toLocaleString() }}명이 참여</p>
            </div>
          </div>

          <div class="flex flex-col md:flex-row items-center justify-center gap-8 md:gap-6 lg:gap-12">
            <!-- 1위 영화 -->
            <div class="group cursor-pointer text-center" @click="openMovie(topMovie)">
              <div class="relative mb-4">
                <div
                  class="w-40 lg:w-48 h-56 lg:h-64 rounded-xl overflow-hidden shadow-xl group-hover:shadow-2xl transition-all duration-300 group-hover:scale-105"
                >
                  <ImageWithFallback
                    :src="topMovie.poster"
                    :alt="topMovie.title"
                    class="w-full h-full object-cover"
                  />
                </div>

                <!-- 1위 배지 -->
                <div class="absolute -top-3 -left-3">
                  <div
                    class="w-12 h-12 bg-gradient-to-br from-yellow-400 to-yellow-600 rounded-full flex items-center justify-center shadow-lg"
                  >
                    <Crown class="h-6 w-6 text-white" />
                  </div>
                </div>

                <!-- 승리 표시 -->
                <div class="absolute -bottom-3 left-1/2 transform -translate-x-1/2">
                  <BaseBadge class="bg-yellow-500 text-white font-bold text-lg px-3 py-1">승리!</BaseBadge>
                </div>
              </div>

              <h3 class="font-bold text-xl text-foreground mb-2 group-hover:text-brand transition-colors">
                {{ topMovie.title }}
              </h3>
              <p class="text-dim mb-2">{{ topMovie.director }}</p>
              <div class="flex items-center justify-center gap-1 mb-3">
                <Star class="h-5 w-5 text-yellow-400 fill-current" />
                <span class="font-semibold text-xl text-foreground">{{ topMovie.rating.toFixed(1) }}</span>
              </div>

              <!-- 투표 결과 -->
              <div class="bg-yellow-600/20 rounded-lg p-4 border border-yellow-500/30">
                <div class="font-bold text-xl mb-1 text-foreground">{{ topMoviePercentage }}%</div>
                <div class="text-sm text-foreground">{{ topMovieVotes.toLocaleString() }}표</div>
              </div>
            </div>

            <!-- VS 텍스트 -->
            <div class="flex flex-col items-center">
              <div
                class="w-20 h-20 bg-gradient-to-br from-brand to-brand-hover rounded-full flex items-center justify-center shadow-2xl mb-3"
              >
                <span class="text-[#1A1408] font-bold text-2xl">VS</span>
              </div>
              <p class="text-dim mb-3">대결</p>

              <!-- 투표 진행률 표시 -->
              <div class="w-32 lg:w-40 bg-surface-2 rounded-full h-4 mb-2">
                <div
                  class="bg-gradient-to-r from-yellow-400 to-yellow-500 h-4 rounded-full transition-all duration-300"
                  :style="{ width: topMoviePercentage + '%' }"
                ></div>
              </div>
              <p class="text-xs text-faint">실시간 투표</p>
            </div>

            <!-- 2위 영화 -->
            <div class="group cursor-pointer text-center" @click="openMovie(secondMovie)">
              <div class="relative mb-4">
                <div
                  class="w-40 lg:w-48 h-56 lg:h-64 rounded-xl overflow-hidden shadow-xl group-hover:shadow-2xl transition-all duration-300 group-hover:scale-105"
                >
                  <ImageWithFallback
                    :src="secondMovie.poster"
                    :alt="secondMovie.title"
                    class="w-full h-full object-cover"
                  />
                </div>

                <!-- 2위 배지 -->
                <div class="absolute -top-3 -left-3">
                  <div
                    class="w-12 h-12 bg-gradient-to-br from-surface-2 to-surface rounded-full flex items-center justify-center shadow-lg"
                  >
                    <Medal class="h-6 w-6 text-white" />
                  </div>
                </div>

                <!-- 순위 표시 -->
                <div class="absolute -bottom-3 left-1/2 transform -translate-x-1/2">
                  <BaseBadge class="bg-surface-2 text-white font-bold text-lg px-3 py-1">2위</BaseBadge>
                </div>
              </div>

              <h3 class="font-bold text-xl text-foreground mb-2 group-hover:text-brand transition-colors">
                {{ secondMovie.title }}
              </h3>
              <p class="text-dim mb-2">{{ secondMovie.director }}</p>
              <div class="flex items-center justify-center gap-1 mb-3">
                <Star class="h-5 w-5 text-yellow-400 fill-current" />
                <span class="font-semibold text-xl text-foreground">{{ secondMovie.rating.toFixed(1) }}</span>
              </div>

              <!-- 투표 결과 -->
              <div class="bg-surface-2 rounded-lg p-4 border border-hairline">
                <div class="font-bold text-xl mb-1 text-foreground">{{ secondMoviePercentage }}%</div>
                <div class="text-sm text-foreground">{{ secondMovieVotes.toLocaleString() }}표</div>
              </div>
            </div>
          </div>

          <!-- 투표 참여 안내 -->
          <div class="mt-8 text-center">
            <div class="bg-surface-2 rounded-xl p-4 inline-block border border-hairline">
              <p class="text-brand">
                <span class="font-semibold">투표는 매주 월요일 초기화됩니다</span>
              </p>
              <p class="text-faint text-sm mt-1">다음 투표는 2일 후 시작됩니다</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 박스오피스 TOP 10 - 가로 슬라이드 -->
      <div class="mb-12">
        <div
          class="bg-surface rounded-2xl overflow-hidden shadow-lg border border-hairline"
        >
          <div class="border-b border-hairline p-6">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <Trophy class="h-7 w-7 text-brand" />
                <div>
                  <h3 class="text-2xl font-bold text-foreground">박스오피스 TOP 10</h3>
                  <p class="text-dim text-sm mt-0.5">{{ targetDateLabel || '주간 박스오피스' }}</p>
                </div>
              </div>

              <!-- 슬라이드 컨트롤 -->
              <div class="flex items-center gap-2">
                <button
                  class="w-10 h-10 bg-white/20 hover:bg-white/30 rounded-full flex items-center justify-center transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  :disabled="totalSlides <= 1"
                  @click="prevSlide"
                >
                  <ChevronLeft class="h-5 w-5 text-white" />
                </button>
                <span class="text-white/70 text-sm px-2">{{ currentSlide + 1 }} / {{ totalSlides }}</span>
                <button
                  class="w-10 h-10 bg-white/20 hover:bg-white/30 rounded-full flex items-center justify-center transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  :disabled="totalSlides <= 1"
                  @click="nextSlide"
                >
                  <ChevronRight class="h-5 w-5 text-white" />
                </button>
              </div>
            </div>
          </div>

          <div class="p-6">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <div
                v-for="movie in currentSlideMovies"
                :key="movie.id"
                class="group cursor-pointer bg-surface rounded-lg p-4 hover:bg-surface-2 transition-all duration-300 hover:scale-105 shadow-sm border border-hairline"
                @click="openMovie(movie)"
              >
                <div class="relative mb-4">
                  <div class="aspect-[2/3] rounded-lg overflow-hidden">
                    <ImageWithFallback
                      :src="movie.poster"
                      :alt="movie.title"
                      class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
                    />
                  </div>

                  <!-- 순위 배지 -->
                  <div class="absolute -top-2 -left-2">
                    <div
                      class="w-8 h-8 rounded-full flex items-center justify-center font-bold text-white text-sm shadow-lg"
                      :class="rankBg(movie.rank)"
                    >
                      {{ movie.rank }}
                    </div>
                  </div>

                  <!-- 상위 3위 아이콘 -->
                  <div v-if="movie.rank <= 3" class="absolute -top-2 -right-2">
                    <div class="w-8 h-8 bg-black/70 rounded-full flex items-center justify-center">
                      <Crown v-if="movie.rank === 1" class="h-5 w-5 text-yellow-500" />
                      <Medal v-else-if="movie.rank === 2" class="h-5 w-5 text-faint" />
                      <Trophy v-else class="h-5 w-5 text-orange-500" />
                    </div>
                  </div>
                </div>

                <div>
                  <h4
                    class="font-semibold text-foreground mb-1 line-clamp-2 group-hover:text-brand transition-colors"
                  >
                    {{ movie.title }}
                  </h4>
                  <p class="text-sm text-dim mb-2 line-clamp-1">{{ movie.director }}</p>
                  <div class="flex items-center gap-2">
                    <Star class="h-4 w-4 text-yellow-400 fill-current" />
                    <span class="text-sm font-medium text-foreground">{{ movie.rating.toFixed(1) }}</span>
                    <span class="text-sm text-faint">•</span>
                    <span class="text-sm text-faint">{{ movie.year }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 이번주 화제작 - 가로 스크롤 -->
      <div class="mb-12">
        <div
          class="bg-surface rounded-2xl overflow-hidden shadow-lg border border-hairline"
        >
          <div class="border-b border-hairline p-6">
            <div class="flex items-center gap-3">
              <TrendingUp class="h-7 w-7 text-brand" />
              <div>
                <h3 class="text-2xl font-bold text-foreground">이번주 화제작</h3>
                <p class="text-dim text-sm mt-0.5">주간 관객수 기준</p>
              </div>
            </div>
          </div>

          <div class="p-6">
            <div class="flex gap-6 overflow-x-auto scrollbar-hide pb-4">
              <div
                v-for="(movie, index) in trendingMovies"
                :key="movie.id"
                class="group cursor-pointer flex-shrink-0"
                @click="openMovie(movie)"
              >
                <div
                  class="w-48 aspect-[2/3] rounded-lg overflow-hidden shadow-lg group-hover:shadow-xl transition-all duration-300 group-hover:scale-105 relative"
                >
                  <ImageWithFallback
                    :src="movie.poster"
                    :alt="movie.title"
                    class="w-full h-full object-cover"
                  />

                  <!-- HOT 배지 -->
                  <div class="absolute top-3 left-3">
                    <div class="bg-brand text-[#1A1408] px-3 py-1 rounded-full text-xs font-bold shadow-lg">
                      HOT
                    </div>
                  </div>

                  <!-- 순위 표시 -->
                  <div class="absolute top-3 right-3">
                    <div class="w-8 h-8 bg-black/70 rounded-full flex items-center justify-center">
                      <span class="text-white font-bold text-sm">#{{ index + 1 }}</span>
                    </div>
                  </div>

                  <!-- 호버 오버레이 -->
                  <div
                    class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <div class="absolute bottom-4 left-4 right-4">
                      <div class="flex items-center gap-2 mb-2">
                        <Star class="h-4 w-4 text-yellow-400 fill-current" />
                        <span class="text-white font-semibold">{{ movie.rating.toFixed(1) }}</span>
                      </div>
                      <div class="text-white/80 text-sm">{{ movie.year }}년 • {{ movie.genre }}</div>
                    </div>
                  </div>
                </div>

                <div class="mt-4 w-48">
                  <h4
                    class="font-semibold text-foreground text-sm line-clamp-2 group-hover:text-brand transition-colors mb-2"
                  >
                    {{ movie.title }}
                  </h4>
                  <p class="text-xs text-dim line-clamp-1 mb-2">{{ movie.director }}</p>

                  <!-- 주간 관객수 -->
                  <div class="flex items-center justify-between text-xs">
                    <span class="text-brand font-medium">{{ formatAudience(movie.audienceCnt) }}명</span>
                    <span class="text-faint">주간 관객</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 화제성 통계 -->
            <div class="mt-8 pt-6 border-t border-hairline">
              <h4 class="font-semibold text-foreground mb-4">실시간 화제성 지표</h4>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div
                  v-for="stat in buzzStats"
                  :key="stat.label"
                  class="bg-surface rounded-lg p-4 border border-hairline shadow-sm"
                >
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-sm text-dim">{{ stat.label }}</span>
                    <span class="text-sm font-bold text-brand">{{ stat.value }}</span>
                  </div>
                  <div class="w-full bg-surface-2 rounded-full h-2">
                    <div class="bg-brand h-2 rounded-full" :style="{ width: stat.width }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 랭킹 정보 -->
      <div class="mt-8 text-center">
        <div class="bg-surface/80 backdrop-blur-sm rounded-2xl p-6 shadow-lg border border-hairline">
          <p class="text-dim mb-2">
            <span class="font-semibold text-foreground">랭킹 기준</span> — KOBIS 주간 박스오피스 순위
          </p>
          <p class="text-dim text-sm">{{ targetDateLabel || '박스오피스 집계 기준 업데이트' }}</p>
        </div>
      </div>
      </template>
    </div>

    <!-- 공통 푸터 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Star, TrendingUp, Crown, Medal, Trophy, ChevronLeft, ChevronRight } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import * as movieApi from '@/api/movie.js'
import { genreLabel } from '@/lib/genre.js'

const router = useRouter()

// 슬라이드 상태 관리
const currentSlide = ref(0)
const MOVIES_PER_SLIDE = 4

const loading = ref(false)
const errorMessage = ref('')
const items = ref([]) // BoxofficeItem[] (rankNo, rankInten, audienceCnt, movie)
const targetDate = ref('')

// 박스오피스 항목 하나를 화면에서 쓰기 좋은 모양으로 정리한다.
// movie-service의 MovieResponse엔 director/runtime이 없어 다른 화면(MovieListView 등)과
// 동일하게 director는 '정보 없음'으로 채운다.
function mapItem(item) {
  const m = item.movie || {}
  return {
    id: m.id,
    title: m.title,
    poster: m.posterUrl || '',
    genre: genreLabel(m.genre),
    year: m.openDt ? new Date(m.openDt).getFullYear() : undefined,
    rating: Number(m.voteAverage ?? 0),
    director: m.director || '정보 없음',
    rank: item.rankNo,
    rankInten: item.rankInten,
    audienceCnt: item.audienceCnt,
    audienceAcc: m.audienceAcc
  }
}

// GET /api/movies/boxoffice?type=WEEKLY
async function fetchBoxoffice() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await movieApi.boxoffice('WEEKLY')
    const data = res?.data?.data ?? res?.data
    targetDate.value = data?.targetDate ?? ''
    // 포스터 없는 영화(검색으로 들어와 TMDB 매칭 실패)는 랭킹 화면에서 제외한다.
    items.value = Array.isArray(data?.items)
      ? data.items.filter((it) => movieApi.hasPoster(it.movie)).map(mapItem)
      : []
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message || err?.message || '박스오피스 정보를 불러오지 못했습니다.'
    items.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchBoxoffice)

// rankNo 순으로 이미 정렬돼 오는 박스오피스 응답을 그대로 화면 순위로 쓴다.
const rankedMovies = computed(() => items.value)

const topMovie = computed(() => rankedMovies.value[0])
const secondMovie = computed(() => rankedMovies.value[1])

const boxOfficeMovies = computed(() => rankedMovies.value.slice(0, 10))
const totalSlides = computed(() => Math.ceil(boxOfficeMovies.value.length / MOVIES_PER_SLIDE))
const currentSlideMovies = computed(() => {
  const start = currentSlide.value * MOVIES_PER_SLIDE
  return boxOfficeMovies.value.slice(start, start + MOVIES_PER_SLIDE)
})

const trendingMovies = computed(() => rankedMovies.value.slice(2, 10))

// targetDate("2026-08-23")를 "8월 23일 기준"처럼 보여준다.
const targetDateLabel = computed(() => {
  if (!targetDate.value) return ''
  const d = new Date(targetDate.value)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}월 ${d.getDate()}일 기준`
})

function formatAudience(n) {
  return Number(n ?? 0).toLocaleString()
}

function nextSlide() {
  currentSlide.value = (currentSlide.value + 1) % totalSlides.value
}
function prevSlide() {
  currentSlide.value = (currentSlide.value - 1 + totalSlides.value) % totalSlides.value
}

function openMovie(movie) {
  router.push(`/movies/${movie.id}`)
}

function rankBg(rank) {
  if (rank === 1) return 'bg-yellow-500'
  if (rank === 2) return 'bg-surface-2'
  if (rank === 3) return 'bg-orange-500'
  return 'bg-surface-2'
}

// VS 대결용 투표 수 (원본과 동일한 고정값)
const topMovieVotes = 15247
const secondMovieVotes = 12893
const totalVotes = topMovieVotes + secondMovieVotes
const topMoviePercentage = Math.round((topMovieVotes / totalVotes) * 100)
const secondMoviePercentage = Math.round((secondMovieVotes / totalVotes) * 100)

const buzzStats = [
  { label: 'SNS 언급', value: '+2,547%', width: '78%' },
  { label: '검색량', value: '+1,893%', width: '65%' },
  { label: '리뷰 작성', value: '+967%', width: '42%' }
]
</script>
