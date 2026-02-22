package dev.o3000y.app;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.storage.api.StorageConfig;
import java.nio.file.Path;

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
}
