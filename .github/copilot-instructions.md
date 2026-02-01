# LIFE System – Copilot Instructions

> **Last Updated:** February 1, 2026  
> This file guides AI coding agents through the LIFE (LEGO Integrated Factory Execution) microservice architecture. It captures critical patterns, domain logic, and workflows required for productive contribution.
> 
> **Navigation:** This is the comprehensive reference. See also:
> - `_dev-docs/BusinessScenarios.md` - Order fulfillment workflows (4 scenarios)
> - `_dev-docs/PROJECT_TECHNICAL_OVERVIEW.md` - Architecture deep-dive
> - `README.md` - Project overview and technology stack
> - `PRE_COMMIT_CHECKLIST.md` - Validation checklist before commits

---

## 🚀 Speed Reference for AI Agents

**First-Time Setup:**
```bash
cp .env.example .env  # Configure ports & JWT secret
docker-compose up -d  # Start all services
```

**Common Dev Tasks:**
```bash
# Rebuild single service after code change
docker-compose build --no-cache order-processing-service && docker-compose up -d order-processing-service

# Debug service
docker-compose logs -f order-processing-service

# Test scenarios (run before every commit)
./test-scenario-1.sh && ./test-scenario-2.sh && ./test-scenario-3.sh && ./test-scenario-4.sh
```

**Critical Architecture Rules:**
- ✅ All traffic → nginx → api-gateway → services (NEVER bypass gateway)
- ✅ Stock updates ONLY on completion endpoints (not confirmation)
- ✅ Services communicate via REST (no shared database)
- ✅ JWT secret must match in: `.env`, `api-gateway/application.properties`, `user-service/application.properties`
- ✅ H2 in-memory DB resets on service restart (check DataInitializer for seed data)

**9 Workstations (WS-1 to WS-9):**
- WS-1, WS-2, WS-3: Manufacturing (Injection Molding, Pre-Production, Finishing)
- WS-4, WS-5, WS-6: Assembly (Gear, Motor, Final Assembly)
- WS-7: Plant Warehouse (Customer orders)
- WS-8: Modules Supermarket (Internal warehouse)
- WS-9: Parts Supply (Raw materials)

**4 Business Scenarios:**
1. Direct fulfillment (stock available at WS-7)
2. Warehouse order + Final Assembly (modules available at WS-8)
3. Full production (manufacture parts → assemble modules → final assembly)
4. High-volume direct production (qty ≥ LOT_SIZE_THRESHOLD, bypasses warehouse)

**Test Credentials (ALL use password: `password`):**
```bash
# Admin & System
lego_admin              # ADMIN role, all workstations
viewer_user             # VIEWER role, read-only

# Warehouses (WS-7, WS-8, WS-9)
warehouse_operator      # WS-7 Plant Warehouse
modules_supermarket     # WS-8 Modules Supermarket
parts_supply            # WS-9 Parts Supply

# Planning & Control
production_planning     # Production Planning (no workstation)
production_control      # Production Control (WS-1,2,3,9)
assembly_control        # Assembly Control (WS-4,5,6,8)

# Manufacturing (WS-1, WS-2, WS-3)
injection_molding       # WS-1 Injection Molding
parts_preproduction     # WS-2 Parts Pre-Production  
part_finishing          # WS-3 Part Finishing

# Assembly (WS-4, WS-5, WS-6)
gear_assembly           # WS-4 Gear Assembly
motor_assembly          # WS-5 Motor Assembly
final_assembly          # WS-6 Final Assembly
```

---

## Quick Start

```bash
# First time: copy environment template and configure ports
# Default: 1011 for dev (NGINX_ROOT_PROXY_EXTERNAL_PORT), 80 for prod
cp .env.example .env
# Edit .env to set JWT secret, ports, and other config

# Start all services: nginx + frontend + api-gateway + 5 microservices
docker-compose up -d

# Watch gateway logs for routing/auth issues
docker-compose logs -f api-gateway

# Fast frontend dev loop (backend in Docker, frontend local on :5173)
docker-compose up -d api-gateway user-service inventory-service order-processing-service masterdata-service
cd lego-factory-frontend && npm run dev

# Rebuild + restart a single service after code changes
docker-compose build --no-cache order-processing-service && docker-compose up -d order-processing-service

# Validate business scenarios (All 4 scenarios complete)
./test-scenario-1.sh  # Direct fulfillment from stock
./test-scenario-2.sh  # Warehouse order + final assembly
./test-scenario-3.sh  # Full production pipeline (all 9 workstations)
./test-scenario-4.sh  # High volume direct production (qty ≥ LOT_SIZE_THRESHOLD)
```

---

## Architecture: Six Microservices + Single Entry Point

```
nginx-root-proxy (port 1011/80)
  ├─ /api/* → api-gateway:8011 (JWT validation, route to services)
  └─ /* → frontend (React SPA)

api-gateway routes to 5 microservices:
  ├─ user-service:8012 (auth, users)
  ├─ masterdata-service:8013 (products, variants, cached)
  ├─ inventory-service:8014 (stock levels by workstation)
  ├─ order-processing-service:8015 (orders, workflows)
  └─ simal-integration-service:8016 (scheduling, Gantt charts)
```

**Critical rule:** All external traffic MUST flow through api-gateway. Never bypass to a service directly.  
**Service communication:** REST over Docker DNS (e.g., `http://inventory-service:8014`). No direct DB access between services.  
**Database isolation:** Each service has independent H2 in-memory database, seeded by `DataInitializer`.  
**JWT:** Issued by user-service, validated by gateway. Keep `SECURITY_JWT_SECRET` synchronized across `.env` and all `application.properties` files.

---

## API Endpoint Quick Reference

### Authentication & Users
```bash
# Login (get JWT token)
POST /api/auth/login
Body: { "username": "alice", "password": "password" }
Response: { "token": "eyJ...", "user": {...} }

# Get current user profile
GET /api/users/me
Headers: Authorization: Bearer <token>

# List all users (ADMIN only)
GET /api/users
```

### Masterdata (Products, Modules, Parts)
```bash
# Get all products (cached 10min)
GET /api/masterdata/products

# Get product details with BOM
GET /api/masterdata/products/{id}

# Get modules for a product
GET /api/masterdata/products/{productId}/modules

# Get parts for a module
GET /api/masterdata/modules/{moduleId}/parts
```

### Inventory (Stock Management)
```bash
# Get stock levels for workstation
GET /api/inventory?workstationId={wsId}

# Get stock for specific item
GET /api/stock?workstationId={wsId}&itemType={PRODUCT|MODULE|PART}&itemId={id}

# Adjust stock (internal use - called by order completion)
POST /api/stock/adjust
Body: { "workstationId": 7, "itemType": "PRODUCT", "itemId": 1, "delta": -5, "reason": "FULFILLMENT" }
```

### Customer Orders (WS-7 Plant Warehouse)
```bash
# List customer orders
GET /api/customer-orders

# Create customer order
POST /api/customer-orders
Body: { "orderItems": [{ "itemId": 1, "itemType": "PRODUCT", "requestedQuantity": 10 }] }

# Confirm customer order (sets triggerScenario)
PUT /api/customer-orders/{id}
Body: { "status": "CONFIRMED" }

# Complete customer order (debits stock)
POST /api/customer-orders/{id}/complete
```

### Warehouse Orders (WS-8 Modules Supermarket)
```bash
# List warehouse orders
GET /api/warehouse-orders

# Confirm warehouse order (checks module stock, sets triggerScenario)
PUT /api/warehouse-orders/{id}
Body: { "status": "CONFIRMED" }

# Fulfill warehouse order (creates Final Assembly orders)
POST /api/warehouse-orders/{id}/fulfill

# Create production order from warehouse order
POST /api/warehouse-orders/{id}/production-order
Body: { "priority": "NORMAL" }
```

### Production Orders (Production Planning)
```bash
# List production orders
GET /api/production-orders

# Get production order details with control orders
GET /api/production-orders/{id}

# Production auto-completes when all control orders done
# No manual submission endpoint needed
```

### Workstation Orders (WS-1 to WS-6)
```bash
# Manufacturing orders (WS-1, WS-2, WS-3)
GET /api/injection-molding-orders        # WS-1
GET /api/part-preproduction-orders       # WS-2
GET /api/part-finishing-orders           # WS-3

# Assembly orders (WS-4, WS-5, WS-6)
GET /api/gear-assembly-orders            # WS-4
GET /api/motor-assembly-orders           # WS-5
GET /api/final-assembly-orders           # WS-6

# Start workstation order
POST /api/{workstation-orders}/{id}/start

# Complete workstation order (credits inventory)
POST /api/{workstation-orders}/{id}/complete

# Halt workstation order
POST /api/{workstation-orders}/{id}/halt
```

### Supply Orders (WS-9 Parts Supply)
```bash
# List supply orders for workstation
GET /api/supply-orders?targetWorkstationId={wsId}

# Fulfill supply order (debits Parts Supply, enables workstation orders)
POST /api/supply-orders/{id}/fulfill
```

### System Configuration
```bash
# Get lot size threshold (Scenario 4)
GET /api/config/lot-size-threshold

# Update lot size threshold (ADMIN only)
PUT /api/config/lot-size-threshold
Body: { "value": 5 }
```

### Testing Endpoints
```bash
# Health check
GET /api/health

# Service-specific Swagger docs
GET /api/docs/user/swagger-ui.html        # User service
GET /api/docs/masterdata/swagger-ui.html  # Masterdata service
GET /api/docs/inventory/swagger-ui.html   # Inventory service
GET /api/docs/orders/swagger-ui.html      # Order processing service
GET /api/docs/simal/swagger-ui.html       # SimAL integration service
```

---

## Troubleshooting Flowchart

### 🔴 Problem: 401 Unauthorized

```
START: Getting 401 on API calls
  ↓
  Q: Is Authorization header present?
  ├─ NO → Add header: "Authorization: Bearer <token>"
  │        Get token from: POST /api/auth/login
  └─ YES → Check token validity
           ↓
           Q: Token expired? (default: 1hr)
           ├─ YES → Re-login to get new token
           └─ NO → Check JWT secret sync
                   ↓
                   Verify SECURITY_JWT_SECRET matches in:
                   - .env
                   - api-gateway/application.properties
                   - user-service/application.properties
                   ↓
                   If mismatch → Sync secrets, rebuild services
```

### 🟠 Problem: 404 Not Found

```
START: Getting 404 on API call
  ↓
  Q: Is route in api-gateway config?
  ├─ NO → Add route to api-gateway/application.properties
  │        Example: spring.cloud.gateway.routes[N].predicates[0]=Path=/api/your-endpoint/**
  │        Rebuild gateway: docker-compose build --no-cache api-gateway
  └─ YES → Check nginx logs
           ↓
           docker-compose logs -f nginx-root-proxy
           ↓
           Q: Does nginx see the request?
           ├─ NO → Frontend calling wrong URL
           │        Check baseURL in src/api/api.js
           └─ YES → Check gateway routing
                   ↓
                   docker-compose logs -f api-gateway
                   ↓
                   Service not responding? Check service health:
                   docker-compose ps
                   docker-compose logs -f <service-name>
```

### 🟡 Problem: Stock Not Updating

```
START: Inventory not changing after order operation
  ↓
  Q: Which endpoint was called?
  ├─ Confirmation (PUT) → ✗ WRONG - confirmations don't move stock
  │                        Use completion endpoint instead
  └─ Completion (POST) → Check inventory-service logs
           ↓
           docker-compose logs -f inventory-service | grep "credit\|debit"
           ↓
           Q: Do you see stock adjustment logs?
           ├─ NO → Backend not calling inventoryClient
           │        Check service implementation
           │        Example: FinalAssemblyOrderService.completeOrder()
           │                 should call inventoryClient.creditStock()
           └─ YES → Check workstation ID match
                   ↓
                   Verify order.workstationId == inventory.workstationId
                   Different workstations = isolated stock pools
```

### 🔵 Problem: Order Status Stuck

```
START: Order stuck in PENDING/IN_PROGRESS
  ↓
  Q: What order type?
  ├─ Workstation Order → Check supply order status
  │   ↓
  │   Q: Is supply order FULFILLED?
  │   ├─ NO → Workstation orders CANNOT start until supply fulfilled
  │   │        Complete supply order first: POST /api/supply-orders/{id}/fulfill
  │   └─ YES → Check parent control order status
  │            ↓
  │            Control order must be ASSIGNED or IN_PROGRESS
  │            Check: GET /api/production-control-orders/{id}
  │
  ├─ Control Order → Check child workstation orders
  │   ↓
  │   Control order completes when ALL workstation orders COMPLETED
  │   Check progress: OrderOrchestrationService.getProgress()
  │
  ├─ Production Order → Check control orders
  │   ↓
  │   Production auto-completes when ALL control orders COMPLETED
  │   No manual action needed (automatic since Feb 2026)
  │
  └─ Warehouse Order → Check triggerScenario
      ↓
      Q: What is triggerScenario value?
      ├─ PRODUCTION_REQUIRED → Production order must complete first
      │                         Check: GET /api/production-orders
      │                         Status should be COMPLETED
      ├─ DIRECT_FULFILLMENT → Click "Fulfill" button
      │                        Modules available in stock
      └─ null → Order not confirmed yet
                Click "Confirm" first
```

### 🟢 Problem: Frontend Shows Wrong Button

```
START: Button doesn't match expected action
  ↓
  Q: What component?
  ├─ CustomerOrderCard → Check order.status
  │   PENDING → Confirm button
  │   CONFIRMED → 
  │     ├─ triggerScenario=DIRECT_FULFILLMENT → Complete button
  │     └─ triggerScenario=WAREHOUSE_ORDER_NEEDED → Process button
  │
  ├─ WarehouseOrderCard → Check triggerScenario
  │   CONFIRMED →
  │     ├─ triggerScenario=DIRECT_FULFILLMENT → Fulfill button
  │     ├─ triggerScenario=PRODUCTION_REQUIRED → Order Production button
  │     └─ triggerScenario=DIRECT_PRODUCTION → Order Production button
  │   AWAITING_PRODUCTION → No button (waiting for production)
  │
  └─ WorkstationOrderCard → Check supply order status
      PENDING + supply=PENDING → Waiting for parts
      PENDING + supply=FULFILLED → Start button
      IN_PROGRESS → Complete button
```

### ⚫ Problem: Database Lost Data

```
START: Data disappeared after service restart
  ↓
  H2 is IN-MEMORY - all data resets on restart
  ↓
  Solutions:
  1. Check DataInitializer seed data
     - Located in each service's main application class
     - Should recreate essential test data on startup
  
  2. For persistent testing, switch to PostgreSQL
     - Infrastructure already configured
     - Update application.properties spring.datasource.url
  
  3. For development, accept data loss
     - Use scenario scripts to recreate test orders
     - ./test-scenario-1.sh, ./test-scenario-2.sh, etc.
```

### 🔧 Problem: Service Won't Start

```
START: Service fails to start in Docker
  ↓
  Check logs: docker-compose logs -f <service-name>
  ↓
  Common errors:
  
  1. Port conflict (Address already in use)
     ↓
     netstat -tulpn | grep <port>
     ↓
     Kill conflicting process or change port in .env
  
  2. JPA schema error (Table already exists)
     ↓
     H2 schema corruption - rebuild with --no-cache
     docker-compose build --no-cache <service-name>
  
  3. Dependency injection error (No qualifying bean)
     ↓
     Missing @Service/@Repository annotation
     Check component scanning in @SpringBootApplication
  
  4. JWT secret mismatch
     ↓
     Sync SECURITY_JWT_SECRET across:
     - .env
     - api-gateway/application.properties
     - user-service/application.properties
  
  5. Database connection error
     ↓
     Check spring.datasource.url in application.properties
     H2: jdbc:h2:mem:<servicename>db
```

### 🟣 Problem: SimAL Integration Issues

```
START: Production order not progressing through workflow
  ↓
  Q: What is current production order status?
  ├─ PENDING → Not yet scheduled
  │   ↓
  │   Check if warehouse order triggered production
  │   WarehouseOrder.productionOrderId should be set
  │   WarehouseOrder.status should be AWAITING_PRODUCTION
  │   ↓
  │   If productionOrderId is null:
  │   - Check warehouse order confirmation logic
  │   - Verify triggerScenario set to PRODUCTION_REQUIRED
  │   - Check insufficient stock detection in WarehouseOrderService
  │
  ├─ SCHEDULED → Waiting for control orders
  │   ↓
  │   Q: Do control orders exist?
  │   ├─ NO → SimAL integration failed
  │   │        See "🟣 Problem: SimAL Integration Issues" above
  │   └─ YES → Check control order status
  │            ↓
  │            GET /api/production-control-orders?productionOrderId={id}
  │            GET /api/assembly-control-orders?productionOrderId={id}
  │            ↓
  │            Q: Are ALL control orders COMPLETED?
  │            ├─ NO → See "Control Order Stuck" below
  │            └─ YES → Production should auto-complete
  │                     If not, check OrderOrchestrationService logs
  │
  ├─ IN_PROGRESS → Control orders executing
  │   ↓
  │   Check which control orders are blocking:
  │   - ProductionControlOrder (WS-1, WS-2, WS-3)
  │   - AssemblyControlOrder (WS-4, WS-5, WS-6)
  │   ↓
  │   For each control order, check workstation order progress
  │   ↓
  │   See "Control Order Stuck" below
  │
  └─ COMPLETED → Should trigger automatic downstream flow
      ↓
      Q: Did production completion credit inventory?
      ├─ NO → Check OrderOrchestrationService.completeProductionOrder()
      │        Should call submitProductionOrderCompletion()
      │        Logs: "Auto-completed ProductionOrder"
      │        Check inventory-service logs for credit operations
      └─ YES → Check warehouse order status update
               ↓
               Q: Scenario 3 or Scenario 4?
               ├─ Scenario 3 (has warehouseOrderId) →
               │   WarehouseOrder should update to CONFIRMED
               │   triggerScenario should be DIRECT_FULFILLMENT
               │   Modules Supermarket (WS-8) should show "Fulfill" button
               │   ↓
               │   If not updated:
               │   - Check notifyProductionOrderComplete() in orchestration
               │   - Verify warehouse order fetch successful
               │   - Check warehouse order status transition logic
               │
               └─ Scenario 4 (has customerOrderId) →
                   Should create Final Assembly orders automatically
                   Check: GET /api/final-assembly-orders
                   ↓
                   If no orders created:
                   - Check createFinalAssemblyOrdersFromProductionOrder()
                   - Verify customer order items exist
                   - Check FinalAssemblyOrderService.createFromProductionOrder()
```

**Common SimAL Integration Fixes:**

````bash
# 1. Restart SimAL service
docker-compose restart simal-integration-service

# 2. Check SimAL service health
curl http://localhost:1011/api/simal/health

# 3. Verify masterdata service accessible
docker-compose logs -f simal-integration-service | grep "masterdata"

# 4. Test schedule generation manually
curl -X POST http://localhost:1011/api/simal/schedules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productionOrderId": 1, "priority": "NORMAL"}'

# 5. Check control order creation endpoint
curl http://localhost:1011/api/production-control-orders \
  -H "Authorization: Bearer $TOKEN"

# 6. Verify task status updates are working (403 resolved in Feb 2026)
curl -X PATCH http://localhost:1011/api/simal/tasks/{taskId}/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
`````

**Known Issue - Gantt Chart Real-Time Updates:**
- ⚠️ **Issue**: Gantt chart task colors don't update in real-time when workstation orders complete
- **Status**: Task status updates to database work correctly (HTTP 403 authentication issue resolved Feb 2026)
- **Root Cause**: Frontend Production Planning dashboard doesn't poll/refresh Gantt chart after task status changes
- **Workaround**: Manual page refresh shows current task statuses
- **Impact**: Non-blocking - task execution proceeds normally, only visual indicator stale
- **Future Enhancement**: Implement WebSocket/SSE for real-time Gantt chart updates or polling mechanism
- **Testing**: Check `simal-integration-service` logs for "✓ Updated task" messages to verify status changes persist

---

## Data Model: Products, Modules, and Parts

**Product Hierarchy (3-tier BOM):**
```
Product (WS-7 Plant Warehouse)
  ↳ Modules (WS-8 Modules Supermarket)
      ↳ Parts (WS-9 Parts Supply)
```

**Key Concepts:**
- **Products** = End items shipped to customers (e.g., "LEGO Model Car")
- **Modules** = Subassemblies (Gear Module, Motor Module) built at WS-4, WS-5, WS-6
- **Parts** = Raw materials/components produced at WS-1, WS-2, WS-3
- **Variants** = No longer used; products have direct `variantName` field (e.g., "Red", "Blue")
- **BOM Lookup** = Masterdata service provides product → modules → parts relationships

**Stock Management:**
- Each workstation has isolated inventory (no shared stock pools)
- Stock queries: `GET /api/inventory?workstationId={id}&variantId={id}`
- Credit/debit operations ONLY happen on order completion (not confirmation)

---

## 9 Roles × 9 Workstations: Role-Based UI Routing

### Authentication Quick Reference

**All test users use password: `password`** (defined in UserInitializer.java)

**Common Authentication Errors:**
- ❌ **401 Unauthorized**: Wrong username/password or expired JWT token
- ❌ **403 Forbidden**: Valid login but insufficient role permissions
- ❌ **Username typos**: `warehouse_operator` (correct) vs `warehouse-operator` (wrong)
- ✅ **Token format**: `Authorization: Bearer eyJ...` (space after "Bearer")

**Login Example:**
```bash
# Correct
curl -X POST http://localhost:1011/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"warehouse_operator","password":"password"}'

# Response includes JWT token valid for 1 hour (configurable)
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 2,
    "username": "warehouse_operator",
    "role": "PLANT_WAREHOUSE",
    "workstation": { "id": 7, "name": "Plant Warehouse" }
  }
}
```

### Role Permission Matrix

**Roles** (in [UserRole.java](../lego-factory-backend/user-service/src/main/java/io/life/user_service/entity/UserRole.java)):

| Role | Test Username | Workstation Access | Primary Dashboard | Key Permissions |
|------|---------------|-------------------|------------------|------------------|
| `ADMIN` | `lego_admin` | ALL (WS-1 to WS-9) | AdminDashboard | All endpoints, system config, user management |
| `PLANT_WAREHOUSE` | `warehouse_operator` | WS-7 | PlantWarehouseDashboard | Customer orders, stock fulfillment |
| `MODULES_SUPERMARKET` | `modules_supermarket` | WS-8 | ModulesSupermarketDashboard | Warehouse orders, module fulfillment |
| `PRODUCTION_PLANNING` | `production_planning` | ALL (planning view) | ProductionPlanningDashboard | Production orders, scheduling, SimAL integration |
| `PRODUCTION_CONTROL` | `production_control` | WS-1,2,3,9 | ProductionControlDashboard | Manufacturing control orders, supply orders |
| `ASSEMBLY_CONTROL` | `assembly_control` | WS-4,5,6,8 | AssemblyControlDashboard | Assembly control orders, module tracking |
| `PARTS_SUPPLY` | `parts_supply` | WS-9 | PartsSupplyWarehouseDashboard | Supply orders, parts distribution |
| `MANUFACTURING` | `injection_molding`<br>`parts_preproduction`<br>`part_finishing` | Assigned WS only | Workstation-specific | Workstation orders (start/complete/halt) |
| `VIEWER` | `viewer_user` | ALL (read-only) | ViewerDashboard | Read-only access, no modifications |

**Access Control Examples:**
```java
// Backend - Check access in service
if (!workstationAccessService.canAccessWorkstation(user, 7)) {
    throw new AccessDeniedException("Cannot access Plant Warehouse");
}

// Frontend - Check access before routing
import { canRoleAccessWorkstation } from '../config/workstationConfig';
if (!canRoleAccessWorkstation(user.role, 7)) {
    return <Redirect to="/unauthorized" />;
}
```

**Workstations** and their dashboards in [src/pages/dashboards/](../lego-factory-frontend/src/pages/dashboards/):
- **WS-1:** Injection Molding (Manufacturing) → `InjectionMoldingDashboard.jsx`
- **WS-2:** Parts Pre-Production (Manufacturing) → `PartsPreProductionDashboard.jsx`
- **WS-3:** Part Finishing (Manufacturing) → `PartFinishingDashboard.jsx`
- **WS-4:** Gear Assembly (Assembly) → `GearAssemblyDashboard.jsx`
- **WS-5:** Motor Assembly (Assembly) → `MotorAssemblyDashboard.jsx`
- **WS-6:** Final Assembly (Assembly) → `FinalAssemblyDashboard.jsx`
- **WS-7:** Plant Warehouse (Customer fulfillment) → `PlantWarehouseDashboard.jsx`
- **WS-8:** Modules Supermarket (Internal warehouse) → `ModulesSupermarketDashboard.jsx`
- **WS-9:** Parts Supply Warehouse (Raw materials) → `PartsSupplyWarehouseDashboard.jsx`

**Workstation Access Control:**
- **Backend**: [UserRole.java](../lego-factory-backend/user-service/src/main/java/io/life/user_service/entity/UserRole.java) defines `accessibleWorkstations` and `primaryWorkstation` per role
- **Backend Service**: [WorkstationAccessService.java](../lego-factory-backend/user-service/src/main/java/io/life/user_service/service/WorkstationAccessService.java) provides centralized access validation
- **Frontend**: [workstationConfig.js](../lego-factory-frontend/src/config/workstationConfig.js) mirrors the access rules with `ROLE_WORKSTATION_ACCESS`
- Use `UserRole.canAccessWorkstation(wsId)` or `WorkstationAccessService.canAccessWorkstation(user, wsId)` for access checks
- Use `getRolePrimaryWorkstation(role)` in frontend for dashboard routing

**Frontend routing** ([DashboardPage.jsx](../lego-factory-frontend/src/pages/DashboardPage.jsx)):
1. Admin → AdminDashboard
2. User's explicit `workstationId` → Workstation-specific dashboard
3. Role's `primaryWorkstation` → Workstation-specific dashboard (via `getRolePrimaryWorkstation()`)
4. Role-specific dashboard (for control/planning roles with no workstation)
5. Fallback for unknown configuration

## Order Processing Domain Rules - CRITICAL SEQUENCE

**Stock Updates Only on Completion:** Inventory credits/debits happen ONLY on completion endpoints (e.g., `completeOrder` calls `inventoryService.credit/debit`); do not change stock on confirmation.

**triggerScenario Field:** Set during CustomerOrder/WarehouseOrder confirmation to determine downstream actions:
- `DIRECT_FULFILLMENT` - Stock available at current workstation, can fulfill immediately
- `WAREHOUSE_ORDER_NEEDED` - Insufficient stock; must create WarehouseOrder (WS-7 only)
- `PRODUCTION_REQUIRED` - Must trigger production workflow (WS-8 only)
- `DIRECT_PRODUCTION` - (Scenario 4) Large lot size (≥ LOT_SIZE_THRESHOLD) bypasses warehouse, goes directly to production

**Order Hierarchy (full Scenario 3 flow with control orders):**
```
CustomerOrder (WS-7, Plant Warehouse)
  ↓ (if WAREHOUSE_ORDER_NEEDED)
WarehouseOrder (WS-8, Modules Supermarket)
  ↓ (if PRODUCTION_REQUIRED)
ProductionOrder (Production Planning)
  ├─ ProductionControlOrder (manages WS-1, WS-2, WS-3)
  │     ├─ SupplyOrder → WS-9 fulfills parts
  │     ├─ InjectionMoldingOrder (WS-1) → passes to WS-2
  │     ├─ PartPreProductionOrder (WS-2) → passes to WS-3
  │     └─ PartFinishingOrder (WS-3) → credits WS-9
  │
  └─ AssemblyControlOrder (manages WS-4, WS-5, WS-6)
        ├─ SupplyOrder → WS-9 fulfills parts
        ├─ GearAssemblyOrder (WS-4) → credits WS-8
        ├─ MotorAssemblyOrder (WS-5) → credits WS-8
        └─ FinalAssemblyOrder (WS-6) → credits WS-7
```

**Scenario 3 Gating Logic (CRITICAL):**
- **Supply must be fulfilled first:** Workstation orders stay in `PENDING` until their `SupplyOrder` status = `FULFILLED`
- **All workstations must complete:** Control order cannot complete until ALL child workstation orders are `COMPLETED`
- **All control orders must complete:** ProductionOrder cannot complete until ALL control orders are `COMPLETED`
- **Upward propagation:** Use `OrderOrchestrationService.notifyWorkstationOrderComplete()` to cascade status updates

**Scenario 4 (✅ FULLY IMPLEMENTED - High Volume):**
- When total order quantity ≥ `LOT_SIZE_THRESHOLD` (default: 3, configurable via Admin panel), skip WarehouseOrder entirely
- CustomerOrder spawns ProductionOrder directly via `ProductionOrderService.createFromCustomerOrder()`
- Frontend shows "Order Production" button when `triggerScenario = "DIRECT_PRODUCTION"`
- Admin can configure threshold via Admin Dashboard > Settings > LOT_SIZE_THRESHOLD
- Production completion automatically creates Final Assembly orders (bypasses Modules Supermarket)
- Final Assembly credits Plant Warehouse directly → Customer order ready for completion

**CRITICAL: ProductId Tracking Through Order Chain:**
- `WarehouseOrderItem` has `productId` field to track which product modules belong to
- `FulfillmentService` sets `productId` when converting products to modules via BOM lookup
- `WarehouseOrderService.createFinalAssemblyOrdersFromWarehouseOrder()` groups items by `productId`
- `FinalAssemblyOrder.outputProductId` must contain actual PRODUCT ID (1-4), never module IDs (7+)
- This ensures Final Assembly credits Plant Warehouse with correct product inventory

**CRITICAL: Production Order Linking (Feb 2026 Enhancement):**
- `WarehouseOrder.productionOrderId` links warehouse order to its production order
- When production created: `warehouseOrder.setProductionOrderId(productionOrder.getId())`
- Purpose: Prevents multiple warehouse orders from fighting over same modules
- **Fulfillment Logic:** Orders with `productionOrderId != null` bypass stock checks (modules are "reserved")
- **Stock Check Logic:** Only recalculate triggerScenario if `productionOrderId == null`
- Example: WO-1 triggers production → Modules credited to WS-8 → WO-2 arrives → WO-2 cannot "steal" WO-1's modules

**AUTOMATIC Production Completion (✅ Feb 2026):**
- Production completes when all control orders complete
- `OrderOrchestrationService.completeProductionOrder()` automatically calls `submitProductionOrderCompletion()`
- No manual "submit" endpoint needed - completion is fully automatic
- Scenario 3: Credits WS-8 → Updates warehouse order → Ready for fulfillment
- Scenario 4: Credits WS-6 (Final Assembly directly) → Creates Final Assembly orders immediately
- `FinalAssemblyOrderService` automatically updates customer order when all Final Assembly orders submitted

**Scenario 2 Workflow (STRICT SEQUENCE):**
1. **PlantWarehouse (WS-7) - Confirm CustomerOrder:** Stock check for PRODUCTS only → sets `triggerScenario`
2. **Frontend shows:** "Fulfill" button (DIRECT_FULFILLMENT) or "Process" button (WAREHOUSE_ORDER_NEEDED)
3. **ModulesSupermarket (WS-8) - Confirm WarehouseOrder:** Stock check for MODULES only → sets `triggerScenario`
4. **Frontend shows:** "Fulfill" button (DIRECT_FULFILLMENT) or "Order Production" button (PRODUCTION_REQUIRED)
5. **FinalAssembly (WS-6) - Submit Order:** Credits Plant Warehouse with PRODUCT using `productId` from warehouse order items

**Scenario 3 Workflow (Full Production - ✅ WORKING):**
1. **ProductionOrder created** when WarehouseOrder finds insufficient modules
2. **WarehouseOrder.productionOrderId set** to link warehouse order to its production (prevents interference)
3. **Production Planning** integrates with SimAL scheduling service for task optimization
4. **SimAL generates schedule** and creates Control Orders via [ControlOrderIntegrationService](lego-factory-backend/simal-integration-service/src/main/java/io/life/simal_integration_service/service/ControlOrderIntegrationService.java):
   - `ProductionControlOrder` - Manufacturing tasks (WS-1, WS-2, WS-3)
   - `AssemblyControlOrder` - Assembly tasks (WS-4, WS-5, WS-6)
5. **Each Control Order spawns Supply Orders** to stage materials at target workstation from Parts Supply (WS-9)
6. **Workstation orders execute** after supply orders complete, crediting inventory at appropriate points
7. **Production auto-completes** → Credits WS-8 → Updates WarehouseOrder to CONFIRMED (DIRECT_FULFILLMENT)
8. **User clicks Fulfill** → Bypasses stock checks (productionOrderId != null) → Creates Final Assembly orders
9. **Final Assembly completes** → Credits WS-7 (Plant Warehouse) → CustomerOrder shows Complete button
10. **Final flow:** ProductionOrder → ControlOrders → SupplyOrders → WorkstationOrders → Auto-completion → Warehouse fulfill → Final Assembly → Customer complete

**Key Changes (Feb 2026):**
- Production completion is AUTOMATIC (no manual submission needed)
- WarehouseOrders with linked production (`productionOrderId != null`) BYPASS stock checks during fulfillment
- Stock checks ONLY for orders without linked production
- Supply orders MUST complete before workstation orders can start

## Backend Patterns & Implementation

### Package Structure & New Endpoints
- Packages follow `io.life.<service>` with folders: `controller/`, `service/`, `entity/`, `dto/`, `repository/`, `exception/`
- Keep DTOs service-local (e.g., `OrderProcessingCreateOrderDTO` in order-processing-service)
- Any new REST endpoint requires: controller + service logic + gateway route in [application.properties](../lego-factory-backend/api-gateway/src/main/resources/application.properties) + RestTemplate calls using Docker hostnames
- Workstation-specific controllers already exist (e.g., `/injection-molding-orders`, `/final-assembly-orders`, `/customer-orders`); extend those instead of adding generic endpoints

### Order Entities & Domain Objects
- CustomerOrder, WarehouseOrder have `triggerScenario` field (set during confirmation, not creation)
- All entities use `@PrePersist`/`@PreUpdate` for audit timestamps (`createdAt`, `updatedAt`); never set these manually
- Status transitions validated in service layer (not controller) before persistence
- Entity DTOs transform to/from JSON; keep conversion logic in service, not controller

### Service Layer Orchestration
- **OrderOrchestrationService** ([OrderOrchestrationService.java](../lego-factory-backend/order-processing-service/src/main/java/io/life/order/service/OrderOrchestrationService.java)) centralizes all order status propagation:
  - `notifyWorkstationOrderComplete(WorkstationOrderType, controlOrderId)` - Workstation → Control Order
  - `notifyControlOrderComplete(ControlOrderType, productionOrderId)` - Control Order → Production Order
  - Use `OrderOrchestrationService.STATUS_COMPLETED` constants instead of magic strings
- **Inter-service clients** in `io.life.order.client/`:
  - `InventoryClient` - Stock adjustments via `/api/stock/adjust` with `delta` field
  - `MasterdataClient` - BOM lookups and product/module queries
- Controllers inject service + clients, call service methods, return DTOs
- Example: `InjectionMoldingOrderService.completeOrder()` calls `orchestrationService.notifyWorkstationOrderComplete()`

### Database & H2 Console
- Each service has isolated H2 in-memory database; schema created by Spring/JPA, seeded by DataInitializer in main() chain
- Access H2 console during dev: `http://localhost:<service-port>/h2-console`, JDBC URL = `jdbc:h2:mem:<servicename>db` (e.g., `jdbc:h2:mem:orderdb`)
- No shared database; inter-service data access ONLY via REST API

### HTTP Verbs Convention
- Confirm/status-change actions → PUT (e.g., `PUT /api/customer-orders/{id}` with status change)
- Start/Complete/Halt workflow actions → POST (e.g., `POST /api/customer-orders/{id}/complete`)
- Retrieve → GET; Create → POST; Update metadata/notes → PATCH

## Frontend Conventions & Component Patterns

### Dashboard Architecture (Standardized January 2026)
- Each workstation has a **dedicated** dashboard file (no generic dashboards): `InjectionMoldingDashboard.jsx`, `FinalAssemblyDashboard.jsx`, etc.
- All dashboards wrap content with [StandardDashboardLayout.jsx](../lego-factory-frontend/src/components/StandardDashboardLayout.jsx) component for consistent UI
- [DashboardPage.jsx](../lego-factory-frontend/src/pages/DashboardPage.jsx) routes users based on `session?.user?.workstationId` (flat field, NOT nested `workstation.id`)

### Manufacturing/Assembly Dashboards (WS-1 to WS-6)
- Use shared config from [src/config/workstationConfig.js](../lego-factory-frontend/src/config/workstationConfig.js) for messages, titles, API endpoints
- Import `useWorkstationOrders` hook to fetch workstation-specific orders: `useWorkstationOrders(workstationId)`
- Each dashboard receives from hook: orders list, loading/error states, handlers (startOrder, completeOrder, haltOrder)
- Hook provides auto-refresh (default 30s), notification management, and order statistics calculation

### Custom Hooks Pattern (Available in src/hooks/)
- `useWorkstationOrders(workstationId, options)` - Order fetching, state management, CRUD handlers
- `useActivityLog(workstationId)` - Activity feed with real-time updates
- `useInventoryDisplay(workstationId)` - Stock levels with auto-refresh
- Always import from `src/hooks/index.js` for future-proofing

### Order Cards & triggerScenario
- Import reusable cards from [src/components/index.js](../lego-factory-frontend/src/components/index.js): `Button`, `StatCard`, `BaseOrderCard`, `OrdersSection`
- Order cards check `triggerScenario` field to display appropriate action buttons:
  - `DIRECT_FULFILLMENT` → Show "Fulfill" button (stock available)
  - `WAREHOUSE_ORDER_NEEDED` → Show "Process" button (create WO)
  - `PRODUCTION_REQUIRED` → Show "Order Production" button
- See [WarehouseOrderCard.jsx](../lego-factory-frontend/src/components/WarehouseOrderCard.jsx) for example implementation

### API Integration & State Management
- Keep API helpers in [src/api/api.js](../lego-factory-frontend/src/api/api.js); axios interceptor automatically injects JWT
- Frontend calls are relative paths (`/api/...`); nginx proxy_pass routes to api-gateway without trailing slash
- Session context provides `session?.user?.workstationId`, roles, and auth state
- Use React hooks for component state; avoid prop drilling across 3+ levels

### Design System & Styling
- Reuse design-system components: `Button`, `StatCard`, `BaseOrderCard`, `OrdersSection` (imported via [src/components/index.js](../lego-factory-frontend/src/components/index.js))
- Do NOT create inline styles or bespoke UI elements; extend CSS Modules in component folders
- DEPRECATED: `DashboardLayout.jsx` in `src/components/` - always use `StandardDashboardLayout` for new dashboards

## Dev Workflow & Testing

### Iterative Development Cycle
1. **Make changes** in your feature branch (e.g., `feature/scenario-2-final-assembly`)
2. **Rebuild Docker image** for affected service(s): `docker-compose build --no-cache order-processing-service`
3. **Restart service** in docker-compose: `docker-compose up -d order-processing-service`
4. **Verify deployment** by checking logs: `docker-compose logs -f order-processing-service` (watch for startup errors)
5. **Test with scenario scripts** or manual API calls: `./test-scenario-2.sh` or `curl -H "Authorization: Bearer $TOKEN" http://localhost:1011/api/...`

### Backend Development (Java Services)
- **Local Spring Boot execution**: `cd lego-factory-backend/order-processing-service && ./mvnw spring-boot:run` (H2 resets on restart)
- **H2 database console**: `http://localhost:8015/h2-console` (or service port). JDBC URL: `jdbc:h2:mem:orderdb`
- **View logs**: `docker-compose logs -f order-processing-service` (or use IDE debugger)
- **Port references**: See `.env` for all ports. Each service has its own Spring application.properties overriding defaults.
- **Entity best practices**: All entities use `@PrePersist`/`@PreUpdate` for audit timestamps; never set `createdAt`/`updatedAt` manually

### Frontend Development (React/Vite)
- **Local dev server** with auto-reload: Start docker services, then `cd lego-factory-frontend && npm run dev` (proxy to :5173)
- **Build production frontend**: `npm run build` (or Docker builds automatically)
- **Clear browser cache**: Vite handles JS hashing, but test in incognito mode if unsure
- **API debugging**: Open DevTools → Network tab → inspect `/api/...` requests to verify auth headers and response codes
- **Component patterns**: Always use StandardDashboardLayout wrapper; import hooks from `src/hooks/index.js`

### Scenario Validation Scripts

**Test Scripts Overview:**
- `./test-scenario-1.sh` - Validates Scenario 1 flow: CustomerOrder with sufficient stock (direct fulfillment)
- `./test-scenario-2.sh` - Validates Scenario 2 flow: CustomerOrder → WarehouseOrder → FinalAssemblyOrder
- `./test-scenario-3.sh` - Validates Scenario 3 flow: Full production pipeline (Control Orders → Supply Orders → Workstation Orders)
- `./test-scenario-4.sh` - Validates Scenario 4 flow: High volume direct production (qty ≥ LOT_SIZE_THRESHOLD)
- `./verify-scenarios-1-2.sh` - Full validation of scenarios 1 & 2 end-to-end
- `./verify-code-in-images.sh` - Verifies Docker images contain expected code changes

**Test Coverage Matrix:**

| Scenario | Services Tested | Workstations | Orders Created | Key Validations |
|----------|----------------|--------------|----------------|------------------|
| **1** | inventory, order-processing | WS-7 | CustomerOrder | Stock debit, order completion |
| **2** | inventory, order-processing, masterdata | WS-7, WS-8, WS-6 | CustomerOrder, WarehouseOrder, FinalAssemblyOrder | BOM lookup, module debit, product credit |
| **3** | ALL 5 services | ALL 9 workstations | CustomerOrder, WarehouseOrder, ProductionOrder, 2 ControlOrders, 6-9 SupplyOrders, 6+ WorkstationOrders | Full production cycle, supply gating, orchestration |
| **4** | order-processing, inventory, simal | WS-7, WS-6 | CustomerOrder, ProductionOrder, FinalAssemblyOrder | Threshold bypass, direct production, auto-completion |

**Expected Output Format:**
```bash
# All test scripts output:
========================================
SCENARIO X: [NAME]
========================================

[TEST N] Step Description
  → Step: Action being performed
  ℹ  Info: Additional context
✅ PASS: Validation description
❌ FAIL: Error description

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TEST SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Passed: X/Y tests
❌ Failed: Z tests
```

**CRITICAL Testing Rules:**
- ✅ Run ALL 4 test scenarios before every commit
- ✅ Test scripts authenticate with service-specific users (warehouse_operator, modules_supermarket, etc.)
- ✅ Scripts validate inventory changes, order status transitions, and API responses
- ✅ Each test is idempotent - can be run multiple times without breaking state
- ✅ Scripts automatically adjust inventory to ensure consistent test conditions
- Read scripts to understand expected sequencing; modify them when adding new endpoints

### Pre-Commit Checklist
Before committing any changes, review [PRE_COMMIT_CHECKLIST.md](../PRE_COMMIT_CHECKLIST.md):
- ✅ All services healthy and compiling
- ✅ Run all 4 test scenario scripts
- ✅ Browser console checks (no critical errors)
- ✅ Documentation updates (copilot-instructions, BusinessScenarios.md)
- ✅ End-to-end workflow validation

### Common Debugging Scenarios
**Service won't start:**
```bash
# Check logs for startup errors (JPA schema issues, port conflicts)
docker-compose logs -f order-processing-service

# Verify port availability
netstat -tulpn | grep 8015

# Rebuild with clean cache
docker-compose build --no-cache order-processing-service
```

**401 Unauthorized on API calls:**
- Verify JWT token in request headers: `Authorization: Bearer <token>`
- Check `SECURITY_JWT_SECRET` matches across `.env`, api-gateway, and user-service
- Token might be expired (default: 1 hour); re-login to get new token

**Stock not updating:**
- Confirm you're calling completion endpoint (POST `/complete`), not confirmation (PUT)
- Check inventory-service logs for debit/credit operations
- Verify workstation ID matches in order and inventory record

**Frontend shows 404 for API:**
- Check nginx-root-proxy logs: `docker-compose logs -f nginx-root-proxy`
- Verify route exists in [api-gateway/application.properties](../lego-factory-backend/api-gateway/src/main/resources/application.properties)
- Test endpoint directly: `curl http://localhost:1011/api/...`

**H2 database "lost" data:**
- H2 is in-memory; all data resets on service restart
- Check DataInitializer seed data if entities missing on startup
- For persistent testing, consider switching to PostgreSQL (infrastructure ready)

### Git Workflow (Jan 2026 Best Practice)
```bash
git checkout -b feature/your-feature  # Create feature branch from dev
# ... make changes, test thoroughly ...
git add . && git commit -m "Implement X feature"
git push origin feature/your-feature  # Push to remote
# Create PR, get review, merge to dev when ready
```
- **NEVER commit directly to `master`**; keep it as last-known-good state
- **Current active branch:** `master` (as of Feb 2026 - stable production-ready state)
- **Branch strategy**: Create feature branches for new work, merge via PR after validation
- `prod` branch is for production-ready releases (can be created from master)
- Each feature should have its own branch with descriptive name
- **Commit message format:** `feat(component): description` or `fix(component): description`
- **Before committing**: Review [PRE_COMMIT_CHECKLIST.md](../PRE_COMMIT_CHECKLIST.md) and run all test scenarios

### Docker Registry Deployment (Production)
**For deploying to live servers:**
```bash
# Development machine: Build, tag, and push to local registry
./push-to-registry.sh  # Auto-detects changes vs origin/prod (smart builds)
# OR force build all:
./push-to-registry.sh --all

# Live server: Pull and deploy latest images (recommended - automatic)
./update-from-registry.sh  # Auto-pull, stop, start, verify

# Alternative: Manual pull with confirmation
./pull-from-registry.sh && docker-compose up -d

# Diagnose server issues
./check-server-state.sh  # Checks services, logs, and network
./diagnose-server-git-state.sh  # Verifies git state and sync issues
```

**Registry:** `192.168.1.237:5000` (local)  
**Images auto-tagged:** Both `latest` and versioned tags for rollback capability  
**See:** [SERVER_UPDATE_GUIDE.md](../SERVER_UPDATE_GUIDE.md) for detailed deployment workflows

---

## Pitfalls to Avoid

1. **Stock timing**: Do NOT credit/debit inventory on confirmation. Only on completion endpoints. Never check stock BEFORE confirmation—stock checks must happen DURING the confirm operation.
2. **ProductId tracking**: WarehouseOrderItem must have BOTH `itemId` (module) AND `productId` (target product). FinalAssemblyOrder uses `productId` to credit correct inventory.
3. **Endpoint routing**: Always add gateway routes in [api-gateway/application.properties](../lego-factory-backend/api-gateway/src/main/resources/application.properties) when creating new endpoints; never assume a service is accessible directly.
4. **Frontend paths**: Use `session?.user?.workstationId` (flat field, NOT `session?.user?.workstation?.id`).
5. **JWT synchronization**: If you change `SECURITY_JWT_SECRET`, update `.env`, gateway `application.properties`, AND user-service `application.properties`.
6. **Order status transitions**: Validate state transitions in the service layer, not the controller. WarehouseOrder status goes PENDING → CONFIRMED (not PROCESSING) after confirmation.
7. **Database assumptions**: Never write raw SQL or assume a shared database. All inter-service queries go via REST API.
8. **HTTP verbs**: Confirm/status-change = PUT, Start/Complete/Halt = POST (not PUT). Retrieve = GET, Create = POST, Update notes = PATCH.
9. **UI components**: Never create inline styles or custom UI; always extend StandardDashboardLayout and reuse design-system components.
10. **Docker hostnames**: Use service DNS names (`http://user-service:8012`), not `localhost`. These only resolve inside the Docker network.
11. **Cache invalidation**: Restart services with `--no-cache` when changing code. Vite handles JS versioning, but Docker layers may cache old JARs.
12. **BOM conversions**: When converting products to modules, preserve original productId through order chain to ensure correct final inventory crediting.
13. **Status magic strings**: Use `OrderOrchestrationService.STATUS_COMPLETED` constants instead of hardcoded `"COMPLETED"` strings.
14. **Inter-service calls**: Use `InventoryClient`/`MasterdataClient` instead of raw RestTemplate calls for centralized error handling.

---

## Additional Notes

### START: Multiple warehouse orders interfering with production
  ↓
  Q: Is productionOrderId field set on warehouse order?
  ├─ NO → Production created but link not established
  │        Check WarehouseOrderService.createProductionOrder()
  │        Should call: warehouseOrder.setProductionOrderId()
  │        ↓
  │        Fix: Manually update database or recreate production order
  │        UPDATE warehouse_order SET production_order_id = X WHERE id = Y;
  │
  └─ YES → Check fulfillment logic
           ↓
           Q: Does fulfillment bypass stock checks?
           ├─ NO → WarehouseOrderService.fulfillWarehouseOrder() bug
           │        Should check: if (order.getProductionOrderId() != null)
           │        Then bypass recalculateTriggerScenario()
           │        ↓
           │        Fix: Update fulfillment logic to check productionOrderId
           │
           └─ YES → Check if modules were credited correctly
                   ↓
                   GET /api/inventory?workstationId=8&itemType=MODULE
                   ↓
                   Q: Do modules exist in WS-8?
                   ├─ NO → Production completion didn't credit inventory
                   │        Check OrderOrchestrationService.creditModulesSupermarketFromProduction()
                   │        Check inventory-service logs for credit operations
                   └─ YES → Fulfillment should work
                            If "Fulfill" button missing, check frontend:
                            - order.triggerScenario should be DIRECT_FULFILLMENT
                            - WarehouseOrderCard.jsx button logic
```
# 1. Check production order hierarchy
curl http://localhost:1011/api/production-orders/{id} \
  -H "Authorization: Bearer $TOKEN" | jq

# 2. Check all control orders for production
curl "http://localhost:1011/api/production-control-orders?productionOrderId={id}" \
  -H "Authorization: Bearer $TOKEN" | jq
curl "http://localhost:1011/api/assembly-control-orders?productionOrderId={id}" \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. Check supply orders for a control order
curl "http://localhost:1011/api/supply-orders?targetWorkstationId={wsId}" \
  -H "Authorization: Bearer $TOKEN" | jq

# 4. Check workstation order completion counts
curl "http://localhost:1011/api/injection-molding-orders?productionControlOrderId={id}" \
  -H "Authorization: Bearer $TOKEN" | jq '[.[] | select(.status=="COMPLETED")] | length'

# 5. Force production order completion (if stuck)
# WARNING: Only use if orchestration service failed
curl -X POST http://localhost:1011/api/production-orders/{id}/submit \
  -H "Authorization: Bearer $TOKEN"

# 6. Check warehouse order production link
curl http://localhost:1011/api/warehouse-orders/{id} \
  -H "Authorization: Bearer $TOKEN" | jq '.productionOrderId'

# 7. Verify module inventory after production
curl "http://localhost:1011/api/inventory?workstationId=8&itemType=MODULE" \
  -H "Authorization: Bearer $TOKEN" | jq

  # 1. Check schedule structure
curl http://localhost:1011/api/simal/schedules?productionOrderId={id} \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected structure:
# {
#   "scheduleId": 1,
#   "productionOrderId": 1,
#   "tasks": [
#     {
#       "taskId": "INJ-1-1",
#       "taskType": "INJECTION_MOLDING",
#       "workstationId": 1,
#       "startTime": "2026-02-01T08:00:00",
#       "endTime": "2026-02-01T10:00:00",
#       "productName": "LEGO Model Car",
#       "quantity": 10,
#       "status": "PENDING"
#     }
#   ]
# }

# 2. Validate task times are ISO 8601 format
curl http://localhost:1011/api/simal/schedules?productionOrderId={id} \
  -H "Authorization: Bearer $TOKEN" | \
  jq '.tasks[0] | {start: .startTime, end: .endTime}'

# 3. Check workstation IDs are valid (1-9)
curl http://localhost:1011/api/simal/schedules?productionOrderId={id} \
  -H "Authorization: Bearer $TOKEN" | \
  jq '.tasks[].workstationId' | sort -u