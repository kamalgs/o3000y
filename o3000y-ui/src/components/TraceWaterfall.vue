<script setup lang="ts">
import { ref, computed } from 'vue'
import type { TraceResponse, SpanResponse } from '../api/client'

const props = defineProps<{ trace: TraceResponse }>()

const selectedSpan = ref<SpanResponse | null>(null)

const SERVICE_COLORS = [
  'bg-blue-500',
  'bg-green-500',
  'bg-purple-500',
  'bg-orange-500',
  'bg-pink-500',
  'bg-teal-500',
  'bg-indigo-500',
  'bg-red-500',
]

interface WaterfallSpan extends SpanResponse {
  depth: number
  children: WaterfallSpan[]
}

const serviceColorMap = computed(() => {
  const map = new Map<string, string>()
  const services = [...new Set(props.trace.spans.map((s) => s.serviceName))]
  services.forEach((svc, i) => {
    map.set(svc, SERVICE_COLORS[i % SERVICE_COLORS.length])
  })
  return map
})

const timeRange = computed(() => {
  if (props.trace.spans.length === 0) return { min: 0, max: 1 }
  const times = props.trace.spans.flatMap((s) => {
    const start = parseTime(s.startTime)
    const end = parseTime(s.endTime)
    return [start, end]
  })
  const min = Math.min(...times)
  const max = Math.max(...times)
  return { min, max: max === min ? min + 1 : max }
})

const orderedSpans = computed<WaterfallSpan[]>(() => {
  const spanMap = new Map<string, WaterfallSpan>()
  for (const span of props.trace.spans) {
    spanMap.set(span.spanId, { ...span, depth: 0, children: [] })
  }
  const roots: WaterfallSpan[] = []
  for (const span of spanMap.values()) {
    if (span.parentSpanId && spanMap.has(span.parentSpanId)) {
      spanMap.get(span.parentSpanId)!.children.push(span)
    } else {
      roots.push(span)
    }
  }
  const result: WaterfallSpan[] = []
  function walk(node: WaterfallSpan, depth: number) {
    node.depth = depth
    result.push(node)
    node.children
      .sort((a, b) => parseTime(a.startTime) - parseTime(b.startTime))
      .forEach((child) => walk(child, depth + 1))
  }
  roots
    .sort((a, b) => parseTime(a.startTime) - parseTime(b.startTime))
    .forEach((root) => walk(root, 0))
  return result
})

function parseTime(t: string): number {
  const d = new Date(t)
  if (!isNaN(d.getTime())) return d.getTime() * 1000
  return Number(t) || 0
}

function barStyle(span: SpanResponse) {
  const range = timeRange.value
  const total = range.max - range.min
  const start = parseTime(span.startTime)
  const end = parseTime(span.endTime)
  const left = ((start - range.min) / total) * 100
  const width = Math.max(0.5, ((end - start) / total) * 100)
  return { left: `${left}%`, width: `${width}%` }
}

function formatDuration(us: number): string {
  if (us < 1000) return `${us}\u00B5s`
  if (us < 1_000_000) return `${(us / 1000).toFixed(1)}ms`
  return `${(us / 1_000_000).toFixed(2)}s`
}
</script>

<template>
  <div class="flex gap-4">
    <div class="flex-1 bg-white rounded-lg border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center gap-4">
        <span class="text-sm font-medium">{{ trace.spanCount }} spans</span>
        <div class="flex gap-2 flex-wrap">
          <span
            v-for="[svc, color] in serviceColorMap"
            :key="svc"
            class="inline-flex items-center gap-1 text-xs"
          >
            <span :class="[color, 'w-3 h-3 rounded-sm inline-block']"></span>
            {{ svc }}
          </span>
        </div>
      </div>
      <div class="divide-y divide-gray-100">
        <div
          v-for="span in orderedSpans"
          :key="span.spanId"
          @click="selectedSpan = span"
          class="flex items-center px-4 py-2 hover:bg-gray-50 cursor-pointer"
          :class="{ 'bg-blue-50': selectedSpan?.spanId === span.spanId }"
        >
          <div
            class="w-48 flex-shrink-0 truncate text-xs font-mono"
            :style="{ paddingLeft: span.depth * 16 + 'px' }"
          >
            {{ span.operationName }}
          </div>
          <div class="flex-1 relative h-6">
            <div
              :class="[serviceColorMap.get(span.serviceName) || 'bg-gray-400', 'absolute h-full rounded-sm opacity-80']"
              :style="barStyle(span)"
            >
              <span class="text-white text-xs px-1 leading-6 whitespace-nowrap">
                {{ formatDuration(span.durationUs) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="selectedSpan"
      class="w-80 flex-shrink-0 bg-white rounded-lg border border-gray-200 p-4 self-start"
    >
      <h3 class="font-semibold text-sm mb-3">Span Details</h3>
      <dl class="space-y-2 text-xs">
        <div>
          <dt class="text-gray-500">Operation</dt>
          <dd class="font-mono">{{ selectedSpan.operationName }}</dd>
        </div>
        <div>
          <dt class="text-gray-500">Service</dt>
          <dd class="font-mono">{{ selectedSpan.serviceName }}</dd>
        </div>
        <div>
          <dt class="text-gray-500">Span ID</dt>
          <dd class="font-mono">{{ selectedSpan.spanId }}</dd>
        </div>
        <div>
          <dt class="text-gray-500">Duration</dt>
          <dd>{{ formatDuration(selectedSpan.durationUs) }}</dd>
        </div>
        <div>
          <dt class="text-gray-500">Status</dt>
          <dd>{{ selectedSpan.statusCode === 0 ? 'UNSET' : selectedSpan.statusCode === 1 ? 'OK' : 'ERROR' }}</dd>
        </div>
        <div v-if="selectedSpan.statusMessage">
          <dt class="text-gray-500">Status Message</dt>
          <dd>{{ selectedSpan.statusMessage }}</dd>
        </div>
        <div v-if="Object.keys(selectedSpan.attributes || {}).length > 0">
          <dt class="text-gray-500 mb-1">Attributes</dt>
          <dd>
            <div
              v-for="(val, key) in selectedSpan.attributes"
              :key="key"
              class="font-mono flex gap-1"
            >
              <span class="text-gray-600">{{ key }}:</span>
              <span>{{ val }}</span>
            </div>
          </dd>
        </div>
      </dl>
    </div>
  </div>
</template>
