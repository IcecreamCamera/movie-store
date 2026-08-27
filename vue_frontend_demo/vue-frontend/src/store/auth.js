import { reactive, computed } from 'vue'
import { exchangeCode, getMe, loginWithPassword } from '@/api/auth.js'
import { TOKEN_KEY, USER_KEY } from '@/api/index.js'

// 실제 세션 저장소. accessToken은 새로고침에도 살아남도록 sessionStorage에 보관한다.
//   loginAndAuthorize() -> POST /login (앱 화면에서 자격증명 제출) + GET /oauth2/authorize (리다이렉트)
//   startLogin()    -> GET /oauth2/authorize (auth-server로 리다이렉트, 호스팅된 로그인 화면이 필요할 때만)
//   completeLogin() -> POST /oauth2/token (콜백에서 인가 코드 교환) + GET /api/users/me
//   register        -> POST /api/users/register (RegisterView에서 호출)

// 자격증명이 틀렸음을 나타내는 전용 에러. LoginView는 이 에러만 인라인 메시지로 처리하고,
// 그 외 에러(네트워크 문제 등)는 그대로 전파한다.
export class CredentialError extends Error {}

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

// authorize 요청 URL 구성. 이 값들은 auth-server에 등록된 client와 정확히 일치해야
// 하므로 .env 밖에서 바꾸면 안 된다. startLogin()과 loginAndAuthorize() 둘 다 이 URL로
// 이동시킨다 - 인가 코드 발급 방식 자체는 동일하고, 그 전에 세션(JSESSIONID)을 앱 화면에서
// 만드느냐 auth-server의 호스팅된 로그인 화면에서 만드느냐만 다르다.
function buildAuthorizeUrl() {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: 'openid'
  })
  return `${API_BASE_URL}/oauth2/authorize?${params.toString()}`
}

// auth-server의 인가 코드 흐름 시작 (호스팅된 로그인 화면으로 이동). 지금은 LoginView가
// 앱 내 폼(loginAndAuthorize)을 쓰지만, 필요하면 여전히 유효한 진입점이라 남겨둔다.
export function startLogin() {
  window.location.href = buildAuthorizeUrl()
}

// 앱 페이지에서 이메일/비밀번호를 받아 auth-server(:8080)에 폼 로그인으로 세션을 만들고,
// 곧바로 authorize 엔드포인트로 이동해 인가 코드를 발급받는다(콜백에서 completeLogin이 처리).
//
// 자격증명 실패를 감지하는 방법: OAuth 2.1에는 패스워드 그랜트가 없고, 브라우저는 로그인
// 302 응답의 Location(/login?error 여부)을 읽을 수 없다. auth-server의 /userinfo가 세션
// 쿠키만으로 인증 여부를 구분해주면 이를 프리플라이트 삼아 판정할 수 있을지 확인했지만,
// /userinfo는 세션 쿠키가 아니라 Bearer 액세스 토큰을 요구하는 OIDC 엔드포인트라
// (WWW-Authenticate: Bearer, 세션 유무와 무관하게 401) 세션 유효성 판정에 쓸 수 없었다.
// 따라서 더 단순한 규칙으로 대체한다: 로그인 POST 자체가 401/403을 반환하면 자격증명 실패로
// 간주하고, 그 외의 경우(200/302 또는 브라우저가 리다이렉트를 따라가며 발생시키는 CORS성
// 네트워크 에러 포함)는 세션이 만들어졌다고 보고 authorize로 진행시켜 결과를 맡긴다.
export async function loginAndAuthorize(email, password) {
  try {
    const res = await loginWithPassword(email, password)
    if (res?.status === 401 || res?.status === 403) {
      throw new CredentialError('이메일 또는 비밀번호가 올바르지 않습니다.')
    }
  } catch (err) {
    if (err instanceof CredentialError) {
      throw err
    }
    const status = err?.response?.status
    if (status === 401 || status === 403) {
      throw new CredentialError('이메일 또는 비밀번호가 올바르지 않습니다.')
    }
    // 그 외 에러(예: 브라우저가 302를 따라가다 CORS 때문에 응답을 읽지 못해 던지는
    // 네트워크 에러)는 자격증명 실패로 단정할 근거가 없으므로 무시하고 진행한다.
  }

  window.location.href = buildAuthorizeUrl()
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
