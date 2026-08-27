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
          <!-- 예매 완료 -->
          <div class="p-6 border-b border-hairline">
            <div class="flex items-start justify-between gap-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-full bg-brand flex items-center justify-center shrink-0">
                  <Check class="h-6 w-6 text-[#1A1408]" />
                </div>
                <div>
                  <h2 id="booking-dialog-title" class="text-xl font-bold text-foreground">예매가 완료되었습니다</h2>
                  <p class="text-dim text-sm mt-0.5">예매번호 {{ booking?.bookingNo }}</p>
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
              <dd class="text-foreground">{{ booking?.movieTitle }}</dd>
              <dt class="text-faint">매수</dt>
              <dd class="text-foreground">{{ booking?.quantity }}매</dd>
            </dl>
          </div>

          <!-- 결제 -->
          <div class="p-6 border-b border-hairline">
            <div class="flex items-center gap-2 mb-4">
              <CreditCard class="h-5 w-5 text-brand" />
              <h3 class="font-semibold text-foreground">결제 정보</h3>
              <span class="ml-auto rounded-full px-2.5 py-0.5 text-xs bg-brand-dim text-brand">
                {{ booking?.payment.status === 'COMPLETED' ? '결제 완료' : booking?.payment.status }}
              </span>
            </div>

            <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
              <dt class="text-faint">결제 수단</dt>
              <dd class="text-foreground">{{ booking?.payment.method }}</dd>
              <dt class="text-faint">거래번호</dt>
              <dd class="text-dim font-mono text-xs pt-0.5">{{ booking?.payment.transactionId }}</dd>
              <dt class="text-faint">결제 금액</dt>
              <dd class="text-brand font-semibold text-base">{{ booking?.payment.amount.toLocaleString() }}원</dd>
            </dl>
          </div>

          <!-- 간식 추천 -->
          <div class="p-6">
            <h3 class="font-semibold text-foreground mb-1">이 영화엔 이 간식</h3>
            <p class="text-dim text-sm mb-4">
              <span class="text-brand">{{ booking?.genre }}</span> 장르에 어울리는 매점 간식이에요
            </p>

            <div class="grid grid-cols-2 gap-3">
              <div
                v-for="snack in snacks"
                :key="snack.id"
                class="rounded-lg bg-surface-2 border border-hairline p-3"
              >
                <div class="flex items-center justify-between gap-2 mb-2">
                  <span class="font-medium text-foreground text-sm">{{ snack.name }}</span>
                  <span class="shrink-0 rounded px-1.5 py-0.5 text-xs bg-brand-dim text-brand">{{ snack.taste }}</span>
                </div>
                <div class="text-sm text-brand">{{ snack.price.toLocaleString() }}원</div>
              </div>
            </div>

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
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Check, X, CreditCard } from '@lucide/vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { recommendByGenre } from '@/data/snacks'

const props = defineProps({
  open: { type: Boolean, default: false },
  booking: { type: Object, default: null }
})

const emit = defineEmits(['close'])
const router = useRouter()

// TODO: GET /api/recommend/{userId}?movieId={booking.movieId} 로 교체.
// 지금은 snacks.js의 장르 매핑으로 프론트에서 고른다.
const snacks = computed(() => recommendByGenre(props.booking?.genre))

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
