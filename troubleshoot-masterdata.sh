#!/bin/bash

# Masterdata Service Troubleshooting Script
# Run this on your Ubuntu server to diagnose the 502/500 errors

echo "=================================================="
echo "LEGO Factory - Masterdata Service Diagnostics"
echo "=================================================="
echo ""

echo "1. Checking Docker container status..."
docker-compose ps masterdata-service
echo ""

echo "2. Checking if masterdata-service is responding..."
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8013/actuator/health || echo "Service not accessible on localhost:8013"
echo ""

echo "3. Last 50 lines of masterdata-service logs:"
echo "--------------------------------------------"
docker-compose logs --tail=50 masterdata-service
echo ""

echo "4. Checking for errors in logs:"
echo "--------------------------------------------"
docker-compose logs masterdata-service | grep -i "error\|exception\|failed" | tail -20
echo ""

echo "5. Checking DataInitializer seeding logs:"
echo "--------------------------------------------"
docker-compose logs masterdata-service | grep -i "seed\|initializ\|product\|module" | tail -20
echo ""

echo "6. Testing API endpoint directly (bypass nginx):"
echo "--------------------------------------------"
curl -s http://localhost:8013/api/masterdata/product-variants | head -100
echo ""

echo "7. Testing through API gateway:"
echo "--------------------------------------------"
curl -s http://localhost:8011/api/masterdata/product-variants | head -100
echo ""

echo "=================================================="
echo "RECOMMENDED FIXES:"
echo "=================================================="
echo ""
echo "If service is not running or crashed:"
echo "  docker-compose restart masterdata-service"
echo ""
echo "If database seeding failed:"
echo "  docker-compose restart masterdata-service && docker-compose logs -f masterdata-service"
echo ""
echo "If compilation errors (check logs above):"
echo "  docker-compose build --no-cache masterdata-service"
echo "  docker-compose up -d masterdata-service"
echo ""
echo "Full rebuild all services:"
echo "  docker-compose down"
echo "  docker-compose build --no-cache"
echo "  docker-compose up -d"
echo ""
