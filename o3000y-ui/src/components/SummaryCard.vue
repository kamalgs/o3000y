<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ value: string; label: string; sql: string }>()
defineEmits<{ copyLink: [] }>()

const showSQL = ref(false)
</script>

<template>
  <div class="card">
    <div class="flex items-center justify-between">
      <div>
        <div class="text-xs text-[var(--color-text-secondary)] uppercase tracking-wide">{{ label }}</div>
        <div class="text-3xl font-semibold mt-0.5" style="font-variant-numeric: tabular-nums">{{ value }}</div>
      </div>
      <div class="flex gap-2">
        <button class="btn btn-ghost" @click="$emit('copyLink')">Copy link</button>
        <button class="btn btn-ghost" @click="showSQL = !showSQL">
          {{ showSQL ? 'Hide SQL' : 'Show SQL' }}
        </button>
      </div>
    </div>
    <div v-if="showSQL" class="sql-display mt-3">{{ sql }}</div>
  </div>
</template>
