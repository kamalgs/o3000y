package dev.o3000y.ingestion.api;

import java.time.Duration;

public record BatchConfig(int maxSpans, long maxBytes, Duration flushInterval) {

  public static BatchConfig defaults() {
    return new BatchConfig(10_000, 16 * 1024 * 1024, Duration.ofSeconds(30));
  }
}
