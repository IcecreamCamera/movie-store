import axios from 'axios'
import api from './index.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// OAuth2 Authorization Code -> Access Token 교환.
// CLIENT_SECRET_BASIC: Authorization 헤더에 client_id:client_secret을 Base64로 인코딩해 보낸다.
// auth-server는 사전 등록된 client_id/secret/redirect_uri만 인정하므로 값은 .env 그대로 써야 한다.
export function exchangeCode(code) {
  const clientId = import.meta.env.VITE_CLIENT_ID
  const clientSecret = import.meta.env.VITE_CLIENT_SECRET
  const redirectUri = import.meta.env.VITE_REDIRECT_URI
  const credentials = btoa(`${clientId}:${clientSecret}`)

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: redirectUri
  })

  return axios.post(`${API_BASE_URL}/oauth2/token`, body.toString(), {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Authorization: `Basic ${credentials}`
    }
  })
}

// auth-server(:8080)의 폼 로그인 엔드포인트에 자격증명을 제출해 세션 쿠키(JSESSIONID)를 만든다.
// 공용 api 인스턴스를 쓰지 않는 이유: 그 인스턴스는 Content-Type을 JSON으로 고정하고
// Authorization: Bearer 헤더를 자동으로 붙이는데, 둘 다 이 요청에는 맞지 않는다.
// Spring Security의 폼 로그인 필터는 application/x-www-form-urlencoded 바디의
// username/password 파라미터만 읽으며, 필드명은 이메일을 담더라도 반드시 "username"이어야 한다.
//
// 상대 경로(/login)로 요청한다: nginx가 이 경로를 api-gateway로 프록시해 프론트(:3000)와
// 동일 출처를 유지하기 때문에, 302 리다이렉트 체인을 브라우저가 따라가는 동안에도
// Origin이 null이 되어 CORS에 막히는 일이 없다. 이 덕분에 체인의 최종 URL
// (response.request.responseURL)을 읽어 성공/실패를 판정할 수 있다 - 판정은 호출부(store)에서 한다.
//
// maxRedirects/validateStatus는 Node용 axios 옵션이라 브라우저에서는 아무 효과가 없으므로
// 지정하지 않는다 - 브라우저가 302 체인을 정상적으로 따라가게 둔다.
export function loginWithPassword(email, password) {
  return axios.post(
    '/login',
    new URLSearchParams({ username: email, password }).toString(),
    {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      withCredentials: true
    }
  )
}

// 내 정보 조회 (게이트웨이가 토큰에서 뽑은 X-User-Id로 사용자를 식별)
export function getMe() {
  return api.get('/api/users/me')
}

// 회원가입
export function register(data) {
  return api.post('/api/users/register', data)
}
