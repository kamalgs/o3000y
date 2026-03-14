<script setup lang="ts">
import { computed, ref } from 'vue'
import { formatNumber, formatTimestamp } from '@/lib/formatting'

export interface Series {
  name: string
  data: { bucket: string; value: number }[]
  color: string
}

const props = defineProps<{ series: Series[]; label: string }>()

const hoveredIndex = ref<number | null>(null)

const W = 800
const H = 260
const P = { t: 20, r: 20, b: 50, l: 60 }
const cw = W - P.l - P.r
const ch = H - P.t - P.b

const allBuckets = computed(() => {
  const set = new Set<string>()
  for (const s of props.series) for (const d of s.data) set.add(d.bucket)
  return [...set].sort()
})

const yRange = computed(() => {
  let min = Infinity
  let max = -Infinity
  for (const s of props.series) for (const d of s.data) {
    if (d.value < min) min = d.value
    if (d.value > max) max = d.value
  }
  if (!isFinite(min)) return { min: 0, max: 1 }
  const pad = (max - min) * 0.1 || 1
  return { min: Math.max(0, min - pad), max: max + pad }
})

function x(i: number): number {
  const n = allBuckets.value.length
  return P.l + (n <= 1 ? cw / 2 : (i / (n - 1)) * cw)
}

function y(v: number): number {
  const { min, max } = yRange.value
  return P.t + ch - ((v - min) / (max - min)) * ch
}

function linePath(s: Series): string {
  const bucketMap = new Map(s.data.map((d) => [d.bucket, d.value]))
  return allBuckets.value
    .map((b, i) => {
      const v = bucketMap.get(b)
      if (v == null) return null
      return `${i === 0 || !bucketMap.has(allBuckets.value[i - 1]) ? 'M' : 'L'}${x(i)},${y(v)}`
    })
    .filter(Boolean)
    .join(' ')
}

const yTicks = computed(() => {
  const { min, max } = yRange.value
  return Array.from({ length: 5 }, (_, i) => min + ((max - min) * i) / 4)
})

const isSingle = computed(() => props.series.length === 1)

function onMouseMove(e: MouseEvent) {
  const svg = e.currentTarget as SVGSVGElement
  const rect = svg.getBoundingClientRect()
  const mx = ((e.clientX - rect.left) / rect.width) * W - P.l
  if (mx < 0 || mx > cw || allBuckets.value.length === 0) {
    hoveredIndex.value = null
    return
  }
  hoveredIndex.value = Math.round((mx / cw) * (allBuckets.value.length - 1))
}

function tooltipValues(): { name: string; value: string; color: string }[] {
  if (hoveredIndex.value == null) return []
  const bucket = allBuckets.value[hoveredIndex.value]
  return props.series.map((s) => {
    const d = s.data.find((d) => d.bucket === bucket)
    return { name: s.name, value: d != null ? formatNumber(d.value) : '-', color: s.color }
  })
}
</script>

<template>
  <div class="card">
    <div class="flex items-center justify-between mb-2">
      <span class="form-label mb-0">{{ label }}</span>
      <div v-if="!isSingle" class="flex flex-wrap gap-3">
        <span v-for="s in series" :key="s.name" class="text-xs flex items-center gap-1">
          <span class="legend-swatch" :style="{ background: s.color }" />
          {{ s.name }}
        </span>
      </div>
    </div>

    <div v-if="allBuckets.length === 0" class="text-center py-12 text-sm" style="color: var(--color-text-muted)">
      No data for selected time range
    </div>

    <svg
      v-else
      :viewBox="`0 0 ${W} ${H}`"
      class="w-full"
      style="max-height: 280px"
      @mousemove="onMouseMove"
      @mouseleave="hoveredIndex = null"
    >
      <!-- grid -->
      <line
        v-for="t in yTicks" :key="t"
        :x1="P.l" :x2="P.l + cw"
        :y1="y(t)" :y2="y(t)"
        stroke="var(--color-border-light)" stroke-width="1"
      />
      <text
        v-for="t in yTicks" :key="'yl' + t"
        :x="P.l - 6" :y="y(t) + 3"
        text-anchor="end" fill="var(--color-text-muted)" font-size="10"
      >{{ formatNumber(t) }}</text>

      <!-- x labels -->
      <template v-for="(b, i) in allBuckets" :key="'xl' + i">
        <text
          v-if="allBuckets.length <= 8 || i % Math.ceil(allBuckets.length / 8) === 0"
          :x="x(i)" :y="P.t + ch + 20"
          text-anchor="middle" fill="var(--color-text-muted)" font-size="10"
        >{{ formatTimestamp(b) }}</text>
      </template>

      <!-- area (single series only) -->
      <path
        v-if="isSingle && series[0]?.data.length > 1"
        :d="linePath(series[0]) + ` L${x(allBuckets.length - 1)},${P.t + ch} L${x(0)},${P.t + ch} Z`"
        :fill="series[0].color" opacity="0.08"
      />

      <!-- lines -->
      <path
        v-for="s in series" :key="s.name"
        :d="linePath(s)" fill="none" :stroke="s.color"
        stroke-width="2" stroke-linejoin="round"
      />

      <!-- hover crosshair -->
      <line
        v-if="hoveredIndex != null"
        :x1="x(hoveredIndex)" :x2="x(hoveredIndex)"
        :y1="P.t" :y2="P.t + ch"
        stroke="var(--color-text-muted)" stroke-width="1" stroke-dasharray="3 2"
      />
    </svg>

    <!-- tooltip -->
    <div
      v-if="hoveredIndex != null"
      class="text-center text-xs mt-1"
      style="color: var(--color-text-secondary)"
    >
      <span class="mr-2">{{ formatTimestamp(allBuckets[hoveredIndex]) }}</span>
      <span v-for="tv in tooltipValues()" :key="tv.name" class="mr-3">
        <span class="legend-swatch" :style="{ background: tv.color }" />
        <strong>{{ tv.value }}</strong>
      </span>
    </div>
  </div>
</template>
