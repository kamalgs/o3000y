package dev.o3000y.loadgen;

import dev.o3000y.model.Span;
import dev.o3000y.testing.fixtures.ProtoFixtures;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoadGenMain {

  private static final Logger LOG = LoggerFactory.getLogger(LoadGenMain.class);

  public static void main(String[] args) {
    LoadGenConfig config = LoadGenConfig.fromArgs(args);
    LOG.info(
        "Starting load generator → {}:{} at {} traces/sec for {}s",
        config.targetHost(),
        config.targetPort(),
        config.tracesPerSecond(),
        config.durationSeconds());

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(config.targetHost(), config.targetPort())
            .usePlaintext()
            .build();

    TraceServiceGrpc.TraceServiceBlockingStub stub = TraceServiceGrpc.newBlockingStub(channel);
    TraceGenerator generator =
        new TraceGenerator(config.errorRate(), config.maxDepth(), config.maxBreadth());
    LoadGenMetrics metrics = new LoadGenMetrics();

    AtomicBoolean running = new AtomicBoolean(true);
    AtomicLong traceCount = new AtomicLong(0);

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  running.set(false);
                  System.out.println(metrics.report());
                }));

    long intervalNanos = (long) (1_000_000_000.0 / config.tracesPerSecond());
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    long deadline =
        System.nanoTime()
            + (config.totalTraces() > 0
                ? Long.MAX_VALUE
                : config.durationSeconds() * 1_000_000_000L);

    scheduler.scheduleAtFixedRate(
        () -> {
          if (!running.get()) return;
          if (System.nanoTime() >= deadline) {
            running.set(false);
            scheduler.shutdown();
            return;
          }
          if (config.totalTraces() > 0 && traceCount.get() >= config.totalTraces()) {
            running.set(false);
            scheduler.shutdown();
            return;
          }

          try {
            List<Span> trace = generator.generate();
            ExportTraceServiceRequest request = ProtoFixtures.toExportRequest(trace);
            long start = System.nanoTime();
            stub.export(request);
            long sendDuration = System.nanoTime() - start;
            metrics.recordSend(trace.size(), sendDuration);
            traceCount.incrementAndGet();

            if (traceCount.get() % 100 == 0) {
              LOG.info("Sent {} traces", traceCount.get());
            }
          } catch (Exception e) {
            metrics.recordGrpcError();
            LOG.warn("gRPC send failed: {}", e.getMessage());
          }
        },
        0,
        intervalNanos,
        TimeUnit.NANOSECONDS);

    // Wait for completion
    try {
      while (running.get()) {
        Thread.sleep(500);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    scheduler.shutdown();
    try {
      scheduler.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    channel.shutdownNow();
    System.out.println(metrics.report());
  }
}
