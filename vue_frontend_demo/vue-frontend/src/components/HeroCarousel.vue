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
    <!--
      슬라이드를 한 줄로 늘어놓고 트랙만 옆으로 민다.
      <Transition>의 enter/leave를 쓰지 않으므로 DOM에 있는 슬라이드 수가 항상 일정하고,
      requestAnimationFrame이 멈춘 상태(창이 가려짐 등)에서도 상태가 어긋나지 않는다.
    -->
    <div ref="track" class="hero-track flex h-full" :style="trackStyle">
      <div
        v-for="(movie, i) in slides"
        :key="i"
        class="relative w-full h-full shrink-0"
        :inert="i !== position ? true : undefined"
        :aria-hidden="i !== position ? 'true' : undefined"
      >
        <div class="absolute inset-0 cursor-pointer" @click="emit('select', movie)">
          <ImageWithFallback
            :src="movie.backdrop || movie.poster"
            :alt="movie.title"
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
                {{ movie.title }}
              </h1>
              <p v-if="movie.description" class="text-white/90 text-lg lg:text-xl leading-relaxed mb-6">
                {{ summary(movie.description) }}
              </p>
              <div class="flex items-center gap-4 mb-8 text-white/80">
                <div class="flex items-center gap-2">
                  <Star class="h-5 w-5 text-yellow-400 fill-current" />
                  <span class="text-lg font-semibold">{{ movie.rating.toFixed(1) }}</span>
                </div>
                <template v-if="movie.year">
                  <span>•</span>
                  <span>{{ movie.year }}년</span>
                </template>
                <template v-if="movie.runtime">
                  <span>•</span>
                  <span>{{ movie.runtime }}분</span>
                </template>
                <template v-if="movie.genre">
                  <span>•</span>
                  <span>{{ movie.genre }}</span>
                </template>
              </div>
              <div class="flex justify-start">
                <BaseButton
                  class="bg-brand text-[#1A1408] hover:bg-brand-hover px-12 py-4 text-xl font-semibold shadow-lg h-auto"
                  @click.stop="emit('select', movie)"
                >
                  <Ticket class="h-6 w-6 mr-3" />
                  보러 가기
                </BaseButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 좌우 이동 버튼 -->
    <template v-if="total > 1">
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
          :aria-current="i === current"
          class="h-1.5 rounded-full overflow-hidden transition-all duration-300 cursor-pointer"
          :class="i === current ? 'w-10 bg-white/30' : 'w-2.5 bg-white/40 hover:bg-white/70'"
          @click="jump(i)"
        >
          <span
            v-if="i === current"
            :key="current"
            class="hero-progress block h-full w-full bg-white"
            :style="progressStyle"
          ></span>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Star, Ticket, ChevronLeft, ChevronRight } from '@lucide/vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

// 한 슬라이드가 머무는 시간. 인디케이터 진행 막대와 같은 값을 쓴다.
const INTERVAL_MS = 6000
// 옆으로 밀리는 시간. 복제본에서 되돌아오는 시점을 재는 데도 쓴다.
const SLIDE_MS = 700

const props = defineProps({
  movies: { type: Array, required: true }
})

const emit = defineEmits(['select'])

const track = ref(null)
// 트랙 위치. 0..total-1은 실제 슬라이드, total은 맨 끝 복제본(= 0번과 같은 화면).
const position = ref(0)
const animate = ref(true)
const paused = ref(false)

const total = computed(() => props.movies.length)

// 마지막에서 처음으로 넘어갈 때 되감기처럼 보이지 않도록 0번을 한 장 더 붙인다.
const slides = computed(() =>
  total.value > 1 ? [...props.movies, props.movies[0]] : props.movies
)

// 복제본 위에 있을 때도 실제로 보고 있는 건 0번이다.
const current = computed(() => position.value % total.value)

const trackStyle = computed(() => ({
  transform: `translateX(-${position.value * 100}%)`,
  transition: animate.value ? `transform ${SLIDE_MS}ms cubic-bezier(0.4, 0, 0.2, 1)` : 'none'
}))

const progressStyle = computed(() => ({
  animationDuration: `${INTERVAL_MS}ms`,
  animationPlayState: paused.value ? 'paused' : 'running'
}))

function summary(text) {
  if (!text) return ''
  return text.length > 200 ? `${text.slice(0, 200)}...` : text
}

// transition을 끈 채 위치를 바꾼 뒤, 그 변경이 DOM에 반영되고 나서 다시 켠다.
// 강제 리플로우를 한 번 읽어야 브라우저가 두 변경을 한 프레임으로 합치지 않는다.
async function resumeAnimation() {
  await nextTick()
  if (track.value) void track.value.offsetWidth
  animate.value = true
}

// 복제본에 서 있으면 티 나지 않게 진짜 0번으로 옮겨둔다.
function normalizeFromClone() {
  if (position.value !== total.value) return false
  animate.value = false
  position.value = 0
  return true
}

let snapTimer = null

function clearSnap() {
  if (snapTimer !== null) {
    window.clearTimeout(snapTimer)
    snapTimer = null
  }
}

// 복제본까지 밀고 난 뒤, 애니메이션이 끝나면 조용히 0번으로 되돌린다.
// setTimeout이라 렌더링이 멈춘 상태에서도 상태가 밀리지 않는다.
function scheduleSnap() {
  clearSnap()
  snapTimer = window.setTimeout(async () => {
    snapTimer = null
    if (normalizeFromClone()) await resumeAnimation()
  }, SLIDE_MS)
}

async function go(step) {
  if (total.value < 2) return
  clearSnap()
  if (normalizeFromClone()) await resumeAnimation()

  if (step > 0) {
    position.value += 1
    // 복제본에 도착했으면 되돌릴 준비를 한다.
    if (position.value === total.value) scheduleSnap()
  } else if (position.value === 0) {
    // 0번에서 뒤로 가려면 화면이 같은 복제본으로 순간 이동한 뒤 왼쪽으로 민다.
    animate.value = false
    position.value = total.value
    await resumeAnimation()
    position.value = total.value - 1
  } else {
    position.value -= 1
  }
}

// 사용자 조작. 다음 슬라이드까지 시간을 다시 채워준다.
async function navigate(step) {
  await go(step)
  schedule()
}

async function jump(i) {
  clearSnap()
  if (normalizeFromClone()) await resumeAnimation()
  if (i !== current.value) position.value = i
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
  // 숨겨진 탭에서는 굳이 돌리지 않는다.
  if (paused.value || document.hidden || total.value < 2) return
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
  clearSnap()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
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
  .hero-track {
    transition: none !important;
  }
  .hero-progress {
    animation: none;
    transform: scaleX(1);
  }
}
</style>
