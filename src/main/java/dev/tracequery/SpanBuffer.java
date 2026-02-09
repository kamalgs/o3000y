package dev.tracequery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Accumulates spans in memory and periodically flushes them to Parquet files
 * using DuckDB's COPY TO command. Thread-safe.
 */
public class SpanBuffer {

    private static final int MAX_SPANS = 1_000;
    private static final long FLUSH_INTERVAL_MS = 10_000;

    private final Path dataDir;
    private final Lock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "span-flush");
        t.setDaemon(true);
        return t;
    });

    private List<SpanRecord> buffer = new ArrayList<>();

    public SpanBuffer(Path dataDir) {
        this.dataDir = dataDir;
        scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** Add spans to the buffer. Triggers a flush if the count threshold is reached. */
    public void add(List<SpanRecord> spans) {
        lock.lock();
        try {
            buffer.addAll(spans);
            if (buffer.size() >= MAX_SPANS) {
                doFlush();
            }
        } finally {
            lock.unlock();
        }
    }

    /** Force-flush the current buffer to a Parquet file. */
    public void flush() {
        lock.lock();
        try {
            doFlush();
        } finally {
            lock.unlock();
        }
    }

    // Must be called while holding the lock.
    private void doFlush() {
        if (buffer.isEmpty()) return;
        var spans = buffer;
        buffer = new ArrayList<>();
        writeParquet(spans);
    }

    private void writeParquet(List<SpanRecord> spans) {
        var now = Instant.now().atZone(ZoneOffset.UTC);
        var partitionPath = dataDir.resolve(String.format(
            "year=%d/month=%02d/day=%02d/hour=%02d",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour()
        ));

        try {
            Files.createDirectories(partitionPath);

            var fileName = String.format("%d_%d.parquet", now.toInstant().toEpochMilli(), spans.size());
            var filePath = partitionPath.resolve(fileName);

            try (var conn = DriverManager.getConnection("jdbc:duckdb:")) {
                // Create a temporary table to hold the batch
                try (var stmt = conn.createStatement()) {
                    stmt.execute("""
                        CREATE TABLE batch (
                            trace_id            VARCHAR,
                            span_id             VARCHAR,
                            parent_span_id      VARCHAR,
                            service_name        VARCHAR,
                            operation_name      VARCHAR,
                            span_kind           INTEGER,
                            start_time          TIMESTAMP,
                            end_time            TIMESTAMP,
                            duration_us         BIGINT,
                            status_code         INTEGER,
                            status_message      VARCHAR,
                            span_attributes     VARCHAR,
                            resource_attributes VARCHAR
                        )
                    """);
                }

                // Batch-insert all spans
                try (var ps = conn.prepareStatement(
                    "INSERT INTO batch VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
                )) {
                    for (var span : spans) {
                        ps.setString(1, span.traceId());
                        ps.setString(2, span.spanId());
                        ps.setString(3, span.parentSpanId());
                        ps.setString(4, span.serviceName());
                        ps.setString(5, span.operationName());
                        ps.setInt(6, span.spanKind());
                        ps.setTimestamp(7, new Timestamp(span.startTimeUnixNano() / 1_000_000));
                        ps.setTimestamp(8, new Timestamp(span.endTimeUnixNano() / 1_000_000));
                        ps.setLong(9, span.durationUs());
                        ps.setInt(10, span.statusCode());
                        ps.setString(11, span.statusMessage());
                        ps.setString(12, span.spanAttributes());
                        ps.setString(13, span.resourceAttributes());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Write to Parquet with ZSTD compression
                try (var stmt = conn.createStatement()) {
                    stmt.execute(String.format(
                        "COPY batch TO '%s' (FORMAT PARQUET, COMPRESSION ZSTD)",
                        filePath.toAbsolutePath()
                    ));
                }
            }

            System.out.printf("[flush] %d spans → %s%n", spans.size(), filePath);

        } catch (Exception e) {
            System.err.printf("[flush] FAILED to write %d spans: %s%n", spans.size(), e.getMessage());
            e.printStackTrace();
        }
    }
}
