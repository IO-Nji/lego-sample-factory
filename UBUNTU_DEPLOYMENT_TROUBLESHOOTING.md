# Ubuntu Server Deployment Troubleshooting Guide

## Issue: User and Workstation Data Not Loading

If your application runs but doesn't load user and workstation data, follow these diagnostic steps:

## 1. Create Environment File (CRITICAL)

The application requires environment variables. Create a `.env` file in the root directory:

```bash
cd /path/to/lego-sample-factory
nano .env
```

Add the following content:

```env
# Security (REQUIRED)
SECURITY_JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-long-change-this-in-production
SECURITY_JWT_EXPIRATION=PT1H

# Networking
NGINX_ROOT_PROXY_EXTERNAL_PORT=80
DOCKER_NETWORK_NAME=lego-network

# Service Ports (Internal) - DO NOT CHANGE unless you have conflicts
API_GATEWAY_PORT=8011
USER_SERVICE_PORT=8012
MASTERDATA_SERVICE_PORT=8013
INVENTORY_SERVICE_PORT=8014
ORDER_PROCESSING_SERVICE_PORT=8015
SIMAL_INTEGRATION_SERVICE_PORT=8016

# Database Configuration (H2 In-Memory)
H2_DB_USER_PATH=jdbc:h2:mem:lego_factory_auth
H2_DB_MASTERDATA_PATH=jdbc:h2:mem:masterdata_db
H2_DB_INVENTORY_PATH=jdbc:h2:mem:inventory_db
H2_DB_ORDER_PROCESSING_PATH=jdbc:h2:mem:orders_db
H2_DB_SIMAL_INTEGRATION_PATH=jdbc:h2:mem:simal_db

SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
SPRING_H2_CONSOLE_ENABLED=true

# Logging
LOGGING_LEVEL_IO_LIFE=DEBUG
LOG_LEVEL_ROOT=INFO
```

**Save and exit** (Ctrl+X, then Y, then Enter)

## 2. Check Port Conflicts

### Check if port 80 is already in use:
```bash
sudo netstat -tulpn | grep :80
# OR
sudo lsof -i :80
```

If port 80 is occupied (e.g., by Apache or another nginx):
- **Option A**: Stop the conflicting service
  ```bash
  sudo systemctl stop apache2  # if Apache is running
  sudo systemctl stop nginx    # if system nginx is running
  ```

- **Option B**: Change the external port in `.env`:
  ```env
  NGINX_ROOT_PROXY_EXTERNAL_PORT=8080
  ```
  Then access via: `http://your-server-ip:8080`

### Check internal ports (8011-8016):
```bash
sudo netstat -tulpn | grep -E ':(8011|8012|8013|8014|8015|8016)'
```

If any of these ports are occupied, either stop the conflicting service or change the port in `.env`.

## 3. Verify Services Are Running

```bash
docker-compose ps
```

**Expected output**: All services should show "Up" status:
- nginx-root-proxy
- frontend
- api-gateway
- user-service
- masterdata-service
- inventory-service
- order-processing-service
- simal-integration-service

## 4. Check Service Logs

### Check user-service logs (most critical for login):
```bash
docker-compose logs user-service | tail -100
```

**Look for**:
- ✅ `Started UserServiceApplication`
- ✅ Database initialization messages
- ✅ `Liquibase` or `Hibernate` schema creation
- ❌ Port binding errors
- ❌ Database connection errors
- ❌ JWT secret errors

### Check api-gateway logs:
```bash
docker-compose logs api-gateway | tail -100
```

**Look for**:
- ✅ `Started ApiGatewayApplication`
- ✅ Route configurations loaded
- ❌ Cannot connect to services errors
- ❌ JWT configuration errors

### Check all services at once:
```bash
docker-compose logs --tail=50 -f
```

## 5. Test Service Connectivity

### Test API Gateway health:
```bash
curl http://localhost:80/api/actuator/health
```

**Expected**: `{"status":"UP"}`

### Test user-service directly (from inside Docker network):
```bash
docker exec lego-sample-factory-user-service-1 curl -s http://localhost:8012/actuator/health
```

**Expected**: `{"status":"UP"}`

### Test login endpoint:
```bash
curl -X POST http://localhost:80/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"lego_admin","password":"password"}'
```

**Expected**: JWT token response with user details

**If you get 404**: API Gateway is not routing correctly
**If you get 401**: User data is not initialized
**If you get connection refused**: Services are not communicating

## 6. Database Initialization Issues

The H2 databases should initialize automatically with seed data. If users aren't loading:

### Check if user-service database initialized:
```bash
docker-compose logs user-service | grep -i "insert\|liquibase\|schema"
```

### Access H2 Console (if enabled):
```bash
# Port forward to access H2 console
docker exec -it lego-sample-factory-user-service-1 /bin/sh
curl http://localhost:8012/h2-console
```

Or expose the port temporarily in docker-compose.yml:
```yaml
user-service:
  ports:
    - "8012:8012"  # Add this line
```

Then access: `http://your-server-ip:8012/h2-console`
- JDBC URL: `jdbc:h2:mem:lego_factory_auth`
- Username: `sa`
- Password: `password`

Check if USER table exists and has data:
```sql
SELECT * FROM USERS;
SELECT * FROM WORKSTATION;
```

## 7. Network Issues

### Verify Docker network exists:
```bash
docker network ls | grep lego
docker network inspect lego-network
```

### Test inter-service communication:
```bash
# From api-gateway, ping user-service
docker exec lego-sample-factory-api-gateway-1 ping -c 2 user-service
```

## 8. Firewall Issues

### Check if UFW is blocking internal Docker traffic:
```bash
sudo ufw status
```

If active, ensure Docker ports are allowed:
```bash
sudo ufw allow 80/tcp
sudo ufw allow from 172.0.0.0/8 to any  # Allow Docker internal network
```

## 9. Common Solutions

### Solution 1: Complete Restart
```bash
# Stop all services
docker-compose down -v

# Ensure .env file exists with all variables
cat .env  # Verify contents

# Rebuild and restart
docker-compose build --no-cache
docker-compose up -d

# Watch logs
docker-compose logs -f
```

### Solution 2: Check JWT Secret
Ensure `SECURITY_JWT_SECRET` in `.env` is at least 32 characters:
```env
SECURITY_JWT_SECRET=this-is-a-very-secure-jwt-secret-key-minimum-32-characters
```

### Solution 3: Database Reset
If data seems corrupted:
```bash
docker-compose down -v  # -v removes volumes
docker-compose up -d
```

### Solution 4: Check Container Resource Limits
```bash
docker stats
```

Ensure containers have sufficient memory (each service needs ~300-500MB).

## 10. Verify Frontend Configuration

### Check frontend API configuration:
```bash
docker exec lego-sample-factory-frontend-1 cat /usr/share/nginx/html/assets/index-*.js | grep -o "api" | head -5
```

The frontend should make requests to `/api/*` (relative URLs).

### Check nginx root proxy configuration:
```bash
docker exec lego-sample-factory-nginx-root-proxy-1 cat /etc/nginx/nginx.conf
```

Verify it forwards `/api/*` requests to api-gateway.

## Expected Default Users

After successful deployment, these users should exist:

| Username | Password | Role | Workstation |
|----------|----------|------|-------------|
| lego_admin | password | ADMIN | N/A |
| plant_warehouse_user | password | PLANT_WAREHOUSE | WH-01 |
| modules_sm_user | password | MODULES_SUPERMARKET | SM-01 |
| production_planning_user | password | PRODUCTION_PLANNING | PP-01 |
| production_control_user | password | PRODUCTION_CONTROL | PC-01 |
| assembly_control_user | password | ASSEMBLY_CONTROL | AC-01 |

## Quick Diagnostic Script

Save this as `diagnose.sh`:

```bash
#!/bin/bash
echo "=== LEGO Factory Diagnostics ==="
echo ""
echo "1. Checking .env file..."
if [ -f .env ]; then
    echo "✅ .env file exists"
    grep -E "(JWT_SECRET|PORT)" .env
else
    echo "❌ .env file NOT FOUND - CREATE IT FIRST!"
fi

echo ""
echo "2. Checking port 80..."
sudo netstat -tulpn | grep :80

echo ""
echo "3. Checking Docker services..."
docker-compose ps

echo ""
echo "4. Checking API Gateway health..."
curl -s http://localhost:80/api/actuator/health || echo "❌ Gateway not responding"

echo ""
echo "5. Testing login endpoint..."
curl -s -X POST http://localhost:80/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"lego_admin","password":"password"}' | head -c 200

echo ""
echo "=== End Diagnostics ==="
```

Run with:
```bash
chmod +x diagnose.sh
./diagnose.sh
```

## Getting Help

If issues persist, provide these outputs:
1. `docker-compose ps`
2. `docker-compose logs user-service | tail -200`
3. `docker-compose logs api-gateway | tail -200`
4. `cat .env` (redact JWT secret)
5. Output from the diagnostic script above
