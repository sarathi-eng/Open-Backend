# Release notes — v0.1.0

v0.1.0 is a sponsor-ready “it runs locally end-to-end” baseline: HTTP routing, DB persistence, and Kafka event flow with observability scaffolding.

## What works

- Gateway (Rust/Axum)
  - Routes `/auth/*`, `/users/*`, `/billing/*`, `/notifications/*` to downstream services
  - Minimal auth middleware (Bearer required for protected surfaces)
  - Optional JWT verification when `OPENCORE_JWT_SECRET` is set
  - In-memory token-bucket rate limiting
  - Automated tests in `gateway` (6 focused tests)

- auth-service (Spring Boot)
  - Email OTP request/verify (dev OTP is returned for local testing)
  - JWT access/refresh token minting

- user-service (Spring Boot + Postgres)
  - Users + organizations + memberships
  - Audit log persistence (`jsonb` metadata)
  - Publishes domain events to Kafka

- billing-service + notification-service
  - Consume user events
  - Billing endpoints publish billing events; notification consumes user + billing events

- Infra + observability
  - Docker Compose includes Postgres, Kafka, OTel Collector, Prometheus, Grafana
  - Smoke test script validates the full flow

## Known limitations (intentionally explicit)

- Event publishing does not wait for broker ack; if Kafka is down, HTTP may still return `200` and the event may be dropped.
- Rate limiting and some auth/session mechanics are in-memory (restart resets state).
- No production deployment packaging yet (Kubernetes manifests are placeholders).
- No license file is included yet (needs a project decision).

## Upgrade / migration notes

- This is the initial cut; no migrations are needed beyond `V1__init.sql`.

## Next up

- See [docs/roadmap.md](roadmap.md) (v0.2+ hardening milestones).
