package dev.o3000y.ingestion.api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BatchConfigTest {

  @Test
  void defaults_hasExpectedValues() {
    BatchConfig config = BatchConfig.defaults();
    assertEquals(10_000, config.maxSpans());
    assertEquals(16 * 1024 * 1024, config.maxBytes());
    assertEquals(Duration.ofSeconds(30), config.flushInterval());
  }
}
