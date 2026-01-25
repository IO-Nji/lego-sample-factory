#!/bin/bash
# Diagnostic script for live server masterdata-service issues
# Run this ON THE LIVE SERVER to identify the root cause of 500 errors

echo "=========================================="
echo "LIFE System - Live Server Diagnostics"
echo "=========================================="
echo ""

# Check current directory
if [[ ! -d "/home/nji/lego-sample-factory" ]]; then
    echo "❌ ERROR: Not in correct directory"
    echo "   Run: cd /home/nji/lego-sample-factory"
    exit 1
fi

cd /home/nji/lego-sample-factory

echo "1️⃣  Git Branch & Commit Status"
echo "   Current branch: $(git branch --show-current)"
echo "   Current commit: $(git rev-parse --short HEAD) - $(git log -1 --pretty=%B | head -1)"
echo "   Expected commit: a542422 - Update deployment script to reference correct commit hash"
echo ""

echo "2️⃣  Checking Product.java for merge conflicts..."
if grep -q "<<<<<<< HEAD" lego-factory-backend/masterdata-service/src/main/java/io/life/masterdata/entity/Product.java 2>/dev/null; then
    echo "   ❌ FOUND MERGE CONFLICT MARKERS in Product.java"
    echo "   This is the problem! The file has unresolved conflicts."
    head -30 lego-factory-backend/masterdata-service/src/main/java/io/life/masterdata/entity/Product.java
else
    echo "   ✅ No merge conflict markers found"
    echo "   First 10 lines of Product.java:"
    head -10 lego-factory-backend/masterdata-service/src/main/java/io/life/masterdata/entity/Product.java
fi
echo ""

echo "3️⃣  Docker Container Status"
docker-compose ps
echo ""

echo "4️⃣  Masterdata Service Logs (last 50 lines)"
echo "   Looking for errors, stack traces, or startup failures..."
docker-compose logs --tail=50 masterdata-service
echo ""

echo "5️⃣  API Gateway Logs (last 30 lines)"
echo "   Checking routing and proxy behavior..."
docker-compose logs --tail=30 api-gateway
echo ""

echo "6️⃣  Testing Endpoints Directly (bypassing nginx)"
echo "   Testing masterdata-service directly on port 8013..."
MASTERDATA_TEST=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8013/api/masterdata/products 2>&1 || echo "FAILED")
echo "   Direct to masterdata-service: HTTP $MASTERDATA_TEST"

echo "   Testing through api-gateway on port 8011..."
GATEWAY_TEST=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8011/api/masterdata/products 2>&1 || echo "FAILED")
echo "   Through api-gateway: HTTP $GATEWAY_TEST"

echo "   Testing through nginx on port 80..."
NGINX_TEST=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/api/masterdata/products 2>&1 || echo "FAILED")
echo "   Through nginx: HTTP $NGINX_TEST"
echo ""

echo "7️⃣  Docker Image Information"
echo "   Masterdata service image details:"
docker images | grep masterdata-service | head -3
echo ""

echo "8️⃣  Checking for Java Compilation Errors in Container"
echo "   Examining container for class files..."
docker-compose exec -T masterdata-service ls -la /app/*.jar 2>/dev/null || echo "   ⚠️  Cannot access container filesystem"
echo ""

echo "=========================================="
echo "Diagnosis Summary"
echo "=========================================="
echo ""

# Determine likely issue
if grep -q "<<<<<<< HEAD" lego-factory-backend/masterdata-service/src/main/java/io/life/masterdata/entity/Product.java 2>/dev/null; then
    echo "🔴 CRITICAL ISSUE: Merge conflict markers found in Product.java"
    echo ""
    echo "Solution:"
    echo "  git reset --hard origin/prod"
    echo "  docker-compose down"
    echo "  docker-compose build --no-cache masterdata-service"
    echo "  docker-compose up -d"
elif [[ "$MASTERDATA_TEST" == "500" ]] || [[ "$MASTERDATA_TEST" == "FAILED" ]]; then
    echo "🔴 ISSUE: Masterdata service is crashing or not responding"
    echo ""
    echo "Check the logs above for Java exceptions. Common issues:"
    echo "  - ClassNotFoundException (wrong entity names)"
    echo "  - Table not found errors (database schema mismatch)"
    echo "  - Port binding failures"
    echo ""
    echo "Solution:"
    echo "  1. Review masterdata-service logs above"
    echo "  2. Rebuild: docker-compose build --no-cache masterdata-service"
    echo "  3. Restart: docker-compose up -d"
elif [[ "$GATEWAY_TEST" == "500" ]]; then
    echo "🟡 ISSUE: API Gateway routing problem"
    echo ""
    echo "Solution:"
    echo "  docker-compose build --no-cache api-gateway"
    echo "  docker-compose up -d"
elif [[ "$NGINX_TEST" == "500" ]]; then
    echo "🟡 ISSUE: Nginx proxy configuration"
    echo ""
    echo "Solution:"
    echo "  docker-compose restart nginx-root-proxy"
else
    echo "🟢 Services appear operational - issue may be frontend or auth"
    echo ""
    echo "Check:"
    echo "  1. Frontend build version (may be cached)"
    echo "  2. Browser cache (try incognito mode)"
    echo "  3. JWT token validity"
fi

echo ""
echo "Next steps:"
echo "  1. Review the logs above for specific error messages"
echo "  2. Check Git commit (should be a542422)"
echo "  3. If Product.java has conflicts, run: git reset --hard origin/prod"
echo "  4. Rebuild affected services based on diagnosis above"
echo ""
