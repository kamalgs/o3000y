package dev.o3000y.storage.local;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.model.*;
import dev.o3000y.storage.api.HivePartitionStrategy;
import dev.o3000y.storage.api.StorageConfig;
import dev.o3000y.storage.parquet.ParquetSpanWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiPartitionWriteTest {

  @TempDir Path tempDir;

  @Test
  void spansSpanningTwoHours_createTwoPartitions() {
    ParquetSpanWriter parquetWriter = new ParquetSpanWriter();
    LocalStorageWriter writer =
        new LocalStorageWriter(
            new StorageConfig(tempDir), new HivePartitionStrategy(), parquetWriter);

    Span hourOne =
        new Span(
            "trace1",
            "span1",
            "",
            "op1",
            "svc",
            Instant.parse("2026-02-09T14:30:00Z"),
            Instant.parse("2026-02-09T14:30:01Z"),
            1_000_000L,
            StatusCode.OK,
            "",
            SpanKind.SERVER,
            Map.of(),
            List.of(),
            List.of());

    Span hourTwo =
        new Span(
            "trace1",
            "span2",
            "span1",
            "op2",
            "svc",
            Instant.parse("2026-02-09T15:00:00Z"),
            Instant.parse("2026-02-09T15:00:01Z"),
            1_000_000L,
            StatusCode.OK,
            "",
            SpanKind.SERVER,
            Map.of(),
            List.of(),
            List.of());

    writer.write(List.of(hourOne, hourTwo));

    assertTrue(Files.exists(tempDir.resolve("year=2026/month=02/day=09/hour=14")));
    assertTrue(Files.exists(tempDir.resolve("year=2026/month=02/day=09/hour=15")));
  }
}
