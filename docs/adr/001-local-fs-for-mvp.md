# ADR-001: Local Filesystem for MVP Storage

## Status
Accepted

## Context
We need a storage backend for Parquet trace data. Options include local filesystem, S3/GCS, HDFS.

## Decision
Use local filesystem with Hive-partitioned directories for the MVP.

## Rationale
- Zero infrastructure dependencies — runs anywhere with a disk
- DuckDB reads local Parquet files natively with excellent performance
- Hive partitioning enables time-based queries without a catalog
- Simple to understand, debug, and operate
- Guice module design allows swapping to S3/GCS later

## Consequences
- Limited to single-node storage capacity
- No built-in replication or durability beyond disk
- Data retention requires manual cleanup of old directories
