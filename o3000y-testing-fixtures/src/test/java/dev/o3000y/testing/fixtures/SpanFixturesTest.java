package dev.o3000y.testing.fixtures;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.model.Span;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpanFixturesTest {

  @Test
  void aTrace_producesLinkedSpans() {
    List<Span> trace = SpanFixtures.aTrace(3);

    assertEquals(3, trace.size());
    // All share same traceId
    String traceId = trace.getFirst().traceId();
    assertTrue(trace.stream().allMatch(s -> s.traceId().equals(traceId)));

    // Root has no parent
    assertTrue(trace.getFirst().parentSpanId().isEmpty());

    // Children reference root's spanId
    String rootSpanId = trace.getFirst().spanId();
    for (int i = 1; i < trace.size(); i++) {
      assertEquals(rootSpanId, trace.get(i).parentSpanId());
    }
  }
}
