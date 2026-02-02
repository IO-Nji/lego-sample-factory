# LIFE System - Production Deployment

**Pre-configured for server: 192.168.1.237 (io-surf)**

This directory contains everything needed to deploy the LIFE System using pre-built Docker images from the local registry.

## Quick Start (First Time)

```bash
# 1. Clone the repo
git clone https://github.com/IO-Nji/lego-sample-factory.git
cd lego-sample-factory/deploy

# 2. Run first-time setup (creates .env, configures Docker)
./setup.sh

# 3. Pull images and start services
./update.sh
```

## Quick Start (Updates)

```bash
# Just pull latest images and restart
cd ~/lego-sample-factory/deploy
./update.sh
```

## Directory Contents

| File | Description |
|------|-------------|
| `docker-compose.yml` | Production compose file (pulls from registry) |
| `.env.example` | Pre-configured environment (ready to use) |
| `setup.sh` | First-time setup script |
| `update.sh` | Pull latest images and restart services |
| `start.sh` | Start services (without pulling) |
| `stop.sh` | Stop all services |
| `status.sh` | Check service health |
| `logs.sh` | View service logs |

## Prerequisites

### 1. Docker & Docker Compose
```bash
# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Verify
docker --version
docker compose version
```

### 2. Configure Insecure Registry
The registry at `192.168.1.237:5000` uses HTTP. Configure Docker to allow it:

```bash
# Create or edit daemon.json
sudo nano /etc/docker/daemon.json
```

Add:
```json
{
  "insecure-registries": ["192.168.1.237:5000"]
}
```

Then restart Docker:
```bash
sudo systemctl restart docker
```

### 3. Network Access
Ensure the server can reach:
- `192.168.1.237:5000` (Docker registry)
- Outbound internet (for initial Docker setup)

## Configuration

### Environment Variables

Edit `.env` to configure your deployment:

| Variable | Description | Default |
|----------|-------------|---------|
| `SECURITY_JWT_SECRET` | JWT signing key (change in production!) | - |
| `NGINX_ROOT_PROXY_EXTERNAL_PORT` | Port to access the app | 1011 |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | localhost |
| `SPRING_PROFILES_ACTIVE` | Spring profile (dev/prod) | prod |
| `LOT_SIZE_THRESHOLD` | Scenario 4 threshold | 3 |

### Spring Profiles

| Profile | Use Case |
|---------|----------|
| `dev` | Development (DEBUG logging) |
| `prod` | Production (INFO logging, optimized) |
| `cloud` | Cloud deployment (Redis, PostgreSQL ready) |

## Daily Operations

### Update to Latest Version
```bash
./update.sh
```

### Check Status
```bash
./status.sh
```

### View Logs
```bash
# All services
./logs.sh

# Specific service
./logs.sh api-gateway

# Follow logs
./logs.sh -f
./logs.sh -f api-gateway
```

### Restart Services
```bash
./stop.sh
./start.sh
```

## Troubleshooting

### Registry Connection Failed
```bash
# Check registry is accessible
curl http://192.168.1.237:5000/v2/_catalog

# Check Docker daemon config
cat /etc/docker/daemon.json

# Restart Docker
sudo systemctl restart docker
```

### Service Unhealthy
```bash
# Check specific service logs
./logs.sh order-processing-service

# Restart just that service
docker compose restart order-processing-service
```

### Port Already in Use
```bash
# Check what's using port 1011
sudo lsof -i :1011

# Kill the process or change NGINX_ROOT_PROXY_EXTERNAL_PORT in .env
```

## Architecture

```
Internet/LAN
     │
     ▼
┌─────────────────┐
│  nginx-proxy    │ :1011
│  (entry point)  │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐ ┌───────────┐
│frontend│ │api-gateway│ :8011
└───────┘ └─────┬─────┘
                │
    ┌───────────┼───────────┐
    ▼           ▼           ▼
┌───────┐  ┌─────────┐  ┌───────┐
│ user  │  │inventory│  │orders │
│service│  │ service │  │service│
└───────┘  └─────────┘  └───────┘
    :8012      :8014       :8015
```

## Development Workflow

1. **On Dev Machine:** Make changes, test, build images
   ```bash
   docker compose build
   ./push-to-registry.sh --all
   ```

2. **On Server:** Pull and deploy
   ```bash
   cd ~/lego-sample-factory/deploy
   ./update.sh
   ```
