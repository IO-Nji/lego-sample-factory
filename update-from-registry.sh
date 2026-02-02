#!/bin/bash
# =========================================================
# LIFE System - Quick Update from Registry
# =========================================================
# Simple script to pull latest images and restart services
# Run this on your server for quick updates
#
# Usage: ./update-from-registry.sh
# =========================================================

set +e  # Don't exit on error - we want to handle errors gracefully

# Configuration
REGISTRY="192.168.1.237:5000"
PROJECT="lego-sample-factory"
PULL_TIMEOUT=300  # 5 minutes timeout per service

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Logging arrays
declare -a PULL_SUCCESS_LOG
declare -a PULL_FAIL_LOG
declare -a PULL_SKIP_LOG

# Services
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

# Helper functions
print_header() {
    echo -e "${BOLD}${BLUE}=========================================${NC}"
    echo -e "${BOLD}${BLUE}$1${NC}"
    echo -e "${BOLD}${BLUE}=========================================${NC}"
}

print_step() {
    echo -e "\n${CYAN}▶ $1${NC}"
}

print_substep() {
    echo -e "  ${MAGENTA}→${NC} $1"
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

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Progress indicator for long operations
show_progress() {
    local pid=$1
    local message=$2
    local delay=0.5
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    while kill -0 $pid 2>/dev/null; do
        local temp=${spinstr#?}
        printf "\r  ${CYAN}%c${NC} %s" "$spinstr" "$message"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
    done
    printf "\r"
}

echo ""
print_header "LIFE System - Quick Update"
echo ""
echo -e "${GREEN}Registry:${NC} ${REGISTRY}"
echo -e "${GREEN}Services:${NC} ${#SERVICES[@]}"
echo -e "${GREEN}Timeout per pull:${NC} ${PULL_TIMEOUT}s (5 minutes)"
echo ""

# Check Docker is running
print_step "Pre-flight checks"
print_substep "Checking Docker daemon..."
if ! docker info >/dev/null 2>&1; then
    print_error "Docker is not running"
    exit 1
fi
print_success "Docker is running"

# Check registry is accessible
print_substep "Testing registry connectivity..."
if ! curl -s --connect-timeout 5 http://${REGISTRY}/v2/_catalog >/dev/null 2>&1; then
    print_error "Registry at ${REGISTRY} is not accessible"
    print_warning "Make sure the registry server is running and accessible"
    exit 1
fi
print_success "Registry is accessible"

# Check docker-compose.yml exists
print_substep "Checking docker-compose.yml..."
if [ ! -f "docker-compose.yml" ]; then
    print_error "docker-compose.yml not found in current directory"
    print_info "Current directory: $(pwd)"
    exit 1
fi
print_success "docker-compose.yml found"
echo ""

# Pull all images
print_header "Pulling Images from Registry"
echo ""

PULL_SUCCESS=0
PULL_FAILED=0
PULL_SKIPPED=0
TOTAL_SERVICES=${#SERVICES[@]}

for i in "${!SERVICES[@]}"; do
    SERVICE="${SERVICES[$i]}"
    CURRENT=$((i + 1))
    IMAGE="${REGISTRY}/${PROJECT}-${SERVICE}:latest"
    
    echo -e "${BOLD}${CYAN}[${CURRENT}/${TOTAL_SERVICES}]${NC} ${BOLD}${SERVICE}${NC}"
    print_substep "Image: ${IMAGE}"
    
    # Create a temp file for pull output
    TEMP_LOG=$(mktemp)
    
    # Pull with timeout in background
    print_substep "Pulling (timeout: ${PULL_TIMEOUT}s)..."
    (
        docker pull "${IMAGE}" > "${TEMP_LOG}" 2>&1
        echo $? > "${TEMP_LOG}.exit"
    ) &
    PULL_PID=$!
    
    # Show progress spinner
    ELAPSED=0
    SPIN_CHARS="⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"
    while kill -0 $PULL_PID 2>/dev/null; do
        if [ $ELAPSED -ge $PULL_TIMEOUT ]; then
            kill -9 $PULL_PID 2>/dev/null
            wait $PULL_PID 2>/dev/null
            print_error "TIMEOUT after ${PULL_TIMEOUT}s"
            PULL_FAIL_LOG+=("${SERVICE} (timeout)")
            ((PULL_FAILED++))
            rm -f "${TEMP_LOG}" "${TEMP_LOG}.exit"
            echo ""
            continue 2
        fi
        
        printf "\r  ${CYAN}%c${NC} Pulling... [%ds / %ds]" \
            "${SPIN_CHARS:$((ELAPSED % 10)):1}" \
            "$ELAPSED" \
            "$PULL_TIMEOUT"
        sleep 1
        ((ELAPSED++))
    done
    printf "\r%60s\r" " "  # Clear progress line
    
    # Wait for background process
    wait $PULL_PID 2>/dev/null
    EXIT_CODE=$(cat "${TEMP_LOG}.exit" 2>/dev/null || echo "1")
    
    # Check result
    if [ "$EXIT_CODE" -eq 0 ]; then
        print_success "Pull successful (${ELAPSED}s)"
        PULL_SUCCESS_LOG+=("${SERVICE}")
        ((PULL_SUCCESS++))
    else
        print_error "Pull FAILED"
        
        # Show last few lines of error
        if [ -f "${TEMP_LOG}" ]; then
            echo -e "${YELLOW}  Last error lines:${NC}"
            tail -3 "${TEMP_LOG}" | sed 's/^/    /'
        fi
        
        PULL_FAIL_LOG+=("${SERVICE}")
        ((PULL_FAILED++))
    fi
    
    # Cleanup temp files
    rm -f "${TEMP_LOG}" "${TEMP_LOG}.exit"
    echo ""
done

# Pull summary
echo ""
print_header "Pull Summary"
echo ""
echo -e "${BOLD}Total Services:${NC} ${TOTAL_SERVICES}"
echo -e "${GREEN}${BOLD}Successful:${NC} ${PULL_SUCCESS}"
echo -e "${RED}${BOLD}Failed:${NC} ${PULL_FAILED}"
echo ""

if [ ${PULL_SUCCESS} -gt 0 ]; then
    echo -e "${GREEN}${BOLD}✓ Successfully pulled:${NC}"
    for SERVICE in "${PULL_SUCCESS_LOG[@]}"; do
        echo -e "  ${GREEN}✓${NC} ${SERVICE}"
    done
    echo ""
fi

if [ ${PULL_FAILED} -gt 0 ]; then
    echo -e "${RED}${BOLD}✗ Failed to pull:${NC}"
    for SERVICE in "${PULL_FAIL_LOG[@]}"; do
        echo -e "  ${RED}✗${NC} ${SERVICE}"
    done
    echo ""
    print_warning "Failed images will use existing local versions (if available)"
    echo ""
fi

# Decision point
if [ ${PULL_SUCCESS} -eq 0 ]; then
    print_error "No images were successfully pulled. Cannot proceed."
    print_info "Check registry connectivity and try again"
    exit 1
fi

if [ ${PULL_FAILED} -gt 0 ]; then
    print_warning "Some pulls failed, but ${PULL_SUCCESS} succeeded"
    read -p "Continue with restart using available images? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Aborted by user"
        exit 0
    fi
    echo ""
fi

# Restart services
print_header "Restarting Services"
echo ""

print_step "Stopping all services..."
if docker compose down 2>&1 | grep -E "(Stopping|Stopped|Removing|Removed|done)"; then
    print_success "Services stopped"
else
    print_warning "Some services may not have stopped cleanly"
fi

echo ""
print_step "Starting services with updated images..."
if docker compose up -d 2>&1 | grep -E "(Creating|Created|Starting|Started|done)"; then
    print_success "Services started"
else
    print_warning "Some services may not have started properly"
fi

# Wait for services to stabilize
echo ""
print_step "Waiting for services to stabilize..."
for i in {10..1}; do
    printf "\r  ${CYAN}⏱${NC}  Waiting... ${i}s "
    sleep 1
done
printf "\r%40s\r" " "
print_success "Wait complete"
echo ""

# Check service status
print_header "Service Status Check"
echo ""

# Get service status
echo -e "${BOLD}Container Status:${NC}"
docker compose ps
echo ""

# Analyze running vs failed
RUNNING=$(docker compose ps --filter "status=running" --format "{{.Service}}" 2>/dev/null | wc -l)
EXITED=$(docker compose ps --filter "status=exited" --format "{{.Service}}" 2>/dev/null | wc -l)
TOTAL_EXPECTED=${#SERVICES[@]}

echo -e "${BOLD}Status Summary:${NC}"
echo -e "  ${GREEN}Running:${NC} ${RUNNING}/${TOTAL_EXPECTED}"
echo -e "  ${RED}Failed:${NC} ${EXITED}"
echo ""

# Check for failed services
FAILED_SERVICES=$(docker compose ps --filter "status=exited" --format "{{.Service}}" 2>/dev/null)
if [ -n "$FAILED_SERVICES" ]; then
    print_error "Some services failed to start:"
    echo ""
    for SERVICE in $FAILED_SERVICES; do
        echo -e "${RED}  ✗ ${SERVICE}${NC}"
        echo -e "${YELLOW}    Showing last 10 log lines:${NC}"
        docker compose logs --tail=10 "$SERVICE" 2>/dev/null | sed 's/^/      /'
        echo ""
    done
    
    echo -e "${YELLOW}${BOLD}Troubleshooting Commands:${NC}"
    echo "  docker compose logs -f <service>     # Watch logs"
    echo "  docker compose restart <service>      # Restart specific service"
    echo "  docker compose up -d <service>        # Recreate service"
    echo ""
fi

# Final summary
print_header "Update Complete"
echo ""

if [ $EXITED -eq 0 ]; then
    print_success "All ${RUNNING} services are running successfully!"
else
    print_warning "${RUNNING} services running, ${EXITED} failed"
    print_info "Check logs above for failed services"
fi

echo ""
echo -e "${BOLD}Image Pull Results:${NC}"
echo -e "  ${GREEN}✓ Successful: ${PULL_SUCCESS}${NC}"
if [ ${PULL_FAILED} -gt 0 ]; then
    echo -e "  ${RED}✗ Failed: ${PULL_FAILED}${NC}"
fi
echo ""

echo -e "${BOLD}${BLUE}Quick Commands:${NC}"
echo "  docker compose logs -f                           # Watch all logs"
echo "  docker compose logs -f order-processing-service  # Watch specific service"
echo "  docker compose ps                                # Check status"
echo "  docker compose restart <service>                 # Restart a service"
echo ""

if [ $EXITED -gt 0 ] || [ ${PULL_FAILED} -gt 0 ]; then
    exit 1
else
    print_success "Update completed successfully!"
    exit 0
fi
