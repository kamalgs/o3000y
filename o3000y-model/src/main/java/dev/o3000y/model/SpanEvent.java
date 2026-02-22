package dev.o3000y.model;

import java.time.Instant;
import java.util.Map;

public record SpanEvent(String name, Instant timestamp, Map<String, String> attributes) {

  public SpanEvent {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
