<template>
  <footer class="bg-gray-900 text-white mt-20">
    <div class="max-w-7xl mx-auto px-8 lg:px-16 py-12">
      <div class="flex justify-between items-start">
        <div class="flex-1">
          <h3 class="text-2xl font-bold text-red-400 mb-4">MovieSSG</h3>
          <p class="text-gray-400 mb-4 text-lg">
            영화를 사랑하는 모든 이들을 위한 스트리밍 서비스
          </p>
          <p class="text-gray-400 text-base">© 2024 MovieSSG. All rights reserved.</p>
        </div>
        <div class="flex gap-[200px]">
          <div v-for="col in columns" :key="col.title" class="w-24">
            <h4 class="font-semibold mb-4 text-lg">{{ col.title }}</h4>
            <ul class="space-y-2 text-gray-400 text-base">
              <li v-for="link in col.links" :key="link">
                <a href="#" class="hover:text-white">{{ link }}</a>
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
  { title: '회사정보', links: ['회사소개', '이용약관', '개인정보처리방침', '제휴문의'] },
  { title: '연결하기', links: ['인스타그램', '페이스북', '트위터', '유튜브'] }
]
</script>
