package dev.o3000y.ingestion.grpc;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import dev.o3000y.model.Span;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Status;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtoSpanMapperTest {

  private final ProtoSpanMapper mapper = new ProtoSpanMapper();

  @Test
  void mapsBasicSpan() {
    byte[] traceIdBytes = hexToBytes("0af7651916cd43dd8448eb211c80319c");
    byte[] spanIdBytes = hexToBytes("00f067aa0ba902b7");

    Resource resource =
        Resource.newBuilder()
            .addAttributes(
                KeyValue.newBuilder()
                    .setKey("service.name")
                    .setValue(AnyValue.newBuilder().setStringValue("test-service")))
            .build();

    io.opentelemetry.proto.trace.v1.Span protoSpan =
        io.opentelemetry.proto.trace.v1.Span.newBuilder()
            .setTraceId(ByteString.copyFrom(traceIdBytes))
            .setSpanId(ByteString.copyFrom(spanIdBytes))
            .setName("GET /api/test")
            .setKind(io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_SERVER)
            .setStartTimeUnixNano(1_707_480_000_000_000_000L)
            .setEndTimeUnixNano(1_707_480_000_100_000_000L)
            .setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_OK))
            .addAttributes(
                KeyValue.newBuilder()
                    .setKey("http.method")
                    .setValue(AnyValue.newBuilder().setStringValue("GET")))
            .build();

    ResourceSpans rs =
        ResourceSpans.newBuilder()
            .setResource(resource)
            .addScopeSpans(ScopeSpans.newBuilder().addSpans(protoSpan))
            .build();

    List<Span> spans = mapper.map(List.of(rs));

    assertEquals(1, spans.size());
    Span span = spans.getFirst();
    assertEquals("0af7651916cd43dd8448eb211c80319c", span.traceId());
    assertEquals("00f067aa0ba902b7", span.spanId());
    assertEquals("GET /api/test", span.operationName());
    assertEquals("test-service", span.serviceName());
    assertEquals(dev.o3000y.model.SpanKind.SERVER, span.spanKind());
    assertEquals(dev.o3000y.model.StatusCode.OK, span.statusCode());
    assertEquals("GET", span.attributes().get("http.method"));
    assertEquals(100_000L, span.durationUs());
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] bytes = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      bytes[i / 2] =
          (byte)
              ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
    }
    return bytes;
  }
}
