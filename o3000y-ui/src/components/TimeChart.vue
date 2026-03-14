<script setup lang="ts">
import { computed, ref } from 'vue'

interface DataPoint {
  bucket: string
  value: number
}

const props = defineProps<{
  data: DataPoint[]
  label: string
}>()

const hoveredIndex = ref<number | null>(null)

const WIDTH = 800
const HEIGHT = 260
const PADDING = { top: 20, right: 20, bottom: 50, left: 70 }
const chartW = WIDTH - PADDING.left - PADDING.right
const chartH = HEIGHT - PADDING.top - PADDING.bottom

const yRange = computed(() => {
  if (props.data.length === 0) return { min: 0, max: 1 }
  const vals = props.data.map((d) => d.value)
  const min = Math.min(...vals)
  const max = Math.max(...vals)
  const pad = (max - min) * 0.1 || 1
  return { min: Math.max(0, min - pad), max: max + pad }
})

const points = computed(() => {
  const { min, max } = yRange.value
  const n = props.data.length
  if (n === 0) return []
  return props.data.map((d, i) => ({
    x: PADDING.left + (n === 1 ? chartW / 2 : (i / (n - 1)) * chartW),
    y: PADDING.top + chartH - ((d.value - min) / (max - min)) * chartH,
    ...d,
  }))
})

const linePath = computed(() => {
  if (points.value.length === 0) return ''
  return points.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ')
})

const areaPath = computed(() => {
  if (points.value.length === 0) return ''
  const bottom = PADDING.top + chartH
  const first = points.value[0]
  const last = points.value[points.value.length - 1]
  return `${linePath.value} L${last.x},${bottom} L${first.x},${bottom} Z`
})

const yTicks = computed(() => {
  const { min, max } = yRange.value
  const ticks: number[] = []
  const step = (max - min) / 4
  for (let i = 0; i <= 4; i++) {
    ticks.push(min + step * i)
  }
  return ticks
})

function formatValue(v: number): string {
  if (v >= 1_000_000) return (v / 1_000_000).toFixed(1) + 'M'
  if (v >= 1_000) return (v / 1_000).toFixed(1) + 'K'
  return Number.isInteger(v) ? v.toString() : v.toFixed(2)
}

function formatBucket(b: string): string {
  try {
    const d = new Date(b)
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } catch {
    return b
  }
}

function onMouseMove(event: MouseEvent) {
  const svg = (event.currentTarget as SVGSVGElement)
  const rect = svg.getBoundingClientRect()
  const mouseX = ((event.clientX - rect.left) / rect.width) * WIDTH
  const relX = mouseX - PADDING.left
  if (relX < 0 || relX > chartW || props.data.length === 0) {
    hoveredIndex.value = null
    return
  }
  const n = props.data.length
  const idx = Math.round((relX / chartW) * (n - 1))
  hoveredIndex.value = Math.max(0, Math.min(n - 1, idx))
}

function onMouseLeave() {
  hoveredIndex.value = null
}
</script>

<template>
  <div class="bg-white rounded-lg border border-gray-200 p-4">
    <div class="text-sm font-medium text-gray-600 mb-2">{{ label }}</div>

    <div v-if="data.length === 0" class="text-center text-gray-400 py-12 text-sm">
      No data for selected time range
    </div>

    <svg
      v-else
      :viewBox="`0 0 ${WIDTH} ${HEIGHT}`"
      class="w-full"
      style="max-height: 280px"
      @mousemove="onMouseMove"
      @mouseleave="onMouseLeave"
    >
      <!-- Y-axis grid + labels -->
      <template v-for="tick in yTicks" :key="tick">
        <line
          :x1="PADDING.left"
          :y1="PADDING.top + chartH - ((tick - yRange.min) / (yRange.max - yRange.min)) * chartH"
          :x2="PADDING.left + chartW"
          :y2="PADDING.top + chartH - ((tick - yRange.min) / (yRange.max - yRange.min)) * chartH"
          stroke="#e5e7eb"
          stroke-width="1"
        />
        <text
          :x="PADDING.left - 8"
          :y="PADDING.top + chartH - ((tick - yRange.min) / (yRange.max - yRange.min)) * chartH + 4"
          text-anchor="end"
          class="fill-gray-400"
          font-size="11"
        >{{ formatValue(tick) }}</text>
      </template>

      <!-- X-axis labels (show ~6 evenly spaced) -->
      <template v-for="(p, i) in points" :key="'x' + i">
        <text
          v-if="data.length <= 6 || i % Math.ceil(data.length / 6) === 0"
          :x="p.x"
          :y="PADDING.top + chartH + 20"
          text-anchor="middle"
          class="fill-gray-400"
          font-size="10"
        >{{ formatBucket(p.bucket) }}</text>
      </template>

      <!-- Area fill -->
      <path :d="areaPath" fill="rgba(99, 102, 241, 0.1)" />

      <!-- Line -->
      <path :d="linePath" fill="none" stroke="#6366f1" stroke-width="2" stroke-linejoin="round" />

      <!-- Data points -->
      <circle
        v-for="(p, i) in points"
        :key="'dot' + i"
        :cx="p.x"
        :cy="p.y"
        :r="hoveredIndex === i ? 5 : 2.5"
        fill="#6366f1"
        :stroke="hoveredIndex === i ? '#4f46e5' : 'none'"
        :stroke-width="hoveredIndex === i ? 2 : 0"
      />

      <!-- Hover crosshair -->
      <template v-if="hoveredIndex !== null && points[hoveredIndex]">
        <line
          :x1="points[hoveredIndex].x"
          :y1="PADDING.top"
          :x2="points[hoveredIndex].x"
          :y2="PADDING.top + chartH"
          stroke="#6366f1"
          stroke-width="1"
          stroke-dasharray="4 2"
          opacity="0.5"
        />
      </template>
    </svg>

    <!-- Tooltip below chart -->
    <div
      v-if="hoveredIndex !== null && points[hoveredIndex]"
      class="text-center text-sm text-gray-600 mt-1"
    >
      <span class="font-medium text-indigo-600">{{ formatValue(points[hoveredIndex].value) }}</span>
      at {{ formatBucket(data[hoveredIndex].bucket) }}
    </div>
  </div>
</template>
