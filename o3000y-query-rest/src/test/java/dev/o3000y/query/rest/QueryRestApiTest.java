package dev.o3000y.query.rest;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.QueryConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QueryRestApiTest {

  @TempDir Path tempDir;

  private DuckDbQueryEngine engine;
  private QueryRestApi restApi;
  private int port;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    engine = new DuckDbQueryEngine(QueryConfig.defaults(tempDir));
    // Use port 0 for random available port — but Javalin needs a specific port
    port = 18080 + (int) (Math.random() * 1000);
    restApi = new QueryRestApi(engine, port);
    restApi.start();
  }

  @AfterEach
  void tearDown() {
    restApi.stop();
    engine.close();
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
}
