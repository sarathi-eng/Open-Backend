# Roadmap

This roadmap is intentionally short and milestone-based (not a wish list).

## v0.1.0 — Local E2E skeleton (current)
- Rust gateway routing + basic middleware + tests
- Auth: email OTP -> JWT access/refresh
- Users/Orgs/Audit: Postgres-backed APIs + migrations
- Kafka domain events + downstream consumers
- Docker Compose infra (Postgres/Kafka/OTel/Prometheus/Grafana)
- Reproducible local smoke test script

## v0.2.0 — DX + reliability
- More automated tests (gateway + service-level)
- Better error surfaces (request IDs, consistent JSON errors)
- Config hardening (env schema, safer defaults)
- Validate docker-compose in CI (no pushing images)

## v0.3.0 — Security + tenancy hardening
- RBAC/permissions (org-scoped)
- Token/session persistence + server-side revocation (DB/Redis)
- Rate limit buckets backed by Redis (instead of in-memory)
- Audit log query improvements (pagination, filters)

## v0.4.0 — Billing + notifications MVP
- Plans/subscriptions primitives
- Webhook retries + DLQ pattern
- Notification retries/backoff + delivery status
