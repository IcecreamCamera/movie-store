<template>
  <div class="rounded-lg bg-surface-2 border border-hairline p-4 flex flex-col gap-3">
    <div class="flex items-center justify-between gap-2">
      <span class="font-semibold text-foreground">{{ snack.name }}</span>
      <span class="shrink-0 rounded px-2 py-0.5 text-xs bg-brand-dim text-brand">{{ snack.taste }}</span>
    </div>
    <div class="mt-auto text-sm text-brand">{{ snack.price.toLocaleString() }}원</div>
  </div>
</template>

<script setup>
defineProps({
  snack: { type: Object, required: true }
})
</script>
