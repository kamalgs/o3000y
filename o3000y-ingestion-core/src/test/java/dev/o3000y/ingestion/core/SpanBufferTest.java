package dev.o3000y.ingestion.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.model.*;
import dev.o3000y.storage.api.StorageWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class SpanBufferTest {

  private Span testSpan(String traceId) {
    return new Span(
        traceId,
        "span1",
        "",
        "op",
        "svc",
        Instant.now(),
        Instant.now().plusMillis(100),
        100_000L,
        StatusCode.OK,
        "",
        SpanKind.SERVER,
        Map.of(),
        List.of(),
        List.of());
  }

  @Test
  void flushesOnSpanCountThreshold() {
    List<List<Span>> flushed = new CopyOnWriteArrayList<>();
    StorageWriter writer = flushed::add;

    BatchConfig config = new BatchConfig(3, Long.MAX_VALUE, Duration.ofHours(1));
    SpanBuffer buffer = new SpanBuffer(writer, config);

    buffer.receive(List.of(testSpan("t1"), testSpan("t2"), testSpan("t3")));
    buffer.shutdown();

    assertEquals(1, flushed.size());
    assertEquals(3, flushed.getFirst().size());
  }

  @Test
  void doesNotFlushBelowThreshold() {
    List<List<Span>> flushed = new CopyOnWriteArrayList<>();
    StorageWriter writer = flushed::add;

    BatchConfig config = new BatchConfig(10, Long.MAX_VALUE, Duration.ofHours(1));
    SpanBuffer buffer = new SpanBuffer(writer, config);

    buffer.receive(List.of(testSpan("t1")));
    // Flush is async, so call shutdown to wait for completion
    buffer.shutdown();
    assertEquals(1, flushed.size());
  }

  @Test
  void timerFlush() throws InterruptedException {
    List<List<Span>> flushed = new CopyOnWriteArrayList<>();
    StorageWriter writer = flushed::add;

    BatchConfig config = new BatchConfig(1000, Long.MAX_VALUE, Duration.ofMillis(100));
    SpanBuffer buffer = new SpanBuffer(writer, config);

    buffer.receive(List.of(testSpan("t1")));
    Thread.sleep(500);
    buffer.shutdown();

    assertEquals(1, flushed.size());
  }
}
