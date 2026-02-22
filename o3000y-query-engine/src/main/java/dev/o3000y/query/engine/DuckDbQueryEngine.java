package dev.o3000y.query.engine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DuckDbQueryEngine {

  private static final Logger LOG = LoggerFactory.getLogger(DuckDbQueryEngine.class);

  private final QueryConfig config;
  private Connection connection;

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

  public synchronized QueryResult executeQuery(String sql) {
    long start = System.currentTimeMillis();
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
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

      long elapsed = System.currentTimeMillis() - start;
      return new QueryResult(columns, rows, rows.size(), elapsed);
    } catch (SQLException e) {
      throw new RuntimeException("Query failed: " + e.getMessage(), e);
    }
  }

  public QueryResult getTrace(String traceIdHex) {
    return executeQuery(
        "SELECT * FROM spans WHERE trace_id = '" + traceIdHex + "' ORDER BY start_time");
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
