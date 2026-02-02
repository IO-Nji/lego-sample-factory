#!/bin/bash
# =========================================================
# LIFE System - Update from Registry
# =========================================================
# Pulls latest images from registry and restarts services
#
# Usage: ./update.sh [--force]
#   --force  Skip confirmation prompt
# =========================================================

set -e

# Configuration
REGISTRY="192.168.1.237:5000"
PROJECT="lego-sample-factory"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Services to update
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

print_header() {
    echo -e "\n${BOLD}${BLUE}=========================================${NC}"
    echo -e "${BOLD}${BLUE}$1${NC}"
    echo -e "${BOLD}${BLUE}=========================================${NC}\n"
}

print_step() {
    echo -e "${CYAN}▶${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if .env exists
check_env() {
    if [ ! -f ".env" ]; then
        print_error ".env file not found!"
        echo ""
        echo "Please create .env from the example:"
        echo "  cp .env.example .env"
        echo "  nano .env  # Edit as needed"
        exit 1
    fi
    print_success ".env file found"
}

# Check registry connectivity
check_registry() {
    print_step "Checking registry connectivity..."
    if curl -s --connect-timeout 5 "http://${REGISTRY}/v2/_catalog" > /dev/null 2>&1; then
        print_success "Registry ${REGISTRY} is accessible"
        return 0
    else
        print_error "Cannot reach registry at ${REGISTRY}"
        echo ""
        echo "Please ensure:"
        echo "  1. The registry server is running"
        echo "  2. Network connectivity exists"
        echo "  3. Docker is configured for insecure registry"
        exit 1
    fi
}

# Pull all images
pull_images() {
    print_header "Pulling Images from Registry"
    
    local failed=0
    for service in "${SERVICES[@]}"; do
        local image="${REGISTRY}/${PROJECT}-${service}:latest"
        print_step "Pulling ${service}..."
        
        if docker pull "${image}" > /dev/null 2>&1; then
            print_success "${service} updated"
        else
            print_error "Failed to pull ${service}"
            ((failed++))
        fi
    done
    
    if [ $failed -gt 0 ]; then
        print_warning "${failed} service(s) failed to pull"
        return 1
    fi
    
    print_success "All images pulled successfully"
    return 0
}

# Restart services
restart_services() {
    print_header "Restarting Services"
    
    print_step "Stopping current services..."
    docker compose down --remove-orphans 2>/dev/null || true
    
    print_step "Starting services with new images..."
    docker compose up -d
    
    print_success "Services started"
}

# Show status
show_status() {
    print_header "Service Status"
    
    echo ""
    docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
    echo ""
    
    # Wait for health checks
    print_step "Waiting for services to be healthy (30s)..."
    sleep 30
    
    # Check health
    local unhealthy=0
    for service in "${SERVICES[@]}"; do
        local container="lego-${service//-service/}"
        container="${container/nginx-root-proxy/nginx-proxy}"
        
        local health=$(docker inspect --format='{{.State.Health.Status}}' "${container}" 2>/dev/null || echo "unknown")
        
        case $health in
            healthy)
                print_success "${service}: healthy"
                ;;
            unhealthy)
                print_error "${service}: unhealthy"
                ((unhealthy++))
                ;;
            *)
                print_warning "${service}: ${health}"
                ;;
        esac
    done
    
    echo ""
    if [ $unhealthy -eq 0 ]; then
        print_success "All services are healthy!"
        echo ""
        echo -e "${GREEN}Application is ready at:${NC}"
        echo -e "  Local:    http://localhost:1011"
        echo -e "  Network:  http://$(hostname -I | awk '{print $1}'):1011"
    else
        print_warning "${unhealthy} service(s) are unhealthy"
        echo "Check logs with: docker compose logs <service-name>"
    fi
}

# Main
main() {
    print_header "LIFE System - Update from Registry"
    
    echo -e "${CYAN}Registry:${NC} ${REGISTRY}"
    echo -e "${CYAN}Services:${NC} ${#SERVICES[@]}"
    echo ""
    
    # Confirmation
    if [ "$1" != "--force" ]; then
        read -p "This will pull new images and restart all services. Continue? [y/N] " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Aborted."
            exit 0
        fi
    fi
    
    check_env
    check_registry
    pull_images
    restart_services
    show_status
    
    echo ""
    print_success "Update complete!"
}

main "$@"
