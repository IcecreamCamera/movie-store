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

// authorize 요청 URL 구성. redirect_uri(REDIRECT_URI)는 auth-server에 등록된 client와
// 정확히 일치해야 하므로 절대 URL(.env의 VITE_REDIRECT_URI) 그대로 둔다. 반면 요청을 보내는
// 경로 자체는 상대 경로로 둬서 nginx가 프론트(:3000)와 동일 출처로 프록시하게 한다.
// startLogin()과 loginAndAuthorize() 둘 다 이 URL로 이동시킨다 - 인가 코드 발급 방식 자체는
// 동일하고, 그 전에 세션(JSESSIONID)을 앱 화면에서 만드느냐 auth-server의 호스팅된 로그인
// 화면에서 만드느냐만 다르다.
function buildAuthorizeUrl() {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: 'openid'
  })
  return `/oauth2/authorize?${params.toString()}`
}

// auth-server의 인가 코드 흐름 시작 (호스팅된 로그인 화면으로 이동). 지금은 LoginView가
// 앱 내 폼(loginAndAuthorize)을 쓰지만, 필요하면 여전히 유효한 진입점이라 남겨둔다.
export function startLogin() {
  window.location.href = buildAuthorizeUrl()
}

// 앱 페이지에서 이메일/비밀번호를 받아 auth-server(:8080)에 폼 로그인으로 세션을 만들고,
// 곧바로 authorize 엔드포인트로 이동해 인가 코드를 발급받는다(콜백에서 completeLogin이 처리).
//
// 자격증명 실패를 감지하는 방법: 로그인 성공/실패 모두 302로 응답하므로(401/403은 오지 않는다)
// 상태 코드로는 구분할 수 없다. auth-server의 /userinfo도 세션 쿠키가 아니라 Bearer 액세스
// 토큰을 요구하는 OIDC 엔드포인트라(WWW-Authenticate: Bearer, 세션 유무와 무관하게 401)
// 세션 유효성 판정에 쓸 수 없다. 대신 브라우저가 302 체인을 다 따라간 뒤 최종적으로 도달한
// URL(response.request.responseURL)을 읽는다 - 실패 시 .../login?error로, 성공 시
// 프론트 콜백(.../callback?code=...)으로 끝난다. 이 값을 JS가 읽으려면 체인 전체가
// 동일 출처여야 하므로(nginx가 /login, /oauth2 등을 프록시), 이 함수는 상대 경로로만 호출한다.
//
// responseURL이 비어 있거나 읽을 수 없으면 추측하지 않고 authorize로 진행시킨다 - 정상
// 로그인을 "비밀번호 오류"로 잘못 판정하는 것이 이전의 미탐지보다 더 나쁘다.
export async function loginAndAuthorize(email, password) {
  const res = await loginWithPassword(email, password)
  const finalUrl = res?.request?.responseURL || ''

  if (finalUrl.includes('login?error') || finalUrl.includes('error=')) {
    throw new CredentialError('이메일 또는 비밀번호가 올바르지 않습니다.')
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
