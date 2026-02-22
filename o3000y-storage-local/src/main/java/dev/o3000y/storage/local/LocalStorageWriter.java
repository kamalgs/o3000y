package dev.o3000y.storage.local;

import dev.o3000y.model.Span;
import dev.o3000y.storage.api.PartitionStrategy;
import dev.o3000y.storage.api.StorageConfig;
import dev.o3000y.storage.api.StorageWriter;
import dev.o3000y.storage.parquet.ParquetSpanWriter;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocalStorageWriter implements StorageWriter {

  private static final Logger LOG = LoggerFactory.getLogger(LocalStorageWriter.class);

  private final StorageConfig config;
  private final PartitionStrategy partitionStrategy;
  private final ParquetSpanWriter parquetWriter;
  private final String instanceId;
  private final AtomicLong sequence = new AtomicLong(0);

  @Inject
  public LocalStorageWriter(
      StorageConfig config, PartitionStrategy partitionStrategy, ParquetSpanWriter parquetWriter) {
    this.config = config;
    this.partitionStrategy = partitionStrategy;
    this.parquetWriter = parquetWriter;
    this.instanceId = UUID.randomUUID().toString().substring(0, 8);
  }

  @Override
  public void write(List<Span> spans) {
    if (spans.isEmpty()) return;

    String partition = partitionStrategy.partitionPath(spans.getFirst().startTime());
    Path partitionDir = config.basePath().resolve(partition);

    try {
      Files.createDirectories(partitionDir);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to create partition directory: " + partitionDir, e);
    }

    String fileName =
        instanceId
            + "_"
            + System.currentTimeMillis()
            + "_"
            + sequence.incrementAndGet()
            + ".parquet";
    Path filePath = partitionDir.resolve(fileName);

    try {
      parquetWriter.write(spans, filePath);
      LOG.info("Wrote {} spans to {}", spans.size(), filePath);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write Parquet file: " + filePath, e);
    }
  }
}
