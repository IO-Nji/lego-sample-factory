#!/bin/bash
# Deploy triggerScenario fix to live server
# Date: January 25, 2026
# Commit: 52980ef

echo "========================================="
echo "LIFE System - TriggerScenario Fix Deploy"
echo "========================================="
echo ""

# Navigate to project directory
cd /home/nji/Documents/DEV/Java/LIFE/lego-sample-factory || exit 1

# Pull latest changes
echo "1. Pulling latest changes from prod branch..."
git fetch origin
git checkout prod
git pull origin prod

# Check current commit
CURRENT_COMMIT=$(git rev-parse --short HEAD)
echo "   Current commit: $CURRENT_COMMIT"
echo ""

# Rebuild order-processing-service (the service we modified)
echo "2. Rebuilding order-processing-service..."
docker-compose build --no-cache order-processing-service

# Restart the service
echo ""
echo "3. Restarting order-processing-service..."
docker-compose up -d order-processing-service

# Wait for service to start
echo ""
echo "4. Waiting for service to start (10 seconds)..."
sleep 10

# Check service health
echo ""
echo "5. Checking service status..."
docker-compose ps order-processing-service

# Show recent logs
echo ""
echo "6. Recent service logs:"
echo "   (Press Ctrl+C to stop watching logs)"
echo ""
docker-compose logs -f --tail=50 order-processing-service
