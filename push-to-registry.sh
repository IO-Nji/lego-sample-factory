#!/bin/bash
# =========================================================
# LIFE System - Push Docker Images to Local Registry
# =========================================================
# This script builds, tags, and pushes all Docker images
# to the local registry at 192.168.1.237:5000
#
# Usage: ./push-to-registry.sh
# =========================================================

set -e  # Exit on error

# Configuration
REGISTRY="192.168.1.237:5000"
PROJECT_NAME="lego-sample-factory"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Services to build and push
SERVICES=(
    "nginx-root-proxy"
    "frontend"
    "api-gateway"
    "user-service"
    "masterdata-service"
    "inventory-service"
    "order-processing-service"
    "simal-integration-service"
)

# Get version from git
GIT_COMMIT=$(git rev-parse --short HEAD)
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
VERSION_TAG="${GIT_BRANCH}-${GIT_COMMIT}-$(date +%Y%m%d-%H%M%S)"

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}LIFE System - Docker Registry Push${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
echo -e "${GREEN}Registry:${NC} ${REGISTRY}"
echo -e "${GREEN}Git Branch:${NC} ${GIT_BRANCH}"
echo -e "${GREEN}Git Commit:${NC} ${GIT_COMMIT}"
echo -e "${GREEN}Version Tag:${NC} ${VERSION_TAG}"
echo -e "${GREEN}Services:${NC} ${#SERVICES[@]}"
echo ""

# Confirmation prompt
read -p "Continue with build and push? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Aborted by user.${NC}"
    exit 0
fi

# Arrays to track results
declare -a SUCCESSFUL_PUSHES
declare -a FAILED_PUSHES

# Function to build and push a service
push_service() {
    local SERVICE=$1
    local IMAGE_NAME="${REGISTRY}/${PROJECT_NAME}-${SERVICE}"
    local IMAGE_LATEST="${IMAGE_NAME}:latest"
    local IMAGE_VERSION="${IMAGE_NAME}:${VERSION_TAG}"
    
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Processing: ${SERVICE}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    # Build with docker-compose
    echo -e "${YELLOW}[1/5]${NC} Building ${SERVICE}..."
    if ! docker-compose build --no-cache "${SERVICE}" 2>&1 | grep -E "(Building|Successfully built|ERROR)"; then
        echo -e "${RED}✗ Build failed for ${SERVICE}${NC}"
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
    echo -e "${GREEN}✓ Build successful${NC}"
    
    # Get the image ID from docker-compose
    echo -e "${YELLOW}[2/5]${NC} Getting image ID..."
    LOCAL_IMAGE=$(docker-compose images -q "${SERVICE}" 2>/dev/null | head -n1)
    if [ -z "$LOCAL_IMAGE" ]; then
        echo -e "${RED}✗ Could not find built image for ${SERVICE}${NC}"
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
    echo -e "${GREEN}✓ Image ID: ${LOCAL_IMAGE}${NC}"
    
    # Tag with latest
    echo -e "${YELLOW}[3/5]${NC} Tagging as latest..."
    if ! docker tag "${LOCAL_IMAGE}" "${IMAGE_LATEST}"; then
        echo -e "${RED}✗ Failed to tag ${SERVICE}:latest${NC}"
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
    echo -e "${GREEN}✓ Tagged: ${IMAGE_LATEST}${NC}"
    
    # Tag with version
    echo -e "${YELLOW}[4/5]${NC} Tagging with version..."
    if ! docker tag "${LOCAL_IMAGE}" "${IMAGE_VERSION}"; then
        echo -e "${RED}✗ Failed to tag ${SERVICE}:${VERSION_TAG}${NC}"
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
    echo -e "${GREEN}✓ Tagged: ${IMAGE_VERSION}${NC}"
    
    # Push latest tag
    echo -e "${YELLOW}[5/5]${NC} Pushing to registry..."
    if ! docker push "${IMAGE_LATEST}" 2>&1 | tail -5; then
        echo -e "${RED}✗ Failed to push ${SERVICE}:latest${NC}"
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
    echo -e "${GREEN}✓ Pushed: ${IMAGE_LATEST}${NC}"
    
    # Push version tag
    if ! docker push "${IMAGE_VERSION}" 2>&1 | tail -5; then
        echo -e "${RED}✗ Failed to push ${SERVICE}:${VERSION_TAG}${NC}"
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
    echo -e "${GREEN}✓ Pushed: ${IMAGE_VERSION}${NC}"
    
    # Verify the push
    echo -e "${YELLOW}Verifying...${NC}"
    if docker pull "${IMAGE_LATEST}" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Verification successful - image is accessible in registry${NC}"
        SUCCESSFUL_PUSHES+=("${SERVICE}")
        return 0
    else
        echo -e "${RED}✗ Verification failed - could not pull from registry${NC}"
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
}

# Process all services
TOTAL_SERVICES=${#SERVICES[@]}
CURRENT=0

for SERVICE in "${SERVICES[@]}"; do
    CURRENT=$((CURRENT + 1))
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${BLUE}Service ${CURRENT}/${TOTAL_SERVICES}${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    
    if ! push_service "${SERVICE}"; then
        echo -e "${RED}Failed to process ${SERVICE}${NC}"
    fi
done

# Summary
echo ""
echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}SUMMARY${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""
echo -e "${GREEN}Successful: ${#SUCCESSFUL_PUSHES[@]}/${TOTAL_SERVICES}${NC}"
for SERVICE in "${SUCCESSFUL_PUSHES[@]}"; do
    echo -e "  ${GREEN}✓${NC} ${SERVICE}"
done

if [ ${#FAILED_PUSHES[@]} -gt 0 ]; then
    echo ""
    echo -e "${RED}Failed: ${#FAILED_PUSHES[@]}/${TOTAL_SERVICES}${NC}"
    for SERVICE in "${FAILED_PUSHES[@]}"; do
        echo -e "  ${RED}✗${NC} ${SERVICE}"
    done
    echo ""
    echo -e "${RED}Some services failed. Please check the output above.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}All images pushed successfully!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo -e "${YELLOW}Version Tag:${NC} ${VERSION_TAG}"
echo -e "${YELLOW}Registry:${NC} ${REGISTRY}"
echo ""
echo -e "${BLUE}Next steps on live server:${NC}"
echo "  1. SSH to live server: ssh nji@io-surf"
echo "  2. Navigate to project: cd ~/lego-sample-factory"
echo "  3. Update docker-compose.yml to use registry images"
echo "  4. Pull images: docker-compose pull"
echo "  5. Restart services: docker-compose up -d"
echo ""
echo -e "${GREEN}Done!${NC}"
