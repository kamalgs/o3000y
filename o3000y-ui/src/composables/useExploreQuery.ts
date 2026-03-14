import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { executeQuery, type QueryResponse } from '@/api/client'
import {
  type ExploreQuery,
  buildTimeSeriesSQL,
  buildSummarySQL,
  serializeQuery,
  deserializeQuery,
  defaultQuery,
  aggLabel,
  CHART_COLORS,
} from '@/lib/queryBuilder'
import { formatNumber } from '@/lib/formatting'
import type { Series } from '@/components/TimeChart.vue'

export function useExploreQuery() {
  const router = useRouter()
  const route = useRoute()

  const query = ref<ExploreQuery>(defaultQuery())
  const loading = ref(false)
  const error = ref('')
  const summaryValue = ref<string | null>(null)
  const chartSeries = ref<Series[]>([])
  const breakdownGroups = ref<{ key: string; value: number }[]>([])
  const generatedSQL = ref('')

  const label = computed(() => aggLabel(query.value))

  function initFromURL() {
    const params = route.query as Record<string, string>
    if (params.agg) {
      query.value = deserializeQuery(params)
    }
  }

  function updateURL() {
    router.replace({ path: '/explore', query: serializeQuery(query.value) })
  }

  async function run() {
    loading.value = true
    error.value = ''
    summaryValue.value = null
    chartSeries.value = []
    breakdownGroups.value = []
    updateURL()

    const tsSQL = buildTimeSeriesSQL(query.value)
    const sumSQL = buildSummarySQL(query.value)
    generatedSQL.value = tsSQL

    try {
      const [tsResult, sumResult] = await Promise.all([
        executeQuery(tsSQL),
        executeQuery(sumSQL),
      ])

      if (query.value.groupBy) {
        chartSeries.value = parseGroupedTimeSeries(tsResult)
        breakdownGroups.value = parseSummaryGroups(sumResult)
        summaryValue.value = breakdownGroups.value.length + ' groups'
      } else {
        chartSeries.value = [
          {
            name: label.value,
            data: parseSingleTimeSeries(tsResult),
            color: CHART_COLORS[0],
          },
        ]
        if (sumResult.rows.length > 0 && sumResult.rows[0].length > 0) {
          summaryValue.value = formatNumber(sumResult.rows[0][0])
        }
      }
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : String(e)
    } finally {
      loading.value = false
    }
  }

  function drillDown(groupKey: string) {
    query.value = {
      ...query.value,
      filters: [
        ...query.value.filters,
        { column: query.value.groupBy, operator: '=', value: groupKey },
      ],
      groupBy: '',
    }
    run()
  }

  return {
    query,
    loading,
    error,
    summaryValue,
    chartSeries,
    breakdownGroups,
    generatedSQL,
    label,
    initFromURL,
    run,
    drillDown,
  }
}

function parseSingleTimeSeries(r: QueryResponse): { bucket: string; value: number }[] {
  const bi = r.columns.indexOf('bucket')
  const vi = r.columns.indexOf('value')
  if (bi < 0 || vi < 0) return []
  return r.rows.map((row) => ({ bucket: String(row[bi]), value: Number(row[vi]) || 0 }))
}

function parseGroupedTimeSeries(r: QueryResponse): Series[] {
  const bi = r.columns.indexOf('bucket')
  const gi = r.columns.indexOf('group_key')
  const vi = r.columns.indexOf('value')
  if (bi < 0 || gi < 0 || vi < 0) return []

  const groups = new Map<string, { bucket: string; value: number }[]>()
  for (const row of r.rows) {
    const key = String(row[gi] ?? '')
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push({ bucket: String(row[bi]), value: Number(row[vi]) || 0 })
  }

  return [...groups.entries()]
    .slice(0, CHART_COLORS.length)
    .map(([name, data], i) => ({ name, data, color: CHART_COLORS[i] }))
}

function parseSummaryGroups(r: QueryResponse): { key: string; value: number }[] {
  const gi = r.columns.indexOf('group_key')
  const vi = r.columns.indexOf('value')
  if (gi < 0 || vi < 0) return []
  return r.rows.map((row) => ({ key: String(row[gi] ?? ''), value: Number(row[vi]) || 0 }))
}
