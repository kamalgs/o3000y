package dev.o3000y.query.rest;

import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.QueryResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QueryRestApi {

  private static final Logger LOG = LoggerFactory.getLogger(QueryRestApi.class);

  private final DuckDbQueryEngine queryEngine;
  private final int port;
  private Javalin app;

  public QueryRestApi(DuckDbQueryEngine queryEngine, int port) {
    this.queryEngine = queryEngine;
    this.port = port;
  }

  public void start() {
    app = Javalin.create().start(port);

    app.post("/api/v1/query", this::handleQuery);
    app.get("/api/v1/trace/{trace_id}", this::handleGetTrace);

    LOG.info("REST API started on port {}", port);
  }

  public int getPort() {
    return port;
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
    } catch (Exception e) {
      LOG.error("Query failed", e);
      ctx.status(500).json(new ErrorResponse(e.getMessage(), 500));
    }
  }

  private void handleGetTrace(Context ctx) {
    try {
      String traceId = ctx.pathParam("trace_id");
      QueryResult result = queryEngine.getTrace(traceId);
      ctx.json(
          new QueryResponse(
              result.columns(), result.rows(), result.rowCount(), result.elapsedMs()));
    } catch (Exception e) {
      LOG.error("Get trace failed", e);
      ctx.status(500).json(new ErrorResponse(e.getMessage(), 500));
    }
  }

  public void stop() {
    if (app != null) {
      app.stop();
    }
  }
}
