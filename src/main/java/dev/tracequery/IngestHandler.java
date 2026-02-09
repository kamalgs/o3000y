package dev.tracequery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles OTLP/HTTP JSON trace export requests.
 * Parses the resourceSpans structure and feeds flattened SpanRecords into the buffer.
 */
public class IngestHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SpanBuffer buffer;

    public IngestHandler(SpanBuffer buffer) {
        this.buffer = buffer;
    }

    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            var body = exchange.getRequestBody().readAllBytes();
            var root = mapper.readTree(body);
            var spans = parseResourceSpans(root);
            buffer.add(spans);
            HttpUtil.sendJson(exchange, 200, Map.of("accepted", spans.size()));
        } catch (Exception e) {
            HttpUtil.sendJson(exchange, 400, Map.of("error", e.getMessage()));
        }
    }

    private List<SpanRecord> parseResourceSpans(JsonNode root) {
        var results = new ArrayList<SpanRecord>();

        for (var rs : root.path("resourceSpans")) {
            var resource = rs.path("resource");
            var serviceName = extractServiceName(resource);
            var resourceAttrs = attributesToJson(resource.path("attributes"));

            for (var scopeSpan : rs.path("scopeSpans")) {
                for (var span : scopeSpan.path("spans")) {
                    long startNano = parseNano(span.path("startTimeUnixNano"));
                    long endNano = parseNano(span.path("endTimeUnixNano"));

                    results.add(new SpanRecord(
                        span.path("traceId").asText(""),
                        span.path("spanId").asText(""),
                        span.path("parentSpanId").asText(""),
                        serviceName,
                        span.path("name").asText(""),
                        span.path("kind").asInt(0),
                        startNano,
                        endNano,
                        (endNano - startNano) / 1_000,  // nanos to micros
                        span.path("status").path("code").asInt(0),
                        span.path("status").path("message").asText(""),
                        attributesToJson(span.path("attributes")),
                        resourceAttrs
                    ));
                }
            }
        }
        return results;
    }

    private String extractServiceName(JsonNode resource) {
        for (var attr : resource.path("attributes")) {
            if ("service.name".equals(attr.path("key").asText())) {
                return extractValue(attr.path("value"));
            }
        }
        return "unknown";
    }

    private String attributesToJson(JsonNode attributes) {
        var map = new LinkedHashMap<String, String>();
        for (var attr : attributes) {
            map.put(attr.path("key").asText(), extractValue(attr.path("value")));
        }
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String extractValue(JsonNode value) {
        if (value.has("stringValue")) return value.path("stringValue").asText();
        if (value.has("intValue"))    return value.path("intValue").asText();
        if (value.has("doubleValue")) return String.valueOf(value.path("doubleValue").asDouble());
        if (value.has("boolValue"))   return String.valueOf(value.path("boolValue").asBoolean());
        return value.toString();
    }

    private static long parseNano(JsonNode node) {
        var text = node.asText("0");
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
