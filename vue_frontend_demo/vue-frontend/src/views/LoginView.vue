<template>
  <div class="min-h-screen bg-background flex flex-col">
    <AppHeader current-page="login" />

    <div class="flex-1 flex items-center justify-center px-8 py-16">
      <div class="w-full max-w-sm">
        <h1 class="text-2xl font-bold text-foreground mb-1">로그인</h1>
        <p class="text-dim text-sm mb-8">예매하려면 로그인이 필요해요.</p>

        <form class="space-y-4" @submit.prevent="onSubmit">
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
            로그인 버튼을 누르면 auth-server의 OAuth2 인가 코드 흐름
            (<span class="font-mono">/oauth2/authorize</span> →
            <span class="font-mono">/oauth2/token</span>)으로 이동합니다.
            아이디/비밀번호는 이동한 화면에서 입력해요.
          </p>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { startLogin } from '@/store/auth'

// 이메일/비밀번호 입력칸은 실제 로그인 폼이 auth-server 쪽에 있어 이 화면에서는 쓰지 않는다.
const email = ref('')
const password = ref('')

// GET /oauth2/authorize 로 리다이렉트. 콜백(/callback)에서 인가 코드를 토큰으로 교환한다.
function onSubmit() {
  startLogin()
}
</script>
