<script setup lang="ts">
import { computed } from 'vue'
import type { QueryResponse } from '../api/client'

const props = defineProps<{ result: QueryResponse }>()

const BUCKET_COUNT = 20

const histogram = computed(() => {
  const durIndex = props.result.columns.indexOf('duration_us')
  if (durIndex === -1) return null

  const durations = props.result.rows
    .map((row) => Number(row[durIndex]) || 0)
    .filter((d) => d > 0)

  if (durations.length === 0) return null

  const min = Math.min(...durations)
  const max = Math.max(...durations)
  if (min === max) return null

  const bucketSize = (max - min) / BUCKET_COUNT
  const buckets: { min: number; max: number; count: number }[] = []
  for (let i = 0; i < BUCKET_COUNT; i++) {
    buckets.push({
      min: min + i * bucketSize,
      max: min + (i + 1) * bucketSize,
      count: 0,
    })
  }

  for (const d of durations) {
    const idx = Math.min(Math.floor((d - min) / bucketSize), BUCKET_COUNT - 1)
    buckets[idx].count++
  }

  const maxCount = Math.max(...buckets.map((b) => b.count))
  return { buckets, maxCount }
})

function formatDuration(us: number): string {
  if (us < 1000) return `${Math.round(us)}us`
  if (us < 1_000_000) return `${(us / 1000).toFixed(0)}ms`
  return `${(us / 1_000_000).toFixed(1)}s`
}
</script>

<template>
  <div v-if="histogram" class="bg-white rounded-lg border border-gray-200 p-4">
    <h3 class="text-sm font-medium text-gray-700 mb-3">Duration Distribution</h3>
    <div class="flex items-end gap-0.5 h-24">
      <div
        v-for="(bucket, i) in histogram.buckets"
        :key="i"
        class="flex-1 bg-blue-400 hover:bg-blue-500 rounded-t cursor-default relative group"
        :style="{ height: `${(bucket.count / histogram.maxCount) * 100}%`, minHeight: bucket.count > 0 ? '2px' : '0' }"
      >
        <div
          class="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 bg-gray-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 whitespace-nowrap pointer-events-none"
        >
          {{ bucket.count }} spans ({{ formatDuration(bucket.min) }} - {{ formatDuration(bucket.max) }})
        </div>
      </div>
    </div>
    <div class="flex justify-between text-xs text-gray-400 mt-1">
      <span>{{ formatDuration(histogram.buckets[0].min) }}</span>
      <span>{{ formatDuration(histogram.buckets[histogram.buckets.length - 1].max) }}</span>
    </div>
  </div>
</template>
