<template>
  <div class="min-h-screen" style="background-color: #ffffff">
    <!-- 공통 헤더 -->
    <AppHeader current-page="ranking" />

    <!-- 페이지 제목 -->
    <div style="background-color: #e4e4e4">
      <div class="max-w-7xl mx-auto px-8 lg:px-16 py-6">
        <div class="flex items-center gap-3">
          <TrendingUp class="h-6 w-6 text-red-600" />
          <h1 class="text-2xl font-bold text-black">영화 랭킹</h1>
        </div>
        <p class="text-black/70 mt-2">실시간 업데이트되는 영화 순위를 확인하세요</p>
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-8 lg:px-16 py-8">
      <!-- VS 섹션 - 1위 vs 2위 -->
      <div class="mb-12">
        <div class="bg-gray-100/50 backdrop-blur-sm rounded-2xl p-8 shadow-lg border border-gray-200/30">
          <div class="text-center mb-8">
            <h2 class="text-3xl font-bold text-gray-800 mb-3">최고 평점 대결</h2>
            <p class="text-gray-600 text-lg">이번 주 최고 평점 영화들의 투표 현황</p>
            <div class="mt-4">
              <p class="text-gray-500">총 {{ totalVotes.toLocaleString() }}명이 참여</p>
            </div>
          </div>

          <div class="flex items-center justify-center gap-12">
            <!-- 1위 영화 -->
            <div class="group cursor-pointer text-center" @click="openMovie(topMovie)">
              <div class="relative mb-4">
                <div
                  class="w-48 h-64 rounded-xl overflow-hidden shadow-xl group-hover:shadow-2xl transition-all duration-300 group-hover:scale-105"
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

              <h3 class="font-bold text-xl text-gray-800 mb-2 group-hover:text-red-500 transition-colors">
                {{ topMovie.title }}
              </h3>
              <p class="text-gray-600 mb-2">{{ topMovie.director }}</p>
              <div class="flex items-center justify-center gap-1 mb-3">
                <Star class="h-5 w-5 text-yellow-400 fill-current" />
                <span class="font-semibold text-xl text-gray-800">{{ topMovie.rating.toFixed(1) }}</span>
              </div>

              <!-- 투표 결과 -->
              <div class="bg-yellow-600/20 rounded-lg p-4 border border-yellow-500/30">
                <div class="font-bold text-xl mb-1" style="color: #000000">{{ topMoviePercentage }}%</div>
                <div class="text-sm" style="color: #000000">{{ topMovieVotes.toLocaleString() }}표</div>
              </div>
            </div>

            <!-- VS 텍스트 -->
            <div class="flex flex-col items-center">
              <div
                class="w-20 h-20 bg-gradient-to-br from-red-600 to-red-700 rounded-full flex items-center justify-center shadow-2xl mb-3"
              >
                <span class="text-white font-bold text-2xl">VS</span>
              </div>
              <p class="text-gray-600 mb-3">대결</p>

              <!-- 투표 진행률 표시 -->
              <div class="w-40 bg-gray-700 rounded-full h-4 mb-2">
                <div
                  class="bg-gradient-to-r from-yellow-400 to-yellow-500 h-4 rounded-full transition-all duration-300"
                  :style="{ width: topMoviePercentage + '%' }"
                ></div>
              </div>
              <p class="text-xs text-gray-500">실시간 투표</p>
            </div>

            <!-- 2위 영화 -->
            <div class="group cursor-pointer text-center" @click="openMovie(secondMovie)">
              <div class="relative mb-4">
                <div
                  class="w-48 h-64 rounded-xl overflow-hidden shadow-xl group-hover:shadow-2xl transition-all duration-300 group-hover:scale-105"
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
                    class="w-12 h-12 bg-gradient-to-br from-gray-300 to-gray-500 rounded-full flex items-center justify-center shadow-lg"
                  >
                    <Medal class="h-6 w-6 text-white" />
                  </div>
                </div>

                <!-- 순위 표시 -->
                <div class="absolute -bottom-3 left-1/2 transform -translate-x-1/2">
                  <BaseBadge class="bg-gray-400 text-white font-bold text-lg px-3 py-1">2위</BaseBadge>
                </div>
              </div>

              <h3 class="font-bold text-xl text-gray-800 mb-2 group-hover:text-red-500 transition-colors">
                {{ secondMovie.title }}
              </h3>
              <p class="text-gray-600 mb-2">{{ secondMovie.director }}</p>
              <div class="flex items-center justify-center gap-1 mb-3">
                <Star class="h-5 w-5 text-yellow-400 fill-current" />
                <span class="font-semibold text-xl text-gray-800">{{ secondMovie.rating.toFixed(1) }}</span>
              </div>

              <!-- 투표 결과 -->
              <div class="bg-gray-300/50 rounded-lg p-4 border border-gray-400">
                <div class="font-bold text-xl mb-1" style="color: #000000">{{ secondMoviePercentage }}%</div>
                <div class="text-sm" style="color: #000000">{{ secondMovieVotes.toLocaleString() }}표</div>
              </div>
            </div>
          </div>

          <!-- 투표 참여 안내 -->
          <div class="mt-8 text-center">
            <div class="bg-red-600/20 rounded-xl p-4 inline-block border border-red-500/30">
              <p class="text-red-400">
                🗳️ <span class="font-semibold">투표는 매주 월요일 초기화됩니다</span>
              </p>
              <p class="text-red-300 text-sm mt-1">다음 투표는 2일 후 시작됩니다</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 박스오피스 TOP 10 - 가로 슬라이드 -->
      <div class="mb-12">
        <div
          class="bg-gradient-to-b from-gray-100/80 to-gray-200/60 backdrop-blur-sm rounded-2xl overflow-hidden shadow-lg"
        >
          <div class="bg-gradient-to-r from-red-600 to-red-700 p-6">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <Trophy class="h-7 w-7 text-white" />
                <div>
                  <h3 class="text-2xl font-bold text-white">박스오피스 TOP 10</h3>
                  <p class="text-red-100">별점 합계 기준</p>
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
                class="group cursor-pointer bg-white/80 rounded-lg p-4 hover:bg-white/90 transition-all duration-300 hover:scale-105 shadow-sm border border-gray-300"
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
                      <Medal v-else-if="movie.rank === 2" class="h-5 w-5 text-gray-400" />
                      <Trophy v-else class="h-5 w-5 text-orange-500" />
                    </div>
                  </div>
                </div>

                <div>
                  <h4
                    class="font-semibold text-gray-800 mb-1 line-clamp-2 group-hover:text-red-500 transition-colors"
                  >
                    {{ movie.title }}
                  </h4>
                  <p class="text-sm text-gray-600 mb-2 line-clamp-1">{{ movie.director }}</p>
                  <div class="flex items-center gap-2">
                    <Star class="h-4 w-4 text-yellow-400 fill-current" />
                    <span class="text-sm font-medium text-gray-800">{{ movie.rating.toFixed(1) }}</span>
                    <span class="text-sm text-gray-500">•</span>
                    <span class="text-sm text-gray-500">{{ movie.year }}</span>
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
          class="bg-gradient-to-b from-gray-100/80 to-gray-200/60 backdrop-blur-sm rounded-2xl overflow-hidden shadow-lg"
        >
          <div class="bg-gradient-to-r from-purple-600 to-purple-700 p-6">
            <div class="flex items-center gap-3">
              <TrendingUp class="h-7 w-7 text-white" />
              <div>
                <h3 class="text-2xl font-bold text-white">이번주 화제작</h3>
                <p class="text-purple-100">트렌딩 지수 기준</p>
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
                    <div class="bg-purple-600 text-white px-3 py-1 rounded-full text-xs font-bold shadow-lg">
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
                    class="font-semibold text-gray-800 text-sm line-clamp-2 group-hover:text-purple-500 transition-colors mb-2"
                  >
                    {{ movie.title }}
                  </h4>
                  <p class="text-xs text-gray-600 line-clamp-1 mb-2">{{ movie.director }}</p>

                  <!-- 트렌딩 지표 -->
                  <div class="flex items-center justify-between text-xs">
                    <span class="text-purple-400 font-medium">+{{ movie.buzz }}%</span>
                    <span class="text-gray-500">화제성</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 화제성 통계 -->
            <div class="mt-8 pt-6 border-t border-gray-300">
              <h4 class="font-semibold text-gray-800 mb-4">실시간 화제성 지표</h4>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div
                  v-for="stat in buzzStats"
                  :key="stat.label"
                  class="bg-white/80 rounded-lg p-4 border border-gray-300 shadow-sm"
                >
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-sm text-gray-600">{{ stat.label }}</span>
                    <span class="text-sm font-bold text-purple-500">{{ stat.value }}</span>
                  </div>
                  <div class="w-full bg-gray-300 rounded-full h-2">
                    <div class="bg-purple-500 h-2 rounded-full" :style="{ width: stat.width }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 랭킹 정보 -->
      <div class="mt-8 text-center">
        <div class="bg-gray-100/50 backdrop-blur-sm rounded-2xl p-6 shadow-lg border border-gray-200/30">
          <p class="text-gray-700 mb-2">
            <span class="font-semibold">📊 랭킹 기준:</span> 평점, 관객수, 리뷰 점수를 종합하여 산정
          </p>
          <p class="text-gray-600 text-sm">매일 오전 6시에 업데이트됩니다.</p>
        </div>
      </div>
    </div>

    <!-- 공통 푸터 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Star, TrendingUp, Crown, Medal, Trophy, ChevronLeft, ChevronRight } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import { allMovies } from '@/data/movies'

const router = useRouter()

// 슬라이드 상태 관리
const currentSlide = ref(0)
const MOVIES_PER_SLIDE = 4

// 평점 기준으로 정렬
const rankedMovies = computed(() =>
  allMovies
    .map((movie, index) => ({ ...movie, rank: index + 1 }))
    .sort((a, b) => b.rating - a.rating)
    .map((movie, index) => ({ ...movie, rank: index + 1 }))
)

const topMovie = computed(() => rankedMovies.value[0])
const secondMovie = computed(() => rankedMovies.value[1])

const boxOfficeMovies = computed(() => rankedMovies.value.slice(0, 10))
const totalSlides = computed(() => Math.ceil(boxOfficeMovies.value.length / MOVIES_PER_SLIDE))
const currentSlideMovies = computed(() => {
  const start = currentSlide.value * MOVIES_PER_SLIDE
  return boxOfficeMovies.value.slice(start, start + MOVIES_PER_SLIDE)
})

// 원본은 Math.random()으로 화제성 %를 만들었는데 렌더마다 흔들리지 않도록 고정값으로 옮겼다.
const trendingMovies = computed(() =>
  rankedMovies.value.slice(2, 10).map((movie, i) => ({ ...movie, buzz: 480 - i * 47 }))
)

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
  if (rank === 2) return 'bg-gray-400'
  if (rank === 3) return 'bg-orange-500'
  return 'bg-gray-500'
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
