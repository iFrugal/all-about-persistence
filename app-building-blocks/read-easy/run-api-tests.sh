#!/bin/bash

# =============================================================================
# Read-Easy API Integration Test Script
# Starts a Spring Boot app with mock readers, tests APIs, then shuts down
# =============================================================================

# Don't exit on error - we want to run all tests
# set -e

PORT=9999
BASE_URL="http://localhost:$PORT"
APP_PID=""
PASSED=0
FAILED=0
TOTAL=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; ((PASSED++)); ((TOTAL++)); }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; ((FAILED++)); ((TOTAL++)); }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

cleanup() {
    if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
        log_info "Stopping application (PID: $APP_PID)..."
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
    fi
}

trap cleanup EXIT

# =============================================================================
# Build
# =============================================================================
log_info "Building project..."
cd "$(dirname "$0")"

mvn compile test-compile -q -DskipTests
if [ $? -ne 0 ]; then
    log_fail "Build failed"
    exit 1
fi
log_info "Build successful"

# =============================================================================
# Start Application
# =============================================================================
log_info "Starting application on port $PORT..."

mvn exec:java \
    -Dexec.mainClass="lazydevs.readeasy.standalone.StandaloneTestApplication" \
    -Dexec.classpathScope=test \
    -Dexec.args="--spring.profiles.active=standalone" \
    -q &
APP_PID=$!

log_info "Application starting (PID: $APP_PID)..."

# Wait for app to be ready
MAX_WAIT=60
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if curl -s "$BASE_URL/read/list?queryId=users.findAll" -X POST -H "Content-Type: application/json" -d '{}' 2>&1 | grep -q "id"; then
        log_info "Application is ready!"
        break
    fi
    sleep 2
    ((WAITED+=2))
    echo -n "."
done
echo ""

if [ $WAITED -ge $MAX_WAIT ]; then
    log_fail "Application failed to start within ${MAX_WAIT}s"
    exit 1
fi

# =============================================================================
# Test Functions
# =============================================================================

test_endpoint() {
    local name="$1"
    local url="$2"
    local method="${3:-POST}"
    local data="${4:-{}}"
    local expected_status="${5:-200}"

    local response
    local status
    local body

    response=$(curl -s -w "\n%{http_code}" -X "$method" \
        -H "Content-Type: application/json" \
        -d "$data" \
        "$url" 2>/dev/null) || true

    status=$(echo "$response" | tail -1) || true
    body=$(echo "$response" | sed '$d') || true

    if [ "$status" = "$expected_status" ]; then
        log_pass "$name (HTTP $status)"
        echo "    Response: ${body:0:100}..."
    else
        log_fail "$name - Expected HTTP $expected_status, got $status"
        echo "    Response: ${body:0:200}"
    fi
    return 0
}

test_json_field() {
    local name="$1"
    local url="$2"
    local data="$3"
    local field="$4"

    local response
    response=$(curl -s -X POST -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null) || true

    if echo "$response" | grep -q "\"$field\""; then
        log_pass "$name - field '$field' present"
    else
        log_fail "$name - field '$field' missing"
        echo "    Response: ${response:0:200}"
    fi
    return 0
}

# =============================================================================
# Run Tests
# =============================================================================

echo ""
echo "=============================================="
echo "  READ-EASY API INTEGRATION TESTS"
echo "=============================================="
echo ""

# ----- JDBC Tests (users namespace) -----
log_info "Testing JDBC queries (users namespace)..."

test_endpoint "POST /read/list - Find all users" \
    "$BASE_URL/read/list?queryId=users.findAll" \
    "POST" '{}'

test_endpoint "POST /read/page - Paginated users" \
    "$BASE_URL/read/page?queryId=users.findAll&pageNum=1&pageSize=2" \
    "POST" '{}'

test_endpoint "POST /read/count - Count users" \
    "$BASE_URL/read/count?queryId=users.findAll" \
    "POST" '{}'

test_endpoint "POST /read/one - Find one user" \
    "$BASE_URL/read/one?queryId=users.findAll" \
    "POST" '{}'

test_json_field "Response has 'id' field" \
    "$BASE_URL/read/list?queryId=users.findAll" \
    '{}' "id"

test_json_field "Response has 'name' field" \
    "$BASE_URL/read/list?queryId=users.findAll" \
    '{}' "name"

# ----- MongoDB Tests (mongo-users namespace) -----
echo ""
log_info "Testing MongoDB queries (mongo-users namespace)..."

test_endpoint "POST /read/list - Find all mongo users" \
    "$BASE_URL/read/list?queryId=mongo-users.findAll" \
    "POST" '{}'

test_endpoint "POST /read/one - Find one mongo user" \
    "$BASE_URL/read/one?queryId=mongo-users.findAll" \
    "POST" '{}'

test_endpoint "POST /read/count - Count mongo users" \
    "$BASE_URL/read/count?queryId=mongo-users.findAll" \
    "POST" '{}'

# ----- Error Handling Tests -----
echo ""
log_info "Testing error handling..."

test_endpoint "Invalid query ID returns 400" \
    "$BASE_URL/read/one?queryId=nonexistent.query" \
    "POST" '{}' "400"

test_endpoint "Missing queryId returns 400" \
    "$BASE_URL/read/one" \
    "POST" '{}' "400"

# ----- Pagination Tests -----
echo ""
log_info "Testing pagination..."

test_endpoint "Page with custom size" \
    "$BASE_URL/read/page?queryId=users.findAll&pageNum=1&pageSize=1" \
    "POST" '{}'

# Verify page structure
response=$(curl -s -X POST -H "Content-Type: application/json" -d '{}' \
    "$BASE_URL/read/page?queryId=users.findAll&pageNum=1&pageSize=2" 2>/dev/null)

if echo "$response" | grep -q '"pageNum"' && echo "$response" | grep -q '"data"'; then
    log_pass "Page response has correct structure (pageNum, data)"
else
    log_fail "Page response missing expected fields"
    echo "    Response: $response"
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo "=============================================="
echo "  TEST SUMMARY"
echo "=============================================="
echo -e "  Total:  $TOTAL"
echo -e "  ${GREEN}Passed: $PASSED${NC}"
echo -e "  ${RED}Failed: $FAILED${NC}"
echo "=============================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi
