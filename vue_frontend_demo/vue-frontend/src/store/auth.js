import { reactive, computed } from 'vue'

// 로그인 상태 임시 저장소.
// 백엔드 연동 시 교체:
//   signIn  → OAuth2 인가 코드 흐름 (/oauth2/authorize → /oauth2/token)
//   user    → GET /api/users/me  (게이트웨이가 X-User-Id를 주입)
//   register→ POST /api/users/register

const state = reactive({ user: null })

export const user = computed(() => state.user)
export const isLoggedIn = computed(() => state.user !== null)

export function signIn(email) {
  state.user = {
    id: 1,
    email,
    name: email.split('@')[0] || '게스트',
    role: 'MEMBER',
    createdAt: new Date()
  }
  return state.user
}

export function signOut() {
  state.user = null
}
