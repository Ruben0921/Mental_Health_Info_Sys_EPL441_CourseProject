#!/usr/bin/env bash
# Hit every published REST endpoint plus 401/403 auth checks.
# Prerequisites: MySQL seeded with scripts/seed-smoke.sql, config.properties set, server running.
#
#   export BASE_URL=http://localhost:8080   # optional
#   ./scripts/smoke-all-endpoints.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"

CLINICAL_USER="${CLINICAL_USER:-clinical1}"
CLINICAL_PASS="${CLINICAL_PASS:-secret}"
RECEPTION_USER="${RECEPTION_USER:-reception1}"
RECEPTION_PASS="${RECEPTION_PASS:-secret}"
RECORDS_USER="${RECORDS_USER:-records1}"
RECORDS_PASS="${RECORDS_PASS:-secret}"

MISSED_DATE="${MISSED_DATE:-2026-01-15}"
NEW_APPT_DATE="${NEW_APPT_DATE:-2026-12-20}"

TMP_BODY="${TMPDIR:-/tmp}/mh_smoke_body_$$"

cleanup() { rm -f "$TMP_BODY"; }
trap cleanup EXIT

fail() {
  echo "SMOKE FAIL: $*" >&2
  exit 1
}

expect_code() {
  local expected="$1"
  shift
  local actual
  actual=$(curl -sS -o "$TMP_BODY" -w "%{http_code}" "$@")
  if [[ "$actual" != "$expected" ]]; then
    echo "Expected HTTP $expected, got $actual. Response:" >&2
    cat "$TMP_BODY" >&2
    fail "request failed"
  fi
}

echo "== GET /patients (Clinical) =="
expect_code 200 -u "${CLINICAL_USER}:${CLINICAL_PASS}" "${BASE_URL}/patients"

echo "== GET /patients/1 (Clinical) =="
expect_code 200 -u "${CLINICAL_USER}:${CLINICAL_PASS}" "${BASE_URL}/patients/1"

echo "== PUT /patients/1 (Clinical) =="
expect_code 200 -u "${CLINICAL_USER}:${CLINICAL_PASS}" \
  -X PUT -H "Content-Type: application/json" \
  -d '{"patientId":1,"firstName":"Jane","lastName":"Doe","address":"1 Test St","homeless":false,"riskStatus":null,"deceased":false,"selfHarmHistory":false}' \
  "${BASE_URL}/patients/1"

echo "== POST /appointments (Receptionist) =="
POST_JSON="{\"patientId\":1,\"clinicId\":1,\"staffId\":1,\"appointmentDate\":\"${NEW_APPT_DATE}\",\"type\":\"Drop-in\",\"status\":\"Pending\",\"recordsUpdated\":false}"
expect_code 200 -u "${RECEPTION_USER}:${RECEPTION_PASS}" \
  -X POST -H "Content-Type: application/json" -d "$POST_JSON" \
  "${BASE_URL}/appointments"

NEW_APPT_ID=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['appointmentId'])" "$TMP_BODY")

echo "== PUT /appointments/${NEW_APPT_ID}/attendance (Receptionist) =="
expect_code 204 -u "${RECEPTION_USER}:${RECEPTION_PASS}" \
  -X PUT -H "Content-Type: application/json" -d '{"status":"Attended"}' \
  "${BASE_URL}/appointments/${NEW_APPT_ID}/attendance"

echo "== GET /appointments/missed?date=${MISSED_DATE} (Receptionist) =="
expect_code 200 -u "${RECEPTION_USER}:${RECEPTION_PASS}" \
  "${BASE_URL}/appointments/missed?date=${MISSED_DATE}"

echo "== GET /appointments/pending-records (Receptionist) =="
expect_code 200 -u "${RECEPTION_USER}:${RECEPTION_PASS}" \
  "${BASE_URL}/appointments/pending-records"

echo "== GET /reports/patients-per-clinic (Medical_Records) =="
expect_code 200 -u "${RECORDS_USER}:${RECORDS_PASS}" \
  "${BASE_URL}/reports/patients-per-clinic"

echo "== GET /reports/prescription-stats (Medical_Records) =="
expect_code 200 -u "${RECORDS_USER}:${RECORDS_PASS}" \
  "${BASE_URL}/reports/prescription-stats"

echo "== POST /reports/change-requests (Medical_Records) =="
expect_code 201 -u "${RECORDS_USER}:${RECORDS_PASS}" \
  -X POST -H "Content-Type: application/json" \
  -d '{"rawPatientData":"{\"patientId\":1}","requestedChanges":"Update address"}' \
  "${BASE_URL}/reports/change-requests"

echo "== Negative: GET /patients without auth (expect 401) =="
expect_code 401 "${BASE_URL}/patients"

echo "== Negative: GET /reports/patients-per-clinic as Clinical (expect 403) =="
expect_code 403 -u "${CLINICAL_USER}:${CLINICAL_PASS}" \
  "${BASE_URL}/reports/patients-per-clinic"

echo "SMOKE OK: all endpoints and auth checks passed."
