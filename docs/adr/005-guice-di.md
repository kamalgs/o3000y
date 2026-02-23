# ADR-005: Google Guice for Dependency Injection

## Status
Accepted

## Context
We need a DI framework to wire together modules. Options include Guice, Spring, Dagger, manual wiring.

## Decision
Use Google Guice 7.x.

## Rationale
- Lightweight — no classpath scanning, no annotation processing
- Each Gradle module provides its own `AbstractModule`, enabling clean swapping
- Explicit binding (`bind(Interface).to(Impl)`) is easy to understand
- Works well with Javalin (no framework conflicts)
- Constructor injection with `@Inject` is minimal and non-invasive

## Consequences
- Runtime wiring (errors at startup, not compile time like Dagger)
- Less convention-over-configuration than Spring Boot
- Swapping implementations = swapping Guice modules
