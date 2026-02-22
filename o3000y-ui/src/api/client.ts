export interface QueryResponse {
  columns: string[]
  rows: unknown[][]
  rowCount: number
  elapsedMs: number
}

export interface SpanResponse {
  traceId: string
  spanId: string
  parentSpanId: string
  operationName: string
  serviceName: string
  startTime: string
  endTime: string
  durationUs: number
  statusCode: number
  statusMessage: string
  spanKind: number
  attributes: Record<string, string>
}

export interface TraceResponse {
  traceId: string
  spanCount: number
  spans: SpanResponse[]
}

export interface SearchParams {
  service?: string
  operation?: string
  minDuration?: number
  maxDuration?: number
  status?: string
  limit?: number
}

const BASE = ''

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(BASE + url, init)
  if (!response.ok) {
    const body = await response.text()
    throw new Error(`HTTP ${response.status}: ${body}`)
  }
  return response.json()
}

export async function executeQuery(sql: string): Promise<QueryResponse> {
  return fetchJson('/api/v1/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sql }),
  })
}

export async function getTrace(traceId: string): Promise<TraceResponse> {
  return fetchJson(`/api/v1/trace/${encodeURIComponent(traceId)}`)
}

export async function getServices(): Promise<string[]> {
  return fetchJson('/api/v1/services')
}

export async function getOperations(service?: string): Promise<string[]> {
  const params = service ? `?service=${encodeURIComponent(service)}` : ''
  return fetchJson(`/api/v1/operations${params}`)
}

export async function searchSpans(params: SearchParams): Promise<QueryResponse> {
  const query = new URLSearchParams()
  if (params.service) query.set('service', params.service)
  if (params.operation) query.set('operation', params.operation)
  if (params.minDuration != null) query.set('minDuration', String(params.minDuration))
  if (params.maxDuration != null) query.set('maxDuration', String(params.maxDuration))
  if (params.status) query.set('status', params.status)
  if (params.limit != null) query.set('limit', String(params.limit))
  return fetchJson(`/api/v1/search?${query.toString()}`)
}

export async function getHealth(): Promise<{ status: string }> {
  return fetchJson('/health')
}
