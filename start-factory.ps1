#!/usr/bin/env pwsh
# LEGO Factory Startup Script
# Starts the complete Docker stack for LEGO Sample Factory Control System

Write-Host "🏭 Starting LEGO Sample Factory Control System..." -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "🔍 Checking Docker status..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "✅ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Check if docker-compose.yml exists
if (-not (Test-Path "docker-compose.yml")) {
    Write-Host "❌ docker-compose.yml not found in current directory." -ForegroundColor Red
    Write-Host "Please run this script from the root project directory." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🔧 Building and starting all services..." -ForegroundColor Yellow
Write-Host "This may take a few minutes on first run..." -ForegroundColor Gray

# Start the Docker stack
try {
    docker-compose up -d
    Write-Host ""
    Write-Host "✅ All services started successfully!" -ForegroundColor Green
    Write-Host ""
    
    # Show service status
    Write-Host "📊 Service Status:" -ForegroundColor Cyan
    docker-compose ps
    
    Write-Host ""
    Write-Host "🌐 Application Access URLs:" -ForegroundColor Cyan
    Write-Host "   Frontend:    http://localhost" -ForegroundColor White
    Write-Host "   API Gateway: http://localhost/api/" -ForegroundColor White
    Write-Host ""
    
    Write-Host "👥 Default Login Accounts:" -ForegroundColor Cyan
    Write-Host "   Admin:       lego_admin / password" -ForegroundColor White
    Write-Host "   Warehouse:   warehouse_operator / password" -ForegroundColor White
    Write-Host "   Modules:     modules_supermarket / password" -ForegroundColor White
    Write-Host ""
    
    Write-Host "🔍 Useful Commands:" -ForegroundColor Cyan
    Write-Host "   View logs:   docker-compose logs -f [service-name]" -ForegroundColor Gray
    Write-Host "   Stop all:    docker-compose down" -ForegroundColor Gray
    Write-Host "   Restart:     docker-compose restart [service-name]" -ForegroundColor Gray
    Write-Host ""
    
    Write-Host "🎯 Ready! Open your browser to http://localhost" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Failed to start services:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "🛠️ Troubleshooting steps:" -ForegroundColor Yellow
    Write-Host "   1. Check Docker Desktop is running" -ForegroundColor Gray
    Write-Host "   2. Run: docker-compose down" -ForegroundColor Gray  
    Write-Host "   3. Run: docker system prune -f" -ForegroundColor Gray
    Write-Host "   4. Try again: ./start-factory.ps1" -ForegroundColor Gray
    exit 1
}