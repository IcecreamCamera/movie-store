import { reactive, computed } from 'vue'
import { exchangeCode, getMe } from '@/api/auth.js'
import { TOKEN_KEY, USER_KEY } from '@/api/index.js'

// 실제 세션 저장소. accessToken은 새로고침에도 살아남도록 sessionStorage에 보관한다.
//   startLogin()    -> GET /oauth2/authorize (auth-server로 리다이렉트)
//   completeLogin() -> POST /oauth2/token (콜백에서 인가 코드 교환) + GET /api/users/me
//   register        -> POST /api/users/register (RegisterView에서 호출)

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const CLIENT_ID = import.meta.env.VITE_CLIENT_ID
const REDIRECT_URI = import.meta.env.VITE_REDIRECT_URI

function readStoredUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

const state = reactive({
  accessToken: sessionStorage.getItem(TOKEN_KEY) || null,
  user: readStoredUser()
})

export const user = computed(() => state.user)
export const isLoggedIn = computed(() => state.user !== null)

function setToken(token) {
  state.accessToken = token
  sessionStorage.setItem(TOKEN_KEY, token)
}

function setUser(userData) {
  state.user = userData
  sessionStorage.setItem(USER_KEY, JSON.stringify(userData))
}

// auth-server의 인가 코드 흐름 시작. 이 값들은 auth-server에 등록된 client와
// 정확히 일치해야 하므로 .env 밖에서 바꾸면 안 된다.
export function startLogin() {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: 'openid'
  })
  window.location.href = `${API_BASE_URL}/oauth2/authorize?${params.toString()}`
}

// 콜백에서 받은 인가 코드를 토큰으로 교환하고, 내 정보를 조회해 세션을 채운다.
export async function completeLogin(code) {
  const res = await exchangeCode(code)
  const token = res?.data?.access_token

  if (!token) {
    throw new Error('액세스 토큰을 받지 못했습니다.')
  }

  setToken(token)

  const meRes = await getMe()
  const userData = meRes?.data?.data ?? meRes?.data

  if (!userData || typeof userData !== 'object') {
    throw new Error('사용자 정보를 불러오지 못했습니다.')
  }

  setUser(userData)
  return userData
}

// 저장된 토큰으로 내 정보를 다시 조회해 갱신한다 (마이페이지 진입 시 사용).
export async function refreshUser() {
  const meRes = await getMe()
  const userData = meRes?.data?.data ?? meRes?.data
  if (userData && typeof userData === 'object') {
    setUser(userData)
  }
  return state.user
}

export function signOut() {
  state.accessToken = null
  state.user = null
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}
