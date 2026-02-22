package dev.o3000y.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.o3000y.ingestion.core.SpanBuffer;
import dev.o3000y.ingestion.grpc.GrpcServer;
import dev.o3000y.ingestion.grpc.IngestionGrpcModule;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.rest.QueryModule;
import dev.o3000y.query.rest.QueryRestApi;
import dev.o3000y.storage.local.LocalStorageModule;
import java.io.IOException;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws IOException {
    LOG.info("o3000y starting...");
    AppConfig config = AppConfig.fromEnvironment();

    Files.createDirectories(config.storage().basePath());

    Injector injector =
        Guice.createInjector(
            new StorageParquetModule(),
            new LocalStorageModule(config.storage()),
            new IngestionCoreModule(config.batch()),
            new IngestionGrpcModule(config.grpcPort()),
            new QueryModule(config.query(), config.restPort()));

    GrpcServer grpcServer = injector.getInstance(GrpcServer.class);
    QueryRestApi restApi = injector.getInstance(QueryRestApi.class);
    DuckDbQueryEngine queryEngine = injector.getInstance(DuckDbQueryEngine.class);
    SpanBuffer spanBuffer = injector.getInstance(SpanBuffer.class);

    queryEngine.refreshView();
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
                  queryEngine.close();
                }));

    LOG.info("o3000y started — gRPC:{}, REST:{}", config.grpcPort(), config.restPort());
  }
}
