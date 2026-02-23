package dev.o3000y.storage.api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class HivePartitionStrategyTest {

  private final HivePartitionStrategy strategy = new HivePartitionStrategy();

  @Test
  void partitionPath_formatsCorrectly() {
    Instant timestamp = Instant.parse("2026-02-09T14:30:00Z");
    assertEquals("year=2026/month=02/day=09/hour=14", strategy.partitionPath(timestamp));
  }

  @Test
  void partitionPath_midnight() {
    Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
    assertEquals("year=2026/month=01/day=01/hour=00", strategy.partitionPath(timestamp));
  }

  @Test
  void partitionPath_endOfDay() {
    Instant timestamp = Instant.parse("2026-12-31T23:59:59Z");
    assertEquals("year=2026/month=12/day=31/hour=23", strategy.partitionPath(timestamp));
  }
}
