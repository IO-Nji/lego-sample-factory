#!/bin/bash
# =========================================================
# LIFE System - Push Docker Images to Local Registry
# =========================================================
# This script builds, tags, and pushes Docker images
# to the local registry at 192.168.1.237:5000
#
# Features:
# - Detects modified services (compares to origin/prod)
# - Only builds changed services by default
# - Continues on warnings (only fails on actual errors)
# - Better image ID detection with fallback methods
#
# Usage: 
#   ./push-to-registry.sh          # Build only modified services
#   ./push-to-registry.sh --all    # Build all services
#   ./push-to-registry.sh --help   # Show this help
# =========================================================

# Show help if requested
if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
    cat << 'EOF'
LIFE System - Docker Registry Push Script

This script builds and pushes Docker images to the local registry.

USAGE:
  ./push-to-registry.sh [OPTIONS]

OPTIONS:
  (none)    Build only services modified since origin/prod
  --all     Build all services regardless of changes
  --help    Show this help message

FEATURES:
  • Smart detection of modified services
  • Continues on build warnings
  • Multiple image ID detection methods
  • Detailed progress output
  • Version tagging with git commit

EXAMPLES:
  # Build only modified services
  ./push-to-registry.sh

  # Build all services
  ./push-to-registry.sh --all

REGISTRY: 192.168.1.237:5000

EOF
    exit 0
fi

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="192.168.1.237:5000"
PROJECT_NAME="lego-sample-factory"

# All available services
ALL_SERVICES=(
    "nginx-root-proxy"
    "frontend"
    "api-gateway"
    "user-service"
    "masterdata-service"
    "inventory-service"
    "order-processing-service"
    "simal-integration-service"
)

# Check if --all flag is provided
BUILD_ALL=false
if [[ "$1" == "--all" ]]; then
    BUILD_ALL=true
fi

# Check if --all flag is provided
BUILD_ALL=false
if [[ "$1" == "--all" ]]; then
    BUILD_ALL=true
fi

# Function to check if service was modified
service_modified() {
    local SERVICE=$1
    local SERVICE_PATH=""
    
    # Determine service path
    case "$SERVICE" in
        "nginx-root-proxy")
            SERVICE_PATH="nginx-root-proxy/"
            ;;
        "frontend")
            SERVICE_PATH="lego-factory-frontend/"
            ;;
        "api-gateway"|"user-service"|"masterdata-service"|"inventory-service"|"order-processing-service"|"simal-integration-service")
            SERVICE_PATH="lego-factory-backend/${SERVICE}/"
            ;;
        *)
            return 1
            ;;
    esac
    
    # Check if files in service path were modified (comparing to origin/prod)
    if git diff --quiet origin/prod HEAD -- "$SERVICE_PATH" 2>/dev/null; then
        return 1  # No changes
    else
        return 0  # Has changes
    fi
}

# Determine which services to build
SERVICES=()
if [ "$BUILD_ALL" = true ]; then
    echo -e "${YELLOW}Building ALL services (--all flag provided)${NC}"
    SERVICES=("${ALL_SERVICES[@]}")
else
    echo -e "${BLUE}Detecting modified services...${NC}"
    for SERVICE in "${ALL_SERVICES[@]}"; do
        if service_modified "$SERVICE"; then
            SERVICES+=("$SERVICE")
            echo -e "${GREEN}  ✓${NC} ${SERVICE} - modified"
        else
            echo -e "${BLUE}  ○${NC} ${SERVICE} - unchanged"
        fi
    done
    
    if [ ${#SERVICES[@]} -eq 0 ]; then
        echo -e "${YELLOW}No modified services detected.${NC}"
        echo -e "${YELLOW}Use --all flag to build all services anyway.${NC}"
        exit 0
    fi
fi

echo ""
echo -e "${GREEN}Services to build: ${#SERVICES[@]}${NC}"
for SERVICE in "${SERVICES[@]}"; do
    echo -e "  • ${SERVICE}"
done
echo ""
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
    
    # Build with docker-compose (allow warnings, only fail on actual errors)
    echo -e "${YELLOW}[1/5]${NC} Building ${SERVICE}..."
    BUILD_OUTPUT=$(docker-compose build --no-cache "${SERVICE}" 2>&1)
    BUILD_EXIT_CODE=$?
    
    # Show relevant build output
    echo "$BUILD_OUTPUT" | grep -E "(Building|Successfully built|Successfully tagged|ERROR|FAILED)" | tail -10
    
    # Check if build actually failed (not just warnings)
    if [ $BUILD_EXIT_CODE -ne 0 ]; then
        echo -e "${RED}✗ Build failed for ${SERVICE} (exit code: $BUILD_EXIT_CODE)${NC}"
        echo "$BUILD_OUTPUT" | grep -E "ERROR" | tail -5
        FAILED_PUSHES+=("${SERVICE}")
        return 1
    fi
    echo -e "${GREEN}✓ Build successful${NC}"
    
    # Get the image ID - try multiple methods
    echo -e "${YELLOW}[2/5]${NC} Getting image ID..."
    
    # Method 1: docker-compose images
    LOCAL_IMAGE=$(docker-compose images -q "${SERVICE}" 2>/dev/null | head -n1)
    
    # Method 2: If not found, try getting from docker images directly
    if [ -z "$LOCAL_IMAGE" ]; then
        LOCAL_IMAGE=$(docker images -q "${PROJECT_NAME}-${SERVICE}:latest" 2>/dev/null | head -n1)
    fi
    
    # Method 3: Try with lego-sample-factory prefix
    if [ -z "$LOCAL_IMAGE" ]; then
        LOCAL_IMAGE=$(docker images -q "lego-sample-factory-${SERVICE}:latest" 2>/dev/null | head -n1)
    fi
    
    if [ -z "$LOCAL_IMAGE" ]; then
        echo -e "${YELLOW}⚠ Could not find image ID automatically, searching...${NC}"
        # Show what images exist for this service
        docker images | grep -E "(${SERVICE}|${PROJECT_NAME})" | head -5
        echo -e "${RED}✗ Could not find built image for ${SERVICE}${NC}"
        echo -e "${YELLOW}Available images:${NC}"
        docker images --format "table {{.Repository}}:{{.Tag}}\t{{.ID}}" | grep -i "${SERVICE}" || echo "  (none found)"
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

echo ""
echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}Starting Build & Push Process${NC}"
echo -e "${BLUE}==========================================${NC}"

for SERVICE in "${SERVICES[@]}"; do
    CURRENT=$((CURRENT + 1))
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${BLUE}Service ${CURRENT}/${TOTAL_SERVICES}${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    
    # Continue even if one service fails
    if ! push_service "${SERVICE}"; then
        echo -e "${YELLOW}⚠ Skipping ${SERVICE}, continuing with next service...${NC}"
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
    echo -e "${YELLOW}Some services failed, but successful ones were pushed.${NC}"
    echo -e "${YELLOW}Check the output above for details.${NC}"
    
    # Don't exit with error if at least some services succeeded
    if [ ${#SUCCESSFUL_PUSHES[@]} -gt 0 ]; then
        echo -e "${GREEN}${#SUCCESSFUL_PUSHES[@]} service(s) pushed successfully.${NC}"
        exit 0
    else
        echo -e "${RED}All services failed!${NC}"
        exit 1
    fi
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
