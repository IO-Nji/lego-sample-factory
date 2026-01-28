#!/bin/bash
# Restart Docker daemon to apply insecure registry configuration
# Then restart all LIFE containers

echo "==========================================="
echo "Restarting Docker for Registry Config"
echo "==========================================="
echo ""
echo "This will:"
echo "  1. Stop all running containers"
echo "  2. Restart Docker daemon"
echo "  3. Restart LIFE containers"
echo ""
read -p "Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

echo ""
echo "[1/4] Stopping LIFE containers..."
docker-compose down

echo ""
echo "[2/4] Restarting Docker daemon..."
sudo systemctl restart docker

echo ""
echo "[3/4] Waiting for Docker to be ready..."
sleep 5

echo ""
echo "[4/4] Starting LIFE containers..."
docker-compose up -d

echo ""
echo "==========================================="
echo "Docker Configuration Verification"
echo "==========================================="
docker info 2>&1 | grep -A 5 "Insecure Registries"

echo ""
echo "✓ Docker restarted with insecure registry support"
echo "You can now run: ./push-to-registry.sh"
