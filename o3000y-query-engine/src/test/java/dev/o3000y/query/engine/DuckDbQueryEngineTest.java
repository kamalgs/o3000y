package dev.o3000y.query.engine;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.testing.fixtures.ParquetTestHelper;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.nio.file.Path;
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
}
