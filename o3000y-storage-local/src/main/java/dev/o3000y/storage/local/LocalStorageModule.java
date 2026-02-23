package dev.o3000y.storage.local;

import com.google.inject.AbstractModule;
import dev.o3000y.storage.api.HivePartitionStrategy;
import dev.o3000y.storage.api.PartitionStrategy;
import dev.o3000y.storage.api.StorageConfig;
import dev.o3000y.storage.api.StorageWriter;
import jakarta.inject.Singleton;

public final class LocalStorageModule extends AbstractModule {

  private final StorageConfig config;

  public LocalStorageModule(StorageConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(StorageConfig.class).toInstance(config);
    bind(PartitionStrategy.class).to(HivePartitionStrategy.class).in(Singleton.class);
    bind(StorageWriter.class).to(LocalStorageWriter.class).in(Singleton.class);
  }
}
