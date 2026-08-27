<template>
  <div class="min-h-screen bg-background flex flex-col">
    <AppHeader current-page="register" />

    <div class="flex-1 flex items-center justify-center px-8 py-16">
      <div class="w-full max-w-sm">
        <h1 class="text-2xl font-bold text-foreground mb-1">회원가입</h1>
        <p class="text-dim text-sm mb-8">가입하면 예매와 간식 추천을 쓸 수 있어요.</p>

        <form class="space-y-4" @submit.prevent="submit">
          <div>
            <label for="name" class="block text-sm text-dim mb-2">이름</label>
            <BaseInput id="name" v-model="form.name" placeholder="홍길동"
              class="bg-surface border-hairline text-foreground placeholder:text-faint" />
          </div>
          <div>
            <label for="email2" class="block text-sm text-dim mb-2">이메일</label>
            <BaseInput id="email2" v-model="form.email" type="email" placeholder="you@example.com"
              class="bg-surface border-hairline text-foreground placeholder:text-faint" />
          </div>
          <div>
            <label for="pw2" class="block text-sm text-dim mb-2">비밀번호</label>
            <BaseInput id="pw2" v-model="form.password" type="password" placeholder="8자 이상"
              class="bg-surface border-hairline text-foreground placeholder:text-faint" />
            <p v-if="tooShort" class="text-brand text-xs mt-2">비밀번호는 8자 이상이어야 해요.</p>
          </div>

          <p v-if="errorMessage" class="text-brand text-xs">{{ errorMessage }}</p>

          <BaseButton
            type="submit"
            class="w-full bg-brand text-[#1A1408] hover:bg-brand-hover font-semibold"
            :disabled="submitting"
          >
            {{ submitting ? '가입 중...' : '가입하기' }}
          </BaseButton>
        </form>

        <p class="text-center text-sm text-dim mt-6">
          이미 계정이 있으신가요?
          <RouterLink to="/login" class="text-brand hover:text-brand-hover ml-1">로그인</RouterLink>
        </p>

        <div class="mt-10 rounded-lg bg-surface border border-hairline p-4">
          <p class="text-faint text-xs leading-relaxed">
            <span class="font-mono">POST /api/users/register</span>
            (name, email, password 8자 이상, role)로 가입합니다. 가입 후에는 직접 로그인해주세요.
          </p>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { register } from '@/api/auth.js'

const router = useRouter()
const form = reactive({ name: '', email: '', password: '' })

const tooShort = computed(() => form.password.length > 0 && form.password.length < 8)
const submitting = ref(false)
const errorMessage = ref('')

// POST /api/users/register. 화면에 역할 선택 UI가 없어 일반 회원(STUDENT)으로 고정 가입한다.
async function submit() {
  if (tooShort.value || submitting.value) return
  errorMessage.value = ''
  submitting.value = true
  try {
    await register({
      name: form.name,
      email: form.email,
      password: form.password,
      role: 'STUDENT'
    })
    router.push('/login')
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message || err?.message || '회원가입에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>
