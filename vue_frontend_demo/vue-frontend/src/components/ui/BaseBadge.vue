<template>
  <span :class="classes" v-bind="rest">
    <slot />
  </span>
</template>

<script setup>
import { computed, useAttrs } from 'vue'
import { cn } from '@/lib/cn'

const props = defineProps({
  variant: { type: String, default: 'default' }
})

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()
const rest = computed(() => {
  const { class: _c, ...others } = attrs
  return others
})

const BASE =
  'inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium w-fit whitespace-nowrap shrink-0 gap-1'

const VARIANTS = {
  default: 'border-transparent bg-primary text-primary-foreground',
  outline: 'border-border text-foreground'
}

const classes = computed(() => cn(BASE, VARIANTS[props.variant] || VARIANTS.default, attrs.class))
</script>
