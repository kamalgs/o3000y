package dev.o3000y.testing.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.model.Span;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.query.engine.QueryResult;
import dev.o3000y.testing.fixtures.DuckLakeTestHelper;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageQueryIntegrationTest {

  @TempDir Path tempDir;
  private DuckDbQueryEngine engine;
  private DuckLakeTestHelper helper;

  @BeforeEach
  void setUp() {
    helper = new DuckLakeTestHelper(tempDir);
    engine = new DuckDbQueryEngine(QueryConfig.defaults(), helper.manager().newConnection());
  }

  @AfterEach
  void tearDown() {
    engine.close();
    helper.close();
  }

  @Test
  void writeAndQuery_knownData() {
    List<Span> trace = SpanFixtures.aTrace(3);
    String traceId = trace.getFirst().traceId();
    helper.writer().write(trace);

    QueryResult result = engine.getTrace(traceId);
    assertEquals(3, result.rowCount());
  }

  @Test
  void filterByServiceName() {
    Span span1 =
        SpanFixtures.aSpan(
            SpanFixtures.randomTraceId(), SpanFixtures.randomSpanId(), "", "op1", "svc-prune");
    Span span2 =
        SpanFixtures.aSpan(
            SpanFixtures.randomTraceId(), SpanFixtures.randomSpanId(), "", "op2", "svc-prune");
    Span span3 =
        SpanFixtures.aSpan(
            SpanFixtures.randomTraceId(), SpanFixtures.randomSpanId(), "", "op3", "svc-prune");

    helper.writer().write(List.of(span1));
    helper.writer().write(List.of(span2));
    helper.writer().write(List.of(span3));

    QueryResult result =
        engine.executeQuery("SELECT count(*) as cnt FROM spans WHERE service_name = 'svc-prune'");
    assertEquals(1, result.rows().size());
    long count = ((Number) result.rows().getFirst().getFirst()).longValue();
    assertEquals(3L, count);
  }

  @Test
  void queryReturnsCorrectColumnValues() {
    Span span =
        SpanFixtures.aSpan(
            "aabbccdd11223344", SpanFixtures.randomSpanId(), "", "test-op", "test-svc");
    helper.writer().write(List.of(span));

    QueryResult result =
        engine.executeQuery(
            "SELECT trace_id, operation_name, service_name FROM spans WHERE trace_id = 'aabbccdd11223344'");
    assertEquals(1, result.rowCount());
    List<Object> row = result.rows().getFirst();
    assertEquals("aabbccdd11223344", row.get(0));
    assertEquals("test-op", row.get(1));
    assertEquals("test-svc", row.get(2));
  }
}
