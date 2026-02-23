package dev.o3000y.storage.parquet;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.model.*;
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

class ParquetSpanWriterTest {

  @TempDir Path tempDir;

  @Test
  void writeAndReadBack() throws Exception {
    ParquetSpanWriter writer = new ParquetSpanWriter();

    Span span =
        new Span(
            "0af7651916cd43dd8448eb211c80319c",
            "00f067aa0ba902b7",
            "",
            "GET /api/users",
            "user-service",
            Instant.parse("2026-02-09T14:00:00Z"),
            Instant.parse("2026-02-09T14:00:00.100Z"),
            100_000L,
            StatusCode.OK,
            "",
            SpanKind.SERVER,
            Map.of("http.method", "GET"),
            List.of(),
            List.of());

    Path parquetFile = tempDir.resolve("test.parquet");
    writer.write(List.of(span), parquetFile);

    assertTrue(parquetFile.toFile().exists());

    // Read back with DuckDB
    try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
        Statement stmt = conn.createStatement()) {
      ResultSet rs =
          stmt.executeQuery(
              "SELECT trace_id, span_id, operation_name, service_name, duration_us, status_code, span_kind "
                  + "FROM read_parquet('"
                  + parquetFile
                  + "')");
      assertTrue(rs.next());
      assertEquals("0af7651916cd43dd8448eb211c80319c", rs.getString("trace_id"));
      assertEquals("00f067aa0ba902b7", rs.getString("span_id"));
      assertEquals("GET /api/users", rs.getString("operation_name"));
      assertEquals("user-service", rs.getString("service_name"));
      assertEquals(100_000L, rs.getLong("duration_us"));
      assertEquals(1, rs.getInt("status_code"));
      assertEquals(2, rs.getInt("span_kind"));
      assertFalse(rs.next());
    }
  }
}
