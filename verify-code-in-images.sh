#!/bin/bash

# Code Verification Script
# Compares actual code in Docker images vs git repository
# Helps identify git merge issues or stale builds

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=================================================="
echo "  CODE VERIFICATION: Docker Image vs Git Repo"
echo "=================================================="
echo ""

# Get current git state
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}GIT STATE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
GIT_COMMIT=$(git rev-parse --short HEAD)
GIT_BRANCH=$(git branch --show-current)
GIT_COMMIT_MSG=$(git log -1 --oneline --format="%s")
UNCOMMITTED=$(git status --short | wc -l)

echo "Current Branch:  ${GIT_BRANCH}"
echo "Current Commit:  ${GIT_COMMIT}"
echo "Commit Message:  ${GIT_COMMIT_MSG}"
echo "Uncommitted:     ${UNCOMMITTED} files"
echo ""

if [ "$UNCOMMITTED" -gt 0 ]; then
    echo -e "${YELLOW}⚠ Warning: You have uncommitted changes${NC}"
    git status --short
    echo ""
fi

# Check critical frontend files
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}FRONTEND: Checking Critical Files${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Extract key code snippets from git
echo "Extracting code from git repository..."
MODULES_DASHBOARD_GIT=$(grep -A 10 "triggerScenario === 'PRODUCTION_REQUIRED'" lego-factory-frontend/src/pages/dashboards/ModulesSupermarketDashboard.jsx 2>/dev/null || echo "NOT FOUND")
WAREHOUSE_CARD_GIT=$(grep -A 10 "triggerScenario.includes('PRODUCTION')" lego-factory-frontend/src/components/WarehouseOrderCard.jsx 2>/dev/null || echo "NOT FOUND")

# Extract from Docker image
echo "Extracting code from Docker image..."
mkdir -p /tmp/image-verify
docker run --rm --entrypoint sh lego-sample-factory-frontend:latest -c "cat /usr/share/nginx/html/assets/*.js" > /tmp/image-verify/frontend-bundle.js 2>/dev/null || echo "Failed to extract"

# Check if key logic exists in bundle
if [ -f /tmp/image-verify/frontend-bundle.js ]; then
    BUNDLE_SIZE=$(wc -c < /tmp/image-verify/frontend-bundle.js)
    echo "Frontend bundle size: ${BUNDLE_SIZE} bytes"
    
    # Search for key strings that should be in the bundle (minified)
    if grep -q "PRODUCTION_REQUIRED" /tmp/image-verify/frontend-bundle.js; then
        echo -e "${GREEN}✓ Found 'PRODUCTION_REQUIRED' in bundle${NC}"
    else
        echo -e "${RED}✗ Missing 'PRODUCTION_REQUIRED' in bundle${NC}"
    fi
    
    if grep -q "DIRECT_FULFILLMENT" /tmp/image-verify/frontend-bundle.js; then
        echo -e "${GREEN}✓ Found 'DIRECT_FULFILLMENT' in bundle${NC}"
    else
        echo -e "${RED}✗ Missing 'DIRECT_FULFILLMENT' in bundle${NC}"
    fi
    
    if grep -q "Order Production" /tmp/image-verify/frontend-bundle.js; then
        echo -e "${GREEN}✓ Found 'Order Production' button text in bundle${NC}"
    else
        echo -e "${RED}✗ Missing 'Order Production' button text${NC}"
    fi
    
    if grep -q "createFinalAssemblyOrdersFromWarehouseOrder" /tmp/image-verify/frontend-bundle.js; then
        echo -e "${GREEN}✓ Found 'createFinalAssemblyOrdersFromWarehouseOrder' method${NC}"
    else
        echo -e "${YELLOW}⚠ Missing 'createFinalAssemblyOrdersFromWarehouseOrder' (may be minified)${NC}"
    fi
else
    echo -e "${RED}✗ Failed to extract frontend bundle${NC}"
fi
echo ""

# Check backend JAR files
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}BACKEND: Checking JAR Files${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Check order-processing-service
echo "Checking order-processing-service JAR..."
docker run --rm --entrypoint sh lego-sample-factory-order-processing-service:latest -c "ls -lh /app/*.jar" 2>/dev/null || echo "Failed to list JAR"

# Extract and check WarehouseOrderService class
docker run --rm --entrypoint sh lego-sample-factory-order-processing-service:latest -c "unzip -p /app/*.jar BOOT-INF/classes/io/life/order_processing_service/service/WarehouseOrderService.class" > /tmp/image-verify/WarehouseOrderService.class 2>/dev/null || echo "Failed to extract"

if [ -f /tmp/image-verify/WarehouseOrderService.class ]; then
    CLASS_SIZE=$(wc -c < /tmp/image-verify/WarehouseOrderService.class)
    echo "WarehouseOrderService.class size: ${CLASS_SIZE} bytes"
    
    # Use strings to search for method names in bytecode
    if strings /tmp/image-verify/WarehouseOrderService.class | grep -q "createFinalAssemblyOrdersFromWarehouseOrder"; then
        echo -e "${GREEN}✓ Found 'createFinalAssemblyOrdersFromWarehouseOrder' method in JAR${NC}"
    else
        echo -e "${RED}✗ Missing 'createFinalAssemblyOrdersFromWarehouseOrder' method${NC}"
    fi
    
    if strings /tmp/image-verify/WarehouseOrderService.class | grep -q "triggerScenario"; then
        echo -e "${GREEN}✓ Found 'triggerScenario' field usage${NC}"
    else
        echo -e "${RED}✗ Missing 'triggerScenario' field usage${NC}"
    fi
else
    echo -e "${RED}✗ Failed to extract WarehouseOrderService.class${NC}"
fi
echo ""

# Compare local build timestamp vs image timestamp
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}BUILD TIMESTAMPS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo "Frontend Image:"
docker images --format "  Created: {{.CreatedAt}}" lego-sample-factory-frontend:latest

echo "Order Processing Image:"
docker images --format "  Created: {{.CreatedAt}}" lego-sample-factory-order-processing-service:latest

echo ""
echo "Git Last Commit:"
git log -1 --format="  Date: %ai%n  Hash: %h%n  Msg:  %s"
echo ""

# Check if images were built AFTER last git commit
IMAGE_DATE=$(docker images --format "{{.CreatedAt}}" lego-sample-factory-frontend:latest | cut -d' ' -f1-2)
COMMIT_DATE=$(git log -1 --format="%ai" | cut -d' ' -f1-2)

echo "Comparison:"
echo "  Git Commit:  ${COMMIT_DATE}"
echo "  Image Built: ${IMAGE_DATE}"

if [[ "$IMAGE_DATE" > "$COMMIT_DATE" ]] || [[ "$IMAGE_DATE" == "$COMMIT_DATE" ]]; then
    echo -e "${GREEN}✓ Images built after last commit${NC}"
else
    echo -e "${RED}✗ Images built BEFORE last commit - REBUILD NEEDED${NC}"
fi
echo ""

# Registry comparison
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}REGISTRY COMPARISON${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

LOCAL_FRONTEND_ID=$(docker images --format "{{.ID}}" lego-sample-factory-frontend:latest)
LOCAL_ORDER_ID=$(docker images --format "{{.ID}}" lego-sample-factory-order-processing-service:latest)

REGISTRY_FRONTEND_ID=$(docker images --format "{{.ID}}" 192.168.1.237:5000/lego-sample-factory-frontend:latest 2>/dev/null || echo "NOT FOUND")
REGISTRY_ORDER_ID=$(docker images --format "{{.ID}}" 192.168.1.237:5000/lego-sample-factory-order-processing-service:latest 2>/dev/null || echo "NOT FOUND")

echo "Frontend:"
echo "  Local:    ${LOCAL_FRONTEND_ID}"
echo "  Registry: ${REGISTRY_FRONTEND_ID}"
if [ "$LOCAL_FRONTEND_ID" == "$REGISTRY_ORDER_ID" ]; then
    echo -e "  ${GREEN}✓ Match${NC}"
else
    echo -e "  ${RED}✗ Mismatch - Push needed${NC}"
fi

echo ""
echo "Order Processing:"
echo "  Local:    ${LOCAL_ORDER_ID}"
echo "  Registry: ${REGISTRY_ORDER_ID}"
if [ "$LOCAL_ORDER_ID" == "$REGISTRY_ORDER_ID" ]; then
    echo -e "  ${GREEN}✓ Match${NC}"
else
    echo -e "  ${RED}✗ Mismatch - Push needed${NC}"
fi
echo ""

# Cleanup
rm -rf /tmp/image-verify

# Final recommendations
echo "=================================================="
echo -e "${BLUE}RECOMMENDATIONS${NC}"
echo "=================================================="
echo ""

if [ "$UNCOMMITTED" -gt 0 ]; then
    echo -e "${YELLOW}1. Commit your changes:${NC}"
    echo "   git add ."
    echo "   git commit -m \"Your message\""
    echo ""
fi

if [ "$LOCAL_FRONTEND_ID" != "$REGISTRY_FRONTEND_ID" ] || [ "$LOCAL_ORDER_ID" != "$REGISTRY_ORDER_ID" ]; then
    echo -e "${YELLOW}2. Push to registry:${NC}"
    echo "   ./push-to-registry.sh"
    echo ""
fi

echo -e "${YELLOW}3. Verify server has same git commit:${NC}"
echo "   ssh nji@io-surf 'cd ~/lego-sample-factory && git log -1 --oneline'"
echo ""

echo -e "${YELLOW}4. Check server image versions:${NC}"
echo "   ssh nji@io-surf 'cd ~/lego-sample-factory && docker images | grep lego-sample-factory'"
echo ""

echo -e "${YELLOW}5. If server git differs, sync it:${NC}"
echo "   ssh nji@io-surf 'cd ~/lego-sample-factory && git fetch && git checkout dev && git pull'"
echo "   ssh nji@io-surf 'cd ~/lego-sample-factory && ./update-from-registry.sh'"
echo ""

echo "For server diagnostics, copy and run:"
echo "  scp diagnose-frontend-cache.sh nji@io-surf:~/lego-sample-factory/"
echo "  ssh nji@io-surf 'cd ~/lego-sample-factory && ./diagnose-frontend-cache.sh'"
