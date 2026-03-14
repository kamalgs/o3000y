package dev.o3000y.storage.ducklake;

import dev.o3000y.model.PipelineMetrics;
import dev.o3000y.model.Span;
import dev.o3000y.model.SpanEvent;
import dev.o3000y.model.SpanLink;
import dev.o3000y.storage.api.StorageWriter;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DuckLakeStorageWriter implements StorageWriter {

  private static final Logger LOG = LoggerFactory.getLogger(DuckLakeStorageWriter.class);

  private static final String INSERT_SQL =
      "INSERT INTO spans (trace_id, span_id, parent_span_id, operation_name, service_name, "
          + "start_time, end_time, duration_us, status_code, status_message, span_kind, "
          + "attributes, events, links) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final int MAX_RETRIES = 3;

  private final Connection connection;
  private final PipelineMetrics metrics;

  @Inject
  public DuckLakeStorageWriter(DuckLakeManager manager, PipelineMetrics metrics) {
    this.connection = manager.newConnection();
    this.metrics = metrics;
  }

  public DuckLakeStorageWriter(DuckLakeManager manager) {
    this(manager, new PipelineMetrics());
  }

  @Override
  public synchronized void write(List<Span> spans) {
    if (spans.isEmpty()) {
      return;
    }
    long start = System.nanoTime();
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        writeWithTransaction(spans);
        long durationNanos = System.nanoTime() - start;
        metrics.recordWrite(spans.size(), durationNanos);
        LOG.debug("Wrote {} spans to DuckLake", spans.size());
        return;
      } catch (SQLException e) {
        if (attempt < MAX_RETRIES && isRetryable(e)) {
          LOG.warn("Write conflict (attempt {}), retrying: {}", attempt, e.getMessage());
        } else {
          metrics.recordWriteError();
          LOG.error(
              "Failed to write {} spans to DuckLake after {} attempts", spans.size(), attempt, e);
          return;
        }
      }
    }
  }

  private void writeWithTransaction(List<Span> spans) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
      for (Span span : spans) {
        ps.setString(1, span.traceId());
        ps.setString(2, span.spanId());
        ps.setString(3, span.parentSpanId().isEmpty() ? null : span.parentSpanId());
        ps.setString(4, span.operationName());
        ps.setString(5, span.serviceName());
        ps.setTimestamp(6, toTimestamp(span.startTime()));
        ps.setTimestamp(7, toTimestamp(span.endTime()));
        ps.setLong(8, span.durationUs());
        ps.setInt(9, span.statusCode().value());
        ps.setString(10, span.statusMessage().isEmpty() ? null : span.statusMessage());
        ps.setInt(11, span.spanKind().value());
        ps.setString(12, mapToJson(span.attributes()));
        ps.setString(13, eventsToJson(span.events()));
        ps.setString(14, linksToJson(span.links()));
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private static Timestamp toTimestamp(Instant instant) {
    Timestamp ts = Timestamp.from(instant);
    ts.setNanos(instant.getNano());
    return ts;
  }

  private static boolean isRetryable(SQLException e) {
    String msg = e.getMessage();
    if (msg == null) return false;
    String lower = msg.toLowerCase();
    return lower.contains("conflict") || lower.contains("concurrent");
  }

  static String mapToJson(Map<String, String> map) {
    if (map.isEmpty()) return "{}";
    return "{"
        + map.entrySet().stream()
            .map(e -> "\"" + escapeJson(e.getKey()) + "\":\"" + escapeJson(e.getValue()) + "\"")
            .collect(Collectors.joining(","))
        + "}";
  }

  static String eventsToJson(List<SpanEvent> events) {
    if (events.isEmpty()) return "[]";
    return "["
        + events.stream()
            .map(
                e ->
                    "{\"name\":\""
                        + escapeJson(e.name())
                        + "\",\"timestamp\":\""
                        + e.timestamp()
                        + "\",\"attributes\":"
                        + mapToJson(e.attributes())
                        + "}")
            .collect(Collectors.joining(","))
        + "]";
  }

  static String linksToJson(List<SpanLink> links) {
    if (links.isEmpty()) return "[]";
    return "["
        + links.stream()
            .map(
                l ->
                    "{\"traceId\":\""
                        + escapeJson(l.traceId())
                        + "\",\"spanId\":\""
                        + escapeJson(l.spanId())
                        + "\",\"traceState\":\""
                        + escapeJson(l.traceState())
                        + "\",\"attributes\":"
                        + mapToJson(l.attributes())
                        + "}")
            .collect(Collectors.joining(","))
        + "]";
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
