# Configuration Manifest

Central documentation for all environment variables and configuration settings in the LEGO Sample Factory application.

## Docker Deployment Status

**Status**: ✅ PRODUCTION READY

- All 8 services containerized (nginx-root-proxy, frontend, api-gateway, 5 backend services)
- Multi-stage Docker builds with health checks
- Docker Compose orchestration with service discovery
- Nginx root proxy as entry point (Port 80)
- Internal container networking (lego-network)
- H2 in-memory databases for all microservices

## Configuration Variables

### Docker & Networking
| Variable | Value | Description |
|----------|-------|-------------|
| `NGINX_ROOT_PROXY_EXTERNAL_PORT` | `80` | Host port for nginx root proxy |
| `FRONTEND_INTERNAL_PORT` | `80` | Frontend nginx container port |
| `DOCKER_NETWORK_NAME` | `lego-network` | Docker Compose network name |

### Authentication & Security
| Variable | Value | Description |
|----------|-------|-------------|
| `SECURITY_JWT_SECRET` | `MySecretKeyForJWT...` | JWT signing secret (min 32 chars) |
| `SECURITY_JWT_EXPIRATION` | `PT1H` | Token expiration (ISO 8601) |

### Microservice Ports (Internal)
| Service | Port | Purpose |
|---------|------|---------|
| `API_GATEWAY_PORT` | `8011` | API Gateway |
| `USER_SERVICE_PORT` | `8012` | Authentication & users |
| `MASTERDATA_SERVICE_PORT` | `8013` | Master data |
| `INVENTORY_SERVICE_PORT` | `8014` | Inventory management |
| `ORDER_PROCESSING_SERVICE_PORT` | `8015` | Order processing |
| `SIMAL_INTEGRATION_SERVICE_PORT` | `8016` | Production scheduling |

### Database Configuration (H2 In-Memory)
| Service | Database Name | JDBC URL |
|---------|---------------|----------|
| User Service | `lego_factory_auth` | `jdbc:h2:mem:lego_factory_auth;DB_CLOSE_DELAY=-1` |
| Masterdata | `masterdata_db` | `jdbc:h2:mem:masterdata_db;DB_CLOSE_DELAY=-1` |
| Inventory | `inventory_db` | `jdbc:h2:mem:inventory_db;DB_CLOSE_DELAY=-1` |
| Order Processing | `orders_db` | `jdbc:h2:mem:orders_db;DB_CLOSE_DELAY=-1` |
| SimAL Integration | `simal_db` | `jdbc:h2:mem:simal_db;DB_CLOSE_DELAY=-1` |

**Database Credentials**:
- Username: `sa`
- Password: `password`

### Spring Boot Configuration
| Variable | Value | Description |
|----------|-------|-------------|
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `create-drop` | Hibernate DDL mode |
| `SPRING_JPA_SHOW_SQL` | `true` | SQL logging |
| `SPRING_H2_CONSOLE_ENABLED` | `true` | H2 web console |
| `SPRING_H2_CONSOLE_PATH` | `/h2-console` | Console path |
| `SPRING_JMX_ENABLED` | `false` | Disable JMX |

### Logging Configuration
| Variable | Value | Description |
|----------|-------|-------------|
| `LOGGING_LEVEL_IO_LIFE` | `DEBUG` | Application logging |
| `LOG_LEVEL_ROOT` | `INFO` | Root logging |

### Management & Monitoring
| Variable | Value | Description |
|----------|-------|-------------|
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info,metrics` | Exposed endpoints |
| `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | `always` | Health details |

### Frontend Configuration (Development)
| Variable | Value | Description |
|----------|-------|-------------|
| `VITE_API_GATEWAY_URL` | `http://localhost:8011` | API Gateway URL |
| `FRONTEND_SERVE_PORT` | `5173` | Development server port |

### API Gateway Configuration
| Variable | Value | Description |
|----------|-------|-------------|
| `API_GATEWAY_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:80,http://localhost,http://localhost:5173` | CORS origins |

### Internal Docker Service URLs
Used for inter-service communication within Docker network:

| Service | Internal URL |
|---------|-------------|
| User Service | `http://user-service:8012` |
| Masterdata Service | `http://masterdata-service:8013` |
| Inventory Service | `http://inventory-service:8014` |
| Order Processing | `http://order-processing-service:8015` |
| SimAL Integration | `http://simal-integration-service:8016` |

## Deployment Information

### Docker Production Deployment
- **Status**: ✅ Operational
- **Access**: http://localhost
- **Startup**: `./start-factory.ps1` or `docker-compose up -d`
- **Services**: 8 containers with health checks
- **Database**: H2 in-memory per service
- **Network**: Internal Docker network (lego-network)

### Environment Setup
1. Clone repository
2. Ensure Docker Desktop is running
3. Run `docker-compose up -d`
4. Wait 2-3 minutes for services to initialize
5. Access application at http://localhost

### Configuration Files
- `docker-compose.yml`: Service orchestration
- `.env`: Environment variables (git-ignored)
- `config_manifest.md`: Configuration documentation

## Validation Rules

| Setting | Rule |
|---------|------|
| JWT_SECRET | Minimum 32 characters |
| Service Ports | Must be unique (8011-8016) |
| Database URLs | Must include H2 parameters |
| Duration Values | ISO 8601 format (PT1H, PT30M) |
| Boolean Values | `true` or `false` |
| URLs | Must include protocol (http:// or https://) |

## Security Considerations

| Component | Development | Production Recommendation |
|-----------|-------------|---------------------------|
| JWT_SECRET | Fixed value | Generate random, secure secret |
| Database | H2 in-memory | Consider persistent database |
| H2 Console | Enabled | **Disable** for production |
| CORS Origins | Localhost only | Restrict to known domains |
| User Passwords | All use `password` | Enforce strong passwords |

## Troubleshooting Configuration

**Port conflicts**: Check `NGINX_ROOT_PROXY_EXTERNAL_PORT` availability

**Database connection**: Verify `H2_DB_*_PATH` JDBC URLs in service logs

**Service communication**: Ensure Docker service names match in API Gateway configuration

**Authentication**: Verify `SECURITY_JWT_SECRET` is at least 32 characters

**Frontend API calls**: Check `VITE_API_GATEWAY_URL` or nginx proxy routing

See [README.md](README.md) for detailed troubleshooting steps.