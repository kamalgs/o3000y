package dev.o3000y.query.engine;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.testing.fixtures.DuckLakeTestHelper;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DuckDbQueryEngineTest {

  @TempDir Path tempDir;
  private DuckLakeTestHelper helper;
  private DuckDbQueryEngine engine;

  @BeforeEach
  void setUp() {
    helper = new DuckLakeTestHelper(tempDir);
    helper.writer().write(SpanFixtures.aTrace(3));
    engine = new DuckDbQueryEngine(QueryConfig.defaults(), helper.manager().newConnection());
  }

  @AfterEach
  void tearDown() {
    engine.close();
    helper.close();
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
  void resultSizeLimited() {
    helper.writer().write(SpanFixtures.aTrace(5));

    DuckDbQueryEngine limitedEngine =
        new DuckDbQueryEngine(
            new QueryConfig(2, Duration.ofSeconds(60)), helper.manager().newConnection());
    try {
      QueryResult result = limitedEngine.executeQuery("SELECT * FROM spans");
      assertTrue(result.rowCount() <= 2);
    } finally {
      limitedEngine.close();
    }
  }

  @Test
  void newDataImmediatelyVisible() {
    // Write additional data
    helper.writer().write(SpanFixtures.aTrace(5));

    // No refresh needed — data is visible immediately
    QueryResult result = engine.executeQuery("SELECT count(*) as cnt FROM spans");
    assertEquals(8L, ((Number) result.rows().getFirst().getFirst()).longValue());
  }
}
