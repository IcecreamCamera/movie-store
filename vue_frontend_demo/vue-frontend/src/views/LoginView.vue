<template>
  <div class="min-h-screen bg-background flex flex-col">
    <AppHeader current-page="login" />

    <div class="flex-1 flex items-center justify-center px-8 py-16">
      <div class="w-full max-w-sm">
        <h1 class="text-2xl font-bold text-foreground mb-1">로그인</h1>
        <p class="text-dim text-sm mb-8">예매하려면 로그인이 필요해요.</p>

        <!--
          XHR 이 아니라 진짜 폼 POST 다. 브라우저가 302 체인을 직접 따라가야
          auth-server 가 저장해 둔 인가 요청을 이어서 처리하고 /callback 으로 코드를 넘겨준다.
          XHR 로 보내면 리다이렉트를 XHR 이 따라가면서 인가 코드를 소모해 버려,
          정작 CallbackView 가 열릴 때는 이미 쓴 코드가 된다.
          필드명이 username 인 것은 Spring Security 폼 로그인 필터의 규약이다(값은 이메일).
        -->
        <form class="space-y-4" method="post" action="/login">
          <div>
            <label for="email" class="block text-sm text-dim mb-2">이메일</label>
            <BaseInput id="email" name="username" v-model="email" type="email" placeholder="you@example.com"
              class="bg-surface border-hairline text-foreground placeholder:text-faint" />
          </div>
          <div>
            <label for="pw" class="block text-sm text-dim mb-2">비밀번호</label>
            <BaseInput id="pw" name="password" v-model="password" type="password" placeholder="8자 이상"
              class="bg-surface border-hairline text-foreground placeholder:text-faint" />
          </div>

          <p v-if="errorMessage" class="text-brand text-xs">{{ errorMessage }}</p>

          <BaseButton
            type="submit"
            class="w-full bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold"
          >
            로그인
          </BaseButton>
        </form>

        <p class="text-center text-sm text-dim mt-6">
          아직 계정이 없으신가요?
          <RouterLink to="/register" class="text-brand hover:text-brand-hover ml-1">회원가입</RouterLink>
        </p>

        <div class="mt-10 rounded-lg bg-surface border border-hairline p-4">
          <p class="text-faint text-xs leading-relaxed">
            이메일과 비밀번호를 입력하고 로그인을 누르면 이 화면에서 바로 인증을 마치고
            홈으로 돌아옵니다. 별도의 로그인 화면으로 이동하지 않아요.
          </p>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { buildAuthorizeUrl } from '@/store/auth'

const route = useRoute()

const email = ref('')
const password = ref('')
const errorMessage = ref('')

// 이 화면은 /oauth2/authorize 를 거쳐 도달해야 한다. auth-server 가 그때 인가 요청을
// 세션에 저장해 두고, 로그인 성공 후 그것을 이어서 처리해 /callback 으로 코드를 넘긴다.
// 주소창에 /login 을 직접 친 경우에는 저장된 인가 요청이 없어 로그인해도 코드가 나오지 않으므로,
// 인가 요청부터 다시 시작시킨다. authorize 가 다시 여기로 돌려보내면 그때는 플래그가 있어
// 반복되지 않는다.
const VISITED_KEY = 'login_via_authorize'

onMounted(() => {
  if (route.query.error !== undefined) {
    errorMessage.value = '이메일 또는 비밀번호가 올바르지 않습니다.'
    return
  }

  if (sessionStorage.getItem(VISITED_KEY)) {
    sessionStorage.removeItem(VISITED_KEY)
    return
  }

  sessionStorage.setItem(VISITED_KEY, '1')
  window.location.href = buildAuthorizeUrl()
})
</script>
