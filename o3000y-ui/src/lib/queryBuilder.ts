export interface Filter {
  column: string
  operator: '=' | '!=' | '>' | '<' | '>=' | '<=' | 'LIKE' | 'NOT LIKE'
  value: string
}

export interface ExploreQuery {
  aggregation: AggregationType
  measure: string
  filters: Filter[]
  timeRange: string
  interval: string
  groupBy: string
}

export type AggregationType =
  | 'count'
  | 'count_distinct_traces'
  | 'avg'
  | 'sum'
  | 'p50'
  | 'p90'
  | 'p99'
  | 'max'
  | 'min'
  | 'rate'

export const AGGREGATIONS: { value: AggregationType; label: string; needsMeasure: boolean }[] = [
  { value: 'count', label: 'COUNT', needsMeasure: false },
  { value: 'count_distinct_traces', label: 'COUNT(distinct traces)', needsMeasure: false },
  { value: 'rate', label: 'RATE (/s)', needsMeasure: false },
  { value: 'avg', label: 'AVG', needsMeasure: true },
  { value: 'sum', label: 'SUM', needsMeasure: true },
  { value: 'p50', label: 'P50', needsMeasure: true },
  { value: 'p90', label: 'P90', needsMeasure: true },
  { value: 'p99', label: 'P99', needsMeasure: true },
  { value: 'max', label: 'MAX', needsMeasure: true },
  { value: 'min', label: 'MIN', needsMeasure: true },
]

export const MEASURES = ['duration_us', 'status_code', 'span_kind']

export const FILTER_COLUMNS = [
  'service_name',
  'operation_name',
  'trace_id',
  'span_id',
  'parent_span_id',
  'status_code',
  'status_message',
  'span_kind',
  'duration_us',
  'attributes',
]

export const GROUP_BY_COLUMNS = [
  'service_name',
  'operation_name',
  'status_code',
  'span_kind',
]

export const OPERATORS: { value: Filter['operator']; label: string }[] = [
  { value: '=', label: '=' },
  { value: '!=', label: '!=' },
  { value: '>', label: '>' },
  { value: '<', label: '<' },
  { value: '>=', label: '>=' },
  { value: '<=', label: '<=' },
  { value: 'LIKE', label: 'LIKE' },
  { value: 'NOT LIKE', label: 'NOT LIKE' },
]

export const TIME_RANGES: { value: string; label: string }[] = [
  { value: '5m', label: 'Last 5 min' },
  { value: '15m', label: 'Last 15 min' },
  { value: '1h', label: 'Last 1 hour' },
  { value: '6h', label: 'Last 6 hours' },
  { value: '24h', label: 'Last 24 hours' },
  { value: '7d', label: 'Last 7 days' },
  { value: '30d', label: 'Last 30 days' },
]

export const INTERVALS: { value: string; label: string }[] = [
  { value: '10s', label: '10s' },
  { value: '30s', label: '30s' },
  { value: '1m', label: '1 min' },
  { value: '5m', label: '5 min' },
  { value: '15m', label: '15 min' },
  { value: '1h', label: '1 hour' },
  { value: '1d', label: '1 day' },
]

export const CHART_COLORS = [
  'var(--color-chart-1)',
  'var(--color-chart-2)',
  'var(--color-chart-3)',
  'var(--color-chart-4)',
  'var(--color-chart-5)',
  'var(--color-chart-6)',
  'var(--color-chart-7)',
  'var(--color-chart-8)',
]

// ── internals ──

function escapeSQL(value: string): string {
  return value.replace(/'/g, "''")
}

function aggExpression(agg: AggregationType, measure: string): string {
  switch (agg) {
    case 'count':
      return 'COUNT(*)'
    case 'count_distinct_traces':
      return 'COUNT(DISTINCT trace_id)'
    case 'rate':
      return 'COUNT(*)'
    case 'avg':
      return `AVG(${measure})`
    case 'sum':
      return `SUM(${measure})`
    case 'p50':
      return `PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ${measure})`
    case 'p90':
      return `PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY ${measure})`
    case 'p99':
      return `PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY ${measure})`
    case 'max':
      return `MAX(${measure})`
    case 'min':
      return `MIN(${measure})`
  }
}

function toIntervalSQL(interval: string): string {
  const match = interval.match(/^(\d+)([smhd])$/)
  if (!match) return "'1 minute'"
  const [, n, unit] = match
  const unitMap: Record<string, string> = { s: 'second', m: 'minute', h: 'hour', d: 'day' }
  const u = unitMap[unit] || 'minute'
  return `'${n} ${u}${Number(n) > 1 ? 's' : ''}'`
}

function toRangeSQL(range: string): string {
  const match = range.match(/^(\d+)([mhd])$/)
  if (!match) return "'1 hour'"
  const [, n, unit] = match
  const unitMap: Record<string, string> = { m: 'minute', h: 'hour', d: 'day' }
  const u = unitMap[unit] || 'hour'
  return `'${n} ${u}${Number(n) > 1 ? 's' : ''}'`
}

function intervalToSeconds(interval: string): number {
  const match = interval.match(/^(\d+)([smhd])$/)
  if (!match) return 60
  const [, n, unit] = match
  const m: Record<string, number> = { s: 1, m: 60, h: 3600, d: 86400 }
  return Number(n) * (m[unit] || 60)
}

function rangeToSeconds(range: string): number {
  const match = range.match(/^(\d+)([mhd])$/)
  if (!match) return 3600
  const [, n, unit] = match
  const m: Record<string, number> = { m: 60, h: 3600, d: 86400 }
  return Number(n) * (m[unit] || 3600)
}

function buildWhere(filters: Filter[], timeRange: string): string {
  const clauses: string[] = []
  clauses.push(`start_time >= NOW() - INTERVAL ${toRangeSQL(timeRange)}`)
  for (const f of filters) {
    if (!f.column || !f.value) continue
    const isNumeric = ['duration_us', 'status_code', 'span_kind'].includes(f.column)
    if (isNumeric && ['=', '!=', '>', '<', '>=', '<='].includes(f.operator)) {
      clauses.push(`${f.column} ${f.operator} ${escapeSQL(f.value)}`)
    } else {
      clauses.push(`${f.column} ${f.operator} '${escapeSQL(f.value)}'`)
    }
  }
  return clauses.join('\n  AND ')
}

function rateExpr(divisor: number): string {
  return `ROUND(COUNT(*)::DOUBLE / ${divisor}, 2) AS value`
}

// ── public API ──

export function buildTimeSeriesSQL(query: ExploreQuery): string {
  const intervalSQL = toIntervalSQL(query.interval)
  const where = buildWhere(query.filters, query.timeRange)
  const isRate = query.aggregation === 'rate'
  const valueExpr = isRate
    ? rateExpr(intervalToSeconds(query.interval))
    : `${aggExpression(query.aggregation, query.measure)} AS value`

  if (query.groupBy) {
    return `SELECT
  time_bucket(INTERVAL ${intervalSQL}, start_time) AS bucket,
  ${query.groupBy} AS group_key,
  ${valueExpr}
FROM spans
WHERE ${where}
GROUP BY bucket, group_key
ORDER BY bucket, group_key`
  }

  return `SELECT
  time_bucket(INTERVAL ${intervalSQL}, start_time) AS bucket,
  ${valueExpr}
FROM spans
WHERE ${where}
GROUP BY bucket
ORDER BY bucket`
}

export function buildSummarySQL(query: ExploreQuery): string {
  const where = buildWhere(query.filters, query.timeRange)
  const isRate = query.aggregation === 'rate'

  if (query.groupBy) {
    const valueExpr = isRate
      ? rateExpr(rangeToSeconds(query.timeRange))
      : `${aggExpression(query.aggregation, query.measure)} AS value`
    return `SELECT
  ${query.groupBy} AS group_key,
  ${valueExpr}
FROM spans
WHERE ${where}
GROUP BY group_key
ORDER BY value DESC
LIMIT 20`
  }

  if (isRate) {
    return `SELECT ${rateExpr(rangeToSeconds(query.timeRange))}\nFROM spans\nWHERE ${where}`
  }
  return `SELECT ${aggExpression(query.aggregation, query.measure)} AS value\nFROM spans\nWHERE ${where}`
}

// ── URL serialization ──

export function serializeQuery(query: ExploreQuery): Record<string, string> {
  const params: Record<string, string> = {
    agg: query.aggregation,
    range: query.timeRange,
    interval: query.interval,
  }
  if (query.measure) params.measure = query.measure
  if (query.groupBy) params.groupBy = query.groupBy
  if (query.filters.length > 0) {
    params.filters = JSON.stringify(query.filters.filter((f) => f.column && f.value))
  }
  return params
}

export function deserializeQuery(params: Record<string, string>): ExploreQuery {
  let filters: Filter[] = []
  if (params.filters) {
    try {
      filters = JSON.parse(params.filters)
    } catch {
      filters = []
    }
  }
  return {
    aggregation: (params.agg as AggregationType) || 'count',
    measure: params.measure || 'duration_us',
    filters,
    timeRange: params.range || '1h',
    interval: params.interval || '5m',
    groupBy: params.groupBy || '',
  }
}

export function defaultQuery(): ExploreQuery {
  return {
    aggregation: 'count',
    measure: 'duration_us',
    filters: [],
    timeRange: '1h',
    interval: '5m',
    groupBy: '',
  }
}

export function aggLabel(query: ExploreQuery): string {
  const agg = AGGREGATIONS.find((a) => a.value === query.aggregation)
  if (!agg) return query.aggregation
  return agg.needsMeasure ? `${agg.label}(${query.measure})` : agg.label
}
