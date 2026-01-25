# Live Server Deployment Commands - Run ON THE SERVER

## Current Situation
Your local prod branch on the server has diverged from the remote. This is expected because we completely replaced prod with a clean version from dev.

## Solution: Reset Local Prod to Match Remote

**Run these commands ON THE LIVE SERVER (lego.nji.io):**

```bash
# 1. Discard local prod and take the clean remote version
cd /home/nji/lego-sample-factory
git reset --hard origin/prod

# 2. Verify you have the correct commit
git log --oneline -5
# Should show:
# a542422 Update deployment script to reference correct commit hash
# b9e4fcb feat: Add live server deployment guide and product migration summary
# 75ddfb8 CRITICAL: Complete removal of ProductVariant entity, replaced with Product throughout codebase

# 3. Verify Product.java is clean (no merge conflicts)
head -20 lego-factory-backend/masterdata-service/src/main/java/io/life/masterdata/entity/Product.java
# Should show clean Java code, NOT merge conflict markers

# 4. Stop all containers
docker-compose down

# 5. Remove old images to force clean rebuild
docker rmi lego-sample-factory-masterdata-service:latest || true
docker rmi lego-sample-factory-api-gateway:latest || true
docker rmi lego-sample-factory-lego-factory-frontend:latest || true
docker rmi lego-sample-factory-order-processing-service:latest || true

# 6. Rebuild affected services (takes 5-10 minutes)
docker-compose build --no-cache masterdata-service
docker-compose build --no-cache api-gateway  
docker-compose build --no-cache lego-factory-frontend
docker-compose build --no-cache order-processing-service

# 7. Start all services
docker-compose up -d

# 8. Wait for services to initialize
sleep 20

# 9. Check service status
docker-compose ps

# 10. Test the products endpoint
curl http://localhost/api/masterdata/products
# Should return JSON array, NOT 500 error

# 11. Check logs if there are issues
docker-compose logs --tail=50 masterdata-service
docker-compose logs --tail=50 api-gateway
```

## Expected Results

### Successful API Response
```json
[
  {
    "id": 1,
    "name": "Steering Wheel",
    "description": "Steering wheel for the LEGO vehicle",
    "sku": "LEGO-SW-001"
  },
  {
    "id": 2,
    "name": "Chassis",
    "description": "Main chassis frame",
    "sku": "LEGO-CH-001"
  }
  // ... more products
]
```

### Docker Services Status
```
NAME                        STATUS
masterdata-service         Up (healthy)
api-gateway                Up (healthy)
lego-factory-frontend      Up (healthy)
order-processing-service   Up (healthy)
user-service              Up (healthy)
inventory-service         Up (healthy)
nginx-root-proxy          Up (healthy)
```

## Verification Steps

### 1. Test in Browser
1. Open: https://lego.nji.io/
2. Login as Plant Warehouse user (username: `plantwarehouse`)
3. Dashboard should load products successfully
4. Check browser console (F12) - should see:
   - ✅ `[useInventoryDisplay] Successfully loaded X products`
   - ❌ NO "Request failed with status code 500"

### 2. Test API Directly
```bash
# From the server:
curl http://localhost/api/masterdata/products | jq .
curl http://localhost/api/masterdata/workstations | jq .
curl http://localhost/api/masterdata/modules | jq .
```

### 3. Check Database
The H2 database should have the `products` table (NOT `product_variants`):
```bash
# Check masterdata-service logs for seeding confirmation
docker-compose logs masterdata-service | grep -i "seeding\|products table"
```

## Troubleshooting

### If curl returns 500 error:
```bash
# Check masterdata-service logs for Java exceptions
docker-compose logs masterdata-service | tail -100

# Look for:
# - "Table 'PRODUCT_VARIANTS' not found" (should NOT appear)
# - "ClassNotFoundException: ProductVariant" (should NOT appear)
# - "Started MasterdataServiceApplication" (should appear)
```

### If Docker build fails:
```bash
# Check for disk space
df -h

# Clear Docker cache and try again
docker system prune -f
docker-compose build --no-cache masterdata-service
```

### If services won't start:
```bash
# Check port conflicts
netstat -tulpn | grep -E ':(80|8011|8012|8013|8014|8015|8016)'

# Check logs for specific service
docker-compose logs --tail=100 <service-name>
```

## Quick Rollback (If Needed)

If the new deployment causes critical issues:
```bash
# Stop all services
docker-compose down

# Checkout previous commit (before ProductVariant removal)
git log --oneline -20  # Find the commit before 75ddfb8
git checkout <previous-commit-hash>

# Rebuild and restart
docker-compose build --no-cache
docker-compose up -d
```

**Note:** Rollback will restore ProductVariant endpoints, so frontend may have issues if it was updated.

## Summary

**Key Command:** `git reset --hard origin/prod`

This discards your local divergent prod branch and takes the clean version we just pushed from the dev machine.

**What Changed:**
- ProductVariant → Product (all code, endpoints, database tables)
- Endpoints: `/product-variants` → `/products`
- Database: `product_variants` → `products`
- No merge conflicts in Product.java

**Expected Outcome:**
- Plant Warehouse dashboard loads products successfully
- No 500 errors from masterdata-service
- All API endpoints return 200 with valid JSON
