package dev.o3000y.query.engine;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.testing.fixtures.ParquetTestHelper;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DuckDbQueryEngineTest {

  @TempDir Path tempDir;
  private DuckDbQueryEngine engine;

  @BeforeEach
  void setUp() throws Exception {
    ParquetTestHelper helper = new ParquetTestHelper();
    helper.writeSpansWithHivePartitioning(tempDir, SpanFixtures.aTrace(3));

    engine = new DuckDbQueryEngine(QueryConfig.defaults(tempDir));
    engine.refreshView();
  }

  @AfterEach
  void tearDown() {
    engine.close();
  }

  @Test
  void selectCount() {
    QueryResult result = engine.executeQuery("SELECT count(*) as cnt FROM spans");
    assertEquals(1, result.rows().size());
    assertEquals(3L, ((Number) result.rows().getFirst().getFirst()).longValue());
  }

  @Test
  void selectColumns() {
    QueryResult result =
        engine.executeQuery("SELECT trace_id, operation_name, service_name FROM spans LIMIT 1");
    assertTrue(result.columns().contains("trace_id"));
    assertTrue(result.columns().contains("operation_name"));
    assertTrue(result.columns().contains("service_name"));
    assertEquals(1, result.rows().size());
  }

  @Test
  void getTrace() {
    QueryResult idResult = engine.executeQuery("SELECT DISTINCT trace_id FROM spans LIMIT 1");
    String traceId = (String) idResult.rows().getFirst().getFirst();

    QueryResult traceResult = engine.getTrace(traceId);
    assertEquals(3, traceResult.rowCount());
  }

  @Test
  void invalidSql_throwsInvalidQueryException() {
    assertThrows(InvalidQueryException.class, () -> engine.executeQuery("THIS IS NOT VALID SQL"));
  }

  @Test
  void getTrace_invalidTraceId_throwsInvalidQueryException() {
    assertThrows(InvalidQueryException.class, () -> engine.getTrace(""));
  }

  @Test
  void resultSizeLimited() throws Exception {
    // Write additional spans to get more rows
    ParquetTestHelper helper = new ParquetTestHelper();
    helper.writeSpansWithHivePartitioning(tempDir, SpanFixtures.aTrace(5));
    engine.refreshView();

    // Use a config with maxResultRows=2
    DuckDbQueryEngine limitedEngine =
        new DuckDbQueryEngine(new QueryConfig(tempDir, 2, Duration.ofSeconds(60)));
    limitedEngine.refreshView();
    try {
      QueryResult result = limitedEngine.executeQuery("SELECT * FROM spans");
      assertTrue(result.rowCount() <= 2);
    } finally {
      limitedEngine.close();
    }
  }

  @Test
  void refreshView_makesNewDataVisible() throws Exception {
    // Start engine on empty temp dir — no view yet
    DuckDbQueryEngine freshEngine =
        new DuckDbQueryEngine(QueryConfig.defaults(tempDir.resolve("empty")));
    try {
      freshEngine.refreshView();

      // Write data after initial view creation
      ParquetTestHelper helper = new ParquetTestHelper();
      Path dataDir = tempDir.resolve("empty");
      java.nio.file.Files.createDirectories(dataDir);
      helper.writeSpansWithHivePartitioning(dataDir, SpanFixtures.aTrace(5));

      // After refresh, new data is visible
      freshEngine.refreshView();
      QueryResult after = freshEngine.executeQuery("SELECT count(*) as cnt FROM spans");
      assertEquals(5L, ((Number) after.rows().getFirst().getFirst()).longValue());
    } finally {
      freshEngine.close();
    }
  }
}
