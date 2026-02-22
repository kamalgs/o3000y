package dev.o3000y.ingestion.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrpcServer {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);

  private final Server server;

  public GrpcServer(int port, OtlpTraceService traceService) {
    this.server = ServerBuilder.forPort(port).addService(traceService).build();
  }

  public void start() throws IOException {
    server.start();
    LOG.info("gRPC server started on port {}", server.getPort());
  }

  public int getPort() {
    return server.getPort();
  }

  public void stop() {
    try {
      server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      server.shutdownNow();
    }
    LOG.info("gRPC server stopped");
  }
}
