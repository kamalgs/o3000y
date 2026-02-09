# TraceQuery MVP — Implementation Plan

## Objective

Build the simplest end-to-end skeleton that demonstrates:
**Ingest OTel spans → Write Parquet → Query via DuckDB**

All in a single process, 6 source files, runnable with `gradle run`.

---

## What's In the MVP

| Concern              | MVP Scope                                                    |
|----------------------|--------------------------------------------------------------|
| Ingestion protocol   | OTLP/HTTP JSON only (no gRPC, no protobuf)                  |
| Buffering            | In-memory list, flush on count threshold or timer            |
| Parquet writing      | Via DuckDB `COPY TO` (no Arrow/parquet-mr dependency)        |
| Storage backend      | Local filesystem (same partitioned layout as S3/GCS)         |
| Query engine         | DuckDB in-process via JDBC, `read_parquet()` with globs     |
| Query interface      | REST API only (POST SQL + convenience endpoints)             |
| HTTP server          | JDK built-in `com.sun.net.httpserver.HttpServer` (zero deps) |
| Deployment           | Single JAR, docker-compose for one-command demo              |

## What's NOT In the MVP

- gRPC / protobuf (OTLP/HTTP JSON is sufficient to prove the concept)
- S3 / GCS integration (local disk uses the same partitioned layout; swapping in `httpfs` is a config change)
- Authentication, mTLS, multi-tenancy
- WAL / crash recovery
- Compaction
- Web UI

---

## Architecture (MVP)

```
 ┌──────────────┐    curl / OTel SDK    ┌────────────────────────────────────┐
 │  Demo script │ ───────────────────▶  │          TraceQuery (single JVM)   │
 │  or any OTel │   POST /v1/traces     │                                    │
 │  exporter    │                       │  IngestHandler  → SpanBuffer       │
 └──────────────┘                       │                     │ flush         │
                                        │                     ▼              │
                                        │              DuckDB COPY TO        │
                                        │                     │              │
                                        │                     ▼              │
                                        │          data/traces/              │
                                        │            year=.../month=.../     │
                                        │              *.parquet             │
                                        │                     │              │
 ┌──────────────┐   POST /api/v1/query  │                     ▼              │
 │  curl / UI   │ ◀──────────────────▶  │  QueryHandler → DuckDB            │
 │              │                       │                read_parquet()      │
 └──────────────┘                       └────────────────────────────────────┘
```

---

## Source Files

```
src/main/java/dev/tracequery/
├── Main.java            — Wires JDK HttpServer routes, starts the server
├── HttpUtil.java        — Tiny JSON response helper for HttpServer
├── SpanRecord.java      — Record (POJO) for a flattened span
├── SpanBuffer.java      — Buffers spans in memory, flushes to Parquet via DuckDB
├── IngestHandler.java   — Parses OTLP/HTTP JSON, feeds SpanBuffer
└── QueryHandler.java    — Executes SQL via DuckDB over Parquet files
```

## Dependencies (2 external, rest is JDK)

| Dependency                            | Purpose                             |
|---------------------------------------|-------------------------------------|
| `com.fasterxml.jackson:jackson-*`     | JSON parsing and serialization      |
| `org.duckdb:duckdb_jdbc`             | Parquet write + SQL query engine    |
| JDK `com.sun.net.httpserver`          | HTTP server (built-in, zero deps)   |

---

## Implementation Phases

### Phase 1: Project skeleton (this PR)
- [x] Gradle build with local JAR dependencies
- [x] `Main.java` — starts JDK HttpServer, registers routes
- [x] `HttpUtil.java` — JSON response helper
- [x] `SpanRecord.java` — flat span representation
- [x] `IngestHandler.java` — parse OTLP/HTTP JSON into SpanRecords
- [x] `SpanBuffer.java` — accumulate + flush to Parquet via DuckDB
- [x] `QueryHandler.java` — SQL query + convenience endpoints
- [x] Demo script (`demo/send-traces.sh`) to send sample traces
- [x] Dockerfile + docker-compose.yml
- [x] End-to-end smoke test (ingest → flush → query)

### Phase 2: Hardening
- [ ] Input validation & error handling edge cases
- [ ] Query timeout enforcement
- [ ] Result row limit
- [ ] Metrics endpoint (spans ingested, flushes, query latency)
- [ ] Structured JSON logging
- [ ] Unit + integration tests (JUnit 5 + Testcontainers)

### Phase 3: Object storage
- [ ] S3 backend via DuckDB `httpfs` extension
- [ ] GCS backend via DuckDB `gcs` extension
- [ ] Storage backend config (local / s3 / gcs)
- [ ] Credential provider chain setup

### Phase 4: Production features
- [ ] OTLP/gRPC ingestion endpoint
- [ ] Protobuf content type support
- [ ] WAL for crash recovery
- [ ] Backpressure (429 when buffer is full)
- [ ] Parquet file compaction job
- [ ] API-key authentication

### Phase 5: Query UX
- [ ] CLI REPL tool
- [ ] Web UI (trace waterfall view)
- [ ] Pre-built query templates (slow traces, error traces, service map)

---

## How to Demo

```bash
# Terminal 1 — start the server
gradle run

# Terminal 2 — send sample traces
chmod +x demo/send-traces.sh
./demo/send-traces.sh

# Terminal 3 — query
# List services
curl -s http://localhost:7070/api/v1/services | jq

# Find slow spans
curl -s http://localhost:7070/api/v1/query \
  -H 'Content-Type: application/json' \
  -d '{"sql": "SELECT service_name, operation_name, duration_us FROM spans ORDER BY duration_us DESC LIMIT 10"}' | jq

# Get a full trace
curl -s http://localhost:7070/api/v1/trace/<trace-id> | jq

# Find error spans
curl -s http://localhost:7070/api/v1/query \
  -H 'Content-Type: application/json' \
  -d '{"sql": "SELECT service_name, operation_name, status_message FROM spans WHERE status_code = 2"}' | jq
```
