package dev.o3000y.loadgen;

import dev.o3000y.model.Span;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoadGenService {

  private static final Logger LOG = LoggerFactory.getLogger(LoadGenService.class);

  private final Consumer<List<Span>> spanSink;
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> genTask;
  private ScheduledFuture<?> stopTask;
  private TraceGenerator generator;

  private final AtomicLong tracesSent = new AtomicLong();
  private final AtomicLong spansSent = new AtomicLong();
  private volatile long startTimeMs;
  private volatile long durationSeconds;
  private volatile double tracesPerSecond;
  private volatile double errorRate;
  private volatile boolean running;

  public LoadGenService(Consumer<List<Span>> spanSink) {
    this.spanSink = spanSink;
  }

  public synchronized Map<String, Object> start(
      int durationSeconds, double tracesPerSecond, double errorRate, int maxDepth, int maxBreadth) {
    if (running) {
      return status();
    }

    this.durationSeconds = durationSeconds;
    this.tracesPerSecond = tracesPerSecond;
    this.errorRate = errorRate;
    this.tracesSent.set(0);
    this.spansSent.set(0);
    this.startTimeMs = System.currentTimeMillis();
    this.running = true;

    generator = new TraceGenerator(errorRate, maxDepth, maxBreadth);
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "loadgen");
              t.setDaemon(true);
              return t;
            });

    long intervalNanos = (long) (1_000_000_000.0 / tracesPerSecond);
    genTask =
        scheduler.scheduleAtFixedRate(this::generateOne, 0, intervalNanos, TimeUnit.NANOSECONDS);
    stopTask = scheduler.schedule(this::stop, durationSeconds, TimeUnit.SECONDS);

    LOG.info(
        "Load generator started: {} traces/sec for {}s, errorRate={}",
        tracesPerSecond,
        durationSeconds,
        errorRate);
    return status();
  }

  private void generateOne() {
    try {
      List<Span> trace = generator.generate();
      spanSink.accept(trace);
      tracesSent.incrementAndGet();
      spansSent.addAndGet(trace.size());
    } catch (Exception e) {
      LOG.warn("Load generator error: {}", e.getMessage());
    }
  }

  public synchronized Map<String, Object> stop() {
    if (!running) {
      return status();
    }
    running = false;
    if (genTask != null) genTask.cancel(false);
    if (stopTask != null) stopTask.cancel(false);
    if (scheduler != null) scheduler.shutdown();
    LOG.info("Load generator stopped: {} traces, {} spans sent", tracesSent.get(), spansSent.get());
    return status();
  }

  public Map<String, Object> status() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("running", running);
    m.put("tracesSent", tracesSent.get());
    m.put("spansSent", spansSent.get());
    if (running) {
      long elapsedMs = System.currentTimeMillis() - startTimeMs;
      m.put("elapsedSeconds", elapsedMs / 1000.0);
      m.put("remainingSeconds", Math.max(0, durationSeconds - elapsedMs / 1000.0));
      m.put("tracesPerSecond", tracesPerSecond);
      m.put("errorRate", errorRate);
    }
    return m;
  }
}
