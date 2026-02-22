# API Reference

Base URL: `http://localhost:8080`

## Health

### `GET /health`
Returns server health status.

**Response:** `{"status": "up"}`

## Query

### `POST /api/v1/query`
Execute an arbitrary SQL query against the spans table.

**Request Body:**
```json
{"sql": "SELECT * FROM spans WHERE service_name = 'frontend' LIMIT 10"}
```

**Response:**
```json
{
  "columns": ["trace_id", "span_id", ...],
  "rows": [["abc...", "def...", ...], ...],
  "rowCount": 10,
  "elapsedMs": 42
}
```

**Error Responses:**
- `400` — Empty or invalid SQL
- `408` — Query timeout
- `500` — Internal error

## Traces

### `GET /api/v1/trace/:trace_id`
Get all spans for a trace, ordered by start time.

**Response:**
```json
{
  "traceId": "abc123...",
  "spanCount": 5,
  "spans": [
    {
      "traceId": "abc123...",
      "spanId": "def456...",
      "parentSpanId": "",
      "operationName": "GET /api/users",
      "serviceName": "frontend",
      "startTime": "2026-02-09T14:30:00Z",
      "endTime": "2026-02-09T14:30:01Z",
      "durationUs": 1000000,
      "statusCode": 1,
      "statusMessage": "",
      "spanKind": 2,
      "attributes": {"http.method": "GET"}
    }
  ]
}
```

## Services

### `GET /api/v1/services`
List all distinct service names.

**Response:** `["frontend", "backend", "database"]`

### `GET /api/v1/operations?service=<name>`
List operations, optionally filtered by service.

**Response:** `["GET /api/users", "POST /api/login"]`

## Search

### `GET /api/v1/search`
Search spans with filters.

**Query Parameters:**

| Param | Description |
|-------|-------------|
| `service` | Filter by service name |
| `operation` | Filter by operation name |
| `minDuration` | Minimum duration in microseconds |
| `maxDuration` | Maximum duration in microseconds |
| `status` | "OK" or "ERROR" |
| `limit` | Max results (default 100, max 10000) |

**Response:** Same format as `POST /api/v1/query`
