package dev.o3000y.query.engine;

import dev.o3000y.model.PipelineMetrics;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DuckDbQueryEngine {

  private static final Logger LOG = LoggerFactory.getLogger(DuckDbQueryEngine.class);

  private final QueryConfig config;
  private final Connection connection;
  private final PipelineMetrics metrics;

  public DuckDbQueryEngine(QueryConfig config, Connection connection, PipelineMetrics metrics) {
    this.config = config;
    this.connection = connection;
    this.metrics = metrics;
  }

  public DuckDbQueryEngine(QueryConfig config, Connection connection) {
    this(config, connection, new PipelineMetrics());
  }

  public synchronized QueryResult executeQuery(String sql) {
    long startNanos = System.nanoTime();
    long startMs = System.currentTimeMillis();
    long timeoutMs = config.queryTimeout().toMillis();

    try (Statement stmt = connection.createStatement()) {
      stmt.setQueryTimeout((int) Math.max(1, config.queryTimeout().toSeconds()));

      ResultSet rs = stmt.executeQuery(sql);
      ResultSetMetaData meta = rs.getMetaData();
      int colCount = meta.getColumnCount();

      List<String> columns = new ArrayList<>();
      for (int i = 1; i <= colCount; i++) {
        columns.add(meta.getColumnName(i));
      }

      List<List<Object>> rows = new ArrayList<>();
      int rowLimit = config.maxResultRows();
      while (rs.next() && rows.size() < rowLimit) {
        List<Object> row = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) {
          row.add(rs.getObject(i));
        }
        rows.add(row);
      }
      rs.close();

      long elapsed = System.currentTimeMillis() - startMs;
      metrics.recordQuery(System.nanoTime() - startNanos);
      return new QueryResult(columns, rows, rows.size(), elapsed);
    } catch (SQLException e) {
      metrics.recordQueryError();
      long elapsed = System.currentTimeMillis() - startMs;
      if (elapsed >= timeoutMs || isTimeoutError(e)) {
        throw new QueryTimeoutException(sql, timeoutMs);
      }
      throw new InvalidQueryException("Query failed: " + e.getMessage(), e);
    }
  }

  public QueryResult getTrace(String traceIdHex) {
    String sanitized = traceIdHex.replaceAll("[^a-fA-F0-9]", "");
    if (sanitized.isEmpty()) {
      throw new InvalidQueryException("Invalid trace ID: " + traceIdHex);
    }
    return executeQuery(
        "SELECT * FROM spans WHERE trace_id = '" + sanitized + "' ORDER BY start_time");
  }

  private static boolean isTimeoutError(SQLException e) {
    String msg = e.getMessage();
    return msg != null
        && (msg.contains("timeout") || msg.contains("INTERRUPT") || msg.contains("canceled"));
  }

  public void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      LOG.warn("Failed to close DuckDB connection", e);
    }
  }
}
