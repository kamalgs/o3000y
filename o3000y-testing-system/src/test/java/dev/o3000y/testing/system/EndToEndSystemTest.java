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
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.query.rest.QueryModule;
import dev.o3000y.query.rest.QueryRestApi;
import dev.o3000y.storage.ducklake.DuckLakeConfig;
import dev.o3000y.storage.ducklake.DuckLakeModule;
import dev.o3000y.testing.fixtures.ProtoFixtures;
import dev.o3000y.testing.fixtures.SpanFixtures;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndToEndSystemTest {

  @TempDir Path tempDir;

  private Injector injector;
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
    BatchConfig batchConfig = new BatchConfig(100, Long.MAX_VALUE, Duration.ofHours(1));

    injector =
        Guice.createInjector(
            new DuckLakeModule(duckLakeConfig),
            new IngestionCoreModule(batchConfig),
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
  void fullRoundTrip_ingestViaGrpc_queryViaRest() throws Exception {
    var spans = SpanFixtures.aTrace(3);
    String traceId = spans.getFirst().traceId();

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", grpcPort).usePlaintext().build();
    try {
      TraceServiceGrpc.TraceServiceBlockingStub stub = TraceServiceGrpc.newBlockingStub(channel);
      ExportTraceServiceRequest request = ProtoFixtures.toExportRequest(spans);
      stub.export(request);
    } finally {
      channel.shutdownNow();
    }

    spanBuffer.flush();

    // No refresh needed — data visible immediately
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpResponse<String> response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + restPort + "/api/v1/trace/" + traceId))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> body = mapper.readValue(response.body(), Map.class);
    assertEquals(3, (int) body.get("spanCount"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void queryEndpoint_sqlQuery() throws Exception {
    var spans = SpanFixtures.aTrace(3);
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", grpcPort).usePlaintext().build();
    try {
      TraceServiceGrpc.TraceServiceBlockingStub stub = TraceServiceGrpc.newBlockingStub(channel);
      stub.export(ProtoFixtures.toExportRequest(spans));
    } finally {
      channel.shutdownNow();
    }

    spanBuffer.flush();

    HttpClient httpClient = HttpClient.newHttpClient();
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

    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> body = mapper.readValue(response.body(), Map.class);
    List<List<Object>> rows = (List<List<Object>>) body.get("rows");
    assertFalse(rows.isEmpty());
    assertTrue(((Number) rows.getFirst().getFirst()).intValue() > 0);
  }
}
