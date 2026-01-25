#!/bin/bash
# Deploy ProductVariant → Product refactoring to live server (lego.nji.io)
# Run this script ON THE LIVE SERVER after SSH-ing in

set -e  # Exit on any error

echo "=========================================="
echo "LIFE System - Live Server Deployment"
echo "ProductVariant → Product Migration"
echo "=========================================="
echo ""

# Check if running on live server
if [[ ! -f "/home/nji/lego-sample-factory/.env" ]]; then
    echo "❌ ERROR: This script must be run ON the live server (lego.nji.io)"
    echo "   Current directory doesn't contain expected .env file"
    exit 1
fi

# Navigate to project directory
cd /home/nji/lego-sample-factory || exit 1

echo "📍 Current directory: $(pwd)"
echo ""

# Show current Git status
echo "1️⃣  Checking current Git status..."
git status
echo ""

# Fetch latest changes
echo "2️⃣  Fetching latest changes from remote..."
git fetch origin
echo ""

# Show current branch and what's available
echo "3️⃣  Current branch: $(git branch --show-current)"
echo "   Latest prod commit on remote: $(git log origin/prod --oneline -1)"
echo ""

# Stash any local changes
echo "4️⃣  Stashing any local changes..."
git stash
echo ""

# Checkout prod branch
echo "5️⃣  Switching to prod branch..."
git checkout prod
echo ""

# Pull latest changes
echo "6️⃣  Pulling latest changes..."
git pull origin prod
echo ""

# Verify we have the critical commit
EXPECTED_COMMIT="75ddfb8"
CURRENT_COMMIT=$(git rev-parse --short HEAD)
echo "7️⃣  Verifying deployment commit..."
echo "   Expected: $EXPECTED_COMMIT (ProductVariant → Product refactoring)"
echo "   Current:  $CURRENT_COMMIT"

if git log --oneline -10 | grep -q "$EXPECTED_COMMIT"; then
    echo "   ✅ Critical commit found in history"
else
    echo "   ⚠️  WARNING: Expected commit not found, but continuing..."
fi
echo ""

# Stop all running containers
echo "8️⃣  Stopping all running containers..."
docker-compose down
echo ""

# Remove old images to force rebuild
echo "9️⃣  Removing old masterdata-service image..."
docker rmi lego-sample-factory-masterdata-service:latest || echo "   (Image not found, skipping)"
echo ""

# Rebuild masterdata-service with no cache
echo "🔟 Rebuilding masterdata-service (this may take 2-3 minutes)..."
docker-compose build --no-cache masterdata-service
echo ""

# Rebuild api-gateway (routing changes)
echo "1️⃣1️⃣  Rebuilding api-gateway..."
docker-compose build --no-cache api-gateway
echo ""

# Rebuild frontend (endpoint changes)
echo "1️⃣2️⃣  Rebuilding frontend..."
docker-compose build --no-cache lego-factory-frontend
echo ""

# Rebuild order-processing-service (endpoint changes)
echo "1️⃣3️⃣  Rebuilding order-processing-service..."
docker-compose build --no-cache order-processing-service
echo ""

# Start all services
echo "1️⃣4️⃣  Starting all services..."
docker-compose up -d
echo ""

# Wait for services to stabilize
echo "1️⃣5️⃣  Waiting 15 seconds for services to start..."
sleep 15
echo ""

# Check service health
echo "1️⃣6️⃣  Checking service health..."
docker-compose ps
echo ""

# Test masterdata endpoint
echo "1️⃣7️⃣  Testing masterdata /products endpoint..."
curl -s -o /dev/null -w "   HTTP Status: %{http_code}\n" http://localhost/api/masterdata/products || echo "   ⚠️  Endpoint test failed"
echo ""

# Show recent logs from masterdata-service
echo "1️⃣8️⃣  Recent masterdata-service logs (last 30 lines)..."
docker-compose logs --tail=30 masterdata-service
echo ""

echo "=========================================="
echo "✅ Deployment Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Check logs: docker-compose logs -f masterdata-service"
echo "2. Test in browser: https://lego.nji.io/"
echo "3. Login as plant warehouse user and verify products load"
echo "4. If you see 500 errors, check:"
echo "   - docker-compose logs masterdata-service"
echo "   - docker-compose logs api-gateway"
echo ""
echo "To rollback if needed:"
echo "   git checkout <previous-commit>"
echo "   docker-compose build --no-cache"
echo "   docker-compose up -d"
echo ""
