import { reactive, computed } from 'vue'

// 예매·결제 내역 임시 저장소.
//
// 백엔드 연동 전까지 화면 흐름(예매 → 결제 → 내역)을 보여주기 위한 것입니다.
// 새로고침하면 사라집니다.
//
// 연동 시 교체할 곳:
//   add()        → POST /api/bookings { movieId, quantity }
//   bookings     → GET  /api/bookings/my
//   payments     → GET  /api/payments/user/{userId}

const state = reactive({
  items: []
})

let seq = 0

/**
 * 예매 1건을 기록합니다.
 * 백엔드에서는 예매 생성 → 결제 요청 → payment.completed 수신 순으로 진행되고,
 * 결제가 끝나야 예매가 CONFIRMED 됩니다. 여기서는 모의 결제라 즉시 완료로 둡니다.
 *
 * ticketAmount는 영화 금액, snacks는 매점에서 함께 고른 간식입니다.
 * 결제 금액은 둘을 합친 값입니다.
 */
export function addBooking({ movie, quantity, amount, snacks = [] }) {
  seq += 1
  const now = new Date()

  const snackItems = snacks.map((s) => ({
    id: s.id,
    name: s.name,
    taste: s.taste,
    price: s.price,
    quantity: s.quantity ?? 1
  }))
  const snackAmount = snackItems.reduce((sum, s) => sum + s.price * s.quantity, 0)
  const totalAmount = amount + snackAmount

  const item = {
    bookingId: seq,
    bookingNo: `B${String(now.getFullYear()).slice(2)}${String(seq).padStart(4, '0')}`,
    movieId: movie?.id ?? null,
    movieTitle: movie?.title ?? '',
    genre: movie?.genre ?? '',
    quantity,
    ticketAmount: amount,
    snacks: snackItems,
    snackAmount,
    amount: totalAmount,
    status: 'CONFIRMED',
    payment: {
      paymentId: seq,
      // 백엔드는 UUID를 발급합니다 (payments.transaction_id).
      transactionId: `${now.getTime().toString(36)}-${seq}`.toUpperCase(),
      amount: totalAmount,
      status: 'COMPLETED',
      method: '간편결제 (모의)',
      paidAt: now
    },
    createdAt: now
  }
  state.items.unshift(item)
  return item
}

export const bookings = computed(() => state.items)

export const payments = computed(() =>
  state.items.map((b) => ({
    ...b.payment,
    bookingNo: b.bookingNo,
    movieTitle: b.movieTitle,
    quantity: b.quantity
  }))
)

export const totalPaid = computed(() =>
  state.items.reduce((sum, b) => sum + b.payment.amount, 0)
)
