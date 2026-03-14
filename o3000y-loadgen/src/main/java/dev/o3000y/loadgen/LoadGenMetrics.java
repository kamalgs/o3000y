package dev.o3000y.loadgen;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class LoadGenMetrics {

  private static final long[] BUCKET_UPPER_US = {
    500,
    1_000,
    2_000,
    5_000,
    10_000,
    20_000,
    50_000,
    100_000,
    200_000,
    500_000,
    1_000_000,
    Long.MAX_VALUE
  };

  private final long startTimeNanos = System.nanoTime();
  private final AtomicLong tracesSent = new AtomicLong();
  private final LongAdder spansSent = new LongAdder();
  private final AtomicLong grpcErrors = new AtomicLong();
  private final AtomicLong sendLatencyNanosTotal = new AtomicLong();
  private final AtomicLong sendLatencyMaxNanos = new AtomicLong();
  private final LongAdder[] buckets;

  public LoadGenMetrics() {
    buckets = new LongAdder[BUCKET_UPPER_US.length];
    for (int i = 0; i < buckets.length; i++) {
      buckets[i] = new LongAdder();
    }
  }

  public void recordSend(int spanCount, long sendDurationNanos) {
    tracesSent.incrementAndGet();
    spansSent.add(spanCount);
    sendLatencyNanosTotal.addAndGet(sendDurationNanos);

    // update max
    long prev;
    do {
      prev = sendLatencyMaxNanos.get();
      if (sendDurationNanos <= prev) break;
    } while (!sendLatencyMaxNanos.compareAndSet(prev, sendDurationNanos));

    // histogram bucket
    long us = sendDurationNanos / 1_000;
    for (int i = 0; i < BUCKET_UPPER_US.length; i++) {
      if (us <= BUCKET_UPPER_US[i]) {
        buckets[i].increment();
        break;
      }
    }
  }

  public void recordGrpcError() {
    grpcErrors.incrementAndGet();
  }

  public String report() {
    long elapsed = System.nanoTime() - startTimeNanos;
    double elapsedSec = Math.max(elapsed / 1_000_000_000.0, 0.001);
    long traces = tracesSent.get();
    long spans = spansSent.sum();
    long errors = grpcErrors.get();

    double avgSendMs = traces > 0 ? (sendLatencyNanosTotal.get() / 1_000_000.0) / traces : 0;
    double maxSendMs = sendLatencyMaxNanos.get() / 1_000_000.0;

    // Compute percentiles from histogram
    long total = 0;
    for (LongAdder b : buckets) total += b.sum();
    String p50 = percentileBucket(total, 0.50);
    String p90 = percentileBucket(total, 0.90);
    String p99 = percentileBucket(total, 0.99);

    return String.format(
        """
        ═══════════════════════════════════════════
         Load Generator Report
        ═══════════════════════════════════════════
         Duration:        %.1f s
         Traces sent:     %,d
         Spans sent:      %,d
         gRPC errors:     %,d
        ───────────────────────────────────────────
         Throughput:
           Traces/sec:    %.1f
           Spans/sec:     %.1f
        ───────────────────────────────────────────
         Send latency:
           avg:           %.2f ms
           p50:           %s
           p90:           %s
           p99:           %s
           max:           %.2f ms
        ═══════════════════════════════════════════""",
        elapsedSec,
        traces,
        spans,
        errors,
        traces / elapsedSec,
        spans / elapsedSec,
        avgSendMs,
        p50,
        p90,
        p99,
        maxSendMs);
  }

  private String percentileBucket(long total, double percentile) {
    if (total == 0) return "N/A";
    long target = (long) Math.ceil(total * percentile);
    long cumulative = 0;
    for (int i = 0; i < buckets.length; i++) {
      cumulative += buckets[i].sum();
      if (cumulative >= target) {
        if (i == buckets.length - 1) return "> 1000 ms";
        return "<= " + formatUs(BUCKET_UPPER_US[i]);
      }
    }
    return "N/A";
  }

  private static String formatUs(long us) {
    if (us < 1_000) return us + " us";
    if (us < 1_000_000) return String.format("%.1f ms", us / 1_000.0);
    return String.format("%.1f s", us / 1_000_000.0);
  }
}
