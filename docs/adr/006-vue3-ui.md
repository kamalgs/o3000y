# ADR-006: Vue 3 for UI

## Status
Accepted

## Context
We need a frontend framework for the trace explorer UI. Options include Vue, React, Svelte, or server-rendered HTML.

## Decision
Use Vue 3 with Vite, TypeScript, and Tailwind CSS.

## Rationale
- Vue 3 Composition API provides clean, reactive state management
- Vite gives fast builds and hot module replacement
- TypeScript catches errors at compile time
- Tailwind CSS avoids writing custom CSS for utility-first styling
- Build output is static assets served by Javalin — no SSR needed

## Consequences
- Requires Node.js for building (not needed at runtime)
- Additional build step in CI/CD
- Hash-based routing (no server-side SPA fallback needed)
