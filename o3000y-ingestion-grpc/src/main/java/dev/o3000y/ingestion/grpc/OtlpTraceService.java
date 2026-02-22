package dev.o3000y.ingestion.grpc;

import dev.o3000y.ingestion.api.SpanReceiver;
import dev.o3000y.model.Span;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OtlpTraceService extends TraceServiceGrpc.TraceServiceImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(OtlpTraceService.class);

  private final SpanReceiver spanReceiver;
  private final ProtoSpanMapper mapper;

  public OtlpTraceService(SpanReceiver spanReceiver) {
    this.spanReceiver = spanReceiver;
    this.mapper = new ProtoSpanMapper();
  }

  @Override
  public void export(
      ExportTraceServiceRequest request,
      StreamObserver<ExportTraceServiceResponse> responseObserver) {
    try {
      List<Span> spans = mapper.map(request.getResourceSpansList());
      if (!spans.isEmpty()) {
        spanReceiver.receive(spans);
      }
      responseObserver.onNext(ExportTraceServiceResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("Failed to process export request", e);
      responseObserver.onError(
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
