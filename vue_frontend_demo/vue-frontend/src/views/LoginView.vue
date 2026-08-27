<template>
  <div class="min-h-screen bg-background flex flex-col">
    <AppHeader current-page="login" />

    <div class="flex-1 flex items-center justify-center px-8 py-16">
      <div class="w-full max-w-sm">
        <h1 class="text-2xl font-bold text-foreground mb-1">로그인</h1>
        <p class="text-dim text-sm mb-8">예매하려면 로그인이 필요해요.</p>

        <form class="space-y-4" @submit.prevent="signIn">
          <div>
            <label for="email" class="block text-sm text-dim mb-2">이메일</label>
            <BaseInput id="email" v-model="email" type="email" placeholder="you@example.com"
              class="bg-surface border-hairline text-foreground placeholder:text-faint" />
          </div>
          <div>
            <label for="pw" class="block text-sm text-dim mb-2">비밀번호</label>
            <BaseInput id="pw" v-model="password" type="password" placeholder="8자 이상"
              class="bg-surface border-hairline text-foreground placeholder:text-faint" />
          </div>

          <BaseButton type="submit" class="w-full bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold">
            로그인
          </BaseButton>
        </form>

        <p class="text-center text-sm text-dim mt-6">
          아직 계정이 없으신가요?
          <RouterLink to="/register" class="text-brand hover:text-brand-hover ml-1">회원가입</RouterLink>
        </p>

        <div class="mt-10 rounded-lg bg-surface border border-hairline p-4">
          <p class="text-faint text-xs leading-relaxed">
            화면 껍데기입니다. 실제 로그인은 auth-server의 OAuth2 인가 코드 흐름
            (<span class="font-mono">/oauth2/authorize</span> →
            <span class="font-mono">/oauth2/token</span>)을 씁니다.
            백엔드 스택을 띄운 뒤 연결합니다.
          </p>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { signIn as store } from '@/store/auth'

const router = useRouter()
const email = ref('')
const password = ref('')

function signIn() {
  // TODO: GET /oauth2/authorize → 콜백에서 POST /oauth2/token 으로 교체.
  store(email.value || 'guest@odok.kr')
  router.push('/mypage')
}
</script>
