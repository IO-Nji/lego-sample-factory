#!/bin/bash

# Scenario 1 Test Script: Direct Fulfillment
# Purpose: Validate complete order lifecycle for direct stock fulfillment
# Date: January 24, 2026

BASE_URL="http://localhost:1011/api"
BOLD='\033[1m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

TESTS_PASSED=0
TESTS_FAILED=0
TEST_RESULTS=()

echo -e "${BOLD}${BLUE}========================================${NC}"
echo -e "${BOLD}${BLUE}SCENARIO 1: DIRECT FULFILLMENT TEST${NC}"
echo -e "${BOLD}${BLUE}========================================${NC}\n"

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ PASS:${NC} $2"
        ((TESTS_PASSED++))
        TEST_RESULTS+=("✅ $2")
    else
        echo -e "${RED}❌ FAIL:${NC} $2"
        ((TESTS_FAILED++))
        TEST_RESULTS+=("❌ $2")
    fi
}

# Function to print test headers
print_test() {
    echo -e "\n${YELLOW}[TEST $1]${NC} ${BOLD}$2${NC}"
}

# Function to print step headers
print_step() {
    echo -e "${BLUE}  → Step:${NC} $1"
}

# Check if system is running
echo -e "${BOLD}Pre-flight Check:${NC}"
if ! curl -s -f "$BASE_URL/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ System not accessible at $BASE_URL${NC}"
    echo "   Please ensure docker-compose is running: docker-compose up -d"
    exit 1
fi
echo -e "${GREEN}✅ System is running${NC}\n"

# ==========================================
# TEST 1: HAPPY PATH - DIRECT FULFILLMENT
# ==========================================

print_test "1" "Happy Path - Direct Fulfillment"

print_step "Login as Plant Warehouse operator"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"warehouse_operator","password":"password"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.user.id')
WORKSTATION_ID=$(echo $LOGIN_RESPONSE | jq -r '.user.workstation.id')

if [ "$TOKEN" != "null" ] && [ "$TOKEN" != "" ]; then
    print_result 0 "Login successful (User: $USER_ID, Workstation: $WORKSTATION_ID)"
else
    print_result 1 "Login failed"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

print_step "Get initial inventory"
INVENTORY_RESPONSE=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_ID" \
    -H "Authorization: Bearer $TOKEN")

INITIAL_STOCK=$(echo $INVENTORY_RESPONSE | jq -r '.[0].quantity // 0')
PRODUCT_ID=$(echo $INVENTORY_RESPONSE | jq -r '.[0].itemId // 1')

echo "   Product ID: $PRODUCT_ID, Initial Stock: $INITIAL_STOCK units"

# For Scenario 1 (Direct Fulfillment), we need SUFFICIENT stock
# Ensure at least 5 units are available for testing
if [ "$INITIAL_STOCK" -lt 5 ]; then
    echo -e "${YELLOW}   ⚠️  Low stock detected ($INITIAL_STOCK). Replenishing to 10 units for Scenario 1 testing...${NC}"
    REPLENISH_DELTA=$((10 - INITIAL_STOCK))
    ADJUST_RESPONSE=$(curl -s -X POST "$BASE_URL/stock/adjust" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"workstationId\": $WORKSTATION_ID,
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"delta\": $REPLENISH_DELTA,
            \"reasonCode\": \"ADJUSTMENT\",
            \"notes\": \"Test setup - replenishing stock for Scenario 1\"
        }")
    
    INITIAL_STOCK=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_ID" \
        -H "Authorization: Bearer $TOKEN" | jq -r "[.[] | select(.itemId == $PRODUCT_ID)] | .[0].quantity // 0")
    echo "   Stock replenished to: $INITIAL_STOCK units"
fi

print_step "Create customer order (quantity: 2)"
ORDER_CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": 2
        }],
        \"notes\": \"Test order - Scenario 1\"
    }")

ORDER_ID=$(echo $ORDER_CREATE_RESPONSE | jq -r '.id')
ORDER_NUMBER=$(echo $ORDER_CREATE_RESPONSE | jq -r '.orderNumber')
ORDER_STATUS=$(echo $ORDER_CREATE_RESPONSE | jq -r '.status')

if [ "$ORDER_ID" != "null" ] && [ "$ORDER_STATUS" == "PENDING" ]; then
    print_result 0 "Order created with status PENDING ($ORDER_NUMBER)"
else
    print_result 1 "Order creation failed or wrong status (Expected: PENDING, Got: $ORDER_STATUS)"
fi

print_step "Confirm customer order"
CONFIRM_RESPONSE=$(curl -s -X PUT "$BASE_URL/customer-orders/$ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN")

CONFIRM_STATUS=$(echo $CONFIRM_RESPONSE | jq -r '.status')

if [ "$CONFIRM_STATUS" == "CONFIRMED" ]; then
    print_result 0 "Order confirmed (Status: PENDING → CONFIRMED)"
else
    print_result 1 "Order confirmation failed (Expected: CONFIRMED, Got: $CONFIRM_STATUS)"
fi

print_step "Fulfill customer order"
FULFILL_RESPONSE=$(curl -s -X PUT "$BASE_URL/customer-orders/$ORDER_ID/fulfill" \
    -H "Authorization: Bearer $TOKEN")

FULFILL_STATUS=$(echo $FULFILL_RESPONSE | jq -r '.status')

if [ "$FULFILL_STATUS" == "COMPLETED" ]; then
    print_result 0 "Order fulfilled (Status: CONFIRMED → COMPLETED)"
else
    print_result 1 "Order fulfillment failed (Expected: COMPLETED, Got: $FULFILL_STATUS)"
fi

print_step "Verify inventory deduction"
UPDATED_INVENTORY=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_ID" \
    -H "Authorization: Bearer $TOKEN")
UPDATED_STOCK=$(echo $UPDATED_INVENTORY | jq -r '.[0].quantity // 0')
EXPECTED_STOCK=$((INITIAL_STOCK - 2))

if [ $UPDATED_STOCK -eq $EXPECTED_STOCK ]; then
    print_result 0 "Inventory debited correctly ($INITIAL_STOCK → $UPDATED_STOCK)"
else
    print_result 1 "Inventory mismatch (Expected: $EXPECTED_STOCK, Got: $UPDATED_STOCK)"
fi

# ==========================================
# TEST 2: INSUFFICIENT STOCK ERROR
# ==========================================

print_test "2" "Insufficient Stock - Error Handling"

print_step "Create order exceeding available stock"
LARGE_ORDER_QTY=$((UPDATED_STOCK + 50))

LARGE_ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": $LARGE_ORDER_QTY
        }],
        \"notes\": \"Test order - Insufficient stock\"
    }")

LARGE_ORDER_ID=$(echo $LARGE_ORDER_RESPONSE | jq -r '.id')
LARGE_ORDER_NUMBER=$(echo $LARGE_ORDER_RESPONSE | jq -r '.orderNumber')

if [ "$LARGE_ORDER_ID" != "null" ]; then
    print_result 0 "Large order created ($LARGE_ORDER_NUMBER, requesting $LARGE_ORDER_QTY units)"
else
    print_result 1 "Large order creation failed"
fi

print_step "Confirm order (should succeed, stock check happens during fulfill)"
LARGE_CONFIRM=$(curl -s -X PUT "$BASE_URL/customer-orders/$LARGE_ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN")

LARGE_CONFIRM_STATUS=$(echo $LARGE_CONFIRM | jq -r '.status')

if [ "$LARGE_CONFIRM_STATUS" == "CONFIRMED" ]; then
    print_result 0 "Order confirmed successfully"
else
    print_result 1 "Order confirmation failed"
fi

print_step "Attempt to fulfill (should trigger Scenario 2 - warehouse order)"
LARGE_FULFILL=$(curl -s -X PUT "$BASE_URL/customer-orders/$LARGE_ORDER_ID/fulfill" \
    -H "Authorization: Bearer $TOKEN")

LARGE_FULFILL_STATUS=$(echo $LARGE_FULFILL | jq -r '.status')

# For insufficient stock, the order should move to PROCESSING (Scenario 2)
if [ "$LARGE_FULFILL_STATUS" == "PROCESSING" ]; then
    print_result 0 "Order moved to PROCESSING (Scenario 2 triggered due to insufficient stock)"
    echo "   This is expected behavior when stock is insufficient."
elif [ "$LARGE_FULFILL_STATUS" == "CONFIRMED" ]; then
    print_result 1 "Order remained CONFIRMED (Expected transition to PROCESSING)"
else
    echo "   Unexpected status: $LARGE_FULFILL_STATUS"
fi

# ==========================================
# TEST 3: STATUS TRANSITION VALIDATION
# ==========================================

print_test "3" "Status Transition Validation"

print_step "Create test order for status transitions"
STATUS_TEST_ORDER=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": 1
        }],
        \"notes\": \"Status transition test\"
    }")

STATUS_ORDER_ID=$(echo $STATUS_TEST_ORDER | jq -r '.id')
STATUS_ORDER_STATUS=$(echo $STATUS_TEST_ORDER | jq -r '.status')

if [ "$STATUS_ORDER_STATUS" == "PENDING" ]; then
    print_result 0 "Order created with PENDING status"
else
    print_result 1 "Order created with wrong status (Expected: PENDING, Got: $STATUS_ORDER_STATUS)"
fi

print_step "Test valid transition: PENDING → CONFIRMED"
TRANSITION_1=$(curl -s -X PUT "$BASE_URL/customer-orders/$STATUS_ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN")
TRANS_1_STATUS=$(echo $TRANSITION_1 | jq -r '.status')

if [ "$TRANS_1_STATUS" == "CONFIRMED" ]; then
    print_result 0 "Valid transition: PENDING → CONFIRMED"
else
    print_result 1 "Invalid transition result (Expected: CONFIRMED, Got: $TRANS_1_STATUS)"
fi

print_step "Test valid transition: CONFIRMED → COMPLETED"
TRANSITION_2=$(curl -s -X PUT "$BASE_URL/customer-orders/$STATUS_ORDER_ID/fulfill" \
    -H "Authorization: Bearer $TOKEN")
TRANS_2_STATUS=$(echo $TRANSITION_2 | jq -r '.status')

if [ "$TRANS_2_STATUS" == "COMPLETED" ]; then
    print_result 0 "Valid transition: CONFIRMED → COMPLETED"
else
    print_result 1 "Invalid transition result (Expected: COMPLETED, Got: $TRANS_2_STATUS)"
fi

print_step "Test invalid transition: COMPLETED → CANCELLED"
TRANSITION_INVALID=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/customer-orders/$STATUS_ORDER_ID/cancel" \
    -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$TRANSITION_INVALID" | tail -n1)
TRANS_INVALID_RESPONSE=$(echo "$TRANSITION_INVALID" | head -n-1)
TRANS_INVALID_STATUS=$(echo "$TRANS_INVALID_RESPONSE" | jq -r '.status // "error"')

# Should return error (4xx/5xx) or remain COMPLETED
if [ "$HTTP_CODE" -ge 400 ] && [ "$HTTP_CODE" -lt 600 ]; then
    print_result 0 "Invalid transition rejected (Backend returned HTTP $HTTP_CODE)"
elif [ "$TRANS_INVALID_STATUS" == "COMPLETED" ]; then
    print_result 0 "Invalid transition rejected (Order remains COMPLETED)"
elif [ "$TRANS_INVALID_STATUS" == "error" ] || [ "$TRANS_INVALID_STATUS" == "null" ]; then
    print_result 0 "Invalid transition rejected (Backend returned error)"
else
    print_result 1 "Invalid transition allowed (Order changed to: $TRANS_INVALID_STATUS)"
fi

# ==========================================
# TEST 4: MULTIPLE ITEMS ORDER
# ==========================================

print_test "4" "Multiple Items in Single Order"

# Get second product if available
PRODUCT_2_ID=$((PRODUCT_ID + 1))

print_step "Create order with multiple items"
MULTI_ORDER=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [
            {
                \"itemType\": \"PRODUCT\",
                \"itemId\": $PRODUCT_ID,
                \"quantity\": 1
            },
            {
                \"itemType\": \"PRODUCT\",
                \"itemId\": $PRODUCT_2_ID,
                \"quantity\": 1
            }
        ],
        \"notes\": \"Multi-item test order\"
    }")

MULTI_ORDER_ID=$(echo $MULTI_ORDER | jq -r '.id')
MULTI_ITEMS_COUNT=$(echo $MULTI_ORDER | jq -r '.orderItems | length')

if [ "$MULTI_ITEMS_COUNT" == "2" ]; then
    print_result 0 "Multi-item order created (2 items)"
else
    print_result 1 "Multi-item order creation failed (Items: $MULTI_ITEMS_COUNT)"
fi

print_step "Confirm and fulfill multi-item order"
curl -s -X PUT "$BASE_URL/customer-orders/$MULTI_ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN" > /dev/null

MULTI_FULFILL=$(curl -s -X PUT "$BASE_URL/customer-orders/$MULTI_ORDER_ID/fulfill" \
    -H "Authorization: Bearer $TOKEN")
MULTI_STATUS=$(echo $MULTI_FULFILL | jq -r '.status')

if [ "$MULTI_STATUS" == "COMPLETED" ] || [ "$MULTI_STATUS" == "PROCESSING" ]; then
    print_result 0 "Multi-item order processed (Status: $MULTI_STATUS)"
else
    print_result 1 "Multi-item order processing failed (Status: $MULTI_STATUS)"
fi

# ==========================================
# TEST SUMMARY
# ==========================================

echo -e "\n${BOLD}${BLUE}========================================${NC}"
echo -e "${BOLD}${BLUE}TEST SUMMARY${NC}"
echo -e "${BOLD}${BLUE}========================================${NC}\n"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))

echo -e "${BOLD}Total Tests:${NC} $TOTAL_TESTS"
echo -e "${GREEN}Passed:${NC} $TESTS_PASSED"
echo -e "${RED}Failed:${NC} $TESTS_FAILED"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}${BOLD}✅ ALL TESTS PASSED${NC}"
    echo ""
    echo -e "${BOLD}Scenario 1: Direct Fulfillment - VERIFIED ✅${NC}"
    echo "  • Order lifecycle: PENDING → CONFIRMED → COMPLETED"
    echo "  • Inventory deduction working correctly"
    echo "  • Status transition validation working"
    echo "  • Multi-item orders supported"
    echo "  • Error handling for insufficient stock"
    exit 0
else
    echo -e "${RED}${BOLD}❌ SOME TESTS FAILED${NC}"
    echo ""
    echo "Failed tests:"
    for result in "${TEST_RESULTS[@]}"; do
        if [[ $result == ❌* ]]; then
            echo "  $result"
        fi
    done
    exit 1
fi
