package dev.o3000y.storage.api;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class HivePartitionStrategy implements PartitionStrategy {

  @Override
  public String partitionPath(Instant timestamp) {
    ZonedDateTime dt = timestamp.atZone(ZoneOffset.UTC);
    return String.format(
        "year=%d/month=%02d/day=%02d/hour=%02d",
        dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(), dt.getHour());
  }
}
