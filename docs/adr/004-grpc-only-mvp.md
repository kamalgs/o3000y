# ADR-004: gRPC-Only Ingestion for MVP

## Status
Accepted

## Context
OpenTelemetry supports multiple transport protocols: gRPC, HTTP/protobuf, HTTP/JSON. We need to choose which to support initially.

## Decision
Support only gRPC (OTLP/gRPC) for the MVP.

## Rationale
- gRPC is the primary OTLP transport and most widely used
- Single transport reduces implementation complexity
- OpenTelemetry SDKs default to gRPC
- Protobuf schema provides strong typing
- Compatible with `telemetrygen` for testing

## Consequences
- Clients using HTTP/protobuf or HTTP/JSON need a collector proxy
- Future work: add HTTP/protobuf endpoint for broader compatibility
