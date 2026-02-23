package dev.o3000y.query.rest;

import java.util.List;

public record TraceResponse(String traceId, int spanCount, List<SpanResponse> spans) {}
