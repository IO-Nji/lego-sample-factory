#!/bin/bash

# Test script for Module Production Workstations (Scenario 3 workflow)
# Tests that modules are properly classified by production workstation
# and that both Production Control and Assembly Control orders are created

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# API base URL
API_URL="http://localhost:1011/api"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Module Production Workstations Test${NC}"
echo -e "${BLUE}========================================${NC}"

# Step 1: Login
echo -e "\n${YELLOW}Step 1: Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"lego_admin","password":"password"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ Login failed${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Login successful${NC}"

# Step 2: Check module production workstations
echo -e "\n${YELLOW}Step 2: Checking module production workstations...${NC}"
MODULES=$(curl -s -X GET "$API_URL/masterdata/modules" \
  -H "Authorization: Bearer $TOKEN")

echo "Truck Modules:"
echo $MODULES | jq -r '.[] | select(.name | contains("Truck")) | "  - \(.name) → WS-\(.productionWorkstationId // "NONE")"'

echo "House Modules:"
echo $MODULES | jq -r '.[] | select(.name | contains("House")) | "  - \(.name) → WS-\(.productionWorkstationId // "NONE")"'

# Step 2.5: Deplete module stock at Modules Supermarket to force production
echo -e "\n${YELLOW}Step 2.5: Depleting module stock at Modules Supermarket...${NC}"
# Product 1 requires modules 1-6 (Truck modules)
for MODULE_ID in 1 2 3 4 5 6; do
  STOCK=$(curl -s -X GET "$API_URL/stock/workstation/8?itemType=MODULE&itemId=$MODULE_ID" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.[0].quantity // 0')
  
  if [ "$STOCK" -gt 0 ]; then
    curl -s -X POST "$API_URL/stock/adjust" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"workstationId\": 8,
        \"itemType\": \"MODULE\",
        \"itemId\": $MODULE_ID,
        \"delta\": -$STOCK,
        \"notes\": \"Test depletion\"
      }" > /dev/null
  fi
done
echo -e "${GREEN}✓ Module stock depleted at Modules Supermarket${NC}"

# Step 3: Create a Customer Order (as Plant Warehouse)
echo -e "\n${YELLOW}Step 3: Creating Customer Order...${NC}"
CUSTOMER_ORDER=$(curl -s -X POST "$API_URL/customer-orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "workstationId": 7,
    "orderItems": [
      {"itemType": "PRODUCT", "itemId": 1, "quantity": 2}
    ],
    "notes": "Test order for module production workstations"
  }')

CUSTOMER_ORDER_ID=$(echo $CUSTOMER_ORDER | jq -r '.id')
CUSTOMER_ORDER_NUMBER=$(echo $CUSTOMER_ORDER | jq -r '.orderNumber')
echo -e "${GREEN}✓ Customer Order created: $CUSTOMER_ORDER_NUMBER (ID: $CUSTOMER_ORDER_ID)${NC}"

# Step 4: Confirm Customer Order
echo -e "\n${YELLOW}Step 4: Confirming Customer Order...${NC}"
curl -s -X PUT "$API_URL/customer-orders/$CUSTOMER_ORDER_ID/confirm" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

# Check if warehouse order was created
CUSTOMER_ORDER_UPDATED=$(curl -s -X GET "$API_URL/customer-orders/$CUSTOMER_ORDER_ID" \
  -H "Authorization: Bearer $TOKEN")

TRIGGER_SCENARIO=$(echo $CUSTOMER_ORDER_UPDATED | jq -r '.triggerScenario')
echo "Trigger Scenario: $TRIGGER_SCENARIO"

if [ "$TRIGGER_SCENARIO" != "WAREHOUSE_ORDER_NEEDED" ]; then
  echo -e "${RED}❌ Expected WAREHOUSE_ORDER_NEEDED scenario${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Customer Order confirmed - Warehouse Order needed${NC}"

# Step 5: Fulfill Customer Order to create Warehouse Order
echo -e "\n${YELLOW}Step 5: Fulfilling Customer Order to create Warehouse Order...${NC}"
curl -s -X PUT "$API_URL/customer-orders/$CUSTOMER_ORDER_ID/fulfill" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

# Wait for warehouse order creation
sleep 3

# Fetch warehouse orders for Modules Supermarket (WS-8)
WAREHOUSE_ORDERS=$(curl -s -X GET "$API_URL/warehouse-orders/workstation/8" \
  -H "Authorization: Bearer $TOKEN")

WAREHOUSE_ORDER_ID=$(echo $WAREHOUSE_ORDERS | jq -r '[.[] | select(.status == "PENDING")] | .[0].id')
WAREHOUSE_ORDER_NUMBER=$(echo $WAREHOUSE_ORDERS | jq -r '[.[] | select(.status == "PENDING")] | .[0].orderNumber')
echo -e "${GREEN}✓ Warehouse Order created: $WAREHOUSE_ORDER_NUMBER (ID: $WAREHOUSE_ORDER_ID)${NC}"

# Step 6: Confirm Warehouse Order
echo -e "\n${YELLOW}Step 6: Confirming Warehouse Order...${NC}"
curl -s -X PUT "$API_URL/warehouse-orders/$WAREHOUSE_ORDER_ID/confirm" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

WAREHOUSE_ORDER_UPDATED=$(curl -s -X GET "$API_URL/warehouse-orders/$WAREHOUSE_ORDER_ID" \
  -H "Authorization: Bearer $TOKEN")

WO_TRIGGER=$(echo $WAREHOUSE_ORDER_UPDATED | jq -r '.triggerScenario')
echo "Warehouse Order Trigger Scenario: $WO_TRIGGER"

if [ "$WO_TRIGGER" != "PRODUCTION_REQUIRED" ]; then
  echo -e "${RED}❌ Expected PRODUCTION_REQUIRED scenario${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Warehouse Order confirmed - Production required${NC}"

# Step 7: Create Production Order
echo -e "\n${YELLOW}Step 7: Creating Production Order...${NC}"
PRODUCTION_ORDER=$(curl -s -X POST "$API_URL/production-orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"sourceWarehouseOrderId\": $WAREHOUSE_ORDER_ID,
    \"priority\": \"NORMAL\",
    \"notes\": \"Test production order for module workstation classification\",
    \"createdByWorkstationId\": 8
  }")

PRODUCTION_ORDER_ID=$(echo $PRODUCTION_ORDER | jq -r '.id')
PRODUCTION_ORDER_NUMBER=$(echo $PRODUCTION_ORDER | jq -r '.productionOrderNumber')
echo -e "${GREEN}✓ Production Order created: $PRODUCTION_ORDER_NUMBER (ID: $PRODUCTION_ORDER_ID)${NC}"

# Check production order items
echo -e "\n${BLUE}Production Order Items:${NC}"
echo $PRODUCTION_ORDER | jq -r '.productionOrderItems[] | "  - \(.itemName) (MODULE ID: \(.itemId)) × \(.quantity) → \(.workstationType)"'

# Count manufacturing vs assembly items
MANUFACTURING_COUNT=$(echo $PRODUCTION_ORDER | jq '[.productionOrderItems[] | select(.workstationType == "MANUFACTURING")] | length')
ASSEMBLY_COUNT=$(echo $PRODUCTION_ORDER | jq '[.productionOrderItems[] | select(.workstationType == "ASSEMBLY")] | length')

echo -e "\n${BLUE}Workstation Type Distribution:${NC}"
echo "  - MANUFACTURING items: $MANUFACTURING_COUNT"
echo "  - ASSEMBLY items: $ASSEMBLY_COUNT"

if [ "$MANUFACTURING_COUNT" -eq "0" ]; then
  echo -e "${RED}❌ No MANUFACTURING items found${NC}"
  exit 1
fi

if [ "$ASSEMBLY_COUNT" -eq "0" ]; then
  echo -e "${RED}❌ No ASSEMBLY items found${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Both MANUFACTURING and ASSEMBLY items present${NC}"

# Step 8: Schedule with SimAL
echo -e "\n${YELLOW}Step 8: Scheduling with SimAL...${NC}"
curl -s -X POST "$API_URL/simal/schedule" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productionOrderId\": $PRODUCTION_ORDER_ID}" > /dev/null

sleep 2  # Wait for scheduling to complete

# Step 9: Dispatch Production Order
echo -e "\n${YELLOW}Step 9: Dispatching Production Order...${NC}"
curl -s -X POST "$API_URL/production-planning/$PRODUCTION_ORDER_ID/dispatch" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

sleep 2  # Wait for control orders to be created

# Step 10: Check Production Control Orders
echo -e "\n${YELLOW}Step 10: Checking Production Control Orders...${NC}"
PRODUCTION_CONTROL_ORDERS=$(curl -s -X GET "$API_URL/production-control-orders" \
  -H "Authorization: Bearer $TOKEN")

PCO_COUNT=$(echo $PRODUCTION_CONTROL_ORDERS | jq 'length')
echo "Production Control Orders found: $PCO_COUNT"

if [ "$PCO_COUNT" -eq "0" ]; then
  echo -e "${RED}❌ No Production Control Orders created${NC}"
  exit 1
fi

echo $PRODUCTION_CONTROL_ORDERS | jq -r '.[] | "  - \(.controlOrderNumber) for \(.itemName) (MODULE ID: \(.itemId)) at WS-\(.assignedWorkstationId)"'
echo -e "${GREEN}✓ Production Control Orders created${NC}"

# Step 11: Check Assembly Control Orders
echo -e "\n${YELLOW}Step 11: Checking Assembly Control Orders...${NC}"
ASSEMBLY_CONTROL_ORDERS=$(curl -s -X GET "$API_URL/assembly-control-orders" \
  -H "Authorization: Bearer $TOKEN")

ACO_COUNT=$(echo $ASSEMBLY_CONTROL_ORDERS | jq 'length')
echo "Assembly Control Orders found: $ACO_COUNT"

if [ "$ACO_COUNT" -eq "0" ]; then
  echo -e "${RED}❌ No Assembly Control Orders created${NC}"
  exit 1
fi

echo $ASSEMBLY_CONTROL_ORDERS | jq -r '.[] | "  - \(.controlOrderNumber) for \(.itemName) (MODULE ID: \(.itemId)) at WS-\(.assignedWorkstationId)"'
echo -e "${GREEN}✓ Assembly Control Orders created${NC}"

# Final Summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Production Control Orders: $PCO_COUNT${NC}"
echo -e "${GREEN}Assembly Control Orders: $ACO_COUNT${NC}"
echo -e "${GREEN}Total Control Orders: $((PCO_COUNT + ACO_COUNT))${NC}"
