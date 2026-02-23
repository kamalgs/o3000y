# Query Engine

## Overview

The query engine uses DuckDB (in-memory) to provide SQL access to Parquet trace data. DuckDB reads Parquet files directly using `read_parquet()` with Hive partition support.

## View Management

A SQL view `spans` is created and periodically refreshed:

```sql
CREATE OR REPLACE VIEW spans AS
SELECT * FROM read_parquet(
  '<data_path>/**/*.parquet',
  hive_partitioning=true,
  union_by_name=true
)
```

The view is refreshed:
- On startup
- Periodically (default: every 30 seconds)

## Query Features

- **SQL Query**: Arbitrary SQL via `POST /api/v1/query`
- **Trace Lookup**: `GET /api/v1/trace/:trace_id` returns structured `TraceResponse`
- **Search**: `GET /api/v1/search` with service, operation, duration, status filters
- **Service List**: `GET /api/v1/services` returns distinct service names
- **Operations**: `GET /api/v1/operations?service=` returns operations for a service

## Safety

- Query timeout via JDBC `setQueryTimeout` (default: 60s)
- Result size limited by `maxResultRows` (default: 10,000)
- Trace ID input sanitized to prevent SQL injection
- Invalid SQL returns 400; timeout returns 408

## Configuration

| Env Variable | Default | Description |
|-------------|---------|-------------|
| O3000Y_DATA_PATH | data | Parquet data directory |
| O3000Y_MAX_RESULT_ROWS | 10000 | Max rows per query result |
| O3000Y_QUERY_TIMEOUT_SEC | 60 | Query timeout |
| O3000Y_REFRESH_INTERVAL_SEC | 30 | View refresh interval |
