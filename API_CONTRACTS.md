# API Contracts Documentation

**Last Updated:** February 2, 2026  
**Phase:** Phase 1 Step 1.1 - API Contract Audit Complete

This document defines all external API contracts in the LIFE (LEGO Integrated Factory Execution) system. All DTOs marked with `@ApiContract` annotation are documented here with their field requirements, consumers, and versioning policies.

---

## Table of Contents

1. [Overview](#overview)
2. [Contract Classification](#contract-classification)
3. [Error Handling](#error-handling)
4. [Breaking Change Protocol](#breaking-change-protocol)
5. [Frontend API Contracts](#frontend-api-contracts)
6. [Cross-Service API Contracts](#cross-service-api-contracts)
7. [Field Mapping Registry](#field-mapping-registry)
8. [Versioning Strategy](#versioning-strategy)

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
---

## Error Handling

**Last Updated:** February 2, 2026 (Phase 1 Step 1.2)

All services return consistent error responses using the `ApiErrorResponse` DTO with machine-readable error codes and debugging context.

### ApiErrorResponse Structure

```json
{
  "timestamp": "2026-02-02T04:19:09.123456",
  "status": 404,
  "error": "Not Found",
  "errorCode": "ORDER_NOT_FOUND",
  "message": "Order with number ORD-12345 not found",
  "path": "/api/customer-orders/123",
  "details": {
    "orderNumber": "ORD-12345",
    "orderId": 123,
    "requestedBy": "warehouse_operator"
  }
}
```

**Fields:**
- `timestamp` (LocalDateTime) - When error occurred (ISO 8601 format)
- `status` (int) - HTTP status code (404, 400, 500, etc.)
- `error` (String) - HTTP status reason phrase
- `errorCode` (String) - Machine-readable error code (see table below)
- `message` (String) - Human-readable error description
- `path` (String) - Request URI that caused the error
- `details` (Map<String, Object>) - Debugging context (optional)

### Error Code Reference

All error codes follow the format: `SERVICE_DOMAIN_ERROR`

#### order-processing-service (5 codes)

| Error Code | HTTP Status | Description | Context Fields | Example Scenario |
|-----------|------------|-------------|----------------|------------------|
| `ORDER_NOT_FOUND` | 404 | Order does not exist in database | `orderNumber`, `orderId`, `orderType` | GET /api/customer-orders/999 |
| `ORDER_INVALID_STATE` | 400 | Invalid status transition attempted | `currentState`, `requestedState`, `orderNumber` | Trying to complete a PENDING order |
| `ORDER_INVALID_OPERATION` | 400 | Operation not allowed for this order | `operation`, `reason`, `orderNumber` | Confirming an already confirmed order |
| `ORDER_INSUFFICIENT_STOCK` | 400 | Not enough inventory to fulfill | `required`, `available`, `shortage`, `itemId` | Order quantity exceeds stock |
| `ORDER_PRODUCTION_PLANNING_ERROR` | 500 | SimAL scheduling or production error | `productionOrderId`, `reason`, `failedOperation` | SimAL service unavailable |

#### inventory-service (6 codes)

| Error Code | HTTP Status | Description | Context Fields | Example Scenario |
|-----------|------------|-------------|----------------|------------------|
| `INVENTORY_NOT_FOUND` | 404 | Stock record not found | `resourceName`, `fieldName`, `fieldValue`, `workstationId` | GET /api/stock?workstationId=99 |
| `INVENTORY_VALIDATION_ERROR` | 400 | Invalid request parameters | `field`, `invalidValue`, `reason` | Negative quantity in adjust request |
| `INVENTORY_UNAUTHORIZED` | 403 | User lacks permission | `requiredRole`, `userRole`, `operation` | Non-admin trying to adjust stock |
| `INVENTORY_ENDPOINT_NOT_FOUND` | 404 | API route does not exist | `path`, `method` | GET /api/nonexistent |
| `INVENTORY_SERVICE_ERROR` | 500 | General service error | `operation`, `reason` | Database connection failure |
| `INVENTORY_INTERNAL_ERROR` | 500 | Unexpected error | `exception`, `stackTrace` | NullPointerException |

#### masterdata-service (6 codes)

| Error Code | HTTP Status | Description | Context Fields | Example Scenario |
|-----------|------------|-------------|----------------|------------------|
| `MASTERDATA_NOT_FOUND` | 404 | Product, module, or part not found | `resourceName`, `fieldName`, `fieldValue` | GET /api/masterdata/products/999 |
| `MASTERDATA_VALIDATION_ERROR` | 400 | Invalid request data | `field`, `invalidValue`, `reason` | Invalid product ID format |
| `MASTERDATA_UNAUTHORIZED` | 403 | Permission denied | `requiredRole`, `userRole`, `operation` | Non-admin modifying masterdata |
| `MASTERDATA_ENDPOINT_NOT_FOUND` | 404 | Route not found | `path`, `method` | POST /api/invalid-endpoint |
| `MASTERDATA_SERVICE_ERROR` | 500 | Service-level error | `operation`, `reason` | Cache invalidation failure |
| `MASTERDATA_INTERNAL_ERROR` | 500 | Unhandled exception | `exception`, `message` | Unexpected runtime error |

#### user-service (8 codes)

| Error Code | HTTP Status | Description | Context Fields | Example Scenario |
|-----------|------------|-------------|----------------|------------------|
| `USER_NOT_FOUND` | 404 | User account not found | `resourceName`, `fieldName`, `fieldValue` | GET /api/users/999 |
| `USER_VALIDATION_ERROR` | 400 | Invalid user data | `field`, `invalidValue`, `reason` | Username contains special chars |
| `USER_UNAUTHORIZED` | 403 | Authentication failed | `username`, `reason` | Invalid credentials |
| `USER_ENDPOINT_NOT_FOUND` | 404 | API route not found | `path`, `method` | DELETE /api/invalid |
| `USER_ENTITY_NOT_FOUND` | 404 | JPA entity not found | `entityType`, `entityId` | User entity deleted during transaction |
| `USER_INVALID_ARGUMENT` | 400 | Business logic validation failed | `argument`, `reason` | Duplicate username |
| `USER_SERVICE_ERROR` | 500 | Service error | `operation`, `reason` | JWT generation failure |
| `USER_INTERNAL_ERROR` | 500 | Unexpected error | `exception`, `message` | Uncaught exception |

#### simal-integration-service (5 codes)

| Error Code | HTTP Status | Description | Context Fields | Example Scenario |
|-----------|------------|-------------|----------------|------------------|
| `SIMAL_NOT_FOUND` | 404 | Schedule or task not found | `resourceName`, `fieldName`, `fieldValue` | GET /api/simal/schedules/999 |
| `SIMAL_VALIDATION_ERROR` | 400 | Invalid scheduling parameters | `field`, `invalidValue`, `reason` | Invalid task duration |
| `SIMAL_UNAUTHORIZED` | 403 | Permission denied | `requiredRole`, `userRole`, `operation` | Non-planner creating schedule |
| `SIMAL_SERVICE_ERROR` | 500 | Scheduling algorithm error | `operation`, `reason`, `productionOrderId` | Gantt chart generation failed |
| `SIMAL_INTERNAL_ERROR` | 500 | Unexpected error | `exception`, `message` | SimAL algorithm crash |

### HTTP Status Code Mapping

| HTTP Status | Usage | Example Error Codes |
|------------|-------|-------------------|
| **400 Bad Request** | Invalid input, validation errors | `*_VALIDATION_ERROR`, `*_INVALID_STATE`, `*_INVALID_OPERATION`, `*_INSUFFICIENT_STOCK`, `*_INVALID_ARGUMENT` |
| **403 Forbidden** | Permission denied, unauthorized | `*_UNAUTHORIZED` |
| **404 Not Found** | Resource or endpoint not found | `*_NOT_FOUND`, `*_ENDPOINT_NOT_FOUND`, `*_ENTITY_NOT_FOUND` |
| **500 Internal Server Error** | Service errors, unexpected exceptions | `*_SERVICE_ERROR`, `*_INTERNAL_ERROR`, `*_PRODUCTION_PLANNING_ERROR` |

### Error Response Examples

**Example 1: Order Not Found**
```json
{
  "timestamp": "2026-02-02T10:30:45.123",
  "status": 404,
  "error": "Not Found",
  "errorCode": "ORDER_NOT_FOUND",
  "message": "Customer order with number ORD-ABC123 not found",
  "path": "/api/customer-orders/42",
  "details": {
    "orderNumber": "ORD-ABC123",
    "orderId": 42,
    "orderType": "CustomerOrder"
  }
}
```

**Example 2: Invalid Order State Transition**
```json
{
  "timestamp": "2026-02-02T10:31:12.456",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "ORDER_INVALID_STATE",
  "message": "Cannot transition order from COMPLETED to PENDING",
  "path": "/api/customer-orders/15",
  "details": {
    "currentState": "COMPLETED",
    "requestedState": "PENDING",
    "orderNumber": "ORD-XYZ789"
  }
}
```

**Example 3: Insufficient Stock**
```json
{
  "timestamp": "2026-02-02T10:32:33.789",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "ORDER_INSUFFICIENT_STOCK",
  "message": "Insufficient stock to fulfill order",
  "path": "/api/customer-orders/23/complete",
  "details": {
    "required": 50,
    "available": 15,
    "shortage": 35,
    "itemId": 1,
    "itemName": "LEGO Model Car",
    "workstationId": 7
  }
}
```

**Example 4: Unauthorized Access**
```json
{
  "timestamp": "2026-02-02T10:33:01.234",
  "status": 403,
  "error": "Forbidden",
  "errorCode": "USER_UNAUTHORIZED",
  "message": "User warehouse_operator does not have permission to access admin panel",
  "path": "/api/admin/config",
  "details": {
    "username": "warehouse_operator",
    "requiredRole": "ADMIN",
    "userRole": "PLANT_WAREHOUSE"
  }
}
```

### Frontend Error Handling

**Client-side error handling pattern:**

```javascript
try {
  const response = await api.post('/api/customer-orders', orderData);
  showSuccess('Order created successfully');
} catch (error) {
  if (error.response?.data?.errorCode) {
    // Machine-readable error code
    const { errorCode, message, details } = error.response.data;
    
    switch (errorCode) {
      case 'ORDER_INSUFFICIENT_STOCK':
        showWarning(`Not enough stock: ${details.shortage} units short`);
        break;
      case 'ORDER_INVALID_STATE':
        showError(`Invalid operation: ${message}`);
        break;
      default:
        showError(message || 'An error occurred');
    }
  } else {
    // Network error or unexpected format
    showError('Unable to connect to server');
  }
}
```

### Backend Exception Handling Pattern

**Service layer example:**

```java
// order-processing-service
public CustomerOrder confirmOrder(Long orderId) {
    CustomerOrder order = customerOrderRepository.findById(orderId)
        .orElseThrow(() -> new EntityNotFoundException("Customer order not found")
            .addDetail("orderId", orderId)
            .addDetail("operation", "confirmOrder"));
    
    if (!"PENDING".equals(order.getStatus())) {
        throw new InvalidOrderStateException(
            "Cannot confirm order in current state"
        )
            .addDetail("currentState", order.getStatus())
            .addDetail("requestedState", "CONFIRMED")
            .addDetail("orderNumber", order.getOrderNumber());
    }
    
    // ... business logic
}
```

**GlobalExceptionHandler extracts error codes:**

```java
@ExceptionHandler(InvalidOrderStateException.class)
public ResponseEntity<ApiErrorResponse> handleInvalidOrderState(
    InvalidOrderStateException ex,
    HttpServletRequest request
) {
    String errorCode = ex.getErrorCode(); // "ORDER_INVALID_STATE"
    Map<String, Object> details = ex.getDetails(); // {currentState, requestedState, orderNumber}
    
    logger.warn("Invalid order state: {} (Code: {})", ex.getMessage(), errorCode);
    
    ApiErrorResponse response = ApiErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .errorCode(errorCode)
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .details(details)
        .build();
    
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
}
```

### Best Practices

1. **Always include error codes**: Machine-readable codes enable programmatic error handling
2. **Provide context in details**: Include entity IDs, names, and relevant state for debugging
3. **Use appropriate HTTP statuses**: 400 for client errors, 500 for server errors
4. **Log errors with context**: Include error code and details in log statements
5. **Document error codes**: Update this table when adding new error codes
6. **Test error responses**: Verify error codes and details in integration tests
7. **Avoid sensitive data**: Never include passwords, tokens, or PII in error details
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

**Document Version:** 1.1  
**Generated:** February 2, 2026  
**Last Updated:** February 2, 2026 (Phase 1 Step 1.2 - Error Handling)  
**Next Review:** After Phase 1 Step 1.3 (Service Layer Refactoring)
