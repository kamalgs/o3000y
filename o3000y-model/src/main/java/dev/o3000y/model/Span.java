package dev.o3000y.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Span(
    String traceId,
    String spanId,
    String parentSpanId,
    String operationName,
    String serviceName,
    Instant startTime,
    Instant endTime,
    long durationUs,
    StatusCode statusCode,
    String statusMessage,
    SpanKind spanKind,
    Map<String, String> attributes,
    List<SpanEvent> events,
    List<SpanLink> links) {

  public Span {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    events = events == null ? List.of() : List.copyOf(events);
    links = links == null ? List.of() : List.copyOf(links);
    if (statusCode == null) {
      statusCode = StatusCode.UNSET;
    }
    if (spanKind == null) {
      spanKind = SpanKind.UNSPECIFIED;
    }
    if (parentSpanId == null) {
      parentSpanId = "";
    }
    if (statusMessage == null) {
      statusMessage = "";
    }
  }
}
