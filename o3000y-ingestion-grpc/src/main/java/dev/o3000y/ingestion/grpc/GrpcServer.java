package dev.o3000y.ingestion.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrpcServer {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);
  private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
  private static final int SHUTDOWN_TIMEOUT_SEC = 10;

  private final Server server;

  public GrpcServer(int port, OtlpTraceService traceService) {
    this.server =
        ServerBuilder.forPort(port)
            .addService(traceService)
            .maxInboundMessageSize(MAX_MESSAGE_SIZE)
            .build();
  }

  public void start() throws IOException {
    server.start();
    LOG.info("gRPC server started on port {}", server.getPort());
  }

  public int getPort() {
    return server.getPort();
  }

  public void stop() {
    LOG.info("Stopping gRPC server...");
    try {
      if (!server.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
        LOG.warn("gRPC server did not terminate in time, forcing shutdown");
        server.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      server.shutdownNow();
    }
    LOG.info("gRPC server stopped");
  }
}
