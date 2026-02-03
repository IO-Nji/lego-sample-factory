# LIFE System – AI Agent Instructions

> Manufacturing Execution System with 6 microservices, 9 workstations, 4 business scenarios.
> **Detailed References:** See `.github/copilot-refs/` for complete documentation.

## Extended Reference Documents

| Topic | File |
|-------|------|
| API Endpoints | [copilot-refs/API_REFERENCE.md](copilot-refs/API_REFERENCE.md) |
| Troubleshooting | [copilot-refs/TROUBLESHOOTING.md](copilot-refs/TROUBLESHOOTING.md) |
| Roles & Workstations | [copilot-refs/ROLES_AND_WORKSTATIONS.md](copilot-refs/ROLES_AND_WORKSTATIONS.md) |
| Order Processing Rules | [copilot-refs/ORDER_PROCESSING_RULES.md](copilot-refs/ORDER_PROCESSING_RULES.md) |
| Backend Patterns | [copilot-refs/BACKEND_PATTERNS.md](copilot-refs/BACKEND_PATTERNS.md) |
| Frontend Patterns | [copilot-refs/FRONTEND_PATTERNS.md](copilot-refs/FRONTEND_PATTERNS.md) |
| Dev Workflow & Testing | [copilot-refs/DEV_WORKFLOW.md](copilot-refs/DEV_WORKFLOW.md) |

**Additional docs:** `_dev-docs/BusinessScenarios.md`, `PRE_COMMIT_CHECKLIST.md`, `API_CONTRACTS.md`

## Quick Start
```bash
docker-compose up -d                    # Start all services (nginx:1011 → gateway:8011 → services)
./test-scenario-1.sh                    # Validate before commit (run all 4 scenarios)
cd lego-factory-frontend && npm run dev # Frontend dev with hot reload
```

## Architecture Rules
- **Traffic flow:** `nginx:1011 → api-gateway:8011 → {user:8012, masterdata:8013, inventory:8014, order:8015, simal:8016}`
- **No shared DB** – services communicate via REST only using Docker DNS (`http://inventory-service:8014`)
- **JWT sync** – `SECURITY_JWT_SECRET` must match in `.env`, `api-gateway`, and `user-service` properties
- **New endpoints** – always add gateway route in `api-gateway/src/main/resources/application.properties`

## Domain Model
- **Workstations:** WS-1,2,3 (Manufacturing) → WS-4,5,6 (Assembly) → WS-7 (Plant Warehouse) ← WS-8 (Modules) ← WS-9 (Parts)
- **Order chain:** CustomerOrder → WarehouseOrder → ProductionOrder → ControlOrders → WorkstationOrders
- **Scenarios:** 1=Direct fulfill, 2=Warehouse+Assembly, 3=Full production, 4=High-volume (qty≥3 bypasses warehouse)

## Critical Patterns

### Stock Operations
```java
// ✅ Completion endpoints credit/debit inventory
POST /api/customer-orders/{id}/complete  // Debits stock
POST /api/final-assembly-orders/{id}/complete  // Credits Plant Warehouse

// ❌ Confirmation endpoints NEVER move stock (only set triggerScenario)
PUT /api/customer-orders/{id}  // Sets triggerScenario, no inventory change
```

### Order Orchestration
```java
// Use OrderOrchestrationService for all status propagation
orchestrationService.notifyWorkstationOrderComplete(WorkstationOrderType.PART_FINISHING, controlOrderId);
inventoryClient.creditStock(7L, "PRODUCT", productId, quantity, "FULFILLMENT", "reason");
// Use STATUS_COMPLETED constant, not "COMPLETED" string
```

### Frontend
```javascript
// User workstation access: session?.user?.workstationId (flat, NOT .workstation.id)
// Dashboard routing: DashboardPage.jsx → WORKSTATION_DASHBOARDS[workstationId]
// All dashboards use StandardDashboardLayout wrapper
```

## Key Files
| Purpose | Location |
|---------|----------|
| Gateway routes | `api-gateway/src/main/resources/application.properties` |
| Order flow logic | `order-processing-service/.../service/OrderOrchestrationService.java` |
| Workstation config | `lego-factory-frontend/src/config/workstationConfig.js` |
| Role definitions | `user-service/.../entity/UserRole.java` |
| Error handling | Each service: `GlobalExceptionHandler.java` with `ApiErrorResponse` |

## Test Credentials (password: `password`)
| Role | Username | Workstation |
|------|----------|-------------|
| Admin | `lego_admin` | All |
| Plant Warehouse | `warehouse_operator` | WS-7 |
| Modules Supermarket | `modules_supermarket` | WS-8 |
| Production Planning | `production_planning` | — |
| Final Assembly | `final_assembly` | WS-6 |
| Production Control | `production_control` | WS-1,2,3,9 |
| Assembly Control | `assembly_control` | WS-4,5,6,8 |

## Common Commands
```bash
docker-compose build --no-cache order-processing-service && docker-compose up -d order-processing-service  # Rebuild service
cd lego-factory-frontend && npm run dev  # Frontend dev (backend in Docker)
./push-to-registry.sh --all              # Deploy to registry
docker-compose logs -f api-gateway       # Debug routing/auth issues
```

## Common Pitfalls (CRITICAL - DO NOT IGNORE)

1. **Stock timing** – credit/debit ONLY on `/complete` endpoints, NEVER on confirmation. Never check stock BEFORE confirmation—stock checks must happen DURING the confirm operation.
2. **HTTP verbs** – PUT for status changes, POST for actions (start/complete/halt), PATCH for notes/metadata
3. **Docker hostnames** – use `http://service-name:port` inside containers (not localhost). These only resolve inside Docker network.
4. **productId tracking** – `WarehouseOrderItem` must have BOTH `itemId` (module) AND `productId` (target product). Preserve through WarehouseOrderItem → FinalAssemblyOrder chain. `FinalAssemblyOrder.outputProductId` must be actual PRODUCT ID (1-4), never module IDs (7+).
5. **Gateway routes** – new endpoints MUST have routes in `api-gateway/application.properties`
6. **Frontend paths** – use `session?.user?.workstationId` (flat field, NOT `session?.user?.workstation?.id`)
7. **JWT synchronization** – if you change `SECURITY_JWT_SECRET`, update `.env`, gateway `application.properties`, AND user-service `application.properties`
8. **Order status transitions** – validate state transitions in service layer, not controller. WarehouseOrder: PENDING → CONFIRMED (not PROCESSING)
9. **Database assumptions** – never write raw SQL or assume shared database. All inter-service queries via REST API only.
10. **UI components** – never create inline styles or custom UI; always extend `StandardDashboardLayout` and reuse design-system components
11. **Cache invalidation** – restart services with `--no-cache` when changing code. Vite handles JS versioning, but Docker layers may cache old JARs.
12. **BOM conversions** – when converting products to modules, preserve original productId through order chain for correct final inventory crediting
13. **Status magic strings** – use `OrderOrchestrationService.STATUS_COMPLETED` constants instead of hardcoded `"COMPLETED"` strings
14. **Inter-service calls** – use `InventoryClient`/`MasterdataClient` instead of raw RestTemplate calls for centralized error handling
15. **Configuration values** – use `OrderProcessingConfig` properties instead of hardcoded values (thresholds, timeouts, workstation IDs)

---

## Extended Reference

### API Endpoints

#### Authentication
```bash
POST /api/auth/login  # Body: {"username":"X","password":"Y"} → Returns JWT
GET /api/users/me     # Headers: Authorization: Bearer <token>
```

#### Orders (Common Pattern)
```bash
GET /api/{order-type}                    # List all
GET /api/{order-type}/{id}               # Get by ID
PUT /api/{order-type}/{id}               # Confirm (status change)
POST /api/{order-type}/{id}/start        # Start work
POST /api/{order-type}/{id}/complete     # Complete (moves inventory)
POST /api/{order-type}/{id}/halt         # Halt work
```

Order types: `customer-orders`, `warehouse-orders`, `production-orders`, `final-assembly-orders`, `supply-orders`, `injection-molding-orders`, `part-finishing-orders`, `gear-assembly-orders`, `motor-assembly-orders`

#### Inventory
```bash
GET /api/inventory?workstationId={wsId}
POST /api/stock/adjust  # Body: {workstationId, itemType, itemId, delta, reason}
```

### Troubleshooting

**401 Unauthorized:**
1. Check `Authorization: Bearer <token>` header
2. Verify JWT secret matches across services
3. Token expires in 1hr – re-login

**404 Not Found:**
1. Check gateway route exists in `api-gateway/application.properties`
2. Verify service is running: `docker-compose ps`
3. Check nginx logs: `docker-compose logs -f nginx-root-proxy`

**Stock not updating:**
1. Confirm using `/complete` endpoint (not PUT confirmation)
2. Check inventory-service logs: `docker-compose logs -f inventory-service | grep credit`
3. Verify workstation ID matches

### Order Status Flow
```
CustomerOrder: PENDING → CONFIRMED → FULFILLED/PROCESSING → COMPLETED
WarehouseOrder: PENDING → CONFIRMED → AWAITING_PRODUCTION/FULFILLED
ProductionOrder: PENDING → SCHEDULED → IN_PROGRESS → COMPLETED
WorkstationOrder: PENDING → IN_PROGRESS → COMPLETED
```

### Configuration (OrderProcessingConfig)
```java
config.getThresholds().getLotSizeThreshold();      // Default: 3
config.getWorkstations().getPlantWarehouse();      // 7L
config.getWorkstations().getModulesSupermarket();  // 8L
```

### Deployment
```bash
./push-to-registry.sh --all                        # Build & push to 192.168.1.237:5000
./update-from-registry.sh                          # On server: pull & restart
docker-compose build --no-cache <service> && docker-compose up -d <service>  # Rebuild single service
```
