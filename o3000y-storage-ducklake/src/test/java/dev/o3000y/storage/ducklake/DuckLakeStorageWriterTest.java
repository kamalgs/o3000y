package dev.o3000y.storage.ducklake;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.testing.fixtures.SpanFixtures;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DuckLakeStorageWriterTest {

  @TempDir Path tempDir;
  private DuckLakeManager manager;
  private DuckLakeStorageWriter writer;

  @BeforeEach
  void setUp() {
    DuckLakeConfig config =
        new DuckLakeConfig(
            tempDir.resolve("metadata.ducklake").toString(),
            tempDir.resolve("files").toString() + "/");
    manager = new DuckLakeManager(config);
    writer = new DuckLakeStorageWriter(manager);
  }

  @AfterEach
  void tearDown() {
    manager.close();
  }

  @Test
  void writeSpans_immediatelyQueryable() throws Exception {
    var spans = SpanFixtures.aTrace(3);
    String traceId = spans.getFirst().traceId();
    writer.write(spans);

    // Query from a separate connection
    try (Connection conn = manager.newConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs =
            stmt.executeQuery("SELECT count(*) FROM spans WHERE trace_id = '" + traceId + "'")) {
      assertTrue(rs.next());
      assertEquals(3L, rs.getLong(1));
    }
  }

  @Test
  void writeSpans_allFieldsCorrect() throws Exception {
    var spans = SpanFixtures.aTrace(1);
    writer.write(spans);

    try (Connection conn = manager.newConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs =
            stmt.executeQuery(
                "SELECT trace_id, span_id, operation_name, service_name, duration_us, "
                    + "status_code, span_kind, attributes FROM spans")) {
      assertTrue(rs.next());
      assertNotNull(rs.getString("trace_id"));
      assertNotNull(rs.getString("span_id"));
      assertNotNull(rs.getString("operation_name"));
      assertNotNull(rs.getString("service_name"));
      assertTrue(rs.getLong("duration_us") > 0);
      assertEquals(1, rs.getInt("status_code")); // OK
      assertEquals(2, rs.getInt("span_kind")); // SERVER
      assertTrue(rs.getString("attributes").contains("test.key"));
    }
  }

  @Test
  void writeEmptyBatch_doesNothing() {
    writer.write(List.of());
    // Should not throw
  }

  @Test
  void concurrentWrites_noDataLoss() throws Exception {
    int threadCount = 4;
    int spansPerThread = 10;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Future<?>> futures = new ArrayList<>();

    for (int t = 0; t < threadCount; t++) {
      futures.add(
          executor.submit(
              () -> {
                DuckLakeStorageWriter threadWriter = new DuckLakeStorageWriter(manager);
                for (int i = 0; i < spansPerThread; i++) {
                  threadWriter.write(SpanFixtures.aTrace(1));
                }
              }));
    }

    for (Future<?> f : futures) {
      f.get();
    }
    executor.shutdown();

    try (Connection conn = manager.newConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM spans")) {
      assertTrue(rs.next());
      assertEquals(threadCount * spansPerThread, rs.getLong(1));
    }
  }
}
