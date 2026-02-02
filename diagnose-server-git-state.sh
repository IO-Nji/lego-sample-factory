#!/bin/bash

# Server Git State Diagnostic
# Finds the actual project location and compares git state with local

set +e  # Don't exit on errors

echo "=================================================="
echo "  SERVER GIT STATE DIAGNOSTIC"
echo "=================================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Local info
LOCAL_COMMIT=$(git rev-parse --short HEAD 2>/dev/null)
LOCAL_BRANCH=$(git branch --show-current 2>/dev/null)

echo "Local Development Machine:"
echo "  Branch: ${LOCAL_BRANCH}"
echo "  Commit: ${LOCAL_COMMIT}"
echo ""

# Server connection info
SERVER_USER="nji"
SERVER_HOST="io-surf"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. Finding project location on server..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Try common locations
POSSIBLE_PATHS=(
    "~/lego-sample-factory"
    "~/Documents/DEV/Java/LIFE/lego-sample-factory"
    "~/projects/lego-sample-factory"
    "~/dev/lego-sample-factory"
    "/var/www/lego-sample-factory"
    "/opt/lego-sample-factory"
)

SERVER_PATH=""
for path in "${POSSIBLE_PATHS[@]}"; do
    echo -n "Checking: ${path} ... "
    if ssh ${SERVER_USER}@${SERVER_HOST} "test -d ${path}" 2>/dev/null; then
        echo -e "${GREEN}✓ Found${NC}"
        SERVER_PATH="${path}"
        break
    else
        echo "not found"
    fi
done

if [ -z "$SERVER_PATH" ]; then
    echo -e "${RED}✗ Cannot find project on server!${NC}"
    echo ""
    echo "Searching for docker-compose.yml files on server..."
    ssh ${SERVER_USER}@${SERVER_HOST} "find ~ -name 'docker-compose.yml' -type f 2>/dev/null | grep lego"
    echo ""
    echo -e "${YELLOW}ACTION REQUIRED:${NC}"
    echo "1. Clone the repository on the server if it doesn't exist:"
    echo "   ssh ${SERVER_USER}@${SERVER_HOST}"
    echo "   git clone <repository-url> ~/lego-sample-factory"
    echo "   cd ~/lego-sample-factory"
    echo "   git checkout dev"
    echo ""
    echo "2. Or specify the correct path and re-run this script"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Found project at: ${SERVER_PATH}${NC}"
echo ""

# Check git status on server
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. Checking git state on server..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

SERVER_GIT_OUTPUT=$(ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && git rev-parse --short HEAD 2>&1 && git branch --show-current 2>&1 && git status --short 2>&1")

SERVER_COMMIT=$(echo "$SERVER_GIT_OUTPUT" | head -1)
SERVER_BRANCH=$(echo "$SERVER_GIT_OUTPUT" | sed -n '2p')
SERVER_STATUS=$(echo "$SERVER_GIT_OUTPUT" | tail -n +3)

echo "Server Git State:"
echo "  Branch: ${SERVER_BRANCH}"
echo "  Commit: ${SERVER_COMMIT}"
echo ""

if [ -n "$SERVER_STATUS" ]; then
    echo -e "${YELLOW}⚠ Server has uncommitted changes:${NC}"
    echo "$SERVER_STATUS"
else
    echo -e "${GREEN}✓ Server working directory is clean${NC}"
fi
echo ""

# Compare commits
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. Comparing commits..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$LOCAL_COMMIT" == "$SERVER_COMMIT" ]; then
    echo -e "${GREEN}✓ Commits MATCH${NC}"
    echo "  Both on: ${LOCAL_COMMIT}"
else
    echo -e "${RED}✗ Commits DIFFER - This is the problem!${NC}"
    echo "  Local:  ${LOCAL_COMMIT} (${LOCAL_BRANCH})"
    echo "  Server: ${SERVER_COMMIT} (${SERVER_BRANCH})"
    echo ""
    
    # Show commit differences
    echo "Commits on local that server doesn't have:"
    git log --oneline ${SERVER_COMMIT}..${LOCAL_COMMIT} 2>/dev/null | head -10
    echo ""
fi

# Compare critical backend files
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. Comparing critical backend files..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

CRITICAL_FILES=(
    "lego-factory-backend/order-processing-service/src/main/java/io/life/order_processing/service/FulfillmentService.java"
    "lego-factory-backend/order-processing-service/src/main/java/io/life/order_processing/service/WarehouseOrderService.java"
    "lego-factory-backend/order-processing-service/src/main/java/io/life/order_processing/controller/WarehouseOrderController.java"
)

for file in "${CRITICAL_FILES[@]}"; do
    echo -n "Checking: $(basename $file) ... "
    
    # Get file hash locally
    LOCAL_HASH=$(md5sum "$file" 2>/dev/null | awk '{print $1}')
    
    # Get file hash on server
    SERVER_HASH=$(ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && md5sum ${file} 2>/dev/null" | awk '{print $1}')
    
    if [ -z "$LOCAL_HASH" ] || [ -z "$SERVER_HASH" ]; then
        echo -e "${RED}✗ File not found${NC}"
    elif [ "$LOCAL_HASH" == "$SERVER_HASH" ]; then
        echo -e "${GREEN}✓ Match${NC}"
    else
        echo -e "${RED}✗ DIFFER${NC}"
        echo "    Local:  ${LOCAL_HASH}"
        echo "    Server: ${SERVER_HASH}"
    fi
done
echo ""

# Compare critical frontend files
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. Comparing critical frontend files..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

FRONTEND_FILES=(
    "lego-factory-frontend/src/pages/dashboards/ModulesSupermarketDashboard.jsx"
    "lego-factory-frontend/src/components/WarehouseOrderCard.jsx"
    "lego-factory-frontend/src/pages/dashboards/PlantWarehouseDashboard.jsx"
)

for file in "${FRONTEND_FILES[@]}"; do
    echo -n "Checking: $(basename $file) ... "
    
    LOCAL_HASH=$(md5sum "$file" 2>/dev/null | awk '{print $1}')
    SERVER_HASH=$(ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && md5sum ${file} 2>/dev/null" | awk '{print $1}')
    
    if [ -z "$LOCAL_HASH" ] || [ -z "$SERVER_HASH" ]; then
        echo -e "${RED}✗ File not found${NC}"
    elif [ "$LOCAL_HASH" == "$SERVER_HASH" ]; then
        echo -e "${GREEN}✓ Match${NC}"
    else
        echo -e "${RED}✗ DIFFER${NC}"
        echo "    Local:  ${LOCAL_HASH}"
        echo "    Server: ${SERVER_HASH}"
    fi
done
echo ""

# Check what images the server is actually running
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "6. Checking running containers on server..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

SERVER_CONTAINERS=$(ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'" 2>/dev/null)
echo "$SERVER_CONTAINERS"
echo ""

# Check server's pulled images
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "7. Checking images on server..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

SERVER_IMAGES=$(ssh ${SERVER_USER}@${SERVER_HOST} "docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}' | grep -E '(lego-sample-factory|192.168.1.237)' | head -10" 2>/dev/null)
echo "$SERVER_IMAGES"
echo ""

# Summary and action plan
echo "=================================================="
echo "  DIAGNOSIS SUMMARY"
echo "=================================================="
echo ""

NEEDS_GIT_PULL=false
NEEDS_REBUILD=false
NEEDS_REGISTRY_PULL=false

if [ "$LOCAL_COMMIT" != "$SERVER_COMMIT" ]; then
    echo -e "${RED}ISSUE FOUND: Git commits don't match${NC}"
    echo "  Local:  ${LOCAL_COMMIT}"
    echo "  Server: ${SERVER_COMMIT}"
    echo ""
    NEEDS_GIT_PULL=true
fi

# Check if server is using registry or local build
if echo "$SERVER_IMAGES" | grep -q "192.168.1.237:5000"; then
    echo -e "${BLUE}Server is using registry images${NC}"
    NEEDS_REGISTRY_PULL=true
else
    echo -e "${BLUE}Server is using locally built images${NC}"
    NEEDS_REBUILD=true
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  RECOMMENDED ACTIONS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ "$NEEDS_GIT_PULL" = true ]; then
    echo -e "${YELLOW}1. UPDATE SERVER GIT REPOSITORY${NC}"
    echo "   ssh ${SERVER_USER}@${SERVER_HOST}"
    echo "   cd ${SERVER_PATH}"
    echo "   git fetch origin"
    echo "   git checkout ${LOCAL_BRANCH}"
    echo "   git pull origin ${LOCAL_BRANCH}"
    echo "   # OR reset to exact commit:"
    echo "   git reset --hard ${LOCAL_COMMIT}"
    echo ""
fi

if [ "$NEEDS_REGISTRY_PULL" = true ]; then
    echo -e "${YELLOW}2. PULL LATEST IMAGES FROM REGISTRY${NC}"
    echo "   ssh ${SERVER_USER}@${SERVER_HOST}"
    echo "   cd ${SERVER_PATH}"
    echo "   ./update-from-registry.sh"
    echo ""
elif [ "$NEEDS_REBUILD" = true ]; then
    echo -e "${YELLOW}2. REBUILD ON SERVER${NC}"
    echo "   ssh ${SERVER_USER}@${SERVER_HOST}"
    echo "   cd ${SERVER_PATH}"
    echo "   docker compose build --no-cache"
    echo "   docker compose down"
    echo "   docker compose up -d"
    echo ""
fi

echo -e "${YELLOW}3. VERIFY FIX${NC}"
echo "   1. Check containers are running:"
echo "      docker compose ps"
echo ""
echo "   2. Check logs for errors:"
echo "      docker compose logs -f order-processing-service"
echo ""
echo "   3. Test Scenario 2 in browser:"
echo "      - Login as modules_supermarket"
echo "      - Confirm warehouse order"
echo "      - Verify correct button appears"
echo "      - Check final assembly order created"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Server Path: ${SERVER_PATH}"
echo "Script Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
