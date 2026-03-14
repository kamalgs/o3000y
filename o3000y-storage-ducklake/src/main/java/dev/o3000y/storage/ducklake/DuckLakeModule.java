package dev.o3000y.storage.ducklake;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.o3000y.storage.api.StorageWriter;
import jakarta.inject.Singleton;

public final class DuckLakeModule extends AbstractModule {

  private final DuckLakeConfig config;

  public DuckLakeModule(DuckLakeConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(StorageWriter.class).to(DuckLakeStorageWriter.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  DuckLakeConfig provideDuckLakeConfig() {
    return config;
  }

  @Provides
  @Singleton
  DuckLakeManager provideDuckLakeManager() {
    return new DuckLakeManager(config);
  }
}
