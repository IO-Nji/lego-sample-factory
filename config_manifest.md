# Configuration Manifest

This document serves as the central, human-readable documentation for all environment variables and configuration settings used across the LEGO Sample Factory application.

## Global Configuration Variables

### Docker & Networking Configuration
- `NGINX_ROOT_PROXY_EXTERNAL_PORT=80` - The host port for the root Nginx proxy
- `FRONTEND_INTERNAL_PORT=80` - The port the Nginx inside the frontend container listens on
- `FRONTEND_SERVE_PORT=80` - Port for frontend development server

### Authentication & Security
- `SECURITY_JWT_SECRET=MySecretKeyForJWTTokenGeneration2024AtLeast32Characters` - JWT signing secret (Secret) - minimum 32 characters
- `SECURITY_JWT_EXPIRATION=PT1H` - JWT token expiration time (ISO 8601 duration format)

### Microservice Ports
- `USER_SERVICE_PORT=8012` - User authentication and authorization service port
- `MASTERDATA_SERVICE_PORT=8013` - Master data management service port  
- `INVENTORY_SERVICE_PORT=8014` - Inventory and stock management service port
- `ORDER_PROCESSING_SERVICE_PORT=8015` - Order processing and fulfillment service port
- `SIMAL_INTEGRATION_SERVICE_PORT=8016` - SIMAL integration service port
- `API_GATEWAY_PORT=8011` - API Gateway service port

### Database Configuration (H2 In-Memory for Development)
- `H2_DB_USER_PATH=jdbc:h2:mem:lego_factory_auth;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` - User service H2 database URL
- `H2_DB_MASTERDATA_PATH=jdbc:h2:mem:masterdata_db;DB_CLOSE_DELAY=-1` - Master data service H2 database URL
- `H2_DB_INVENTORY_PATH=jdbc:h2:mem:inventory_db;DB_CLOSE_DELAY=-1` - Inventory service H2 database URL
- `H2_DB_ORDER_PROCESSING_PATH=jdbc:h2:mem:orders_db;DB_CLOSE_DELAY=-1` - Order processing service H2 database URL
- `H2_DB_SIMAL_INTEGRATION_PATH=jdbc:h2:mem:simal_db;DB_CLOSE_DELAY=-1` - SIMAL integration service H2 database URL

### Spring Boot Common Configuration
- `SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop` - Hibernate DDL auto mode for development
- `SPRING_JPA_SHOW_SQL=true` - Enable SQL query logging for debugging
- `SPRING_H2_CONSOLE_ENABLED=true` - Enable H2 web console for database access
- `SPRING_H2_CONSOLE_PATH=/h2-console` - Path for H2 console access
- `SPRING_JMX_ENABLED=false` - Disable JMX to prevent connection issues

### Logging Configuration
- `LOGGING_LEVEL_IO_LIFE=DEBUG` - Application-specific logging level
- `LOG_LEVEL_ROOT=INFO` - Root logging level

### Management & Monitoring
- `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics` - Exposed actuator endpoints
- `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always` - Health endpoint detail level

### Frontend Configuration
- `VITE_API_GATEWAY_URL=http://localhost:8011` - API Gateway URL for frontend

### API Gateway Configuration
- `API_GATEWAY_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:80,http://localhost,http://localhost:5173` - CORS allowed origins

## Development vs Production Notes

### Development Environment
- Uses H2 in-memory databases for all services
- H2 console enabled for database inspection
- Enhanced logging for debugging
- CORS configured for local development ports

### Docker-Specific Variables (to be added during dockerization)
- `DOCKER_RESTART_POLICY=unless-stopped` - Container restart policy
- `DOCKER_MEMORY_LIMIT=512m` - Memory limit per container
- `DOCKER_CPU_LIMIT=1.0` - CPU limit per container

## Variable Validation Rules

1. **JWT_SECRET**: Must be at least 32 characters long
2. **Ports**: Must be unique across all services
3. **Database URLs**: Must include proper H2 configuration parameters
4. **Duration Values**: Must follow ISO 8601 duration format (PT1H, PT30M, etc.)
5. **Boolean Values**: Must be 'true' or 'false'
6. **URLs**: Must include protocol (http:// or https://)

## Security Considerations

- **JWT_SECRET**: Should be generated randomly and kept secure
- **Database passwords**: Use strong passwords in production
- **CORS origins**: Restrict to known domains in production
- **H2 console**: Should be disabled in production environments