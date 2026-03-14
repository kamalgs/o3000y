package dev.o3000y.testing.system;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.o3000y.app.IngestionCoreModule;
import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.core.SpanBuffer;
import dev.o3000y.ingestion.grpc.GrpcServer;
import dev.o3000y.ingestion.grpc.IngestionGrpcModule;
import dev.o3000y.model.Span;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.query.rest.QueryModule;
import dev.o3000y.query.rest.QueryRestApi;
import dev.o3000y.storage.ducklake.DuckLakeConfig;
import dev.o3000y.storage.ducklake.DuckLakeModule;
import dev.o3000y.testing.fixtures.ProtoFixtures;
import dev.o3000y.testing.fixtures.SpanFixtures;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConcurrentIngestionSystemTest {

  @TempDir Path tempDir;

  private GrpcServer grpcServer;
  private QueryRestApi restApi;
  private SpanBuffer spanBuffer;
  private int grpcPort;
  private int restPort;

  @BeforeEach
  void setUp() throws Exception {
    grpcPort = 14317 + (int) (Math.random() * 1000);
    restPort = 18080 + (int) (Math.random() * 1000);

    DuckLakeConfig duckLakeConfig =
        new DuckLakeConfig(
            tempDir.resolve("metadata.ducklake").toString(),
            tempDir.resolve("files").toString() + "/");

    Injector injector =
        Guice.createInjector(
            new DuckLakeModule(duckLakeConfig),
            new IngestionCoreModule(new BatchConfig(1000, Long.MAX_VALUE, Duration.ofHours(1))),
            new IngestionGrpcModule(grpcPort),
            new QueryModule(QueryConfig.defaults(), restPort));

    grpcServer = injector.getInstance(GrpcServer.class);
    restApi = injector.getInstance(QueryRestApi.class);
    spanBuffer = injector.getInstance(SpanBuffer.class);

    grpcServer.start();
    restApi.start();
  }

  @AfterEach
  void tearDown() {
    spanBuffer.shutdown();
    grpcServer.stop();
    restApi.stop();
  }

  @Test
  @SuppressWarnings("unchecked")
  void concurrentIngestion_noDataLoss() throws Exception {
    int threadCount = 5;
    int tracesPerThread = 20;
    int spansPerTrace = 3;
    int expectedTotalSpans = threadCount * tracesPerThread * spansPerTrace;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Future<?>> futures = new ArrayList<>();

    for (int t = 0; t < threadCount; t++) {
      futures.add(
          executor.submit(
              () -> {
                ManagedChannel channel =
                    ManagedChannelBuilder.forAddress("localhost", grpcPort).usePlaintext().build();
                try {
                  var stub = TraceServiceGrpc.newBlockingStub(channel);
                  for (int i = 0; i < tracesPerThread; i++) {
                    List<Span> trace = SpanFixtures.aTrace(spansPerTrace);
                    stub.export(ProtoFixtures.toExportRequest(trace));
                  }
                } finally {
                  channel.shutdownNow();
                }
              }));
    }

    for (Future<?> f : futures) {
      f.get();
    }
    executor.shutdown();

    spanBuffer.flush();

    HttpClient httpClient = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();

    String sqlBody = "{\"sql\": \"SELECT count(*) as cnt FROM spans\"}";
    HttpResponse<String> response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + restPort + "/api/v1/query"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sqlBody))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    Map<String, Object> body = mapper.readValue(response.body(), Map.class);
    List<List<Object>> rows = (List<List<Object>>) body.get("rows");
    int totalSpans = ((Number) rows.getFirst().getFirst()).intValue();
    assertEquals(
        expectedTotalSpans,
        totalSpans,
        "Expected " + expectedTotalSpans + " spans, but found " + totalSpans);
  }
}
