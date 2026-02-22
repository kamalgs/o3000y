# o3000y Architecture

## Overview

o3000y is a distributed trace query tool that receives OpenTelemetry spans via gRPC, stores them as Parquet files on the local filesystem, and exposes them through a SQL query engine (DuckDB) and REST API.

## Architecture Diagram

```
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   OTLP Clients   │───▶│   gRPC Server    │───▶│   SpanBuffer     │
│  (telemetrygen)  │    │  (port 4317)     │    │  (batch + flush) │
└──────────────────┘    └──────────────────┘    └────────┬─────────┘
                                                         │
                                                         ▼
                        ┌──────────────────┐    ┌──────────────────┐
                        │  Parquet Files   │◀───│  LocalStorage    │
                        │  (Hive layout)   │    │  Writer          │
                        └────────┬─────────┘    └──────────────────┘
                                 │
                                 ▼
                        ┌──────────────────┐    ┌──────────────────┐
                        │   DuckDB Query   │───▶│   REST API       │
                        │   Engine         │    │  (port 8080)     │
                        └──────────────────┘    └────────┬─────────┘
                                                         │
                                                         ▼
                                                ┌──────────────────┐
                                                │   Vue 3 UI       │
                                                │  (static assets) │
                                                └──────────────────┘
```

## Module Structure

| Module | Purpose | Dependencies |
|--------|---------|-------------|
| `o3000y-model` | Domain records (Span, SpanEvent, etc.) | None |
| `o3000y-ingestion-api` | SpanReceiver interface, BatchConfig | model |
| `o3000y-ingestion-core` | SpanBuffer batching implementation | ingestion-api, storage-api |
| `o3000y-ingestion-grpc` | OTLP gRPC transport + proto mapping | ingestion-api, model |
| `o3000y-storage-api` | StorageWriter, PartitionStrategy | model |
| `o3000y-storage-parquet` | Parquet file serialization | model |
| `o3000y-storage-local` | Local filesystem writer | storage-api, storage-parquet |
| `o3000y-query-engine` | DuckDB wrapper | model |
| `o3000y-query-rest` | Javalin REST API + static UI serving | query-engine |
| `o3000y-app` | Guice assembly + Main entry point | all modules |
| `o3000y-ui` | Vue 3 SPA (builds to static assets) | N/A (JS) |

## Key Design Decisions

- **Java records** for immutable domain model
- **Google Guice** for dependency injection — each module provides an `AbstractModule`
- **Hive partitioning** (`year=/month=/day=/hour=`) for efficient time-based queries
- **DuckDB in-memory** with `read_parquet()` for zero-copy querying of Parquet files
- **ZSTD compression** with dictionary encoding for Parquet storage
- **Shadow JAR** for single-file deployment
