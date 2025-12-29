# Failure modes (and what happens today)

This document is candid by design: sponsors/users should know what breaks, what degrades, and what we plan to harden next.

## Kafka is down / unreachable

**Symptoms**
- Services that publish events (user-service, billing-service) may log producer errors.
- Consumers (billing-service, notification-service) stop receiving new events.

**What happens today**
- Event publishing uses `kafka.send(...)` without waiting for broker ack.
- The HTTP request that triggered the publish typically still returns `200` if the DB work succeeded.
- Result: the system can become *eventually inconsistent* (write succeeded, event not delivered).

**Impact**
- Downstream projections/side-effects (billing customer creation, notifications) may be skipped.

**Planned hardening**
- Outbox pattern (DB transaction writes an “outbox” row; background publisher retries until delivered).
- Dead-letter + replay tooling.

## Postgres is down / partial DB outage

**Symptoms**
- user-service endpoints that hit the DB (users/orgs/audit) return `5xx`.
- Flyway migration/boot may fail at startup.

**What happens today**
- DB-backed operations fail the request (exception -> `5xx`).
- No caching/fallbacks are implemented.

**Planned hardening**
- Better error mapping (consistent JSON errors).
- Operational runbook + health/readiness split.

## Gateway restarts

**Symptoms**
- In-flight requests can fail with transient `502`.

**What happens today**
- In-memory rate limit buckets reset (clients temporarily get a “fresh” bucket).
- Auth enforcement is minimal and currently based on gateway config:
  - If `OPENCORE_JWT_SECRET` is set, gateway validates JWTs.
  - If not set, gateway only checks that a Bearer token is present for protected routes.

**Planned hardening**
- Persist rate limit state (Redis).
- Standardize JWT verification (always-on in prod; configurable for local dev).

## What to do during a demo

- If Kafka is down: infra `docker compose up -d` should restore it; rerun the smoke script.
- If Postgres is down: check port `15432` conflicts and Postgres container health.
- If gateway restarts: rerun the failing call; check `gateway` logs for upstream health.
