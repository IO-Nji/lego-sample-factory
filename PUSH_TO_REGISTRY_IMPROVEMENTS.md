# Push to Registry Script Improvements

**Date:** January 26, 2026  
**Script:** `push-to-registry.sh`

## Summary of Changes

The `push-to-registry.sh` script has been enhanced to handle build errors more gracefully and only build services that have actually been modified.

## Key Improvements

### 1. Smart Service Detection
**Before:** Built all 8 services every time  
**After:** Detects which services have been modified by comparing with `origin/prod`

```bash
# Build only modified services (default)
./push-to-registry.sh

# Build all services
./push-to-registry.sh --all
```

### 2. Better Error Handling
**Before:** Script failed on first build warning/error  
**After:** 
- Continues even if warnings occur
- Only fails on actual build errors (exit code ≠ 0)
- Processes remaining services even if one fails
- Exits successfully if at least some services were pushed

### 3. Improved Image Detection
**Before:** Single method to find built images  
**After:** Three fallback methods:
1. `docker-compose images -q`
2. `docker images -q ${PROJECT_NAME}-${SERVICE}`
3. `docker images -q lego-sample-factory-${SERVICE}`

Shows helpful diagnostics if image can't be found.

### 4. Enhanced Output
- Shows which services are modified vs unchanged
- Displays relevant build output only (filtered)
- Better progress indicators
- Detailed summary at the end

## Usage Examples

### Default Mode (Modified Services Only)
```bash
./push-to-registry.sh
```
Output:
```
Detecting modified services...
  ✓ frontend - modified
  ○ api-gateway - unchanged
  ○ user-service - unchanged
  ...

Services to build: 1
  • frontend
```

### Build All Services
```bash
./push-to-registry.sh --all
```

### Get Help
```bash
./push-to-registry.sh --help
```

## Service Path Mapping

The script correctly maps services to their directories:

| Service | Path |
|---------|------|
| nginx-root-proxy | `nginx-root-proxy/` |
| frontend | `lego-factory-frontend/` |
| api-gateway | `lego-factory-backend/api-gateway/` |
| user-service | `lego-factory-backend/user-service/` |
| masterdata-service | `lego-factory-backend/masterdata-service/` |
| inventory-service | `lego-factory-backend/inventory-service/` |
| order-processing-service | `lego-factory-backend/order-processing-service/` |
| simal-integration-service | `lego-factory-backend/simal-integration-service/` |

## How It Works

1. **Detection Phase:**
   - Runs `git diff --quiet origin/prod HEAD -- <service-path>`
   - Marks services as modified/unchanged
   - Builds list of services to process

2. **Build Phase:**
   - Runs `docker-compose build --no-cache` for each service
   - Captures build output
   - Checks exit code (not just output)
   - Continues to next service if one fails

3. **Tag & Push Phase:**
   - Tries multiple methods to find image ID
   - Tags with `latest` and version tag
   - Pushes both tags to registry
   - Verifies by pulling from registry

4. **Summary:**
   - Shows successful vs failed services
   - Exits with code 0 if at least one succeeded
   - Exits with code 1 only if all failed

## Benefits

1. **Faster Builds:** Only builds what changed (saves time on multi-service updates)
2. **More Resilient:** Doesn't abort entire process on single service warning
3. **Better Debugging:** Shows which services failed and why
4. **Flexible:** Can force build all services with `--all` when needed
5. **User-Friendly:** Clear output and help documentation

## Troubleshooting

### No Services Detected
If no modified services are detected:
```
No modified services detected.
Use --all flag to build all services anyway.
```
**Solution:** Use `--all` flag or commit your changes first

### Image Not Found After Build
If the script can't find the built image, it will show:
```
⚠ Could not find image ID automatically, searching...
Available images:
  (lists matching images)
```
**Solution:** Check the displayed images and verify build completed successfully

### Some Services Failed
The script will continue and show summary:
```
Successful: 5/8
  ✓ frontend
  ✓ api-gateway
  ...

Failed: 3/8
  ✗ user-service
  ...

Some services failed, but successful ones were pushed.
```
**Result:** Exit code 0 (success) - partial push completed

## Next Steps on Live Server

After successful push, update live server:

```bash
# SSH to live server
ssh nji@io-surf

# Navigate to project
cd ~/lego-sample-factory

# Pull new images
docker-compose pull

# Or use the update script
./pull-from-registry.sh

# Restart services
docker-compose up -d
```

---

**Script Location:** `/home/nji/Documents/DEV/Java/LIFE/lego-sample-factory/push-to-registry.sh`  
**Registry:** `192.168.1.237:5000`

