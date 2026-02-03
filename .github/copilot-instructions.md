# LIFE System – AI Agent Instructions

> Manufacturing Execution System: 6 microservices, 9 workstations, 4 business scenarios.
> **Detailed docs:** `.github/copilot-refs/` | `_dev-docs/BusinessScenarios.md` | `API_CONTRACTS.md`

## Quick Start
```bash
docker-compose up -d                    # Start all (nginx:1011 → gateway:8011 → services)
./test-scenario-{1,2,3,4}.sh            # Run ALL 4 scenario scripts before commit
cd lego-factory-frontend && npm run dev # Frontend dev with hot reload (port 5173)
```

## Architecture (CRITICAL)
```
nginx:1011 → api-gateway:8011 → {user:8012, masterdata:8013, inventory:8014, order:8015, simal:8016}
```
- **No shared DB** – REST-only via Docker DNS (`http://inventory-service:8014`)
- **New endpoints** require gateway route in `api-gateway/src/main/resources/application.properties`
- **JWT secret** must match across `.env`, api-gateway, and user-service

## Domain Model
- **Order chain:** CustomerOrder → WarehouseOrder → ProductionOrder → ControlOrders → SupplyOrders → WorkstationOrders
- **Workstations:** WS-1,2,3 (Manufacturing) → WS-4,5,6 (Assembly) → WS-7 (Plant Warehouse) ← WS-8 (Modules) ← WS-9 (Parts)
- **Scenarios:** 1=Direct fulfill, 2=Warehouse+Assembly, 3=Full production (auto-completes), 4=High-volume (qty≥3)
- **Supply orders MUST complete** before workstation orders can start (Scenario 3 gating)

## Stock & Order Rules (MOST COMMON MISTAKES)
```java
// ✅ ONLY /complete endpoints move inventory
POST /api/{order-type}/{id}/complete  // Credits/debits stock

// ❌ Confirmation NEVER moves stock (just sets triggerScenario)
PUT /api/{order-type}/{id}            // Status change only
```
- **HTTP verbs:** PUT=status changes, POST=actions (start/complete/halt)
- **Production auto-completes** when all control orders finish – no manual submission
- **productId tracking:** Preserve through WarehouseOrderItem → FinalAssemblyOrder chain

## Backend Patterns
```java
// Use centralized services, not raw RestTemplate
orchestrationService.notifyWorkstationOrderComplete(type, controlOrderId);
inventoryClient.creditStock(7L, "PRODUCT", productId, qty, "FULFILLMENT", "reason");

// Use constants from OrderOrchestrationService
STATUS_COMPLETED  // Not hardcoded "COMPLETED"

// Use OrderProcessingConfig for thresholds/IDs
config.getThresholds().getLotSizeThreshold();  // Default: 3
config.getWorkstations().getPlantWarehouse();  // 7L
```

## Frontend Patterns
```javascript
// User workstation: FLAT field (not nested)
session?.user?.workstationId  // ✅ Correct
session?.user?.workstation?.id  // ❌ Wrong

// All dashboards extend StandardDashboardLayout
// Import hooks from src/hooks/index.js

// GanttChart enhanced props (Feb 2026)
<GanttChart 
  onRefresh={handleRefresh}   // Auto-refresh callback
  refreshInterval={30000}      // ms, 0 to disable
  showCurrentTime={true}       // Red current-time indicator
/>
```
- **Homepage CSS:** Modular structure in `src/styles/homepage/` – import via `index.css`
- **Workstation dashboards:** `src/pages/dashboards/{WorkstationName}Dashboard.jsx`

## Key Files
| Purpose | Location |
|---------|----------|
| Gateway routes | `api-gateway/src/main/resources/application.properties` |
| Order orchestration | `order-processing-service/.../service/OrderOrchestrationService.java` |
| Config properties | `order-processing-service/.../config/OrderProcessingConfig.java` |
| Workstation config | `lego-factory-frontend/src/config/workstationConfig.js` |
| Error handling | Each service: `GlobalExceptionHandler.java` + `ApiErrorResponse` |
| Homepage CSS modules | `lego-factory-frontend/src/styles/homepage/index.css` |

## Test Credentials (password: `password`)
`lego_admin` (all), `warehouse_operator` (WS-7), `modules_supermarket` (WS-8), `production_planning`, `final_assembly` (WS-6)

## Common Commands
```bash
docker-compose build --no-cache <service> && docker-compose up -d <service>  # Rebuild
docker-compose logs -f api-gateway          # Debug auth/routing
./push-to-registry.sh --all                 # Deploy to 192.168.1.237:5000
```

## Reference Docs
| Topic | File |
|-------|------|
| API Endpoints | [copilot-refs/API_REFERENCE.md](copilot-refs/API_REFERENCE.md) |
| Order Processing | [copilot-refs/ORDER_PROCESSING_RULES.md](copilot-refs/ORDER_PROCESSING_RULES.md) |
| Troubleshooting | [copilot-refs/TROUBLESHOOTING.md](copilot-refs/TROUBLESHOOTING.md) |
| Backend Patterns | [copilot-refs/BACKEND_PATTERNS.md](copilot-refs/BACKEND_PATTERNS.md) |
| Frontend Patterns | [copilot-refs/FRONTEND_PATTERNS.md](copilot-refs/FRONTEND_PATTERNS.md) |
| Dev Workflow | [copilot-refs/DEV_WORKFLOW.md](copilot-refs/DEV_WORKFLOW.md) |
| Roles & Access | [copilot-refs/ROLES_AND_WORKSTATIONS.md](copilot-refs/ROLES_AND_WORKSTATIONS.md) |
