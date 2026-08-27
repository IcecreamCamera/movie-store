<template>
  <footer class="bg-surface border-t border-hairline text-foreground mt-20">
    <div class="max-w-7xl mx-auto px-8 lg:px-16 py-12">
      <div class="flex justify-between items-start">
        <div class="flex-1">
          <h3 class="text-2xl font-bold text-brand mb-4">오도독</h3>
          <p class="text-dim mb-4 text-lg">
            영화 장르에 어울리는 매점 간식을 추천하는 서비스
          </p>
          <p class="text-faint text-base">© 2026 오도독. All rights reserved.</p>
        </div>
        <div class="flex gap-[200px]">
          <div v-for="col in columns" :key="col.title" class="w-24">
            <h4 class="font-semibold mb-4 text-lg">{{ col.title }}</h4>
            <ul class="space-y-2 text-dim text-base">
              <li v-for="link in col.links" :key="link">
                <a href="#" class="hover:text-brand transition-colors">{{ link }}</a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
const columns = [
  { title: '고객서비스', links: ['자주 묻는 질문', '이용 안내', '문의하기', '고객센터'] },
  { title: '회사정보', links: ['회사소개', '이용약관', '개인정보처리방침', '제휴문의'] }
]
</script>
