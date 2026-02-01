# LIFE System – Copilot Instructions

> **Last Updated:** January 30, 2026  
> This file guides AI coding agents through the LIFE (LEGO Integrated Factory Execution) microservice architecture. It captures critical patterns, domain logic, and workflows required for productive contribution.

---

## Quick Start

```bash
# First time: copy environment template and configure port (1011 dev, 80 prod)
cp .env.example .env

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

**Roles** (in [UserRole.java](../lego-factory-backend/user-service/src/main/java/io/life/user_service/entity/UserRole.java)):
- `ADMIN` - System administration (no specific workstation)
- `PLANT_WAREHOUSE` - Customer order fulfillment (WS-7)
- `MODULES_SUPERMARKET` - Internal warehouse operations (WS-8)
- `PRODUCTION_PLANNING` - Factory scheduling and production orders (no specific workstation)
- `PRODUCTION_CONTROL` - Manufacturing oversight and control orders (no specific workstation)
- `ASSEMBLY_CONTROL` - Assembly coordination and control orders (no specific workstation)
- `PARTS_SUPPLY` - Raw materials warehouse operations (WS-9)
- `MANUFACTURING` - Production line execution (WS-1, WS-2, WS-3 - workstation assigned per user)
- `VIEWER` - Read-only monitoring (no workstation)

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

**Scenario 4 (IMPLEMENTED - High Volume):**
- When total order quantity ≥ `LOT_SIZE_THRESHOLD` (default: 3, configurable via Admin panel), skip WarehouseOrder entirely
- CustomerOrder spawns ProductionOrder directly via `ProductionOrderService.createFromCustomerOrder()`
- Frontend shows "Order Production" button when `triggerScenario = "DIRECT_PRODUCTION"`
- Admin can configure threshold via Admin Dashboard > Settings > LOT_SIZE_THRESHOLD`

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

**AUTOMATIC Production Completion (Feb 2026):**
- Production completes when all control orders complete
- `OrderOrchestrationService.completeProductionOrder()` automatically calls `submitProductionOrderCompletion()`
- No manual "submit" endpoint needed - completion is fully automatic
- Scenario 3: Credits WS-8 → Updates warehouse order → Ready for fulfillment
- Scenario 4: Credits WS-6 → Creates Final Assembly orders immediately

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
- `./test-scenario-1.sh` - Validates Scenario 1 flow: CustomerOrder with sufficient stock (direct fulfillment)
- `./test-scenario-2.sh` - Validates Scenario 2 flow: CustomerOrder → WarehouseOrder → FinalAssemblyOrder
- `./test-scenario-3.sh` - Validates Scenario 3 flow: Full production pipeline (Control Orders → Supply Orders → Workstation Orders)
- `./verify-scenarios-1-2.sh` - Full validation of scenarios 1 & 2 end-to-end
- `./verify-code-in-images.sh` - Verifies Docker images contain expected code changes
- Read scripts to understand expected sequencing; modify them when adding new endpoints
- Scripts use bash with colored output, jq for JSON parsing, and provide detailed test results

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
- **Active branch:** `dev` (current development), `feature/scenario-3-production-orders` (Scenario 3 in progress)
- `prod` branch is for production-ready releases
- Each feature should have its own branch
- **Commit message format:** `feat(component): description` or `fix(component): description`

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
