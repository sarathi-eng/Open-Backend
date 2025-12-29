# Architecture Decision Records (ADRs)

## ADR-0001: Monorepo structure
**Decision**: Keep gateway and services in a single `opencore/` monorepo.
**Why**: Shared infra, consistent CI, easier cross-service refactors.

## ADR-0002: Rust gateway
**Decision**: Rust for the API Gateway.
**Why**: low latency, high throughput, memory safety.

## ADR-0003: Spring Boot core services
**Decision**: Java Spring Boot for core services.
**Why**: team familiarity, mature ecosystem, rapid delivery, production ergonomics.

## ADR-0004: Kafka event bus
**Decision**: Kafka for domain events.
**Why**: scalable pub/sub, replay, enterprise-grade patterns.

## ADR-0005: OpenTelemetry
**Decision**: Standardize on OpenTelemetry for logs/metrics/traces.
**Why**: vendor-neutral instrumentation and consistent observability.
