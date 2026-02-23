package dev.o3000y.model;

import java.util.Map;

public record SpanLink(
    String traceId, String spanId, String traceState, Map<String, String> attributes) {

  public SpanLink {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
