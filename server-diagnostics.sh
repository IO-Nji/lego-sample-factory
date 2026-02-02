#!/bin/bash
# =========================================================
# Server Diagnostics - Run this on 192.168.1.237
# =========================================================
# This script gathers information needed for deployment setup
# Copy output and share with the developer
# =========================================================

echo "========================================="
echo "LIFE System - Server Diagnostics"
echo "========================================="
echo ""

echo "=== System Info ==="
echo "Hostname: $(hostname)"
echo "IP Addresses: $(hostname -I)"
echo "OS: $(cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2)"
echo "User: $(whoami)"
echo "Home: $HOME"
echo ""

echo "=== Docker Info ==="
if command -v docker &> /dev/null; then
    echo "Docker Version: $(docker --version)"
    echo "Docker Compose: $(docker compose version 2>/dev/null || docker-compose --version 2>/dev/null || echo 'Not found')"
    echo "Docker Running: $(systemctl is-active docker 2>/dev/null || echo 'unknown')"
    echo ""
    echo "Docker Daemon Config:"
    if [ -f /etc/docker/daemon.json ]; then
        cat /etc/docker/daemon.json
    else
        echo "  No daemon.json found"
    fi
else
    echo "Docker: NOT INSTALLED"
fi
echo ""

echo "=== Registry Connectivity ==="
if curl -s --connect-timeout 3 http://192.168.1.237:5000/v2/_catalog > /dev/null 2>&1; then
    echo "Registry (192.168.1.237:5000): ACCESSIBLE"
    echo "Available images:"
    curl -s http://192.168.1.237:5000/v2/_catalog 2>/dev/null | python3 -m json.tool 2>/dev/null || curl -s http://192.168.1.237:5000/v2/_catalog
else
    echo "Registry (192.168.1.237:5000): NOT ACCESSIBLE"
fi
echo ""

echo "=== Port Availability ==="
for port in 1011 8011 8080; do
    if netstat -tuln 2>/dev/null | grep -q ":${port} " || ss -tuln 2>/dev/null | grep -q ":${port} "; then
        echo "Port $port: IN USE"
        netstat -tuln 2>/dev/null | grep ":${port} " || ss -tuln 2>/dev/null | grep ":${port} "
    else
        echo "Port $port: AVAILABLE"
    fi
done
echo ""

echo "=== Existing LIFE Containers ==="
docker ps -a --filter "name=lego" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "None found"
echo ""

echo "=== Current Directory ==="
echo "PWD: $(pwd)"
echo ""

echo "=== Git Status ==="
if [ -d ".git" ]; then
    echo "Git repo found"
    echo "Branch: $(git branch --show-current 2>/dev/null)"
    echo "Remote: $(git remote -v 2>/dev/null | head -1)"
else
    echo "Not in a git repository"
fi
echo ""

echo "=== Disk Space ==="
df -h / | tail -1
echo ""

echo "========================================="
echo "Diagnostics complete. Share this output."
echo "========================================="
