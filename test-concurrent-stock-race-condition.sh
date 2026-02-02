#!/bin/bash
# ==========================================
# CONCURRENT STOCK DEBIT TEST
# Tests race condition in inventory management
# ==========================================
#
# PURPOSE: Validate that simultaneous order fulfillment doesn't cause
#          inventory over-selling or negative stock
#
# SCENARIO: Two operators at WS-7 both try to fulfill orders for the
#           last remaining units of a product at the same time
#
# EXPECTED BEHAVIOR: One succeeds, one fails with insufficient stock error
# BUG RISK: Without proper locking, both may debit stock leading to negative inventory
#
# USAGE: ./test-concurrent-stock-race-condition.sh
#
# REQUIREMENTS:
#   - GNU Parallel (install: sudo apt-get install parallel)
#   - jq (sudo apt-get install jq)
#   - curl
#

set -e

# ==========================================
# COLORS & FORMATTING
# ==========================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ==========================================
# CONFIGURATION
# ==========================================

BASE_URL="${API_BASE_URL:-http://localhost:1011/api}"
WORKSTATION_ID=7  # Plant Warehouse
PRODUCT_ID=1      # LEGO Model Car

# Test parameters
INITIAL_STOCK=5   # Set stock to exactly 5 units
ORDER_1_QTY=5     # First order wants 5 units
ORDER_2_QTY=5     # Second order wants 5 units (should fail)

# Results tracking
TESTS_PASSED=0
TESTS_FAILED=0
declare -a TEST_RESULTS

# ==========================================
# UTILITY FUNCTIONS
# ==========================================

print_header() {
    echo -e "\n${BOLD}${CYAN}========================================${NC}"
    echo -e "${BOLD}${CYAN}$1${NC}"
    echo -e "${BOLD}${CYAN}========================================${NC}\n"
}

print_test() {
    echo -e "\n${BOLD}${BLUE}[TEST $1]${NC} $2"
    echo -e "${BLUE}────────────────────────────────────────${NC}"
}

print_step() {
    echo -e "${CYAN}  → Step:${NC} $1"
}

print_info() {
    echo -e "${BLUE}  ℹ  Info:${NC} $1"
}

print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}  ✅ PASS:${NC} $2"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        TEST_RESULTS+=("${GREEN}✅${NC} $2")
    else
        echo -e "${RED}  ❌ FAIL:${NC} $2"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        TEST_RESULTS+=("${RED}❌${NC} $2")
    fi
}

# ==========================================
# CHECK PREREQUISITES
# ==========================================

print_header "CONCURRENT STOCK RACE CONDITION TEST"

echo "Checking prerequisites..."

# Check if GNU Parallel is installed
if ! command -v parallel &> /dev/null; then
    echo -e "${RED}ERROR: GNU Parallel is not installed${NC}"
    echo "Install with: sudo apt-get install parallel"
    exit 1
fi

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${RED}ERROR: jq is not installed${NC}"
    echo "Install with: sudo apt-get install jq"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites met${NC}"

# ==========================================
# AUTHENTICATION
# ==========================================

print_test "0" "Authentication & Setup"

print_step "Login as Plant Warehouse operator"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"warehouse_operator","password":"password"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

if [ "$TOKEN" != "null" ] && [ "$TOKEN" != "" ]; then
    print_result 0 "Authentication successful"
else
    print_result 1 "Authentication failed"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

# ==========================================
# TEST SETUP: SET EXACT STOCK LEVEL
# ==========================================

print_step "Set exact stock level to $INITIAL_STOCK units"

# First, get current stock
CURRENT_STOCK_RESPONSE=$(curl -s -X GET "$BASE_URL/stock?workstationId=$WORKSTATION_ID&itemType=PRODUCT&itemId=$PRODUCT_ID" \
    -H "Authorization: Bearer $TOKEN")

CURRENT_STOCK=$(echo $CURRENT_STOCK_RESPONSE | jq -r '.[0].quantity // 0')
print_info "Current stock: $CURRENT_STOCK units"

# Calculate adjustment needed
STOCK_DELTA=$((INITIAL_STOCK - CURRENT_STOCK))

if [ $STOCK_DELTA -ne 0 ]; then
    print_step "Adjusting stock by $STOCK_DELTA to reach $INITIAL_STOCK units"
    
    ADJUST_RESPONSE=$(curl -s -X POST "$BASE_URL/stock/adjust" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"workstationId\": $WORKSTATION_ID,
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"delta\": $STOCK_DELTA,
            \"reason\": \"TEST_SETUP\",
            \"notes\": \"Setting stock for concurrent test\"
        }")
    
    NEW_STOCK=$(echo $ADJUST_RESPONSE | jq -r '.quantity')
    
    if [ "$NEW_STOCK" == "$INITIAL_STOCK" ]; then
        print_result 0 "Stock set to $INITIAL_STOCK units"
    else
        print_result 1 "Failed to set stock (Expected: $INITIAL_STOCK, Got: $NEW_STOCK)"
        exit 1
    fi
else
    print_result 0 "Stock already at $INITIAL_STOCK units"
fi

# ==========================================
# TEST 1: SEQUENTIAL BASELINE (SHOULD PASS)
# ==========================================

print_test "1" "Sequential Order Processing (Baseline)"

print_step "Create first order (5 units)"
ORDER_1=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": $ORDER_1_QTY
        }],
        \"notes\": \"Concurrent test - Order 1 (sequential)\"
    }")

ORDER_1_ID=$(echo $ORDER_1 | jq -r '.id')
print_info "Order 1 ID: $ORDER_1_ID"

print_step "Create second order (5 units)"
ORDER_2=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": $ORDER_2_QTY
        }],
        \"notes\": \"Concurrent test - Order 2 (sequential)\"
    }")

ORDER_2_ID=$(echo $ORDER_2 | jq -r '.id')
print_info "Order 2 ID: $ORDER_2_ID"

print_step "Confirm and fulfill Order 1 (should succeed)"
curl -s -X PUT "$BASE_URL/customer-orders/$ORDER_1_ID/confirm" \
    -H "Authorization: Bearer $TOKEN" > /dev/null

FULFILL_1=$(curl -s -X POST "$BASE_URL/customer-orders/$ORDER_1_ID/complete" \
    -H "Authorization: Bearer $TOKEN")

FULFILL_1_STATUS=$(echo $FULFILL_1 | jq -r '.status')

if [ "$FULFILL_1_STATUS" == "COMPLETED" ]; then
    print_result 0 "Order 1 fulfilled successfully (5 units debited)"
else
    print_result 1 "Order 1 fulfillment failed (Status: $FULFILL_1_STATUS)"
fi

print_step "Check stock after Order 1 (should be 0)"
STOCK_AFTER_1=$(curl -s -X GET "$BASE_URL/stock?workstationId=$WORKSTATION_ID&itemType=PRODUCT&itemId=$PRODUCT_ID" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.[0].quantity // 0')

print_info "Stock after Order 1: $STOCK_AFTER_1 units"

if [ $STOCK_AFTER_1 -eq 0 ]; then
    print_result 0 "Stock correctly debited to 0"
else
    print_result 1 "Stock debit error (Expected: 0, Got: $STOCK_AFTER_1)"
fi

print_step "Attempt to fulfill Order 2 (should fail - insufficient stock)"
curl -s -X PUT "$BASE_URL/customer-orders/$ORDER_2_ID/confirm" \
    -H "Authorization: Bearer $TOKEN" > /dev/null

FULFILL_2=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/customer-orders/$ORDER_2_ID/complete" \
    -H "Authorization: Bearer $TOKEN")

HTTP_CODE_2=$(echo "$FULFILL_2" | tail -n1)
FULFILL_2_RESPONSE=$(echo "$FULFILL_2" | head -n-1)
FULFILL_2_STATUS=$(echo "$FULFILL_2_RESPONSE" | jq -r '.status // "error"')

if [ "$HTTP_CODE_2" -ge 400 ] || [ "$FULFILL_2_STATUS" == "error" ]; then
    print_result 0 "Order 2 correctly rejected (insufficient stock)"
else
    print_result 1 "Order 2 incorrectly fulfilled (Stock should be insufficient)"
fi

# ==========================================
# TEST 2: CONCURRENT RACE CONDITION TEST
# ==========================================

print_test "2" "Concurrent Order Processing (Race Condition)"

print_step "Reset stock to $INITIAL_STOCK units"
curl -s -X POST "$BASE_URL/stock/adjust" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"itemType\": \"PRODUCT\",
        \"itemId\": $PRODUCT_ID,
        \"delta\": $INITIAL_STOCK,
        \"reason\": \"TEST_RESET\",
        \"notes\": \"Resetting stock for concurrent test\"
    }" > /dev/null

print_result 0 "Stock reset to $INITIAL_STOCK units"

print_step "Create two new orders for concurrent test"
ORDER_3=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": $ORDER_1_QTY
        }],
        \"notes\": \"Concurrent test - Order 3 (parallel)\"
    }")

ORDER_3_ID=$(echo $ORDER_3 | jq -r '.id')

ORDER_4=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": $ORDER_2_QTY
        }],
        \"notes\": \"Concurrent test - Order 4 (parallel)\"
    }")

ORDER_4_ID=$(echo $ORDER_4 | jq -r '.id')

print_info "Order 3 ID: $ORDER_3_ID"
print_info "Order 4 ID: $ORDER_4_ID"

print_step "Confirm both orders"
curl -s -X PUT "$BASE_URL/customer-orders/$ORDER_3_ID/confirm" -H "Authorization: Bearer $TOKEN" > /dev/null
curl -s -X PUT "$BASE_URL/customer-orders/$ORDER_4_ID/confirm" -H "Authorization: Bearer $TOKEN" > /dev/null
print_result 0 "Both orders confirmed"

print_step "Fulfill both orders SIMULTANEOUSLY using GNU Parallel"
print_info "This tests for race condition in inventory debit"

# Create temporary directory for parallel results
TEMP_DIR=$(mktemp -d)
export BASE_URL TOKEN ORDER_3_ID ORDER_4_ID

# Function to fulfill an order (will be run in parallel)
fulfill_order() {
    local ORDER_ID=$1
    local OUTPUT_FILE=$2
    
    RESULT=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/customer-orders/$ORDER_ID/complete" \
        -H "Authorization: Bearer $TOKEN")
    
    echo "$RESULT" > "$OUTPUT_FILE"
}

export -f fulfill_order

# Run fulfillments in parallel (literally at the same time)
echo "$ORDER_3_ID" > "$TEMP_DIR/orders.txt"
echo "$ORDER_4_ID" >> "$TEMP_DIR/orders.txt"

cat "$TEMP_DIR/orders.txt" | parallel -j 2 --delay 0 \
    "fulfill_order {} $TEMP_DIR/result_{}.txt"

# Read results
RESULT_3=$(cat "$TEMP_DIR/result_$ORDER_3_ID.txt")
HTTP_CODE_3=$(echo "$RESULT_3" | tail -n1)
RESPONSE_3=$(echo "$RESULT_3" | head -n-1)
STATUS_3=$(echo "$RESPONSE_3" | jq -r '.status // "error"')

RESULT_4=$(cat "$TEMP_DIR/result_$ORDER_4_ID.txt")
HTTP_CODE_4=$(echo "$RESULT_4" | tail -n1)
RESPONSE_4=$(echo "$RESULT_4" | head -n-1)
STATUS_4=$(echo "$RESPONSE_4" | jq -r '.status // "error"')

print_info "Order 3 result: HTTP $HTTP_CODE_3, Status: $STATUS_3"
print_info "Order 4 result: HTTP $HTTP_CODE_4, Status: $STATUS_4"

# ==========================================
# ANALYZE RACE CONDITION RESULTS
# ==========================================

print_step "Analyze concurrent fulfillment results"

SUCCESS_COUNT=0
FAILURE_COUNT=0

if [ "$HTTP_CODE_3" == "200" ] && [ "$STATUS_3" == "COMPLETED" ]; then
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
fi

if [ "$HTTP_CODE_4" == "200" ] && [ "$STATUS_4" == "COMPLETED" ]; then
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
fi

if [ "$HTTP_CODE_3" -ge 400 ] || [ "$STATUS_3" == "error" ]; then
    FAILURE_COUNT=$((FAILURE_COUNT + 1))
fi

if [ "$HTTP_CODE_4" -ge 400 ] || [ "$STATUS_4" == "error" ]; then
    FAILURE_COUNT=$((FAILURE_COUNT + 1))
fi

print_info "Successful fulfillments: $SUCCESS_COUNT"
print_info "Failed fulfillments: $FAILURE_COUNT"

# EXPECTED: One succeeds, one fails (proper atomicity)
# BUG: Both succeed (race condition, stock goes negative)
# BUG: Both fail (over-cautious locking)

if [ $SUCCESS_COUNT -eq 1 ] && [ $FAILURE_COUNT -eq 1 ]; then
    print_result 0 "CORRECT BEHAVIOR: One succeeded, one failed (atomic stock debit)"
elif [ $SUCCESS_COUNT -eq 2 ]; then
    print_result 1 "RACE CONDITION DETECTED: Both orders fulfilled (stock over-sold!)"
    print_info "This is a CRITICAL BUG - inventory can go negative"
elif [ $FAILURE_COUNT -eq 2 ]; then
    print_result 1 "OVERLY CAUTIOUS: Both orders failed (too aggressive locking)"
else
    print_result 1 "UNEXPECTED RESULT: Success=$SUCCESS_COUNT, Failure=$FAILURE_COUNT"
fi

# ==========================================
# VERIFY FINAL STOCK STATE
# ==========================================

print_step "Check final stock level"
FINAL_STOCK=$(curl -s -X GET "$BASE_URL/stock?workstationId=$WORKSTATION_ID&itemType=PRODUCT&itemId=$PRODUCT_ID" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.[0].quantity // 0')

print_info "Final stock: $FINAL_STOCK units"

# EXPECTED: Stock should be 0 (one order fulfilled 5 units from initial 5)
# BUG: Stock is -5 (both orders fulfilled, double debit)
# BUG: Stock is 5 (both orders failed, no debit)

if [ $FINAL_STOCK -eq 0 ]; then
    print_result 0 "Stock correctly at 0 (one order fulfilled from 5 units)"
elif [ $FINAL_STOCK -lt 0 ]; then
    print_result 1 "CRITICAL BUG: Negative stock ($FINAL_STOCK) - over-selling occurred!"
elif [ $FINAL_STOCK -eq $INITIAL_STOCK ]; then
    print_result 1 "No stock debited (both orders failed)"
else
    print_result 1 "Unexpected stock level: $FINAL_STOCK"
fi

# Cleanup
rm -rf "$TEMP_DIR"

# ==========================================
# SUMMARY
# ==========================================

echo -e "\n${BOLD}${CYAN}========================================${NC}"
echo -e "${BOLD}${CYAN}TEST SUMMARY${NC}"
echo -e "${BOLD}${CYAN}========================================${NC}\n"

echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}\n"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}${BOLD}✅ ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}No race condition detected - inventory is thread-safe.${NC}\n"
    exit 0
else
    echo -e "${RED}${BOLD}⚠️  TESTS FAILED - RACE CONDITION DETECTED!${NC}"
    echo -e "${YELLOW}Action required:${NC}"
    echo "  1. Add database-level locking (SELECT FOR UPDATE)"
    echo "  2. Implement optimistic locking with version field"
    echo "  3. Use distributed lock (Redis, Hazelcast) for multi-instance deployments"
    echo "  4. Add retry logic with exponential backoff"
    echo ""
    echo -e "${YELLOW}Example fix (PostgreSQL):${NC}"
    echo "  BEGIN TRANSACTION;"
    echo "  SELECT quantity FROM stock WHERE ... FOR UPDATE;"
    echo "  -- Check if quantity >= requested"
    echo "  UPDATE stock SET quantity = quantity - ? WHERE ...;"
    echo "  COMMIT;"
    echo ""
    exit 1
fi
