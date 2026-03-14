package dev.o3000y.testing.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.core.SpanBuffer;
import dev.o3000y.model.Span;
import dev.o3000y.testing.fixtures.DuckLakeTestHelper;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IngestionStorageIntegrationTest {

  @TempDir Path tempDir;

  @Test
  void ingestionBuffer_flushesSpans_toDuckLake() throws Exception {
    DuckLakeTestHelper helper = new DuckLakeTestHelper(tempDir);

    BatchConfig batchConfig = new BatchConfig(5, Long.MAX_VALUE, Duration.ofHours(1));
    SpanBuffer buffer = new SpanBuffer(helper.writer(), batchConfig);

    List<Span> trace = SpanFixtures.aTrace(5);
    buffer.receive(trace);
    buffer.shutdown();

    // Verify spans were written to DuckLake
    try (Connection conn = helper.manager().newConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM spans")) {
      assertTrue(rs.next());
      assertTrue(rs.getLong(1) >= 5, "Expected at least 5 spans in DuckLake");
    }

    helper.close();
  }
}
