package dev.o3000y.storage.ducklake;

public record DuckLakeConfig(String catalogUri, String dataPath, int dataInliningRowLimit) {

  public DuckLakeConfig(String catalogUri, String dataPath) {
    this(catalogUri, dataPath, 1000);
  }

  public static DuckLakeConfig defaults() {
    return new DuckLakeConfig("data/metadata.ducklake", "data/files/", 1000);
  }
}
