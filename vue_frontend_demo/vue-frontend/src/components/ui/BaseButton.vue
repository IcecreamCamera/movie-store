<template>
  <button :class="classes" v-bind="rest">
    <slot />
  </button>
</template>

<script setup>
import { computed, useAttrs } from 'vue'
import { cn } from '@/lib/cn'

// shadcn/ui의 Button을 최소 구현으로 옮긴 것.
// 원본에서 쓰는 variant(default/ghost/outline)와 size(default/sm/icon)만 지원한다.
const props = defineProps({
  variant: { type: String, default: 'default' },
  size: { type: String, default: 'default' }
})

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()
const rest = computed(() => {
  const { class: _c, ...others } = attrs
  return others
})

const BASE =
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ' +
  'transition-colors cursor-pointer disabled:pointer-events-none disabled:opacity-50 ' +
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50'

const VARIANTS = {
  default: 'bg-primary text-primary-foreground hover:bg-primary/90',
  ghost: 'hover:bg-accent hover:text-accent-foreground',
  outline: 'border border-border bg-transparent hover:bg-accent hover:text-accent-foreground'
}

const SIZES = {
  default: 'h-9 px-4 py-2',
  sm: 'h-8 rounded-md px-3 text-xs',
  icon: 'h-9 w-9 p-0'
}

const classes = computed(() =>
  cn(BASE, VARIANTS[props.variant] || VARIANTS.default, SIZES[props.size] || SIZES.default, attrs.class)
)
</script>
