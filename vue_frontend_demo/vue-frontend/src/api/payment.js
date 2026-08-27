import api from './index.js'

// 사용자 결제 내역
export function byUser(userId) {
  return api.get(`/api/payments/user/${userId}`)
}
