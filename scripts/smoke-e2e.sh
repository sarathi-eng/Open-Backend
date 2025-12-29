#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"

require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "missing required command: $1" >&2
    exit 1
  }
}

require curl
require python3

check_health() {
  local url="$1"
  curl -sf "$url" >/dev/null
}

echo "== OpenCore E2E smoke =="
check_health "$BASE/health" || { echo "gateway not healthy: $BASE/health" >&2; exit 1; }
check_health "$BASE/auth/actuator/health" || { echo "auth not healthy via gateway" >&2; exit 1; }
check_health "$BASE/users/actuator/health" || { echo "user not healthy via gateway" >&2; exit 1; }
check_health "$BASE/billing/actuator/health" || { echo "billing not healthy via gateway" >&2; exit 1; }
check_health "$BASE/notifications/actuator/health" || { echo "notification not healthy via gateway" >&2; exit 1; }

EMAIL="smoke+$(date +%s)@example.com"

# OTP request
curl -sS -X POST "$BASE/auth/v1/auth/login/email-otp/request" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\"}" | tee /tmp/opencore_otp_request.json >/dev/null

REQ_ID=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_otp_request.json"))["requestId"])')
OTP=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_otp_request.json"))["devOtp"])')

# OTP verify
curl -sS -X POST "$BASE/auth/v1/auth/login/email-otp/verify" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"otp\":\"$OTP\",\"requestId\":\"$REQ_ID\",\"deviceId\":\"smoke-device\"}" | tee /tmp/opencore_otp_verify.json >/dev/null

ACCESS=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_otp_verify.json"))["accessToken"])')

# Create user
curl -sS -X POST "$BASE/users/v1/users" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"email\":\"$EMAIL\"}" | tee /tmp/opencore_create_user.json >/dev/null

USER_ID=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_create_user.json"))["userId"])')

# Create org
ORG_NAME="Smoke Org $(date +%s)"
curl -sS -X POST "$BASE/users/v1/orgs" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"name\":\"$ORG_NAME\",\"ownerUserId\":\"$USER_ID\"}" | tee /tmp/opencore_create_org.json >/dev/null

ORG_ID=$(python3 -c 'import json;print(json.load(open("/tmp/opencore_create_org.json"))["orgId"])')

# Emit billing events
PAYMENT_ID="pay_$(date +%s)"
SUB_ID="sub_$(date +%s)"

curl -sS -X POST "$BASE/billing/v1/billing/payments/succeeded" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"userId\":\"$USER_ID\",\"orgId\":\"$ORG_ID\",\"amountCents\":1999,\"currency\":\"USD\",\"externalPaymentId\":\"$PAYMENT_ID\"}" \
  -o /dev/null -w "PaymentSucceeded HTTP:%{http_code}\n"

curl -sS -X POST "$BASE/billing/v1/billing/subscriptions/expired" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"userId\":\"$USER_ID\",\"orgId\":\"$ORG_ID\",\"subscriptionId\":\"$SUB_ID\",\"reason\":\"smoke-test\"}" \
  -o /dev/null -w "SubscriptionExpired HTTP:%{http_code}\n"

echo "EMAIL=$EMAIL"
echo "USER_ID=$USER_ID"
echo "ORG_ID=$ORG_ID"
echo "PAYMENT_ID=$PAYMENT_ID"
echo "SUB_ID=$SUB_ID"

echo "== Kafka proof (optional log grep) =="
if [ -f /tmp/opencore-billing-service.log ]; then
  grep -n "consumed event" /tmp/opencore-billing-service.log | tail -n 3 || true
else
  echo "missing /tmp/opencore-billing-service.log (restart billing with nohup redirect to capture)"
fi

if [ -f /tmp/opencore-notification-service.log ]; then
  grep -n "notify: consumed event" /tmp/opencore-notification-service.log | tail -n 6 || true
else
  echo "missing /tmp/opencore-notification-service.log (restart notification with nohup redirect to capture)"
fi
