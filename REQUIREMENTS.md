# TraceQuery — Distributed Trace Query Tool

## 1. Overview

TraceQuery is a tool for ingesting, storing, and querying OpenTelemetry (OTel) distributed traces. It receives spans via the OTel protocol, persists them as Parquet files on object storage (GCS / S3), and exposes a SQL-based query interface by mapping those files into DuckDB.

### 1.1 Goals

- **Low-cost storage** — Parquet on object storage keeps per-GB costs orders of magnitude below a dedicated trace database.
- **Familiar query interface** — Analysts and engineers query traces with SQL through DuckDB; no proprietary query language to learn.
- **Minimal infrastructure** — No long-running cluster. The ingestion path is a lightweight service; the query path is on-demand DuckDB.
- **OTel-native** — Accepts standard OTLP (gRPC and HTTP) so any OTel-instrumented service can export to it without custom instrumentation.

### 1.2 Non-Goals (v1)

- Real-time streaming analytics / alerting on traces.
- Full-text search across span attributes (basic filtering only).
- Multi-tenant isolation (single-tenant deployment assumed).
- A hosted SaaS offering.

---

## 2. Architecture

```
┌──────────────┐       OTLP/gRPC        ┌──────────────────┐
│  Instrumented│ ────────────────────▶   │                  │
│  Services    │       OTLP/HTTP         │  Ingestion       │
│              │ ────────────────────▶   │  Service         │
└──────────────┘                         │                  │
                                         └──────┬───────────┘
                                                │
                                         Batches spans, converts
                                         to Parquet, writes to
                                         object storage
                                                │
                                                ▼
                                     ┌─────────────────────┐
                                     │  Object Storage      │
                                     │  (GCS / S3)          │
                                     │                      │
                                     │  /traces/            │
                                     │    YYYY/MM/DD/HH/    │
                                     │      <batch>.parquet │
                                     └──────────┬──────────┘
                                                │
                                         Maps Parquet files
                                         as external tables
                                                │
                                                ▼
                                     ┌─────────────────────┐
                                     │  Query Service       │
                                     │  (DuckDB engine)     │
                                     │                      │
                                     │  REST / gRPC API     │
                                     │  + optional CLI      │
                                     └─────────────────────┘
```

---

## 3. Components

### 3.1 Ingestion Service

**Responsibility:** Accept OTel spans and write them to object storage as Parquet files.

#### 3.1.1 Protocol Support

| Protocol   | Endpoint                        | Content Types              |
|------------|---------------------------------|----------------------------|
| OTLP/gRPC  | `0.0.0.0:4317`                 | protobuf                   |
| OTLP/HTTP  | `0.0.0.0:4318/v1/traces`       | protobuf, JSON             |

#### 3.1.2 Batching & Flush Strategy

Spans are accumulated in an in-memory buffer and flushed to Parquet when **any** of the following thresholds is met:

| Parameter              | Default   | Configurable |
|------------------------|-----------|--------------|
| `batch.max_spans`      | 10,000    | Yes          |
| `batch.max_bytes`      | 16 MB     | Yes          |
| `batch.flush_interval` | 30 s      | Yes          |

On flush:
1. Convert the span batch to a Parquet file (see schema in §4).
2. Upload to object storage under the partitioning path (see §3.3).
3. Acknowledge the batch (return success to the exporter).

#### 3.1.3 Backpressure

- When the in-memory buffer reaches `2 × batch.max_bytes`, the service returns `RESOURCE_EXHAUSTED` (gRPC) / `429` (HTTP) to exporters so they back off.

#### 3.1.4 Reliability

- Spans are buffered in memory only; a crash loses the current unflushed batch. This is acceptable for v1 — most OTel SDKs retry on failure.
- Optional: WAL (write-ahead log) to local disk for crash recovery (v2).

---

### 3.2 Object Storage Layout

Parquet files are stored under a configurable base prefix with **hourly time partitioning** derived from span start time:

```
s3://<bucket>/<prefix>/traces/year=2026/month=02/day=09/hour=14/<batch-id>.parquet
```

Partitioning by time enables:
- Efficient time-range pruning during queries.
- Simple retention via lifecycle policies on the bucket.

#### File Naming

`<batch-id>` is `<ingestion-instance-id>_<flush-timestamp-ms>_<sequence>` to avoid collisions across multiple ingestion instances.

---

### 3.3 Query Service

**Responsibility:** Provide a SQL query interface over the stored Parquet files using DuckDB.

#### 3.3.1 DuckDB Integration

- On startup (or on-demand), the query service discovers Parquet files under the storage prefix and registers them as a DuckDB view using `read_parquet()` with glob / Hive partitioning.
- DuckDB's `httpfs` / `gcs` / `s3` extensions handle direct reads from object storage — no local copy required.
- The Parquet partition columns (`year`, `month`, `day`, `hour`) are exposed as columns so `WHERE` clauses on time ranges translate into partition pruning.

#### 3.3.2 Query API

| Interface    | Description                                              |
|--------------|----------------------------------------------------------|
| REST API     | `POST /api/v1/query` — accepts SQL, returns JSON rows    |
| gRPC API     | `QueryService.ExecuteQuery` — accepts SQL, streams rows  |
| CLI          | Interactive REPL that connects to the REST/gRPC endpoint |

**Request format (REST):**
```json
{
  "sql": "SELECT trace_id, span_id, operation_name, duration_ms FROM spans WHERE service_name = 'checkout' AND start_time > '2026-02-09T00:00:00Z' ORDER BY duration_ms DESC LIMIT 50"
}
```

**Response format (REST):**
```json
{
  "columns": ["trace_id", "span_id", "operation_name", "duration_ms"],
  "rows": [
    ["abc123...", "def456...", "POST /pay", 342],
    ...
  ],
  "row_count": 50,
  "elapsed_ms": 120
}
```

#### 3.3.3 Pre-built Query Templates

The query service ships convenience endpoints / functions for common trace queries:

| Endpoint / Function              | Description                                         |
|----------------------------------|-----------------------------------------------------|
| `GET /api/v1/trace/:trace_id`    | Retrieve all spans for a trace, ordered for display |
| `GET /api/v1/services`           | List distinct service names                         |
| `GET /api/v1/operations`         | List distinct operations, optionally by service     |
| `GET /api/v1/search`             | Search spans by service, operation, tags, duration  |

---

## 4. Parquet Schema

Each row represents a single span.

| Column                | Type                  | Description                                      |
|-----------------------|-----------------------|--------------------------------------------------|
| `trace_id`            | `BINARY(16)`          | 128-bit trace identifier                         |
| `span_id`             | `BINARY(8)`           | 64-bit span identifier                           |
| `parent_span_id`      | `BINARY(8)`           | 64-bit parent span id (empty for root spans)     |
| `trace_state`         | `VARCHAR`             | W3C tracestate header value                      |
| `operation_name`      | `VARCHAR`             | Span name / operation                            |
| `span_kind`           | `INT32`               | OTel SpanKind enum (0–4)                         |
| `start_time`          | `TIMESTAMP_MICROS`    | Span start (microsecond precision)               |
| `end_time`            | `TIMESTAMP_MICROS`    | Span end                                         |
| `duration_us`         | `INT64`               | Duration in microseconds (derived)               |
| `status_code`         | `INT32`               | OTel StatusCode (0=Unset, 1=Ok, 2=Error)        |
| `status_message`      | `VARCHAR`             | Status description (typically set on error)       |
| `service_name`        | `VARCHAR`             | `service.name` resource attribute                |
| `service_namespace`   | `VARCHAR`             | `service.namespace` resource attribute           |
| `service_version`     | `VARCHAR`             | `service.version` resource attribute             |
| `resource_attributes` | `MAP(VARCHAR,VARCHAR)` | All resource attributes as key-value map         |
| `span_attributes`     | `MAP(VARCHAR,VARCHAR)` | All span attributes as key-value map             |
| `events`              | `LIST(STRUCT(...))`   | Span events (name, timestamp, attributes)        |
| `links`               | `LIST(STRUCT(...))`   | Span links (trace_id, span_id, attributes)       |

### 4.1 Nested Struct Definitions

**Event struct:**
```
STRUCT(
  name          VARCHAR,
  timestamp     TIMESTAMP_MICROS,
  attributes    MAP(VARCHAR, VARCHAR)
)
```

**Link struct:**
```
STRUCT(
  trace_id      BINARY(16),
  span_id       BINARY(8),
  trace_state   VARCHAR,
  attributes    MAP(VARCHAR, VARCHAR)
)
```

### 4.2 Parquet File Settings

| Setting              | Value           | Rationale                                    |
|----------------------|-----------------|----------------------------------------------|
| Compression          | ZSTD            | Best ratio for structured trace data         |
| Row group size       | 128 MB          | Good balance for DuckDB scan performance     |
| Page size            | 1 MB            | Enables efficient predicate pushdown         |
| Dictionary encoding  | Enabled         | High cardinality columns benefit (operations, service names) |
| Sorting              | `service_name, start_time` | Improves compression and query locality |

---

## 5. Configuration

All configuration is via environment variables or a YAML config file.

```yaml
storage:
  backend: "s3"            # "s3" | "gcs"
  bucket: "my-trace-bucket"
  prefix: "traces"
  region: "us-east-1"      # S3 only

ingestion:
  grpc_port: 4317
  http_port: 4318
  batch:
    max_spans: 10000
    max_bytes_mb: 16
    flush_interval_seconds: 30

query:
  port: 8080
  grpc_port: 8081
  max_result_rows: 10000
  query_timeout_seconds: 60
  duckdb:
    memory_limit: "4GB"
    threads: 4
```

---

## 6. Technology Stack

| Component            | Technology                                   |
|----------------------|----------------------------------------------|
| Language             | Java 21                                      |
| Build tool           | Gradle (Kotlin DSL)                          |
| OTel protocol        | opentelemetry-proto (gRPC + HTTP)            |
| Parquet writing      | Apache Parquet (parquet-mr) via Apache Arrow  |
| Object storage SDK   | AWS SDK v2 (S3), Google Cloud Storage client  |
| Query engine         | DuckDB (via duckdb-java JDBC driver)         |
| HTTP framework       | Javalin or Helidon SE (lightweight)          |
| gRPC framework       | grpc-java                                    |
| Serialization        | Protocol Buffers                             |
| Testing              | JUnit 5, Testcontainers (MinIO for S3)       |
| Containerization     | Docker, docker-compose for local dev         |

---

## 7. Functional Requirements

### FR-1: Span Ingestion
- **FR-1.1** Accept spans via OTLP/gRPC on a configurable port.
- **FR-1.2** Accept spans via OTLP/HTTP on a configurable port.
- **FR-1.3** Validate incoming spans (reject malformed payloads with appropriate error codes).
- **FR-1.4** Buffer spans in memory and flush to Parquet on threshold triggers.
- **FR-1.5** Support concurrent exporters without data loss under normal operation.

### FR-2: Storage
- **FR-2.1** Write Parquet files to S3-compatible or GCS object storage.
- **FR-2.2** Partition files by hourly time buckets based on span start time.
- **FR-2.3** Use ZSTD compression and dictionary encoding.
- **FR-2.4** Generate collision-free file names across multiple ingestion instances.

### FR-3: Querying
- **FR-3.1** Accept arbitrary SQL queries over the spans table via REST and gRPC.
- **FR-3.2** Map Parquet files on object storage into DuckDB using Hive partitioning.
- **FR-3.3** Prune partitions on time-range filters (must not scan all files for bounded time queries).
- **FR-3.4** Retrieve a full trace by trace_id.
- **FR-3.5** Search spans by service name, operation name, duration range, status, and attribute key-value pairs.
- **FR-3.6** List all distinct services and operations.
- **FR-3.7** Enforce query timeouts and result-size limits.

---

## 8. Non-Functional Requirements

### NFR-1: Performance
- **NFR-1.1** Ingestion: sustain ≥ 50,000 spans/sec per instance on commodity hardware (4 vCPU, 8 GB RAM).
- **NFR-1.2** Query: sub-second response for point lookups (single trace_id, bounded time range) on datasets up to 1 TB.
- **NFR-1.3** Query: < 10s for analytical scans (aggregations across hours of data) on datasets up to 1 TB.

### NFR-2: Scalability
- **NFR-2.1** Ingestion scales horizontally — run N instances behind a load balancer; each writes independent Parquet files.
- **NFR-2.2** Query scales vertically by allocating more memory/threads to DuckDB. Horizontal query scaling is a v2 concern.

### NFR-3: Reliability
- **NFR-3.1** No single span write should block or crash the ingestion service.
- **NFR-3.2** Malformed Parquet files must not break the query service (skip with a warning).

### NFR-4: Observability
- **NFR-4.1** The tool itself exports metrics (spans ingested, bytes written, query latency) via Prometheus endpoint.
- **NFR-4.2** Structured JSON logging.

### NFR-5: Security
- **NFR-5.1** Optional mTLS for OTLP endpoints.
- **NFR-5.2** Query API supports API-key authentication.
- **NFR-5.3** Object storage credentials via standard provider chains (IAM roles, workload identity).
- **NFR-5.4** SQL injection is not a concern since DuckDB runs read-only on Parquet files, but the query service must restrict DDL / DML statements.

---

## 9. Data Retention

- Retention is managed by object storage lifecycle rules (e.g., delete files older than 30 days).
- The query service automatically reflects retention — expired files disappear from the view on the next metadata refresh.
- A `retention.days` config option is provided for documentation and for the CLI to warn when querying beyond the retention window.

---

## 10. Deployment

### 10.1 Local Development

```bash
docker-compose up   # starts MinIO (S3), ingestion service, query service
```

### 10.2 Production

- **Ingestion:** Deploy as a stateless container (Kubernetes Deployment / Cloud Run). Scale replicas based on CPU.
- **Query:** Deploy as a stateless container. Each instance holds an ephemeral DuckDB in-process. Scale based on query load.
- **Storage:** Managed object storage (S3 / GCS). No database to operate.

---

## 11. Future Work (v2)

- **Write-ahead log** on the ingestion path for crash recovery.
- **Metadata catalog** (e.g., Iceberg / Delta Lake) for ACID-like semantics and better file management.
- **Compaction job** to merge small Parquet files into larger ones for query efficiency.
- **Horizontal query scaling** using DuckDB's upcoming distributed capabilities or a federation layer.
- **Tail-based sampling** at the ingestion layer to reduce storage for uninteresting traces.
- **Web UI** for trace visualization (waterfall view, service dependency graph).
- **Multi-tenancy** with per-tenant storage prefixes and access control.
- **Full-text search** on span attributes via an auxiliary inverted index.
