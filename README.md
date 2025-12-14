# LEGO Sample Factory Control System

A **microservice-based manufacturing control platform** that digitizes and automates supply chain operations for the LEGO Sample Factory. The system provides intelligent digital workflows for production order management, real-time inventory tracking, workstation assignments, and role-based operational dashboards.

## Overview

LIFE (LEGO Integrated Factory Execution) manages:

- **Multi-stage manufacturing**: Order creation through assembly to final product delivery
- **Real-time inventory tracking**: Stock management across workstations and warehouse locations
- **Production scheduling**: Intelligent workstation assignment and task sequencing
- **Role-based operations**: Specialized dashboards for warehouse, supermarket, and manufacturing teams
- **System observability**: Comprehensive error handling, structured logging, and health monitoring

## System Architecture

LIFE uses a **Docker containerized microservice architecture** with nginx proxy as the entry point:

```plaintext
┌─────────────────────────────────────────────────────────────┐
│              Nginx Root Proxy (Port 80)                      │
│                     Entry Point                              │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────▼─────────────────────────┐
        │         React Frontend                  │
        │    (Containerized with Nginx)           │
        │  ┌─────────────┬─────────────────────┐  │
        │  │ Dashboard   │ Workstation Pages   │  │
        │  │ (All roles) │ (Plant WH, Modules) │  │
        │  └─────────────┴─────────────────────┘  │
        └─────────────────┬───────────────────────┘
                          │ /api/* requests
        ┌─────────────────▼───────────────────────┐
        │        API Gateway                      │
        │   - Route all API requests              │
        │   - JWT authentication                  │
        │   - CORS support                        │
        └───┬──────┬──────┬───────┬────┬─────────┘
            │      │      │       │    │
       ┌────▼──┐ ┌─▼────┐┌──▼──┐┌─▼──┐┌──▼──┐
       │ User  │ │Master││Inven-││Order│ │Simal│
       │Service│ │data  ││tory  ││Proc.│ │Integ│
       │(H2 DB)│ │(H2DB)││(H2DB)││(H2) │ │(H2) │
       └───────┘ └──────┘└─────┘└────┘└─────┘
```

**Backend Services**:

- **User Service** (Port 8012): JWT authentication, authorization, user management
- **Masterdata Service** (Port 8013): Product catalog, modules, parts, workstations
- **Inventory Service** (Port 8014): Stock tracking and workstation inventory
- **Order Processing Service** (Port 8015): Customer orders, fulfillment, warehouse operations
- **SimAL Integration Service** (Port 8016): Production scheduling and simulation
- **API Gateway** (Port 8011): Centralized routing, CORS, JWT validation

**Persistence**: Each microservice maintains its own H2 in-memory database for complete data isolation.

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.4.2, Spring Cloud Gateway 2024.0.0, Spring Security, Maven
- **Database**: H2 in-memory databases (one per service) for simplified development
- **Frontend**: React 18+, Vite, Axios, React Router
- **Containerization**: Docker, Docker Compose, multi-stage builds
- **Proxy**: Nginx (root proxy + frontend serving)
- **Configuration**: Environment variables via .env file, Spring profiles
- **Tools**: Visual Studio Code, Docker Desktop, PowerShell scripts

**Database Architecture**: H2 in-memory databases provide zero-setup development with complete microservice isolation.

## Key Features

### Authentication & Authorization
- JWT-based authentication with role-based access control
- User management dashboard (create, update, delete, assign workstations)
- Automatic user initialization with default credentials

### Product & Inventory Management
- Product variants catalog with pricing and production time estimates
- Modular component structure (products → modules → parts)
- Real-time inventory tracking by workstation

### Order Processing & Fulfillment
- Customer order creation and lifecycle management (PENDING → CONFIRMED → PROCESSING → COMPLETED/CANCELLED)
- Warehouse order management with automatic inventory adjustments
- Fulfill/reject operations for inter-warehouse transfers

### Workstation Operations
- **Admin Dashboard**: System-wide KPIs, user management, workstation configuration
- **Plant Warehouse**: Customer order fulfillment
- **Modules Supermarket**: Warehouse request handling, inventory fulfillment
- **Manufacturing Workstations**: Task execution for manufacturing and assembly
- Real-time updates with 15-30 second auto-refresh

### Production Scheduling
- Intelligent workstation allocation based on work type
- Task sequencing with ISO 8601 timestamps
- Order-to-schedule linking

### Observability
- Global exception handlers with standardized JSON error responses
- Structured logging with package-specific configuration
- Health check endpoints for all services

## Deployment & Configuration

### Prerequisites
- **Docker Desktop** (version 20.10+ with Docker Compose)
- **Git** for repository management
- **Web Browser** (Chrome, Firefox, Edge recommended)
- **Windows PowerShell** (for scripts)

### Quick Start with Docker (Recommended)

**1. Clone the repository**:
```powershell
git clone https://github.com/IO-Nji/lego-sample-factory.git
cd lego-sample-factory
```

**2. Start all services**:
```powershell
# Using PowerShell script
.\start-factory.ps1

# OR using docker-compose directly
docker-compose up -d
```

**3. Wait for services to be ready** (2-3 minutes):
```powershell
# Check service health
docker-compose ps
```

**4. Access the application**:
- **Application**: http://localhost
- **API Gateway**: http://localhost/api/

**5. Login with default credentials**:
- **Username**: `lego_admin`
- **Password**: `password`

### Environment Configuration

The application uses environment variables defined in [config_manifest.md](config_manifest.md). Key configurations:

**Service Ports** (internal to Docker):
- API Gateway: 8011
- User Service: 8012
- Masterdata Service: 8013
- Inventory Service: 8014
- Order Processing: 8015
- SimAL Integration: 8016

**External Access**:
- Nginx Root Proxy: Port 80 (http://localhost)

**Database Configuration**:
Each service uses its own H2 in-memory database. See [Database Architecture](#database-architecture) for details.

**Security**:
- JWT Secret: Configured in `.env` file (minimum 32 characters)
- Token Expiration: 1 hour (PT1H)
- CORS: Configured for localhost origins

### Service Management

**Stop all services**:
```powershell
docker-compose down
```

**Stop and remove volumes** (fresh database):
```powershell
docker-compose down -v
```

**View logs** (all services):
```powershell
docker-compose logs -f
```

**View logs** (specific service):
```powershell
docker-compose logs -f api-gateway
docker-compose logs -f user-service
docker-compose logs -f frontend
```

**Restart specific service**:
```powershell
docker-compose restart [service-name]
```

**Rebuild service** (after code changes):
```powershell
docker-compose build --no-cache [service-name]
docker-compose up -d [service-name]
```

**Check container status**:
```powershell
# All containers
docker-compose ps

# Detailed status
docker ps

# Resource usage
docker stats
```

### Development Mode (Optional)

For local development without Docker:

**Prerequisites**:
- Java 21 (Eclipse Adoptium or equivalent)
- Node.js 18+ with npm
- Maven (included via mvnw wrapper)

**Start backend services**:
```powershell
# Example: User Service
cd lego-factory-backend\user-service
.\mvnw.cmd spring-boot:run

# Repeat for other services as needed
```

**Start frontend**:
```powershell
cd lego-factory-frontend
npm install
npm run dev  # Available at http://localhost:5173
```

**Note**: In development mode, services run on their configured ports (8011-8016) without nginx proxy.

## Database Architecture

The application uses **H2 in-memory databases** exclusively, providing:

- **Zero setup**: No external database installation required
- **Instant startup**: Services start immediately without database connections
- **Isolated testing**: Each service has its own database
- **CI/CD friendly**: No complex database provisioning

### Database Distribution

Each microservice maintains its own H2 database:

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| User Service | 8012 | `lego_factory_auth` | Authentication & users |
| Masterdata Service | 8013 | `masterdata_db` | Product catalog & configuration |
| Inventory Service | 8014 | `inventory_db` | Stock records & workstations |
| Order Processing | 8015 | `order_processing_db` | Customer orders & fulfillment |
| SimAL Integration | 8016 | `simal_db` | Production scheduling |

### H2 Console Access

**In development mode** (services running locally):
- User Service: http://localhost:8012/h2-console
- **JDBC URL**: `jdbc:h2:mem:lego_factory_auth`
- **Username**: `sa`
- **Password**: `password`

**In Docker mode**, access via container:
```powershell
docker exec -it lego-sample-factory-user-service-1 curl http://localhost:8012/h2-console
```

**Note**: H2 console should be disabled in production environments.

## Default User Accounts

All users use **password** as the default password.

### Administrative Access
| Username | Role | Access |
|----------|------|--------|
| `lego_admin` | ADMIN | Full system access, user management, configuration |

### Warehouse Operations
| Username | Role | Workstation | Access |
|----------|------|-------------|--------|
| `warehouse_operator` | PLANT_WAREHOUSE | 7 | Customer order fulfillment |
| `modules_supermarket` | MODULES_SUPERMARKET | 8 | Module inventory fulfillment |
| `parts_supply_warehouse` | PARTS_SUPPLY | 9 | Parts supply operations |

### Production Control
| Username | Role | Workstation | Access |
|----------|------|-------------|--------|
| `production_planning` | PRODUCTION_PLANNING | - | Production scheduling |
| `production_control` | PRODUCTION_CONTROL | 1 | Production oversight |

### Manufacturing Workstations
| Username | Role | Workstation | Operations |
|----------|------|-------------|-----------|
| `injection_molding` | MANUFACTURING | 1 | Injection molding |
| `parts_preproduction` | MANUFACTURING | 2 | Parts pre-production |
| `part_finishing` | MANUFACTURING | 3 | Part finishing |

### Assembly Operations
| Username | Role | Workstation | Operations |
|----------|------|-------------|-----------|
| `assembly_control` | ASSEMBLY_CONTROL | 4 | Assembly oversight |
| `gear_assembly` | ASSEMBLY_CONTROL | 4 | Gear assembly |
| `motor_assembly` | ASSEMBLY_CONTROL | 5 | Motor assembly |
| `final_assembly` | ASSEMBLY_CONTROL | 6 | Final assembly |

### Read-Only Access
| Username | Role | Access |
|----------|------|--------|
| `viewer_user` | VIEWER | Read-only monitoring |

## API Reference

All API requests are routed through the nginx proxy and API Gateway:
- **Docker mode**: `http://localhost/api/...`
- **Development mode**: `http://localhost:8011/...`

### Authentication
**POST** `/api/auth/login`
- Submit username and password
- Returns JWT token and user details

### User Management (Admin only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | List all users |
| POST | `/api/users` | Create user |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |

### Master Data (Authenticated users)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/masterdata/product-variants` | Product catalog |
| GET | `/api/masterdata/modules` | Manufacturing modules |
| GET | `/api/masterdata/parts` | Component parts |
| GET | `/api/masterdata/workstations` | Workstation config |

### Inventory Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stock/records` | All stock records |
| GET | `/api/stock/by-workstation/{id}` | Workstation inventory |
| PUT | `/api/stock/records/{id}` | Update stock |

### Order Processing (Plant Warehouse)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/customer-orders` | Create order |
| GET | `/api/customer-orders` | List orders |
| PATCH | `/api/customer-orders/{id}/status` | Update status |

### Warehouse Orders (Modules Supermarket)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/warehouse-orders` | Pending orders |
| POST | `/api/warehouse-orders/{id}/fulfill` | Fulfill order |
| POST | `/api/warehouse-orders/{id}/reject` | Reject order |

### Production Scheduling
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/simal/production-order` | Submit order |
| GET | `/api/simal/scheduled-orders` | View schedules |

## Health Monitoring

### Docker Mode

**Check all services**:
```powershell
# Container status
docker-compose ps

# Health status
docker ps

# Resource usage
docker stats
```

**Service health endpoints**:
```powershell
# Via nginx proxy
curl http://localhost/api/actuator/health

# Specific service (requires port exposure)
curl http://localhost:8012/actuator/health
```

### Development Mode

Access health endpoints directly:
- `http://localhost:801X/actuator/health` — Service health
- `http://localhost:801X/actuator/info` — Service information

Replace `801X` with service port (8011-8016).

## Troubleshooting

### Port Conflicts

**Check if port 80 is in use**:
```powershell
netstat -ano | findstr :80
```

**Stop conflicting services**:
```powershell
docker-compose down
```

### Container Issues

**View logs**:
```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f [service-name]

# Last 50 lines with error filtering
docker-compose logs --tail=50 [service-name] | Select-String -Pattern "ERROR|Exception|Failed"
```

**Restart service**:
```powershell
docker-compose restart [service-name]
```

**Force rebuild**:
```powershell
docker-compose build --no-cache [service-name]
docker-compose up -d [service-name]
```

**Complete system reset**:
```powershell
# Remove all containers and volumes
docker-compose down -v

# Clean up Docker system
docker system prune -f

# Restart
docker-compose up -d
```

### Memory/Performance Issues

**Monitor resources**:
```powershell
docker stats
```

**Clean up unused resources**:
```powershell
# Remove unused containers and images
docker system prune -af

# Remove unused volumes
docker volume prune -f
```

### Authentication Issues

**Browser cache**: Clear browser cache or use incognito mode

**JWT token expired**: Login again to get fresh token

**Check user service logs**:
```powershell
docker-compose logs -f user-service
```

### Database Issues

**Reset database** (H2 in-memory):
```powershell
# Stop and restart service
docker-compose restart [service-name]

# Or remove volumes for fresh start
docker-compose down -v
docker-compose up -d
```

**Access H2 console** (development mode):
- URL: `http://localhost:801X/h2-console`
- JDBC URL: See [Database Architecture](#database-architecture)
- Username: `sa`
- Password: `password`

### Network Issues

**Check service connectivity**:
```powershell
# Access container shell
docker exec -it lego-sample-factory-api-gateway-1 /bin/bash

# Test connection to other service
curl http://user-service:8012/actuator/health
```

**Verify Docker network**:
```powershell
docker network inspect lego-sample-factory_lego-network
```

### Frontend Issues

**Browser DevTools**: Check Network tab for failed API requests

**CORS errors**: Verify API Gateway CORS configuration in logs

**Hard refresh**: Use Ctrl+Shift+R or Ctrl+F5 to clear cached resources

**Check nginx logs**:
```powershell
docker-compose logs -f nginx-root-proxy
```

### Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `Port 80 already in use` | Another service using port 80 | Stop conflicting service or change port |
| `Container exited with code 1` | Application startup failure | Check service logs |
| `Connection refused` | Service not ready | Wait for health check, restart service |
| `401 Unauthorized` | JWT token expired/invalid | Login again |
| `500 Internal Server Error` | Backend service error | Check service logs |
| `504 Gateway Timeout` | Service not responding | Check service health, restart if needed |

### Getting Help

**Check logs for errors**:
```powershell
# API Gateway (routing issues)
docker-compose logs -f api-gateway

# User Service (authentication issues)
docker-compose logs -f user-service

# Nginx (request routing)
docker-compose logs -f nginx-root-proxy
```

**Container shell access**:
```powershell
docker exec -it lego-sample-factory-[service-name]-1 /bin/bash
```

**Export logs for debugging**:
```powershell
docker-compose logs > debug-logs.txt
```

## Additional Resources

- **Configuration Reference**: [config_manifest.md](config_manifest.md)
- **Docker Compose**: [docker-compose.yml](docker-compose.yml)
- **Architecture Diagram**: See [System Architecture](#system-architecture)

---

**License**: See [LICENSE](LICENSE) file
**Repository**: https://github.com/IO-Nji/lego-sample-factory