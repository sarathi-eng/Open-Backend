# OpenCore Backend Platform

[![CI](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-informational)
![Rust](https://img.shields.io/badge/Rust-stable-informational)

> Note: replace `OWNER/REPO` in the CI badge URL with your GitHub repo.

An open-source, production-grade backend platform for building large-scale apps.

This repo is a monorepo containing:
- `gateway/` (Rust) — API Gateway
- `auth-service/` (Java/Spring Boot) — Auth & Identity
- `user-service/` (Java/Spring Boot) — Users, Orgs, Permissions, Audit
- `billing-service/` (Java/Spring Boot) — Billing & Subscriptions
- `notification-service/` (Java/Spring Boot) — Email/SMS/Push/Webhooks (starter)
- `infra/` — Docker + Kubernetes manifests
- `docs/` — architecture, decisions, roadmap

## Quick start (local)

Prereqs:
- Docker + Docker Compose
- Java 17+
- Rust stable

Start infra:

```bash
cd infra/docker
docker compose up -d
```

Notes:
- Postgres is exposed on host port `15432` (container port `5432`).
- Kafka is exposed on host port `9092`.

Run services (in separate terminals):

```bash
# gateway
cd ../../gateway
cargo run

# auth-service
cd ../auth-service
mvn spring-boot:run

# user-service
cd ../user-service
USER_DB_URL='jdbc:postgresql://localhost:15432/opencore' \
USER_DB_USER=opencore \
USER_DB_PASSWORD=opencore \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
mvn spring-boot:run
```

Health checks:
- Gateway: `http://localhost:8080/health`
- Auth: `http://localhost:8081/health`
- User: `http://localhost:8082/health`

## Sponsors

### Why sponsor this

- This repo is intentionally built to be *verifiable*: reproducible local infra, an end-to-end smoke script, and real automated tests (gateway has focused coverage).
- Funding directly buys reliability work: outbox/retries for Kafka, DB/tenancy hardening, and security improvements (server-side session revocation, Redis-backed rate limits).

### What sponsorship supports

- Maintenance time (security patches, dependency updates)
- CI + demo infra costs (observability stack + e2e proof)
- Shipping milestone features (see roadmap)

Key docs:
- `ROADMAP.md` (high-level): [ROADMAP.md](ROADMAP.md)
- Detailed roadmap: [docs/roadmap.md](docs/roadmap.md)
- Failure modes: [docs/failure-modes.md](docs/failure-modes.md)
- v0.1.0 release notes: [docs/release-notes-v0.1.0.md](docs/release-notes-v0.1.0.md)
