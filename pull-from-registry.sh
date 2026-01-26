#!/bin/bash
# =========================================================
# LIFE System - Pull and Update from Local Registry
# =========================================================
# Run this script on the live server to pull latest images
# from the local registry and restart services
#
# Usage: ./pull-from-registry.sh
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

# Services to pull
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

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}LIFE System - Pull from Registry${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
echo -e "${GREEN}Registry:${NC} ${REGISTRY}"
echo -e "${GREEN}Services:${NC} ${#SERVICES[@]}"
echo ""

# Check if docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}✗ Docker is not running${NC}"
    exit 1
fi

# Arrays to track results
declare -a SUCCESSFUL_PULLS
declare -a FAILED_PULLS

# Function to pull a service image
pull_service() {
    local SERVICE=$1
    local IMAGE_NAME="${REGISTRY}/${PROJECT_NAME}-${SERVICE}:latest"
    
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Pulling: ${SERVICE}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    if docker pull "${IMAGE_NAME}" 2>&1 | grep -E "(Pulling|Downloaded|Status|Digest)"; then
        echo -e "${GREEN}✓ Successfully pulled ${SERVICE}${NC}"
        SUCCESSFUL_PULLS+=("${SERVICE}")
        return 0
    else
        echo -e "${RED}✗ Failed to pull ${SERVICE}${NC}"
        FAILED_PULLS+=("${SERVICE}")
        return 1
    fi
}

# Pull all services
TOTAL_SERVICES=${#SERVICES[@]}
CURRENT=0

for SERVICE in "${SERVICES[@]}"; do
    CURRENT=$((CURRENT + 1))
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${BLUE}Service ${CURRENT}/${TOTAL_SERVICES}${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    
    pull_service "${SERVICE}"
done

# Summary
echo ""
echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}PULL SUMMARY${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""
echo -e "${GREEN}Successful: ${#SUCCESSFUL_PULLS[@]}/${TOTAL_SERVICES}${NC}"
for SERVICE in "${SUCCESSFUL_PULLS[@]}"; do
    echo -e "  ${GREEN}✓${NC} ${SERVICE}"
done

if [ ${#FAILED_PULLS[@]} -gt 0 ]; then
    echo ""
    echo -e "${RED}Failed: ${#FAILED_PULLS[@]}/${TOTAL_SERVICES}${NC}"
    for SERVICE in "${FAILED_PULLS[@]}"; do
        echo -e "  ${RED}✗${NC} ${SERVICE}"
    done
    echo ""
    echo -e "${YELLOW}Warning: Some images failed to pull.${NC}"
    echo -e "${YELLOW}Continuing with restart anyway...${NC}"
fi

# Ask before restarting
echo ""
read -p "Restart all services with new images? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Skipped restart. Images are pulled but services not restarted.${NC}"
    exit 0
fi

# Stop all services
echo ""
echo -e "${YELLOW}Stopping all services...${NC}"
docker-compose down

# Start all services
echo ""
echo -e "${YELLOW}Starting all services with new images...${NC}"
docker-compose up -d

# Wait for services to start
echo ""
echo -e "${YELLOW}Waiting for services to start (15 seconds)...${NC}"
sleep 15

# Check service status
echo ""
echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}SERVICE STATUS${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""
docker-compose ps

# Show logs of failed services
FAILED_SERVICES=$(docker-compose ps --filter "status=exited" --format "{{.Service}}" 2>/dev/null || true)
if [ -n "$FAILED_SERVICES" ]; then
    echo ""
    echo -e "${RED}Some services failed to start:${NC}"
    echo "$FAILED_SERVICES"
    echo ""
    echo -e "${YELLOW}Showing logs of failed services:${NC}"
    for SERVICE in $FAILED_SERVICES; do
        echo ""
        echo -e "${RED}Logs for ${SERVICE}:${NC}"
        docker-compose logs --tail=20 "$SERVICE"
    done
else
    echo ""
    echo -e "${GREEN}==========================================${NC}"
    echo -e "${GREEN}All services started successfully!${NC}"
    echo -e "${GREEN}==========================================${NC}"
fi

echo ""
echo -e "${BLUE}To monitor logs, use:${NC}"
echo "  docker-compose logs -f [service-name]"
echo ""
echo -e "${BLUE}To check specific service:${NC}"
echo "  docker-compose logs -f order-processing-service"
echo ""
echo -e "${GREEN}Done!${NC}"
