package dev.o3000y.model;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class PipelineMetrics {

  private final long startTimeMillis = System.currentTimeMillis();

  // Ingestion
  private final LongAdder spansReceived = new LongAdder();
  private final AtomicLong receiveCalls = new AtomicLong();

  // Buffer flush
  private final AtomicLong flushCount = new AtomicLong();
  private final LongAdder flushSpanCount = new LongAdder();
  private final AtomicLong flushDurationNanosTotal = new AtomicLong();

  // Storage write
  private final AtomicLong writeCount = new AtomicLong();
  private final LongAdder writeSpanCount = new LongAdder();
  private final AtomicLong writeDurationNanosTotal = new AtomicLong();
  private final AtomicLong writeErrors = new AtomicLong();
  private final AtomicLong writeLatencyMaxNanos = new AtomicLong();

  // Query
  private final AtomicLong queryCount = new AtomicLong();
  private final AtomicLong queryDurationNanosTotal = new AtomicLong();
  private final AtomicLong queryErrors = new AtomicLong();

  public void recordReceive(int spanCount) {
    spansReceived.add(spanCount);
    receiveCalls.incrementAndGet();
  }

  public void recordFlush(int spanCount, long durationNanos) {
    flushCount.incrementAndGet();
    flushSpanCount.add(spanCount);
    flushDurationNanosTotal.addAndGet(durationNanos);
  }

  public void recordWrite(int spanCount, long durationNanos) {
    writeCount.incrementAndGet();
    writeSpanCount.add(spanCount);
    writeDurationNanosTotal.addAndGet(durationNanos);
    updateMax(writeLatencyMaxNanos, durationNanos);
  }

  public void recordWriteError() {
    writeErrors.incrementAndGet();
  }

  public void recordQuery(long durationNanos) {
    queryCount.incrementAndGet();
    queryDurationNanosTotal.addAndGet(durationNanos);
  }

  public void recordQueryError() {
    queryErrors.incrementAndGet();
  }

  public long getSpansReceived() {
    return spansReceived.sum();
  }

  public long getFlushCount() {
    return flushCount.get();
  }

  public String toPrometheus() {
    long uptimeMs = System.currentTimeMillis() - startTimeMillis;

    long spans = spansReceived.sum();
    long receives = receiveCalls.get();
    long flushes = flushCount.get();
    long flushedSpans = flushSpanCount.sum();
    long flushNanos = flushDurationNanosTotal.get();
    long writes = writeCount.get();
    long writtenSpans = writeSpanCount.sum();
    long writeNanos = writeDurationNanosTotal.get();
    long writeMaxNanos = writeLatencyMaxNanos.get();
    long wErrors = writeErrors.get();
    long queries = queryCount.get();
    long queryNanos = queryDurationNanosTotal.get();
    long qErrors = queryErrors.get();

    StringBuilder sb = new StringBuilder();

    // Uptime
    gauge(sb, "o3000y_uptime_seconds", "Process uptime in seconds", uptimeMs / 1000.0);

    // Ingestion
    counter(sb, "o3000y_ingestion_spans_received_total", "Total spans received", spans);
    counter(sb, "o3000y_ingestion_receive_calls_total", "Total receive() calls", receives);

    // Buffer
    counter(sb, "o3000y_buffer_flushes_total", "Total buffer flushes", flushes);
    counter(sb, "o3000y_buffer_spans_flushed_total", "Total spans flushed", flushedSpans);
    counter(
        sb,
        "o3000y_buffer_flush_duration_seconds_total",
        "Total time spent flushing in seconds",
        nanosToSec(flushNanos));

    // Storage
    counter(sb, "o3000y_storage_writes_total", "Total storage write operations", writes);
    counter(
        sb, "o3000y_storage_spans_written_total", "Total spans written to storage", writtenSpans);
    counter(
        sb,
        "o3000y_storage_write_duration_seconds_total",
        "Total time spent writing in seconds",
        nanosToSec(writeNanos));
    gauge(
        sb,
        "o3000y_storage_write_duration_seconds_max",
        "Maximum single write duration in seconds",
        nanosToSec(writeMaxNanos));
    counter(sb, "o3000y_storage_write_errors_total", "Total storage write errors", wErrors);

    // Query
    counter(sb, "o3000y_query_total", "Total queries executed", queries);
    counter(
        sb,
        "o3000y_query_duration_seconds_total",
        "Total time spent querying in seconds",
        nanosToSec(queryNanos));
    counter(sb, "o3000y_query_errors_total", "Total query errors", qErrors);

    return sb.toString();
  }

  private static void counter(StringBuilder sb, String name, String help, long value) {
    sb.append("# HELP ").append(name).append(' ').append(help).append('\n');
    sb.append("# TYPE ").append(name).append(" counter\n");
    sb.append(name).append(' ').append(value).append('\n');
  }

  private static void counter(StringBuilder sb, String name, String help, double value) {
    sb.append("# HELP ").append(name).append(' ').append(help).append('\n');
    sb.append("# TYPE ").append(name).append(" counter\n");
    sb.append(name).append(' ').append(value).append('\n');
  }

  private static void gauge(StringBuilder sb, String name, String help, double value) {
    sb.append("# HELP ").append(name).append(' ').append(help).append('\n');
    sb.append("# TYPE ").append(name).append(" gauge\n");
    sb.append(name).append(' ').append(value).append('\n');
  }

  private static void updateMax(AtomicLong max, long value) {
    long prev;
    do {
      prev = max.get();
      if (value <= prev) return;
    } while (!max.compareAndSet(prev, value));
  }

  private static double nanosToSec(long nanos) {
    return nanos / 1_000_000_000.0;
  }
}
