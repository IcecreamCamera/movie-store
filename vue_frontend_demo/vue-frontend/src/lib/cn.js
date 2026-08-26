import { twMerge } from 'tailwind-merge'

// shadcn/ui의 cn() 헬퍼 대응.
// 컴포넌트 기본 클래스와 호출부에서 넘긴 클래스가 충돌할 때(예: text-primary-foreground vs text-black)
// 뒤에 온 쪽이 이기도록 병합한다.
export function cn(...inputs) {
  return twMerge(inputs.flat(Infinity).filter(Boolean).join(' '))
}
