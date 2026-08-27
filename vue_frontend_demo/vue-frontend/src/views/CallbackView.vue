<template>
  <div class="min-h-screen bg-background flex items-center justify-center px-8">
    <div class="w-full max-w-sm text-center">
      <template v-if="!errorMessage">
        <div
          class="mx-auto mb-6 h-10 w-10 rounded-full border-2 border-hairline border-t-brand animate-spin"
        ></div>
        <p class="text-dim">{{ message }}</p>
      </template>

      <template v-else>
        <p class="text-foreground font-semibold mb-2">로그인에 실패했어요.</p>
        <p class="text-dim text-sm mb-6">{{ errorMessage }}</p>
        <RouterLink to="/login" class="text-brand hover:text-brand-hover">
          로그인으로 돌아가기
        </RouterLink>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { completeLogin } from '@/store/auth'

const route = useRoute()
const router = useRouter()

const message = ref('로그인 처리 중')
const errorMessage = ref('')

onMounted(async () => {
  const code = route.query.code
  const error = route.query.error
  const errorDescription = route.query.error_description

  if (error) {
    errorMessage.value = String(errorDescription || error)
    return
  }

  if (!code) {
    errorMessage.value = '인가 코드(code)가 없습니다.'
    return
  }

  try {
    await completeLogin(String(code))
    message.value = '로그인 완료! 이동 중입니다...'
    router.replace('/')
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message || err?.message || '로그인 처리에 실패했습니다.'
  }
})
</script>
