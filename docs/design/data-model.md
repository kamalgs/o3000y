# Data Model

## Domain Records

### Span
The core domain object representing an OpenTelemetry span.

| Field | Type | Description |
|-------|------|-------------|
| traceId | String | 32-char hex trace identifier |
| spanId | String | 16-char hex span identifier |
| parentSpanId | String | Parent span ID (empty for root spans) |
| operationName | String | Operation/method name |
| serviceName | String | Originating service |
| startTime | Instant | Span start timestamp |
| endTime | Instant | Span end timestamp |
| durationUs | long | Duration in microseconds |
| statusCode | StatusCode | OK, ERROR, or UNSET |
| statusMessage | String | Status description |
| spanKind | SpanKind | SERVER, CLIENT, PRODUCER, CONSUMER, INTERNAL |
| attributes | Map<String, String> | Key-value metadata |
| events | List<SpanEvent> | Timestamped events within the span |
| links | List<SpanLink> | References to other traces/spans |

## Parquet Schema

```
message spans {
  required binary trace_id (UTF8);
  required binary span_id (UTF8);
  optional binary parent_span_id (UTF8);
  required binary operation_name (UTF8);
  required binary service_name (UTF8);
  required int64 start_time (TIMESTAMP(MICROS,true));
  required int64 end_time (TIMESTAMP(MICROS,true));
  required int64 duration_us;
  required int32 status_code;
  optional binary status_message (UTF8);
  required int32 span_kind;
  optional binary attributes (UTF8);     -- JSON string
  optional binary events (UTF8);         -- JSON string
  optional binary links (UTF8);          -- JSON string
}
```

## Storage Layout

Parquet files are stored in Hive-partitioned directories:

```
data/
  year=2026/
    month=02/
      day=09/
        hour=14/
          abc12345_1707481200_1.parquet
          abc12345_1707481200_2.parquet
        hour=15/
          abc12345_1707484800_3.parquet
```

Sort order within files: `service_name ASC, start_time ASC`
