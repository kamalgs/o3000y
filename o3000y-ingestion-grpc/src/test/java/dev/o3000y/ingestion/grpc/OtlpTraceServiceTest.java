package dev.o3000y.ingestion.grpc;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import dev.o3000y.ingestion.api.SpanReceiver;
import dev.o3000y.model.Span;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class OtlpTraceServiceTest {

  @Test
  void exportCallsSpanReceiver() throws Exception {
    List<List<Span>> received = new CopyOnWriteArrayList<>();
    SpanReceiver receiver = received::add;

    OtlpTraceService service = new OtlpTraceService(receiver);

    String serverName = InProcessServerBuilder.generateName();
    io.grpc.Server server =
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();

    ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    try {
      TraceServiceGrpc.TraceServiceBlockingStub stub = TraceServiceGrpc.newBlockingStub(channel);

      Resource resource =
          Resource.newBuilder()
              .addAttributes(
                  KeyValue.newBuilder()
                      .setKey("service.name")
                      .setValue(AnyValue.newBuilder().setStringValue("test-svc")))
              .build();

      io.opentelemetry.proto.trace.v1.Span protoSpan =
          io.opentelemetry.proto.trace.v1.Span.newBuilder()
              .setTraceId(ByteString.copyFrom(new byte[16]))
              .setSpanId(ByteString.copyFrom(new byte[8]))
              .setName("test-op")
              .setStartTimeUnixNano(1_000_000_000_000_000_000L)
              .setEndTimeUnixNano(1_000_000_000_100_000_000L)
              .build();

      ExportTraceServiceRequest request =
          ExportTraceServiceRequest.newBuilder()
              .addResourceSpans(
                  ResourceSpans.newBuilder()
                      .setResource(resource)
                      .addScopeSpans(ScopeSpans.newBuilder().addSpans(protoSpan)))
              .build();

      ExportTraceServiceResponse response = stub.export(request);
      assertNotNull(response);
      assertEquals(1, received.size());
      assertEquals(1, received.getFirst().size());
      assertEquals("test-op", received.getFirst().getFirst().operationName());
    } finally {
      channel.shutdownNow();
      server.shutdownNow();
    }
  }
}
