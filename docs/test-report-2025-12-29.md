# Test report — 2025-12-29

## Scope

This report covers the repo’s automated test commands across all modules:

- Rust gateway: `cargo test`
- Spring Boot services: `mvn test`

It also includes a short end-to-end smoke verification (runtime) that exercises gateway routing + Kafka event flow.

## Environment

- Date: 2025-12-29
- OS: Linux

## Automated tests

### Gateway (Rust)

Command:

- `cd opencore/gateway && cargo test`

Result:

- PASS (exit code 0)
- Note: gateway includes **6 focused tests** (routing, auth middleware, rate limiting).

### auth-service (Spring Boot)

Command:

- `cd opencore/auth-service && mvn test`

Result:

- PASS (BUILD SUCCESS)

### user-service (Spring Boot)

Command:

- `cd opencore/user-service && mvn test`

Result:

- PASS (BUILD SUCCESS)

### billing-service (Spring Boot)

Command:

- `cd opencore/billing-service && mvn test`

Result:

- PASS (BUILD SUCCESS)

### notification-service (Spring Boot)

Command:

- `cd opencore/notification-service && mvn test`

Result:

- PASS (BUILD SUCCESS)

## Build warnings (non-fatal)

- Maven plugin versions are pinned across services.
- No build-breaking warnings were observed during the test run.

## End-to-end smoke verification (runtime)

This is not part of `mvn test`/`cargo test`, but confirms real integration:

- Gateway routes to services: `/health` and `/{service}/actuator/health` return `UP`.
- Auth OTP flow returns tokens.
- User create + org create succeed.
- Kafka propagation confirmed by consumer logs:
  - billing-service consumed `UserCreated`.
  - notification-service consumed `UserCreated`, `PaymentSucceeded`, `SubscriptionExpired`.

Repro steps:

- Script: `opencore/scripts/smoke-e2e.sh`
- Doc: `opencore/docs/smoke-test.md`

## Overall status

- Automated test matrix: PASS ✅
- E2E smoke (runtime): PASS ✅
