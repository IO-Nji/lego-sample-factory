#!/bin/bash

# Frontend Cache Diagnostic Script
# Verifies that live server has latest frontend images and bundles

set -e

echo "=================================================="
echo "  FRONTEND CACHE DIAGNOSTIC TOOL"
echo "=================================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Expected values (update these when you rebuild)
EXPECTED_IMAGE_ID="31263030a301"
EXPECTED_ASSET_DATE="Jan 26 03:57"
REGISTRY="192.168.1.237:5000"

echo "Expected Image ID: ${EXPECTED_IMAGE_ID}"
echo "Expected Asset Date: ${EXPECTED_ASSET_DATE}"
echo ""

# Check 1: Docker image on server
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. Checking local Docker images..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
CURRENT_IMAGE_ID=$(docker images --format "{{.ID}}" ${REGISTRY}/lego-sample-factory-frontend:latest 2>/dev/null | head -1)
if [ -z "$CURRENT_IMAGE_ID" ]; then
    echo -e "${RED}✗ Frontend image not found locally!${NC}"
    echo "  Run: ./update-from-registry.sh"
    exit 1
fi

echo "Current Image ID: ${CURRENT_IMAGE_ID}"
if [ "$CURRENT_IMAGE_ID" == "$EXPECTED_IMAGE_ID" ]; then
    echo -e "${GREEN}✓ Image ID matches expected version${NC}"
else
    echo -e "${YELLOW}⚠ Image ID mismatch!${NC}"
    echo "  Expected: ${EXPECTED_IMAGE_ID}"
    echo "  Current:  ${CURRENT_IMAGE_ID}"
    echo "  Action: Run ./update-from-registry.sh to pull latest"
fi
echo ""

# Check 2: Registry has latest
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. Checking registry for available versions..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
REGISTRY_TAGS=$(curl -s http://${REGISTRY}/v2/lego-sample-factory-frontend/tags/list 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "Registry tags:"
    echo "$REGISTRY_TAGS" | jq -r '.tags[]' | sort -r | head -5
    echo -e "${GREEN}✓ Registry is accessible${NC}"
else
    echo -e "${RED}✗ Cannot connect to registry at ${REGISTRY}${NC}"
fi
echo ""

# Check 3: Running container
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. Checking running containers..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
NGINX_RUNNING=$(docker ps --format "{{.Names}}" | grep nginx-root-proxy || echo "")
if [ -z "$NGINX_RUNNING" ]; then
    echo -e "${RED}✗ nginx-root-proxy container not running!${NC}"
    echo "  Run: docker compose up -d"
    exit 1
fi

echo -e "${GREEN}✓ nginx-root-proxy is running${NC}"

FRONTEND_RUNNING=$(docker ps --format "{{.Names}}" | grep frontend || echo "")
if [ -n "$FRONTEND_RUNNING" ]; then
    echo -e "${GREEN}✓ frontend container is running${NC}"
else
    echo -e "${YELLOW}⚠ frontend container not running (nginx may be serving static files)${NC}"
fi
echo ""

# Check 4: Assets inside container
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. Checking asset files inside nginx container..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
ASSETS=$(docker exec nginx-root-proxy ls -lh /usr/share/nginx/html/assets/ 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$ASSETS"
    
    # Check if asset dates match expected
    if echo "$ASSETS" | grep -q "$EXPECTED_ASSET_DATE"; then
        echo -e "${GREEN}✓ Asset dates match expected build time${NC}"
    else
        echo -e "${YELLOW}⚠ Asset dates don't match expected build time${NC}"
        echo "  Expected date pattern: ${EXPECTED_ASSET_DATE}"
        echo "  Action: Restart containers with --force-recreate"
    fi
else
    echo -e "${RED}✗ Cannot access assets inside nginx-root-proxy${NC}"
fi
echo ""

# Check 5: Container image version
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. Checking which image nginx container is using..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
CONTAINER_IMAGE=$(docker inspect nginx-root-proxy --format='{{.Image}}' 2>/dev/null)
if [ -n "$CONTAINER_IMAGE" ]; then
    echo "Container Image SHA: ${CONTAINER_IMAGE}"
    
    # Get image ID from SHA
    RUNNING_IMAGE_ID=$(docker images --format "{{.ID}} {{.Digest}}" | grep "${CONTAINER_IMAGE:7:12}" | awk '{print $1}')
    echo "Running Image ID: ${RUNNING_IMAGE_ID}"
    
    if [ "$RUNNING_IMAGE_ID" == "$EXPECTED_IMAGE_ID" ]; then
        echo -e "${GREEN}✓ Container is running latest image${NC}"
    else
        echo -e "${YELLOW}⚠ Container is running old image!${NC}"
        echo "  Action: docker compose down && docker compose up -d --force-recreate"
    fi
else
    echo -e "${RED}✗ Cannot inspect nginx-root-proxy container${NC}"
fi
echo ""

# Summary and recommendations
echo "=================================================="
echo "  SUMMARY & RECOMMENDATIONS"
echo "=================================================="
echo ""

# Decision tree for fixes
if [ "$CURRENT_IMAGE_ID" != "$EXPECTED_IMAGE_ID" ]; then
    echo -e "${YELLOW}ACTION REQUIRED:${NC}"
    echo "1. Pull latest images from registry:"
    echo "   ./update-from-registry.sh"
    echo ""
elif [ -n "$RUNNING_IMAGE_ID" ] && [ "$RUNNING_IMAGE_ID" != "$EXPECTED_IMAGE_ID" ]; then
    echo -e "${YELLOW}ACTION REQUIRED:${NC}"
    echo "1. Restart containers with latest image:"
    echo "   docker compose down"
    echo "   docker compose up -d --force-recreate"
    echo ""
else
    echo -e "${GREEN}✓ Server has correct images and containers${NC}"
    echo ""
    echo -e "${YELLOW}If you still see old frontend behavior:${NC}"
    echo "1. Clear browser cache:"
    echo "   - Chrome/Edge: Ctrl+Shift+R (Windows) or Cmd+Shift+R (Mac)"
    echo "   - Or open DevTools → Right-click refresh → Empty Cache and Hard Reload"
    echo ""
    echo "2. Test in incognito/private browsing mode"
    echo ""
    echo "3. Check browser DevTools Network tab:"
    echo "   - Look for JS/CSS files with hash names (e.g., index-k5PY5sRo.js)"
    echo "   - Verify Status is 200 (not 304 Not Modified)"
    echo "   - Check if files match: Jan 26 03:57 timestamp"
    echo ""
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Expected Asset Hashes (Jan 26 04:57 build):"
echo "  index-BHVMtFsv.css (166.7K)"
echo "  index-k5PY5sRo.js  (308.3K)"
echo "  router-X4-hMGP6.js (21.6K)"
echo "  vendor-CRB3T2We.js (138.4K)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "For detailed troubleshooting, see: FRONTEND_CACHE_FIX.md"
