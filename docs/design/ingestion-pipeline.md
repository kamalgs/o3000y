# Ingestion Pipeline

## Flow

1. **gRPC Server** receives `ExportTraceServiceRequest` on port 4317
2. **ProtoSpanMapper** converts OTel proto spans to domain `Span` records
   - Extracts resource attributes (including `service.name`)
   - Validates spans (skips empty trace_id/span_id, invalid timestamps)
   - Handles all AnyValue types (string, int, double, bool, bytes, array, kvlist)
3. **SpanBuffer** batches spans in memory
   - Flushes when span count threshold is reached
   - Flushes when byte size threshold is reached
   - Periodic timer-based flush (configurable interval)
   - Thread-safe via `ReentrantLock`
4. **LocalStorageWriter** writes Parquet files
   - Groups spans by hourly partition
   - Creates Hive-partitioned directory structure
   - Collision-free file naming: `<instanceId>_<timestamp>_<sequence>.parquet`

## Configuration

| Env Variable | Default | Description |
|-------------|---------|-------------|
| O3000Y_GRPC_PORT | 4317 | gRPC listen port |
| O3000Y_BATCH_MAX_SPANS | 10000 | Flush threshold (span count) |
| O3000Y_BATCH_MAX_BYTES | 16MB | Flush threshold (estimated bytes) |
| O3000Y_BATCH_FLUSH_INTERVAL_SEC | 30 | Timer flush interval |

## Error Handling

- Invalid spans are logged and skipped (not rejected)
- Storage IO failures are logged but don't crash the process
- gRPC errors return `Status.INTERNAL` with description
