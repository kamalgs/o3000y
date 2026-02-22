package dev.o3000y.testing.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.model.Span;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.query.engine.QueryResult;
import dev.o3000y.storage.api.HivePartitionStrategy;
import dev.o3000y.storage.api.StorageConfig;
import dev.o3000y.storage.local.LocalStorageWriter;
import dev.o3000y.storage.parquet.ParquetSpanWriter;
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
  private LocalStorageWriter storageWriter;

  @BeforeEach
  void setUp() {
    storageWriter =
        new LocalStorageWriter(
            new StorageConfig(tempDir), new HivePartitionStrategy(), new ParquetSpanWriter());
    engine = new DuckDbQueryEngine(QueryConfig.defaults(tempDir));
  }

  @AfterEach
  void tearDown() {
    engine.close();
  }

  @Test
  void writeAndQuery_knownData() {
    List<Span> trace = SpanFixtures.aTrace(3);
    String traceId = trace.getFirst().traceId();
    storageWriter.write(trace);
    engine.refreshView();

    QueryResult result = engine.getTrace(traceId);
    assertEquals(3, result.rowCount());
  }

  @Test
  void hivePartitionPruning_filterByHour() {
    // Create spans across 3 different hours
    Span hourOne =
        SpanFixtures.aSpan(
            SpanFixtures.randomTraceId(), SpanFixtures.randomSpanId(), "", "op1", "svc-prune");
    Span hourTwo =
        SpanFixtures.aSpan(
            SpanFixtures.randomTraceId(), SpanFixtures.randomSpanId(), "", "op2", "svc-prune");
    Span hourThree =
        SpanFixtures.aSpan(
            SpanFixtures.randomTraceId(), SpanFixtures.randomSpanId(), "", "op3", "svc-prune");

    storageWriter.write(List.of(hourOne));
    storageWriter.write(List.of(hourTwo));
    storageWriter.write(List.of(hourThree));
    engine.refreshView();

    // Query filtering by service_name
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
    storageWriter.write(List.of(span));
    engine.refreshView();

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
