# Live Server Deployment Guide - ProductVariant → Product Migration

## Current Issue
The live server (lego.nji.io) is returning 500 errors for:
- `GET /api/masterdata/products` → 500
- `GET /api/masterdata/workstations` → 500

## Root Cause
The live server is running **old code** with the deprecated ProductVariant entity. After our refactoring (commits 75ddfb8, 82046a7, d9ad043), the codebase now uses the Product entity with `/products` endpoints.

## Solution: Deploy Latest prod Branch

### Quick Deploy (Recommended)

**Step 1:** SSH into the live server
```bash
ssh nji@io-surf  # or ssh nji@lego.nji.io
```

**Step 2:** Copy the deployment script to the server
```bash
# From your LOCAL machine (new terminal):
scp deploy-to-live.sh nji@io-surf:/home/nji/lego-sample-factory/

# Or manually copy the script content
```

**Step 3:** Run the deployment script ON the server
```bash
# Now you're ON the live server:
cd /home/nji/lego-sample-factory
chmod +x deploy-to-live.sh
./deploy-to-live.sh
```

The script will:
1. ✅ Fetch latest changes from origin/prod
2. ✅ Pull commit d9ad043 (Product.java fix)
3. ✅ Stop all containers
4. ✅ Rebuild masterdata-service, api-gateway, frontend, order-processing-service
5. ✅ Start all services
6. ✅ Test the /products endpoint
7. ✅ Show recent logs

---

### Manual Deployment (If Script Fails)

If the automated script has issues, follow these steps manually:

```bash
# ON THE LIVE SERVER (lego.nji.io):
cd /home/nji/lego-sample-factory

# 1. Fetch and pull latest prod
git fetch origin
git stash  # Save any local changes
git checkout prod
git pull origin prod

# 2. Verify you have the critical commit
git log --oneline -5
# Should show: d9ad043 Fix merge conflict in Product.java

# 3. Stop all containers
docker-compose down

# 4. Remove old images
docker rmi lego-sample-factory-masterdata-service:latest || true
docker rmi lego-sample-factory-api-gateway:latest || true

# 5. Rebuild affected services (takes 5-10 minutes)
docker-compose build --no-cache masterdata-service
docker-compose build --no-cache api-gateway
docker-compose build --no-cache lego-factory-frontend
docker-compose build --no-cache order-processing-service

# 6. Start all services
docker-compose up -d

# 7. Wait for startup
sleep 20

# 8. Check status
docker-compose ps
docker-compose logs --tail=50 masterdata-service

# 9. Test endpoint
curl http://localhost/api/masterdata/products
# Should return JSON array of products (not 500 error)
```

---

## Verification Steps

### 1. Check Docker Containers Running
```bash
docker-compose ps
```
Expected output: All services should be "Up" (not "Exit 1" or "Restarting")

### 2. Check Masterdata Service Logs
```bash
docker-compose logs -f masterdata-service
```
Look for:
- ✅ `Started MasterdataServiceApplication in X.XXX seconds`
- ✅ `Seeding products table...` (NOT "product_variants")
- ✅ No Java exceptions or stack traces

### 3. Test API Endpoint
```bash
# From within the server:
curl http://localhost/api/masterdata/products

# Or from your browser:
# https://lego.nji.io/api/masterdata/products
```
Expected: JSON array with products (e.g., `[{"id":1,"name":"Steering Wheel",...}]`)

### 4. Test Frontend
1. Open https://lego.nji.io/
2. Login as Plant Warehouse user (username: `plantwarehouse`, password from .env)
3. Dashboard should load products without "Failed to load products" error
4. Check browser console (F12) - should see:
   - ✅ `[useInventoryDisplay] Fetching PRODUCT from /masterdata/products...`
   - ✅ `[useInventoryDisplay] Successfully loaded X products`
   - ❌ NO "Request failed with status code 500"

---

## Troubleshooting

### Issue: "500 Internal Server Error" persists

**Check logs for specific error:**
```bash
docker-compose logs masterdata-service | tail -100
```

**Common causes:**
1. **Database table name mismatch**: Look for `product_variants does not exist`
   - Solution: Ensure H2 database is fresh (happens automatically on restart)
2. **Java compilation errors**: Look for `ClassNotFoundException: ProductVariant`
   - Solution: Rebuild with `--no-cache` flag
3. **Port conflicts**: Look for `Address already in use`
   - Solution: `docker-compose down`, check `netstat -tulpn | grep 80`

### Issue: Git pull shows "Already up to date" but old code is running

**Force rebuild Docker images:**
```bash
docker-compose down
docker system prune -f
docker-compose build --no-cache
docker-compose up -d
```

### Issue: "Merge conflict" errors in logs

**Verify Product.java is clean:**
```bash
head -30 lego-factory-backend/masterdata-service/src/main/java/io/life/masterdata/entity/Product.java
```

Should show:
```java
package io.life.masterdata.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
```

Should NOT show:
```
<<<<<<< HEAD
// Legacy code
========
```

If you see merge markers, run:
```bash
git checkout prod
git reset --hard origin/prod
docker-compose build --no-cache masterdata-service
docker-compose up -d
```

---

## Rollback Plan (If Deployment Fails)

If the new deployment causes critical issues:

```bash
# ON THE LIVE SERVER:
cd /home/nji/lego-sample-factory

# 1. Checkout the previous stable commit
git log --oneline -10  # Find the commit before 75ddfb8
git checkout <previous-commit-hash>

# 2. Rebuild and restart
docker-compose down
docker-compose build --no-cache
docker-compose up -d

# 3. Monitor logs
docker-compose logs -f
```

**Note:** Rollback will restore ProductVariant endpoints (`/product-variants`), so frontend may break if it was already updated.

---

## Key Changes in This Deployment

### Backend Changes
- **Entity renamed**: `ProductVariant` → `Product`
- **Table renamed**: `product_variants` → `products`
- **Endpoints changed**: `/api/masterdata/product-variants` → `/api/masterdata/products`
- **DTO renamed**: `ProductVariantDto` → `ProductDto`
- **ProductModule field**: `productVariantId` → `productId`

### Frontend Changes
- **API constant**: `PRODUCT_VARIANTS_ENDPOINT` → `PRODUCTS_ENDPOINT`
- **useInventoryDisplay**: Removed PRODUCT_VARIANT compatibility
- **PlantWarehouseDashboard**: Filters updated to `itemType === "PRODUCT"`

### Database Impact
- H2 in-memory database will automatically create `products` table (not `product_variants`)
- DataInitializer seeds new table structure on startup
- **No manual migration needed** - fresh H2 database on each restart

---

## Contact & Support

If deployment fails after following this guide:
1. Save docker-compose logs: `docker-compose logs > deployment-error.log`
2. Save git status: `git log --oneline -10 > git-status.txt`
3. Share both files for debugging

**Critical Commits:**
- `75ddfb8` - ProductVariant → Product refactoring (dev)
- `82046a7` - Merge dev into prod
- `d9ad043` - Fix merge conflict in Product.java (LATEST ON PROD)
