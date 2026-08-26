<template>
  <input :value="modelValue" :class="classes" v-bind="rest" @input="onInput" />
</template>

<script setup>
import { computed, useAttrs } from 'vue'
import { cn } from '@/lib/cn'

defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()
const rest = computed(() => {
  const { class: _c, ...others } = attrs
  return others
})

const BASE =
  'flex h-9 w-full min-w-0 rounded-md border border-border bg-input-background px-3 py-1 text-base ' +
  'transition-colors outline-none placeholder:text-muted-foreground ' +
  'focus-visible:ring-2 focus-visible:ring-ring/50 md:text-sm'

const classes = computed(() => cn(BASE, attrs.class))

function onInput(e) {
  emit('update:modelValue', e.target.value)
}
</script>
