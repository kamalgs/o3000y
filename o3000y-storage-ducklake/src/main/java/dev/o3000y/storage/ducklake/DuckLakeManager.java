package dev.o3000y.storage.ducklake;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.duckdb.DuckDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DuckLakeManager {

  private static final Logger LOG = LoggerFactory.getLogger(DuckLakeManager.class);

  private final DuckLakeConfig config;
  private final Connection connection;

  public DuckLakeManager(DuckLakeConfig config) {
    this.config = config;
    try {
      this.connection = DriverManager.getConnection("jdbc:duckdb:");
      initialize();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to initialize DuckLake", e);
    }
  }

  private void initialize() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("INSTALL ducklake");
      stmt.execute("LOAD ducklake");

      String attachSql =
          "ATTACH 'ducklake:"
              + config.catalogUri()
              + "' AS lake (DATA_PATH '"
              + config.dataPath()
              + "')";
      LOG.info("Attaching DuckLake catalog: {}", attachSql);
      stmt.execute(attachSql);

      stmt.execute(
          "CREATE TABLE IF NOT EXISTS lake.spans ("
              + "trace_id VARCHAR NOT NULL, "
              + "span_id VARCHAR NOT NULL, "
              + "parent_span_id VARCHAR, "
              + "operation_name VARCHAR NOT NULL, "
              + "service_name VARCHAR NOT NULL, "
              + "start_time TIMESTAMP NOT NULL, "
              + "end_time TIMESTAMP NOT NULL, "
              + "duration_us BIGINT NOT NULL, "
              + "status_code INTEGER NOT NULL, "
              + "status_message VARCHAR, "
              + "span_kind INTEGER NOT NULL, "
              + "attributes VARCHAR, "
              + "events VARCHAR, "
              + "links VARCHAR"
              + ")");

      LOG.info(
          "DuckLake initialized — catalog: {}, data: {}", config.catalogUri(), config.dataPath());
    }
  }

  public Connection newConnection() {
    try {
      Connection dup = ((DuckDBConnection) connection).duplicate();
      try (Statement stmt = dup.createStatement()) {
        stmt.execute("USE lake");
      }
      return dup;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create new DuckDB connection", e);
    }
  }

  public void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      LOG.warn("Failed to close DuckLake connection", e);
    }
  }
}
