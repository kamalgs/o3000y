package dev.o3000y.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.o3000y.storage.parquet.ParquetSpanWriter;
import jakarta.inject.Singleton;

public final class StorageParquetModule extends AbstractModule {

  @Provides
  @Singleton
  ParquetSpanWriter provideParquetWriter() {
    return new ParquetSpanWriter();
  }
}
