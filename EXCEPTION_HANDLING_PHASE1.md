# Phase 1 Step 1.2: Exception Handling Standardization

**Timeline**: Days 3-5 (February 2-4, 2026)  
**Status**: IN PROGRESS  
**Branch**: feature/phase1-exception-handling

## Objectives

Standardize exception handling across all 5 microservices to provide:
1. Consistent error response format (ApiErrorResponse)
2. Error codes for programmatic error handling
3. Context details for debugging
4. Proper HTTP status mapping
5. Comprehensive error documentation

---

## Current State Assessment

### Existing Infrastructure

✅ **order-processing-service**:
- Base: `OrderProcessingException` (basic implementation)
- Specific: `EntityNotFoundException`, `InvalidOrderStateException`, `InvalidOperationException`, `InsufficientQuantityException`, `ProductionPlanningException`
- Handler: `GlobalExceptionHandler` with @RestControllerAdvice
- DTO: `ApiErrorResponse` (basic implementation)

✅ **Other Services** (api-gateway, user-service, masterdata-service, inventory-service, simal-integration-service):
- All have: `GlobalExceptionHandler`
- All have: Generic exceptions (`ResourceNotFoundException`, `ValidationException`, `UnauthorizedException`, `UserServiceException`)

### Gaps Identified

❌ **Missing errorCode field**: Exceptions don't provide machine-readable error codes
❌ **Missing details map**: No structured context for debugging
❌ **ApiErrorResponse incomplete**: Missing errorCode and details fields
❌ **No @ApiContract annotation**: ApiErrorResponse not documented as external contract
❌ **Inconsistent exception hierarchy**: Each service has different exception structure
❌ **No error code documentation**: No central registry of error codes
❌ **No integration tests**: Exception handling not validated

---

## Implementation Plan

### Task 1: Enhance Base Exception Classes (2 hours)

**Goal**: Add errorCode and details map to all base exceptions

#### order-processing-service:
- [x] Assess current `OrderProcessingException`
- [ ] Add `errorCode` field (String)
- [ ] Add `details` map (Map<String, Object>)
- [ ] Add constructors for errorCode and details
- [ ] Add `addDetail(key, value)` fluent method
- [ ] Update all extending exceptions to use errorCode

#### Other services:
- [ ] inventory-service: Create `InventoryException` base class
- [ ] masterdata-service: Create `MasterdataException` base class
- [ ] user-service: Create `UserServiceException` base class (enhance existing)
- [ ] simal-integration-service: Create `SimalException` base class

---

### Task 2: Standardize ApiErrorResponse DTO (1 hour)

**Goal**: Create universal error response format with @ApiContract

- [ ] Add `errorCode` field (String) - machine-readable code
- [ ] Add `details` map (Map<String, Object>) - debugging context
- [ ] Add @ApiContract annotation
- [ ] Document as external API contract
- [ ] Add static factory methods for common patterns
- [ ] Copy to all services (shared DTO pattern)

**Target Structure**:
```java
@ApiContract(
    version = "v1",
    externalSource = "All frontend clients",
    description = "Standard error response format for all API errors"
)
public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String errorCode;  // NEW: Machine-readable code
    private Map<String, Object> details;  // NEW: Context map
}
```

---

### Task 3: Update GlobalExceptionHandler in Each Service (3 hours)

**Goal**: Standardize exception handling across all services

#### For each service:
- [ ] Update handlers to use errorCode from exceptions
- [ ] Pass details map to ApiErrorResponse
- [ ] Add missing exception handlers for common cases
- [ ] Ensure proper HTTP status mapping
- [ ] Add comprehensive logging with context

#### HTTP Status Mapping:
- `EntityNotFoundException` → 404 NOT_FOUND
- `InvalidOrderStateException` → 400 BAD_REQUEST
- `InsufficientQuantityException` → 400 BAD_REQUEST
- `OrderProcessingException` (base) → 500 INTERNAL_SERVER_ERROR
- `IllegalArgumentException` → 400 BAD_REQUEST
- `IllegalStateException` → 400 BAD_REQUEST
- Generic `Exception` → 500 INTERNAL_SERVER_ERROR

---

### Task 4: Define Error Code Registry (2 hours)

**Goal**: Central documentation of all error codes

#### Error Code Format: `SERVICE_DOMAIN_ERROR`

**order-processing-service**:
- `ORDER_NOT_FOUND` - EntityNotFoundException
- `ORDER_INVALID_STATE` - InvalidOrderStateException
- `ORDER_INVALID_OPERATION` - InvalidOperationException
- `ORDER_INSUFFICIENT_STOCK` - InsufficientQuantityException
- `ORDER_BOM_CONVERSION_FAILED` - BomConversionException (new)
- `ORDER_PRODUCTION_PLANNING_ERROR` - ProductionPlanningException

**inventory-service**:
- `INVENTORY_NOT_FOUND` - Stock record not found
- `INVENTORY_INSUFFICIENT_STOCK` - Not enough stock for operation
- `INVENTORY_INVALID_ADJUSTMENT` - Invalid stock adjustment request

**masterdata-service**:
- `MASTERDATA_PRODUCT_NOT_FOUND` - Product not found
- `MASTERDATA_MODULE_NOT_FOUND` - Module not found
- `MASTERDATA_PART_NOT_FOUND` - Part not found
- `MASTERDATA_BOM_NOT_FOUND` - BOM relationship not found

**user-service**:
- `USER_NOT_FOUND` - User not found
- `USER_UNAUTHORIZED` - Authentication failed
- `USER_FORBIDDEN` - Authorization failed
- `USER_INVALID_CREDENTIALS` - Login failed

**simal-integration-service**:
- `SIMAL_SCHEDULE_FAILED` - Scheduling operation failed
- `SIMAL_TASK_NOT_FOUND` - Task not found
- `SIMAL_INVALID_PRODUCTION_ORDER` - Invalid production order data

---

### Task 5: Document Error Codes in API_CONTRACTS.md (1 hour)

- [ ] Add "Error Handling" section
- [ ] Document ApiErrorResponse structure
- [ ] List all error codes by service
- [ ] Provide examples for each error type
- [ ] Document HTTP status mapping

---

### Task 6: Write Integration Tests (3 hours)

**Goal**: Validate exception handling works correctly

#### Test Coverage:
- [ ] Test ApiErrorResponse serialization
- [ ] Test each exception type returns correct HTTP status
- [ ] Test errorCode is populated correctly
- [ ] Test details map is included in response
- [ ] Test generic Exception handling (500 fallback)
- [ ] Test validation errors (400 with details)

#### Test File Locations:
- `order-processing-service/src/test/java/io/life/order/integration/ExceptionHandlingTest.java`
- Similar tests in other services

---

### Task 7: Update Documentation (1 hour)

- [ ] Update PRE_COMMIT_CHECKLIST.md with exception handling guidelines
- [ ] Update API_CONTRACTS.md with error codes section
- [ ] Document exception handling patterns in copilot-instructions.md

---

## Success Criteria

✅ All exceptions have errorCode and details map  
✅ ApiErrorResponse standardized with @ApiContract  
✅ GlobalExceptionHandler consistent across all services  
✅ Error code registry documented  
✅ Integration tests validate error responses  
✅ Documentation updated  
✅ All services compile and pass E2E tests

---

## Timeline

**Day 3 (Feb 2)**: Tasks 1-2 (Base exceptions + ApiErrorResponse)  
**Day 4 (Feb 3)**: Tasks 3-4 (GlobalExceptionHandler + Error codes)  
**Day 5 (Feb 4)**: Tasks 5-7 (Documentation + Tests + Review)

---

## Quality Metrics

- **Target**: 100% compilation success
- **Target**: All E2E tests pass (maintain 97%+ pass rate)
- **Target**: 0 breaking changes to existing error responses
- **Target**: 100% backward compatibility

---

_Started: February 2, 2026_  
_Updated: February 2, 2026_
