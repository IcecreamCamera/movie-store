<template>
  <div
    class="relative h-[85vh] mb-8 overflow-hidden"
    role="region"
    aria-roledescription="carousel"
    aria-label="추천 영화"
    @mouseenter="pause"
    @mouseleave="resume"
    @focusin="pause"
    @focusout="resume"
  >
    <!-- 슬라이드: 방향에 따라 좌/우로 밀려 들어오고 나간다 -->
    <Transition :name="transitionName">
      <div :key="index" class="absolute inset-0">
        <div class="absolute inset-0 cursor-pointer" @click="emit('select', current)">
          <ImageWithFallback
            :src="current.poster"
            :alt="current.title"
            class="w-full h-full object-cover"
          />
          <!-- 그라데이션 오버레이 -->
          <div class="absolute inset-0 bg-gradient-to-r from-black via-black/50 to-transparent"></div>
          <div class="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent"></div>
        </div>

        <!-- 히어로 콘텐츠 -->
        <div class="absolute bottom-0 left-0 w-full">
          <div class="max-w-7xl mx-auto px-8 lg:px-16 relative pb-8 lg:pb-16">
            <div class="max-w-lg">
              <h1 class="text-5xl lg:text-7xl font-bold text-white mb-6 leading-tight">
                {{ current.title }}
              </h1>
              <p class="text-white/90 text-lg lg:text-xl leading-relaxed mb-6">
                {{ summary(current.description) }}
              </p>
              <div class="flex items-center gap-4 mb-8 text-white/80">
                <div class="flex items-center gap-2">
                  <Star class="h-5 w-5 text-yellow-400 fill-current" />
                  <span class="text-lg font-semibold">{{ current.rating.toFixed(1) }}</span>
                </div>
                <span>•</span>
                <span>{{ current.year }}년</span>
                <span>•</span>
                <span>{{ current.runtime }}분</span>
                <span>•</span>
                <span>{{ current.genre }}</span>
              </div>
              <div class="flex justify-start">
                <BaseButton
                  class="bg-white text-black hover:bg-white/90 px-12 py-4 text-xl font-semibold shadow-lg h-auto"
                  @click.stop="emit('select', current)"
                >
                  <Info class="h-6 w-6 mr-3" />
                  상세 정보
                </BaseButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 좌우 이동 버튼 -->
    <template v-if="movies.length > 1">
      <button
        type="button"
        aria-label="이전 영화"
        class="absolute left-4 top-1/2 -translate-y-1/2 z-10 h-12 w-12 rounded-full bg-black/40 text-white
               flex items-center justify-center opacity-60 hover:opacity-100 hover:bg-black/70
               focus-visible:opacity-100 focus-visible:outline-2 focus-visible:outline-white
               transition-opacity cursor-pointer"
        @click="navigate(-1)"
      >
        <ChevronLeft class="h-7 w-7" />
      </button>
      <button
        type="button"
        aria-label="다음 영화"
        class="absolute right-4 top-1/2 -translate-y-1/2 z-10 h-12 w-12 rounded-full bg-black/40 text-white
               flex items-center justify-center opacity-60 hover:opacity-100 hover:bg-black/70
               focus-visible:opacity-100 focus-visible:outline-2 focus-visible:outline-white
               transition-opacity cursor-pointer"
        @click="navigate(1)"
      >
        <ChevronRight class="h-7 w-7" />
      </button>

      <!-- 인디케이터 (현재 슬라이드는 남은 시간을 막대로 보여준다) -->
      <div class="absolute bottom-8 right-8 lg:right-16 z-10 flex items-center gap-2">
        <button
          v-for="(movie, i) in movies"
          :key="movie.id"
          type="button"
          :aria-label="`${i + 1}번째 영화: ${movie.title}`"
          :aria-current="i === index"
          class="h-1.5 rounded-full overflow-hidden transition-all duration-300 cursor-pointer"
          :class="i === index ? 'w-10 bg-white/30' : 'w-2.5 bg-white/40 hover:bg-white/70'"
          @click="jump(i)"
        >
          <span
            v-if="i === index"
            :key="index"
            class="hero-progress block h-full w-full bg-white"
            :style="progressStyle"
          ></span>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Star, Info, ChevronLeft, ChevronRight } from '@lucide/vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

// 한 슬라이드가 머무는 시간. 인디케이터 진행 막대와 같은 값을 쓴다.
const INTERVAL_MS = 6000

const props = defineProps({
  movies: { type: Array, required: true }
})

const emit = defineEmits(['select'])

const index = ref(0)
const direction = ref(1)
const paused = ref(false)

const current = computed(() => props.movies[index.value])
const transitionName = computed(() => (direction.value === 1 ? 'slide-next' : 'slide-prev'))
const progressStyle = computed(() => ({
  animationDuration: `${INTERVAL_MS}ms`,
  animationPlayState: paused.value ? 'paused' : 'running'
}))

function summary(text) {
  return text.length > 200 ? `${text.slice(0, 200)}...` : text
}

// 인덱스만 옮긴다. 타이머가 직접 호출하는 경로.
function go(step) {
  const total = props.movies.length
  if (total < 2) return
  direction.value = step >= 0 ? 1 : -1
  index.value = (index.value + step + total) % total
}

// 사용자 조작. 다음 슬라이드까지 시간을 다시 채워준다.
function navigate(step) {
  go(step)
  schedule()
}

function jump(i) {
  if (i === index.value) return
  direction.value = i > index.value ? 1 : -1
  index.value = i
  schedule()
}

let timer = null

function clear() {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

function schedule() {
  clear()
  // 탭이 숨겨져 있으면 아예 돌리지 않는다. 배경 탭에서는 requestAnimationFrame이
  // 멈춰 <Transition>의 leave가 끝나지 않고, 슬라이드 DOM이 계속 쌓인다.
  if (paused.value || document.hidden || props.movies.length < 2) return
  timer = window.setInterval(() => go(1), INTERVAL_MS)
}

function pause() {
  paused.value = true
  clear()
}

function resume() {
  paused.value = false
  schedule()
}

// 탭이 백그라운드로 가면 멈춘다. 돌아왔을 때 밀린 만큼 한꺼번에 넘어가지 않도록.
function onVisibilityChange() {
  if (document.hidden) clear()
  else schedule()
}

onMounted(() => {
  schedule()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  clear()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.slide-next-enter-active,
.slide-next-leave-active,
.slide-prev-enter-active,
.slide-prev-leave-active {
  transition: transform 700ms cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-next-enter-from {
  transform: translateX(100%);
}
.slide-next-leave-to {
  transform: translateX(-100%);
}
.slide-prev-enter-from {
  transform: translateX(-100%);
}
.slide-prev-leave-to {
  transform: translateX(100%);
}

@keyframes hero-progress {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

.hero-progress {
  transform-origin: left;
  animation-name: hero-progress;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

/* 모션 최소화를 켠 사용자에게는 밀림/진행 애니메이션을 없앤다 */
@media (prefers-reduced-motion: reduce) {
  .slide-next-enter-active,
  .slide-next-leave-active,
  .slide-prev-enter-active,
  .slide-prev-leave-active {
    transition-duration: 1ms;
  }
  .hero-progress {
    animation: none;
    transform: scaleX(1);
  }
}
</style>
