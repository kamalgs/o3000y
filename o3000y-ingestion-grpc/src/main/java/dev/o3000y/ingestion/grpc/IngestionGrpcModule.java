package dev.o3000y.ingestion.grpc;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.o3000y.ingestion.api.SpanReceiver;
import jakarta.inject.Singleton;

public final class IngestionGrpcModule extends AbstractModule {

  private final int grpcPort;

  public IngestionGrpcModule(int grpcPort) {
    this.grpcPort = grpcPort;
  }

  @Provides
  @Singleton
  OtlpTraceService provideTraceService(SpanReceiver spanReceiver) {
    return new OtlpTraceService(spanReceiver);
  }

  @Provides
  @Singleton
  GrpcServer provideGrpcServer(OtlpTraceService traceService) {
    return new GrpcServer(grpcPort, traceService);
  }
}
