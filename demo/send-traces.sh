#!/usr/bin/env bash
#
# Sends sample OTLP/HTTP JSON traces to the TraceQuery ingestion endpoint.
# Simulates a distributed system: api-gateway → order-service → payment-service
#
set -euo pipefail

BASE_URL="${1:-http://localhost:7070}"

# --- Helper: current time in nanoseconds (approximation from epoch millis) ---
now_ns() {
  echo "$(date +%s%N)"
}

# --- Generate deterministic-looking hex IDs ---
TRACE1="aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbb"
TRACE2="ccccccccccccccccdddddddddddddddd"

# Base time: now
BASE=$(now_ns)

send_batch() {
  local payload="$1"
  echo "→ Sending batch to ${BASE_URL}/v1/traces ..."
  curl -s -X POST "${BASE_URL}/v1/traces" \
    -H "Content-Type: application/json" \
    -d "$payload"
  echo ""
}

# ── Trace 1: Successful order flow ──────────────────────────────────────────

PAYLOAD1=$(cat <<EOF
{
  "resourceSpans": [
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "api-gateway"}},
          {"key": "service.version", "value": {"stringValue": "1.2.0"}}
        ]
      },
      "scopeSpans": [{
        "spans": [
          {
            "traceId": "${TRACE1}",
            "spanId": "1000000000000001",
            "name": "POST /api/orders",
            "kind": 2,
            "startTimeUnixNano": "${BASE}",
            "endTimeUnixNano": "$((BASE + 350000000))",
            "status": {"code": 1},
            "attributes": [
              {"key": "http.method", "value": {"stringValue": "POST"}},
              {"key": "http.url", "value": {"stringValue": "/api/orders"}},
              {"key": "http.status_code", "value": {"intValue": "201"}}
            ]
          }
        ]
      }]
    },
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "order-service"}},
          {"key": "service.version", "value": {"stringValue": "2.0.1"}}
        ]
      },
      "scopeSpans": [{
        "spans": [
          {
            "traceId": "${TRACE1}",
            "spanId": "1000000000000002",
            "parentSpanId": "1000000000000001",
            "name": "CreateOrder",
            "kind": 1,
            "startTimeUnixNano": "$((BASE + 5000000))",
            "endTimeUnixNano": "$((BASE + 320000000))",
            "status": {"code": 1},
            "attributes": [
              {"key": "order.id", "value": {"stringValue": "ORD-42"}},
              {"key": "order.total", "value": {"doubleValue": 99.95}}
            ]
          },
          {
            "traceId": "${TRACE1}",
            "spanId": "1000000000000003",
            "parentSpanId": "1000000000000002",
            "name": "ValidateInventory",
            "kind": 1,
            "startTimeUnixNano": "$((BASE + 10000000))",
            "endTimeUnixNano": "$((BASE + 50000000))",
            "status": {"code": 1},
            "attributes": [
              {"key": "item.count", "value": {"intValue": "3"}}
            ]
          }
        ]
      }]
    },
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "payment-service"}},
          {"key": "service.version", "value": {"stringValue": "3.1.0"}}
        ]
      },
      "scopeSpans": [{
        "spans": [
          {
            "traceId": "${TRACE1}",
            "spanId": "1000000000000004",
            "parentSpanId": "1000000000000002",
            "name": "ChargeCard",
            "kind": 3,
            "startTimeUnixNano": "$((BASE + 55000000))",
            "endTimeUnixNano": "$((BASE + 300000000))",
            "status": {"code": 1},
            "attributes": [
              {"key": "payment.method", "value": {"stringValue": "credit_card"}},
              {"key": "payment.amount", "value": {"doubleValue": 99.95}}
            ]
          }
        ]
      }]
    }
  ]
}
EOF
)

send_batch "$PAYLOAD1"

# ── Trace 2: Failed payment ─────────────────────────────────────────────────

BASE2=$((BASE + 1000000000))

PAYLOAD2=$(cat <<EOF
{
  "resourceSpans": [
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "api-gateway"}}
        ]
      },
      "scopeSpans": [{
        "spans": [
          {
            "traceId": "${TRACE2}",
            "spanId": "2000000000000001",
            "name": "POST /api/orders",
            "kind": 2,
            "startTimeUnixNano": "${BASE2}",
            "endTimeUnixNano": "$((BASE2 + 500000000))",
            "status": {"code": 2, "message": "payment declined"},
            "attributes": [
              {"key": "http.method", "value": {"stringValue": "POST"}},
              {"key": "http.url", "value": {"stringValue": "/api/orders"}},
              {"key": "http.status_code", "value": {"intValue": "402"}}
            ]
          }
        ]
      }]
    },
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "order-service"}}
        ]
      },
      "scopeSpans": [{
        "spans": [
          {
            "traceId": "${TRACE2}",
            "spanId": "2000000000000002",
            "parentSpanId": "2000000000000001",
            "name": "CreateOrder",
            "kind": 1,
            "startTimeUnixNano": "$((BASE2 + 5000000))",
            "endTimeUnixNano": "$((BASE2 + 480000000))",
            "status": {"code": 2, "message": "payment declined"},
            "attributes": [
              {"key": "order.id", "value": {"stringValue": "ORD-43"}}
            ]
          }
        ]
      }]
    },
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "payment-service"}}
        ]
      },
      "scopeSpans": [{
        "spans": [
          {
            "traceId": "${TRACE2}",
            "spanId": "2000000000000003",
            "parentSpanId": "2000000000000002",
            "name": "ChargeCard",
            "kind": 3,
            "startTimeUnixNano": "$((BASE2 + 50000000))",
            "endTimeUnixNano": "$((BASE2 + 450000000))",
            "status": {"code": 2, "message": "card declined: insufficient funds"},
            "attributes": [
              {"key": "payment.method", "value": {"stringValue": "credit_card"}},
              {"key": "payment.amount", "value": {"doubleValue": 249.99}},
              {"key": "error", "value": {"boolValue": true}}
            ]
          }
        ]
      }]
    }
  ]
}
EOF
)

send_batch "$PAYLOAD2"

# ── Summary ──────────────────────────────────────────────────────────────────

echo ""
echo "=== Done! Sent 2 traces (7 spans total) ==="
echo ""
echo "Try these queries:"
echo ""
echo "  # List services"
echo "  curl -s ${BASE_URL}/api/v1/services | jq"
echo ""
echo "  # Get trace 1 (successful order)"
echo "  curl -s ${BASE_URL}/api/v1/trace/${TRACE1} | jq"
echo ""
echo "  # Find error spans"
echo "  curl -s ${BASE_URL}/api/v1/query -H 'Content-Type: application/json' \\"
echo "    -d '{\"sql\": \"SELECT service_name, operation_name, status_message FROM spans WHERE status_code = 2\"}' | jq"
echo ""
echo "  # Top spans by duration"
echo "  curl -s ${BASE_URL}/api/v1/query -H 'Content-Type: application/json' \\"
echo "    -d '{\"sql\": \"SELECT service_name, operation_name, duration_us FROM spans ORDER BY duration_us DESC\"}' | jq"
