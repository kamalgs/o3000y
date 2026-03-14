package dev.o3000y.query.rest;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.testing.fixtures.DuckLakeTestHelper;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QueryRestApiTest {

  @TempDir Path tempDir;

  private DuckLakeTestHelper helper;
  private DuckDbQueryEngine engine;
  private QueryRestApi restApi;
  private int port;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    helper = new DuckLakeTestHelper(tempDir);
    engine = new DuckDbQueryEngine(QueryConfig.defaults(), helper.manager().newConnection());
    port = 18080 + (int) (Math.random() * 1000);
    restApi = new QueryRestApi(engine, port);
    restApi.start();
  }

  @AfterEach
  void tearDown() {
    restApi.stop();
    engine.close();
    helper.close();
  }

  @Test
  void postQuery_returnsResult() throws Exception {
    String body = objectMapper.writeValueAsString(new QueryRequest("SELECT 1 as value"));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/query"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());

    QueryResponse queryResponse = objectMapper.readValue(response.body(), QueryResponse.class);
    assertEquals(1, queryResponse.rowCount());
    assertTrue(queryResponse.columns().contains("value"));
  }

  @Test
  void postQuery_emptySql_returns400() throws Exception {
    String body = objectMapper.writeValueAsString(new QueryRequest(""));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/query"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(400, response.statusCode());
  }

  @Test
  void postQuery_invalidSql_returns400() throws Exception {
    String body = objectMapper.writeValueAsString(new QueryRequest("NOT VALID SQL"));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/query"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(400, response.statusCode());
  }

  @Test
  void health_returnsUp() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/health"))
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("up"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getTrace_returnsStructuredResponse() throws Exception {
    var spans = SpanFixtures.aTrace(3);
    String traceId = spans.getFirst().traceId();
    helper.writer().write(spans);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/trace/" + traceId))
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());

    Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
    assertEquals(traceId, body.get("traceId"));
    assertEquals(3, (int) body.get("spanCount"));
    List<Map<String, Object>> spanList = (List<Map<String, Object>>) body.get("spans");
    assertEquals(3, spanList.size());

    Map<String, Object> firstSpan = spanList.getFirst();
    assertNotNull(firstSpan.get("spanId"));
    assertNotNull(firstSpan.get("operationName"));
    assertNotNull(firstSpan.get("serviceName"));
  }

  @Test
  void getTrace_purelyInvalidId_returns400() throws Exception {
    // Use chars that are ALL non-hex to ensure sanitized ID is empty
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/trace/!!!---!!!"))
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(400, response.statusCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getTrace_nonexistentId_returnsEmptyTrace() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/trace/deadbeef00000000"))
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());

    Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
    assertEquals(0, (int) body.get("spanCount"));
  }

  @Test
  void metrics_returnsPrometheusFormat() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/v1/metrics"))
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/plain"));
    String body = response.body();
    assertTrue(body.contains("# HELP o3000y_uptime_seconds"));
    assertTrue(body.contains("# TYPE o3000y_uptime_seconds gauge"));
    assertTrue(body.contains("o3000y_ingestion_spans_received_total"));
    assertTrue(body.contains("o3000y_ingestion_delay_seconds"));
    assertTrue(body.contains("o3000y_ingestion_delay_seconds_bucket{le="));
    assertTrue(body.contains("o3000y_ingestion_delay_seconds_sum"));
    assertTrue(body.contains("o3000y_ingestion_delay_seconds_count"));
    assertTrue(body.contains("o3000y_storage_writes_total"));
    assertTrue(body.contains("o3000y_query_total"));
  }
}
