<template>
  <div class="min-h-screen bg-background">
    <AppHeader current-page="mypage" @search="onSearch" />

    <div class="bg-surface border-b border-hairline">
      <div class="max-w-7xl mx-auto px-8 lg:px-16 py-6">
        <div class="flex items-center gap-3">
          <User class="h-6 w-6 text-brand" />
          <h1 class="text-2xl font-bold text-foreground">마이페이지</h1>
        </div>
        <p class="text-dim mt-2">내 정보와 예매 내역을 확인해요.</p>
      </div>
    </div>

    <div class="max-w-4xl mx-auto px-8 lg:px-16 py-8">
      <!-- 비로그인 -->
      <div v-if="!isLoggedIn" class="py-20 text-center">
        <p class="text-dim text-lg mb-2">로그인이 필요해요.</p>
        <p class="text-faint text-sm mb-6">로그인하면 예매 내역과 맞춤 간식 추천을 볼 수 있어요.</p>
        <BaseButton
          class="bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold"
          @click="router.push('/login')"
        >
          로그인하러 가기
        </BaseButton>
      </div>

      <template v-else>
        <!-- 내 정보 -->
        <section class="rounded-xl bg-surface border border-hairline p-6 mb-8">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 class="text-xl font-bold text-foreground">{{ user.name }}</h2>
              <p class="text-dim text-sm mt-1">{{ user.email }}</p>
              <span class="inline-block mt-3 rounded-full px-2.5 py-0.5 text-xs bg-brand-dim text-brand">
                {{ user.role }}
              </span>
            </div>
            <BaseButton
              variant="outline"
              class="border-hairline text-dim hover:text-foreground hover:bg-surface-2"
              @click="logout"
            >
              로그아웃
            </BaseButton>
          </div>
        </section>

        <!-- 요약 -->
        <div class="grid grid-cols-2 gap-4 mb-8">
          <div class="rounded-xl bg-surface border border-hairline p-5">
            <span class="block text-sm text-faint mb-1">예매</span>
            <span class="text-2xl font-bold text-foreground">{{ items.length }}건</span>
          </div>
          <div class="rounded-xl bg-surface border border-hairline p-5">
            <span class="block text-sm text-faint mb-1">총 결제</span>
            <span class="text-2xl font-bold text-brand">{{ totalPaid.toLocaleString() }}원</span>
          </div>
        </div>

        <!-- 최근 예매 -->
        <section>
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-foreground">최근 예매</h2>
            <BaseButton
              variant="ghost"
              class="text-dim hover:text-white"
              @click="router.push('/payments')"
            >
              전체 보기
            </BaseButton>
          </div>

          <p v-if="items.length === 0" class="text-faint text-sm py-8 text-center">
            아직 예매한 영화가 없어요.
          </p>
          <ul v-else class="space-y-3">
            <li
              v-for="item in items.slice(0, 3)"
              :key="item.bookingId"
              class="rounded-lg bg-surface border border-hairline p-4 flex items-center justify-between gap-4"
            >
              <div>
                <span class="text-foreground font-medium">{{ item.movieTitle }}</span>
                <span class="text-faint text-sm ml-2">{{ item.quantity }}매</span>
              </div>
              <span class="text-brand text-sm shrink-0">{{ item.payment.amount.toLocaleString() }}원</span>
            </li>
          </ul>
        </section>
      </template>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { User } from '@lucide/vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { user, isLoggedIn, signOut } from '@/store/auth'
import { bookings as items, totalPaid } from '@/store/bookings'

const router = useRouter()

// TODO: GET /api/users/me + GET /api/bookings/my 로 교체.

function logout() {
  signOut()
  router.push('/')
}

function onSearch(q) {
  router.push({ name: 'Search', query: { q } })
}
</script>
