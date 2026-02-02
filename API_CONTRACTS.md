# API Contracts Documentation

**Last Updated:** February 2, 2026  
**Phase:** Phase 1 Step 1.1 - API Contract Audit Complete

This document defines all external API contracts in the LIFE (LEGO Integrated Factory Execution) system. All DTOs marked with `@ApiContract` annotation are documented here with their field requirements, consumers, and versioning policies.

---

## Table of Contents

1. [Overview](#overview)
2. [Contract Classification](#contract-classification)
3. [Breaking Change Protocol](#breaking-change-protocol)
4. [Frontend API Contracts](#frontend-api-contracts)
5. [Cross-Service API Contracts](#cross-service-api-contracts)
6. [Field Mapping Registry](#field-mapping-registry)
7. [Versioning Strategy](#versioning-strategy)

---

## Overview

**Total API Contracts:** 23 DTOs across 5 microservices

| Service | Frontend APIs | Cross-Service APIs | Total |
|---------|--------------|-------------------|-------|
| order-processing-service | 9 | 5 | 14 |
| user-service | 3 | 0 | 3 |
| inventory-service | 1 | 1 | 2 |
| masterdata-service | 0 | 2 | 2 |
| simal-integration-service | 1 | 1 | 2 |
| **TOTAL** | **14** | **9** | **23** |

---

## Contract Classification

### Frontend API Contracts (16 DTOs)
Consumed by React frontend via `/api/*` endpoints through nginx → api-gateway.

**Authentication & Session:**
- `LoginRequest` - POST /api/auth/login
- `LoginResponse` - JWT token response
- `UserDto` - User profile with session data

**Order Management:**
- `CustomerOrderDTO` - Customer orders (WS-7)
- `WarehouseOrderDTO` - Warehouse orders (WS-8)
- `FinalAssemblyOrderDTO` - Final assembly (WS-6)
- `ProductionOrderDTO` - Production orders with SimAL
- `SupplyOrderDTO` - Parts supply (WS-9)
- `ProductionControlOrderDTO` - Manufacturing control
- `AssemblyControlOrderDTO` - Assembly control
- `SystemConfigurationDTO` - Admin configuration

**Data Display:**
- `OrderItemDTO` - Order line items
- `StockRecordDto` - Inventory levels
- `ScheduledTaskResponse` - Gantt chart tasks

### Cross-Service API Contracts (7 DTOs)
Consumed by microservices via internal Docker DNS.

**Masterdata Service → Order Service:**
- `ProductDto` (SOURCE) → `ProductDTO` (CLIENT)
- `ModuleDto` (SOURCE) → `ModuleDTO` (CLIENT)
- `BomEntryDTO` - BOM relationships

**Order Service → Inventory Service:**
- `StockAdjustmentRequest` - Credit/debit operations

**Order Service → SimAL Service:**
- `SimalProductionOrderRequest` - Production scheduling

**Cross-Service Queries:**
- `PartDTO` - Part master data
- `WorkstationDTO` - Workstation metadata

---

## Breaking Change Protocol

When modifying any DTO marked with `@ApiContract`:

### 1. Dual Field Support (2 Release Cycles)
```java
// OLD field name
private String oldFieldName;

// NEW field name
private String newFieldName;

// Support BOTH during transition
@JsonProperty("oldFieldName")
public void setOldFieldName(String value) {
    log.warn("DEPRECATED: oldFieldName used, migrate to newFieldName");
    this.newFieldName = value;
}
```

### 2. Documentation Requirements
- Update this API_CONTRACTS.md with migration guide
- Add deprecation notice in DTO Javadoc
- Set `deprecated = true` in @ApiContract annotation
- Create Jira ticket for cleanup in 2 releases

### 3. Notification Process
- Notify all consuming services via team channel
- Update Swagger/OpenAPI documentation
- Add migration example in API docs
- Include in release notes

### 4. Validation
- Integration tests must pass with BOTH old and new field names
- Monitor logs for deprecation warnings
- Track usage metrics before removal

---

## Frontend API Contracts

### 1. Authentication & User Management

#### 1.1 LoginRequest
**Endpoint:** `POST /api/auth/login`  
**Service:** user-service  
**Consumer:** frontend  

**Contract:**
```json
{
  "username": "string (required, not blank)",
  "password": "string (required, not blank)"
}
```

**Field Requirements:**
- `username`: User's login name (validates against User table)
- `password`: Plain text password (hashed on server with BCrypt)

**Validation:**
- Both fields required
- Username case-sensitive
- Password minimum length enforced by UserRegistrationRequest

---

#### 1.2 LoginResponse
**Endpoint:** `POST /api/auth/login` response  
**Service:** user-service  
**Consumer:** frontend  

**Contract:**
```json
{
  "token": "string (JWT)",
  "tokenType": "string (always 'Bearer')",
  "expiresAt": "ISO 8601 timestamp",
  "user": {
    "id": "number",
    "username": "string",
    "role": "enum (UserRole)",
    "workstationId": "number | null",
    "workstation": "WorkstationDto | null"
  }
}
```

**Field Requirements:**
- `token`: JWT signed with SECURITY_JWT_SECRET (default 1h expiry)
- `user.workstationId`: **FLAT FIELD** for easy frontend access (not nested)
- `user.role`: One of: ADMIN, PLANT_WAREHOUSE, MODULES_SUPERMARKET, etc.

**Frontend Usage:**
```javascript
// Store token
localStorage.setItem('authToken', response.token);

// Access workstation directly (no nested navigation)
const wsId = response.user.workstationId; // ✅ Correct
// NOT: response.user.workstation.id       // ❌ Wrong
```

---

#### 1.3 UserDto
**Endpoints:** 
- `GET /api/users/me` - Current user
- `GET /api/users` - User list (ADMIN)

**Service:** user-service  
**Consumer:** frontend  

**Contract:**
```json
{
  "id": "number",
  "username": "string",
  "role": "enum (UserRole)",
  "workstationId": "number | null",
  "workstation": {
    "id": "number",
    "name": "string"
  }
}
```

**Field Requirements:**
- `workstationId`: Duplicate of workstation.id for flat access
- `role`: Maps to dashboard routing (see workstationConfig.js)
- `workstation`: May be null if no workstation assigned

---

### 2. Order Management APIs

#### 2.1 OrderItemDTO
**Usage:** Nested in CustomerOrderDTO, WarehouseOrderDTO  
**Service:** order-processing-service  
**Consumer:** frontend  

**Contract:**
```json
{
  "id": "number",
  "itemId": "number",
  "itemName": "string",
  "itemType": "enum (PRODUCT, MODULE, PART)",
  "quantity": "number",
  "requestedQuantity": "number",
  "fulfilledQuantity": "number"
}
```

**CRITICAL Field Mapping:**
- Frontend sends: `requestedQuantity`
- Backend expects: `quantity` (internal field name)
- **Solution:** `@JsonProperty` setter maps requestedQuantity → quantity

```java
@JsonProperty("requestedQuantity")
public void setRequestedQuantity(Integer value) {
    this.quantity = value;
}
```

**Why This Mapping Exists:**
- Historical: Backend initially used `quantity` for internal processing
- Frontend convention: Always use `requestedQuantity` for consistency
- **Resolution:** Defensive setter supports BOTH field names

---

#### 2.2 CustomerOrderDTO
**Endpoints:**
- `GET /api/customer-orders` - List orders
- `POST /api/customer-orders` - Create order
- `PUT /api/customer-orders/{id}` - Confirm order
- `POST /api/customer-orders/{id}/complete` - Complete order

**Service:** order-processing-service  
**Consumer:** frontend (Plant Warehouse dashboard, WS-7)  

**Contract:**
```json
{
  "id": "number",
  "orderNumber": "string (CO-XXX)",
  "status": "enum (PENDING, CONFIRMED, PROCESSING, COMPLETED)",
  "triggerScenario": "enum | null",
  "orderItems": [OrderItemDTO],
  "totalAmount": "number",
  "createdAt": "ISO 8601 timestamp",
  "updatedAt": "ISO 8601 timestamp"
}
```

**Trigger Scenario Values:**
- `null` - Not yet confirmed
- `DIRECT_FULFILLMENT` - Stock available at WS-7
- `WAREHOUSE_ORDER_NEEDED` - Needs Warehouse Order (WS-8)
- `DIRECT_PRODUCTION` - Qty ≥ LOT_SIZE_THRESHOLD, bypass warehouse

**Workflow States:**
1. `PENDING` → User clicks "Confirm"
2. Backend sets `triggerScenario` based on stock check
3. `CONFIRMED` → User sees action button based on scenario
4. `PROCESSING` → Order executing (warehouse/production)
5. `COMPLETED` → Final fulfillment done

---

#### 2.3 WarehouseOrderDTO
**Endpoints:**
- `GET /api/warehouse-orders`
- `PUT /api/warehouse-orders/{id}` - Confirm
- `POST /api/warehouse-orders/{id}/fulfill` - Fulfill
- `POST /api/warehouse-orders/{id}/production-order` - Create production

**Service:** order-processing-service  
**Consumer:** frontend (Modules Supermarket dashboard, WS-8)  

**Contract:**
```json
{
  "id": "number",
  "orderNumber": "string (WO-XXX)",
  "customerOrderId": "number",
  "productionOrderId": "number | null",
  "status": "enum",
  "triggerScenario": "enum | null",
  "orderItems": [WarehouseOrderItemDTO],
  "createdAt": "ISO 8601 timestamp"
}
```

**CRITICAL Field: productionOrderId**
- Purpose: Links warehouse order to its production order
- Prevents: Multiple warehouse orders fighting over same modules
- Fulfillment Logic: Orders with `productionOrderId != null` BYPASS stock checks
- Reason: Modules are "reserved" for this specific warehouse order

**Why This Matters:**
```
WO-1: Confirms → Stock insufficient → Creates Production → productionOrderId = 1
(Production starts manufacturing modules...)
WO-2: Arrives → Confirms → Sees modules in WS-8 (from WO-1's production)
Without productionOrderId check: WO-2 would "steal" WO-1's reserved modules
With productionOrderId check: WO-2 correctly determines it ALSO needs production
```

---

#### 2.4 FinalAssemblyOrderDTO
**Endpoints:**
- `GET /api/final-assembly-orders`
- `POST /api/final-assembly-orders/{id}/start`
- `POST /api/final-assembly-orders/{id}/complete`

**Service:** order-processing-service  
**Consumer:** frontend (Final Assembly dashboard, WS-6)  

**Contract:**
```json
{
  "id": "number",
  "orderNumber": "string (FA-XXX)",
  "warehouseOrderId": "number | null",
  "assemblyControlOrderId": "number | null",
  "workstationId": 6,
  "outputProductId": "number",
  "outputQuantity": "number",
  "status": "enum",
  "startTime": "ISO 8601 | null",
  "completionTime": "ISO 8601 | null"
}
```

**CRITICAL Field: outputProductId**
- Type: PRODUCT ID (1-4), NOT module ID (7+)
- Purpose: Credits Plant Warehouse (WS-7) with correct product
- Common Error: Using module ID instead of product ID
- Validation: Must match ProductDto.id from masterdata-service

---

#### 2.5 ProductionOrderDTO
**Endpoints:**
- `GET /api/production-orders`
- `GET /api/production-orders/{id}` - With control orders

**Service:** order-processing-service  
**Consumer:** frontend (Production Planning dashboard)  

**Contract:**
```json
{
  "id": "number",
  "productionOrderNumber": "string (PO-XXX)",
  "sourceCustomerOrderId": "number | null",
  "sourceWarehouseOrderId": "number | null",
  "simalScheduleId": "string",
  "status": "enum (PENDING, SCHEDULED, IN_PROGRESS, COMPLETED)",
  "priority": "enum (LOW, NORMAL, HIGH, URGENT)",
  "triggerScenario": "enum (SCENARIO_3, SCENARIO_4)",
  "createdAt": "ISO 8601 timestamp",
  "actualCompletionTime": "ISO 8601 | null"
}
```

**Source Order Fields:**
- Scenario 3: Has `sourceWarehouseOrderId`
- Scenario 4: Has `sourceCustomerOrderId`
- Mutually exclusive: Only ONE source field populated

**Status Progression:**
1. `PENDING` → Created, not yet scheduled
2. `SCHEDULED` → SimAL generated schedule, control orders created
3. `IN_PROGRESS` → At least one control order started
4. `COMPLETED` → ALL control orders completed (auto-completes)

---

#### 2.6 SupplyOrderDTO
**Endpoints:**
- `GET /api/supply-orders?targetWorkstationId={wsId}`
- `POST /api/supply-orders/{id}/fulfill`

**Service:** order-processing-service  
**Consumer:** frontend (Parts Supply dashboard, WS-9)  

**Contract:**
```json
{
  "id": "number",
  "supplyOrderNumber": "string (SO-XXX)",
  "sourceControlOrderId": "number",
  "sourceControlOrderType": "enum (PRODUCTION, ASSEMBLY)",
  "requestingWorkstationId": "number",
  "supplyWarehouseWorkstationId": 9,
  "status": "enum (PENDING, FULFILLED)",
  "supplyOrderItems": [SupplyOrderItemDTO],
  "priority": "enum"
}
```

**Gating Logic:**
- Workstation orders CANNOT start until supply order FULFILLED
- Supply fulfillment debits Parts Supply Warehouse (WS-9)
- Prevents workstations from starting without required materials

---

#### 2.7 ProductionControlOrderDTO & AssemblyControlOrderDTO
**Endpoints:**
- `GET /api/production-control-orders?productionOrderId={id}`
- `GET /api/assembly-control-orders?productionOrderId={id}`

**Service:** order-processing-service  
**Consumer:** frontend (Production/Assembly Control dashboards)  

**Contract (both similar):**
```json
{
  "id": "number",
  "controlOrderNumber": "string",
  "sourceProductionOrderId": "number",
  "assignedWorkstationId": "number | null",
  "simalScheduleId": "string",
  "status": "enum (PENDING, ASSIGNED, IN_PROGRESS, COMPLETED)",
  "targetStartTime": "ISO 8601",
  "targetCompletionTime": "ISO 8601",
  "actualCompletionTime": "ISO 8601 | null"
}
```

**Auto-Completion Logic:**
- ProductionControlOrder: Completes when ALL workstation orders (WS-1,2,3) COMPLETED
- AssemblyControlOrder: Completes when ALL workstation orders (WS-4,5,6) COMPLETED
- No manual submission needed (orchestration service handles)

---

#### 2.8 SystemConfigurationDTO
**Endpoints:**
- `GET /api/config/lot-size-threshold`
- `PUT /api/config/lot-size-threshold` (ADMIN only)

**Service:** order-processing-service  
**Consumer:** frontend (Admin dashboard)  

**Contract:**
```json
{
  "id": "number",
  "configKey": "string (LOT_SIZE_THRESHOLD)",
  "configValue": "string",
  "valueType": "string (INTEGER)",
  "description": "string",
  "editable": "boolean",
  "updatedBy": "string",
  "updatedAt": "ISO 8601 timestamp"
}
```

**Key Configuration: LOT_SIZE_THRESHOLD**
- Default: 3
- Purpose: Trigger Scenario 4 (direct production)
- Logic: If total order quantity ≥ threshold, bypass Modules Supermarket

---

#### 2.9 StockRecordDto
**Endpoint:** `GET /api/inventory?workstationId={wsId}`  
**Service:** inventory-service  
**Consumer:** frontend (all workstation dashboards)  

**Contract:**
```json
{
  "id": "number",
  "workstationId": "number (1-9)",
  "itemType": "enum (PRODUCT, MODULE, PART)",
  "itemId": "number",
  "itemName": "string",
  "quantity": "number",
  "lastUpdated": "ISO 8601 timestamp"
}
```

**Workstation Isolation:**
- Each workstation has independent inventory
- No shared stock pools
- Query by workstationId to get specific location's stock

---

#### 2.10 ScheduledTaskResponse
**Endpoint:** `GET /api/simal/schedules?productionOrderId={id}`  
**Service:** simal-integration-service  
**Consumer:** frontend (Production Planning Gantt chart)  

**Contract:**
```json
{
  "taskId": "string",
  "itemId": "string",
  "itemName": "string",
  "quantity": "number",
  "workstationId": "string",
  "workstationName": "string",
  "startTime": "ISO 8601 timestamp",
  "endTime": "ISO 8601 timestamp",
  "duration": "number (minutes)",
  "status": "enum (PENDING, IN_PROGRESS, COMPLETED)",
  "sequence": "number",
  "manuallyAdjusted": "boolean",
  "adjustedBy": "string | null",
  "adjustmentReason": "string | null"
}
```

**Gantt Chart Usage:**
- Tasks displayed in chronological order by startTime
- Color coding by status (pending/in-progress/completed)
- Manual adjustment tracking for production planning

---

## Cross-Service API Contracts

### 3. Masterdata Service (SOURCE) → Order Service (CLIENT)

#### 3.1 ProductDto (SOURCE)
**Endpoint:** `GET /api/masterdata/products/{id}`  
**Service:** masterdata-service (SOURCE)  
**Consumer:** order-processing-service (CLIENT)  

**SOURCE Contract:**
```json
{
  "id": "number",
  "name": "string",
  "description": "string",
  "price": "number (double)",
  "estimatedTimeMinutes": "number (integer)"
}
```

**CLIENT Mapping:**
```java
// order-processing-service/dto/masterdata/ProductDTO.java
// Field names MUST match SOURCE exactly
private Long id;              // ✅ Matches
private String name;          // ✅ Matches
private String description;   // ✅ Matches
private Double price;         // ✅ Matches
private Integer estimatedTimeMinutes;  // ✅ Matches
```

**Validation:**
- No @JsonProperty needed - field names identical
- Client DTO updated via MasterdataClient RestTemplate calls
- Cache: Gateway caches masterdata responses (10min TTL)

---

#### 3.2 ModuleDto (SOURCE)
**Endpoint:** `GET /api/masterdata/products/{id}/modules`  
**Service:** masterdata-service (SOURCE)  
**Consumer:** order-processing-service (CLIENT)  

**SOURCE Contract:**
```json
{
  "id": "number",
  "name": "string",
  "description": "string",
  "type": "string",
  "productionWorkstationId": "number"
}
```

**CLIENT Mapping:**
```java
// order-processing-service/dto/masterdata/ModuleDTO.java
private Long id;
private String name;
private String description;
private String type;
private Integer productionWorkstationId;
```

**Cross-Reference:**
- SOURCE: io.life.masterdata.dto.ModuleDto
- CLIENT: io.life.order.dto.masterdata.ModuleDTO
- Field names match exactly - no mapping needed

---

#### 3.3 BomEntryDTO
**Endpoint:** `GET /api/masterdata/products/{id}/modules` response format  
**Service:** masterdata-service  
**Consumer:** order-processing-service  

**Contract:**
```json
{
  "componentId": "number",
  "componentName": "string",
  "componentType": "enum (MODULE, PART)",
  "quantity": "number"
}
```

**CRITICAL Field: componentId**
- Not `moduleId` - generic term for BOM components
- Can be module ID OR part ID depending on context
- ProductModuleDTO in MasterdataService was previously fixed to handle this:

```java
private Long moduleId;
private Long componentId;

public Long getModuleId() {
    return moduleId != null ? moduleId : componentId;
}
```

---

### 4. Order Service → Inventory Service

#### 4.1 StockAdjustmentRequest
**Endpoint:** `POST /api/stock/adjust`  
**Service:** inventory-service  
**Consumer:** order-processing-service (via InventoryClient)  

**Contract:**
```json
{
  "workstationId": "number (1-9, required)",
  "itemType": "enum (PRODUCT, MODULE, PART, required)",
  "itemId": "number (required)",
  "delta": "number (signed integer, required)",
  "reasonCode": "string (enum-like, required)",
  "notes": "string (optional)"
}
```

**Field Requirements:**
- `delta`: Signed integer (positive = credit, negative = debit)
- `reasonCode`: One of: FULFILLMENT, PRODUCTION, CONSUMPTION, ADJUSTMENT, RETURN
- `workstationId`: Must be valid workstation (1-9)

**Usage Pattern:**
```java
// Credit stock (add inventory)
inventoryClient.creditStock(
    workstationId: 8L,
    itemType: "MODULE",
    itemId: 7L,
    quantity: 10,
    reason: "PRODUCTION"
);

// Debit stock (remove inventory)
inventoryClient.debitStock(
    workstationId: 7L,
    itemType: "PRODUCT",
    itemId: 1L,
    quantity: 5,
    reason: "FULFILLMENT"
);
```

---

### 5. Order Service → SimAL Service

#### 5.1 SimalProductionOrderRequest
**Endpoint:** `POST /api/simal/schedules`  
**Service:** simal-integration-service  
**Consumer:** order-processing-service  

**Contract:**
```json
{
  "orderNumber": "string (PO-XXX, required)",
  "customerName": "string (optional)",
  "dueDate": "string (ISO 8601 date, optional)",
  "priority": "enum (LOW, NORMAL, HIGH, required)",
  "lineItems": [
    {
      "itemId": "number",
      "itemName": "string",
      "quantity": "number",
      "estimatedTimeMinutes": "number"
    }
  ],
  "notes": "string (optional)"
}
```

**SimAL Integration Flow:**
1. Order service creates ProductionOrder
2. Calls simal-service with SimalProductionOrderRequest
3. SimAL generates optimized schedule
4. Creates ProductionControlOrder and AssemblyControlOrder
5. Returns ScheduledTaskResponse list for Gantt chart

---

## Field Mapping Registry

### Active Field Mappings

| DTO | API Field | Internal Field | Mapping Method | Reason |
|-----|-----------|---------------|----------------|--------|
| OrderItemDTO | requestedQuantity | quantity | @JsonProperty setter | Frontend convention vs backend naming |
| ProductModuleDTO | componentId | moduleId | Defensive getter | Generic BOM term vs specific module concept |
| UserDto | workstationId | (flat field) | Duplicate field | Frontend ease of access (no nesting) |

### Deprecated Mappings
None currently. When fields are deprecated, they will be documented here with:
- Deprecation date
- Migration deadline (2 releases)
- Replacement field name
- Migration example

---

## Versioning Strategy

### Current Version: v1
All API contracts are currently at version 1. No breaking changes have occurred since initial release.

### Version Increment Triggers
Increment version when:
1. **Field removed** without backward-compatible mapping
2. **Field type changed** (String → Number, etc.)
3. **Required field added** (breaks existing clients)
4. **Enum values removed** (breaks validation)

### Version Maintenance
- v1 contracts supported indefinitely (stable API)
- v2 contracts will be introduced if breaking changes needed
- Clients specify version via Accept header or URL path
- Gateway routes to appropriate service version

### Backward Compatibility Guarantee
- All v1 contracts will remain available for 2+ years
- Deprecation notices given 6 months in advance
- Migration guides provided for all breaking changes
- Old and new contracts supported simultaneously during transition

---

## API Contract Validation

### Automated Validation (Future)
- Integration tests verify field names match across SOURCE ↔ CLIENT
- Contract tests ensure response structure matches documentation
- Property-based tests validate edge cases

### Manual Validation Checklist
Before deploying DTO changes:
- [ ] All @ApiContract annotations reviewed
- [ ] Field name changes have @JsonProperty mappings
- [ ] Cross-service DTOs match SOURCE exactly
- [ ] Frontend developers notified of changes
- [ ] API_CONTRACTS.md updated
- [ ] Integration tests passing
- [ ] Swagger docs regenerated

---

## Contact & Support

**Questions about API contracts?**
- Check this document first
- Review DTO @ApiContract annotations in code
- Check PRE_COMMIT_CHECKLIST.md for best practices
- Contact: Backend Team Lead

**Reporting API Contract Issues:**
1. Create Jira ticket: `[API-CONTRACT] - Description`
2. Include: DTO name, field name, expected vs actual behavior
3. Tag: backend-team, api-contracts
4. Priority: CRITICAL if production-blocking

---

**Document Version:** 1.0  
**Generated:** February 2, 2026  
**Next Review:** After Phase 1 Step 1.2 (Exception Handling)
