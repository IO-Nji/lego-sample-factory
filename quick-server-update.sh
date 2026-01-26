#!/bin/bash

# Quick Server Update Script
# Updates server to latest dev commit and deploys

set -e

SERVER_USER="nji"
SERVER_HOST="io-surf"
SERVER_PATH="/home/nji/DEV/Java/LIFE/lego-sample-factory"

LOCAL_COMMIT=$(git rev-parse --short HEAD)
LOCAL_BRANCH=$(git branch --show-current)

echo "=================================================="
echo "  SERVER UPDATE & DEPLOYMENT"
echo "=================================================="
echo ""
echo "Local:  ${LOCAL_COMMIT} (${LOCAL_BRANCH})"
echo "Server: Checking..."
echo ""

# Get server current commit
SERVER_COMMIT=$(ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && git rev-parse --short HEAD" 2>/dev/null)
echo "Server: ${SERVER_COMMIT}"
echo ""

if [ "$LOCAL_COMMIT" == "$SERVER_COMMIT" ]; then
    echo "✓ Server is already up to date!"
    read -p "Pull registry images anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 0
    fi
else
    echo "Server is behind by these commits:"
    git log --oneline ${SERVER_COMMIT}..${LOCAL_COMMIT}
    echo ""
    
    read -p "Update server git repository to ${LOCAL_COMMIT}? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
    
    echo ""
    echo "Updating server repository..."
    ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && git fetch origin && git checkout ${LOCAL_BRANCH} && git pull origin ${LOCAL_BRANCH}"
    echo "✓ Git updated"
    echo ""
fi

echo "Pulling latest images from registry..."
ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && ./update-from-registry.sh" || {
    echo ""
    echo "⚠ update-from-registry.sh failed or doesn't exist"
    echo "Falling back to manual pull..."
    
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
    
    for service in "${SERVICES[@]}"; do
        echo "Pulling ${service}..."
        ssh ${SERVER_USER}@${SERVER_HOST} "docker pull 192.168.1.237:5000/lego-sample-factory-${service}:latest"
    done
}

echo ""
echo "Restarting services..."
ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && docker-compose down && docker-compose up -d --force-recreate"

echo ""
echo "Checking service status..."
ssh ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && docker-compose ps"

echo ""
echo "=================================================="
echo "  DEPLOYMENT COMPLETE"
echo "=================================================="
echo ""
echo "Next steps:"
echo "1. Wait ~30 seconds for services to fully start"
echo "2. Test scenario 2:"
echo "   - Login as modules_supermarket"
echo "   - Confirm warehouse order"
echo "   - Verify final assembly orders are created"
echo "3. Hard refresh browser (Ctrl+Shift+R)"
echo ""
echo "To check logs:"
echo "  ssh ${SERVER_USER}@${SERVER_HOST}"
echo "  cd ${SERVER_PATH}"
echo "  docker-compose logs -f order-processing-service"
