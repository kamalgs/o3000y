# Storage Layout

## Hive Partitioning

Spans are partitioned by time using Hive-style directory naming:

```
year=YYYY/month=MM/day=DD/hour=HH/
```

This enables:
- Efficient time-range queries (DuckDB partition pruning)
- Simple data retention (delete old directories)
- Parallel writes (different hours go to different directories)

## File Naming

Each Parquet file has a unique name:

```
<instanceId>_<timestampMillis>_<sequence>.parquet
```

- `instanceId`: 8-char random UUID prefix (unique per writer instance)
- `timestampMillis`: wall clock time of write
- `sequence`: monotonically increasing counter

## Parquet Configuration

| Setting | Value | Rationale |
|---------|-------|-----------|
| Compression | ZSTD | Best compression/speed ratio |
| Dictionary Encoding | Enabled | Efficient for low-cardinality string columns |
| Row Group Size | 128 MB | Balance between parallelism and overhead |
| Page Size | 1 MB | Efficient predicate pushdown |
| Sort Order | service_name, start_time | Improves compression and query filtering |

## Multi-Partition Writes

When a batch contains spans from different hours, the `LocalStorageWriter` groups them by partition and writes one Parquet file per partition directory.
