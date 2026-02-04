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
- **H2 in-memory DBs** reset on restart – use DataInitializer for seed data

## Domain Model
- **Order chain:** CustomerOrder → WarehouseOrder → ProductionOrder → ControlOrders → SupplyOrders → WorkstationOrders
- **Workstations:** WS-1,2,3 (Manufacturing) → WS-4,5,6 (Assembly) → WS-7 (Plant Warehouse) ← WS-8 (Modules) ← WS-9 (Parts)
- **Scenarios:** 1=Direct fulfill, 2=Warehouse+Assembly, 3=Full production (auto-completes), 4=High-volume (qty≥LOT_SIZE_THRESHOLD)
- **Supply orders MUST complete** before workstation orders can start (Scenario 3 gating)
- **productionOrderId linking:** WarehouseOrder.productionOrderId prevents concurrent orders from stealing each other's modules

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

// All dashboards extend StandardDashboardLayout (DashboardLayout.jsx is DEPRECATED)
// Import hooks from src/hooks/index.js
```
- **Homepage CSS:** Modular structure in `src/styles/homepage/` – import via `index.css`
- **Workstation dashboards:** `src/pages/dashboards/{WorkstationName}Dashboard.jsx`

## Key Files
| Purpose | Location |
|---------|----------|
| Gateway routes | `api-gateway/src/main/resources/application.properties` |
| Order orchestration | `order-processing-service/.../service/OrderOrchestrationService.java` |
| Config (thresholds, IDs) | `order-processing-service/.../config/OrderProcessingConfig.java` |
| Workstation config | `lego-factory-frontend/src/config/workstationConfig.js` |
| Error handling | Each service: `GlobalExceptionHandler.java` + `ApiErrorResponse` |
| Dashboard layout | `lego-factory-frontend/src/components/StandardDashboardLayout.jsx` |

## Test Credentials (password: `password`)
| Username | Role | Workstation |
|----------|------|-------------|
| `lego_admin` | ADMIN | All |
| `warehouse_operator` | PLANT_WAREHOUSE | WS-7 |
| `modules_supermarket` | MODULES_SUPERMARKET | WS-8 |
| `parts_supply` | PARTS_SUPPLY | WS-9 |
| `production_planning` | PRODUCTION_PLANNING | All (planning) |
| `production_control` | PRODUCTION_CONTROL | WS-1,2,3,9 |
| `assembly_control` | ASSEMBLY_CONTROL | WS-4,5,6,8 |
| `injection_molding` | MANUFACTURING | WS-1 |
| `parts_preproduction` | MANUFACTURING | WS-2 |
| `part_finishing` | MANUFACTURING | WS-3 |
| `gear_assembly` | ASSEMBLY_CONTROL | WS-4 |
| `motor_assembly` | ASSEMBLY_CONTROL | WS-5 |
| `final_assembly` | ASSEMBLY_CONTROL | WS-6 |
| `viewer_user` | VIEWER | Read-only |

## SimAL Integration (Scheduling Service)
SimAL (simal-integration-service:8016) handles production scheduling and Gantt chart generation.

**Key Flow:**
```
ProductionOrder created → POST /api/simal/production-order → Schedule generated
→ ControlOrderIntegrationService.createControlOrdersFromSchedule() → ProductionControlOrder + AssemblyControlOrder
```

**Critical Files:**
- `simal-integration-service/.../controller/SimalController.java` – Scheduling endpoints
- `simal-integration-service/.../service/ControlOrderIntegrationService.java` – Creates control orders from schedule

**Workstation Assignment:**
- WS-1,2,3 → ProductionControlOrder (manufacturing tasks)
- WS-4,5,6 → AssemblyControlOrder (assembly tasks)

**Frontend Integration:**
- Production Planning dashboard fetches schedules via `/api/simal/schedules`
- GanttChart component renders `scheduledTasks` with zoom/pan controls
- Task status updates via `/api/simal/tasks/{taskId}/status`

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
