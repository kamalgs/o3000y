package dev.o3000y.loadgen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoadGenConfigTest {

  @Test
  void defaultsAreReasonable() {
    LoadGenConfig config = LoadGenConfig.defaults();
    assertEquals("localhost", config.targetHost());
    assertEquals(4317, config.targetPort());
    assertEquals(10.0, config.tracesPerSecond());
    assertEquals(60, config.durationSeconds());
    assertTrue(config.errorRate() >= 0 && config.errorRate() <= 1);
  }

  @Test
  void parsesArgs() {
    LoadGenConfig config =
        LoadGenConfig.fromArgs(
            new String[] {"--host=example.com", "--port=5555", "--tps=100", "--duration=30"});
    assertEquals("example.com", config.targetHost());
    assertEquals(5555, config.targetPort());
    assertEquals(100.0, config.tracesPerSecond());
    assertEquals(30, config.durationSeconds());
  }

  @Test
  void ignoresUnknownArgs() {
    LoadGenConfig config = LoadGenConfig.fromArgs(new String[] {"--unknown=value", "--tps=5"});
    assertEquals(5.0, config.tracesPerSecond());
    assertEquals("localhost", config.targetHost()); // default
  }
}
