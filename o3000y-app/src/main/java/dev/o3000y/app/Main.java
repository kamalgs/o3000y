package dev.o3000y.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.o3000y.ingestion.core.SpanBuffer;
import dev.o3000y.ingestion.grpc.GrpcServer;
import dev.o3000y.ingestion.grpc.IngestionGrpcModule;
import dev.o3000y.query.rest.QueryModule;
import dev.o3000y.query.rest.QueryRestApi;
import dev.o3000y.storage.ducklake.DuckLakeManager;
import dev.o3000y.storage.ducklake.DuckLakeModule;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws IOException {
    LOG.info("o3000y starting...");
    AppConfig config = AppConfig.fromEnvironment();

    Injector injector =
        Guice.createInjector(
            new DuckLakeModule(config.duckLake()),
            new IngestionCoreModule(config.batch()),
            new IngestionGrpcModule(config.grpcPort()),
            new QueryModule(config.query(), config.restPort()));

    GrpcServer grpcServer = injector.getInstance(GrpcServer.class);
    QueryRestApi restApi = injector.getInstance(QueryRestApi.class);
    SpanBuffer spanBuffer = injector.getInstance(SpanBuffer.class);
    DuckLakeManager duckLakeManager = injector.getInstance(DuckLakeManager.class);

    grpcServer.start();
    restApi.start();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  LOG.info("Shutting down...");
                  spanBuffer.shutdown();
                  grpcServer.stop();
                  restApi.stop();
                  duckLakeManager.close();
                }));

    LOG.info("o3000y started — gRPC:{}, REST:{}", config.grpcPort(), config.restPort());
  }
}
