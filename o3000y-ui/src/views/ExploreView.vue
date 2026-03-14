<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import QueryBuilder from '@/components/QueryBuilder.vue'
import TimeChart from '@/components/TimeChart.vue'
import {
  type ExploreQuery,
  buildTimeSeriesSQL,
  buildSummarySQL,
  serializeQuery,
  deserializeQuery,
  defaultQuery,
  AGGREGATIONS,
} from '@/lib/queryBuilder'
import { executeQuery, type QueryResponse } from '@/api/client'

const router = useRouter()
const route = useRoute()

const query = ref<ExploreQuery>(defaultQuery())
const loading = ref(false)
const error = ref('')
const summaryValue = ref<string | null>(null)
const chartData = ref<{ bucket: string; value: number }[]>([])
const generatedSQL = ref('')
const showSQL = ref(false)

const chartLabel = ref('')

onMounted(() => {
  const params = route.query as Record<string, string>
  if (params.agg) {
    query.value = deserializeQuery(params)
    run()
  }
})

function updateURL() {
  const params = serializeQuery(query.value)
  router.replace({ path: '/explore', query: params })
}

async function run() {
  loading.value = true
  error.value = ''
  summaryValue.value = null
  chartData.value = []

  updateURL()

  const timeSeriesSQL = buildTimeSeriesSQL(query.value)
  const summarySQL = buildSummarySQL(query.value)
  generatedSQL.value = timeSeriesSQL

  const aggInfo = AGGREGATIONS.find((a) => a.value === query.value.aggregation)
  chartLabel.value = aggInfo
    ? `${aggInfo.label}${aggInfo.needsMeasure ? `(${query.value.measure})` : ''}`
    : query.value.aggregation

  try {
    const [tsResult, sumResult] = await Promise.all([
      executeQuery(timeSeriesSQL),
      executeQuery(summarySQL),
    ])

    chartData.value = parseTimeSeries(tsResult)

    if (sumResult.rows.length > 0 && sumResult.rows[0].length > 0) {
      const val = sumResult.rows[0][0]
      summaryValue.value = typeof val === 'number' ? formatNumber(val) : String(val ?? '0')
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

function parseTimeSeries(result: QueryResponse): { bucket: string; value: number }[] {
  const bucketIdx = result.columns.indexOf('bucket')
  const valueIdx = result.columns.indexOf('value')
  if (bucketIdx === -1 || valueIdx === -1) return []
  return result.rows.map((row) => ({
    bucket: String(row[bucketIdx]),
    value: Number(row[valueIdx]) || 0,
  }))
}

function formatNumber(v: number): string {
  if (Number.isInteger(v)) return v.toLocaleString()
  return v.toLocaleString(undefined, { maximumFractionDigits: 2 })
}

function copyLink() {
  navigator.clipboard.writeText(window.location.href)
}

watch(
  () => route.query,
  (newQuery) => {
    const params = newQuery as Record<string, string>
    if (params.agg && !loading.value) {
      query.value = deserializeQuery(params)
    }
  },
)
</script>

<template>
  <div class="space-y-4">
    <QueryBuilder v-model="query" :loading="loading" @run="run" />

    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
      {{ error }}
    </div>

    <!-- Summary card -->
    <div
      v-if="summaryValue !== null"
      class="bg-white rounded-lg border border-gray-200 p-4 flex items-center justify-between"
    >
      <div>
        <div class="text-sm text-gray-500">{{ chartLabel }} over entire range</div>
        <div class="text-3xl font-semibold text-gray-900 mt-1">{{ summaryValue }}</div>
      </div>
      <div class="flex gap-2">
        <button
          class="text-xs text-gray-500 hover:text-gray-700 border border-gray-300 rounded px-2 py-1"
          @click="copyLink"
          title="Copy shareable link"
        >
          Copy link
        </button>
        <button
          class="text-xs text-gray-500 hover:text-gray-700 border border-gray-300 rounded px-2 py-1"
          @click="showSQL = !showSQL"
        >
          {{ showSQL ? 'Hide SQL' : 'Show SQL' }}
        </button>
      </div>
    </div>

    <!-- Generated SQL -->
    <div v-if="showSQL && generatedSQL" class="bg-gray-900 rounded-lg p-3 text-sm">
      <pre class="text-green-400 whitespace-pre-wrap font-mono text-xs">{{ generatedSQL }}</pre>
    </div>

    <!-- Chart -->
    <TimeChart v-if="chartData.length > 0" :data="chartData" :label="chartLabel" />

    <!-- Table -->
    <div
      v-if="chartData.length > 0"
      class="bg-white rounded-lg border border-gray-200 overflow-hidden"
    >
      <table class="min-w-full divide-y divide-gray-200 text-sm">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-2 text-left font-medium text-gray-500">Time</th>
            <th class="px-4 py-2 text-right font-medium text-gray-500">{{ chartLabel }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="(row, i) in chartData" :key="i" class="hover:bg-gray-50">
            <td class="px-4 py-1.5 text-gray-600 font-mono text-xs">{{ row.bucket }}</td>
            <td class="px-4 py-1.5 text-right text-gray-900 font-medium">
              {{ formatNumber(row.value) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
