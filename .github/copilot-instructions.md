# LIFE System – AI Agent Instructions

> MES with 6 microservices, 9 workstations, 4 business scenarios.
> **Deep-dive docs:** `.github/copilot-refs/` | `_dev-docs/BusinessScenarios.md`

## Quick Start
```bash
docker-compose up -d                    # nginx:1011 → gateway:8011 → services
./test-scenario-{1,2,3,4}.sh            # Run ALL 4 before commit
cd lego-factory-frontend && npm run dev # Hot reload on :5173
```

## Architecture
```
nginx:1011 → api-gateway:8011 → {user:8012, masterdata:8013, inventory:8014, order:8015, simal:8016}
```
- **No shared DB** – REST-only via Docker DNS (`http://inventory-service:8014`)
- **New endpoints** require gateway route in `api-gateway/.../application.properties`
- **JWT secret** must match in `.env`, api-gateway, user-service
- **H2 in-memory** – data resets on restart; seed via DataInitializer

## Domain Model
- **Order chain:** CustomerOrder → WarehouseOrder → ProductionOrder → ControlOrders → SupplyOrders → WorkstationOrders
- **Workstations:** WS-1,2,3 (Manufacturing) → WS-4,5,6 (Assembly) → WS-7 (Plant Warehouse) ← WS-8 (Modules) ← WS-9 (Parts)
- **Scenarios:** 1=Direct fulfill, 2=Warehouse+Assembly, 3=Full production, 4=High-volume (qty≥LOT_SIZE_THRESHOLD)
- **WarehouseOrder.productionOrderId** links production to prevent concurrent order interference
- **triggerScenario field:** `DIRECT_FULFILLMENT`, `WAREHOUSE_ORDER_NEEDED`, `PRODUCTION_REQUIRED` – set on confirmation, drives UI buttons

## Critical Rules (Common Mistakes)
```java
POST /api/{order-type}/{id}/complete   // ✅ ONLY this moves inventory
PUT  /api/{order-type}/{id}            // ❌ Status change only, no stock
```
- **HTTP verbs:** PUT=status, POST=actions (start/complete/halt)
- **Production auto-completes** when all control orders finish
- **Supply orders MUST complete** before workstation orders can start
- **productId tracking:** Preserve through WarehouseOrderItem → FinalAssemblyOrder

## Backend Patterns
```java
// Use orchestration service, not raw RestTemplate
orchestrationService.notifyWorkstationOrderComplete(type, controlOrderId);
inventoryClient.creditStock(7L, "PRODUCT", productId, qty, "FULFILLMENT", "reason");
// Use constants: STATUS_COMPLETED, not "COMPLETED"
// Use config: config.getThresholds().getLotSizeThreshold() (default: 3)
```
- Package structure: `io.life.<service>/{controller,service,entity,dto,repository,exception}`
- Error responses use `ApiErrorResponse` with `errorCode` + `details` map

## Frontend Patterns
```javascript
session?.user?.workstationId   // ✅ FLAT field
session?.user?.workstation?.id // ❌ WRONG - not nested
```
- **Dashboard header:** ALL dashboards use `DashboardHeader` component (modular, unified design)
- **Dashboard system:** Use new unified components from `src/components/dashboard/`
  - `DashboardHeader` – Unified header for ALL dashboards (icon + title + refresh)
  - `WorkstationDashboard` – WS-1 to WS-6 (Manufacturing/Assembly)
  - `WarehouseDashboard` – WS-7, WS-8, WS-9 (3-column: Orders + Form/Inventory + Activity)
  - `ControlDashboard` – Production/Assembly Control (70/30 split)
- Import hooks from `src/hooks/index.js`: `useWorkstationOrders`, `useDashboardData`
- CSS: `src/styles/WorkstationDashboard.css` (layout), `src/styles/DashboardHeader.css` (unified header)
- API client: `src/api/api.js` (axios with auto-JWT injection)
- Workstation dashboards: `src/pages/dashboards/{WorkstationName}Dashboard.jsx`

## Key Files
| Purpose | Location |
|---------|----------|
| Gateway routes | `api-gateway/.../application.properties` |
| Order orchestration | `order-processing-service/.../OrderOrchestrationService.java` |
| Dashboard components | `lego-factory-frontend/src/components/dashboard/index.js` |
| Dashboard CSS | `lego-factory-frontend/src/styles/WorkstationDashboard.css` |
| Dashboard header | `lego-factory-frontend/src/components/dashboard/DashboardHeader.jsx` |
| Header CSS | `lego-factory-frontend/src/styles/DashboardHeader.css` |
| Shared utilities | `lego-factory-frontend/src/styles/utilities.css` |
| Workstation config | `lego-factory-frontend/src/config/workstationConfig.js` |
| Dashboard config | `lego-factory-frontend/src/components/dashboard/dashboardConfig.js` |

## Test Credentials (all use password: `password`)
- **Admin:** `lego_admin` | **WS-7:** `warehouse_operator` | **WS-8:** `modules_supermarket` | **WS-9:** `parts_supply`
- **Manufacturing:** `injection_molding` (WS-1), `parts_preproduction` (WS-2), `part_finishing` (WS-3)
- **Assembly:** `gear_assembly` (WS-4), `motor_assembly` (WS-5), `final_assembly` (WS-6)

## Commands
```bash
docker-compose build --no-cache <service> && docker-compose up -d <service>
docker-compose logs -f api-gateway    # Debug auth/routing
./push-to-registry.sh --all           # Deploy to registry
```

## Reference Docs
See `.github/copilot-refs/` for: [API_REFERENCE](copilot-refs/API_REFERENCE.md) | [ORDER_PROCESSING_RULES](copilot-refs/ORDER_PROCESSING_RULES.md) | [TROUBLESHOOTING](copilot-refs/TROUBLESHOOTING.md) | [BACKEND_PATTERNS](copilot-refs/BACKEND_PATTERNS.md) | [FRONTEND_PATTERNS](copilot-refs/FRONTEND_PATTERNS.md) | [ROLES_AND_WORKSTATIONS](copilot-refs/ROLES_AND_WORKSTATIONS.md) | [DEV_WORKFLOW](copilot-refs/DEV_WORKFLOW.md)
