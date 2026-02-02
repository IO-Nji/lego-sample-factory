#!/bin/bash
# =========================================================
# LIFE System - First-Time Setup for 192.168.1.237
# =========================================================
# Run this ONCE after cloning the prod branch
# =========================================================

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}=========================================${NC}"
echo -e "${CYAN}LIFE System - First-Time Setup${NC}"
echo -e "${CYAN}=========================================${NC}"
echo ""

# Check we're in the right directory
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}Error: docker-compose.yml not found${NC}"
    echo "Please run this script from the deploy directory"
    exit 1
fi

# Step 1: Create .env from example
echo -e "${CYAN}Step 1: Creating .env file${NC}"
if [ -f ".env" ]; then
    echo -e "${YELLOW}⚠ .env already exists${NC}"
    read -p "Overwrite with defaults? [y/N] " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cp .env.example .env
        echo -e "${GREEN}✓ .env overwritten${NC}"
    else
        echo "Keeping existing .env"
    fi
else
    cp .env.example .env
    echo -e "${GREEN}✓ .env created from template${NC}"
fi

# Step 2: Check Docker daemon config
echo ""
echo -e "${CYAN}Step 2: Checking Docker configuration${NC}"
if [ -f /etc/docker/daemon.json ]; then
    if grep -q "192.168.1.237:5000" /etc/docker/daemon.json; then
        echo -e "${GREEN}✓ Insecure registry already configured${NC}"
    else
        echo -e "${YELLOW}⚠ Registry not in daemon.json${NC}"
        echo "Please add to /etc/docker/daemon.json:"
        echo '  {"insecure-registries": ["192.168.1.237:5000"]}'
    fi
else
    echo -e "${YELLOW}⚠ No daemon.json found${NC}"
    echo "Creating /etc/docker/daemon.json..."
    echo '{"insecure-registries": ["192.168.1.237:5000"]}' | sudo tee /etc/docker/daemon.json > /dev/null
    echo "Restarting Docker..."
    sudo systemctl restart docker
    echo -e "${GREEN}✓ Docker configured for insecure registry${NC}"
fi

# Step 3: Test registry
echo ""
echo -e "${CYAN}Step 3: Testing registry connectivity${NC}"
if curl -s --connect-timeout 5 http://192.168.1.237:5000/v2/_catalog > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Registry is accessible${NC}"
else
    echo -e "${RED}✗ Cannot reach registry${NC}"
    echo "This might be because this IS the registry server (localhost)."
    echo "Trying localhost:5000..."
    if curl -s --connect-timeout 5 http://localhost:5000/v2/_catalog > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Registry accessible via localhost${NC}"
    else
        echo -e "${RED}✗ Registry not accessible${NC}"
        echo "Please ensure the Docker registry is running"
    fi
fi

# Step 4: Pull images
echo ""
echo -e "${CYAN}Step 4: Ready to pull images${NC}"
echo ""
echo -e "${GREEN}Setup complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Review .env file if needed: nano .env"
echo "  2. Pull images and start: ./update.sh"
echo ""
echo "Or run everything now with: ./update.sh --force"
