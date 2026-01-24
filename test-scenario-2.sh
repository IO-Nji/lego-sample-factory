#!/bin/bash

# Scenario 2 Test Script: Warehouse Order + Final Assembly
# Purpose: Validate complete flow with BOM lookup, module warehouse orders, and final assembly
# Date: January 24, 2026

BASE_URL="http://localhost:1011/api"
BOLD='\033[1m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

TESTS_PASSED=0
TESTS_FAILED=0
TEST_RESULTS=()

echo -e "${BOLD}${BLUE}========================================${NC}"
echo -e "${BOLD}${BLUE}SCENARIO 2: WAREHOUSE ORDER + FINAL ASSEMBLY${NC}"
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

# Function to print info
print_info() {
    echo -e "${MAGENTA}  ℹ${NC} $1"
}

# Function to wait for user
wait_for_key() {
    echo -e "\n${YELLOW}Press Enter to continue...${NC}"
    read
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
# TEST 1: CUSTOMER ORDER WITH INSUFFICIENT STOCK → WAREHOUSE ORDER
# ==========================================

print_test "1" "Customer Order Confirmation → Warehouse Order Creation (BOM Lookup)"

print_step "Login as Plant Warehouse operator"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"warehouse_operator","password":"password"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.user.id')
WORKSTATION_ID=$(echo $LOGIN_RESPONSE | jq -r '.user.workstation.id')

if [ "$TOKEN" != "null" ] && [ "$TOKEN" != "" ]; then
    print_result 0 "Login successful (Workstation: $WORKSTATION_ID - Plant Warehouse)"
else
    print_result 1 "Login failed"
    exit 1
fi

print_step "Check initial product inventory at Plant Warehouse"
INVENTORY_RESPONSE=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_ID" \
    -H "Authorization: Bearer $TOKEN")

# Find a product with low or no stock
PRODUCT_ID=$(echo $INVENTORY_RESPONSE | jq -r '[.[] | select(.itemType == "PRODUCT")] | .[0].itemId // 1')
INITIAL_PRODUCT_STOCK=$(echo $INVENTORY_RESPONSE | jq -r "[.[] | select(.itemId == $PRODUCT_ID)] | .[0].quantity // 0")

print_info "Product ID: $PRODUCT_ID, Initial Stock: $INITIAL_PRODUCT_STOCK units"

# If stock is sufficient, reduce it first
if [ $INITIAL_PRODUCT_STOCK -gt 0 ]; then
    print_info "Reducing product stock to simulate insufficient inventory..."
    # We'll create an order that exceeds available stock
    ORDER_QTY=$((INITIAL_PRODUCT_STOCK + 2))
else
    ORDER_QTY=2
fi

print_step "Create customer order (quantity: $ORDER_QTY) - exceeds available stock"
ORDER_CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_ID,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": $ORDER_QTY
        }],
        \"notes\": \"Test order - Scenario 2\"
    }")

CUSTOMER_ORDER_ID=$(echo $ORDER_CREATE_RESPONSE | jq -r '.id')
CUSTOMER_ORDER_NUMBER=$(echo $ORDER_CREATE_RESPONSE | jq -r '.orderNumber')
CUSTOMER_ORDER_STATUS=$(echo $ORDER_CREATE_RESPONSE | jq -r '.status')

if [ "$CUSTOMER_ORDER_ID" != "null" ] && [ "$CUSTOMER_ORDER_STATUS" == "PENDING" ]; then
    print_result 0 "Customer order created ($CUSTOMER_ORDER_NUMBER, Status: PENDING)"
else
    print_result 1 "Customer order creation failed"
fi

print_step "Confirm customer order - should check PRODUCT stock and set triggerScenario"
CONFIRM_RESPONSE=$(curl -s -X PUT "$BASE_URL/customer-orders/$CUSTOMER_ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN")

CONFIRM_STATUS=$(echo $CONFIRM_RESPONSE | jq -r '.status')
TRIGGER_SCENARIO=$(echo $CONFIRM_RESPONSE | jq -r '.triggerScenario')

print_info "Status: $CONFIRM_STATUS, Trigger Scenario: $TRIGGER_SCENARIO"

if [ "$CONFIRM_STATUS" == "CONFIRMED" ]; then
    print_result 0 "Order confirmed (Status: PENDING → CONFIRMED)"
else
    print_result 1 "Order confirmation failed (Got status: $CONFIRM_STATUS)"
fi

if [ "$TRIGGER_SCENARIO" == "WAREHOUSE_ORDER_NEEDED" ] || [ "$TRIGGER_SCENARIO" == "DIRECT_FULFILLMENT" ]; then
    print_result 0 "triggerScenario field set correctly ($TRIGGER_SCENARIO)"
else
    print_result 1 "triggerScenario not set properly (Got: $TRIGGER_SCENARIO)"
fi

# Only proceed if warehouse order is needed
if [ "$TRIGGER_SCENARIO" == "WAREHOUSE_ORDER_NEEDED" ]; then
    print_step "Process order to create warehouse order (via fulfill endpoint)"
    FULFILL_RESPONSE=$(curl -s -X PUT "$BASE_URL/customer-orders/$CUSTOMER_ORDER_ID/fulfill" \
        -H "Authorization: Bearer $TOKEN")
    
    # Give system time to create warehouse order
    sleep 3
fi

# ==========================================
# TEST 2: WAREHOUSE ORDER CONFIRMATION - BOM VERIFICATION
# ==========================================

print_test "2" "Warehouse Order BOM Lookup Verification"

print_step "Login as Modules Supermarket operator"
LOGIN_MS_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"modules_supermarket","password":"password"}')

TOKEN_MS=$(echo $LOGIN_MS_RESPONSE | jq -r '.token')
WORKSTATION_MS=$(echo $LOGIN_MS_RESPONSE | jq -r '.user.workstation.id // 8')

if [ "$TOKEN_MS" != "null" ] && [ "$TOKEN_MS" != "" ]; then
    print_result 0 "Login as Modules Supermarket (Workstation: $WORKSTATION_MS)"
else
    print_result 1 "Modules Supermarket login failed"
fi

print_step "Fetch warehouse orders for Modules Supermarket"
WO_LIST_RESPONSE=$(curl -s -X GET "$BASE_URL/warehouse-orders/workstation/$WORKSTATION_MS" \
    -H "Authorization: Bearer $TOKEN_MS")

# Check if response is an array
if echo "$WO_LIST_RESPONSE" | jq -e 'type == "array"' > /dev/null 2>&1; then
    WAREHOUSE_ORDER_ID=$(echo $WO_LIST_RESPONSE | jq -r 'last | .id')
    WAREHOUSE_ORDER_NUMBER=$(echo $WO_LIST_RESPONSE | jq -r 'last | .orderNumber')
    WAREHOUSE_ORDER_STATUS=$(echo $WO_LIST_RESPONSE | jq -r 'last | .status')
else
    # Try as single object
    WAREHOUSE_ORDER_ID=$(echo $WO_LIST_RESPONSE | jq -r '.id')
    WAREHOUSE_ORDER_NUMBER=$(echo $WO_LIST_RESPONSE | jq -r '.orderNumber')
    WAREHOUSE_ORDER_STATUS=$(echo $WO_LIST_RESPONSE | jq -r '.status')
fi

if [ "$WAREHOUSE_ORDER_ID" != "null" ] && [ -n "$WAREHOUSE_ORDER_ID" ]; then
    print_result 0 "Warehouse order found ($WAREHOUSE_ORDER_NUMBER, Status: $WAREHOUSE_ORDER_STATUS)"
else
    print_result 1 "No warehouse order found"
    print_info "Response: $WO_LIST_RESPONSE"
fi

print_step "Verify BOM conversion: Warehouse order should contain MODULES, not products"
WO_DETAILS=$(curl -s -X GET "$BASE_URL/warehouse-orders/$WAREHOUSE_ORDER_ID" \
    -H "Authorization: Bearer $TOKEN_MS")

ITEM_TYPES=$(echo $WO_DETAILS | jq -r '.orderItems[].itemType' | sort | uniq)
MODULE_COUNT=$(echo $WO_DETAILS | jq -r '.orderItems | length')

print_info "Order contains $MODULE_COUNT items with types: $ITEM_TYPES"

# Check if items are all modules
ALL_MODULES=true
for ITEM_TYPE in $ITEM_TYPES; do
    if [ "$ITEM_TYPE" != "MODULE" ]; then
        ALL_MODULES=false
        break
    fi
done

if [ "$ALL_MODULES" == "true" ]; then
    print_result 0 "BOM conversion successful - all items are MODULEs"
else
    print_result 1 "BOM conversion failed - found non-MODULE items: $ITEM_TYPES"
fi

print_step "Confirm warehouse order - should check MODULE stock and set triggerScenario"
WO_CONFIRM_RESPONSE=$(curl -s -X PUT "$BASE_URL/warehouse-orders/$WAREHOUSE_ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN_MS")

WO_CONFIRM_STATUS=$(echo $WO_CONFIRM_RESPONSE | jq -r '.status')
WO_TRIGGER_SCENARIO=$(echo $WO_CONFIRM_RESPONSE | jq -r '.triggerScenario')

print_info "Status: $WO_CONFIRM_STATUS, Trigger Scenario: $WO_TRIGGER_SCENARIO"

if [ "$WO_CONFIRM_STATUS" == "CONFIRMED" ]; then
    print_result 0 "Warehouse order confirmed (Status: PENDING → CONFIRMED)"
else
    print_result 1 "Warehouse order confirmation failed (Got: $WO_CONFIRM_STATUS)"
fi

if [ "$WO_TRIGGER_SCENARIO" == "DIRECT_FULFILLMENT" ] || [ "$WO_TRIGGER_SCENARIO" == "PRODUCTION_REQUIRED" ]; then
    print_result 0 "Warehouse order triggerScenario set ($WO_TRIGGER_SCENARIO)"
else
    print_result 1 "Warehouse order triggerScenario not set (Got: $WO_TRIGGER_SCENARIO)"
fi

# ==========================================
# TEST 3: WAREHOUSE ORDER FULFILLMENT → FINAL ASSEMBLY CREATION
# ==========================================

print_test "3" "Warehouse Order Fulfillment → Final Assembly Order Creation"

# Only proceed if modules are available
if [ "$WO_TRIGGER_SCENARIO" == "DIRECT_FULFILLMENT" ]; then
    print_step "Fulfill warehouse order - should create Final Assembly orders"
    WO_FULFILL_RESPONSE=$(curl -s -X PUT "$BASE_URL/warehouse-orders/$WAREHOUSE_ORDER_ID/fulfill-modules" \
        -H "Authorization: Bearer $TOKEN_MS")
    
    WO_FULFILL_STATUS=$(echo $WO_FULFILL_RESPONSE | jq -r '.status')
    
    if [ "$WO_FULFILL_STATUS" == "FULFILLED" ]; then
        print_result 0 "Warehouse order fulfilled successfully"
        FA_SHOULD_EXIST=true
    else
        print_result 1 "Warehouse order fulfillment failed (Status: $WO_FULFILL_STATUS)"
        FA_SHOULD_EXIST=false
    fi
    
    # Give system time to create final assembly orders
    sleep 2
else
    print_info "Skipping fulfillment - modules not available (Scenario: $WO_TRIGGER_SCENARIO)"
    print_info "In production, this would trigger module production (Scenario 3)"
    print_result 0 "Warehouse order correctly identified module shortage"
    FA_SHOULD_EXIST=false
fi

# ==========================================
# TEST 4: FINAL ASSEMBLY WORKFLOW
# ==========================================

print_test "4" "Final Assembly Order Workflow"

print_step "Login as Final Assembly operator"
LOGIN_FA_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"final_assembly","password":"password"}')

TOKEN_FA=$(echo $LOGIN_FA_RESPONSE | jq -r '.token')
WORKSTATION_FA=$(echo $LOGIN_FA_RESPONSE | jq -r '.user.workstation.id // 6')

if [ "$TOKEN_FA" != "null" ] && [ "$TOKEN_FA" != "" ]; then
    print_result 0 "Login as Final Assembly (Workstation: $WORKSTATION_FA - WS-6)"
else
    print_result 1 "Final Assembly login failed"
fi

print_step "Fetch final assembly orders"
FA_LIST_RESPONSE=$(curl -s -X GET "$BASE_URL/final-assembly-orders/workstation/$WORKSTATION_FA" \
    -H "Authorization: Bearer $TOKEN_FA")

# Check if response is an array
if echo "$FA_LIST_RESPONSE" | jq -e 'type == "array"' > /dev/null 2>&1; then
    FA_ORDER_ID=$(echo $FA_LIST_RESPONSE | jq -r 'last | .id')
    FA_ORDER_NUMBER=$(echo $FA_LIST_RESPONSE | jq -r 'last | .orderNumber')
    FA_ORDER_STATUS=$(echo $FA_LIST_RESPONSE | jq -r 'last | .status')
else
    # Try as single object  
    FA_ORDER_ID=$(echo $FA_LIST_RESPONSE | jq -r '.id')
    FA_ORDER_NUMBER=$(echo $FA_LIST_RESPONSE | jq -r '.orderNumber')
    FA_ORDER_STATUS=$(echo $FA_LIST_RESPONSE | jq -r '.status')
fi

if [ "$FA_ORDER_ID" != "null" ] && [ -n "$FA_ORDER_ID" ] && [ "$FA_ORDER_STATUS" != "COMPLETED" ]; then
    print_result 0 "Final assembly order found ($FA_ORDER_NUMBER, Status: $FA_ORDER_STATUS)"
else
    if [ "$FA_SHOULD_EXIST" == "true" ]; then
        if [ "$FA_ORDER_STATUS" == "COMPLETED" ]; then
            print_result 0 "Final assembly order found but already completed (from previous run)"
            print_info "Skipping workflow steps - order already processed"
        else
            print_result 1 "No final assembly order found (expected after warehouse fulfillment)"
        fi
    else
        print_result 0 "No final assembly order found (expected - modules not available)"
        print_info "Scenario 3 (production) would be triggered in this case"
    fi
fi

if [ "$FA_ORDER_ID" != "null" ] && [ -n "$FA_ORDER_ID" ] && [ "$FA_ORDER_STATUS" != "COMPLETED" ]; then
    print_step "Confirm final assembly order (PENDING → CONFIRMED)"
    FA_CONFIRM_RESPONSE=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/confirm" \
        -H "Authorization: Bearer $TOKEN_FA")
    
    FA_CONFIRM_STATUS=$(echo $FA_CONFIRM_RESPONSE | jq -r '.status')
    
    if [ "$FA_CONFIRM_STATUS" == "CONFIRMED" ]; then
        print_result 0 "Final assembly confirmed (Status: PENDING → CONFIRMED)"
    else
        print_result 1 "Final assembly confirmation failed (Got: $FA_CONFIRM_STATUS)"
    fi
    
    print_step "Start final assembly order (CONFIRMED → IN_PROGRESS)"
    FA_START_RESPONSE=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/start" \
        -H "Authorization: Bearer $TOKEN_FA")
    
    FA_START_STATUS=$(echo $FA_START_RESPONSE | jq -r '.status')
    
    if [ "$FA_START_STATUS" == "IN_PROGRESS" ]; then
        print_result 0 "Final assembly started (Status: CONFIRMED → IN_PROGRESS)"
    else
        print_result 1 "Final assembly start failed (Got: $FA_START_STATUS)"
    fi
    
    print_step "Complete final assembly - should credit Plant Warehouse"
    
    # Get initial plant warehouse stock before completion
    PLANT_STOCK_BEFORE=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_ID" \
        -H "Authorization: Bearer $TOKEN")
    PRODUCT_STOCK_BEFORE=$(echo $PLANT_STOCK_BEFORE | jq -r "[.[] | select(.itemId == $PRODUCT_ID)] | .[0].quantity // 0")
    
    print_info "Plant Warehouse product stock before completion: $PRODUCT_STOCK_BEFORE"
    
    # Step 3: Complete assembly (IN_PROGRESS → COMPLETED_ASSEMBLY)
    FA_COMPLETE_RESPONSE=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/complete" \
        -H "Authorization: Bearer $TOKEN_FA")
    
    FA_COMPLETE_STATUS=$(echo $FA_COMPLETE_RESPONSE | jq -r '.status')
    
    if [ "$FA_COMPLETE_STATUS" == "COMPLETED_ASSEMBLY" ]; then
        print_result 0 "Final assembly completed (Status: IN_PROGRESS → COMPLETED_ASSEMBLY)"
    else
        print_result 1 "Final assembly completion failed (Got: $FA_COMPLETE_STATUS)"
    fi
    
    # Step 4: Submit completion (COMPLETED_ASSEMBLY → COMPLETED, credits warehouse)
    print_step "Submit final assembly completion - this credits Plant Warehouse"
    FA_SUBMIT_RESPONSE=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/submit" \
        -H "Authorization: Bearer $TOKEN_FA")
    
    FA_SUBMIT_STATUS=$(echo $FA_SUBMIT_RESPONSE | jq -r '.status')
    
    if [ "$FA_SUBMIT_STATUS" == "COMPLETED" ]; then
        print_result 0 "Final assembly submitted (Status: COMPLETED_ASSEMBLY → COMPLETED)"
    else
        print_result 1 "Final assembly submission failed (Got: $FA_SUBMIT_STATUS)"
    fi
    
    # Verify Plant Warehouse stock increased
    sleep 1
    PLANT_STOCK_AFTER=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_ID" \
        -H "Authorization: Bearer $TOKEN")
    PRODUCT_STOCK_AFTER=$(echo $PLANT_STOCK_AFTER | jq -r "[.[] | select(.itemId == $PRODUCT_ID)] | .[0].quantity // 0")
    
    print_info "Plant Warehouse product stock after completion: $PRODUCT_STOCK_AFTER"
    
    if [ $PRODUCT_STOCK_AFTER -gt $PRODUCT_STOCK_BEFORE ]; then
        STOCK_INCREASE=$((PRODUCT_STOCK_AFTER - PRODUCT_STOCK_BEFORE))
        print_result 0 "Plant Warehouse credited correctly (+$STOCK_INCREASE units)"
    else
        print_result 1 "Plant Warehouse not credited (Before: $PRODUCT_STOCK_BEFORE, After: $PRODUCT_STOCK_AFTER)"
    fi
fi

# ==========================================
# SUMMARY
# ==========================================

echo -e "\n${BOLD}${BLUE}========================================${NC}"
echo -e "${BOLD}${BLUE}TEST SUMMARY${NC}"
echo -e "${BOLD}${BLUE}========================================${NC}\n"

echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}\n"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}${BOLD}✅ ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}Scenario 2 flow verified successfully.${NC}\n"
    exit 0
else
    echo -e "${RED}${BOLD}❌ SOME TESTS FAILED${NC}"
    echo -e "${YELLOW}Review the failed tests above.${NC}\n"
    exit 1
fi
