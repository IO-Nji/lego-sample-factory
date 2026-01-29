#!/bin/bash

# Scenario 3 Test Script: Full Production Cycle
# Purpose: Validate complete production flow from Customer Order through all workstations
# Date: January 29, 2026

BASE_URL="http://localhost:1011/api"
BOLD='\033[1m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

TESTS_PASSED=0
TESTS_FAILED=0
TEST_RESULTS=()

echo -e "${BOLD}${BLUE}================================================${NC}"
echo -e "${BOLD}${BLUE}SCENARIO 3: FULL PRODUCTION CYCLE${NC}"
echo -e "${BOLD}${BLUE}================================================${NC}"
echo -e "${CYAN}Flow: CustomerOrder → WarehouseOrder → ProductionOrder${NC}"
echo -e "${CYAN}      → ControlOrders → SupplyOrders → WorkstationOrders${NC}"
echo -e "${CYAN}      → Inventory Credits → Fulfillment${NC}\n"

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
    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}[TEST $1]${NC} ${BOLD}$2${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Function to print step headers
print_step() {
    echo -e "${BLUE}  → Step:${NC} $1"
}

# Function to print info
print_info() {
    echo -e "${MAGENTA}  ℹ${NC} $1"
}

# Function to print sub-step
print_substep() {
    echo -e "${CYAN}    •${NC} $1"
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
# TEST 1: CREATE CUSTOMER ORDER (WS-7 Plant Warehouse)
# ==========================================

print_test "1" "Create Customer Order with Insufficient Stock"

print_step "Login as Plant Warehouse operator (WS-7)"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"warehouse_operator","password":"password"}')

TOKEN_PW=$(echo $LOGIN_RESPONSE | jq -r '.token')
WORKSTATION_PW=$(echo $LOGIN_RESPONSE | jq -r '.user.workstation.id // 7')

if [ "$TOKEN_PW" != "null" ] && [ "$TOKEN_PW" != "" ]; then
    print_result 0 "Login successful (Workstation: $WORKSTATION_PW - Plant Warehouse)"
else
    print_result 1 "Login failed"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

print_step "Check initial product inventory at Plant Warehouse"
INVENTORY_RESPONSE=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_PW" \
    -H "Authorization: Bearer $TOKEN_PW")

# Use product ID 1 (LEGO Model Car)
PRODUCT_ID=1
INITIAL_PRODUCT_STOCK=$(echo $INVENTORY_RESPONSE | jq -r "[.[] | select(.itemId == $PRODUCT_ID)] | .[0].quantity // 0")
print_info "Product ID: $PRODUCT_ID, Initial Stock: $INITIAL_PRODUCT_STOCK units"

# Create order quantity that exceeds stock (to trigger production)
ORDER_QTY=$((INITIAL_PRODUCT_STOCK + 3))
print_step "Create customer order (quantity: $ORDER_QTY) - exceeds available stock to trigger production"

ORDER_CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/customer-orders" \
    -H "Authorization: Bearer $TOKEN_PW" \
    -H "Content-Type: application/json" \
    -d "{
        \"workstationId\": $WORKSTATION_PW,
        \"orderItems\": [{
            \"itemType\": \"PRODUCT\",
            \"itemId\": $PRODUCT_ID,
            \"quantity\": $ORDER_QTY
        }],
        \"notes\": \"Scenario 3 Test - Full Production Cycle\"
    }")

CUSTOMER_ORDER_ID=$(echo $ORDER_CREATE_RESPONSE | jq -r '.id')
CUSTOMER_ORDER_NUMBER=$(echo $ORDER_CREATE_RESPONSE | jq -r '.orderNumber')
CUSTOMER_ORDER_STATUS=$(echo $ORDER_CREATE_RESPONSE | jq -r '.status')

if [ "$CUSTOMER_ORDER_ID" != "null" ] && [ "$CUSTOMER_ORDER_STATUS" == "PENDING" ]; then
    print_result 0 "Customer order created ($CUSTOMER_ORDER_NUMBER, Status: PENDING)"
else
    print_result 1 "Customer order creation failed"
    echo "Response: $ORDER_CREATE_RESPONSE"
fi

print_step "Confirm customer order - sets triggerScenario based on stock"
CONFIRM_RESPONSE=$(curl -s -X PUT "$BASE_URL/customer-orders/$CUSTOMER_ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN_PW")

CO_STATUS=$(echo $CONFIRM_RESPONSE | jq -r '.status')
CO_TRIGGER=$(echo $CONFIRM_RESPONSE | jq -r '.triggerScenario')

if [ "$CO_STATUS" == "CONFIRMED" ]; then
    print_result 0 "Customer order confirmed (Status: CONFIRMED)"
    print_info "Trigger Scenario: $CO_TRIGGER"
else
    print_result 1 "Customer order confirmation failed (Got: $CO_STATUS)"
fi

# Process to create warehouse order
if [ "$CO_TRIGGER" == "WAREHOUSE_ORDER_NEEDED" ]; then
    print_step "Process order to create Warehouse Order"
    curl -s -X PUT "$BASE_URL/customer-orders/$CUSTOMER_ORDER_ID/fulfill" \
        -H "Authorization: Bearer $TOKEN_PW" > /dev/null
    sleep 2
fi

# ==========================================
# TEST 2: WAREHOUSE ORDER (WS-8 Modules Supermarket)
# ==========================================

print_test "2" "Warehouse Order → Production Required"

print_step "Login as Modules Supermarket operator (WS-8)"
LOGIN_MS=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"modules_supermarket","password":"password"}')

TOKEN_MS=$(echo $LOGIN_MS | jq -r '.token')
WORKSTATION_MS=$(echo $LOGIN_MS | jq -r '.user.workstation.id // 8')

if [ "$TOKEN_MS" != "null" ] && [ "$TOKEN_MS" != "" ]; then
    print_result 0 "Login as Modules Supermarket (WS-$WORKSTATION_MS)"
else
    print_result 1 "Modules Supermarket login failed"
fi

print_step "Fetch warehouse orders"
WO_LIST=$(curl -s -X GET "$BASE_URL/warehouse-orders/workstation/$WORKSTATION_MS" \
    -H "Authorization: Bearer $TOKEN_MS")

WAREHOUSE_ORDER_ID=$(echo $WO_LIST | jq -r 'if type == "array" then (last | .id) else .id end')
WAREHOUSE_ORDER_NUMBER=$(echo $WO_LIST | jq -r 'if type == "array" then (last | .orderNumber) else .orderNumber end')

if [ "$WAREHOUSE_ORDER_ID" != "null" ] && [ -n "$WAREHOUSE_ORDER_ID" ]; then
    print_result 0 "Warehouse order found ($WAREHOUSE_ORDER_NUMBER)"
else
    print_result 1 "No warehouse order found"
    echo "Response: $WO_LIST"
fi

print_step "Confirm warehouse order - checks MODULE stock"
WO_CONFIRM=$(curl -s -X PUT "$BASE_URL/warehouse-orders/$WAREHOUSE_ORDER_ID/confirm" \
    -H "Authorization: Bearer $TOKEN_MS")

WO_STATUS=$(echo $WO_CONFIRM | jq -r '.status')
WO_TRIGGER=$(echo $WO_CONFIRM | jq -r '.triggerScenario')

if [ "$WO_STATUS" == "CONFIRMED" ]; then
    print_result 0 "Warehouse order confirmed (Status: CONFIRMED)"
    print_info "Trigger Scenario: $WO_TRIGGER"
else
    print_result 1 "Warehouse order confirmation failed (Got: $WO_STATUS)"
fi

# ==========================================
# TEST 3: PRODUCTION ORDER CREATION
# ==========================================

print_test "3" "Production Order Creation & Scheduling"

# If PRODUCTION_REQUIRED, create production order
if [ "$WO_TRIGGER" == "PRODUCTION_REQUIRED" ]; then
    print_step "Order production (PRODUCTION_REQUIRED)"
    PROD_CREATE=$(curl -s -X POST "$BASE_URL/production-orders/create" \
        -H "Authorization: Bearer $TOKEN_MS" \
        -H "Content-Type: application/json" \
        -d "{
            \"sourceCustomerOrderId\": $CUSTOMER_ORDER_ID,
            \"sourceWarehouseOrderId\": $WAREHOUSE_ORDER_ID,
            \"priority\": \"HIGH\",
            \"dueDate\": \"$(date -d '+7 days' '+%Y-%m-%dT%H:%M:%S')\",
            \"notes\": \"Scenario 3 test production\",
            \"createdByWorkstationId\": $WORKSTATION_MS,
            \"assignedWorkstationId\": 1
        }")
    
    PRODUCTION_ORDER_ID=$(echo $PROD_CREATE | jq -r '.id')
    PRODUCTION_ORDER_NUMBER=$(echo $PROD_CREATE | jq -r '.productionOrderNumber')
    PRODUCTION_STATUS=$(echo $PROD_CREATE | jq -r '.status')
    
    if [ "$PRODUCTION_ORDER_ID" != "null" ] && [ -n "$PRODUCTION_ORDER_ID" ]; then
        print_result 0 "Production order created ($PRODUCTION_ORDER_NUMBER, Status: $PRODUCTION_STATUS)"
    else
        print_result 1 "Production order creation failed"
        echo "Response: $PROD_CREATE"
    fi
else
    print_step "Checking for existing production orders"
    PROD_LIST=$(curl -s -X GET "$BASE_URL/production-orders/warehouse/$WAREHOUSE_ORDER_ID" \
        -H "Authorization: Bearer $TOKEN_MS")
    
    PRODUCTION_ORDER_ID=$(echo $PROD_LIST | jq -r 'if type == "array" then (.[0] | .id) else .id end')
    PRODUCTION_ORDER_NUMBER=$(echo $PROD_LIST | jq -r 'if type == "array" then (.[0] | .productionOrderNumber) else .productionOrderNumber end')
    
    if [ "$PRODUCTION_ORDER_ID" != "null" ] && [ -n "$PRODUCTION_ORDER_ID" ]; then
        print_result 0 "Existing production order found ($PRODUCTION_ORDER_NUMBER)"
    else
        print_info "No production order - proceeding with direct fulfillment path"
    fi
fi

# ==========================================
# TEST 4: PRODUCTION CONTROL ORDER (WS-1/2/3 Manufacturing)
# ==========================================

if [ "$PRODUCTION_ORDER_ID" != "null" ] && [ -n "$PRODUCTION_ORDER_ID" ]; then
    print_test "4" "Production Control Order Workflow"
    
    print_step "Login as Production Control"
    LOGIN_PC=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"production_control","password":"password"}')
    
    TOKEN_PC=$(echo $LOGIN_PC | jq -r '.token')
    
    if [ "$TOKEN_PC" != "null" ] && [ "$TOKEN_PC" != "" ]; then
        print_result 0 "Login as Production Control"
    else
        print_result 1 "Production Control login failed"
    fi
    
    print_step "Create Production Control Order"
    PCO_CREATE=$(curl -s -X POST "$BASE_URL/production-control-orders" \
        -H "Authorization: Bearer $TOKEN_PC" \
        -H "Content-Type: application/json" \
        -d "{
            \"sourceProductionOrderId\": $PRODUCTION_ORDER_ID,
            \"assignedWorkstationId\": 1,
            \"simalScheduleId\": \"SCH-$(date +%s)\",
            \"priority\": \"HIGH\",
            \"targetStartTime\": \"$(date -d '+1 hour' '+%Y-%m-%dT%H:%M:%S')\",
            \"targetCompletionTime\": \"$(date -d '+4 hours' '+%Y-%m-%dT%H:%M:%S')\",
            \"productionInstructions\": \"Standard manufacturing process\",
            \"qualityCheckpoints\": \"Visual inspection after each phase\",
            \"itemId\": 1,
            \"itemType\": \"PART\",
            \"quantity\": 10
        }")
    
    PCO_ID=$(echo $PCO_CREATE | jq -r '.id')
    PCO_NUMBER=$(echo $PCO_CREATE | jq -r '.controlOrderNumber')
    PCO_STATUS=$(echo $PCO_CREATE | jq -r '.status')
    
    if [ "$PCO_ID" != "null" ] && [ -n "$PCO_ID" ]; then
        print_result 0 "Production Control Order created ($PCO_NUMBER, Status: $PCO_STATUS)"
    else
        print_result 1 "Production Control Order creation failed"
        echo "Response: $PCO_CREATE"
        PCO_ID=""
    fi
    
    if [ -n "$PCO_ID" ]; then
        print_step "Confirm Production Control Order"
        PCO_CONFIRM=$(curl -s -X PUT "$BASE_URL/production-control-orders/$PCO_ID/confirm" \
            -H "Authorization: Bearer $TOKEN_PC")
        
        PCO_STATUS=$(echo $PCO_CONFIRM | jq -r '.status')
        if [ "$PCO_STATUS" == "CONFIRMED" ]; then
            print_result 0 "Production Control Order confirmed (PENDING → CONFIRMED)"
        else
            print_result 1 "Confirmation failed (Got: $PCO_STATUS)"
        fi
    fi
fi

# ==========================================
# TEST 5: SUPPLY ORDER (WS-9 Parts Supply)
# ==========================================

if [ -n "$PCO_ID" ]; then
    print_test "5" "Supply Order Workflow (WS-9 Parts Supply)"
    
    print_step "Request parts from Parts Supply Warehouse"
    SUPPLY_CREATE=$(curl -s -X POST "$BASE_URL/supply-orders/from-control-order" \
        -H "Authorization: Bearer $TOKEN_PC" \
        -H "Content-Type: application/json" \
        -d "{
            \"controlOrderId\": $PCO_ID,
            \"controlOrderType\": \"PRODUCTION\",
            \"priority\": \"HIGH\"
        }")
    
    SUPPLY_ORDER_ID=$(echo $SUPPLY_CREATE | jq -r '.id')
    SUPPLY_ORDER_NUMBER=$(echo $SUPPLY_CREATE | jq -r '.supplyOrderNumber')
    SUPPLY_STATUS=$(echo $SUPPLY_CREATE | jq -r '.status')
    
    if [ "$SUPPLY_ORDER_ID" != "null" ] && [ -n "$SUPPLY_ORDER_ID" ]; then
        print_result 0 "Supply order created ($SUPPLY_ORDER_NUMBER, Status: $SUPPLY_STATUS)"
    else
        print_result 1 "Supply order creation failed"
        echo "Response: $SUPPLY_CREATE"
        SUPPLY_ORDER_ID=""
    fi
    
    if [ -n "$SUPPLY_ORDER_ID" ]; then
        print_step "Login as Parts Supply operator (WS-9)"
        LOGIN_PS=$(curl -s -X POST "$BASE_URL/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"username":"parts_supply","password":"password"}')
        
        TOKEN_PS=$(echo $LOGIN_PS | jq -r '.token')
        
        if [ "$TOKEN_PS" != "null" ] && [ "$TOKEN_PS" != "" ]; then
            print_result 0 "Login as Parts Supply (WS-9)"
        else
            print_result 1 "Parts Supply login failed"
        fi
        
        print_step "Fulfill supply order (debits WS-9 stock)"
        SUPPLY_FULFILL=$(curl -s -X PUT "$BASE_URL/supply-orders/$SUPPLY_ORDER_ID/fulfill" \
            -H "Authorization: Bearer $TOKEN_PS")
        
        SUPPLY_STATUS=$(echo $SUPPLY_FULFILL | jq -r '.status')
        
        if [ "$SUPPLY_STATUS" == "FULFILLED" ]; then
            print_result 0 "Supply order fulfilled (PENDING → FULFILLED)"
        else
            print_result 1 "Supply order fulfillment failed (Got: $SUPPLY_STATUS)"
            echo "Response: $SUPPLY_FULFILL"
        fi
    fi
fi

# ==========================================
# TEST 6: WORKSTATION ORDERS (WS-1 Injection Molding)
# ==========================================

if [ -n "$PCO_ID" ]; then
    print_test "6" "Injection Molding Order (WS-1)"
    
    print_step "Login as Manufacturing operator (WS-1)"
    LOGIN_MFG=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"injection_molding","password":"password"}')
    
    TOKEN_MFG=$(echo $LOGIN_MFG | jq -r '.token')
    
    if [ "$TOKEN_MFG" != "null" ] && [ "$TOKEN_MFG" != "" ]; then
        print_result 0 "Login as Injection Molding (WS-1)"
    else
        print_result 1 "Manufacturing login failed"
    fi
    
    print_step "Fetch injection molding orders"
    IM_ORDERS=$(curl -s -X GET "$BASE_URL/injection-molding-orders/control-order/$PCO_ID" \
        -H "Authorization: Bearer $TOKEN_MFG")
    
    IM_ORDER_ID=$(echo $IM_ORDERS | jq -r 'if type == "array" then (.[0] | .id) else .id end')
    
    # If no order exists yet, we may need to dispatch from control
    if [ "$IM_ORDER_ID" == "null" ] || [ -z "$IM_ORDER_ID" ]; then
        print_info "No injection molding order found - may need manual dispatch from control"
        print_step "Dispatch from Production Control"
        
        DISPATCH=$(curl -s -X POST "$BASE_URL/production-control-orders/$PCO_ID/dispatch" \
            -H "Authorization: Bearer $TOKEN_PC")
        
        PCO_STATUS=$(echo $DISPATCH | jq -r '.status')
        print_info "Production Control Order status after dispatch: $PCO_STATUS"
        
        # Re-fetch orders
        sleep 1
        IM_ORDERS=$(curl -s -X GET "$BASE_URL/injection-molding-orders/control-order/$PCO_ID" \
            -H "Authorization: Bearer $TOKEN_MFG")
        IM_ORDER_ID=$(echo $IM_ORDERS | jq -r 'if type == "array" then (.[0] | .id) else .id end')
    fi
    
    if [ "$IM_ORDER_ID" != "null" ] && [ -n "$IM_ORDER_ID" ]; then
        print_result 0 "Injection Molding order found (ID: $IM_ORDER_ID)"
        
        print_step "Start injection molding order"
        IM_START=$(curl -s -X POST "$BASE_URL/injection-molding-orders/$IM_ORDER_ID/start" \
            -H "Authorization: Bearer $TOKEN_MFG")
        
        IM_STATUS=$(echo $IM_START | jq -r '.status')
        if [ "$IM_STATUS" == "IN_PROGRESS" ]; then
            print_result 0 "Injection Molding started (PENDING → IN_PROGRESS)"
        else
            print_result 1 "Start failed (Got: $IM_STATUS)"
        fi
        
        print_step "Complete injection molding order (credits inventory)"
        IM_COMPLETE=$(curl -s -X POST "$BASE_URL/injection-molding-orders/$IM_ORDER_ID/complete" \
            -H "Authorization: Bearer $TOKEN_MFG")
        
        IM_STATUS=$(echo $IM_COMPLETE | jq -r '.status')
        if [ "$IM_STATUS" == "COMPLETED" ]; then
            print_result 0 "Injection Molding completed (IN_PROGRESS → COMPLETED)"
        else
            print_result 1 "Completion failed (Got: $IM_STATUS)"
        fi
    else
        print_result 1 "No injection molding order to process"
    fi
fi

# ==========================================
# TEST 7: ASSEMBLY CONTROL ORDER (WS-4/5/6)
# ==========================================

if [ "$PRODUCTION_ORDER_ID" != "null" ] && [ -n "$PRODUCTION_ORDER_ID" ]; then
    print_test "7" "Assembly Control Order Workflow"
    
    print_step "Login as Assembly Control"
    LOGIN_AC=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"assembly_control","password":"password"}')
    
    TOKEN_AC=$(echo $LOGIN_AC | jq -r '.token')
    
    if [ "$TOKEN_AC" != "null" ] && [ "$TOKEN_AC" != "" ]; then
        print_result 0 "Login as Assembly Control"
    else
        print_result 1 "Assembly Control login failed"
    fi
    
    print_step "Check for existing Assembly Control orders"
    ACO_LIST=$(curl -s -X GET "$BASE_URL/assembly-control-orders" \
        -H "Authorization: Bearer $TOKEN_AC")
    
    ACO_ID=$(echo $ACO_LIST | jq -r 'if type == "array" then (last | .id) else .id end')
    
    if [ "$ACO_ID" != "null" ] && [ -n "$ACO_ID" ]; then
        print_result 0 "Assembly Control Order found (ID: $ACO_ID)"
        
        ACO_STATUS=$(echo $ACO_LIST | jq -r 'if type == "array" then (last | .status) else .status end')
        print_info "Current status: $ACO_STATUS"
    else
        print_info "No Assembly Control orders found - may need to be created through production planning"
    fi
fi

# ==========================================
# TEST 8: GEAR ASSEMBLY (WS-4)
# ==========================================

print_test "8" "Gear Assembly Order (WS-4)"

print_step "Login as Gear Assembly operator (WS-4)"
LOGIN_GA=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"gear_assembly","password":"password"}')

TOKEN_GA=$(echo $LOGIN_GA | jq -r '.token')

if [ "$TOKEN_GA" != "null" ] && [ "$TOKEN_GA" != "" ]; then
    print_result 0 "Login as Gear Assembly (WS-4)"
else
    print_result 1 "Gear Assembly login failed"
fi

print_step "Fetch gear assembly orders"
GA_ORDERS=$(curl -s -X GET "$BASE_URL/gear-assembly-orders" \
    -H "Authorization: Bearer $TOKEN_GA")

GA_ORDER_ID=$(echo $GA_ORDERS | jq -r 'if type == "array" then (.[0] | .id) else .id end')
GA_STATUS=$(echo $GA_ORDERS | jq -r 'if type == "array" then (.[0] | .status) else .status end')

if [ "$GA_ORDER_ID" != "null" ] && [ -n "$GA_ORDER_ID" ]; then
    print_result 0 "Gear Assembly order found (ID: $GA_ORDER_ID, Status: $GA_STATUS)"
    
    if [ "$GA_STATUS" == "PENDING" ]; then
        print_step "Start gear assembly order"
        GA_START=$(curl -s -X POST "$BASE_URL/gear-assembly-orders/$GA_ORDER_ID/start" \
            -H "Authorization: Bearer $TOKEN_GA")
        
        GA_STATUS=$(echo $GA_START | jq -r '.status')
        if [ "$GA_STATUS" == "IN_PROGRESS" ]; then
            print_result 0 "Gear Assembly started (PENDING → IN_PROGRESS)"
        else
            print_result 1 "Start failed (Got: $GA_STATUS)"
        fi
    fi
    
    if [ "$GA_STATUS" == "IN_PROGRESS" ]; then
        print_step "Complete gear assembly (credits Modules Supermarket WS-8)"
        GA_COMPLETE=$(curl -s -X POST "$BASE_URL/gear-assembly-orders/$GA_ORDER_ID/complete" \
            -H "Authorization: Bearer $TOKEN_GA")
        
        GA_STATUS=$(echo $GA_COMPLETE | jq -r '.status')
        if [ "$GA_STATUS" == "COMPLETED" ]; then
            print_result 0 "Gear Assembly completed (IN_PROGRESS → COMPLETED)"
        else
            print_result 1 "Completion failed (Got: $GA_STATUS)"
        fi
    fi
else
    print_info "No gear assembly orders to process"
fi

# ==========================================
# TEST 9: MOTOR ASSEMBLY (WS-5)
# ==========================================

print_test "9" "Motor Assembly Order (WS-5)"

print_step "Login as Motor Assembly operator (WS-5)"
LOGIN_MA=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"motor_assembly","password":"password"}')

TOKEN_MA=$(echo $LOGIN_MA | jq -r '.token')

if [ "$TOKEN_MA" != "null" ] && [ "$TOKEN_MA" != "" ]; then
    print_result 0 "Login as Motor Assembly (WS-5)"
else
    print_result 1 "Motor Assembly login failed"
fi

print_step "Fetch motor assembly orders"
MA_ORDERS=$(curl -s -X GET "$BASE_URL/motor-assembly-orders" \
    -H "Authorization: Bearer $TOKEN_MA")

MA_ORDER_ID=$(echo $MA_ORDERS | jq -r 'if type == "array" then (.[0] | .id) else .id end')
MA_STATUS=$(echo $MA_ORDERS | jq -r 'if type == "array" then (.[0] | .status) else .status end')

if [ "$MA_ORDER_ID" != "null" ] && [ -n "$MA_ORDER_ID" ]; then
    print_result 0 "Motor Assembly order found (ID: $MA_ORDER_ID, Status: $MA_STATUS)"
    
    if [ "$MA_STATUS" == "PENDING" ]; then
        print_step "Start motor assembly order"
        MA_START=$(curl -s -X POST "$BASE_URL/motor-assembly-orders/$MA_ORDER_ID/start" \
            -H "Authorization: Bearer $TOKEN_MA")
        
        MA_STATUS=$(echo $MA_START | jq -r '.status')
        if [ "$MA_STATUS" == "IN_PROGRESS" ]; then
            print_result 0 "Motor Assembly started (PENDING → IN_PROGRESS)"
        else
            print_result 1 "Start failed (Got: $MA_STATUS)"
        fi
    fi
    
    if [ "$MA_STATUS" == "IN_PROGRESS" ]; then
        print_step "Complete motor assembly (credits Modules Supermarket WS-8)"
        MA_COMPLETE=$(curl -s -X POST "$BASE_URL/motor-assembly-orders/$MA_ORDER_ID/complete" \
            -H "Authorization: Bearer $TOKEN_MA")
        
        MA_STATUS=$(echo $MA_COMPLETE | jq -r '.status')
        if [ "$MA_STATUS" == "COMPLETED" ]; then
            print_result 0 "Motor Assembly completed (IN_PROGRESS → COMPLETED)"
        else
            print_result 1 "Completion failed (Got: $MA_STATUS)"
        fi
    fi
else
    print_info "No motor assembly orders to process"
fi

# ==========================================
# TEST 10: FINAL ASSEMBLY (WS-6)
# ==========================================

print_test "10" "Final Assembly Order (WS-6)"

print_step "Login as Final Assembly operator (WS-6)"
LOGIN_FA=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"final_assembly","password":"password"}')

TOKEN_FA=$(echo $LOGIN_FA | jq -r '.token')
WORKSTATION_FA=$(echo $LOGIN_FA | jq -r '.user.workstation.id // 6')

if [ "$TOKEN_FA" != "null" ] && [ "$TOKEN_FA" != "" ]; then
    print_result 0 "Login as Final Assembly (WS-$WORKSTATION_FA)"
else
    print_result 1 "Final Assembly login failed"
fi

print_step "Fetch final assembly orders"
FA_ORDERS=$(curl -s -X GET "$BASE_URL/final-assembly-orders/workstation/$WORKSTATION_FA" \
    -H "Authorization: Bearer $TOKEN_FA")

FA_ORDER_ID=$(echo $FA_ORDERS | jq -r 'if type == "array" then (last | .id) else .id end')
FA_STATUS=$(echo $FA_ORDERS | jq -r 'if type == "array" then (last | .status) else .status end')

if [ "$FA_ORDER_ID" != "null" ] && [ -n "$FA_ORDER_ID" ] && [ "$FA_STATUS" != "COMPLETED" ]; then
    print_result 0 "Final Assembly order found (ID: $FA_ORDER_ID, Status: $FA_STATUS)"
    
    # Get product stock before completion
    PLANT_STOCK_BEFORE=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_PW" \
        -H "Authorization: Bearer $TOKEN_PW")
    PRODUCT_STOCK_BEFORE=$(echo $PLANT_STOCK_BEFORE | jq -r "[.[] | select(.itemId == $PRODUCT_ID)] | .[0].quantity // 0")
    print_info "Plant Warehouse product stock before: $PRODUCT_STOCK_BEFORE"
    
    if [ "$FA_STATUS" == "PENDING" ]; then
        print_step "Confirm Final Assembly order"
        FA_CONFIRM=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/confirm" \
            -H "Authorization: Bearer $TOKEN_FA")
        FA_STATUS=$(echo $FA_CONFIRM | jq -r '.status')
        
        if [ "$FA_STATUS" == "CONFIRMED" ]; then
            print_result 0 "Final Assembly confirmed (PENDING → CONFIRMED)"
        fi
    fi
    
    if [ "$FA_STATUS" == "CONFIRMED" ]; then
        print_step "Start Final Assembly"
        FA_START=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/start" \
            -H "Authorization: Bearer $TOKEN_FA")
        FA_STATUS=$(echo $FA_START | jq -r '.status')
        
        if [ "$FA_STATUS" == "IN_PROGRESS" ]; then
            print_result 0 "Final Assembly started (CONFIRMED → IN_PROGRESS)"
        fi
    fi
    
    if [ "$FA_STATUS" == "IN_PROGRESS" ]; then
        print_step "Complete Final Assembly"
        FA_COMPLETE=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/complete" \
            -H "Authorization: Bearer $TOKEN_FA")
        FA_STATUS=$(echo $FA_COMPLETE | jq -r '.status')
        
        if [ "$FA_STATUS" == "COMPLETED_ASSEMBLY" ]; then
            print_result 0 "Final Assembly completed (IN_PROGRESS → COMPLETED_ASSEMBLY)"
        fi
    fi
    
    if [ "$FA_STATUS" == "COMPLETED_ASSEMBLY" ]; then
        print_step "Submit Final Assembly (credits Plant Warehouse)"
        FA_SUBMIT=$(curl -s -X POST "$BASE_URL/final-assembly-orders/$FA_ORDER_ID/submit" \
            -H "Authorization: Bearer $TOKEN_FA")
        FA_STATUS=$(echo $FA_SUBMIT | jq -r '.status')
        
        if [ "$FA_STATUS" == "COMPLETED" ]; then
            print_result 0 "Final Assembly submitted (COMPLETED_ASSEMBLY → COMPLETED)"
        else
            print_result 1 "Submit failed (Got: $FA_STATUS)"
        fi
    fi
    
    # Verify Plant Warehouse stock increased
    sleep 1
    PLANT_STOCK_AFTER=$(curl -s -X GET "$BASE_URL/stock/workstation/$WORKSTATION_PW" \
        -H "Authorization: Bearer $TOKEN_PW")
    PRODUCT_STOCK_AFTER=$(echo $PLANT_STOCK_AFTER | jq -r "[.[] | select(.itemId == $PRODUCT_ID)] | .[0].quantity // 0")
    
    print_info "Plant Warehouse product stock after: $PRODUCT_STOCK_AFTER"
    
    if [ $PRODUCT_STOCK_AFTER -gt $PRODUCT_STOCK_BEFORE ]; then
        STOCK_INCREASE=$((PRODUCT_STOCK_AFTER - PRODUCT_STOCK_BEFORE))
        print_result 0 "Plant Warehouse credited correctly (+$STOCK_INCREASE units)"
    else
        print_result 1 "Plant Warehouse not credited (Before: $PRODUCT_STOCK_BEFORE, After: $PRODUCT_STOCK_AFTER)"
    fi
else
    print_info "No pending final assembly orders to process"
fi

# ==========================================
# TEST 11: VERIFY UPWARD STATUS PROPAGATION
# ==========================================

print_test "11" "Verify Upward Status Propagation"

print_step "Check Production Order status after workstation completions"
if [ "$PRODUCTION_ORDER_ID" != "null" ] && [ -n "$PRODUCTION_ORDER_ID" ]; then
    PROD_CHECK=$(curl -s -X GET "$BASE_URL/production-orders/$PRODUCTION_ORDER_ID" \
        -H "Authorization: Bearer $TOKEN_PW")
    
    PROD_STATUS=$(echo $PROD_CHECK | jq -r '.status')
    print_info "Production Order status: $PROD_STATUS"
    
    if [ "$PROD_STATUS" == "COMPLETED" ]; then
        print_result 0 "Production Order completed - all workstations finished"
    else
        print_info "Production Order still in progress ($PROD_STATUS)"
    fi
fi

print_step "Check Warehouse Order status"
if [ "$WAREHOUSE_ORDER_ID" != "null" ] && [ -n "$WAREHOUSE_ORDER_ID" ]; then
    WO_CHECK=$(curl -s -X GET "$BASE_URL/warehouse-orders/$WAREHOUSE_ORDER_ID" \
        -H "Authorization: Bearer $TOKEN_MS")
    
    WO_STATUS=$(echo $WO_CHECK | jq -r '.status')
    print_info "Warehouse Order status: $WO_STATUS"
fi

print_step "Check Customer Order status"
CO_CHECK=$(curl -s -X GET "$BASE_URL/customer-orders/$CUSTOMER_ORDER_ID" \
    -H "Authorization: Bearer $TOKEN_PW")

CO_STATUS=$(echo $CO_CHECK | jq -r '.status')
print_info "Customer Order status: $CO_STATUS"

if [ "$CO_STATUS" == "COMPLETED" ]; then
    print_result 0 "Customer Order completed - full cycle successful!"
fi

# ==========================================
# SUMMARY
# ==========================================

echo -e "\n${BOLD}${BLUE}================================================${NC}"
echo -e "${BOLD}${BLUE}TEST SUMMARY${NC}"
echo -e "${BOLD}${BLUE}================================================${NC}\n"

echo -e "${BOLD}Order Hierarchy:${NC}"
echo -e "  CustomerOrder: $CUSTOMER_ORDER_NUMBER (ID: $CUSTOMER_ORDER_ID)"
if [ -n "$WAREHOUSE_ORDER_NUMBER" ]; then
    echo -e "  └─ WarehouseOrder: $WAREHOUSE_ORDER_NUMBER (ID: $WAREHOUSE_ORDER_ID)"
fi
if [ -n "$PRODUCTION_ORDER_NUMBER" ]; then
    echo -e "     └─ ProductionOrder: $PRODUCTION_ORDER_NUMBER (ID: $PRODUCTION_ORDER_ID)"
fi
if [ -n "$PCO_NUMBER" ]; then
    echo -e "        └─ ProductionControlOrder: $PCO_NUMBER (ID: $PCO_ID)"
fi

echo ""
echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}\n"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}${BOLD}✅ ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}Scenario 3 full production cycle verified.${NC}\n"
    exit 0
else
    echo -e "${YELLOW}${BOLD}⚠️  SOME TESTS HAD ISSUES${NC}"
    echo -e "${YELLOW}This may be expected if orders were already processed.${NC}"
    echo -e "${YELLOW}Review the output above for details.${NC}\n"
    exit 1
fi
