# LEGO Sample Factory Control System

A comprehensive **microservice-based manufacturing control platform** that digitizes and automates the supply chain operations of the LEGO Sample Factory. The system replaces manual, paper-heavy processes with intelligent digital workflows that manage production orders, inventory, workstation assignments, and real-time operational dashboards.

## Overview

LEGO Factory is a production-ready prototype designed to:

- **Manage multi-stage manufacturing**: From order creation through modules assembly to final product delivery
- **Track inventory in real-time**: Stock management across multiple workstations and warehouse locations
- **Optimize production scheduling**: Intelligent workstation assignment and task sequencing
- **Enable role-based operations**: Specialized dashboards for different factory roles
- **Provide system observability**: Comprehensive error handling, structured logging, and performance monitoring

## System Architecture

The system uses a **microservice architecture** with an API Gateway as the central routing layer and complete frontend/backend separation:

```plaintext
┌─────────────────────────────────────────────────────────────┐
│           React Frontend (Port 3000)                         │
│  ┌──────────────┬──────────────┬──────────────────────────┐ │
│  │ Dashboard    │ Products     │ Workstation Pages        │ │
│  │ (Multi-role) │ Catalog      │ (Admin, Manager, etc.)   │ │
│  └──────────────┴──────────────┴──────────────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │ API Gateway Communication
        ┌───────────────────▼────────────────────┐
        │   API Gateway (Port 8080)              │
        │   - Central routing                    │
        │   - CORS handling                      │
        │   - Service discovery                  │
        └───┬──────┬──────┬───────┬────┬────────┘
            │      │      │       │    │
       ┌────▼──┐ ┌─▼────┐┌──▼──┐┌─▼──┐┌──▼──┐
       │ User  │ │Master││Inven-││Order│ │SIMAL│
       │Service│ │data  ││tory ││Proc.│ │Integ│
       │ 8012  │ │ 8013 ││8014 ││ 8015│ │ 8016│
       └───┬───┘ └──┬───┘└──┬──┘└─┬──┘└──┬──┘
           │        │       │      │      │
      ┌────▼────┐   └───────┴──────┴──────┴──────┐
      │PostgreSQL│          H2 Databases         │
      │  Port    │        (File-based per        │
      │  5432    │         service)               │
      └─────────┘                                 │
```

## Project Structure

The project follows a clean microservices architecture with separated concerns:

```
lego-sample-factory/
├── lego-factory-backend/                    # Backend microservices
│   ├── docker-compose.yml                   # Service orchestration
│   ├── .env                                 # Environment configuration
│   ├── api-gateway/                         # Central routing (Port 8080)
│   │   ├── src/main/java/...
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── user-service/                        # Authentication (Port 8012)
│   │   ├── src/main/java/io/life/user_service/
│   │   │   ├── UserServiceApplication.java
│   │   │   ├── service/UserService.java
│   │   │   ├── entity/User.java
│   │   │   ├── repository/UserRepository.java
│   │   │   └── config/SecurityConfig.java
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── masterdata-service/                  # Product data (Port 8013)
│   ├── inventory-service/                   # Stock management (Port 8014)
│   ├── order-processing-service/            # Order handling (Port 8015)
│   └── simal-integration-service/           # External integration (Port 8016)
└── lego-factory-frontend/                   # React application
    ├── src/
    ├── package.json
    ├── package-lock.json
    ├── Dockerfile
    ├── nginx.conf
    └── .env.development / .env.production
```

## Technology Stack

**Backend Services**:
- **Java 17+**: Spring Boot 3.x, Spring Cloud Gateway, Spring Security, Spring Data JPA
- **Authentication**: JWT-based with BCrypt password encoding
- **Database**: 
  - PostgreSQL 15 (User Service - production auth data)
  - H2 Database (Other services - file-based with persistence)
- **Build**: Maven with wrapper
- **Containerization**: Docker with multi-stage builds

**Frontend**:
- **React 18+**: Modern functional components with hooks
- **Build Tool**: Vite for fast development and optimized builds
- **HTTP Client**: Axios with interceptors for API communication
- **Routing**: React Router for SPA navigation
- **Deployment**: Nginx for production serving

**DevOps & Deployment**:
- **Container Orchestration**: Docker Compose for local development
- **CI/CD**: GitLab CI/CD pipeline with multi-stage builds
- **Environment Management**: Separate configs for dev/production

## Security Notice ⚠️

**PostgreSQL Dependency Vulnerability**: The current PostgreSQL driver version has a HIGH severity vulnerability. Update required:

```xml
<!-- Current (vulnerable) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.4</version> <!-- or 42.7.5 -->
</dependency>

<!-- Update to -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.6</version> <!-- Latest secure version -->
</dependency>
```

## Current Implementation Status

### ✅ Completed Features

#### Core Infrastructure
- **Microservices Architecture**: 6 services with API Gateway routing
- **Database Layer**: PostgreSQL for auth + H2 for business data with persistence
- **Security Implementation**: JWT authentication with role-based authorization
- **Docker Integration**: Complete containerization with health checks
- **CI/CD Pipeline**: GitLab integration with automated builds and deployments

#### Backend Services
- **User Service**: Complete authentication system with password encoding
- **Entity Framework**: User, UserRole enums, repository pattern implementation
- **API Gateway**: Service routing with CORS support for frontend integration
- **Database Configuration**: Multi-database strategy with proper connection pools

#### Frontend Application
- **Environment Configuration**: Separate dev/production builds with Vite
- **API Integration**: Axios client with JWT token management
- **Build System**: Optimized production builds with nginx serving

### 🚧 In Progress
- Service-specific business logic implementation
- Frontend component development for each microservice
- Integration testing between services

### 📋 Planned Features
- Production-ready logging and monitoring
- Advanced error handling and recovery
- Performance optimization and caching
- Comprehensive test suites

## Quick Start

### Prerequisites
- **Docker Desktop** (for Windows)
- **Java 17+** (for development)
- **Node.js 18+** (for frontend development)

### Start Backend Services

```powershell
# Navigate to backend directory
cd lego-factory-backend

# Ensure environment file exists
# Copy .env.example to .env if needed

# Start all services with Docker Compose
docker-compose up -d --build

# Check service health
docker-compose ps
docker-compose logs -f api-gateway
```

### Start Frontend (Development)

```powershell
# Navigate to frontend directory
cd lego-factory-frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

**Application URLs**:
- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- Individual services: http://localhost:801X (where X = 2-6)

### Environment Configuration

**Backend (.env)**:
```bash
# Database
POSTGRES_DB=lego_factory_auth
POSTGRES_USER=legoapp
POSTGRES_PASSWORD=legoapp123

# Security
JWT_SECRET=your_jwt_secret_here
SPRING_PROFILES_ACTIVE=docker

# Integration
FRONTEND_URL=http://localhost:3000
```

**Frontend (.env.development)**:
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=LEGO Factory (Dev)
VITE_DEBUG_MODE=true
```

## API Documentation

All API endpoints are accessible through the API Gateway at `http://localhost:8080/api`

### Authentication Endpoints
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Token refresh

### User Management (Admin only)
- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Master Data
- `GET /api/masterdata/products` - Product catalog
- `GET /api/masterdata/workstations` - Workstation configuration

### Inventory Management
- `GET /api/inventory/stock` - Current stock levels
- `PUT /api/inventory/stock/{id}` - Update stock

### Order Processing
- `POST /api/customer-orders` - Create customer order
- `GET /api/customer-orders` - List orders
- `PATCH /api/customer-orders/{id}/status` - Update order status

## Development Guidelines

### Code Standards
- **Java**: Follow Spring Boot best practices, use constructor injection
- **React**: Functional components with hooks, TypeScript preferred
- **Database**: Repository pattern, transaction management with `@Transactional`

### Testing Strategy
- **Unit Tests**: JUnit 5 for backend services
- **Integration Tests**: TestContainers for database testing
- **Frontend Tests**: Jest + React Testing Library

### Deployment
- **Local Development**: Docker Compose for backend, npm dev server for frontend
- **Production**: GitLab CI/CD with Docker registry and deployment automation

## Contributing

1. Follow the microservices architecture pattern
2. Maintain separation between frontend and backend
3. Use proper environment configuration for different deployment stages
4. Update dependencies regularly to address security vulnerabilities
5. Write comprehensive tests for new features

## License

This project is a prototype for educational and demonstration purposes.