<template>
  <div class="min-h-screen" style="background-color: #ffffff">
    <!-- 공통 헤더 -->
    <AppHeader current-page="movie-detail" />

    <!-- 페이지 제목 헤더 -->
    <div style="background-color: #e4e4e4">
      <div class="max-w-7xl mx-auto px-8 lg:px-16 py-6">
        <div class="flex items-center gap-3">
          <Film class="h-6 w-6 text-red-600" />
          <h1 class="text-2xl font-bold text-black">영화 상세정보</h1>
        </div>
        <p class="text-black/70 mt-2">{{ movie.title }}의 상세 정보를 확인하세요</p>
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-8 lg:px-16 py-8">
      <!-- 뒤로가기 버튼 -->
      <div class="mb-6">
        <BaseButton
          variant="ghost"
          class="text-gray-600 hover:text-black hover:bg-gray-100 -ml-2"
          @click="goBack"
        >
          <ArrowLeft class="h-5 w-5 mr-2" />
          뒤로가기
        </BaseButton>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <!-- 메인 콘텐츠 -->
        <div class="lg:col-span-3 space-y-8">
          <!-- 영화 기본 정보 + 줄거리 + 출연진 -->
          <div class="bg-gradient-to-b from-gray-100/80 to-gray-200/60 backdrop-blur-sm rounded-2xl p-8 shadow-lg">
            <div class="flex flex-col md:flex-row gap-8 mb-8">
              <!-- 포스터 -->
              <div class="flex-shrink-0">
                <div class="w-64 h-80 rounded-xl overflow-hidden shadow-lg">
                  <ImageWithFallback
                    :src="movie.poster"
                    :alt="movie.title"
                    class="w-full h-full object-cover"
                  />
                </div>
              </div>

              <!-- 영화 정보 + 줄거리 -->
              <div class="flex-1">
                <div class="flex items-center gap-3 mb-4">
                  <BaseBadge class="bg-red-100 text-red-700">{{ movie.year }}년 작품</BaseBadge>
                  <BaseBadge variant="outline">{{ movie.genre }}</BaseBadge>
                </div>

                <h1 class="text-4xl font-bold text-black mb-4">{{ movie.title }}</h1>
                <p class="text-xl text-gray-700 mb-6">감독: {{ movie.director }}</p>

                <div class="grid grid-cols-2 gap-6 mb-8">
                  <div class="flex items-center gap-2">
                    <Star class="h-5 w-5 text-yellow-400 fill-current" />
                    <span class="font-semibold">{{ movie.rating.toFixed(1) }}</span>
                    <span class="text-gray-500">(3,847명 평가)</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Clock class="h-5 w-5 text-gray-400" />
                    <span>{{ movie.runtime }}분</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Calendar class="h-5 w-5 text-gray-400" />
                    <span>{{ movie.year }}년 개봉</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Users class="h-5 w-5 text-gray-400" />
                    <span>15세 이상 관람가</span>
                  </div>
                </div>

                <!-- 줄거리 -->
                <div class="mb-6">
                  <h3 class="text-lg font-semibold text-black mb-3">줄거리</h3>
                  <p class="text-gray-700 leading-relaxed">{{ movie.description || DEFAULT_PLOT }}</p>
                </div>
              </div>
            </div>

            <!-- 출연진 -->
            <div>
              <h3 class="text-lg font-semibold text-black mb-4">주요 출연진</h3>
              <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
                <div v-for="actor in cast" :key="actor.id" class="flex items-center gap-3">
                  <div class="w-12 h-12 rounded-full overflow-hidden flex-shrink-0 shadow-md">
                    <ImageWithFallback
                      :src="actor.image"
                      :alt="actor.name"
                      class="w-full h-full object-cover"
                    />
                  </div>
                  <div class="min-w-0 flex-1">
                    <h4 class="font-semibold text-black text-sm truncate">{{ actor.name }}</h4>
                    <p class="text-xs text-gray-600 truncate">{{ actor.character }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- OST 섹션 -->
          <div class="bg-gradient-to-b from-gray-100/80 to-gray-200/60 backdrop-blur-sm rounded-2xl p-8 shadow-lg">
            <div class="flex items-center gap-3 mb-6">
              <Volume2 class="h-6 w-6 text-purple-600" />
              <h2 class="text-2xl font-bold text-black">OST</h2>
            </div>
            <div class="space-y-3">
              <div
                v-for="track in ostTracks"
                :key="track.id"
                class="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
              >
                <div class="flex items-center gap-4">
                  <div class="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center">
                    <Play class="h-4 w-4 text-purple-600" />
                  </div>
                  <div>
                    <h4 class="font-medium text-black">{{ track.title }}</h4>
                    <p class="text-sm text-gray-600">{{ track.artist }}</p>
                  </div>
                </div>
                <span class="text-gray-500 text-sm">{{ track.duration }}</span>
              </div>
            </div>
          </div>

          <!-- 예고편 & 영상 섹션 -->
          <div class="bg-gradient-to-b from-gray-100/80 to-gray-200/60 backdrop-blur-sm rounded-2xl p-8 shadow-lg">
            <div class="flex items-center gap-3 mb-6">
              <Film class="h-6 w-6 text-red-600" />
              <h2 class="text-2xl font-bold text-black">예고편 &amp; 영상</h2>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div v-for="trailer in trailers" :key="trailer.id" class="group cursor-pointer">
                <div class="aspect-video rounded-lg overflow-hidden relative mb-3">
                  <ImageWithFallback
                    :src="trailer.thumbnail"
                    :alt="trailer.title"
                    class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div class="absolute inset-0 bg-black/30 flex items-center justify-center">
                    <div class="w-12 h-12 bg-white/90 rounded-full flex items-center justify-center">
                      <Play class="h-6 w-6 text-gray-800 ml-1" />
                    </div>
                  </div>
                  <div class="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-2 py-1 rounded">
                    {{ trailer.duration }}
                  </div>
                </div>
                <h4 class="font-medium text-black text-sm">{{ trailer.title }}</h4>
              </div>
            </div>
          </div>

          <!-- 비슷한 키워드 영화 추천 -->
          <div class="bg-gradient-to-b from-gray-100/80 to-gray-200/60 backdrop-blur-sm rounded-2xl p-8 shadow-lg">
            <h2 class="text-2xl font-bold text-black mb-6">비슷한 키워드 영화</h2>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
              <div v-for="km in keywordMovies" :key="km.id" class="group cursor-pointer">
                <div
                  class="aspect-[3/4] rounded-lg overflow-hidden mb-3 shadow-md group-hover:shadow-lg transition-shadow"
                >
                  <ImageWithFallback
                    :src="km.poster"
                    :alt="km.title"
                    class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                </div>
                <h4
                  class="font-semibold text-black text-sm mb-1 group-hover:text-blue-600 transition-colors"
                >
                  {{ km.title }}
                </h4>
                <div class="flex items-center justify-between">
                  <span class="text-gray-500 text-xs">{{ km.year }}년</span>
                  <div class="flex items-center gap-1">
                    <Star class="h-3 w-3 text-yellow-400 fill-current" />
                    <span class="text-yellow-600 text-xs font-medium">{{ km.rating }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 사이드바 - 관련 기사 -->
        <div class="lg:col-span-1">
          <div class="bg-gradient-to-b from-gray-100/80 to-gray-200/60 backdrop-blur-sm rounded-2xl p-6 shadow-lg">
            <h3 class="font-bold text-lg text-black mb-6">관련 기사</h3>
            <div class="space-y-6">
              <div v-for="article in relatedArticles" :key="article.id" class="group cursor-pointer">
                <div class="aspect-video rounded-lg overflow-hidden mb-3">
                  <ImageWithFallback
                    :src="article.thumbnail"
                    :alt="article.title"
                    class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                </div>
                <h4
                  class="font-medium text-black text-sm mb-2 line-clamp-2 group-hover:text-blue-600 transition-colors"
                >
                  {{ article.title }}
                </h4>
                <div class="flex items-center justify-between text-xs text-gray-500">
                  <span>{{ article.source }}</span>
                  <span>{{ article.date }}</span>
                </div>
              </div>
            </div>
            <BaseButton variant="outline" class="w-full mt-6">
              <ExternalLink class="h-4 w-4 mr-2" />
              더 많은 기사 보기
            </BaseButton>
          </div>
        </div>
      </div>
    </div>

    <!-- 공통 푸터 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Star,
  Clock,
  Calendar,
  Users,
  Play,
  Volume2,
  Film,
  ExternalLink
} from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import { findMovieById, featuredMovie, ACTOR_IMAGE, SCENE_IMAGE, allMovies } from '@/data/movies'

const route = useRoute()
const router = useRouter()

const movie = computed(() => findMovieById(route.params.id) || featuredMovie)

const DEFAULT_PLOT =
  '절대 울지 않는 남자의 마지막 눈물을 그린 감동 액션 드라마. 복수와 용서 사이에서 고뇌하는 한 남자의 이야기가 깊은 울림을 준다. 가족을 잃은 슬픔과 분노로 가득한 주인공이 진정한 용서와 구원을 찾아가는 여정을 그린 작품으로, 액션과 드라마가 완벽하게 조화를 이룬다.'

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

// 출연진 데이터
const cast = [
  { id: '1', name: '이병헌', character: '김수현 역', image: ACTOR_IMAGE },
  { id: '2', name: '하정우', character: '박민수 역', image: ACTOR_IMAGE },
  { id: '3', name: '전지현', character: '이서연 역', image: ACTOR_IMAGE },
  { id: '4', name: '조진웅', character: '최대식 역', image: ACTOR_IMAGE }
]

// OST 데이터
const ostTracks = [
  { id: '1', title: '메인 테마', artist: '조영욱', duration: '4:32' },
  { id: '2', title: '슬픔의 멜로디', artist: '조영욱', duration: '3:28' },
  { id: '3', title: '액션 테마', artist: '조영욱', duration: '5:15' },
  { id: '4', title: '이별의 노래', artist: '이하이', duration: '3:45' }
]

// 예고편 데이터
const trailers = [
  { id: '1', title: '메인 예고편', thumbnail: SCENE_IMAGE, duration: '2:30' },
  { id: '2', title: '캐릭터 예고편', thumbnail: allMovies[0].poster, duration: '1:45' },
  { id: '3', title: '스페셜 영상', thumbnail: allMovies[1].poster, duration: '3:20' },
  { id: '4', title: '메이킹 영상', thumbnail: allMovies[2].poster, duration: '4:15' }
]

// 관련 키워드 영화 데이터
const keywordMovies = [
  { id: '1', title: '베테랑', poster: allMovies[0].poster, rating: 8.2, year: 2015 },
  { id: '2', title: '신세계', poster: allMovies[1].poster, rating: 8.5, year: 2013 },
  { id: '3', title: '암살', poster: allMovies[2].poster, rating: 8.8, year: 2015 },
  { id: '4', title: '도둑들', poster: allMovies[3].poster, rating: 7.9, year: 2012 }
]

// 관련 기사 데이터
const relatedArticles = [
  {
    id: '1',
    title: "'우는 남자' 이정범 감독 \"액션과 드라마의 조화에 집중\"",
    source: '영화저널',
    date: '2024.01.15',
    thumbnail: SCENE_IMAGE
  },
  {
    id: '2',
    title: '이병헌 "10년 만의 액션 영화, 새로운 도전이었다"',
    source: '씨네21',
    date: '2024.01.12',
    thumbnail: allMovies[0].poster
  },
  {
    id: '3',
    title: "'우는 남자' 첫 주 박스오피스 1위 등극",
    source: '무비위크',
    date: '2024.01.10',
    thumbnail: allMovies[1].poster
  },
  {
    id: '4',
    title: '관객들이 뽑은 올해의 액션 영화 TOP 3',
    source: '스크린데일리',
    date: '2024.01.08',
    thumbnail: allMovies[2].poster
  }
]
</script>
