package dev.tracequery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes SQL queries over Parquet span files using DuckDB.
 * Each request gets a fresh in-memory DuckDB instance that maps
 * the Parquet files as a "spans" view.
 */
public class QueryHandler {

    private final Path dataDir;
    private final ObjectMapper mapper = new ObjectMapper();

    public QueryHandler(Path dataDir) {
        this.dataDir = dataDir;
    }

    /** POST /api/v1/query — execute arbitrary SELECT SQL. */
    public void query(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            var body = exchange.getRequestBody().readAllBytes();
            var root = mapper.readTree(body);
            var sql = root.path("sql").asText("");

            if (sql.isBlank()) {
                HttpUtil.sendJson(exchange, 400, Map.of("error", "sql field is required"));
                return;
            }

            // Block non-SELECT statements
            var normalized = sql.stripLeading().toUpperCase();
            if (!normalized.startsWith("SELECT") && !normalized.startsWith("WITH")) {
                HttpUtil.sendJson(exchange, 400, Map.of("error", "Only SELECT / WITH queries are allowed"));
                return;
            }

            HttpUtil.sendJson(exchange, 200, executeQuery(sql));
        } catch (Exception e) {
            HttpUtil.sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/v1/trace/{traceId} — get all spans for a single trace. */
    public void getTrace(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        var path = exchange.getRequestURI().getPath();
        var traceId = path.substring(path.lastIndexOf('/') + 1);

        try {
            HttpUtil.sendJson(exchange, 200, executeQuery(
                "SELECT * FROM spans WHERE trace_id = '" + escapeSql(traceId) + "' ORDER BY start_time"
            ));
        } catch (Exception e) {
            HttpUtil.sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/v1/services — list distinct service names. */
    public void listServices(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            HttpUtil.sendJson(exchange, 200,
                executeQuery("SELECT DISTINCT service_name FROM spans ORDER BY service_name"));
        } catch (Exception e) {
            HttpUtil.sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/v1/operations — list distinct operations. */
    public void listOperations(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        var queryStr = exchange.getRequestURI().getQuery();
        String service = null;
        if (queryStr != null) {
            for (var param : queryStr.split("&")) {
                var parts = param.split("=", 2);
                if (parts.length == 2 && "service".equals(parts[0])) {
                    service = parts[1];
                }
            }
        }

        try {
            String sql;
            if (service != null && !service.isBlank()) {
                sql = "SELECT DISTINCT operation_name FROM spans WHERE service_name = '"
                    + escapeSql(service) + "' ORDER BY operation_name";
            } else {
                sql = "SELECT DISTINCT service_name, operation_name FROM spans ORDER BY service_name, operation_name";
            }
            HttpUtil.sendJson(exchange, 200, executeQuery(sql));
        } catch (Exception e) {
            HttpUtil.sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> executeQuery(String sql) throws SQLException {
        var glob = dataDir.toAbsolutePath() + "/**/*.parquet";

        try (var conn = DriverManager.getConnection("jdbc:duckdb:")) {
            if (!registerSpansView(conn, glob)) {
                return Map.of("columns", List.of(), "rows", List.of(), "row_count", 0, "elapsed_ms", 0);
            }

            long start = System.currentTimeMillis();
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(sql)) {

                var meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                var columns = new ArrayList<String>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    columns.add(meta.getColumnName(i));
                }

                var rows = new ArrayList<List<Object>>();
                while (rs.next()) {
                    var row = new ArrayList<Object>(colCount);
                    for (int i = 1; i <= colCount; i++) {
                        var val = rs.getObject(i);
                        row.add(val instanceof java.sql.Timestamp ? val.toString() : val);
                    }
                    rows.add(row);
                }

                return Map.of(
                    "columns", columns,
                    "rows", rows,
                    "row_count", rows.size(),
                    "elapsed_ms", System.currentTimeMillis() - start
                );
            }
        }
    }

    /** Register the spans view. Returns false if no parquet files exist yet. */
    private boolean registerSpansView(Connection conn, String glob) {
        try (var stmt = conn.createStatement()) {
            stmt.execute(String.format(
                "CREATE OR REPLACE VIEW spans AS SELECT * FROM read_parquet('%s', hive_partitioning=true, union_by_name=true)",
                glob
            ));
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && (e.getMessage().contains("No files found") || e.getMessage().contains("no files found"))) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    private static String escapeSql(String input) {
        return input.replace("'", "''");
    }
}
