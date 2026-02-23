# Future Backlog (v2+)

## Storage
- [ ] S3/GCS storage backend (swap Guice module)
- [ ] Proper Arrow MAP/LIST types for attributes, events, links (replace JSON VARCHAR)
- [ ] Data retention policy (auto-delete partitions older than N days)
- [ ] Compaction (merge small Parquet files)
- [ ] Bloom filters for trace_id lookups

## Ingestion
- [ ] HTTP/protobuf transport (OTLP/HTTP)
- [ ] Backpressure signaling (gRPC flow control)
- [ ] Metrics ingestion (OTLP metrics)
- [ ] Log ingestion (OTLP logs)
- [ ] Tail sampling

## Query
- [ ] Query caching (LRU cache for frequent queries)
- [ ] Materialized views for common aggregations
- [ ] Time-range pruning in DuckDB queries
- [ ] Saved queries
- [ ] Query history

## UI
- [ ] CodeMirror 6 SQL editor with autocomplete
- [ ] Real-time span streaming (WebSocket)
- [ ] Service dependency graph
- [ ] Alerting rules UI
- [ ] Dark mode

## Operations
- [ ] Prometheus metrics endpoint
- [ ] Kubernetes Helm chart
- [ ] Multi-node clustering
- [ ] Authentication/authorization
- [ ] Rate limiting
