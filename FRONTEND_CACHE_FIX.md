# Frontend Cache Issue - Diagnostic and Fix Guide

## Problem
Latest frontend changes (triggerScenario fix) visible on localhost but NOT on live server, even after rebuilding and pushing images.

## Root Cause
**Browser and/or Nginx caching** is serving old JavaScript/CSS bundles instead of the new ones.

## Verification Steps

### 1. Verify Registry Has Latest Image
```bash
# Check registry image timestamp
curl -s http://192.168.1.237:5000/v2/lego-sample-factory-frontend/tags/list | jq .

# Expected output should include: "dev-5c6d0a9-20260126-122214" or newer
```

### 2. Verify Server Pulled Latest Image
```bash
# On live server
ssh nji@io-surf
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}" | grep frontend

# Should show image ID: 31263030a301, Created: 2026-01-26 04:57:40
```

### 3. Check What's Inside Running Container
```bash
# On live server - check asset timestamps inside running container
docker exec nginx-root-proxy ls -lh /usr/share/nginx/html/assets/

# Should show files dated: Jan 26 03:57 (UTC) = 04:57 (local)
```

## Solution: Clear All Caches

### Option 1: Hard Browser Refresh (First Try This)

**Chrome/Edge/Brave:**
- Windows/Linux: `Ctrl + Shift + R` or `Ctrl + F5`
- Mac: `Cmd + Shift + R`

**Firefox:**
- Windows/Linux: `Ctrl + Shift + R` or `Ctrl + F5`
- Mac: `Cmd + Shift + R`

**Safari:**
- Mac: `Cmd + Option + R`

### Option 2: Clear Browser Cache Completely

1. Open DevTools (F12)
2. Right-click the refresh button → "Empty Cache and Hard Reload"
3. Or go to Settings → Privacy → Clear browsing data → Cached images and files

### Option 3: Force Nginx to Clear Cache + Restart Services

```bash
# On live server
ssh nji@io-surf
cd ~/lego-sample-factory

# Stop all services
docker-compose down

# Pull latest images (ensures we have newest)
./update-from-registry.sh

# Clear any Docker volume caches (if any)
docker volume prune -f

# Restart with clean state
docker-compose up -d --force-recreate

# Verify services are running
docker-compose ps
docker-compose logs -f nginx-root-proxy frontend
```

### Option 4: Add Cache-Busting Headers to Nginx Config

Edit `nginx-root-proxy/nginx.conf` to prevent caching:

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    root /usr/share/nginx/html;
    expires -1;  # Don't cache
    add_header Cache-Control "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0";
    try_files $uri =404;
}
```

Then rebuild and push:
```bash
# On dev machine
docker-compose build --no-cache nginx-root-proxy
./push-to-registry.sh

# On live server
./update-from-registry.sh
```

### Option 5: Verify JavaScript Bundle Hash Changed

The frontend uses Vite which generates hashed filenames (e.g., `index-k5PY5sRo.js`). Each build creates unique hashes, so browsers automatically fetch new files.

**Check current bundle names:**
```bash
# On live server
docker exec nginx-root-proxy ls -l /usr/share/nginx/html/assets/

# Compare to what browser is requesting:
# Open browser DevTools → Network tab → Filter JS/CSS
# Look for filenames like: index-XXXXX.js, index-XXXXX.css
```

If browser is requesting **old hash names**, it means:
- Browser has cached the `index.html` file which references old bundles
- Solution: Clear browser cache (Option 1 or 2)

## Testing After Fix

1. **Open browser DevTools** (F12)
2. Go to **Network tab**
3. **Hard refresh** (Ctrl+Shift+R)
4. Verify you see:
   - `Status 200` (not 304 Not Modified) for JS/CSS files
   - New hash names in filenames (e.g., `index-k5PY5sRo.js`)
5. **Test the triggerScenario functionality:**
   - Login as `modules_supermarket` / `password`
   - Navigate to Modules Supermarket dashboard
   - Confirm a warehouse order
   - Check if correct button appears:
     - `PRODUCTION_REQUIRED` → Should show "🏭 Order Production" button
     - `DIRECT_FULFILLMENT` → Should show "Fulfill" button

## How to Prevent This in Future

1. **Always hard refresh** after deployments: `Ctrl + Shift + R`
2. **Use incognito/private mode** for testing new deployments
3. **Add version number to frontend** (display git commit in footer)
4. **Monitor Network tab** in DevTools to see what's being cached

## Expected Asset Timestamps (Latest Build)

| File | Expected Date | Hash |
|------|---------------|------|
| index-BHVMtFsv.css | Jan 26 03:57 UTC | 166.7K |
| index-k5PY5sRo.js | Jan 26 03:57 UTC | 308.3K |
| router-X4-hMGP6.js | Jan 26 03:57 UTC | 21.6K |
| vendor-CRB3T2We.js | Jan 26 03:57 UTC | 138.4K |

If live server shows different hashes or dates, run `./update-from-registry.sh` again.

## Quick Diagnostic Command

Run this on the live server to verify everything:

```bash
#!/bin/bash
echo "=== Image Version ==="
docker images | grep frontend | grep latest

echo -e "\n=== Running Container ==="
docker ps | grep frontend

echo -e "\n=== Assets Inside Container ==="
docker exec nginx-root-proxy ls -lh /usr/share/nginx/html/assets/

echo -e "\n=== Registry Latest Tag ==="
curl -s http://192.168.1.237:5000/v2/lego-sample-factory-frontend/tags/list | jq .

echo -e "\n=== Expected Image ID: 31263030a301 ==="
echo "=== Expected Asset Date: Jan 26 03:57 (UTC) ==="
```

---

**Created:** January 26, 2026  
**Issue:** Frontend cache preventing latest triggerScenario fix from appearing  
**Solution:** Hard browser refresh + verify image versions
