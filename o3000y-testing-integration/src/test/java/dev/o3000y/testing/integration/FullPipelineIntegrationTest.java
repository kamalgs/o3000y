package dev.o3000y.testing.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.core.SpanBuffer;
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
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FullPipelineIntegrationTest {

  @TempDir Path tempDir;
  private DuckDbQueryEngine engine;

  @AfterEach
  void tearDown() {
    if (engine != null) engine.close();
  }

  @Test
  void receive_flush_refreshView_query() {
    // Storage layer
    LocalStorageWriter storageWriter =
        new LocalStorageWriter(
            new StorageConfig(tempDir), new HivePartitionStrategy(), new ParquetSpanWriter());

    // Ingestion layer
    BatchConfig batchConfig = new BatchConfig(100, Long.MAX_VALUE, Duration.ofHours(1));
    SpanBuffer buffer = new SpanBuffer(storageWriter, batchConfig);

    // Query layer
    engine = new DuckDbQueryEngine(QueryConfig.defaults(tempDir));

    // Send spans through buffer
    List<Span> trace = SpanFixtures.aTrace(5);
    String traceId = trace.getFirst().traceId();
    buffer.receive(trace);
    buffer.flush();

    // Refresh query view and verify
    engine.refreshView();
    QueryResult result = engine.getTrace(traceId);
    assertEquals(5, result.rowCount());

    // Verify count query
    QueryResult countResult = engine.executeQuery("SELECT count(*) as cnt FROM spans");
    assertEquals(5L, ((Number) countResult.rows().getFirst().getFirst()).longValue());

    // Verify metrics
    assertEquals(5, buffer.getTotalSpansReceived());
    assertEquals(1, buffer.getTotalFlushes());

    buffer.shutdown();
  }

  @Test
  void multipleBatches_allVisible() {
    LocalStorageWriter storageWriter =
        new LocalStorageWriter(
            new StorageConfig(tempDir), new HivePartitionStrategy(), new ParquetSpanWriter());

    BatchConfig batchConfig = new BatchConfig(100, Long.MAX_VALUE, Duration.ofHours(1));
    SpanBuffer buffer = new SpanBuffer(storageWriter, batchConfig);
    engine = new DuckDbQueryEngine(QueryConfig.defaults(tempDir));

    // Send 3 separate batches
    buffer.receive(SpanFixtures.aTrace(3));
    buffer.receive(SpanFixtures.aTrace(4));
    buffer.receive(SpanFixtures.aTrace(2));
    buffer.flush();

    engine.refreshView();
    QueryResult result = engine.executeQuery("SELECT count(*) as cnt FROM spans");
    assertEquals(9L, ((Number) result.rows().getFirst().getFirst()).longValue());

    buffer.shutdown();
  }
}
