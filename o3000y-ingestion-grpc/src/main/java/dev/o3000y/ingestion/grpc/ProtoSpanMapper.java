package dev.o3000y.ingestion.grpc;

import dev.o3000y.model.*;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import java.time.Instant;
import java.util.*;

public final class ProtoSpanMapper {

  public List<Span> map(List<ResourceSpans> resourceSpansList) {
    List<Span> result = new ArrayList<>();
    for (ResourceSpans rs : resourceSpansList) {
      String serviceName = extractServiceName(rs.getResource());
      Map<String, String> resourceAttrs = extractAttributes(rs.getResource().getAttributesList());

      for (ScopeSpans ss : rs.getScopeSpansList()) {
        for (io.opentelemetry.proto.trace.v1.Span protoSpan : ss.getSpansList()) {
          result.add(mapSpan(protoSpan, serviceName, resourceAttrs));
        }
      }
    }
    return result;
  }

  private Span mapSpan(
      io.opentelemetry.proto.trace.v1.Span proto,
      String serviceName,
      Map<String, String> resourceAttrs) {
    String traceId = TraceId.fromBytes(proto.getTraceId().toByteArray());
    String spanId = TraceId.fromBytes(proto.getSpanId().toByteArray());
    String parentSpanId =
        proto.getParentSpanId().isEmpty()
            ? ""
            : TraceId.fromBytes(proto.getParentSpanId().toByteArray());

    long startNanos = proto.getStartTimeUnixNano();
    long endNanos = proto.getEndTimeUnixNano();
    Instant startTime =
        Instant.ofEpochSecond(startNanos / 1_000_000_000, startNanos % 1_000_000_000);
    Instant endTime = Instant.ofEpochSecond(endNanos / 1_000_000_000, endNanos % 1_000_000_000);
    long durationUs = (endNanos - startNanos) / 1_000;

    Map<String, String> spanAttrs = extractAttributes(proto.getAttributesList());
    // Merge resource attributes into span attributes
    Map<String, String> allAttrs = new HashMap<>(resourceAttrs);
    allAttrs.putAll(spanAttrs);

    List<SpanEvent> events = new ArrayList<>();
    for (io.opentelemetry.proto.trace.v1.Span.Event e : proto.getEventsList()) {
      long eventNanos = e.getTimeUnixNano();
      events.add(
          new SpanEvent(
              e.getName(),
              Instant.ofEpochSecond(eventNanos / 1_000_000_000, eventNanos % 1_000_000_000),
              extractAttributes(e.getAttributesList())));
    }

    List<SpanLink> links = new ArrayList<>();
    for (io.opentelemetry.proto.trace.v1.Span.Link l : proto.getLinksList()) {
      links.add(
          new SpanLink(
              TraceId.fromBytes(l.getTraceId().toByteArray()),
              TraceId.fromBytes(l.getSpanId().toByteArray()),
              l.getTraceState(),
              extractAttributes(l.getAttributesList())));
    }

    SpanKind spanKind = SpanKind.fromValue(proto.getKindValue());
    StatusCode statusCode = StatusCode.fromValue(proto.getStatus().getCodeValue());

    return new Span(
        traceId,
        spanId,
        parentSpanId,
        proto.getName(),
        serviceName,
        startTime,
        endTime,
        durationUs,
        statusCode,
        proto.getStatus().getMessage(),
        spanKind,
        allAttrs,
        events,
        links);
  }

  private String extractServiceName(Resource resource) {
    for (KeyValue kv : resource.getAttributesList()) {
      if ("service.name".equals(kv.getKey())) {
        return anyValueToString(kv.getValue());
      }
    }
    return "unknown";
  }

  private Map<String, String> extractAttributes(List<KeyValue> attrs) {
    Map<String, String> result = new LinkedHashMap<>();
    for (KeyValue kv : attrs) {
      result.put(kv.getKey(), anyValueToString(kv.getValue()));
    }
    return result;
  }

  private String anyValueToString(AnyValue value) {
    return switch (value.getValueCase()) {
      case STRING_VALUE -> value.getStringValue();
      case INT_VALUE -> String.valueOf(value.getIntValue());
      case DOUBLE_VALUE -> String.valueOf(value.getDoubleValue());
      case BOOL_VALUE -> String.valueOf(value.getBoolValue());
      case BYTES_VALUE -> TraceId.fromBytes(value.getBytesValue().toByteArray());
      case ARRAY_VALUE ->
          value.getArrayValue().getValuesList().stream()
              .map(this::anyValueToString)
              .collect(java.util.stream.Collectors.joining(",", "[", "]"));
      case KVLIST_VALUE ->
          value.getKvlistValue().getValuesList().stream()
              .map(kv -> kv.getKey() + "=" + anyValueToString(kv.getValue()))
              .collect(java.util.stream.Collectors.joining(",", "{", "}"));
      default -> value.toString();
    };
  }
}
