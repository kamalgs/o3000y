package dev.o3000y.testing.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.core.SpanBuffer;
import dev.o3000y.model.Span;
import dev.o3000y.storage.api.HivePartitionStrategy;
import dev.o3000y.storage.api.StorageConfig;
import dev.o3000y.storage.local.LocalStorageWriter;
import dev.o3000y.storage.parquet.ParquetSpanWriter;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IngestionStorageIntegrationTest {

  @TempDir Path tempDir;

  @Test
  void ingestionBuffer_flushesSpans_toParquetFiles() throws Exception {
    // Setup storage
    LocalStorageWriter storageWriter =
        new LocalStorageWriter(
            new StorageConfig(tempDir), new HivePartitionStrategy(), new ParquetSpanWriter());

    // Setup buffer with low threshold to flush immediately
    BatchConfig batchConfig = new BatchConfig(5, Long.MAX_VALUE, Duration.ofHours(1));
    SpanBuffer buffer = new SpanBuffer(storageWriter, batchConfig);

    // Send spans through ingestion
    List<Span> trace = SpanFixtures.aTrace(5);
    buffer.receive(trace);

    // Buffer should have auto-flushed at threshold
    buffer.shutdown();

    // Verify Parquet files were written in Hive partition paths
    try (var stream = Files.walk(tempDir)) {
      long parquetCount = stream.filter(p -> p.toString().endsWith(".parquet")).count();
      assertTrue(parquetCount >= 1, "Expected at least one Parquet file, found " + parquetCount);
    }

    // Verify partition directory structure
    try (var stream = Files.walk(tempDir)) {
      boolean hasHivePartitions =
          stream.anyMatch(p -> p.toString().contains("year=") && p.toString().contains("month="));
      assertTrue(hasHivePartitions, "Expected Hive partition directories");
    }
  }
}
