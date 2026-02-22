# Getting Started

## Prerequisites

- Java 21+
- Docker and Docker Compose (for quick start)
- Node.js 20+ (only for UI development)

## Quick Start with Docker Compose

```bash
cd docker
docker-compose up --build
```

This starts:
- **o3000y** on ports 4317 (gRPC) and 8080 (REST + UI)
- **telemetrygen** sending sample traces

Open http://localhost:8080 to access the UI.

## Quick Start without Docker

```bash
# Build
./gradlew shadowJar

# Run
java -jar o3000y-app/build/libs/o3000y-app-all.jar
```

## Send Test Traces

Using telemetrygen:
```bash
telemetrygen traces --otlp-endpoint localhost:4317 --otlp-insecure --traces 10 --child-spans 3
```

## Query Traces

Via REST API:
```bash
# List services
curl http://localhost:8080/api/v1/services

# SQL query
curl -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -d '{"sql": "SELECT * FROM spans LIMIT 10"}'

# Get a trace by ID
curl http://localhost:8080/api/v1/trace/<trace_id>

# Search spans
curl 'http://localhost:8080/api/v1/search?service=frontend&limit=50'
```
