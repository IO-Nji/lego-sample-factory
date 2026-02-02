#!/bin/bash
# =========================================================
# LIFE System - Start Services
# =========================================================
# Starts all services using existing images
# =========================================================

set -e

echo "Starting LIFE System services..."
docker compose up -d

echo ""
echo "Services starting. Check status with:"
echo "  docker compose ps"
echo ""
echo "View logs with:"
echo "  docker compose logs -f"
