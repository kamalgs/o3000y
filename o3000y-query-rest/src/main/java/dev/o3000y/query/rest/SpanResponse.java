package dev.o3000y.query.rest;

import java.util.Map;

public record SpanResponse(
    String traceId,
    String spanId,
    String parentSpanId,
    String operationName,
    String serviceName,
    String startTime,
    String endTime,
    long durationUs,
    int statusCode,
    String statusMessage,
    int spanKind,
    Map<String, String> attributes) {}
