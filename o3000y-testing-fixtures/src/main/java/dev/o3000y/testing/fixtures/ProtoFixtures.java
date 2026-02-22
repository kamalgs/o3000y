package dev.o3000y.testing.fixtures;

import com.google.protobuf.ByteString;
import dev.o3000y.model.Span;
import dev.o3000y.model.TraceId;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Status;
import java.util.List;
import java.util.Map;

public final class ProtoFixtures {

  private ProtoFixtures() {}

  public static ExportTraceServiceRequest toExportRequest(List<Span> spans) {
    ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();

    // Group spans by service name
    Map<String, List<Span>> byService = new java.util.LinkedHashMap<>();
    for (Span span : spans) {
      byService.computeIfAbsent(span.serviceName(), k -> new java.util.ArrayList<>()).add(span);
    }

    for (Map.Entry<String, List<Span>> entry : byService.entrySet()) {
      Resource resource =
          Resource.newBuilder()
              .addAttributes(
                  KeyValue.newBuilder()
                      .setKey("service.name")
                      .setValue(AnyValue.newBuilder().setStringValue(entry.getKey())))
              .build();

      ScopeSpans.Builder scopeBuilder = ScopeSpans.newBuilder();
      for (Span span : entry.getValue()) {
        scopeBuilder.addSpans(toProtoSpan(span));
      }

      builder.addResourceSpans(
          ResourceSpans.newBuilder().setResource(resource).addScopeSpans(scopeBuilder));
    }

    return builder.build();
  }

  private static io.opentelemetry.proto.trace.v1.Span toProtoSpan(Span span) {
    long startNanos =
        span.startTime().getEpochSecond() * 1_000_000_000 + span.startTime().getNano();
    long endNanos = span.endTime().getEpochSecond() * 1_000_000_000 + span.endTime().getNano();

    io.opentelemetry.proto.trace.v1.Span.Builder builder =
        io.opentelemetry.proto.trace.v1.Span.newBuilder()
            .setTraceId(ByteString.copyFrom(TraceId.toBytes(span.traceId())))
            .setSpanId(ByteString.copyFrom(TraceId.toBytes(span.spanId())))
            .setName(span.operationName())
            .setKind(
                io.opentelemetry.proto.trace.v1.Span.SpanKind.forNumber(span.spanKind().value()))
            .setStartTimeUnixNano(startNanos)
            .setEndTimeUnixNano(endNanos)
            .setStatus(
                Status.newBuilder()
                    .setCode(Status.StatusCode.forNumber(span.statusCode().value())));

    if (!span.parentSpanId().isEmpty()) {
      builder.setParentSpanId(ByteString.copyFrom(TraceId.toBytes(span.parentSpanId())));
    }

    for (Map.Entry<String, String> attr : span.attributes().entrySet()) {
      builder.addAttributes(
          KeyValue.newBuilder()
              .setKey(attr.getKey())
              .setValue(AnyValue.newBuilder().setStringValue(attr.getValue())));
    }

    return builder.build();
  }
}
