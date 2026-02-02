#!/bin/bash

# Fix server docker-compose.yml to use registry images instead of building

SERVER_USER="nji"
SERVER_HOST="io-surf"
SERVER_PATH="/home/nji/DEV/Java/LIFE/lego-sample-factory"
REGISTRY="192.168.1.237:5000"

echo "Updating server's docker-compose.yml to use registry images..."

ssh ${SERVER_USER}@${SERVER_HOST} << 'ENDSSH'
cd /home/nji/DEV/Java/LIFE/lego-sample-factory

# Backup current docker-compose.yml
cp docker-compose.yml docker-compose.yml.backup

# Create new docker-compose.yml using registry images
cat > docker-compose.yml << 'EOF'
services:
  nginx-root-proxy:
    image: 192.168.1.237:5000/lego-sample-factory-nginx-root-proxy:latest
    container_name: nginx-root-proxy
    ports:
      - "${NGINX_ROOT_PROXY_EXTERNAL_PORT:-1011}:80"
    depends_on:
      - frontend
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 3s
      retries: 5

  frontend:
    image: 192.168.1.237:5000/lego-sample-factory-frontend:latest
    container_name: frontend
    ports:
      - "${FRONTEND_EXTERNAL_PORT:-8080}:80"
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 3s
      retries: 5

  api-gateway:
    image: 192.168.1.237:5000/lego-sample-factory-api-gateway:latest
    container_name: api-gateway
    ports:
      - "${API_GATEWAY_PORT:-8011}:8011"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SECURITY_JWT_SECRET=${SECURITY_JWT_SECRET}
      - USER_SERVICE_URL=http://user-service:8012
      - MASTERDATA_SERVICE_URL=http://masterdata-service:8013
      - INVENTORY_SERVICE_URL=http://inventory-service:8014
      - ORDER_PROCESSING_SERVICE_URL=http://order-processing-service:8015
      - SIMAL_INTEGRATION_SERVICE_URL=http://simal-integration-service:8016
    depends_on:
      - user-service
      - masterdata-service
      - inventory-service
      - order-processing-service
      - simal-integration-service
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8011/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  user-service:
    image: 192.168.1.237:5000/lego-sample-factory-user-service:latest
    container_name: user-service
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SECURITY_JWT_SECRET=${SECURITY_JWT_SECRET}
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8012/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  masterdata-service:
    image: 192.168.1.237:5000/lego-sample-factory-masterdata-service:latest
    container_name: masterdata-service
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8013/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  inventory-service:
    image: 192.168.1.237:5000/lego-sample-factory-inventory-service:latest
    container_name: inventory-service
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MASTERDATA_SERVICE_URL=http://masterdata-service:8013
    depends_on:
      - masterdata-service
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8014/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  order-processing-service:
    image: 192.168.1.237:5000/lego-sample-factory-order-processing-service:latest
    container_name: order-processing-service
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MASTERDATA_SERVICE_URL=http://masterdata-service:8013
      - INVENTORY_SERVICE_URL=http://inventory-service:8014
    depends_on:
      - masterdata-service
      - inventory-service
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8015/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  simal-integration-service:
    image: 192.168.1.237:5000/lego-sample-factory-simal-integration-service:latest
    container_name: simal-integration-service
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    networks:
      - lego-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8016/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

networks:
  lego-network:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.enable_icc: "true"
EOF

echo "✓ Updated docker-compose.yml to use registry images"

# Pull latest images
echo "Pulling latest images from registry..."
docker compose pull

# Restart services
echo "Restarting services..."
docker compose down
docker compose up -d

echo "✓ Services restarted with registry images"
docker compose ps

ENDSSH

echo "Done! Server is now using registry images."
