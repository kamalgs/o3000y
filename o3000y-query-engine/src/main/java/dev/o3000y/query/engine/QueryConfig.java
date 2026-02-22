package dev.o3000y.query.engine;

import java.nio.file.Path;
import java.time.Duration;

public record QueryConfig(Path dataPath, int maxResultRows, Duration queryTimeout) {

  public static QueryConfig defaults(Path dataPath) {
    return new QueryConfig(dataPath, 10_000, Duration.ofSeconds(60));
  }
}
