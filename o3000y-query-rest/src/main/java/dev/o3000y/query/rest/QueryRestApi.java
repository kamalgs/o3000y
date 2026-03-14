package dev.o3000y.query.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.o3000y.loadgen.LoadGenService;
import dev.o3000y.model.PipelineMetrics;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.InvalidQueryException;
import dev.o3000y.query.engine.QueryResult;
import dev.o3000y.query.engine.QueryTimeoutException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QueryRestApi {

  private static final Logger LOG = LoggerFactory.getLogger(QueryRestApi.class);

  private final DuckDbQueryEngine queryEngine;
  private final PipelineMetrics metrics;
  private final LoadGenService loadGenService;
  private final int port;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private Javalin app;

  public QueryRestApi(
      DuckDbQueryEngine queryEngine,
      PipelineMetrics metrics,
      LoadGenService loadGenService,
      int port) {
    this.queryEngine = queryEngine;
    this.metrics = metrics;
    this.loadGenService = loadGenService;
    this.port = port;
  }

  public QueryRestApi(DuckDbQueryEngine queryEngine, int port) {
    this(queryEngine, new PipelineMetrics(), null, port);
  }

  public void start() {
    app =
        Javalin.create(
                config -> {
                  config.staticFiles.add(
                      staticFiles -> {
                        staticFiles.directory = "/static";
                        staticFiles.hostedPath = "/";
                      });
                })
            .start(port);

    app.get("/health", this::handleHealth);
    app.post("/api/v1/query", this::handleQuery);
    app.get("/api/v1/trace/{trace_id}", this::handleGetTrace);
    app.get("/api/v1/services", this::handleGetServices);
    app.get("/api/v1/operations", this::handleGetOperations);
    app.get("/api/v1/search", this::handleSearch);
    app.get("/api/v1/metrics", this::handleMetrics);

    if (loadGenService != null) {
      app.post("/api/v1/loadgen/start", this::handleLoadGenStart);
      app.post("/api/v1/loadgen/stop", this::handleLoadGenStop);
      app.get("/api/v1/loadgen/status", this::handleLoadGenStatus);
    }

    LOG.info("REST API started on port {}", port);
  }

  public int getPort() {
    return port;
  }

  private void handleHealth(Context ctx) {
    ctx.json(Map.of("status", "up"));
  }

  private void handleQuery(Context ctx) {
    try {
      QueryRequest request = ctx.bodyAsClass(QueryRequest.class);
      if (request.sql() == null || request.sql().isBlank()) {
        ctx.status(400).json(new ErrorResponse("SQL query is required", 400));
        return;
      }
      QueryResult result = queryEngine.executeQuery(request.sql());
      ctx.json(
          new QueryResponse(
              result.columns(), result.rows(), result.rowCount(), result.elapsedMs()));
    } catch (QueryTimeoutException e) {
      LOG.warn("Query timeout: {}", e.getMessage());
      ctx.status(408).json(new ErrorResponse(e.getMessage(), 408));
    } catch (InvalidQueryException e) {
      LOG.warn("Invalid query: {}", e.getMessage());
      ctx.status(400).json(new ErrorResponse(e.getMessage(), 400));
    } catch (Exception e) {
      LOG.error("Query failed", e);
      ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
    }
  }

  private void handleGetTrace(Context ctx) {
    try {
      String traceId = ctx.pathParam("trace_id");
      QueryResult result = queryEngine.getTrace(traceId);
      TraceResponse traceResponse = toTraceResponse(traceId, result);
      ctx.json(traceResponse);
    } catch (InvalidQueryException e) {
      LOG.warn("Invalid trace query: {}", e.getMessage());
      ctx.status(400).json(new ErrorResponse(e.getMessage(), 400));
    } catch (QueryTimeoutException e) {
      LOG.warn("Trace query timeout: {}", e.getMessage());
      ctx.status(408).json(new ErrorResponse(e.getMessage(), 408));
    } catch (Exception e) {
      LOG.error("Get trace failed", e);
      ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
    }
  }

  private TraceResponse toTraceResponse(String traceId, QueryResult result) {
    List<SpanResponse> spans = new ArrayList<>();
    Map<String, Integer> colIndex = new HashMap<>();
    for (int i = 0; i < result.columns().size(); i++) {
      colIndex.put(result.columns().get(i), i);
    }

    for (List<Object> row : result.rows()) {
      spans.add(
          new SpanResponse(
              getString(row, colIndex, "trace_id"),
              getString(row, colIndex, "span_id"),
              getString(row, colIndex, "parent_span_id"),
              getString(row, colIndex, "operation_name"),
              getString(row, colIndex, "service_name"),
              getString(row, colIndex, "start_time"),
              getString(row, colIndex, "end_time"),
              getLong(row, colIndex, "duration_us"),
              getInt(row, colIndex, "status_code"),
              getString(row, colIndex, "status_message"),
              getInt(row, colIndex, "span_kind"),
              parseAttributes(getString(row, colIndex, "attributes"))));
    }

    return new TraceResponse(traceId, spans.size(), spans);
  }

  private static String getString(List<Object> row, Map<String, Integer> colIndex, String col) {
    Integer idx = colIndex.get(col);
    if (idx == null || idx >= row.size()) return "";
    Object val = row.get(idx);
    return val == null ? "" : val.toString();
  }

  private static long getLong(List<Object> row, Map<String, Integer> colIndex, String col) {
    Integer idx = colIndex.get(col);
    if (idx == null || idx >= row.size()) return 0;
    Object val = row.get(idx);
    return val instanceof Number n ? n.longValue() : 0;
  }

  private static int getInt(List<Object> row, Map<String, Integer> colIndex, String col) {
    Integer idx = colIndex.get(col);
    if (idx == null || idx >= row.size()) return 0;
    Object val = row.get(idx);
    return val instanceof Number n ? n.intValue() : 0;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> parseAttributes(String json) {
    if (json == null || json.isEmpty() || json.equals("{}")) {
      return Map.of();
    }
    try {
      Map<String, Object> raw = objectMapper.readValue(json, Map.class);
      Map<String, String> result = new HashMap<>();
      for (Map.Entry<String, Object> entry : raw.entrySet()) {
        result.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
      }
      return result;
    } catch (JsonProcessingException e) {
      LOG.debug("Failed to parse attributes JSON: {}", json);
      return Map.of();
    }
  }

  private void handleGetServices(Context ctx) {
    try {
      QueryResult result =
          queryEngine.executeQuery("SELECT DISTINCT service_name FROM spans ORDER BY service_name");
      List<String> services = new ArrayList<>();
      for (List<Object> row : result.rows()) {
        if (!row.isEmpty() && row.getFirst() != null) {
          services.add(row.getFirst().toString());
        }
      }
      ctx.json(services);
    } catch (Exception e) {
      LOG.error("Get services failed", e);
      ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
    }
  }

  private void handleGetOperations(Context ctx) {
    try {
      String service = ctx.queryParam("service");
      String sql;
      if (service != null && !service.isBlank()) {
        String sanitized = service.replace("'", "''");
        sql =
            "SELECT DISTINCT operation_name FROM spans WHERE service_name = '"
                + sanitized
                + "' ORDER BY operation_name";
      } else {
        sql = "SELECT DISTINCT operation_name FROM spans ORDER BY operation_name";
      }
      QueryResult result = queryEngine.executeQuery(sql);
      List<String> operations = new ArrayList<>();
      for (List<Object> row : result.rows()) {
        if (!row.isEmpty() && row.getFirst() != null) {
          operations.add(row.getFirst().toString());
        }
      }
      ctx.json(operations);
    } catch (Exception e) {
      LOG.error("Get operations failed", e);
      ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
    }
  }

  private void handleSearch(Context ctx) {
    try {
      List<String> conditions = new ArrayList<>();
      String service = ctx.queryParam("service");
      if (service != null && !service.isBlank()) {
        conditions.add("service_name = '" + service.replace("'", "''") + "'");
      }
      String operation = ctx.queryParam("operation");
      if (operation != null && !operation.isBlank()) {
        conditions.add("operation_name = '" + operation.replace("'", "''") + "'");
      }
      String minDuration = ctx.queryParam("minDuration");
      if (minDuration != null && !minDuration.isBlank()) {
        conditions.add("duration_us >= " + Long.parseLong(minDuration));
      }
      String maxDuration = ctx.queryParam("maxDuration");
      if (maxDuration != null && !maxDuration.isBlank()) {
        conditions.add("duration_us <= " + Long.parseLong(maxDuration));
      }
      String status = ctx.queryParam("status");
      if (status != null && !status.isBlank()) {
        int statusCode = "ERROR".equalsIgnoreCase(status) ? 2 : 1;
        conditions.add("status_code = " + statusCode);
      }
      int limit = 100;
      String limitStr = ctx.queryParam("limit");
      if (limitStr != null && !limitStr.isBlank()) {
        limit = Math.min(Integer.parseInt(limitStr), 10_000);
      }

      StringBuilder sql = new StringBuilder("SELECT * FROM spans");
      if (!conditions.isEmpty()) {
        sql.append(" WHERE ").append(String.join(" AND ", conditions));
      }
      sql.append(" ORDER BY start_time DESC LIMIT ").append(limit);

      QueryResult result = queryEngine.executeQuery(sql.toString());
      ctx.json(
          new QueryResponse(
              result.columns(), result.rows(), result.rowCount(), result.elapsedMs()));
    } catch (NumberFormatException e) {
      ctx.status(400).json(new ErrorResponse("Invalid numeric parameter: " + e.getMessage(), 400));
    } catch (Exception e) {
      LOG.error("Search failed", e);
      ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage(), 500));
    }
  }

  private void handleMetrics(Context ctx) {
    ctx.contentType("text/plain; version=0.0.4; charset=utf-8").result(metrics.toPrometheus());
  }

  @SuppressWarnings("unchecked")
  private void handleLoadGenStart(Context ctx) {
    try {
      Map<String, Object> body = objectMapper.readValue(ctx.body(), Map.class);
      int duration = ((Number) body.getOrDefault("durationSeconds", 60)).intValue();
      double tps = ((Number) body.getOrDefault("tracesPerSecond", 10.0)).doubleValue();
      double errRate = ((Number) body.getOrDefault("errorRate", 0.05)).doubleValue();
      int depth = ((Number) body.getOrDefault("maxDepth", 5)).intValue();
      int breadth = ((Number) body.getOrDefault("maxBreadth", 3)).intValue();
      ctx.json(loadGenService.start(duration, tps, errRate, depth, breadth));
    } catch (Exception e) {
      ctx.status(400).json(new ErrorResponse("Invalid request: " + e.getMessage(), 400));
    }
  }

  private void handleLoadGenStop(Context ctx) {
    ctx.json(loadGenService.stop());
  }

  private void handleLoadGenStatus(Context ctx) {
    ctx.json(loadGenService.status());
  }

  public void stop() {
    if (app != null) {
      app.stop();
    }
  }
}
