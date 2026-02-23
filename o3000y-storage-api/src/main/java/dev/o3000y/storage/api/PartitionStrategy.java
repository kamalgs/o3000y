package dev.o3000y.storage.api;

import java.time.Instant;

public interface PartitionStrategy {

  String partitionPath(Instant timestamp);
}
