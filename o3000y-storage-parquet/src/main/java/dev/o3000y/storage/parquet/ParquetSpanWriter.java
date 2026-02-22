package dev.o3000y.storage.parquet;

import dev.o3000y.model.Span;
import dev.o3000y.model.SpanEvent;
import dev.o3000y.model.SpanLink;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

public final class ParquetSpanWriter {

  private static final MessageType SCHEMA =
      MessageTypeParser.parseMessageType(
          "message spans {\n"
              + "  required binary trace_id (UTF8);\n"
              + "  required binary span_id (UTF8);\n"
              + "  optional binary parent_span_id (UTF8);\n"
              + "  required binary operation_name (UTF8);\n"
              + "  required binary service_name (UTF8);\n"
              + "  required int64 start_time (TIMESTAMP(MICROS,true));\n"
              + "  required int64 end_time (TIMESTAMP(MICROS,true));\n"
              + "  required int64 duration_us;\n"
              + "  required int32 status_code;\n"
              + "  optional binary status_message (UTF8);\n"
              + "  required int32 span_kind;\n"
              + "  optional binary attributes (UTF8);\n"
              + "  optional binary events (UTF8);\n"
              + "  optional binary links (UTF8);\n"
              + "}");

  public void write(List<Span> spans, Path outputPath) throws IOException {
    Configuration hadoopConf = new Configuration();
    org.apache.hadoop.fs.Path hadoopPath = new org.apache.hadoop.fs.Path(outputPath.toUri());

    SimpleGroupFactory groupFactory = new SimpleGroupFactory(SCHEMA);

    try (ParquetWriter<Group> writer =
        ExampleParquetWriter.builder(hadoopPath)
            .withType(SCHEMA)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
            .withConf(hadoopConf)
            .build()) {

      for (Span span : spans) {
        Group group = groupFactory.newGroup();
        group.add("trace_id", span.traceId());
        group.add("span_id", span.spanId());
        if (!span.parentSpanId().isEmpty()) {
          group.add("parent_span_id", span.parentSpanId());
        }
        group.add("operation_name", span.operationName());
        group.add("service_name", span.serviceName());
        group.add("start_time", toMicros(span.startTime()));
        group.add("end_time", toMicros(span.endTime()));
        group.add("duration_us", span.durationUs());
        group.add("status_code", span.statusCode().value());
        if (!span.statusMessage().isEmpty()) {
          group.add("status_message", span.statusMessage());
        }
        group.add("span_kind", span.spanKind().value());
        group.add("attributes", mapToJson(span.attributes()));
        group.add("events", eventsToJson(span.events()));
        group.add("links", linksToJson(span.links()));

        writer.write(group);
      }
    }
  }

  private static long toMicros(Instant instant) {
    return instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1_000;
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
