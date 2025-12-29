# Architecture

## High-level components
- API Gateway (Rust): routing, auth verification, rate limiting (planned), structured request logs.
- Auth & Identity (Spring Boot): email+OTP login, OAuth2 providers, JWT access/refresh, session revocation (phased).
- User & Organization (Spring Boot): multi-tenant orgs, team membership, permissions, audit logs.
- Billing & Subscriptions (Spring Boot): plans, subscriptions, usage metering, webhooks, invoices (phased).
- Notification Platform (Spring Boot): email/SMS/push/webhooks, templates, retries, user preferences (phased).
- Event Bus (Kafka): domain events for loose coupling.
- Admin Control Plane (planned): operational controls and insights.

## Data stores
- PostgreSQL: system-of-record for identities, orgs, subscriptions.
- Redis: caching, rate limit buckets, OTP/session storage (planned).

## Observability
Non-negotiable:
- Structured logs
- Metrics
- Tracing

Implementation:
- OpenTelemetry SDK in services (OTLP exporter)
- OpenTelemetry Collector in `infra/docker/`
- Prometheus + Grafana for metrics visualization

## Security model (summary)
- JWT access tokens (short TTL)
- Refresh tokens (rotation; persistent storage planned)
- RBAC roles: Admin/User/Service
- Rate limiting + brute-force protection (phased, starting in Auth)
