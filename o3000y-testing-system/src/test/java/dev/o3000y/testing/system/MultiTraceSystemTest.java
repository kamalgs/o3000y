package dev.o3000y.testing.system;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.o3000y.app.IngestionCoreModule;
import dev.o3000y.app.StorageParquetModule;
import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.core.SpanBuffer;
import dev.o3000y.ingestion.grpc.GrpcServer;
import dev.o3000y.ingestion.grpc.IngestionGrpcModule;
import dev.o3000y.model.Span;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.query.rest.QueryModule;
import dev.o3000y.query.rest.QueryRestApi;
import dev.o3000y.storage.api.StorageConfig;
import dev.o3000y.storage.local.LocalStorageModule;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiTraceSystemTest {

  @TempDir Path tempDir;

  private GrpcServer grpcServer;
  private QueryRestApi restApi;
  private DuckDbQueryEngine queryEngine;
  private SpanBuffer spanBuffer;
  private int grpcPort;
  private int restPort;

  @BeforeEach
  void setUp() throws Exception {
    grpcPort = 14317 + (int) (Math.random() * 1000);
    restPort = 18080 + (int) (Math.random() * 1000);

    Injector injector =
        Guice.createInjector(
            new StorageParquetModule(),
            new LocalStorageModule(new StorageConfig(tempDir)),
            new IngestionCoreModule(new BatchConfig(100, Long.MAX_VALUE, Duration.ofHours(1))),
            new IngestionGrpcModule(grpcPort),
            new QueryModule(QueryConfig.defaults(tempDir), restPort));

    grpcServer = injector.getInstance(GrpcServer.class);
    restApi = injector.getInstance(QueryRestApi.class);
    queryEngine = injector.getInstance(DuckDbQueryEngine.class);
    spanBuffer = injector.getInstance(SpanBuffer.class);

    grpcServer.start();
    restApi.start();
  }

  @AfterEach
  void tearDown() {
    spanBuffer.shutdown();
    grpcServer.stop();
    restApi.stop();
    queryEngine.close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void tenTraces_eachRetrievableById() throws Exception {
    List<String> traceIds = new ArrayList<>();

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", grpcPort).usePlaintext().build();
    try {
      var stub = TraceServiceGrpc.newBlockingStub(channel);
      for (int i = 0; i < 10; i++) {
        List<Span> trace = SpanFixtures.aTrace(5);
        traceIds.add(trace.getFirst().traceId());
        stub.export(ProtoFixtures.toExportRequest(trace));
      }
    } finally {
      channel.shutdownNow();
    }

    spanBuffer.flush();
    queryEngine.refreshView();

    HttpClient httpClient = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();

    for (String traceId : traceIds) {
      HttpResponse<String> response =
          httpClient.send(
              HttpRequest.newBuilder()
                  .uri(URI.create("http://localhost:" + restPort + "/api/v1/trace/" + traceId))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());

      assertEquals(200, response.statusCode());
      Map<String, Object> body = mapper.readValue(response.body(), Map.class);
      assertEquals(5, (int) body.get("spanCount"), "Trace " + traceId + " should have 5 spans");
    }
  }
}
