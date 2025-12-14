# LEGO Sample Factory Control System

A **microservice-based manufacturing control platform** that digitizes and automates supply chain operations for the LEGO Sample Factory. The system provides intelligent digital workflows for production order management, real-time inventory tracking, workstation assignments, and role-based operational dashboards.

## Overview

LIFE (LEGO Integrated Factory Execution) is a production-ready prototype designed to:

- **Manage multi-stage manufacturing**: From order creation through modules assembly to final product delivery
- **Track inventory in real-time**: Stock management across multiple workstations and warehouse locations
- **Optimize production scheduling**: Intelligent workstation assignment and task sequencing
- **Enable role-based operations**: Specialized dashboards for plant warehouse, modules supermarket, and manufacturing teams
- **Provide system observability**: Comprehensive error handling, structured logging, and performance monitoring

## System Architecture

LIFE uses a **6-tier microservice architecture** with an API Gateway as the central routing layer:

```plaintext
┌─────────────────────────────────────────────────────────────┐
│           React Frontend (Port 5173/5174)                    │
│  ┌──────────────┬──────────────┬──────────────────────────┐ │
│  │ Dashboard    │ Products     │ Workstation Pages        │ │
│  │ (Multi-role) │ Catalog      │ (Plant WH, Modules SM)   │ │
│  └──────────────┴──────────────┴──────────────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │
        ┌───────────────────▼────────────────────┐
        │   API Gateway (Port 8011)              │
        │   - Route all requests                 │
        │   - CORS support                       │
        │   - Load balancing                     │
        └───┬──────┬──────┬───────┬────┬────────┘
            │      │      │       │    │
       ┌────▼──┐ ┌─▼────┐┌──▼──┐┌─▼──┐┌──▼──┐
       │ User  │ │Master││Stock││Order│ │Simal│
       │Service│ │data  ││ mgmt││Proc.│ │Integ│
       │ 8012  │ │ 8013 ││8014 ││ 8015│ │ 8016│
       └───────┘ └──────┘└─────┘└────┘└─────┘
```

**Backend Services**:

- **User Service** (Port 8012): Authentication, authorization, user management
- **Masterdata Service** (Port 8013): Product catalog, modules, parts, workstations
- **Inventory Service** (Port 8014): Stock tracking and workstation inventory
- **Order Processing Service** (Port 8015): Customer orders, fulfillment, warehouse operations
- **SimAL Integration Service** (Port 8016): Production scheduling and simulation

**Persistence**: Each microservice maintains its own H2 file-based database for complete data isolation.

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.4.2, Spring Cloud Gateway 2024.0.0, Spring Security, Maven
- **Database**: H2 (file-based, one per service) for data isolation and easy deployment
- **Frontend**: React 18+, Vite, Axios, React Router
- **Tools**: Visual Studio Code, Node.js, npm

## Current Features & Status

### ✅ Implemented Features

#### Authentication & Authorization

- JWT-based authentication for all API requests
- Role-based access control (ADMIN, PLANT_WAREHOUSE, MODULES_SUPERMARKET, MANUFACTURING_OPERATOR)
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
- Structured logging with rolling file appenders (application.log, error.log, debug.log)
- Frontend toast notifications for user-facing error feedback
- Stack trace logging for debugging

#### UI/UX Features

- Compact, responsive grid layouts for product and order displays
- Color-coded status badges and item-type indicators
- Expandable component details (products show modules, modules show parts)
- Mobile-friendly design with adaptive font sizes and spacing
- Reduced header height (60% of original) for better screen utilization

## Setup & Running the Application

1. Prerequisites:
   - Windows with Docker Desktop running.
   - VS Code recommended.

2. Create your local environment file from the blueprint:
   - Copy the example:
     ```
     Copy-Item .env.example .env
     ```
   - Open `.env` and set values. At minimum, set a secure `SECURITY_JWT_SECRET` (32+ chars).
   - Keep `.env` private. It should be ignored by Git.

3. Build and run:
   ```
   docker compose up --build
   ```

4. Access the app:
   - Frontend via Nginx root proxy: http://localhost:80
   - API Gateway (direct dev port): http://localhost:8011
   - H2 console (per service, if enabled): http://localhost:<service-port>/h2-console

5. Tear down:
   ```
   docker compose down
   ```

### Default Test Accounts

- **Admin Account**: `legoAdmin` / `legoPass`
  - Full system access, user management, workstation configuration

- **Plant Warehouse**: `warehouseOperator` / `warehousePass`
  - Access to plant warehouse operations and customer order fulfillment

- **Modules Supermarket**: `modulesSupermarketOp` / `modulesPass`
  - Warehouse order management and module inventory fulfillment

## API Endpoints Summary

All endpoints are routed through the API Gateway at `http://localhost:8011`

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
