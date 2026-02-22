# Configuration

o3000y is configured via environment variables with sensible defaults.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `O3000Y_DATA_PATH` | `data` | Directory for Parquet storage |
| `O3000Y_GRPC_PORT` | `4317` | gRPC listen port |
| `O3000Y_REST_PORT` | `8080` | REST API + UI port |
| `O3000Y_BATCH_MAX_SPANS` | `10000` | Flush buffer at this span count |
| `O3000Y_BATCH_MAX_BYTES` | `16777216` | Flush buffer at this byte size (16 MB) |
| `O3000Y_BATCH_FLUSH_INTERVAL_SEC` | `30` | Timer-based flush interval |
| `O3000Y_MAX_RESULT_ROWS` | `10000` | Maximum rows per query result |
| `O3000Y_QUERY_TIMEOUT_SEC` | `60` | Query timeout in seconds |
| `O3000Y_REFRESH_INTERVAL_SEC` | `30` | DuckDB view refresh interval |

## Docker Compose Override

In `docker/docker-compose.yml`, environment variables are set for demo purposes:

```yaml
environment:
  O3000Y_BATCH_MAX_SPANS: "100"
  O3000Y_BATCH_FLUSH_INTERVAL_SEC: "5"
```

## Logging

Logging is configured via `logback.xml` in `o3000y-app/src/main/resources/`. The default configuration uses console output with a human-readable pattern. A JSON appender is also defined for production use.
