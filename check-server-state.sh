#!/bin/bash

# Remote Server Code Verification
# Checks git state and Docker images on live server
# Run this from your local machine

SERVER="nji@io-surf"
SERVER_DIR="~/lego-sample-factory"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=================================================="
echo "  LIVE SERVER CODE VERIFICATION"
echo "=================================================="
echo ""
echo "Connecting to: ${SERVER}"
echo "Directory: ${SERVER_DIR}"
echo ""

# Check if server is reachable
if ! ssh -q ${SERVER} exit; then
    echo -e "${RED}✗ Cannot connect to server${NC}"
    echo "Check SSH connection: ssh ${SERVER}"
    exit 1
fi

echo -e "${GREEN}✓ Connected to server${NC}"
echo ""

# Get server git state
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}SERVER GIT STATE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
ssh ${SERVER} "cd ${SERVER_DIR} && git log -1 --oneline && echo 'Branch:' && git branch --show-current && echo 'Uncommitted:' && git status --short"
echo ""

# Get local git state for comparison
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}LOCAL GIT STATE${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
git log -1 --oneline
echo "Branch: $(git branch --show-current)"
echo "Uncommitted:"
git status --short
echo ""

# Compare commits
LOCAL_COMMIT=$(git rev-parse --short HEAD)
SERVER_COMMIT=$(ssh ${SERVER} "cd ${SERVER_DIR} && git rev-parse --short HEAD")

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}COMMIT COMPARISON${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo "Local:  ${LOCAL_COMMIT}"
echo "Server: ${SERVER_COMMIT}"

if [ "$LOCAL_COMMIT" == "$SERVER_COMMIT" ]; then
    echo -e "${GREEN}✓ Git commits match${NC}"
else
    echo -e "${RED}✗ Git commits DIFFER - This is likely the problem!${NC}"
    echo ""
    echo "Commits ahead on local:"
    git log --oneline ${SERVER_COMMIT}..${LOCAL_COMMIT} 2>/dev/null || echo "  (none or commits diverged)"
    echo ""
    echo "Commits ahead on server:"
    ssh ${SERVER} "cd ${SERVER_DIR} && git log --oneline ${LOCAL_COMMIT}..${SERVER_COMMIT}" 2>/dev/null || echo "  (none or commits diverged)"
fi
echo ""

# Check Docker images on server
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}SERVER DOCKER IMAGES${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
ssh ${SERVER} "cd ${SERVER_DIR} && docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}' | grep -E '(REPOSITORY|lego-sample-factory)' | head -10"
echo ""

# Check running containers on server
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}SERVER RUNNING CONTAINERS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
ssh ${SERVER} "cd ${SERVER_DIR} && docker compose ps"
echo ""

# Get local image IDs
LOCAL_FRONTEND_ID=$(docker images --format "{{.ID}}" lego-sample-factory-frontend:latest)
LOCAL_ORDER_ID=$(docker images --format "{{.ID}}" lego-sample-factory-order-processing-service:latest)

# Get registry image IDs from server
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}IMAGE ID COMPARISON${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

SERVER_FRONTEND_ID=$(ssh ${SERVER} "docker images --format '{{.ID}}' 192.168.1.237:5000/lego-sample-factory-frontend:latest" 2>/dev/null || echo "NOT FOUND")
SERVER_ORDER_ID=$(ssh ${SERVER} "docker images --format '{{.ID}}' 192.168.1.237:5000/lego-sample-factory-order-processing-service:latest" 2>/dev/null || echo "NOT FOUND")

echo "Frontend:"
echo "  Local:  ${LOCAL_FRONTEND_ID}"
echo "  Server: ${SERVER_FRONTEND_ID}"
if [ "$LOCAL_FRONTEND_ID" == "$SERVER_FRONTEND_ID" ]; then
    echo -e "  ${GREEN}✓ Images match${NC}"
else
    echo -e "  ${RED}✗ Images differ${NC}"
fi

echo ""
echo "Order Processing:"
echo "  Local:  ${LOCAL_ORDER_ID}"
echo "  Server: ${SERVER_ORDER_ID}"
if [ "$LOCAL_ORDER_ID" == "$SERVER_ORDER_ID" ]; then
    echo -e "  ${GREEN}✓ Images match${NC}"
else
    echo -e "  ${RED}✗ Images differ${NC}"
fi
echo ""

# Recommendations
echo "=================================================="
echo -e "${BLUE}DIAGNOSIS & RECOMMENDATIONS${NC}"
echo "=================================================="
echo ""

if [ "$LOCAL_COMMIT" != "$SERVER_COMMIT" ]; then
    echo -e "${RED}PROBLEM FOUND: Git commits differ${NC}"
    echo ""
    echo -e "${YELLOW}Fix Steps:${NC}"
    echo ""
    echo "1. Sync server to latest code:"
    echo "   ssh ${SERVER} 'cd ${SERVER_DIR} && git fetch origin'"
    echo "   ssh ${SERVER} 'cd ${SERVER_DIR} && git checkout dev && git pull origin dev'"
    echo ""
    echo "2. Pull latest images from registry:"
    echo "   ssh ${SERVER} 'cd ${SERVER_DIR} && ./update-from-registry.sh'"
    echo ""
    echo "3. Restart containers:"
    echo "   ssh ${SERVER} 'cd ${SERVER_DIR} && docker compose down && docker compose up -d --force-recreate'"
    echo ""
elif [ "$LOCAL_FRONTEND_ID" != "$SERVER_FRONTEND_ID" ] || [ "$LOCAL_ORDER_ID" != "$SERVER_ORDER_ID" ]; then
    echo -e "${YELLOW}Git matches, but images differ${NC}"
    echo ""
    echo "Server has old images. Fix:"
    echo "   ssh ${SERVER} 'cd ${SERVER_DIR} && ./update-from-registry.sh'"
    echo "   ssh ${SERVER} 'cd ${SERVER_DIR} && docker compose down && docker compose up -d --force-recreate'"
    echo ""
else
    echo -e "${GREEN}✓ Git commits match${NC}"
    echo -e "${GREEN}✓ Docker images match${NC}"
    echo ""
    echo "The code should be the same. If you still see differences:"
    echo "1. Clear browser cache (Ctrl+Shift+R)"
    echo "2. Test in incognito mode"
    echo "3. Check browser console for errors"
    echo ""
fi

echo "For detailed server diagnostics, run:"
echo "  ssh ${SERVER} 'cd ${SERVER_DIR} && ./diagnose-frontend-cache.sh'"
