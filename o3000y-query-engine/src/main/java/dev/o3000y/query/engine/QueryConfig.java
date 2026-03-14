package dev.o3000y.query.engine;

import java.time.Duration;

public record QueryConfig(int maxResultRows, Duration queryTimeout) {

  public static QueryConfig defaults() {
    return new QueryConfig(10_000, Duration.ofSeconds(60));
  }
}
