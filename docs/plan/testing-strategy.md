# Testing Strategy

## Test Pyramid

### Unit Tests (fastest, most isolated)
- **o3000y-model**: TraceId hex conversion
- **o3000y-ingestion-api**: BatchConfig defaults
- **o3000y-storage-api**: HivePartitionStrategy path generation
- **o3000y-storage-parquet**: Parquet write + DuckDB read-back
- **o3000y-storage-local**: Multi-partition write verification
- **o3000y-ingestion-core**: SpanBuffer threshold/timer flush, concurrency
- **o3000y-ingestion-grpc**: ProtoSpanMapper field mapping, OtlpTraceService with InProcessServer
- **o3000y-query-engine**: DuckDB query, trace lookup, invalid SQL, result limits
- **o3000y-query-rest**: REST endpoint HTTP status codes, JSON responses

### Integration Tests (cross-module)
- **Ingestion → Storage**: Buffer flush writes Parquet files at correct paths
- **Storage → Query**: Write known data → DuckDB query → verify results
- **Full Pipeline**: receive → flush → refreshView → query (no transport)

### Architecture Tests (fitness functions)
- Model depends on nothing
- Storage cannot depend on ingestion
- Query cannot depend on ingestion
- Ingestion cannot depend on query

### System Tests (end-to-end)
- Full round-trip: gRPC → Parquet → DuckDB → REST
- SQL query endpoint
- Multi-trace retrieval (10 traces × 5 spans)
- Service filtering via search endpoint
- Concurrent ingestion (5 threads × 20 traces, zero data loss)

### UI Tests (Vitest + vue-test-utils)
- ResultsTable rendering, sorting, trace_id links
- TraceWaterfall span display, service colors, span details on click
- DurationHistogram with duration_us column detection

## Tools

| Tool | Purpose |
|------|---------|
| JUnit 5 | Test framework |
| ArchUnit | Architecture fitness tests |
| JaCoCo | Code coverage reports |
| Vitest | Vue component tests |
| Spotless | Code formatting enforcement |
