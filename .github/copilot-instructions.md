# LIFE System – AI Agent Instructions

> Manufacturing Execution System: 6 microservices, 9 workstations, 4 business scenarios.
> **Deep-dive docs:** `.github/copilot-refs/` | `_dev-docs/BusinessScenarios.md`

## Quick Start
```bash
docker-compose up -d                    # Start all services (nginx:1011 → gateway → services)
./test-scenario-{1,2,3,4}.sh            # Run ALL 4 before commit
cd lego-factory-frontend && npm run dev # Hot reload on :5173
```

## Architecture
```
nginx:1011 → api-gateway:8011 → {user:8012, masterdata:8013, inventory:8014, order:8015, simal:8016}
```
- **No shared DB** – REST-only via Docker DNS (`http://inventory-service:8014`)
- **New endpoints** require gateway route in `api-gateway/src/main/resources/application.properties`
- **JWT secret** must match in `.env`, api-gateway, user-service
- **H2 in-memory** – data resets on restart; seed via `DataInitializer` classes

## Domain Model
- **Order chain:** CustomerOrder → WarehouseOrder → ProductionOrder → ControlOrders → SupplyOrders → WorkstationOrders
- **Workstations:** WS-1,2,3 (Manufacturing) → WS-4,5,6 (Assembly) → WS-7 (Plant Warehouse) ← WS-8 (Modules) ← WS-9 (Parts)
- **Scenarios:** 1=Direct fulfill, 2=Warehouse+Assembly, 3=Full production, 4=High-volume (qty≥`LOT_SIZE_THRESHOLD`)
- **triggerScenario:** `DIRECT_FULFILLMENT` | `WAREHOUSE_ORDER_NEEDED` | `PRODUCTION_REQUIRED` | `DIRECT_PRODUCTION` – set on order confirmation, drives UI buttons

## Critical Rules (Common Mistakes)
```java
POST /api/{order-type}/{id}/complete   // ✅ ONLY this moves inventory
PUT  /api/{order-type}/{id}            // ❌ Status change only, no stock movement
```
- **HTTP verbs:** PUT=status update, POST=actions (start/complete/halt)
- **Production auto-completes** when all control orders finish (no manual submission)
- **Supply orders MUST complete** before workstation orders can start
- **productId tracking:** Preserve through `WarehouseOrderItem.productId` → `FinalAssemblyOrder.outputProductId`

## Backend Patterns
```java
// Use orchestration service for order flow (not raw RestTemplate)
orchestrationService.notifyWorkstationOrderComplete(type, controlOrderId);
inventoryClient.creditStock(7L, "PRODUCT", productId, qty, "FULFILLMENT", "reason");

// Use constants and config, not magic values
OrderOrchestrationService.STATUS_COMPLETED  // not "COMPLETED"
config.getThresholds().getLotSizeThreshold() // default: 3
```
- Package: `io.life.<service>/{controller,service,entity,dto,repository,exception}`
- Errors: `ApiErrorResponse` with `errorCode` + `details` map (see [BACKEND_PATTERNS](copilot-refs/BACKEND_PATTERNS.md))

## Frontend Patterns
```javascript
session?.user?.workstationId   // ✅ FLAT field
session?.user?.workstation?.id // ❌ WRONG - not nested
```
- **Dashboards:** Use `src/components/dashboard/` unified components:
  - `DashboardHeader` – ALL dashboards (icon + title + refresh)
  - `WorkstationDashboard` – WS-1 to WS-6 | `WarehouseDashboard` – WS-7,8,9 | `ControlDashboard` – Control stations
- **Order cards:** Use `UnifiedOrderCard` and `OrdersGrid` from `src/components/orders/`
- **Hooks:** Import from `src/hooks/index.js`: `useWorkstationOrders`, `useDashboardData`
- **API:** `src/api/api.js` (axios with auto-JWT injection)

## Key Files
| Purpose | Location |
|---------|----------|
| Gateway routes | `api-gateway/src/main/resources/application.properties` |
| Order orchestration | `order-processing-service/.../service/OrderOrchestrationService.java` |
| Dashboard components | `lego-factory-frontend/src/components/dashboard/index.js` |
| Dashboard CSS | `lego-factory-frontend/src/styles/WorkstationDashboard.css` |
| Dashboard header | `lego-factory-frontend/src/components/dashboard/DashboardHeader.jsx` |
| Header CSS | `lego-factory-frontend/src/styles/DashboardHeader.css` |
| Shared utilities | `lego-factory-frontend/src/styles/utilities.css` |
| Workstation config | `lego-factory-frontend/src/config/workstationConfig.js` |
| Dashboard config | `lego-factory-frontend/src/components/dashboard/dashboardConfig.js` |

## Test Credentials (password: `password`)
| Role | Username | Workstation |
|------|----------|-------------|
| Admin | `lego_admin` | ALL |
| Plant Warehouse | `warehouse_operator` | WS-7 |
| Modules Supermarket | `modules_supermarket` | WS-8 |
| Parts Supply | `parts_supply` | WS-9 |
| Manufacturing | `injection_molding`, `parts_preproduction`, `part_finishing` | WS-1,2,3 |
| Assembly | `gear_assembly`, `motor_assembly`, `final_assembly` | WS-4,5,6 |

## Commands
```bash
docker-compose build --no-cache <service> && docker-compose up -d <service>  # Rebuild service
docker-compose logs -f api-gateway                                            # Debug auth/routing
./push-to-registry.sh --all                                                   # Deploy to registry
```

## Reference Docs
See `.github/copilot-refs/` for detailed documentation:
- [API_REFERENCE](copilot-refs/API_REFERENCE.md) – All REST endpoints
- [ORDER_PROCESSING_RULES](copilot-refs/ORDER_PROCESSING_RULES.md) – Order flow & scenario logic  
- [BACKEND_PATTERNS](copilot-refs/BACKEND_PATTERNS.md) – Java/Spring conventions
- [FRONTEND_PATTERNS](copilot-refs/FRONTEND_PATTERNS.md) – React/Vite conventions
- [ROLES_AND_WORKSTATIONS](copilot-refs/ROLES_AND_WORKSTATIONS.md) – Access control
- [TROUBLESHOOTING](copilot-refs/TROUBLESHOOTING.md) – Debug flowcharts
- [DEV_WORKFLOW](copilot-refs/DEV_WORKFLOW.md) – Build, test, deploy
