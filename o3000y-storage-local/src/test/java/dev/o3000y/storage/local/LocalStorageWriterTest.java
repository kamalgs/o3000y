package dev.o3000y.storage.local;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.model.*;
import dev.o3000y.storage.api.HivePartitionStrategy;
import dev.o3000y.storage.api.StorageConfig;
import dev.o3000y.storage.parquet.ParquetSpanWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStorageWriterTest {

  @TempDir Path tempDir;

  @Test
  void writeBatch_createsParquetInPartitionDir() throws Exception {
    ParquetSpanWriter parquetWriter = new ParquetSpanWriter();
    LocalStorageWriter writer =
        new LocalStorageWriter(
            new StorageConfig(tempDir), new HivePartitionStrategy(), parquetWriter);

    Span span =
        new Span(
            "0af7651916cd43dd8448eb211c80319c",
            "00f067aa0ba902b7",
            "",
            "GET /api/users",
            "user-service",
            Instant.parse("2026-02-09T14:30:00Z"),
            Instant.parse("2026-02-09T14:30:00.100Z"),
            100_000L,
            StatusCode.OK,
            "",
            SpanKind.SERVER,
            Map.of(),
            List.of(),
            List.of());

    writer.write(List.of(span));

    Path partitionDir = tempDir.resolve("year=2026/month=02/day=09/hour=14");
    assertTrue(Files.exists(partitionDir));

    Path[] files =
        Files.list(partitionDir)
            .filter(p -> p.toString().endsWith(".parquet"))
            .toArray(Path[]::new);
    assertEquals(1, files.length);

    // Verify contents via DuckDB
    try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
        Statement stmt = conn.createStatement()) {
      ResultSet rs =
          stmt.executeQuery("SELECT trace_id, service_name FROM read_parquet('" + files[0] + "')");
      assertTrue(rs.next());
      assertEquals("0af7651916cd43dd8448eb211c80319c", rs.getString("trace_id"));
      assertEquals("user-service", rs.getString("service_name"));
    }
  }
}
