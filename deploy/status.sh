#!/bin/bash
# =========================================================
# LIFE System - Service Status
# =========================================================
# Shows status and health of all services
# =========================================================

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "\n${CYAN}=========================================${NC}"
echo -e "${CYAN}LIFE System - Service Status${NC}"
echo -e "${CYAN}=========================================${NC}\n"

# Container status
echo -e "${CYAN}Container Status:${NC}"
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

echo ""

# Health check
echo -e "${CYAN}Health Status:${NC}"

SERVICES=(
    "lego-nginx-proxy"
    "lego-frontend"
    "lego-api-gateway"
    "lego-user-service"
    "lego-masterdata-service"
    "lego-inventory-service"
    "lego-order-processing-service"
    "lego-simal-integration-service"
)

for container in "${SERVICES[@]}"; do
    health=$(docker inspect --format='{{.State.Health.Status}}' "${container}" 2>/dev/null || echo "not found")
    
    case $health in
        healthy)
            echo -e "  ${GREEN}✓${NC} ${container}: ${GREEN}healthy${NC}"
            ;;
        unhealthy)
            echo -e "  ${RED}✗${NC} ${container}: ${RED}unhealthy${NC}"
            ;;
        starting)
            echo -e "  ${YELLOW}○${NC} ${container}: ${YELLOW}starting${NC}"
            ;;
        *)
            echo -e "  ${YELLOW}?${NC} ${container}: ${YELLOW}${health}${NC}"
            ;;
    esac
done

echo ""

# API Gateway health endpoint
echo -e "${CYAN}API Gateway Health:${NC}"
if curl -s --connect-timeout 5 http://localhost:8011/actuator/health > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓${NC} API Gateway responding"
else
    echo -e "  ${RED}✗${NC} API Gateway not responding"
fi

# Main endpoint
echo -e "\n${CYAN}Application URLs:${NC}"
echo -e "  Local:    http://localhost:1011"
echo -e "  Network:  http://$(hostname -I | awk '{print $1}'):1011"
echo ""
