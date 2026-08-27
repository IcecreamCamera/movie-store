<template>
  <div class="min-h-screen bg-background">
    <AppHeader current-page="payments" @search="onSearch" />

    <div class="bg-surface border-b border-hairline">
      <div class="max-w-7xl mx-auto px-8 lg:px-16 py-6">
        <div class="flex items-center gap-3">
          <CreditCard class="h-6 w-6 text-brand" />
          <h1 class="text-2xl font-bold text-foreground">예매 · 결제 내역</h1>
        </div>
        <p class="text-dim mt-2">지금까지 예매하고 결제한 내역이에요.</p>
      </div>
    </div>

    <div class="max-w-4xl mx-auto px-8 lg:px-16 py-8">
      <!-- 로딩 -->
      <div v-if="loading" class="py-20 text-center">
        <p class="text-dim text-lg">불러오는 중...</p>
      </div>

      <!-- 에러 -->
      <div v-else-if="errorMessage" class="py-20 text-center">
        <p class="text-dim text-lg mb-2">결제 내역을 불러오지 못했어요.</p>
        <p class="text-faint text-sm">{{ errorMessage }}</p>
      </div>

      <!-- 비었을 때 -->
      <div v-else-if="items.length === 0" class="py-20 text-center">
        <p class="text-dim text-lg mb-2">아직 예매한 영화가 없어요.</p>
        <p class="text-faint text-sm mb-6">영화를 예매하면 여기에 결제 내역이 쌓여요.</p>
        <BaseButton
          class="bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold"
          @click="router.push('/booking')"
        >
          <Ticket class="h-5 w-5 mr-2" />
          예매하러 가기
        </BaseButton>
      </div>

      <template v-else>
        <!-- 합계 -->
        <div class="rounded-xl bg-surface border border-hairline p-5 mb-6 flex items-center justify-between">
          <span class="text-dim">총 {{ items.length }}건</span>
          <span class="text-xl font-bold text-brand">{{ totalPaid.toLocaleString() }}원</span>
        </div>

        <!-- 내역 -->
        <ul class="space-y-4">
          <li
            v-for="item in items"
            :key="item.bookingId"
            class="rounded-xl bg-surface border border-hairline p-5"
          >
            <div class="flex items-start justify-between gap-4 mb-4">
              <div>
                <h2 class="font-semibold text-foreground text-lg">{{ item.movieTitle }}</h2>
                <p class="text-faint text-sm mt-0.5">
                  예매번호 {{ item.bookingNo }} · {{ formatDate(item.createdAt) }}
                </p>
              </div>
              <span class="shrink-0 rounded-full px-2.5 py-0.5 text-xs bg-brand-dim text-brand">
                {{ item.payment.status === 'COMPLETED' ? '결제 완료' : item.payment.status }}
              </span>
            </div>

            <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
              <dt class="text-faint">장르</dt>
              <dd class="text-dim">{{ item.genre }}</dd>
              <dt class="text-faint">매수</dt>
              <dd class="text-dim">{{ item.quantity }}매</dd>
              <template v-if="item.snacks?.length">
                <dt class="text-faint">간식</dt>
                <dd class="text-dim">
                  {{ snackSummary(item.snacks) }}
                  <span class="text-faint">· {{ item.snackAmount.toLocaleString() }}원</span>
                </dd>
              </template>
              <dt class="text-faint">결제 수단</dt>
              <dd class="text-dim">{{ item.payment.method }}</dd>
              <dt class="text-faint">거래번호</dt>
              <dd class="text-faint font-mono text-xs pt-0.5">{{ item.payment.transactionId }}</dd>
              <dt class="text-faint">결제 금액</dt>
              <dd class="text-brand font-semibold">{{ item.payment.amount.toLocaleString() }}원</dd>
            </dl>
          </li>
        </ul>

      </template>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CreditCard, Ticket } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import * as bookingApi from '@/api/booking.js'
import * as paymentApi from '@/api/payment.js'
import { user, isLoggedIn } from '@/store/auth'
import { genreLabel } from '@/lib/genre.js'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const items = ref([])

const totalPaid = computed(() =>
  items.value.reduce((sum, item) => sum + (item.payment?.amount ?? 0), 0)
)

// GET /api/bookings/my + GET /api/payments/user/{userId} 를 bookingId로 묶는다.
async function load() {
  if (!isLoggedIn.value || !user.value?.id) return
  loading.value = true
  errorMessage.value = ''
  try {
    const [bookingRes, paymentRes] = await Promise.all([
      bookingApi.my(),
      paymentApi.byUser(user.value.id)
    ])
    const bookingList = bookingRes?.data?.data ?? bookingRes?.data ?? []
    const paymentList = paymentRes?.data?.data ?? paymentRes?.data ?? []
    const paymentByBookingId = new Map(
      (Array.isArray(paymentList) ? paymentList : []).map((p) => [p.bookingId, p])
    )

    items.value = (Array.isArray(bookingList) ? bookingList : []).map((b) => {
      const payment = paymentByBookingId.get(b.id)
      return {
        bookingId: b.id,
        bookingNo: b.id ? `BK-${b.id}` : '',
        movieTitle: b.movie?.title ?? '',
        genre: genreLabel(b.movie?.genre),
        quantity: b.quantity,
        createdAt: b.createdAt ? new Date(b.createdAt) : new Date(),
        snacks: [],
        snackAmount: 0,
        payment: {
          amount: Number(payment?.amount ?? b.amount ?? 0),
          status: payment?.status ?? (b.status === 'CONFIRMED' ? 'COMPLETED' : b.status),
          method: '간편결제',
          transactionId: payment?.transactionId ?? '-'
        }
      }
    })
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message || err?.message || '결제 내역을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 템플릿에서 v-for로 이어붙이면 개행이 공백으로 접혀 "핫도그 2개 , 나쵸"처럼 된다.
function snackSummary(list) {
  return list.map((s) => `${s.name} ${s.quantity}개`).join(', ')
}

function formatDate(d) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(d)
}

function onSearch(q) {
  router.push({ name: 'Search', query: { q } })
}
</script>
