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

          <p v-if="errorMessage" class="text-brand text-xs">{{ errorMessage }}</p>

          <BaseButton
            type="submit"
            :disabled="isSubmitting"
            class="w-full bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold"
          >
            {{ isSubmitting ? '로그인 중...' : '로그인' }}
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
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { loginAndAuthorize, CredentialError } from '@/store/auth'

const email = ref('')
const password = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')

// 앱 화면에서 자격증명을 받아 세션을 만들고 authorize로 이동한다. 성공하면 이 화면을
// 떠나 /callback을 거쳐 홈으로 돌아오고, 자격증명이 틀리면 이 화면에 남아 인라인
// 메시지를 보여준다.
async function onSubmit() {
  if (!email.value.trim() || !password.value.trim()) {
    errorMessage.value = '이메일과 비밀번호를 모두 입력해 주세요.'
    return
  }

  errorMessage.value = ''
  isSubmitting.value = true
  try {
    await loginAndAuthorize(email.value, password.value)
    // 성공 시 loginAndAuthorize 내부에서 window.location.href로 이동하므로
    // 이 컴포넌트는 곧 언마운트된다.
  } catch (err) {
    if (err instanceof CredentialError) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    }
    isSubmitting.value = false
  }
}
</script>
