package dev.o3000y.ingestion.core;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.api.SpanReceiver;
import dev.o3000y.model.PipelineMetrics;
import dev.o3000y.model.Span;
import dev.o3000y.storage.api.StorageWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SpanBuffer implements SpanReceiver {

  private static final Logger LOG = LoggerFactory.getLogger(SpanBuffer.class);
  private static final int ESTIMATED_SPAN_BYTES = 512;

  private final StorageWriter storageWriter;
  private final BatchConfig config;
  private final PipelineMetrics metrics;
  private final ReentrantLock lock = new ReentrantLock();
  private List<Span> buffer = new ArrayList<>();
  private final AtomicLong currentBufferBytes = new AtomicLong(0);
  private final ScheduledExecutorService scheduler;

  public SpanBuffer(StorageWriter storageWriter, BatchConfig config, PipelineMetrics metrics) {
    this.storageWriter = storageWriter;
    this.config = config;
    this.metrics = metrics;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "span-buffer-timer");
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleAtFixedRate(
        this::timerFlush,
        config.flushInterval().toMillis(),
        config.flushInterval().toMillis(),
        TimeUnit.MILLISECONDS);
  }

  public SpanBuffer(StorageWriter storageWriter, BatchConfig config) {
    this(storageWriter, config, new PipelineMetrics());
  }

  @Override
  public void receive(List<Span> spans) {
    long batchBytes = (long) spans.size() * estimateSpanSize(spans);
    metrics.recordReceive(spans.size());

    Instant now = Instant.now();
    for (Span span : spans) {
      double delaySec =
          (now.getEpochSecond() - span.startTime().getEpochSecond())
              + (now.getNano() - span.startTime().getNano()) / 1_000_000_000.0;
      metrics.recordIngestionDelay(Math.max(0.0, delaySec));
    }

    LOG.debug("Received {} spans ({} bytes est.)", spans.size(), batchBytes);

    lock.lock();
    try {
      buffer.addAll(spans);
      currentBufferBytes.addAndGet(batchBytes);
      if (buffer.size() >= config.maxSpans() || currentBufferBytes.get() >= config.maxBytes()) {
        doFlush();
      }
    } finally {
      lock.unlock();
    }
  }

  public void flush() {
    lock.lock();
    try {
      doFlush();
    } finally {
      lock.unlock();
    }
  }

  private void doFlush() {
    if (buffer.isEmpty()) return;
    List<Span> toFlush = buffer;
    buffer = new ArrayList<>();
    long bytes = currentBufferBytes.getAndSet(0);
    LOG.info("Flushing {} spans (~{} bytes)", toFlush.size(), bytes);
    long start = System.nanoTime();
    try {
      storageWriter.write(toFlush);
    } catch (Exception e) {
      LOG.error("Flush failed for {} spans", toFlush.size(), e);
    }
    long durationNanos = System.nanoTime() - start;
    metrics.recordFlush(toFlush.size(), durationNanos);
  }

  private void timerFlush() {
    try {
      flush();
    } catch (Exception e) {
      LOG.error("Timer flush failed", e);
    }
  }

  private static int estimateSpanSize(List<Span> spans) {
    if (spans.isEmpty()) return ESTIMATED_SPAN_BYTES;
    Span first = spans.getFirst();
    return ESTIMATED_SPAN_BYTES
        + first.attributes().size() * 64
        + first.events().size() * 128
        + first.links().size() * 128;
  }

  public long getTotalSpansReceived() {
    return metrics.getSpansReceived();
  }

  public long getTotalFlushes() {
    return metrics.getFlushCount();
  }

  public void shutdown() {
    scheduler.shutdown();
    flush();
  }
}
