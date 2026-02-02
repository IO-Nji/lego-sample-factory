#!/bin/bash
# =========================================================
# LIFE System - View Logs
# =========================================================
# Shows logs for all or specific services
#
# Usage:
#   ./logs.sh                    # All services
#   ./logs.sh api-gateway        # Specific service
#   ./logs.sh -f                 # Follow all logs
#   ./logs.sh -f api-gateway     # Follow specific service
# =========================================================

if [ "$1" == "-f" ]; then
    if [ -n "$2" ]; then
        docker compose logs -f "$2"
    else
        docker compose logs -f
    fi
else
    if [ -n "$1" ]; then
        docker compose logs --tail=100 "$1"
    else
        docker compose logs --tail=50
    fi
fi
