# LEGO Sample Factory Control System

A comprehensive **microservice-based manufacturing control platform** that digitizes and automates the supply chain operations of the LEGO Sample Factory. The system replaces manual, paper-heavy processes with intelligent digital workflows that manage production orders, inventory, workstation assignments, and real-time operational dashboards.

## Overview

LIFE (LEGO Integrated Factory Execution) is a production-ready prototype designed to:

- **Manage multi-stage manufacturing**: From order creation through modules assembly to final product delivery
- **Track inventory in real-time**: Stock management across multiple workstations and warehouse locations
- **Optimize production scheduling**: Intelligent workstation assignment and task sequencing
- **Enable role-based operations**: Specialized dashboards for plant warehouse, modules supermarket, and manufacturing teams
- **Provide system observability**: Comprehensive error handling, structured logging, and performance monitoring

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

**🗄️ Database Philosophy**: 
The application uses **H2 in-memory databases exclusively** for development and testing. This eliminates the need for external database setup, making it ideal for rapid development, demos, and CI/CD environments. Each microservice maintains its own isolated H2 database that initializes automatically on startup.

## Recent Updates (December 2025)

### ✅ Docker Implementation Complete
- **Full containerization**: All 8 services (nginx proxy, frontend, 6 backend services) containerized
- **Production-ready deployment**: Multi-stage builds, health checks, service discovery
- **Simplified startup**: Single command `./start-factory.ps1` or `docker-compose up -d`
- **Nginx proxy**: Entry point on port 80 with automatic API routing
- **Container networking**: Internal service communication without port conflicts

### ✅ Configuration Improvements
- **Simplified database setup**: Pure H2 in-memory databases eliminate external dependencies
- **Environment variable support**: Centralized configuration via `.env` file  
- **JMX disabled**: Prevents connection issues in development environment
- **Java-based user initialization**: Robust UserInitializer service creates all users automatically
- **Authentication system resolved**: Fixed SQL/Java initialization conflicts for reliable login
- **Development-first approach**: No PostgreSQL setup required for development and testing

### ✅ Enhanced Build Process
- **Individual service management**: Each service runs independently using Maven wrapper
- **Service dependency management**: Proper startup order enforcement
- **Clean build process**: Integrated Maven clean and package commands

### ✅ Simplified User Management  
- **Automatic user initialization**: UserInitializer service creates all users at startup
- **Standardized passwords**: All test users use `password` for development ease
- **Role-based access**: Complete UserRole enum mapping with workstation assignments
- **Authentication reliability**: Resolved initialization conflicts for consistent login experience

## Current Features & Status

### ✅ Implemented Features

#### Authentication & Authorization
- JWT-based authentication for all API requests
- Role-based access control (ADMIN, PLANT_WAREHOUSE, MODULES_SUPERMARKET, MANUFACTURING, etc.)
- User management dashboard (create, update, delete, assign workstations)
- Automatic session management with localStorage

#### Product & Inventory Management
- Product variants catalog with pricing and production time estimates
- Modular component structure (products → modules → parts)
- Real-time inventory tracking by workstation
- Stock record management with item type classification

#### Order Processing & Fulfillment
- Customer order creation with multiple order items
- Order status lifecycle (PENDING → CONFIRMED → PROCESSING → COMPLETED/CANCELLED)
- Warehouse order management for inter-warehouse transfers
- Fulfill/reject operations with automatic inventory adjustments

#### Workstation Operations
- Multi-role workstation dashboards:
  - **Admin Dashboard**: System-wide KPIs, user management, workstation configuration
  - **Plant Warehouse**: Incoming customer orders, fulfillment actions
  - **Modules Supermarket**: Warehouse request handling, inventory fulfillment
  - **Manufacturing Workstations**: Task execution pages for manufacturing and assembly
- Real-time order/task updates with 15-30 second auto-refresh

#### Production Scheduling (SimAL)
- Intelligent workstation allocation based on work type
- Task sequencing with ISO 8601 timestamps
- Order-to-schedule linking with realistic time estimates

#### Error Handling & Observability
- Global exception handlers with standardized JSON error responses
- Structured logging with proper log levels and package-specific configuration
- Frontend toast notifications for user-facing error feedback
- Health check endpoints for all services

## Setup & Running the Application

### Prerequisites
- **Docker Desktop** (with Docker Compose)
- **Git** for cloning the repository
- **Web Browser** for accessing the application

### 🚀 Quick Start (Docker - Recommended)

1. **Clone and navigate to project**:
   ```powershell
   cd "e:\My Documents\DEV\Arduino\libraries\lego-sample-factory"
   ```

2. **Start the entire application stack**:
   ```powershell
   docker-compose up -d
   ```
   ✅ **Wait for**: All containers to be healthy (about 2-3 minutes)

3. **Access the application**:
   - **Frontend**: `http://localhost` (or `http://localhost:80`)
   - **API Gateway**: `http://localhost/api/` (proxied through nginx)

### 📋 Application Management Scripts

**Start all services**:
```powershell
.\start-factory.ps1
```

**Stop all services**:
```powershell
docker-compose down
```

**View service logs**:
```powershell
docker-compose logs -f [service-name]
# Examples:
docker-compose logs -f api-gateway
docker-compose logs -f user-service
docker-compose logs -f frontend
```

**Rebuild specific service**:
```powershell
docker-compose build --no-cache [service-name]
docker-compose up -d [service-name]
```

### 🔧 Development Mode (Manual Services)

For active development, you may want to run services manually:

**Prerequisites for manual mode**:
- **Java 21** (Eclipse Adoptium or equivalent)
- **Node.js 18+** and npm

**Start backend services individually**:
```powershell
cd lego-factory-backend\user-service
.\mvnw.cmd spring-boot:run
```

**Start frontend in development mode**:
```powershell
cd lego-factory-frontend
npm install  # First time only
npm run dev  # Available at http://localhost:5173
```

## Database Architecture (H2-Only)

The application uses **H2 in-memory databases exclusively** for simplified development:

### 🎯 **Development Benefits**
- **Zero setup**: No external database installation required
- **Instant startup**: Services start immediately without waiting for database connections
- **Isolated testing**: Each service has its own database, preventing data conflicts
- **Demo-ready**: Perfect for presentations and demonstrations
- **CI/CD friendly**: No complex database provisioning in pipelines

### 📊 **Database Distribution**
- **User Service** (Port 8012): `jdbc:h2:mem:lego_factory_auth` - Authentication & user management
- **Masterdata Service** (Port 8013): `jdbc:h2:mem:masterdata_db` - Product catalog & configurations  
- **Inventory Service** (Port 8014): `jdbc:h2:mem:inventory_db` - Stock records & workstation inventory
- **Order Processing** (Port 8015): `jdbc:h2:mem:order_processing_db` - Customer orders & fulfillment
- **SimAL Integration** (Port 8016): `jdbc:h2:mem:simal_db` - Production scheduling & simulation

### 🔍 **Database Console Access**
H2 console access is available when services are exposed (development mode):
- **User Service**: `http://localhost:8012/h2-console` (if port exposed)
- **JDBC URLs**: As defined in config_manifest.md
- **Username**: `sa`
- **Password**: `password`

**Note**: In Docker mode, database consoles are internal to containers. Use `docker exec` to access:
```powershell
docker exec -it lego-sample-factory-user-service-1 curl http://localhost:8012/h2-console
```

### Simplified Development Approach

**For basic development and testing**, you only need:

1. **User Service** (Port 8012) - Provides authentication and user management
2. **Frontend** (Port 5173) - React application

The frontend will gracefully handle missing services and show appropriate messages for unavailable features.

### Service Architecture & Ports

```
┌─────────────────────────────────────┐
│  React Frontend (Port 5173)         │
└─────────────┬───────────────────────┘
              │
    ┌─────────▼──────────┐
    │ API Gateway (8011) │ (Optional)
    └─────────┬──────────┘
              │
  ┌───────────▼───────────────────────────────┐
  │            Core Services                   │
  │  ┌─────────┐ ┌──────────┐ ┌─────────────┐ │
  │  │  User   │ │Masterdata│ │  Inventory  │ │
  │  │  8012   │ │   8013   │ │    8014     │ │
  │  └─────────┘ └──────────┘ └─────────────┘ │
  │  ┌─────────┐ ┌──────────┐                 │
  │  │  Order  │ │  SimAL   │                 │
  │  │  8015   │ │   8016   │                 │
  │  └─────────┘ └──────────┘                 │
  └───────────────────────────────────────────┘
```

6. **API Gateway** (Port 8011) - Request routing (Optional for development):
   ```powershell
   cd api-gateway
   .\mvnw.cmd spring-boot:run
   ```

### Authentication Testing

Once the User Service is running, you can test authentication:

```powershell
# Test admin login
curl -X POST http://localhost:8012/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"lego_admin\",\"password\":\"password\"}"

# Expected response: JWT token and user details
```

### Frontend Application
```powershell
cd lego-factory-frontend
npm install  # First time only
npm run dev
```

**Access URL**: `http://localhost:5173`

## Default Test Accounts

### Updated User Credentials (December 2025)

**🔑 Universal Password**: All users use `password` for simplified testing and development.

**Admin Account**:
- **Username**: `lego_admin`
- **Password**: `password`
- **Role**: ADMIN
- **Access**: Full system access, user management, workstation configuration

**Warehouse Operations**:
- **Username**: `warehouse_operator`
- **Password**: `password`
- **Role**: PLANT_WAREHOUSE
- **Workstation**: 7
- **Access**: Customer order fulfillment, plant warehouse operations

- **Username**: `modules_supermarket`
- **Password**: `password`
- **Role**: MODULES_SUPERMARKET
- **Workstation**: 8
- **Access**: Warehouse order management, module inventory fulfillment

- **Username**: `parts_supply_warehouse`
- **Password**: `password`
- **Role**: PARTS_SUPPLY
- **Workstation**: 9
- **Access**: Parts supply warehouse operations

**Production Control**:
- **Username**: `production_planning`
- **Password**: `password`
- **Role**: PRODUCTION_PLANNING
- **Access**: Production scheduling and planning

- **Username**: `production_control`
- **Password**: `password`
- **Role**: PRODUCTION_CONTROL
- **Workstation**: 1

**Manufacturing Workstations**:
- **Username**: `injection_molding`
- **Password**: `password`
- **Role**: MANUFACTURING
- **Workstation**: 1

- **Username**: `parts_preproduction`
- **Password**: `password`
- **Role**: MANUFACTURING
- **Workstation**: 2

- **Username**: `part_finishing`
- **Password**: `password`
- **Role**: MANUFACTURING
- **Workstation**: 3

**Assembly Operations**:
- **Username**: `assembly_control`
- **Password**: `password`
- **Role**: ASSEMBLY_CONTROL
- **Workstation**: 4

- **Username**: `gear_assembly`
- **Password**: `password`
- **Role**: ASSEMBLY_CONTROL
- **Workstation**: 4

- **Username**: `motor_assembly`
- **Password**: `password`
- **Role**: ASSEMBLY_CONTROL
- **Workstation**: 5

- **Username**: `final_assembly`
- **Password**: `password`
- **Role**: ASSEMBLY_CONTROL
- **Workstation**: 6

**Read-Only Access**:
- **Username**: `viewer_user`
- **Password**: `password`
- **Role**: VIEWER
- **Access**: Read-only system monitoring and reports

## API Endpoints Summary

All endpoints are routed through nginx proxy and API Gateway:
- **Docker mode**: `http://localhost/api/...`
- **Development mode**: `http://localhost:8011/...`

**Authentication**: `POST /api/auth/login` — Submit username/password, receive JWT token

**User Management** (Admin-only):
- `GET /api/users` — List all users
- `POST /api/users` — Create new user
- `PUT /api/users/{id}` — Update user
- `DELETE /api/users/{id}` — Delete user

**Master Data** (All authenticated users):
- `GET /api/masterdata/product-variants` — Product catalog
- `GET /api/masterdata/modules` — Manufacturing modules
- `GET /api/masterdata/parts` — Component parts
- `GET /api/masterdata/workstations` — Workstation configuration

**Inventory Management**:
- `GET /api/stock/records` — All stock records
- `GET /api/stock/by-workstation/{workstationId}` — Workstation inventory
- `PUT /api/stock/records/{id}` — Update stock

**Order Processing** (Plant Warehouse role):
- `POST /api/customer-orders` — Create order
- `GET /api/customer-orders` — List orders
- `PATCH /api/customer-orders/{id}/status` — Update status

**Warehouse Orders** (Modules Supermarket role):
- `GET /api/warehouse-orders` — Pending orders
- `POST /api/warehouse-orders/{id}/fulfill` — Fulfill order
- `POST /api/warehouse-orders/{id}/reject` — Reject order

**Production Scheduling**:
- `POST /api/simal/production-order` — Submit production order
- `GET /api/simal/scheduled-orders` — View schedules

## Health Checks & Monitoring

**Docker mode health checks**:
```powershell
# Check all container status
docker-compose ps

# View container health
docker ps

# Check specific service health
curl http://localhost/api/actuator/health
```

**Development mode health checks**:
- `http://localhost:801X/actuator/health` — Service health status
- `http://localhost:801X/actuator/info` — Service information

## Troubleshooting

### Common Docker Issues

**Port conflicts**: 
```powershell
# Check if port 80 is in use
netstat -ano | findstr :80
# Stop other services using port 80
docker-compose down
```

**Container startup issues**:
```powershell
# Check container logs
docker-compose logs [service-name]

# Restart specific service
docker-compose restart [service-name]

# Force rebuild and restart
docker-compose build --no-cache [service-name]
docker-compose up -d [service-name]
```

**Memory/Performance issues**:
```powershell
# Check container resource usage
docker stats

# Clean up unused containers and images
docker system prune -f
```

### Docker Commands

**Complete system restart**:
```powershell
docker-compose down -v  # Remove volumes
docker-compose up -d    # Fresh start
```

**View service logs**:
```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f user-service
```

**Access container shell**:
```powershell
docker exec -it lego-sample-factory-user-service-1 /bin/bash
```