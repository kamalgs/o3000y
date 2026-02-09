package dev.tracequery;

/**
 * Flat representation of a single OTel span, ready for Parquet storage.
 * Attributes are stored as JSON strings for simplicity in the MVP.
 */
public record SpanRecord(
    String traceId,
    String spanId,
    String parentSpanId,
    String serviceName,
    String operationName,
    int spanKind,
    long startTimeUnixNano,
    long endTimeUnixNano,
    long durationUs,
    int statusCode,
    String statusMessage,
    String spanAttributes,
    String resourceAttributes
) {}
