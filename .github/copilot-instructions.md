# LIFE System – Copilot Instructions

> **Last Updated:** January 29, 2026

## Quick Start

```bash
docker-compose up -d                    # Start all services (nginx:1011 + 6 microservices)
docker-compose logs -f api-gateway      # Debug routing/auth issues
cd lego-factory-frontend && npm run dev # Frontend dev with HMR on :5173
docker-compose build --no-cache <service> && docker-compose up -d <service>  # Rebuild after changes
./test-scenario-2.sh                    # Validate business flow
```

## Architecture

```
nginx:1011 → api-gateway:8011 → {user:8012, masterdata:8013, inventory:8014, order-processing:8015, simal:8016}
```

- **All traffic via gateway** – never bypass to services directly
- **Inter-service REST** – use Docker DNS (`http://inventory-service:8014`), not localhost
- **Isolated H2 DBs** – each service seeds via `DataInitializer`; data resets on restart
- **JWT** – sync `SECURITY_JWT_SECRET` across `.env`, api-gateway, and user-service

## Domain Model

**3-tier BOM:** Products (WS-7) → Modules (WS-8) → Parts (WS-9)

**Order flow with stock credit points:**
```
CustomerOrder (WS-7) → WarehouseOrder (WS-8) → ProductionOrder
  WS-1/2/3 complete → credit WS-9 | WS-4/5 complete → credit WS-8 | WS-6 complete → credit WS-7
```

**Critical rule:** Stock credits/debits happen ONLY on `POST .../complete` endpoints, never on confirmation.

**triggerScenario field** (set during confirm, drives UI buttons):
- `DIRECT_FULFILLMENT` → "Fulfill" button
- `WAREHOUSE_ORDER_NEEDED` → "Process" button  
- `PRODUCTION_REQUIRED` → "Order Production" button

## Standard Button Sequence Flow

| Order Type | Button Flow |
|------------|-------------|
| CustomerOrder | Confirm → Fulfill/Process → Complete |
| WarehouseOrder | Confirm → Fulfill/Order Production |
| ProductionOrder | Confirm → Schedule → Submit → Complete |
| FinalAssemblyOrder | Confirm → Start → Complete → Submit |
| ControlOrders | Confirm → Request Parts → Dispatch → Start → Complete |
| SupplyOrders | Confirm → Fulfill/Reject |
| WorkstationOrders (WS-1 to WS-5) | Start → Complete |

**ALL order types follow the Confirm → [Operations] → Complete pattern.**

## Inventory API

**Stock adjustments use `/api/stock/adjust`** with `StockAdjustmentRequest`:
```java
{
  "workstationId": 8,          // Target workstation (WS-8 for modules, WS-9 for parts)
  "itemType": "MODULE",        // "MODULE" or "PART"
  "itemId": 123,               // Module or Part ID
  "delta": 5,                  // Positive=credit, Negative=debit
  "reasonCode": "PRODUCTION",  // PRODUCTION, ADJUSTMENT, RECEIPT, CONSUMPTION
  "notes": "Completed order: WS-ORD-001"
}
```

**Stock credit points:**
- WS-1/2/3 complete → credit Parts Supply (WS-9) with PART
- WS-4/5 complete → credit Modules Supermarket (WS-8) with MODULE
- WS-6 (Final Assembly) complete → credit Plant Warehouse (WS-7) with PRODUCT

## Backend Patterns

- **Packages:** `io.life.<service>/{controller,service,entity,dto,repository,exception}`
- **New endpoints require:** controller + service + gateway route in [api-gateway/application.properties](lego-factory-backend/api-gateway/src/main/resources/application.properties)
- **HTTP verbs:** GET=read, POST=create/actions, PUT=status changes, PATCH=notes
- **RestTemplate for inter-service:** `restTemplate.postForObject("http://inventory-service:8014/api/...")`
- **Entities:** Use `@PrePersist/@PreUpdate` for timestamps; validate state transitions in service layer

## Frontend Patterns

- **Dashboard routing:** [DashboardPage.jsx](lego-factory-frontend/src/pages/DashboardPage.jsx) uses `session?.user?.workstationId` (flat, not nested)
- **9 workstation dashboards** in [src/pages/dashboards/](lego-factory-frontend/src/pages/dashboards/) – each wraps with `StandardDashboardLayout`
- **Order hooks:** `useWorkstationOrders(workstationId)` for manufacturing/assembly dashboards
- **Components:** Import from [src/components/index.js](lego-factory-frontend/src/components/index.js) – never inline styles
- **API calls:** Relative paths (`/api/...`); axios interceptor injects JWT

## Key Files

| Purpose | File |
|---------|------|
| Gateway routes | [api-gateway/application.properties](lego-factory-backend/api-gateway/src/main/resources/application.properties) |
| User roles | [UserRole.java](lego-factory-backend/user-service/src/main/java/io/life/user_service/entity/UserRole.java) |
| Workstation config | [workstationConfig.js](lego-factory-frontend/src/config/workstationConfig.js) |
| Order card example | [WarehouseOrderCard.jsx](lego-factory-frontend/src/components/WarehouseOrderCard.jsx) |
| Workstation card | [WorkstationOrderCard.jsx](lego-factory-frontend/src/components/WorkstationOrderCard.jsx) |
| Inventory API | [StockLedgerController.java](lego-factory-backend/inventory-service/src/main/java/io/life/inventory/controller/StockLedgerController.java) |

## Debugging Quick Reference

| Symptom | Check |
|---------|-------|
| 401 Unauthorized | JWT token in header? `SECURITY_JWT_SECRET` synced across `.env`, gateway, user-service? |
| 404 on API | Route exists in [api-gateway/application.properties](lego-factory-backend/api-gateway/src/main/resources/application.properties)? |
| Stock not updating | Using `POST .../complete` (not PUT confirm)? Check inventory-service logs |
| Service won't start | `docker-compose logs -f <service>` for JPA/port errors; rebuild with `--no-cache` |
| H2 data "lost" | Expected – H2 is in-memory; check `DataInitializer` for seed data |

## Deployment (Registry)

```bash
# Dev machine: build and push to local registry
./push-to-registry.sh                   # Tags: {branch}-{commit}-{timestamp}

# Live server: pull and deploy
./update-from-registry.sh && docker-compose up -d

# Diagnose live issues
./check-server-state.sh
```

**Registry:** `192.168.1.237:5000` – images get both `latest` and versioned tags for rollback

## Critical Pitfalls

1. **Stock timing** – credit/debit on completion only, stock check DURING confirm
2. **ProductId tracking** – `WarehouseOrderItem.productId` must flow to `FinalAssemblyOrder.outputProductId`
3. **Docker hostnames** – use service names inside containers, localhost outside
4. **JWT sync** – update all 3 locations when changing secret
5. **Gateway routes** – every new endpoint needs a route added
