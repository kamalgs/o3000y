package dev.o3000y.app;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.storage.ducklake.DuckLakeConfig;
import java.time.Duration;

public record AppConfig(
    DuckLakeConfig duckLake, BatchConfig batch, QueryConfig query, int grpcPort, int restPort) {

  public static AppConfig fromDefaults() {
    return new AppConfig(
        DuckLakeConfig.defaults(), BatchConfig.defaults(), QueryConfig.defaults(), 4317, 8080);
  }

  public static AppConfig fromEnvironment() {
    String catalogUri = env("O3000Y_CATALOG_URI", "data/metadata.ducklake");
    String dataPath = env("O3000Y_DATA_PATH", "data/files/");
    int grpcPort = Integer.parseInt(env("O3000Y_GRPC_PORT", "4317"));
    int restPort = Integer.parseInt(env("O3000Y_REST_PORT", "8080"));
    int batchMaxSpans = Integer.parseInt(env("O3000Y_BATCH_MAX_SPANS", "10000"));
    long batchMaxBytes =
        Long.parseLong(env("O3000Y_BATCH_MAX_BYTES", String.valueOf(16 * 1024 * 1024)));
    int batchFlushIntervalSec = Integer.parseInt(env("O3000Y_BATCH_FLUSH_INTERVAL_SEC", "30"));
    int maxResultRows = Integer.parseInt(env("O3000Y_MAX_RESULT_ROWS", "10000"));
    int queryTimeoutSec = Integer.parseInt(env("O3000Y_QUERY_TIMEOUT_SEC", "60"));

    return new AppConfig(
        new DuckLakeConfig(catalogUri, dataPath),
        new BatchConfig(batchMaxSpans, batchMaxBytes, Duration.ofSeconds(batchFlushIntervalSec)),
        new QueryConfig(maxResultRows, Duration.ofSeconds(queryTimeoutSec)),
        grpcPort,
        restPort);
  }

  private static String env(String key, String defaultValue) {
    String value = System.getenv(key);
    return value != null && !value.isEmpty() ? value : defaultValue;
  }
}
