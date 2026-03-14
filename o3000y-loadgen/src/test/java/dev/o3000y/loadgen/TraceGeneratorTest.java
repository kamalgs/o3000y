package dev.o3000y.loadgen;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.model.Span;
import dev.o3000y.model.StatusCode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TraceGeneratorTest {

  @Test
  void generatesValidTrace() {
    TraceGenerator gen = new TraceGenerator(0.0, 3, 2);
    List<Span> trace = gen.generate();

    assertFalse(trace.isEmpty());

    // All spans share the same traceId
    String traceId = trace.getFirst().traceId();
    assertTrue(trace.stream().allMatch(s -> s.traceId().equals(traceId)));

    // All spans have non-empty spanId
    assertTrue(trace.stream().allMatch(s -> !s.spanId().isEmpty()));

    // All spans have valid timestamps (end >= start)
    assertTrue(trace.stream().allMatch(s -> !s.endTime().isBefore(s.startTime())));
  }

  @Test
  void respectsMaxDepth() {
    TraceGenerator gen = new TraceGenerator(0.0, 2, 3);
    // With depth 2, we should have limited nesting
    for (int i = 0; i < 20; i++) {
      List<Span> trace = gen.generate();
      // Count depth by following parent chain
      for (Span span : trace) {
        int depth = 0;
        String parentId = span.parentSpanId();
        while (!parentId.isEmpty()) {
          depth++;
          String pid = parentId;
          Span parent = trace.stream().filter(s -> s.spanId().equals(pid)).findFirst().orElse(null);
          parentId = parent != null ? parent.parentSpanId() : "";
        }
        assertTrue(depth <= 2, "Depth " + depth + " exceeds maxDepth 2");
      }
    }
  }

  @Test
  void usesRealisticServiceNames() {
    TraceGenerator gen = new TraceGenerator(0.0, 5, 3);
    List<Span> trace = gen.generate();

    Set<String> serviceNames = trace.stream().map(Span::serviceName).collect(Collectors.toSet());
    // Should always start with api-gateway
    assertTrue(
        trace.stream().anyMatch(s -> s.serviceName().equals("api-gateway")),
        "Should have api-gateway span");
    // Should have at least 2 different services
    assertTrue(serviceNames.size() >= 2, "Should have multiple services, got: " + serviceNames);
  }

  @Test
  void errorInjectionWorks() {
    // High error rate to make it statistically reliable
    TraceGenerator gen = new TraceGenerator(1.0, 2, 1);
    List<Span> trace = gen.generate();

    long errorCount = trace.stream().filter(s -> s.statusCode() == StatusCode.ERROR).count();
    assertTrue(errorCount > 0, "With 100% error rate, should have errors");
  }

  @Test
  void zeroErrorRateProducesNoErrors() {
    TraceGenerator gen = new TraceGenerator(0.0, 3, 2);
    long errors = 0;
    for (int i = 0; i < 50; i++) {
      errors += gen.generate().stream().filter(s -> s.statusCode() == StatusCode.ERROR).count();
    }
    assertEquals(0, errors, "Zero error rate should produce no errors");
  }

  @Test
  void spansHaveAttributes() {
    TraceGenerator gen = new TraceGenerator(0.0, 3, 2);
    List<Span> trace = gen.generate();

    // At least the root api-gateway span should have http attributes
    Span gateway =
        trace.stream().filter(s -> s.serviceName().equals("api-gateway")).findFirst().orElseThrow();
    assertTrue(gateway.attributes().containsKey("http.method"));
  }
}
