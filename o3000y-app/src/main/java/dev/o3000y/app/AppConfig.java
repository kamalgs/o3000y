package dev.o3000y.app;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.storage.api.StorageConfig;
import java.nio.file.Path;
import java.time.Duration;

public record AppConfig(
    StorageConfig storage, BatchConfig batch, QueryConfig query, int grpcPort, int restPort) {

  public static AppConfig fromDefaults() {
    Path dataPath = Path.of("data");
    return new AppConfig(
        new StorageConfig(dataPath),
        BatchConfig.defaults(),
        QueryConfig.defaults(dataPath),
        4317,
        8080);
  }

  public static AppConfig fromEnvironment() {
    Path dataPath = Path.of(env("O3000Y_DATA_PATH", "data"));
    int grpcPort = Integer.parseInt(env("O3000Y_GRPC_PORT", "4317"));
    int restPort = Integer.parseInt(env("O3000Y_REST_PORT", "8080"));
    int batchMaxSpans = Integer.parseInt(env("O3000Y_BATCH_MAX_SPANS", "10000"));
    long batchMaxBytes =
        Long.parseLong(env("O3000Y_BATCH_MAX_BYTES", String.valueOf(16 * 1024 * 1024)));
    int batchFlushIntervalSec = Integer.parseInt(env("O3000Y_BATCH_FLUSH_INTERVAL_SEC", "30"));
    int maxResultRows = Integer.parseInt(env("O3000Y_MAX_RESULT_ROWS", "10000"));
    int queryTimeoutSec = Integer.parseInt(env("O3000Y_QUERY_TIMEOUT_SEC", "60"));
    long refreshIntervalSec = Long.parseLong(env("O3000Y_REFRESH_INTERVAL_SEC", "30"));

    return new AppConfig(
        new StorageConfig(dataPath),
        new BatchConfig(batchMaxSpans, batchMaxBytes, Duration.ofSeconds(batchFlushIntervalSec)),
        new QueryConfig(
            dataPath, maxResultRows, Duration.ofSeconds(queryTimeoutSec), refreshIntervalSec),
        grpcPort,
        restPort);
  }

  private static String env(String key, String defaultValue) {
    String value = System.getenv(key);
    return value != null && !value.isEmpty() ? value : defaultValue;
  }
}
