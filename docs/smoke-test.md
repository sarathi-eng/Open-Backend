# End-to-end smoke test (local)

This is a sponsor-ready, reproducible local demo that validates:

- Gateway routing (HTTP)
- Auth token minting (email OTP)
- User + org APIs (Postgres-backed)
- Kafka domain events (user-service + billing-service publish)
- Downstream consumption (billing-service + notification-service)

## Prereqs

- Docker + Docker Compose
- Java 21+
- Rust stable
- `curl`
- `python3`

## 1) Start infra

```bash
cd infra/docker
docker compose up -d
```

Notes:
- Postgres is exposed on host port `15432` (container port `5432`).
- Kafka is exposed on host port `9092`.

## 2) Start services

Run each in a separate terminal (or use `nohup` as shown).

### Gateway

```bash
cd gateway
cargo run
```

### Auth service

```bash
cd auth-service
mvn -q spring-boot:run -DskipTests
```

### User service (points at Docker Postgres)

```bash
cd user-service
USER_DB_URL='jdbc:postgresql://localhost:15432/opencore' \
USER_DB_USER=opencore \
USER_DB_PASSWORD=opencore \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
mvn -q spring-boot:run -DskipTests
```

### Billing + Notification

```bash
cd billing-service
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 mvn -q spring-boot:run -DskipTests

cd ../notification-service
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 mvn -q spring-boot:run -DskipTests
```

## 3) Run the smoke flow

Option A: run the helper script.

```bash
./scripts/smoke-e2e.sh
```

Option B: manual curl sequence (same as the script).

```bash
BASE=http://localhost:8080
EMAIL="smoke+$(date +%s)@example.com"

# OTP request
curl -sS -X POST "$BASE/auth/v1/auth/login/email-otp/request" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\"}" | tee /tmp/opencore_otp_request.json

REQ_ID=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_otp_request.json"))["requestId"])')
OTP=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_otp_request.json"))["devOtp"])')

# OTP verify -> tokens
curl -sS -X POST "$BASE/auth/v1/auth/login/email-otp/verify" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"otp\":\"$OTP\",\"requestId\":\"$REQ_ID\",\"deviceId\":\"smoke-device\"}" | tee /tmp/opencore_otp_verify.json

ACCESS=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_otp_verify.json"))["accessToken"])')

# Create user
curl -sS -X POST "$BASE/users/v1/users" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"email\":\"$EMAIL\"}" | tee /tmp/opencore_create_user.json

USER_ID=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_create_user.json"))["userId"])')

# Create org
ORG_NAME="Smoke Org $(date +%s)"
curl -sS -X POST "$BASE/users/v1/orgs" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"name\":\"$ORG_NAME\",\"ownerUserId\":\"$USER_ID\"}" | tee /tmp/opencore_create_org.json

ORG_ID=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_create_org.json"))["orgId"])')

# Emit billing events
PAYMENT_ID="pay_$(date +%s)"
SUB_ID="sub_$(date +%s)"

curl -sS -X POST "$BASE/billing/v1/billing/payments/succeeded" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"userId\":\"$USER_ID\",\"orgId\":\"$ORG_ID\",\"amountCents\":1999,\"currency\":\"USD\",\"externalPaymentId\":\"$PAYMENT_ID\"}"

curl -sS -X POST "$BASE/billing/v1/billing/subscriptions/expired" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"userId\":\"$USER_ID\",\"orgId\":\"$ORG_ID\",\"subscriptionId\":\"$SUB_ID\",\"reason\":\"smoke-test\"}"
```

## 4) Expected results

- `POST /auth/.../request` returns `requestId` and `devOtp`.
- `POST /auth/.../verify` returns `accessToken`, `refreshToken`.
- `POST /users/v1/users` returns `{"userId":"..."}`.
- `POST /users/v1/orgs` returns `{"orgId":"..."}`.
- Billing endpoints return `HTTP 200`.

Kafka consumption proof should show up in logs:

- Billing service consumes `UserCreated`.
- Notification service consumes:
  - `UserCreated`
  - `PaymentSucceeded`
  - `SubscriptionExpired`
