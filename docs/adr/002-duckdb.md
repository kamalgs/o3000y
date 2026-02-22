# ADR-002: DuckDB for Query Engine

## Status
Accepted

## Context
We need a SQL engine to query Parquet trace data. Options include DuckDB, Apache Spark, Trino, ClickHouse.

## Decision
Use DuckDB in-memory mode with `read_parquet()` views.

## Rationale
- Embedded (no separate process), single JAR dependency
- Native Parquet reader with Hive partition pruning
- Full SQL support including aggregations, window functions
- Excellent single-node performance for OLAP queries
- Simple JDBC interface

## Consequences
- Single-node query capacity (no distributed execution)
- View refresh needed to see new data (not real-time)
- Memory-bound for large result sets
