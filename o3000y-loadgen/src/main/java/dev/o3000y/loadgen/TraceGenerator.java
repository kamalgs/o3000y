package dev.o3000y.loadgen;

import dev.o3000y.loadgen.ServiceTopology.ServiceDef;
import dev.o3000y.model.*;
import dev.o3000y.testing.fixtures.SpanFixtures;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class TraceGenerator {

  private static final String[] ERROR_MESSAGES = {
    "connection timeout",
    "UNIQUE constraint violation",
    "500 Internal Server Error",
    "service unavailable",
    "deadline exceeded",
    "connection refused",
    "out of memory",
    "rate limit exceeded"
  };

  private final Map<String, ServiceDef> topology;
  private final double errorRate;
  private final int maxDepth;
  private final int maxBreadth;

  public TraceGenerator(double errorRate, int maxDepth, int maxBreadth) {
    this.topology = ServiceTopology.defaultTopologyMap();
    this.errorRate = errorRate;
    this.maxDepth = maxDepth;
    this.maxBreadth = maxBreadth;
  }

  public List<Span> generate() {
    String traceId = SpanFixtures.randomTraceId();
    List<Span> spans = new ArrayList<>();
    ServiceDef entryPoint = topology.get("api-gateway");
    Instant start = Instant.now();
    generateSpan(traceId, "", entryPoint, start, 0, spans);
    return spans;
  }

  private long generateSpan(
      String traceId,
      String parentSpanId,
      ServiceDef service,
      Instant start,
      int depth,
      List<Span> spans) {

    ThreadLocalRandom rng = ThreadLocalRandom.current();
    String spanId = SpanFixtures.randomSpanId();
    String operation = service.operations().get(rng.nextInt(service.operations().size()));

    // Determine if this span should error
    boolean isError = rng.nextDouble() < errorRate;
    boolean isLeaf = depth >= maxDepth - 1 || service.downstream().isEmpty();

    // Own processing time
    long ownLatencyUs = rng.nextLong(service.minLatencyUs(), service.maxLatencyUs() + 1);
    long childDurationUs = 0;

    // Generate child spans
    if (!isLeaf) {
      int numChildren = Math.min(rng.nextInt(1, maxBreadth + 1), service.downstream().size());
      List<String> shuffled = new ArrayList<>(service.downstream());
      // pick random subset
      Instant childStart =
          start.plusNanos(ownLatencyUs * 500); // children start after some processing
      for (int i = 0; i < numChildren && i < shuffled.size(); i++) {
        ServiceDef child = topology.get(shuffled.get(i));
        if (child != null) {
          long childUs = generateSpan(traceId, spanId, child, childStart, depth + 1, spans);
          childDurationUs += childUs;
          childStart = childStart.plusNanos(childUs * 1000);
        }
      }
    }

    long totalDurationUs = ownLatencyUs + childDurationUs;
    Instant end = start.plusNanos(totalDurationUs * 1000);

    StatusCode status = isError ? StatusCode.ERROR : StatusCode.OK;
    String statusMsg = isError ? ERROR_MESSAGES[rng.nextInt(ERROR_MESSAGES.length)] : "";
    SpanKind kind = SpanKind.fromValue(service.spanKindValue());

    Map<String, String> attributes = buildAttributes(service, operation, isError);

    Span span =
        new Span(
            traceId,
            spanId,
            parentSpanId,
            operation,
            service.name(),
            start,
            end,
            totalDurationUs,
            status,
            statusMsg,
            kind,
            attributes,
            List.of(),
            List.of());
    spans.add(span);

    return totalDurationUs;
  }

  private static Map<String, String> buildAttributes(
      ServiceDef service, String operation, boolean isError) {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    switch (service.name()) {
      case "api-gateway":
        String method = operation.split(" ")[0];
        String path = operation.split(" ").length > 1 ? operation.split(" ")[1] : "/";
        return Map.of(
            "http.method",
            method,
            "http.url",
            "https://api.example.com" + path,
            "http.status_code",
            isError ? "500" : "200");
      case "postgres-client":
        return Map.of(
            "db.system",
            "postgresql",
            "db.statement",
            operation + " FROM users WHERE id = " + rng.nextInt(10000));
      case "redis-cache":
        return Map.of("db.system", "redis", "db.operation", operation);
      case "kafka-producer":
        return Map.of(
            "messaging.system", "kafka", "messaging.destination", operation.replace("send ", ""));
      case "elasticsearch-client":
        return Map.of("db.system", "elasticsearch", "db.operation", operation);
      case "stripe-client":
        return Map.of("rpc.system", "stripe", "rpc.method", operation);
      default:
        return Map.of("service.operation", operation);
    }
  }
}
