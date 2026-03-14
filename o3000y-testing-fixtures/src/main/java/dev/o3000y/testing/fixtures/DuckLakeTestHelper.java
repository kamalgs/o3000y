package dev.o3000y.testing.fixtures;

import dev.o3000y.storage.ducklake.DuckLakeConfig;
import dev.o3000y.storage.ducklake.DuckLakeManager;
import dev.o3000y.storage.ducklake.DuckLakeStorageWriter;
import java.nio.file.Path;

public final class DuckLakeTestHelper {

  private final DuckLakeManager manager;
  private final DuckLakeStorageWriter writer;

  public DuckLakeTestHelper(Path tempDir) {
    DuckLakeConfig config =
        new DuckLakeConfig(
            tempDir.resolve("metadata.ducklake").toString(),
            tempDir.resolve("files").toString() + "/");
    this.manager = new DuckLakeManager(config);
    this.writer = new DuckLakeStorageWriter(manager);
  }

  public DuckLakeManager manager() {
    return manager;
  }

  public DuckLakeStorageWriter writer() {
    return writer;
  }

  public void close() {
    manager.close();
  }
}
