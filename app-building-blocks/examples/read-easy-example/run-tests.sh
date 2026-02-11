#!/bin/bash

# =============================================================================
# Read-Easy Example - API Test Script
# =============================================================================

PORT=8085
BASE_URL="http://localhost:$PORT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; }

test_endpoint() {
    local name="$1"
    local url="$2"
    local data="${3:-{}}"
    local expected="${4:-200}"

    local response
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$data" "$url" 2>/dev/null) || true

    local status=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')

    if [ "$status" = "$expected" ]; then
        log_pass "$name (HTTP $status)"
        echo "    ${body:0:80}..."
    else
        log_fail "$name - Expected $expected, got $status"
        echo "    ${body:0:150}"
    fi
}

echo ""
echo "=============================================="
echo "  READ-EASY EXAMPLE - API TESTS"
echo "=============================================="
echo ""

# Check if server is running
if ! curl -s "$BASE_URL/read/list?queryId=users.findAll" -X POST -H "Content-Type: application/json" -d '{}' > /dev/null 2>&1; then
    log_fail "Server not running on $BASE_URL"
    echo ""
    echo "Start the server first:"
    echo "  mvn spring-boot:run"
    echo ""
    exit 1
fi

log_info "Server is running on $BASE_URL"
echo ""

# JDBC Tests
log_info "Testing JDBC queries (users namespace)..."
test_endpoint "GET /read/list - Find all users" "$BASE_URL/read/list?queryId=users.findAll"
test_endpoint "GET /read/one - Find one user" "$BASE_URL/read/one?queryId=users.findAll"
test_endpoint "GET /read/count - Count users" "$BASE_URL/read/count?queryId=users.findAll"
test_endpoint "GET /read/page - Paginated" "$BASE_URL/read/page?queryId=users.findAll&pageNum=1&pageSize=2"

echo ""

# MongoDB Tests
log_info "Testing MongoDB queries (mongo-users namespace)..."
test_endpoint "GET /read/list - Find all mongo users" "$BASE_URL/read/list?queryId=mongo-users.findAll"
test_endpoint "GET /read/one - Find one mongo user" "$BASE_URL/read/one?queryId=mongo-users.findAll"
test_endpoint "GET /read/count - Count mongo users" "$BASE_URL/read/count?queryId=mongo-users.findAll"

echo ""

# Error Handling
log_info "Testing error handling..."
test_endpoint "Invalid queryId returns 400" "$BASE_URL/read/one?queryId=invalid.query" '{}' "400"
test_endpoint "Missing queryId returns 400" "$BASE_URL/read/one" '{}' "400"

echo ""
echo "=============================================="
echo "  TESTS COMPLETE"
echo "=============================================="
