<template>
  <img v-if="!failed" :src="src" :alt="alt" :class="attrs.class" @error="failed = true" />
  <div v-else :class="cn('flex items-center justify-center bg-gray-200 text-gray-400', attrs.class)">
    <ImageIcon class="h-8 w-8" />
  </div>
</template>

<script setup>
import { ref, watch, useAttrs } from 'vue'
import { ImageIcon } from '@lucide/vue'
import { cn } from '@/lib/cn'

// 원본 components/figma/ImageWithFallback.tsx 대응.
const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' }
})

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()
const failed = ref(false)
watch(() => props.src, () => (failed.value = false))
</script>
