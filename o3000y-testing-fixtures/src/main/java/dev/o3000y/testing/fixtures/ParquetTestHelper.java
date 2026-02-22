package dev.o3000y.testing.fixtures;

import dev.o3000y.model.Span;
import dev.o3000y.storage.parquet.ParquetSpanWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public final class ParquetTestHelper {

  private final ParquetSpanWriter writer;

  public ParquetTestHelper() {
    this.writer = new ParquetSpanWriter();
  }

  public void writeSpansWithHivePartitioning(Path baseDir, List<Span> spans) throws IOException {
    if (spans.isEmpty()) return;
    Span first = spans.getFirst();
    ZonedDateTime dt = first.startTime().atZone(ZoneOffset.UTC);
    String partition =
        String.format(
            "year=%d/month=%02d/day=%02d/hour=%02d",
            dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(), dt.getHour());

    Path partitionDir = baseDir.resolve(partition);
    Files.createDirectories(partitionDir);
    Path file = partitionDir.resolve("test_" + System.nanoTime() + ".parquet");
    writer.write(spans, file);
  }
}
