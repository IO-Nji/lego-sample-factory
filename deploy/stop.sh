#!/bin/bash
# =========================================================
# LIFE System - Stop Services
# =========================================================
# Stops all services gracefully
# =========================================================

set -e

echo "Stopping LIFE System services..."
docker compose down

echo ""
echo "All services stopped."
