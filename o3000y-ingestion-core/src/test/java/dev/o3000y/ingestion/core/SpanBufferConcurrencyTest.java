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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class SpanBufferConcurrencyTest {

  private Span testSpan() {
    return new Span(
        "trace1",
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
  void concurrentReceive_noLostSpans() throws InterruptedException {
    List<List<Span>> flushed = new CopyOnWriteArrayList<>();
    StorageWriter writer = flushed::add;

    int threads = 5;
    int spansPerThread = 20;
    BatchConfig config =
        new BatchConfig(threads * spansPerThread + 1, Long.MAX_VALUE, Duration.ofHours(1));
    SpanBuffer buffer = new SpanBuffer(writer, config);

    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);

    for (int t = 0; t < threads; t++) {
      executor.submit(
          () -> {
            for (int i = 0; i < spansPerThread; i++) {
              buffer.receive(List.of(testSpan()));
            }
            latch.countDown();
          });
    }

    latch.await();
    buffer.shutdown();
    executor.shutdown();

    int totalFlushed = flushed.stream().mapToInt(List::size).sum();
    assertEquals(threads * spansPerThread, totalFlushed);
  }
}
