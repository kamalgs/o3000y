package dev.o3000y.query.engine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DuckDbQueryEngine {

  private static final Logger LOG = LoggerFactory.getLogger(DuckDbQueryEngine.class);

  private final QueryConfig config;
  private Connection connection;
  private ScheduledExecutorService refreshScheduler;

  public DuckDbQueryEngine(QueryConfig config) {
    this.config = config;
    try {
      this.connection = DriverManager.getConnection("jdbc:duckdb:");
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create DuckDB connection", e);
    }
  }

  public synchronized void refreshView() {
    try (Statement stmt = connection.createStatement()) {
      String globPattern = config.dataPath().toAbsolutePath() + "/**/*.parquet";
      stmt.execute(
          "CREATE OR REPLACE VIEW spans AS SELECT * FROM read_parquet('"
              + globPattern
              + "', hive_partitioning=true, union_by_name=true)");
      LOG.info("Refreshed spans view from {}", globPattern);
    } catch (SQLException e) {
      LOG.warn("Failed to refresh view (data dir may be empty): {}", e.getMessage());
    }
  }

  public void startPeriodicRefresh(long intervalSeconds) {
    if (refreshScheduler != null) {
      return;
    }
    refreshScheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "duckdb-view-refresh");
              t.setDaemon(true);
              return t;
            });
    refreshScheduler.scheduleAtFixedRate(
        () -> {
          try {
            refreshView();
          } catch (Exception e) {
            LOG.error("Periodic view refresh failed", e);
          }
        },
        intervalSeconds,
        intervalSeconds,
        TimeUnit.SECONDS);
    LOG.info("Started periodic view refresh every {}s", intervalSeconds);
  }

  public synchronized QueryResult executeQuery(String sql) {
    long start = System.currentTimeMillis();
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

      long elapsed = System.currentTimeMillis() - start;
      return new QueryResult(columns, rows, rows.size(), elapsed);
    } catch (SQLException e) {
      long elapsed = System.currentTimeMillis() - start;
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
    if (refreshScheduler != null) {
      refreshScheduler.shutdown();
    }
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      LOG.warn("Failed to close DuckDB connection", e);
    }
  }
}
