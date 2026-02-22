package dev.o3000y.testing.fixtures;

import dev.o3000y.model.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class SpanFixtures {

  private SpanFixtures() {}

  public static String randomTraceId() {
    byte[] bytes = new byte[16];
    ThreadLocalRandom.current().nextBytes(bytes);
    return TraceId.fromBytes(bytes);
  }

  public static String randomSpanId() {
    byte[] bytes = new byte[8];
    ThreadLocalRandom.current().nextBytes(bytes);
    return TraceId.fromBytes(bytes);
  }

  public static Span aSpan() {
    return aSpan(randomTraceId(), randomSpanId(), "", "test-operation", "test-service");
  }

  public static Span aRootSpan(String traceId, String serviceName) {
    return aSpan(traceId, randomSpanId(), "", "root-operation", serviceName);
  }

  public static Span aChildSpan(String traceId, String parentSpanId, String serviceName) {
    return aSpan(traceId, randomSpanId(), parentSpanId, "child-operation", serviceName);
  }

  public static Span aSpan(
      String traceId,
      String spanId,
      String parentSpanId,
      String operationName,
      String serviceName) {
    Instant start = Instant.now();
    Instant end = start.plusMillis(100);
    return new Span(
        traceId,
        spanId,
        parentSpanId,
        operationName,
        serviceName,
        start,
        end,
        100_000L,
        StatusCode.OK,
        "",
        SpanKind.SERVER,
        Map.of("test.key", "test.value"),
        List.of(),
        List.of());
  }

  public static List<Span> aTrace(int spanCount) {
    String traceId = randomTraceId();
    String rootSpanId = randomSpanId();
    List<Span> spans = new ArrayList<>();
    spans.add(aSpan(traceId, rootSpanId, "", "root-operation", "root-service"));
    for (int i = 1; i < spanCount; i++) {
      spans.add(aChildSpan(traceId, rootSpanId, "child-service-" + i));
    }
    return spans;
  }
}
