package dev.o3000y.query.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final int port;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private Javalin app;

  public QueryRestApi(DuckDbQueryEngine queryEngine, int port) {
    this.queryEngine = queryEngine;
    this.port = port;
  }

  public void start() {
    app = Javalin.create().start(port);

    app.get("/health", this::handleHealth);
    app.post("/api/v1/query", this::handleQuery);
    app.get("/api/v1/trace/{trace_id}", this::handleGetTrace);

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

  public void stop() {
    if (app != null) {
      app.stop();
    }
  }
}
