<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="open"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
        role="dialog"
        aria-modal="true"
        aria-labelledby="booking-dialog-title"
        @click.self="$emit('close')"
      >
        <div
          class="w-full max-w-lg max-h-[88vh] overflow-y-auto rounded-2xl bg-surface border border-hairline shadow-2xl"
        >
          <!-- ── 1단계: 간식 고르기 (아직 결제 전) ────────────────────────── -->
          <template v-if="!booking">
            <div class="p-6 border-b border-hairline">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <h2 id="booking-dialog-title" class="text-xl font-bold text-foreground">
                    이 영화엔 이 간식
                  </h2>
                  <p class="text-dim text-sm mt-1">
                    <span class="text-brand">{{ movie?.genre }}</span> 장르에 어울리는 매점 간식이에요.
                    담으면 영화 값과 함께 결제돼요.
                  </p>
                </div>
                <button
                  class="text-faint hover:text-foreground transition-colors cursor-pointer shrink-0"
                  aria-label="닫기"
                  @click="$emit('close')"
                >
                  <X class="h-5 w-5" />
                </button>
              </div>
            </div>

            <div class="p-6 border-b border-hairline">
              <div class="grid grid-cols-2 gap-3">
                <div
                  v-for="snack in snacks"
                  :key="snack.id"
                  class="rounded-lg border p-3 transition-colors"
                  :class="picked[snack.id]
                    ? 'bg-brand-dim border-brand'
                    : 'bg-surface-2 border-hairline'"
                >
                  <button
                    type="button"
                    class="w-full text-left cursor-pointer"
                    :aria-pressed="!!picked[snack.id]"
                    @click="toggle(snack)"
                  >
                    <div class="flex items-center justify-between gap-2 mb-2">
                      <span class="font-medium text-foreground text-sm">{{ snack.name }}</span>
                      <span class="shrink-0 rounded px-1.5 py-0.5 text-xs bg-brand-dim text-brand">
                        {{ snack.taste }}
                      </span>
                    </div>
                    <div class="text-sm text-brand">{{ snack.price.toLocaleString() }}원</div>
                  </button>

                  <!-- 담은 간식만 수량 조절 -->
                  <div v-if="picked[snack.id]" class="flex items-center justify-end gap-2 mt-3">
                    <button
                      type="button"
                      class="w-7 h-7 rounded-full border border-hairline text-foreground hover:bg-surface transition-colors cursor-pointer"
                      :aria-label="`${snack.name} 수량 줄이기`"
                      @click="dec(snack)"
                    >−</button>
                    <span class="w-6 text-center text-sm font-semibold text-foreground">
                      {{ picked[snack.id] }}
                    </span>
                    <button
                      type="button"
                      class="w-7 h-7 rounded-full border border-hairline text-foreground hover:bg-surface disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                      :disabled="picked[snack.id] >= 9"
                      :aria-label="`${snack.name} 수량 늘리기`"
                      @click="inc(snack)"
                    >+</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 합계 -->
            <div class="p-6">
              <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm mb-5">
                <dt class="text-faint">영화</dt>
                <dd class="text-foreground text-right">
                  {{ movie?.title }} {{ quantity }}매 · {{ ticketAmount.toLocaleString() }}원
                </dd>
                <dt class="text-faint">간식</dt>
                <dd class="text-right" :class="snackAmount ? 'text-foreground' : 'text-faint'">
                  {{ snackAmount ? `${snackCount}개 · ${snackAmount.toLocaleString()}원` : '선택 안 함' }}
                </dd>
                <dt class="text-foreground font-semibold pt-2 border-t border-hairline">최종 결제</dt>
                <dd class="text-brand font-bold text-lg text-right pt-2 border-t border-hairline">
                  {{ total.toLocaleString() }}원
                </dd>
              </dl>

              <div class="flex gap-3">
                <BaseButton
                  variant="outline"
                  class="flex-1 border-hairline text-dim hover:text-foreground hover:bg-surface-2"
                  @click="$emit('confirm', [])"
                >
                  안 살래요
                </BaseButton>
                <BaseButton
                  class="flex-1 bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold"
                  @click="$emit('confirm', selected)"
                >
                  {{ total.toLocaleString() }}원 결제하기
                </BaseButton>
              </div>
            </div>
          </template>

          <!-- ── 2단계: 결제 완료 ─────────────────────────────────────────── -->
          <template v-else>
            <div class="p-6 border-b border-hairline">
              <div class="flex items-start justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full bg-brand flex items-center justify-center shrink-0">
                    <Check class="h-6 w-6 text-[#1A1408]" />
                  </div>
                  <div>
                    <h2 id="booking-dialog-title" class="text-xl font-bold text-foreground">
                      예매가 완료되었습니다
                    </h2>
                    <p class="text-dim text-sm mt-0.5">예매번호 {{ booking.bookingNo }}</p>
                  </div>
                </div>
                <button
                  class="text-faint hover:text-foreground transition-colors cursor-pointer shrink-0"
                  aria-label="닫기"
                  @click="$emit('close')"
                >
                  <X class="h-5 w-5" />
                </button>
              </div>

              <dl class="mt-5 grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
                <dt class="text-faint">영화</dt>
                <dd class="text-foreground">{{ booking.movieTitle }}</dd>
                <dt class="text-faint">매수</dt>
                <dd class="text-foreground">{{ booking.quantity }}매</dd>
                <template v-if="booking.snacks.length">
                  <dt class="text-faint">간식</dt>
                  <dd class="text-foreground">{{ snackSummary(booking.snacks) }}</dd>
                </template>
              </dl>
            </div>

            <!-- 결제 -->
            <div class="p-6">
              <div class="flex items-center gap-2 mb-4">
                <CreditCard class="h-5 w-5 text-brand" />
                <h3 class="font-semibold text-foreground">결제 정보</h3>
                <span class="ml-auto rounded-full px-2.5 py-0.5 text-xs bg-brand-dim text-brand">
                  {{ booking.payment.status === 'COMPLETED' ? '결제 완료' : booking.payment.status }}
                </span>
              </div>

              <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
                <dt class="text-faint">결제 수단</dt>
                <dd class="text-foreground text-right">{{ booking.payment.method }}</dd>
                <dt class="text-faint">거래번호</dt>
                <dd class="text-dim font-mono text-xs pt-0.5 text-right">{{ booking.payment.transactionId }}</dd>
                <dt class="text-faint">영화</dt>
                <dd class="text-foreground text-right">{{ booking.ticketAmount.toLocaleString() }}원</dd>
                <template v-if="booking.snackAmount">
                  <dt class="text-faint">간식</dt>
                  <dd class="text-foreground text-right">{{ booking.snackAmount.toLocaleString() }}원</dd>
                </template>
                <dt class="text-foreground font-semibold pt-2 border-t border-hairline">결제 금액</dt>
                <dd class="text-brand font-bold text-lg text-right pt-2 border-t border-hairline">
                  {{ booking.payment.amount.toLocaleString() }}원
                </dd>
              </dl>

              <div class="flex gap-3 mt-5">
                <BaseButton
                  variant="outline"
                  class="flex-1 border-hairline text-dim hover:text-foreground hover:bg-surface-2"
                  @click="goPayments"
                >
                  결제 내역
                </BaseButton>
                <BaseButton
                  class="flex-1 bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold"
                  @click="$emit('close')"
                >
                  확인
                </BaseButton>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Check, X, CreditCard } from '@lucide/vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { recommendByGenre } from '@/data/snacks'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 1단계에서 쓰는 값들
  movie: { type: Object, default: null },
  quantity: { type: Number, default: 1 },
  ticketAmount: { type: Number, default: 0 },
  // 2단계. 결제가 끝나면 채워지고, 그때 완료 화면으로 바뀐다.
  booking: { type: Object, default: null }
})

const emit = defineEmits(['close', 'confirm'])
const router = useRouter()

// TODO: GET /api/recommend/{userId}?movieId={movie.id} 로 교체.
// 지금은 snacks.js의 장르 매핑으로 프론트에서 고른다.
const snacks = computed(() => recommendByGenre(props.movie?.genre))

// { 간식id: 수량 }
const picked = ref({})

// 다시 열 때마다 장바구니를 비운다.
watch(() => props.open, (isOpen) => { if (isOpen) picked.value = {} })

function toggle(snack) {
  const next = { ...picked.value }
  if (next[snack.id]) delete next[snack.id]
  else next[snack.id] = 1
  picked.value = next
}

function inc(snack) {
  picked.value = { ...picked.value, [snack.id]: Math.min((picked.value[snack.id] ?? 0) + 1, 9) }
}

function dec(snack) {
  const qty = (picked.value[snack.id] ?? 0) - 1
  const next = { ...picked.value }
  if (qty <= 0) delete next[snack.id]
  else next[snack.id] = qty
  picked.value = next
}

const selected = computed(() =>
  snacks.value
    .filter((s) => picked.value[s.id])
    .map((s) => ({ ...s, quantity: picked.value[s.id] }))
)

const snackCount = computed(() => selected.value.reduce((n, s) => n + s.quantity, 0))
const snackAmount = computed(() => selected.value.reduce((n, s) => n + s.price * s.quantity, 0))
const total = computed(() => props.ticketAmount + snackAmount.value)

// 템플릿에서 v-for로 이어붙이면 개행이 공백으로 접혀 "핫도그 2개 , 나쵸"처럼 된다.
function snackSummary(list) {
  return list.map((s) => `${s.name} ${s.quantity}개`).join(', ')
}

function goPayments() {
  emit('close')
  router.push('/payments')
}

function onKeydown(e) {
  if (e.key === 'Escape' && props.open) emit('close')
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
