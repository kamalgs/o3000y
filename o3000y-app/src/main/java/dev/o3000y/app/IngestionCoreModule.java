package dev.o3000y.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.api.SpanReceiver;
import dev.o3000y.ingestion.core.SpanBuffer;
import dev.o3000y.storage.api.StorageWriter;
import jakarta.inject.Singleton;

public final class IngestionCoreModule extends AbstractModule {

  private final BatchConfig config;

  public IngestionCoreModule(BatchConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(BatchConfig.class).toInstance(config);
  }

  @Provides
  @Singleton
  SpanBuffer provideSpanBuffer(StorageWriter storageWriter) {
    return new SpanBuffer(storageWriter, config);
  }

  @Provides
  @Singleton
  SpanReceiver provideSpanReceiver(SpanBuffer buffer) {
    return buffer;
  }
}
