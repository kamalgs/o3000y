import { describe, it, expect } from 'vitest'
import {
  buildTimeSeriesSQL,
  buildSummarySQL,
  serializeQuery,
  deserializeQuery,
  defaultQuery,
  type ExploreQuery,
} from '../lib/queryBuilder'

describe('buildTimeSeriesSQL', () => {
  it('builds count query with no filters', () => {
    const sql = buildTimeSeriesSQL(defaultQuery())
    expect(sql).toContain('COUNT(*) AS value')
    expect(sql).toContain('time_bucket')
    expect(sql).toContain('GROUP BY bucket')
    expect(sql).toContain("NOW() - INTERVAL '1 hour'")
  })

  it('builds avg query with measure', () => {
    const q: ExploreQuery = { ...defaultQuery(), aggregation: 'avg', measure: 'duration_us' }
    expect(buildTimeSeriesSQL(q)).toContain('AVG(duration_us) AS value')
  })

  it('builds percentile query', () => {
    const q: ExploreQuery = { ...defaultQuery(), aggregation: 'p99', measure: 'duration_us' }
    const sql = buildTimeSeriesSQL(q)
    expect(sql).toContain('PERCENTILE_CONT(0.99)')
    expect(sql).toContain('duration_us')
  })

  it('builds rate query', () => {
    const q: ExploreQuery = { ...defaultQuery(), aggregation: 'rate', interval: '1m' }
    const sql = buildTimeSeriesSQL(q)
    expect(sql).toContain('COUNT(*)')
    expect(sql).toContain('/ 60')
  })

  it('includes filters in WHERE clause', () => {
    const q: ExploreQuery = {
      ...defaultQuery(),
      filters: [
        { column: 'service_name', operator: '=', value: 'api-gateway' },
        { column: 'duration_us', operator: '>', value: '1000' },
      ],
    }
    const sql = buildTimeSeriesSQL(q)
    expect(sql).toContain("service_name = 'api-gateway'")
    expect(sql).toContain('duration_us > 1000')
  })

  it('escapes SQL single quotes in filter values', () => {
    const q: ExploreQuery = {
      ...defaultQuery(),
      filters: [{ column: 'service_name', operator: '=', value: "it's" }],
    }
    expect(buildTimeSeriesSQL(q)).toContain("service_name = 'it''s'")
  })

  it('uses correct interval', () => {
    const q: ExploreQuery = { ...defaultQuery(), interval: '15m' }
    expect(buildTimeSeriesSQL(q)).toContain("'15 minutes'")
  })

  it('builds GROUP BY time series', () => {
    const q: ExploreQuery = { ...defaultQuery(), groupBy: 'service_name' }
    const sql = buildTimeSeriesSQL(q)
    expect(sql).toContain('service_name AS group_key')
    expect(sql).toContain('GROUP BY bucket, group_key')
  })
})

describe('buildSummarySQL', () => {
  it('builds count summary', () => {
    const sql = buildSummarySQL(defaultQuery())
    expect(sql).toContain('COUNT(*) AS value')
    expect(sql).not.toContain('GROUP BY')
  })

  it('builds GROUP BY summary', () => {
    const q: ExploreQuery = { ...defaultQuery(), groupBy: 'service_name' }
    const sql = buildSummarySQL(q)
    expect(sql).toContain('service_name AS group_key')
    expect(sql).toContain('GROUP BY group_key')
    expect(sql).toContain('ORDER BY value DESC')
    expect(sql).toContain('LIMIT 20')
  })

  it('builds count_distinct_traces summary', () => {
    const q: ExploreQuery = { ...defaultQuery(), aggregation: 'count_distinct_traces' }
    expect(buildSummarySQL(q)).toContain('COUNT(DISTINCT trace_id)')
  })
})

describe('serializeQuery / deserializeQuery', () => {
  it('round-trips a query with groupBy', () => {
    const q: ExploreQuery = {
      aggregation: 'p90',
      measure: 'duration_us',
      filters: [{ column: 'service_name', operator: '=', value: 'api' }],
      timeRange: '6h',
      interval: '15m',
      groupBy: 'service_name',
    }
    const restored = deserializeQuery(serializeQuery(q))
    expect(restored.aggregation).toBe('p90')
    expect(restored.groupBy).toBe('service_name')
    expect(restored.filters).toHaveLength(1)
  })

  it('handles empty params gracefully', () => {
    const restored = deserializeQuery({})
    expect(restored.aggregation).toBe('count')
    expect(restored.groupBy).toBe('')
  })
})
