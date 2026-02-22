package dev.o3000y.ingestion.core;

import dev.o3000y.ingestion.api.BatchConfig;
import dev.o3000y.ingestion.api.SpanReceiver;
import dev.o3000y.model.Span;
import dev.o3000y.storage.api.StorageWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SpanBuffer implements SpanReceiver {

  private static final Logger LOG = LoggerFactory.getLogger(SpanBuffer.class);

  private final StorageWriter storageWriter;
  private final BatchConfig config;
  private final ReentrantLock lock = new ReentrantLock();
  private List<Span> buffer = new ArrayList<>();
  private final ScheduledExecutorService scheduler;

  public SpanBuffer(StorageWriter storageWriter, BatchConfig config) {
    this.storageWriter = storageWriter;
    this.config = config;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "span-buffer-flush");
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleAtFixedRate(
        this::timerFlush,
        config.flushInterval().toMillis(),
        config.flushInterval().toMillis(),
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void receive(List<Span> spans) {
    lock.lock();
    try {
      buffer.addAll(spans);
      if (buffer.size() >= config.maxSpans()) {
        flush();
      }
    } finally {
      lock.unlock();
    }
  }

  public void flush() {
    List<Span> toFlush;
    lock.lock();
    try {
      if (buffer.isEmpty()) return;
      toFlush = buffer;
      buffer = new ArrayList<>();
    } finally {
      lock.unlock();
    }
    LOG.info("Flushing {} spans", toFlush.size());
    storageWriter.write(toFlush);
  }

  private void timerFlush() {
    try {
      flush();
    } catch (Exception e) {
      LOG.error("Timer flush failed", e);
    }
  }

  public void shutdown() {
    scheduler.shutdown();
    flush();
  }
}
