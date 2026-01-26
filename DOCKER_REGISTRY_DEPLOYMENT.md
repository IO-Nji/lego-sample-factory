# Docker Registry Deployment Guide

> **Date:** January 26, 2026  
> **Purpose:** Deploy LIFE System updates using local Docker registry (192.168.1.237:5000)

## Overview

This deployment method uses a local Docker registry to push images from the development machine and pull them on the live server, avoiding the need to rebuild on the live server.

**Benefits:**
- Faster deployments (no compilation on live server)
- Consistent images (same build everywhere)
- Version tracking (tagged with git commit + timestamp)
- Rollback capability (version tags preserved)

## Prerequisites

### On Development Machine
- Docker and docker-compose installed
- Access to local registry at 192.168.1.237:5000
- Git repository up to date

### On Live Server
- Docker and docker-compose installed
- Network access to registry at 192.168.1.237:5000
- docker-compose.yml configured to use registry images

## Deployment Workflow

### Step 1: Build and Push (Development Machine)

```bash
cd /home/nji/Documents/DEV/Java/LIFE/lego-sample-factory

# Run the push script
./push-to-registry.sh
```

**What it does:**
- Gets current git commit and branch
- Creates version tag: `{branch}-{commit}-{timestamp}`
- Builds all 8 services with docker-compose
- Tags each image with:
  - `latest` tag (for easy updates)
  - Version tag (for rollback/tracking)
- Pushes both tags to registry
- Verifies each image by pulling it back
- Provides detailed summary

**Example output:**
```
Registry: 192.168.1.237:5000
Git Branch: dev
Git Commit: 52980ef
Version Tag: dev-52980ef-20260126-143022
Services: 8

Processing: user-service
[1/5] Building user-service...
✓ Build successful
[2/5] Getting image ID...
✓ Image ID: sha256:abc123...
[3/5] Tagging as latest...
✓ Tagged: 192.168.1.237:5000/lego-sample-factory-user-service:latest
[4/5] Tagging with version...
✓ Tagged: 192.168.1.237:5000/lego-sample-factory-user-service:dev-52980ef-20260126-143022
[5/5] Pushing to registry...
✓ Pushed: 192.168.1.237:5000/lego-sample-factory-user-service:latest
✓ Pushed: 192.168.1.237:5000/lego-sample-factory-user-service:dev-52980ef-20260126-143022
✓ Verification successful - image is accessible in registry

SUMMARY
Successful: 8/8
  ✓ nginx-root-proxy
  ✓ frontend
  ✓ api-gateway
  ✓ user-service
  ✓ masterdata-service
  ✓ inventory-service
  ✓ order-processing-service
  ✓ simal-integration-service

All images pushed successfully!
```

### Step 2: Update Live Server

**Option A: Using the pull script (Recommended)**

```bash
# Copy script to live server
scp pull-from-registry.sh nji@io-surf:/home/nji/lego-sample-factory/

# SSH to live server
ssh nji@io-surf

# Navigate to project
cd ~/lego-sample-factory

# Run pull script
./pull-from-registry.sh
```

**Option B: Manual steps**

```bash
# SSH to live server
ssh nji@io-surf
cd ~/lego-sample-factory

# Pull all latest images
docker pull 192.168.1.237:5000/lego-sample-factory-nginx-root-proxy:latest
docker pull 192.168.1.237:5000/lego-sample-factory-frontend:latest
docker pull 192.168.1.237:5000/lego-sample-factory-api-gateway:latest
docker pull 192.168.1.237:5000/lego-sample-factory-user-service:latest
docker pull 192.168.1.237:5000/lego-sample-factory-masterdata-service:latest
docker pull 192.168.1.237:5000/lego-sample-factory-inventory-service:latest
docker pull 192.168.1.237:5000/lego-sample-factory-order-processing-service:latest
docker pull 192.168.1.237:5000/lego-sample-factory-simal-integration-service:latest

# Restart services
docker-compose down
docker-compose up -d

# Check status
docker-compose ps
docker-compose logs -f
```

## Live Server docker-compose.yml Configuration

Update your `docker-compose.yml` on the live server to use registry images instead of building locally:

```yaml
services:
  nginx-root-proxy:
    image: 192.168.1.237:5000/lego-sample-factory-nginx-root-proxy:latest
    # Remove 'build' section
    ports:
      - "80:80"
    # ... rest of config ...

  frontend:
    image: 192.168.1.237:5000/lego-sample-factory-frontend:latest
    # Remove 'build' section
    # ... rest of config ...

  api-gateway:
    image: 192.168.1.237:5000/lego-sample-factory-api-gateway:latest
    # Remove 'build' section
    # ... rest of config ...

  user-service:
    image: 192.168.1.237:5000/lego-sample-factory-user-service:latest
    # Remove 'build' section
    # ... rest of config ...

  masterdata-service:
    image: 192.168.1.237:5000/lego-sample-factory-masterdata-service:latest
    # Remove 'build' section
    # ... rest of config ...

  inventory-service:
    image: 192.168.1.237:5000/lego-sample-factory-inventory-service:latest
    # Remove 'build' section
    # ... rest of config ...

  order-processing-service:
    image: 192.168.1.237:5000/lego-sample-factory-order-processing-service:latest
    # Remove 'build' section
    # ... rest of config ...

  simal-integration-service:
    image: 192.168.1.237:5000/lego-sample-factory-simal-integration-service:latest
    # Remove 'build' section
    # ... rest of config ...
```

## Rollback to Previous Version

If you need to rollback to a previous version:

```bash
# List available versions (on dev machine or registry)
curl http://192.168.1.237:5000/v2/lego-sample-factory-user-service/tags/list

# Pull specific version on live server
docker pull 192.168.1.237:5000/lego-sample-factory-user-service:dev-52980ef-20260126-143022

# Update docker-compose.yml to use specific version tag
# Then restart
docker-compose down
docker-compose up -d
```

## Troubleshooting

### Push fails with "connection refused"
```bash
# Check if registry is accessible
curl http://192.168.1.237:5000/v2/_catalog

# If not accessible, check network or registry service
```

### Pull fails on live server
```bash
# Check network connectivity
ping 192.168.1.237

# Check registry is accessible
curl http://192.168.1.237:5000/v2/_catalog

# Verify image exists in registry
curl http://192.168.1.237:5000/v2/lego-sample-factory-user-service/tags/list
```

### Service fails to start after update
```bash
# Check logs
docker-compose logs -f [service-name]

# Check if image was pulled correctly
docker images | grep 192.168.1.237:5000

# Rollback to previous version if needed
```

### Build fails during push
```bash
# Clean Docker cache
docker system prune -af

# Rebuild specific service
docker-compose build --no-cache [service-name]

# Try push again
./push-to-registry.sh
```

## Services List

All 8 services managed by these scripts:

1. **nginx-root-proxy** - Nginx reverse proxy (port 80/1011)
2. **frontend** - React frontend application
3. **api-gateway** - API gateway (port 8011)
4. **user-service** - User authentication (port 8012)
5. **masterdata-service** - Master data management (port 8013)
6. **inventory-service** - Inventory management (port 8014)
7. **order-processing-service** - Order processing (port 8015)
8. **simal-integration-service** - Scheduling integration (port 8016)

## Quick Commands Reference

**Development machine:**
```bash
./push-to-registry.sh                    # Build and push all services
docker images | grep 192.168.1.237       # List pushed images locally
```

**Live server:**
```bash
./pull-from-registry.sh                  # Pull all and restart
docker-compose pull                      # Just pull images
docker-compose up -d                     # Restart services
docker-compose ps                        # Check status
docker-compose logs -f                   # Watch all logs
docker-compose logs -f user-service      # Watch specific service
```

**Registry management:**
```bash
# List all repositories
curl http://192.168.1.237:5000/v2/_catalog

# List tags for a service
curl http://192.168.1.237:5000/v2/lego-sample-factory-user-service/tags/list
```

## Version Tag Format

Format: `{branch}-{commit}-{timestamp}`

Example: `dev-52980ef-20260126-143022`
- Branch: `dev`
- Commit: `52980ef` (short git commit hash)
- Timestamp: `20260126-143022` (YYYYMMDD-HHMMSS)

This allows you to:
- Identify which branch/commit the image was built from
- Track when the image was created
- Rollback to specific versions if needed

---

**Last Updated:** January 26, 2026  
**Registry:** 192.168.1.237:5000  
**Scripts:** `push-to-registry.sh`, `pull-from-registry.sh`
