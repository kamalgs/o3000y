# ADR-003: Javalin for REST API

## Status
Accepted

## Context
We need a lightweight HTTP framework for the REST API. Options include Javalin, Spring Boot, Micronaut, Vert.x.

## Decision
Use Javalin 6.x.

## Rationale
- Minimal footprint — no annotation processing, no DI framework assumptions
- Works well with Guice (manual injection rather than framework-managed beans)
- Simple API: `app.get("/path", handler)`
- Built-in static file serving for the Vue UI
- Good for microservices where Spring Boot is overkill

## Consequences
- Less ecosystem support than Spring Boot
- No built-in validation framework (manual validation)
- Limited middleware ecosystem
