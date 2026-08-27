import axios from 'axios'

// 세션 저장 키. store/auth.js와 동일한 키를 써야 한다.
export const TOKEN_KEY = 'access_token'
export const USER_KEY = 'user'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// JWT exp 를 읽어 만료 여부를 판단한다. 읽지 못하면 만료로 간주한다.
function isExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

// 유효한 토큰이 있을 때만 Authorization 헤더에 실어 보낸다.
//
// 만료된 토큰을 붙이면 회원가입(/api/users/register)처럼 인증이 필요 없는 요청까지 401 이 된다.
// 게이트웨이의 리소스 서버는 Authorization 헤더가 있으면 공개 경로라도 먼저 검증하고,
// 토큰이 무효하면 거부하기 때문이다. 그래서 만료된 토큰은 붙이지 않고 즉시 지운다.
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY)

  if (token && isExpired(token)) {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
    return config
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 토큰을 실어 보낸 요청이 401 이면 세션이 끊긴 것이므로 지우고 로그인 화면으로 보낸다.
//
// 토큰 없이 보낸 요청의 401 은 여기서 가로채지 않는다. 그런 401 은 "로그인이 필요한 요청을
// 비로그인 상태로 보냈다"는 뜻이고, 호출한 화면이 직접 처리해야 할 정상적인 응답이다.
// 이것까지 로그인 화면으로 튕기면 회원가입 실패 같은 원인이 화면에 드러나지 않는다.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const hadToken = Boolean(err.config?.headers?.Authorization)

    if (err.response?.status === 401 && hadToken) {
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(USER_KEY)
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default api
