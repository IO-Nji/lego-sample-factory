# Refactoring Implementation Plan – Industrial-Grade Evolution

> **Last Updated:** February 3, 2026  
> **Current Production Version:** v1.6.0 (Phases 1-6 Complete, February 2026)  
> **Total Tests:** 775 (all passing across 6 services)  
> **Target:** Cloud-native, modular, enterprise-grade architecture  
> **Strategy:** Incremental enhancement with zero-downtime deployment

---

## 📊 Implementation Status

### ✅ Phase 1: API Contract Standardization & Exception Handling (COMPLETE)

**Status:** 🎉 **COMPLETE** (Merged to master, commit 2bfac60)  
**Completion Date:** February 1, 2026  
**Branch:** `feature/phase1-exception-handling`

**Deliverables:**
- ✅ **Step 1.2:** Exception handling standardization across all 5 services
  - 30+ error codes defined
  - Consistent ApiErrorResponse structure
  - GlobalExceptionHandler for all services
  - Fluent details API with addDetail() method
  - Backward compatibility maintained (100%)
- ✅ **Test Results:** 97.2% test pass rate
- ✅ **Docker Images:** All 8 services rebuilt and pushed to registry
- ✅ **Documentation:** Exception handling patterns documented

**Git History:**
```
2bfac60 - Merge feature/phase1-exception-handling: Complete Phase 1 Step 1.2
```

### ✅ Phase 2: Exception Handling Documentation & Integration Tests (COMPLETE)

**Status:** 🎉 **COMPLETE** (Merged to master, commit 2db807b)  
**Completion Date:** February 2, 2026  
**Branches:** `feature/phase2-exception-handling-docs`, `feature/phase2-exception-handling-tests`

#### Phase 2.1: Documentation
**Branch:** `feature/phase2-exception-handling-docs`  
**Commit:** fe29e1d → merged as e998c60

**Deliverables:**
- ✅ **API_CONTRACTS.md:** Comprehensive error handling section (+281 lines)
  - 30+ error codes documented
  - ApiErrorResponse structure specification
  - HTTP status code mappings
  - Error code format conventions
  - Service-specific error codes by domain
- ✅ **.github/copilot-instructions.md:** Exception handling patterns (+88 lines)
  - Standardized exception handling pattern
  - GlobalExceptionHandler implementation guide
  - ApiErrorResponse factory method documentation
  - Error code conventions and examples

#### Phase 2.2: Integration Tests
**Branch:** `feature/phase2-exception-handling-tests`  
**Commit:** 6938cb9 → merged as 2db807b

**Deliverables:**
- ✅ **31 integration tests** across 5 services (100% passing)
  - order-processing-service: 9/9 tests ✅
  - inventory-service: 6/6 tests ✅
  - masterdata-service: 6/6 tests ✅
  - user-service: 5/5 tests ✅
  - simal-integration-service: 5/5 tests ✅

**Test Coverage:**
- ✅ Error code format validation (SERVICE_DOMAIN_ERROR)
- ✅ ApiErrorResponse structure completeness
- ✅ Fluent details API (addDetail method)
- ✅ Backward compatibility (original constructors)
- ✅ Exception hierarchy verification
- ✅ HTTP status code mapping

**Testing Strategy:**
- Unit-level testing (no HTTP layer)
- Direct exception instantiation
- Fast execution (~10s per service)
- 100% reliability

**Git History:**
```
2db807b - Merge Phase 2.2: Exception handling integration tests
6938cb9 - test(exceptions): Add integration tests (Phase 2.2)
e998c60 - Merge Phase 2.1: Exception handling documentation
fe29e1d - docs(exceptions): Complete Phase 2.1 documentation
```

### ✅ Phase 3: Service Layer Refactoring (COMPLETE)

**Status:** 🎉 **COMPLETE** (Merged to master)  
**Completion Date:** February 2, 2026  
**Branch:** `feature/phase3-service-refactoring`

**Deliverables:**
- ✅ **FulfillmentService Decomposition**: Split 460+ line class into focused services
  - `BomService` - BOM lookup and module resolution
  - `StockCheckService` - Inventory validation
  - `OrderCreationService` - Order entity creation
  - `WarehouseOrderService` - Warehouse order logic
  - `FinalAssemblyCreationService` - Final assembly order creation
- ✅ **Validation Extraction**: Centralized `BomValidationService` for BOM validation
- ✅ **Improved Testability**: Services can be unit tested in isolation
- ✅ **All 4 scenarios passing**: Complete backward compatibility

**Git History:**
```
74a6f2e - refactor(order-processing): Phase 3 - Extract services from FulfillmentService
f61b516 - fix(order-processing): Add overloaded validateBomResult for BomConversionResult
```

### ✅ Phase 4: Configuration Externalization (COMPLETE)

**Status:** 🎉 **COMPLETE** (Merged to master, commit 3cdb250)  
**Completion Date:** February 2, 2026  
**Branch:** `feature/phase4-configuration`

**Deliverables:**
- ✅ **OrderProcessingConfig**: Centralized configuration with @ConfigurationProperties
  - Thresholds (lotSizeThreshold, maxOrderItems)
  - Timeouts (masterdataReadMs, inventoryWriteMs, simalScheduleMs)
  - Workstations (plantWarehouse, modulesSupermarket, partsSupply, finalAssembly)
- ✅ **Spring Profiles**: `dev`, `prod`, `cloud` with environment-specific settings
- ✅ **Environment Variables**: All magic numbers externalized to application.properties
- ✅ **Server Deployment**: Registry-based deployment at 192.168.1.237:5000
- ✅ **Deploy Directory**: Production-ready docker-compose with image: directives

**Git History:**
```
3cdb250 - feat(config): Implement configuration externalization (Phase 4)
```

**Server Deployment (192.168.1.237):**
- All 8 services running from registry images
- docker-compose.yml uses `image:` instead of `build:`
- Environment: `SPRING_PROFILES_ACTIVE=prod`
- Access: http://192.168.1.237:1011

### ✅ Phase 5: Comprehensive Unit Test Coverage (COMPLETE)

**Status:** 🎉 **COMPLETE**  
**Completion Date:** February 3, 2026  
**Branch:** `master`

**Deliverables:**
- ✅ **order-processing-service:** 394 tests
  - OrderOrchestrationServiceTest, WarehouseOrderServiceTest, ProductionOrderServiceTest
  - FinalAssemblyOrderServiceTest, SupplyOrderServiceTest, CustomerOrderServiceTest
  - All controller tests (Warehouse, Production, FinalAssembly, Supply)
  - 6 workstation order service tests (InjectionMolding, PartPreProduction, PartFinishing, GearAssembly, MotorAssembly)
- ✅ **inventory-service:** 82 tests
  - StockRecordServiceTest, StockLedgerServiceTest
  - StockRecordControllerTest, StockLedgerControllerTest, StockAlertControllerTest
- ✅ **masterdata-service:** 65 tests
  - ProductServiceTest, ModuleServiceTest, PartServiceTest, ProductModuleServiceTest
- ✅ **user-service:** 120 tests
  - WorkstationAccessServiceTest (88 tests), JwtTokenProviderTest (23 tests)
- ✅ **simal-integration-service:** 30 tests
  - ControlOrderIntegrationServiceTest (25 tests)

**Test Infrastructure:**
- MockitoExtension for unit tests
- TestFixtures for common test data
- OrderAssertions for domain-specific assertions

### ✅ Phase 6: API Gateway Tests & Integration Test Fixes (COMPLETE)

**Status:** 🎉 **COMPLETE**  
**Completion Date:** February 3, 2026  
**Branch:** `master`

**Deliverables:**
- ✅ **api-gateway:** 66 tests (NEW)
  - JwtAuthenticationFilterTest (35 tests) - Public path handling, token validation, header propagation
  - GlobalExceptionHandlerTest (30 tests) - Exception handlers for 404, 400, 401, 500
  - ApiGatewayApplicationTests (1 test) - Context loading with test profile
  - Created `application-test.properties` with test JWT secret configuration
- ✅ **Production Bug Fix:** Empty Bearer token handling in JwtAuthenticationFilter
  - Added validation for empty token after "Bearer " prefix extraction
- ✅ **user-service Integration Test Fixes:** 137 tests (17 new from fixes)
  - AuthControllerIntegrationTest - Fixed password mismatch (`lego_Pass` → `password`)
  - Added `@ActiveProfiles("test")` for proper test configuration
  - Fixed SQL init mode (`always` → `never`) to handle empty `data.sql`
  - Added JWT secret to `application-test.yml`

**Total Test Count After Phase 6:** 775 tests (all passing)

| Service | Tests |
|---------|-------|
| order-processing-service | 394 |
| inventory-service | 82 |
| masterdata-service | 65 |
| user-service | 137 |
| simal-integration-service | 31 |
| api-gateway | 66 |
| **Total** | **775** |

---

## 🎯 Strategic Objectives

### Core Principles
1. **Zero Breaking Changes** - All enhancements are additive, backward compatible
2. **Feature Branch Development** - Isolate changes, test thoroughly before merge
3. **Incremental Deployment** - Deploy by phase, validate in production, rollback if needed
4. **Cloud-Native Ready** - Microservices architecture, stateless design, 12-factor compliance
5. **Modular Integration** - Service contracts enable easy integration with external systems
6. **Industrial Robustness** - Circuit breakers, retries, graceful degradation, comprehensive observability

### Success Criteria
- ✅ Zero downtime during rollout
- ✅ All existing scenarios (1-4) continue working
- ✅ All test scripts pass after each phase
- ✅ Performance metrics maintained or improved
- ✅ Docker images deployable to cloud platforms (AWS ECS, Azure Container Apps, GCP Cloud Run)
- ✅ API contracts documented and versioned
- ✅ Observability stack integrated (metrics, logs, traces)

---

## 🌿 Branching Strategy

### Branch Hierarchy
```
master (protected, production-ready)
  ├─ dev (integration branch for completed features)
  │   ├─ feature/phase1-api-contracts
  │   ├─ feature/phase1-exception-handling
  │   ├─ feature/phase2-service-refactoring
  │   ├─ feature/phase2-configuration
  │   └─ feature/phase3-caching
  └─ hotfix/* (emergency production fixes)
```

### Workflow
1. **Feature Development**: `master → feature/phase1-api-contracts`
2. **Local Testing**: Run all 4 scenario tests, verify endpoints
3. **Integration**: `feature/phase1-api-contracts → dev` (PR with reviews)
4. **Staging Validation**: Deploy dev branch to staging environment
5. **Production Deployment**: `dev → master` after complete phase testing
6. **Registry Update**: Tag and push Docker images with semantic versioning

### Branch Naming Convention
- `feature/phase{N}-{component}` - Feature branches
- `hotfix/{issue-description}` - Emergency fixes
- `test/{test-enhancement}` - Test infrastructure improvements
- `docs/{documentation-update}` - Documentation only

### Commit Message Standard
```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`

Example:
```
feat(api-contracts): Add @JsonProperty annotations to all cross-service DTOs

- Audit revealed 12 DTOs with potential field name mismatches
- Added explicit @JsonProperty mappings for all external APIs
- Created API_CONTRACTS.md documenting service contracts
- All existing tests pass, no breaking changes

Refs: #PHASE1-API-CONTRACTS
```

---

## 📋 Phase Breakdown & Implementation Steps

## PHASE 1: API Contract Standardization & Exception Handling (CRITICAL)

**Timeline:** 5-7 days (includes testing and documentation)  
**Branch:** `feature/phase1-api-contracts` + `feature/phase1-exception-handling`  
**Risk:** LOW-MEDIUM (additive changes, backward compatible)  
**Cloud-Native Impact:** HIGH (service contract clarity enables orchestration)

### Objectives
1. Prevent silent deserialization failures (componentId/moduleId issue)
2. Establish consistent error handling across all services
3. Document all API contracts for external integration
4. Enable API versioning for future evolution

### Step 1.1: API Contract Audit & Standardization (2 days)

**Branch:** `feature/phase1-api-contracts`

**Implementation Steps:**

1. **Audit Phase** (Day 1 Morning)
   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/phase1-api-contracts
   ```

   Tasks:
   - [ ] Audit all DTOs in `order-processing-service/src/main/java/io/life/order/dto/`
   - [ ] Audit all DTOs in `order-processing-service/src/main/java/io/life/order/client/`
   - [ ] Audit all DTOs in other services (user, inventory, masterdata, simal)
   - [ ] Create audit spreadsheet: DTO name, service, field names, external API source
   - [ ] Identify mismatches using pattern: API field ≠ Java field name

   **Audit Checklist:**
   ```
   DTO Name                    | Service              | API Source           | Mismatch?
   --------------------------- | -------------------- | -------------------- | ---------
   ProductModuleDTO            | order-processing     | masterdata-service   | ✅ FIXED
   OrderItemDTO                | order-processing     | frontend             | ✅ FIXED
   CustomerOrderDTO            | order-processing     | frontend             | ❓
   WarehouseOrderDTO           | order-processing     | frontend             | ❓
   StockAdjustmentDTO          | order-processing     | inventory-service    | ❓
   ... (continue for all DTOs)
   ```

2. **Standardization Phase** (Day 1 Afternoon)
   
   Create new utility class:
   ```java
   // order-processing-service/src/main/java/io/life/order/dto/ApiContract.java
   
   /**
    * Marker annotation for DTOs that define external API contracts.
    * Indicates this DTO is part of a published API and changes require versioning.
    */
   @Target(ElementType.TYPE)
   @Retention(RetentionPolicy.RUNTIME)
   @Documented
   public @interface ApiContract {
       String version() default "v1";
       String externalSource() default "";
       String description() default "";
   }
   ```

   Apply to all external-facing DTOs:
   ```java
   @ApiContract(
       version = "v1",
       externalSource = "masterdata-service",
       description = "Product BOM module information"
   )
   public class ProductModuleDTO {
       // Existing fields with @JsonProperty annotations
   }
   ```

3. **Add @JsonProperty Annotations** (Day 2 Morning)

   For each identified mismatch:
   ```java
   // BEFORE (silent null deserialization)
   public class StockAdjustmentDTO {
       private Long workstationId;
       private String itemType;
       private Long itemId;
   }
   
   // AFTER (explicit field mapping)
   @ApiContract(version = "v1", externalSource = "inventory-service")
   public class StockAdjustmentDTO {
       @JsonProperty("workstation_id")  // API uses snake_case
       private Long workstationId;
       
       @JsonProperty("item_type")
       private String itemType;
       
       @JsonProperty("item_id")
       private Long itemId;
       
       // Backward compatibility setters if needed
       @JsonProperty("workstationId")
       private void setWorkstationIdCamelCase(Long id) {
           this.workstationId = id;
       }
   }
   ```

4. **Create API_CONTRACTS.md** (Day 2 Afternoon)

   Document all service contracts:
   ```markdown
   # API Contracts Documentation
   
   ## Cross-Service Contracts
   
   ### order-processing-service → masterdata-service
   
   **Endpoint:** `GET /api/masterdata/products/{id}/modules`
   
   **Request:** None (path parameter only)
   
   **Response:**
   ```json
   [
     {
       "componentId": 1,          // Maps to: ProductModuleDTO.moduleId
       "componentName": "Chassis", // Maps to: ProductModuleDTO.moduleName
       "componentType": "MODULE",  // Maps to: ProductModuleDTO.moduleType
       "quantity": 1               // Maps to: ProductModuleDTO.quantity
     }
   ]
   ```
   
   **DTO Mapping:**
   - Source: `MasterdataModuleResponseDTO` (masterdata-service)
   - Target: `ProductModuleDTO` (order-processing-service)
   - Mapping Strategy: Dual field support with defensive getter
   
   **Backward Compatibility:** Both `componentId` and `moduleId` accepted
   ```
   
   Continue for all service interactions.

5. **Testing** (Day 2 End)
   ```bash
   # Build service
   cd lego-factory-backend/order-processing-service
   ./mvnw clean package -DskipTests
   
   # Build Docker image
   docker-compose build --no-cache order-processing-service
   docker-compose up -d order-processing-service
   
   # Run scenario tests
   cd ../..
   ./test-scenario-1.sh
   ./test-scenario-2.sh
   ./test-scenario-3.sh
   ./test-scenario-4.sh
   
   # Verify all pass
   ```

6. **Commit & Push**
   ```bash
   git add .
   git commit -m "feat(api-contracts): Add @JsonProperty annotations and API contract documentation

   - Audited 45 DTOs across all services
   - Added @ApiContract marker annotation
   - Explicit @JsonProperty mappings for 12 external-facing DTOs
   - Created API_CONTRACTS.md with all service contracts
   - Backward compatible: dual field support where needed
   
   BREAKING CHANGES: None
   
   Tests: All 4 scenarios pass
   Refs: #PHASE1-API-CONTRACTS"
   
   git push origin feature/phase1-api-contracts
   ```

**Deliverables:**
- [ ] API_CONTRACTS.md with all cross-service contracts
- [ ] @ApiContract annotation on all external DTOs
- [ ] @JsonProperty annotations on all field mismatches
- [ ] Backward compatibility setters where needed
- [ ] All tests passing
- [ ] Docker image built and tagged

---

### Step 1.2: Exception Handling Standardization (3 days)

**Branch:** `feature/phase1-exception-handling`

**Implementation Steps:**

1. **Create Exception Hierarchy** (Day 1)

   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/phase1-exception-handling
   ```

   Create base exception:
   ```java
   // order-processing-service/src/main/java/io/life/order/exception/OrderProcessingException.java
   
   /**
    * Base exception for all order processing domain errors.
    * Extends RuntimeException for unchecked exception handling.
    */
   public class OrderProcessingException extends RuntimeException {
       private final Map<String, Object> context;
       private final ErrorCode errorCode;
       
       public OrderProcessingException(String message, ErrorCode errorCode) {
           super(message);
           this.errorCode = errorCode;
           this.context = new HashMap<>();
       }
       
       public OrderProcessingException(String message, ErrorCode errorCode, 
                                       Map<String, Object> context) {
           super(message);
           this.errorCode = errorCode;
           this.context = context;
       }
       
       public OrderProcessingException(String message, Throwable cause, 
                                       ErrorCode errorCode, Map<String, Object> context) {
           super(message, cause);
           this.errorCode = errorCode;
           this.context = context;
       }
       
       public ErrorCode getErrorCode() {
           return errorCode;
       }
       
       public Map<String, Object> getContext() {
           return Collections.unmodifiableMap(context);
       }
       
       public String getDetailedMessage() {
           StringBuilder sb = new StringBuilder(getMessage());
           if (!context.isEmpty()) {
               sb.append(" | Context: ");
               context.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
           }
           return sb.toString();
       }
   }
   ```

   Create error codes enum:
   ```java
   // order-processing-service/src/main/java/io/life/order/exception/ErrorCode.java
   
   public enum ErrorCode {
       // Business Logic Errors (4xx)
       ORDER_NOT_FOUND("ORD-001", "Order not found", 404),
       INVALID_ORDER_STATE("ORD-002", "Invalid order state transition", 400),
       INSUFFICIENT_STOCK("ORD-003", "Insufficient stock available", 400),
       INVALID_ORDER_ITEM("ORD-004", "Invalid order item data", 400),
       
       // Data Conversion Errors (4xx)
       BOM_CONVERSION_FAILED("BOM-001", "BOM conversion failed", 400),
       INVALID_MODULE_DATA("BOM-002", "Invalid module data", 400),
       PRODUCT_NOT_CONFIGURED("BOM-003", "Product not configured in BOM", 404),
       
       // Integration Errors (5xx)
       EXTERNAL_SERVICE_ERROR("EXT-001", "External service error", 502),
       MASTERDATA_SERVICE_UNAVAILABLE("EXT-002", "Masterdata service unavailable", 503),
       INVENTORY_SERVICE_UNAVAILABLE("EXT-003", "Inventory service unavailable", 503),
       SIMAL_SERVICE_UNAVAILABLE("EXT-004", "SimAL service unavailable", 503),
       
       // Internal Errors (5xx)
       DATABASE_ERROR("DB-001", "Database operation failed", 500),
       INTERNAL_ERROR("INT-001", "Internal server error", 500);
       
       private final String code;
       private final String message;
       private final int httpStatus;
       
       ErrorCode(String code, String message, int httpStatus) {
           this.code = code;
           this.message = message;
           this.httpStatus = httpStatus;
       }
       
       // Getters...
   }
   ```

   Create specific exceptions:
   ```java
   // StockInsufficientException.java
   public class StockInsufficientException extends OrderProcessingException {
       public StockInsufficientException(Long workstationId, Long itemId, 
                                         int required, int available) {
           super(
               String.format("Insufficient stock at WS-%d for item %d", 
                             workstationId, itemId),
               ErrorCode.INSUFFICIENT_STOCK,
               Map.of(
                   "workstationId", workstationId,
                   "itemId", itemId,
                   "required", required,
                   "available", available
               )
           );
       }
   }
   
   // BomConversionException.java
   public class BomConversionException extends OrderProcessingException {
       public BomConversionException(String message, Long productId, 
                                      String orderNumber) {
           super(
               message,
               ErrorCode.BOM_CONVERSION_FAILED,
               Map.of(
                   "productId", productId,
                   "orderNumber", orderNumber
               )
           );
       }
   }
   
   // Continue for: OrderNotFoundException, InvalidOrderStateException, 
   // ExternalServiceException, etc.
   ```

2. **Create Global Exception Handler** (Day 2)

   ```java
   // order-processing-service/src/main/java/io/life/order/exception/GlobalExceptionHandler.java
   
   @RestControllerAdvice
   @Slf4j
   public class GlobalExceptionHandler {
       
       @ExceptionHandler(OrderProcessingException.class)
       public ResponseEntity<ApiErrorResponse> handleOrderProcessingException(
               OrderProcessingException ex, WebRequest request) {
           
           log.error("Order processing error [{}]: {} | Context: {}", 
                     ex.getErrorCode().getCode(), 
                     ex.getMessage(), 
                     ex.getContext());
           
           ApiErrorResponse response = ApiErrorResponse.builder()
               .timestamp(LocalDateTime.now())
               .status(ex.getErrorCode().getHttpStatus())
               .error(ex.getErrorCode().getCode())
               .message(ex.getMessage())
               .context(ex.getContext())
               .path(extractPath(request))
               .build();
           
           return ResponseEntity
               .status(ex.getErrorCode().getHttpStatus())
               .body(response);
       }
       
       @ExceptionHandler(Exception.class)
       public ResponseEntity<ApiErrorResponse> handleGenericException(
               Exception ex, WebRequest request) {
           
           log.error("Unexpected error: {}", ex.getMessage(), ex);
           
           ApiErrorResponse response = ApiErrorResponse.builder()
               .timestamp(LocalDateTime.now())
               .status(500)
               .error("INTERNAL_ERROR")
               .message("An unexpected error occurred")
               .path(extractPath(request))
               .build();
           
           return ResponseEntity
               .status(500)
               .body(response);
       }
       
       @ExceptionHandler(MethodArgumentNotValidException.class)
       public ResponseEntity<ApiErrorResponse> handleValidationException(
               MethodArgumentNotValidException ex, WebRequest request) {
           
           Map<String, String> validationErrors = new HashMap<>();
           ex.getBindingResult().getFieldErrors().forEach(error -> 
               validationErrors.put(error.getField(), error.getDefaultMessage())
           );
           
           log.warn("Validation failed: {}", validationErrors);
           
           ApiErrorResponse response = ApiErrorResponse.builder()
               .timestamp(LocalDateTime.now())
               .status(400)
               .error("VALIDATION_ERROR")
               .message("Request validation failed")
               .context(Map.of("validationErrors", validationErrors))
               .path(extractPath(request))
               .build();
           
           return ResponseEntity.badRequest().body(response);
       }
       
       private String extractPath(WebRequest request) {
           return ((ServletWebRequest) request).getRequest().getRequestURI();
       }
   }
   ```

   Create error response DTO:
   ```java
   @Data
   @Builder
   @ApiContract(version = "v1", description = "Standard error response format")
   public class ApiErrorResponse {
       private LocalDateTime timestamp;
       private int status;
       private String error;
       private String message;
       private Map<String, Object> context;
       private String path;
   }
   ```

3. **Refactor Existing Exception Handling** (Day 3)

   Replace generic exceptions:
   ```java
   // BEFORE (in FulfillmentService.java)
   if (items == null || items.isEmpty()) {
       throw new RuntimeException("No items in customer order");
   }
   
   // AFTER
   if (items == null || items.isEmpty()) {
       throw new OrderProcessingException(
           "Customer order contains no items",
           ErrorCode.INVALID_ORDER_ITEM,
           Map.of("orderNumber", order.getOrderNumber())
       );
   }
   
   // BEFORE (in BomConversionService.java)
   if (modules.isEmpty()) {
       throw new RuntimeException("No modules found for product");
   }
   
   // AFTER
   if (modules.isEmpty()) {
       throw new BomConversionException(
           "No modules found in BOM lookup",
           productId,
           orderNumber
       );
   }
   ```

   Add try-catch blocks for external services:
   ```java
   // In MasterdataClient.java
   public List<ProductModuleDTO> getProductModules(Long productId) {
       try {
           ResponseEntity<ProductModuleDTO[]> response = restTemplate.exchange(
               url, HttpMethod.GET, null, ProductModuleDTO[].class
           );
           return Arrays.asList(response.getBody());
       } catch (HttpClientErrorException e) {
           if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
               throw new ProductNotConfiguredException(productId);
           }
           throw new ExternalServiceException(
               "Masterdata service returned error",
               ErrorCode.MASTERDATA_SERVICE_UNAVAILABLE,
               Map.of("productId", productId, "statusCode", e.getStatusCode())
           );
       } catch (ResourceAccessException e) {
           throw new ExternalServiceException(
               "Masterdata service unreachable",
               ErrorCode.MASTERDATA_SERVICE_UNAVAILABLE,
               Map.of("productId", productId),
               e
           );
       }
   }
   ```

4. **Testing** (Day 3 End)

   Create integration tests:
   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   class GlobalExceptionHandlerTest {
       
       @Autowired
       private MockMvc mockMvc;
       
       @Test
       void testOrderNotFoundException() throws Exception {
           mockMvc.perform(get("/api/customer-orders/99999"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.error").value("ORD-001"))
               .andExpect(jsonPath("$.message").exists())
               .andExpect(jsonPath("$.timestamp").exists());
       }
       
       @Test
       void testValidationError() throws Exception {
           mockMvc.perform(post("/api/customer-orders")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("{\"orderItems\": []}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
               .andExpect(jsonPath("$.context.validationErrors").exists());
       }
   }
   ```

   Run all tests:
   ```bash
   ./mvnw test
   ./test-scenario-1.sh
   ./test-scenario-2.sh
   ./test-scenario-3.sh
   ./test-scenario-4.sh
   ```

5. **Commit & Push**
   ```bash
   git add .
   git commit -m "refactor(exceptions): Implement standardized exception handling

   - Created OrderProcessingException hierarchy with ErrorCode enum
   - Added GlobalExceptionHandler for consistent error responses
   - Created ApiErrorResponse DTO with context information
   - Refactored all RuntimeException throws to domain exceptions
   - Added try-catch blocks for external service calls
   
   BACKWARD COMPATIBILITY: Error response format enhanced but all HTTP status codes preserved
   
   Tests: All scenarios pass + 12 new integration tests
   Refs: #PHASE1-EXCEPTION-HANDLING"
   
   git push origin feature/phase1-exception-handling
   ```

**Deliverables:**
- [ ] OrderProcessingException base class with context
- [ ] ErrorCode enum with HTTP status mappings
- [ ] 8+ specific exception classes
- [ ] GlobalExceptionHandler with @RestControllerAdvice
- [ ] ApiErrorResponse DTO
- [ ] All generic exceptions refactored
- [ ] Integration tests for error scenarios
- [ ] All tests passing

---

### Phase 1 Integration & Deployment (2 days)

**Steps:**

1. **Merge to Dev** (Day 1 Morning)
   ```bash
   # Merge API contracts
   git checkout dev
   git pull origin dev
   git merge feature/phase1-api-contracts
   git push origin dev
   
   # Merge exception handling
   git merge feature/phase1-exception-handling
   git push origin dev
   ```

2. **Build & Test Integration** (Day 1 Afternoon)
   ```bash
   git checkout dev
   docker-compose build --no-cache
   docker-compose up -d
   
   # Wait for services to start
   sleep 30
   
   # Run all scenario tests
   ./test-scenario-1.sh
   ./test-scenario-2.sh
   ./test-scenario-3.sh
   ./test-scenario-4.sh
   
   # Verify all pass
   ```

3. **Registry Push - Dev Tag** (Day 1 End)
   ```bash
   # Tag dev images
   ./push-to-registry.sh --tag dev
   
   # Verify registry
   curl http://192.168.1.237:5000/v2/lego-sample-factory-order-processing-service/tags/list
   ```

4. **Staging Deployment** (Day 2 Morning)
   
   On staging server:
   ```bash
   ssh nji@io-surf-staging
   cd ~/lego-sample-factory
   git checkout dev
   git pull origin dev
   
   # Pull dev images from registry
   ./update-from-registry.sh --tag dev
   
   # Smoke test
   curl http://localhost:1011/api/health
   ```

5. **Staging Validation** (Day 2 Afternoon)
   
   Run tests on staging:
   ```bash
   # Update .env with staging endpoints
   export API_BASE_URL=http://io-surf-staging:1011
   
   ./test-scenario-1.sh
   ./test-scenario-2.sh
   ./test-scenario-3.sh
   ./test-scenario-4.sh
   ```

6. **Production Deployment** (Day 2 End - after validation)
   ```bash
   # Merge dev to master
   git checkout master
   git pull origin master
   git merge dev
   git tag v1.1.0-phase1
   git push origin master --tags
   
   # Push production images
   ./push-to-registry.sh --tag latest --tag v1.1.0
   
   # Deploy to production
   ssh nji@io-surf
   cd ~/lego-sample-factory
   git checkout master
   git pull origin master
   ./update-from-registry.sh
   
   # Verify production
   curl https://lego-factory.io-surf.com/api/health
   ```

**Phase 1 Deliverables:**
- ✅ API_CONTRACTS.md documentation
- ✅ @ApiContract annotations on all DTOs
- ✅ @JsonProperty field mappings
- ✅ OrderProcessingException hierarchy
- ✅ GlobalExceptionHandler
- ✅ All tests passing
- ✅ Dev images in registry
- ✅ Production v1.1.0 deployed

---

## PHASE 2: Service Layer Refactoring & Configuration (IMPORTANT)

**Timeline:** 10-12 days  
**Branches:** `feature/phase2-service-refactoring` + `feature/phase2-configuration`  
**Risk:** MEDIUM (structural changes, requires careful testing)  
**Cloud-Native Impact:** HIGH (enables horizontal scaling, service mesh integration)

### Objectives
1. Split large service classes into single-responsibility components
2. Externalize configuration for cloud-native deployment
3. Implement validation as reusable components
4. Enable service discovery and health checks

### Step 2.1: Service Layer Refactoring (7 days)

**Branch:** `feature/phase2-service-refactoring`

**Target:** FulfillmentService (460+ lines) → 4 specialized services

**Implementation Steps:**

1. **Create Service Package Structure** (Day 1)

   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/phase2-service-refactoring
   ```

   Create new package structure:
   ```
   src/main/java/io/life/order/service/
   ├── orchestration/           # High-level workflow coordination
   │   ├── OrderOrchestrationService.java (EXISTING)
   │   ├── FulfillmentOrchestrationService.java (NEW)
   │   └── ProductionOrchestrationService.java (NEW)
   ├── domain/                  # Core business logic
   │   ├── CustomerOrderService.java (EXISTING)
   │   ├── WarehouseOrderService.java (EXISTING)
   │   ├── OrderValidationService.java (NEW)
   │   └── BomConversionService.java (NEW)
   ├── integration/             # External service clients
   │   ├── MasterdataClient.java (EXISTING - move from client/)
   │   ├── InventoryClient.java (EXISTING - move from client/)
   │   └── SimalClient.java (EXISTING - move from client/)
   └── validation/              # Reusable validators
       ├── OrderValidator.java (NEW)
       ├── StockValidator.java (NEW)
       └── BomValidator.java (NEW)
   ```

2. **Extract BomConversionService** (Day 2)

   Create new service:
   ```java
   @Service
   @Slf4j
   @RequiredArgsConstructor
   public class BomConversionService {
       
       private final MasterdataClient masterdataClient;
       private final BomValidator bomValidator;
       
       /**
        * Convert order items (products) to warehouse items (modules) using BOM.
        * 
        * @param orderItems List of product-based order items
        * @param orderNumber Order number for context in errors
        * @return List of module-based warehouse items with productId preserved
        * @throws BomConversionException if conversion fails
        */
       public List<WarehouseOrderItem> convertProductsToModules(
               List<OrderItem> orderItems, String orderNumber) {
           
           log.info("Starting BOM conversion for order {}", orderNumber);
           
           // Validate input
           bomValidator.validateOrderItems(orderItems);
           
           List<WarehouseOrderItem> warehouseItems = new ArrayList<>();
           
           for (OrderItem item : orderItems) {
               try {
                   List<ProductModuleDTO> modules = fetchModulesForProduct(
                       item.getItemId(), orderNumber
                   );
                   
                   List<WarehouseOrderItem> convertedItems = convertModulesToWarehouseItems(
                       modules, item, orderNumber
                   );
                   
                   warehouseItems.addAll(convertedItems);
                   
                   log.info("✓ Converted product {} to {} modules", 
                           item.getItemId(), convertedItems.size());
                   
               } catch (Exception e) {
                   log.error("✗ BOM conversion failed for product {}", 
                           item.getItemId(), e);
                   throw new BomConversionException(
                       "Failed to convert product to modules",
                       item.getItemId(),
                       orderNumber,
                       e
                   );
               }
           }
           
           // Validate output
           bomValidator.validateWarehouseItems(warehouseItems);
           
           log.info("✓ BOM conversion complete: {} items → {} warehouse items",
                   orderItems.size(), warehouseItems.size());
           
           return warehouseItems;
       }
       
       private List<ProductModuleDTO> fetchModulesForProduct(Long productId, 
                                                              String orderNumber) {
           try {
               return masterdataClient.getProductModules(productId);
           } catch (ExternalServiceException e) {
               // Try fallback to cached data if available
               log.warn("Masterdata service unavailable, attempting cache lookup");
               return getCachedModules(productId)
                   .orElseThrow(() -> new BomConversionException(
                       "BOM lookup failed and no cached data available",
                       productId,
                       orderNumber,
                       e
                   ));
           }
       }
       
       private List<WarehouseOrderItem> convertModulesToWarehouseItems(
               List<ProductModuleDTO> modules, 
               OrderItem sourceItem, 
               String orderNumber) {
           
           return modules.stream()
               .map(module -> {
                   WarehouseOrderItem item = new WarehouseOrderItem();
                   item.setItemId(module.getModuleId());  // Uses defensive getter
                   item.setItemName(module.getModuleName());
                   item.setItemType("MODULE");
                   item.setRequestedQuantity(module.getQuantity() * sourceItem.getQuantity());
                   item.setProductId(sourceItem.getItemId());  // CRITICAL: Preserve product ID
                   return item;
               })
               .filter(item -> item.getItemId() != null)  // Filter invalid entries
               .collect(Collectors.toList());
       }
       
       private Optional<List<ProductModuleDTO>> getCachedModules(Long productId) {
           // Implement caching logic (Phase 3)
           return Optional.empty();
       }
   }
   ```

3. **Extract OrderValidator** (Day 3)

   ```java
   @Component
   @Slf4j
   public class OrderValidator {
       
       /**
        * Validate order and throw exception if invalid.
        * Collects ALL validation errors before throwing.
        */
       public void validateOrderForFulfillment(CustomerOrder order) {
           List<String> errors = new ArrayList<>();
           
           if (order == null) {
               throw new OrderProcessingException(
                   "Order cannot be null",
                   ErrorCode.INVALID_ORDER_ITEM
               );
           }
           
           if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
               errors.add("Order must contain at least one item");
           }
           
           if (!"CONFIRMED".equals(order.getStatus())) {
               errors.add(String.format(
                   "Order status must be CONFIRMED, current: %s", 
                   order.getStatus()
               ));
           }
           
           if (order.getOrderItems() != null) {
               for (int i = 0; i < order.getOrderItems().size(); i++) {
                   OrderItem item = order.getOrderItems().get(i);
                   validateOrderItem(item, i, errors);
               }
           }
           
           if (!errors.isEmpty()) {
               throw new OrderProcessingException(
                   "Order validation failed: " + String.join(", ", errors),
                   ErrorCode.INVALID_ORDER_ITEM,
                   Map.of(
                       "orderNumber", order.getOrderNumber(),
                       "validationErrors", errors
                   )
               );
           }
       }
       
       private void validateOrderItem(OrderItem item, int index, List<String> errors) {
           if (item.getItemId() == null) {
               errors.add(String.format("Item %d: itemId is required", index));
           }
           
           if (item.getItemType() == null || item.getItemType().isBlank()) {
               errors.add(String.format("Item %d: itemType is required", index));
           }
           
           if (item.getQuantity() == null || item.getQuantity() <= 0) {
               errors.add(String.format("Item %d: quantity must be positive", index));
           }
       }
       
       public void validateWarehouseOrder(WarehouseOrder order) {
           // Similar validation for warehouse orders
       }
   }
   ```

4. **Extract FulfillmentOrchestrationService** (Day 4-5)

   ```java
   @Service
   @Slf4j
   @RequiredArgsConstructor
   public class FulfillmentOrchestrationService {
       
       private final CustomerOrderService customerOrderService;
       private final WarehouseOrderService warehouseOrderService;
       private final ProductionOrderService productionOrderService;
       private final BomConversionService bomConversionService;
       private final OrderValidator orderValidator;
       private final StockValidator stockValidator;
       private final InventoryClient inventoryClient;
       
       @Value("${life.order-processing.thresholds.lot-size-threshold:3}")
       private int lotSizeThreshold;
       
       /**
        * Fulfill customer order using appropriate scenario (1-4).
        * This is the main entry point for order fulfillment.
        */
       @Transactional
       public FulfillmentResult fulfillCustomerOrder(Long customerOrderId) {
           log.info("=== Starting order fulfillment for customer order {} ===", 
                   customerOrderId);
           
           // Fetch and validate order
           CustomerOrder order = customerOrderService.findById(customerOrderId)
               .orElseThrow(() -> new OrderNotFoundException(customerOrderId));
           
           orderValidator.validateOrderForFulfillment(order);
           
           // Determine scenario based on order characteristics
           ScenarioType scenario = determineScenario(order);
           log.info("Selected scenario: {}", scenario);
           
           // Execute scenario-specific fulfillment
           return switch (scenario) {
               case DIRECT_FULFILLMENT -> executeScenario1(order);
               case WAREHOUSE_ORDER -> executeScenario2(order);
               case FULL_PRODUCTION -> executeScenario3(order);
               case DIRECT_PRODUCTION -> executeScenario4(order);
           };
       }
       
       private ScenarioType determineScenario(CustomerOrder order) {
           int totalQuantity = order.getOrderItems().stream()
               .mapToInt(OrderItem::getQuantity)
               .sum();
           
           // Scenario 4: High volume (qty ≥ LOT_SIZE_THRESHOLD)
           if (totalQuantity >= lotSizeThreshold) {
               return ScenarioType.DIRECT_PRODUCTION;
           }
           
           // Check stock availability
           boolean stockAvailable = stockValidator.checkStockAvailability(
               order.getWorkstationId(),
               order.getOrderItems()
           );
           
           if (stockAvailable) {
               return ScenarioType.DIRECT_FULFILLMENT;  // Scenario 1
           } else {
               // Check module availability
               boolean modulesAvailable = checkModuleAvailability(order);
               return modulesAvailable ? 
                   ScenarioType.WAREHOUSE_ORDER :      // Scenario 2
                   ScenarioType.FULL_PRODUCTION;       // Scenario 3
           }
       }
       
       private FulfillmentResult executeScenario1(CustomerOrder order) {
           log.info("Executing Scenario 1: Direct fulfillment from stock");
           
           // Debit inventory
           order.getOrderItems().forEach(item -> 
               inventoryClient.debitStock(
                   order.getWorkstationId(),
                   InventoryClient.ITEM_TYPE_PRODUCT,
                   item.getItemId(),
                   item.getQuantity(),
                   InventoryClient.REASON_FULFILLMENT,
                   "Customer order: " + order.getOrderNumber()
               )
           );
           
           // Update order status
           order.setStatus("COMPLETED");
           customerOrderService.save(order);
           
           return FulfillmentResult.success(order, ScenarioType.DIRECT_FULFILLMENT);
       }
       
       private FulfillmentResult executeScenario2(CustomerOrder order) {
           log.info("Executing Scenario 2: Warehouse order + Final Assembly");
           
           // Convert products to modules
           List<WarehouseOrderItem> warehouseItems = bomConversionService
               .convertProductsToModules(order.getOrderItems(), order.getOrderNumber());
           
           // Create warehouse order
           WarehouseOrder warehouseOrder = warehouseOrderService.createFromCustomerOrder(
               order, warehouseItems
           );
           
           // Update customer order
           order.setStatus("PROCESSING");
           order.setTriggerScenario("WAREHOUSE_ORDER_NEEDED");
           customerOrderService.save(order);
           
           return FulfillmentResult.success(order, ScenarioType.WAREHOUSE_ORDER, warehouseOrder);
       }
       
       // Continue for scenarios 3 and 4...
   }
   
   // Result DTO
   @Data
   @Builder
   public class FulfillmentResult {
       private boolean success;
       private ScenarioType scenario;
       private CustomerOrder customerOrder;
       private WarehouseOrder warehouseOrder;
       private ProductionOrder productionOrder;
       private String message;
       private Map<String, Object> metadata;
       
       public static FulfillmentResult success(CustomerOrder order, ScenarioType scenario) {
           return FulfillmentResult.builder()
               .success(true)
               .scenario(scenario)
               .customerOrder(order)
               .message("Order fulfilled successfully via " + scenario)
               .build();
       }
   }
   ```

5. **Refactor Controllers** (Day 6)

   Update controllers to use new services:
   ```java
   @RestController
   @RequestMapping("/api/customer-orders")
   @RequiredArgsConstructor
   public class CustomerOrderController {
       
       private final FulfillmentOrchestrationService fulfillmentService;
       private final CustomerOrderService customerOrderService;
       
       @PutMapping("/{id}/fulfill")
       public ResponseEntity<CustomerOrderDTO> fulfillOrder(@PathVariable Long id) {
           FulfillmentResult result = fulfillmentService.fulfillCustomerOrder(id);
           
           if (!result.isSuccess()) {
               throw new OrderProcessingException(
                   "Order fulfillment failed: " + result.getMessage(),
                   ErrorCode.INTERNAL_ERROR
               );
           }
           
           return ResponseEntity.ok(
               CustomerOrderDTO.fromEntity(result.getCustomerOrder())
           );
       }
   }
   ```

6. **Testing** (Day 7)

   Create unit tests for each new service:
   ```java
   @ExtendWith(MockitoExtension.class)
   class BomConversionServiceTest {
       
       @Mock
       private MasterdataClient masterdataClient;
       
       @Mock
       private BomValidator bomValidator;
       
       @InjectMocks
       private BomConversionService bomConversionService;
       
       @Test
       void testConvertProductsToModules_Success() {
           // Given
           OrderItem item = new OrderItem();
           item.setItemId(1L);
           item.setQuantity(2);
           
           List<ProductModuleDTO> modules = List.of(
               createModule(1L, "Chassis", 1),
               createModule(2L, "Motor", 1)
           );
           
           when(masterdataClient.getProductModules(1L)).thenReturn(modules);
           
           // When
           List<WarehouseOrderItem> result = bomConversionService
               .convertProductsToModules(List.of(item), "ORD-TEST");
           
           // Then
           assertEquals(2, result.size());
           assertEquals(1L, result.get(0).getItemId());
           assertEquals(2, result.get(0).getRequestedQuantity());  // qty * 2
           assertEquals(1L, result.get(0).getProductId());  // Preserved
       }
       
       @Test
       void testConvertProductsToModules_MasterdataFailure() {
           // Given
           OrderItem item = new OrderItem();
           item.setItemId(1L);
           
           when(masterdataClient.getProductModules(1L))
               .thenThrow(new ExternalServiceException(...));
           
           // When/Then
           assertThrows(BomConversionException.class, () -> 
               bomConversionService.convertProductsToModules(
                   List.of(item), "ORD-TEST"
               )
           );
       }
   }
   ```

   Run integration tests:
   ```bash
   ./mvnw test
   docker-compose build --no-cache order-processing-service
   docker-compose up -d
   ./test-scenario-1.sh
   ./test-scenario-2.sh
   ./test-scenario-3.sh
   ./test-scenario-4.sh
   ```

7. **Commit & Push**
   ```bash
   git add .
   git commit -m "refactor(services): Split FulfillmentService into specialized components

   - Created service package structure: orchestration/domain/integration/validation
   - Extracted BomConversionService (180 lines) with caching fallback
   - Extracted OrderValidator with fluent validation API
   - Extracted FulfillmentOrchestrationService as main entry point
   - Moved integration clients to dedicated package
   - All controllers updated to use new services
   
   BENEFITS:
   - Single Responsibility: Each service has one clear purpose
   - Testability: 95% test coverage on new services
   - Maintainability: Changes isolated to specific layer
   - Reusability: Validators shared across all order types
   
   BREAKING CHANGES: None (internal refactoring only)
   
   Tests: All scenarios pass + 45 new unit tests
   Refs: #PHASE2-SERVICE-REFACTORING"
   
   git push origin feature/phase2-service-refactoring
   ```

**Deliverables:**
- [ ] New package structure (orchestration/domain/integration/validation)
- [ ] BomConversionService extracted (180 lines)
- [ ] OrderValidator with fluent API
- [ ] FulfillmentOrchestrationService as main entry point
- [ ] StockValidator for inventory checks
- [ ] All controllers refactored
- [ ] 45+ unit tests (95% coverage)
- [ ] All integration tests passing

---

### Step 2.2: Configuration Externalization (3 days)

**Branch:** `feature/phase2-configuration`

**Implementation Steps:**

1. **Create Configuration Classes** (Day 1)

   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/phase2-configuration
   ```

   Create main config:
   ```java
   @Configuration
   @ConfigurationProperties(prefix = "life.order-processing")
   @Validated
   @Data
   public class OrderProcessingConfig {
       
       /**
        * Order processing thresholds and business rules
        */
       private Thresholds thresholds = new Thresholds();
       
       /**
        * Timeout configurations for external service calls
        */
       private Timeouts timeouts = new Timeouts();
       
       /**
        * Workstation identifiers
        */
       private Workstations workstations = new Workstations();
       
       /**
        * Feature flags for gradual rollout
        */
       private Features features = new Features();
       
       @Data
       public static class Thresholds {
           @Min(1)
           private int lotSizeThreshold = 3;
           
           @Min(1)
           @Max(1000)
           private int maxOrderItems = 100;
           
           @Min(1)
           private int stockReserveMinutes = 30;
           
           @Min(0)
           @Max(100)
           private int stockSafetyMarginPercent = 10;
       }
       
       @Data
       public static class Timeouts {
           @Min(1000)
           private int masterdataReadMs = 5000;
           
           @Min(1000)
           private int masterdataWriteMs = 10000;
           
           @Min(1000)
           private int inventoryReadMs = 3000;
           
           @Min(1000)
           private int inventoryWriteMs = 10000;
           
           @Min(1000)
           private int simalScheduleMs = 30000;
           
           @Min(1000)
           private int simalTaskUpdateMs = 5000;
       }
       
       @Data
       public static class Workstations {
           private Long plantWarehouse = 7L;
           private Long modulesSupermarket = 8L;
           private Long partsSupply = 9L;
           private Long injectionMolding = 1L;
           private Long partsPreProduction = 2L;
           private Long partFinishing = 3L;
           private Long gearAssembly = 4L;
           private Long motorAssembly = 5L;
           private Long finalAssembly = 6L;
       }
       
       @Data
       public static class Features {
           private boolean enableCaching = false;           // Phase 3
           private boolean enableAsyncProcessing = false;   // Phase 3
           private boolean enableCircuitBreaker = false;    // Phase 3
           private boolean enableMetrics = true;
           private boolean enableDetailedLogging = true;
       }
   }
   ```

2. **Create application.properties Templates** (Day 1 Afternoon)

   Create `application.properties`:
   ```properties
   # Application Info
   spring.application.name=order-processing-service
   server.port=${ORDER_PROCESSING_SERVICE_PORT:8015}
   
   # Order Processing Configuration
   life.order-processing.thresholds.lot-size-threshold=${LOT_SIZE_THRESHOLD:3}
   life.order-processing.thresholds.max-order-items=${MAX_ORDER_ITEMS:100}
   life.order-processing.thresholds.stock-reserve-minutes=${STOCK_RESERVE_MINUTES:30}
   life.order-processing.thresholds.stock-safety-margin-percent=${STOCK_SAFETY_MARGIN:10}
   
   # External Service Timeouts
   life.order-processing.timeouts.masterdata-read-ms=${MASTERDATA_READ_TIMEOUT:5000}
   life.order-processing.timeouts.masterdata-write-ms=${MASTERDATA_WRITE_TIMEOUT:10000}
   life.order-processing.timeouts.inventory-read-ms=${INVENTORY_READ_TIMEOUT:3000}
   life.order-processing.timeouts.inventory-write-ms=${INVENTORY_WRITE_TIMEOUT:10000}
   life.order-processing.timeouts.simal-schedule-ms=${SIMAL_SCHEDULE_TIMEOUT:30000}
   life.order-processing.timeouts.simal-task-update-ms=${SIMAL_TASK_UPDATE_TIMEOUT:5000}
   
   # Workstation Configuration
   life.order-processing.workstations.plant-warehouse=${WS_PLANT_WAREHOUSE:7}
   life.order-processing.workstations.modules-supermarket=${WS_MODULES_SUPERMARKET:8}
   life.order-processing.workstations.parts-supply=${WS_PARTS_SUPPLY:9}
   life.order-processing.workstations.injection-molding=${WS_INJECTION_MOLDING:1}
   life.order-processing.workstations.parts-pre-production=${WS_PARTS_PRE_PRODUCTION:2}
   life.order-processing.workstations.part-finishing=${WS_PART_FINISHING:3}
   life.order-processing.workstations.gear-assembly=${WS_GEAR_ASSEMBLY:4}
   life.order-processing.workstations.motor-assembly=${WS_MOTOR_ASSEMBLY:5}
   life.order-processing.workstations.final-assembly=${WS_FINAL_ASSEMBLY:6}
   
   # Feature Flags
   life.order-processing.features.enable-caching=${ENABLE_CACHING:false}
   life.order-processing.features.enable-async-processing=${ENABLE_ASYNC:false}
   life.order-processing.features.enable-circuit-breaker=${ENABLE_CIRCUIT_BREAKER:false}
   life.order-processing.features.enable-metrics=${ENABLE_METRICS:true}
   life.order-processing.features.enable-detailed-logging=${ENABLE_DETAILED_LOGGING:true}
   ```

   Create `application-dev.properties`:
   ```properties
   # Development Profile
   life.order-processing.features.enable-detailed-logging=true
   life.order-processing.thresholds.lot-size-threshold=1
   logging.level.io.life=DEBUG
   ```

   Create `application-prod.properties`:
   ```properties
   # Production Profile
   life.order-processing.features.enable-detailed-logging=false
   life.order-processing.thresholds.lot-size-threshold=10
   life.order-processing.timeouts.masterdata-read-ms=3000
   logging.level.io.life=INFO
   ```

   Create `application-cloud.properties`:
   ```properties
   # Cloud-Native Profile (AWS ECS, Azure Container Apps, GCP Cloud Run)
   
   # External Service URLs (from environment)
   masterdata.service.url=${MASTERDATA_SERVICE_URL}
   inventory.service.url=${INVENTORY_SERVICE_URL}
   simal.service.url=${SIMAL_SERVICE_URL}
   
   # Database (Cloud-managed RDS, Azure SQL, Cloud SQL)
   spring.datasource.url=${DATABASE_URL}
   spring.datasource.username=${DATABASE_USERNAME}
   spring.datasource.password=${DATABASE_PASSWORD}
   
   # Observability (CloudWatch, Azure Monitor, Cloud Logging)
   management.endpoints.web.exposure.include=health,info,metrics,prometheus
   management.metrics.export.prometheus.enabled=true
   
   # Circuit Breaker (for cloud resilience)
   life.order-processing.features.enable-circuit-breaker=true
   
   # Caching (Redis/ElastiCache)
   life.order-processing.features.enable-caching=${ENABLE_REDIS_CACHE:true}
   spring.cache.type=${CACHE_TYPE:redis}
   spring.redis.host=${REDIS_HOST}
   spring.redis.port=${REDIS_PORT:6379}
   ```

3. **Update Services to Use Config** (Day 2)

   ```java
   @Service
   @RequiredArgsConstructor
   public class FulfillmentOrchestrationService {
       
       private final OrderProcessingConfig config;  // Injected
       
       private ScenarioType determineScenario(CustomerOrder order) {
           int totalQuantity = order.getOrderItems().stream()
               .mapToInt(OrderItem::getQuantity)
               .sum();
           
           // Use config instead of hardcoded value
           if (totalQuantity >= config.getThresholds().getLotSizeThreshold()) {
               return ScenarioType.DIRECT_PRODUCTION;
           }
           
           // ... rest of logic
       }
   }
   ```

   Update RestTemplate configurations:
   ```java
   @Configuration
   public class RestTemplateConfig {
       
       @Autowired
       private OrderProcessingConfig config;
       
       @Bean
       public RestTemplate masterdataRestTemplate() {
           HttpComponentsClientHttpRequestFactory factory = 
               new HttpComponentsClientHttpRequestFactory();
           
           factory.setConnectTimeout(
               config.getTimeouts().getMasterdataReadMs()
           );
           factory.setReadTimeout(
               config.getTimeouts().getMasterdataReadMs()
           );
           
           return new RestTemplate(factory);
       }
       
       @Bean
       public RestTemplate inventoryRestTemplate() {
           HttpComponentsClientHttpRequestFactory factory = 
               new HttpComponentsClientHttpRequestFactory();
           
           factory.setConnectTimeout(
               config.getTimeouts().getInventoryWriteMs()
           );
           factory.setReadTimeout(
               config.getTimeouts().getInventoryWriteMs()
           );
           
           return new RestTemplate(factory);
       }
   }
   ```

4. **Create Admin Configuration Endpoint** (Day 2 Afternoon)

   ```java
   @RestController
   @RequestMapping("/api/config")
   @RequiredArgsConstructor
   public class ConfigurationController {
       
       private final OrderProcessingConfig config;
       
       @GetMapping
       public ResponseEntity<OrderProcessingConfig> getConfiguration() {
           return ResponseEntity.ok(config);
       }
       
       @GetMapping("/thresholds")
       public ResponseEntity<OrderProcessingConfig.Thresholds> getThresholds() {
           return ResponseEntity.ok(config.getThresholds());
       }
       
       @PutMapping("/thresholds/lot-size")
       @PreAuthorize("hasRole('ADMIN')")
       public ResponseEntity<Void> updateLotSizeThreshold(
               @RequestBody @Valid LotSizeThresholdUpdateDTO dto) {
           
           config.getThresholds().setLotSizeThreshold(dto.getValue());
           
           log.info("Lot size threshold updated to {} by user {}", 
                   dto.getValue(), getCurrentUsername());
           
           return ResponseEntity.ok().build();
       }
       
       @GetMapping("/features")
       public ResponseEntity<OrderProcessingConfig.Features> getFeatureFlags() {
           return ResponseEntity.ok(config.getFeatures());
       }
   }
   ```

5. **Create Docker Environment Templates** (Day 3)

   Create `.env.template`:
   ```bash
   # Order Processing Configuration
   LOT_SIZE_THRESHOLD=3
   MAX_ORDER_ITEMS=100
   STOCK_RESERVE_MINUTES=30
   STOCK_SAFETY_MARGIN=10
   
   # External Service Timeouts (milliseconds)
   MASTERDATA_READ_TIMEOUT=5000
   MASTERDATA_WRITE_TIMEOUT=10000
   INVENTORY_READ_TIMEOUT=3000
   INVENTORY_WRITE_TIMEOUT=10000
   SIMAL_SCHEDULE_TIMEOUT=30000
   SIMAL_TASK_UPDATE_TIMEOUT=5000
   
   # Feature Flags (true/false)
   ENABLE_CACHING=false
   ENABLE_ASYNC=false
   ENABLE_CIRCUIT_BREAKER=false
   ENABLE_METRICS=true
   ENABLE_DETAILED_LOGGING=true
   
   # Spring Profile (dev, prod, cloud)
   SPRING_PROFILES_ACTIVE=dev
   ```

   Update `docker-compose.yml`:
   ```yaml
   services:
     order-processing-service:
       image: 192.168.1.237:5000/lego-sample-factory-order-processing-service:latest
       environment:
         - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-dev}
         - LOT_SIZE_THRESHOLD=${LOT_SIZE_THRESHOLD:-3}
         - MAX_ORDER_ITEMS=${MAX_ORDER_ITEMS:-100}
         - STOCK_RESERVE_MINUTES=${STOCK_RESERVE_MINUTES:-30}
         - MASTERDATA_READ_TIMEOUT=${MASTERDATA_READ_TIMEOUT:-5000}
         - INVENTORY_WRITE_TIMEOUT=${INVENTORY_WRITE_TIMEOUT:-10000}
         - ENABLE_CACHING=${ENABLE_CACHING:-false}
         - ENABLE_METRICS=${ENABLE_METRICS:-true}
   ```

6. **Create Cloud Deployment Manifests** (Day 3 Afternoon)

   Create `k8s/configmap.yaml`:
   ```yaml
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: order-processing-config
     namespace: lego-factory
   data:
     LOT_SIZE_THRESHOLD: "10"
     MAX_ORDER_ITEMS: "100"
     STOCK_RESERVE_MINUTES: "30"
     MASTERDATA_READ_TIMEOUT: "3000"
     INVENTORY_WRITE_TIMEOUT: "10000"
     ENABLE_CACHING: "true"
     ENABLE_CIRCUIT_BREAKER: "true"
     ENABLE_METRICS: "true"
     SPRING_PROFILES_ACTIVE: "cloud"
   ```

   Create `k8s/deployment.yaml`:
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: order-processing-service
     namespace: lego-factory
   spec:
     replicas: 3
     selector:
       matchLabels:
         app: order-processing-service
     template:
       metadata:
         labels:
           app: order-processing-service
       spec:
         containers:
         - name: order-processing-service
           image: 192.168.1.237:5000/lego-sample-factory-order-processing-service:v1.2.0
           ports:
           - containerPort: 8015
           envFrom:
           - configMapRef:
               name: order-processing-config
           - secretRef:
               name: order-processing-secrets
           livenessProbe:
             httpGet:
               path: /actuator/health/liveness
               port: 8015
             initialDelaySeconds: 30
             periodSeconds: 10
           readinessProbe:
             httpGet:
               path: /actuator/health/readiness
               port: 8015
             initialDelaySeconds: 20
             periodSeconds: 5
           resources:
             requests:
               memory: "512Mi"
               cpu: "250m"
             limits:
               memory: "1Gi"
               cpu: "500m"
   ```

7. **Testing** (Day 3 End)

   Test with different profiles:
   ```bash
   # Test dev profile
   SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
   
   # Test prod profile
   SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
   
   # Test config endpoint
   curl http://localhost:8015/api/config/thresholds
   
   # Build and test in Docker
   docker-compose build --no-cache order-processing-service
   docker-compose up -d
   ./test-scenario-4.sh  # Test lot size threshold
   ```

8. **Commit & Push**
   ```bash
   git add .
   git commit -m "feat(config): Externalize configuration for cloud-native deployment

   - Created OrderProcessingConfig with @ConfigurationProperties
   - Added application-{dev,prod,cloud}.properties profiles
   - Externalized all magic numbers (lot size, timeouts, workstation IDs)
   - Created .env.template for Docker deployments
   - Added Kubernetes ConfigMap and Deployment manifests
   - Created admin configuration endpoint
   - All timeouts now configurable via environment variables
   
   CLOUD-NATIVE FEATURES:
   - Profile-based configuration (dev/prod/cloud)
   - Environment variable overrides
   - Kubernetes ConfigMap support
   - Health probes configured
   - Resource limits defined
   
   BREAKING CHANGES: None (defaults preserve existing behavior)
   
   Tests: All scenarios pass with all profiles
   Refs: #PHASE2-CONFIGURATION"
   
   git push origin feature/phase2-configuration
   ```

**Deliverables:**
- [ ] OrderProcessingConfig with nested classes
- [ ] application-{dev,prod,cloud}.properties profiles
- [ ] .env.template for Docker
- [ ] k8s/configmap.yaml and k8s/deployment.yaml
- [ ] Admin configuration endpoint
- [ ] All services refactored to use config
- [ ] All tests passing with all profiles

---

### Phase 2 Integration & Deployment (2 days)

**Steps:**

1. **Merge to Dev** (Day 1 Morning)
   ```bash
   git checkout dev
   git pull origin dev
   git merge feature/phase2-service-refactoring
   git merge feature/phase2-configuration
   git push origin dev
   ```

2. **Integration Testing** (Day 1 Afternoon)
   ```bash
   git checkout dev
   docker-compose build --no-cache
   docker-compose up -d
   
   # Test all scenarios with dev profile
   SPRING_PROFILES_ACTIVE=dev ./test-scenario-1.sh
   SPRING_PROFILES_ACTIVE=dev ./test-scenario-2.sh
   SPRING_PROFILES_ACTIVE=dev ./test-scenario-3.sh
   SPRING_PROFILES_ACTIVE=dev ./test-scenario-4.sh
   
   # Test with prod profile
   docker-compose down
   SPRING_PROFILES_ACTIVE=prod docker-compose up -d
   ./test-scenario-1.sh
   ./test-scenario-2.sh
   ./test-scenario-3.sh
   ./test-scenario-4.sh
   ```

3. **Performance Testing** (Day 1 End)
   
   Create performance test:
   ```bash
   # test-performance.sh
   #!/bin/bash
   
   echo "Performance Test: Order Creation"
   
   for i in {1..100}; do
     curl -X POST http://localhost:1011/api/customer-orders \
       -H "Authorization: Bearer $TOKEN" \
       -H "Content-Type: application/json" \
       -d '{
         "orderItems": [
           {"itemId": 1, "itemType": "PRODUCT", "requestedQuantity": 1}
         ]
       }' &
   done
   
   wait
   echo "Performance test complete"
   ```

   Run and measure:
   ```bash
   chmod +x test-performance.sh
   time ./test-performance.sh
   
   # Expected: < 5 seconds for 100 orders
   ```

4. **Registry Push** (Day 2 Morning)
   ```bash
   ./push-to-registry.sh --tag dev --tag v1.2.0-dev
   ```

5. **Staging Deployment** (Day 2 Afternoon)
   
   On staging server:
   ```bash
   ssh nji@io-surf-staging
   cd ~/lego-sample-factory
   git checkout dev
   git pull origin dev
   
   # Update environment
   cp .env.template .env
   nano .env  # Set staging values
   
   # Deploy
   ./update-from-registry.sh --tag v1.2.0-dev
   
   # Smoke test
   curl http://localhost:1011/api/health
   curl http://localhost:1011/api/config/thresholds
   ```

6. **Production Deployment** (Day 2 End)
   ```bash
   # Merge to master
   git checkout master
   git pull origin master
   git merge dev
   git tag v1.2.0-phase2
   git push origin master --tags
   
   # Push production images
   ./push-to-registry.sh --tag latest --tag v1.2.0
   
   # Deploy to production
   ssh nji@io-surf
   cd ~/lego-sample-factory
   git checkout master
   git pull origin master
   
   # Backup production config
   cp .env .env.backup
   
   # Deploy
   ./update-from-registry.sh
   
   # Verify
   curl https://lego-factory.io-surf.com/api/health
   ./test-scenario-1.sh
   ./test-scenario-2.sh
   ./test-scenario-3.sh
   ./test-scenario-4.sh
   ```

**Phase 2 Deliverables:**
- ✅ Service layer refactored (4 new services)
- ✅ Configuration externalized (all profiles)
- ✅ Kubernetes manifests created
- ✅ Admin configuration endpoint
- ✅ All tests passing (100 orders/5s performance)
- ✅ Dev and prod images in registry
- ✅ Production v1.2.0 deployed

---

## PHASE 3: Performance Optimization (FUTURE)

**Timeline:** 15-20 days  
**Branches:** `feature/phase3-caching`, `feature/phase3-async`, `feature/phase3-events`  
**Risk:** MEDIUM-HIGH (changes execution model)  
**Cloud-Native Impact:** VERY HIGH (enables horizontal scaling, resilience)

**Planned Enhancements:**
1. Redis caching for masterdata (BOM, products)
2. Async processing for long operations (SimAL scheduling)
3. Event-driven architecture (Spring Events → Kafka)
4. Circuit breaker pattern (Resilience4j)
5. Batch processing optimizations
6. Database query optimizations

**This phase will be detailed after Phase 2 completion.**

---

## 🧪 Testing Strategy

### Test Pyramid

```
          /\
         /  \        E2E Tests (10%)
        /____\       - Scenario scripts
       /      \      - Production smoke tests
      /  INTE  \     
     /  GRATION \    Integration Tests (30%)
    /____________\   - Controller tests
   /              \  - Service integration
  /   UNIT TESTS  \ Unit Tests (60%)
 /__________________\ - Service logic
                     - Validator tests
                     - Exception handling
```

### Test Categories

**1. Unit Tests** (60% coverage target)
- Service logic (BomConversionService, OrderValidator)
- Exception handling (all custom exceptions)
- DTO mapping (@JsonProperty annotations)
- Configuration validation

**2. Integration Tests** (30% coverage target)
- Controller endpoints (all CRUD operations)
- Database operations (JPA repositories)
- External service clients (MasterdataClient, InventoryClient)
- Cross-service workflows

**3. End-to-End Tests** (10% coverage target)
- Scenario 1: Direct fulfillment
- Scenario 2: Warehouse order + Final Assembly
- Scenario 3: Full production pipeline
- Scenario 4: Direct production (high volume)

### Test Execution Schedule

**Before Each Commit:**
```bash
# Quick unit tests
./mvnw test

# Quick scenario validation
./test-scenario-1.sh
```

**Before Branch Merge:**
```bash
# Full test suite
./mvnw test
./mvnw verify

# All scenario tests
./test-scenario-1.sh
./test-scenario-2.sh
./test-scenario-3.sh
./test-scenario-4.sh

# Performance test
./test-performance.sh
```

**Before Production Deployment:**
```bash
# Staging validation
ssh nji@io-surf-staging
cd ~/lego-sample-factory
./test-scenario-1.sh
./test-scenario-2.sh
./test-scenario-3.sh
./test-scenario-4.sh

# Production smoke test (read-only)
curl https://lego-factory.io-surf.com/api/health
curl https://lego-factory.io-surf.com/api/config/thresholds
```

### Test Data Management

**Development:**
- H2 in-memory database
- DataInitializer seeds test data on startup
- Each test run starts with clean state

**Staging:**
- PostgreSQL with staging schema
- Seed data script for reproducible state
- Can be reset between test runs

**Production:**
- PostgreSQL with production data
- NO test scripts run against production
- Smoke tests are read-only queries only

---

## 🚀 Deployment Workflow

### Development → Staging → Production Pipeline

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│ DEVELOPMENT │────▶│   STAGING   │────▶│  PRODUCTION  │
│   (local)   │     │ (io-surf-   │     │  (io-surf)   │
│             │     │  staging)   │     │              │
└─────────────┘     └─────────────┘     └──────────────┘
      │                    │                     │
      │                    │                     │
  Feature Branch      Dev Branch           Master Branch
      │                    │                     │
      ├─ Unit Tests        ├─ Integration       ├─ E2E Tests
      ├─ Quick Smoke       ├─ All Scenarios     ├─ Performance
      └─ Code Review       └─ Load Test         └─ Monitoring
```

### Deployment Checklist

**Before Deployment:**
- [ ] All tests passing (unit + integration + E2E)
- [ ] Code review approved (PR)
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version bumped (semantic versioning)
- [ ] Docker images built and tagged
- [ ] Registry push successful

**During Deployment:**
- [ ] Backup current production database
- [ ] Backup current .env configuration
- [ ] Pull latest code (git pull origin master)
- [ ] Pull latest images (./update-from-registry.sh)
- [ ] Services restart successful
- [ ] Health checks pass

**After Deployment:**
- [ ] Smoke tests pass
- [ ] Scenario tests pass (on staging first)
- [ ] Monitoring shows normal metrics
- [ ] No error spikes in logs
- [ ] Performance metrics within SLA

**Rollback Plan:**
- [ ] Keep previous Docker images tagged (v1.1.0, v1.2.0)
- [ ] Database backup available
- [ ] .env.backup file exists
- [ ] Rollback script: `./rollback.sh v1.1.0`

---

## 🔄 Backward Compatibility Strategy

### Principles

1. **Additive Changes Only** - New features don't break existing APIs
2. **Dual Field Support** - Accept both old and new field names (componentId/moduleId)
3. **Feature Flags** - New behavior behind flags, off by default
4. **API Versioning** - When breaking changes needed, create /api/v2 endpoints
5. **Deprecation Period** - Mark old APIs as deprecated for 2 release cycles before removal

### Backward Compatibility Checklist

**API Contracts:**
- [ ] All DTOs support old field names via @JsonProperty
- [ ] No required fields removed
- [ ] New required fields have defaults
- [ ] HTTP status codes unchanged for existing endpoints

**Database:**
- [ ] No columns dropped (mark as @Deprecated instead)
- [ ] New columns are nullable or have defaults
- [ ] Liquibase/Flyway migrations are additive only
- [ ] Old queries still work (views for refactored tables)

**Service Behavior:**
- [ ] New features behind feature flags
- [ ] Default behavior unchanged
- [ ] Old workflows still supported
- [ ] Scenario 1-4 tests pass without changes

**Configuration:**
- [ ] All new config has defaults matching old behavior
- [ ] Environment variables backward compatible
- [ ] Old .env files work without modification

### Breaking Change Process

If breaking change is absolutely necessary:

1. **Version the API**
   ```
   /api/v1/customer-orders  (existing)
   /api/v2/customer-orders  (new version)
   ```

2. **Deprecation Notice**
   - Add @Deprecated annotation
   - Log warning when old API used
   - Document in API_CONTRACTS.md

3. **Transition Period**
   - Support both v1 and v2 for 2 release cycles (6 months)
   - Provide migration guide
   - Notify all API consumers

4. **Removal**
   - Remove old API in major version bump (v2.0.0)
   - Update all documentation
   - Verify no internal usage

---

## 📊 Success Metrics

### Phase 1 (API Contracts & Exceptions)

**Code Quality:**
- ✅ Zero silent deserialization failures
- ✅ All DTOs have @ApiContract annotations
- ✅ All exceptions use ErrorCode enum
- ✅ Test coverage > 80%

**Performance:**
- ✅ No regression in response times
- ✅ Error responses include context
- ✅ All scenarios complete < 10s

**Documentation:**
- ✅ API_CONTRACTS.md documents all service interactions
- ✅ All error codes documented
- ✅ Examples for all error scenarios

### Phase 2 (Service Refactoring & Configuration)

**Code Quality:**
- ✅ No service class > 300 lines
- ✅ Single Responsibility Principle applied
- ✅ Test coverage > 85%
- ✅ All magic numbers externalized

**Performance:**
- ✅ 100 concurrent orders processed < 5s
- ✅ No N+1 queries
- ✅ Database connection pool optimized

**Cloud-Native Readiness:**
- ✅ Kubernetes manifests validate successfully
- ✅ Health probes respond correctly
- ✅ Horizontal scaling possible (stateless services)
- ✅ Config externalized to environment variables

### Phase 3 (Performance Optimization - Future)

**Performance:**
- ✅ Cache hit ratio > 80% for BOM lookups
- ✅ Async operations don't block main thread
- ✅ Circuit breaker prevents cascade failures
- ✅ 1000 concurrent orders processed < 30s

**Resilience:**
- ✅ Graceful degradation when external services down
- ✅ Zero downtime during deployments
- ✅ Automatic recovery from transient errors

---

## 🛠️ Tools & Infrastructure

### Development Tools

**IDE:**
- IntelliJ IDEA Ultimate (recommended)
- VS Code with Java extensions

**Build Tools:**
- Maven 3.9+
- Docker 24.0+
- Docker Compose 2.20+

**Testing:**
- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers (for integration tests)

**Code Quality:**
- SonarQube (optional)
- Checkstyle (Maven plugin)
- SpotBugs (Maven plugin)

### CI/CD Pipeline (Future)

**GitHub Actions Workflow:**
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ master, dev ]
  pull_request:
    branches: [ master, dev ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build and Test
        run: |
          cd lego-factory-backend/order-processing-service
          ./mvnw clean verify
      - name: Run E2E Tests
        run: |
          docker-compose up -d
          ./test-scenario-1.sh
          ./test-scenario-2.sh
          ./test-scenario-3.sh
          ./test-scenario-4.sh
  
  deploy-staging:
    needs: test
    if: github.ref == 'refs/heads/dev'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Staging
        run: |
          ssh ${{ secrets.STAGING_SERVER }} 'cd ~/lego-sample-factory && ./update-from-registry.sh --tag dev'
  
  deploy-production:
    needs: test
    if: github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Production
        run: |
          ssh ${{ secrets.PRODUCTION_SERVER }} 'cd ~/lego-sample-factory && ./update-from-registry.sh --tag latest'
```

### Monitoring & Observability

**Phase 2 (Basic):**
- Spring Boot Actuator (health, metrics)
- Docker logs (docker-compose logs -f)
- Application logs (structured JSON)

**Phase 3 (Advanced - Future):**
- Prometheus (metrics collection)
- Grafana (metrics visualization)
- ELK Stack (log aggregation)
- Jaeger (distributed tracing)
- AlertManager (alerting)

---

## 📝 Documentation Updates

### Documentation Checklist

**Per Feature Branch:**
- [ ] Update API_CONTRACTS.md if API changed
- [ ] Update PRE_COMMIT_CHECKLIST.md if new checks added
- [ ] Update README.md if setup process changed
- [ ] Add entry to CHANGELOG.md

**Per Phase Completion:**
- [ ] Update PROJECT_TECHNICAL_OVERVIEW.md
- [ ] Update BusinessScenarios.md if workflows changed
- [ ] Create migration guide if breaking changes
- [ ] Update deployment scripts documentation

**Per Production Release:**
- [ ] Create release notes
- [ ] Update version in all pom.xml files
- [ ] Tag release in Git
- [ ] Update Docker image tags

---

## 🎯 Phase Completion Criteria

### Phase 1 Completion

- [ ] All DTOs have @JsonProperty where needed
- [ ] All exceptions use OrderProcessingException hierarchy
- [ ] API_CONTRACTS.md complete
- [ ] All tests passing
- [ ] Production deployed successfully
- [ ] Zero customer-reported issues for 7 days

### Phase 2 Completion

- [ ] All services < 300 lines
- [ ] All magic numbers externalized
- [ ] Kubernetes manifests tested
- [ ] All tests passing
- [ ] Production deployed successfully
- [ ] Performance benchmarks met

### Phase 3 Completion (Future)

- [ ] Caching implemented with 80%+ hit ratio
- [ ] Circuit breakers prevent cascade failures
- [ ] Event-driven architecture functional
- [ ] All tests passing
- [ ] Production deployed successfully
- [ ] System handles 10x current load

---

## 🔐 Security Considerations

### Phase 1-2 Security

**Already Implemented:**
- JWT authentication (user-service)
- Role-based access control (RBAC)
- HTTPS in production (nginx)
- SQL injection prevention (JPA/Hibernate)

**Phase 2 Additions:**
- Admin-only configuration endpoints
- Sensitive data not logged
- API rate limiting (gateway)
- Input validation (@Valid annotations)

**Phase 3 Additions (Future):**
- API key authentication for external systems
- OAuth2/OIDC integration
- Audit logging for all mutations
- Encryption at rest (database)

---

## 💡 Tips for Seamless Development

### 1. Feature Branch Best Practices

```bash
# Always start from latest master
git checkout master
git pull origin master
git checkout -b feature/my-feature

# Commit frequently with descriptive messages
git commit -m "feat(component): Add specific functionality"

# Keep branch updated with master
git checkout master
git pull origin master
git checkout feature/my-feature
git rebase master
```

### 2. Testing Before Pushing

```bash
# Quick pre-push check
./mvnw test                 # Unit tests
./test-scenario-1.sh        # Quick E2E
docker-compose logs -f      # Check for errors

# Full validation
./mvnw verify               # All tests + integration
./test-scenario-1.sh        # All scenarios
./test-scenario-2.sh
./test-scenario-3.sh
./test-scenario-4.sh
```

### 3. Debugging Tips

```bash
# Check service logs
docker-compose logs -f order-processing-service

# Check specific error
docker-compose logs order-processing-service | grep ERROR

# Restart single service
docker-compose restart order-processing-service

# Rebuild after code change
docker-compose build --no-cache order-processing-service
docker-compose up -d order-processing-service
```

### 4. Rollback Procedure

```bash
# If deployment fails
cd ~/lego-sample-factory

# Rollback to previous version
docker-compose down
docker-compose pull  # Gets previous images
docker-compose up -d

# Restore config if needed
cp .env.backup .env
docker-compose restart

# Verify
curl http://localhost:1011/api/health
./test-scenario-1.sh
```

---

## 📅 Implementation Timeline

### Month 1: Phase 1

- **Week 1-2:** API Contract Standardization
  - Days 1-2: Audit & standardize DTOs
  - Days 3-4: Create API_CONTRACTS.md
  - Days 5-7: Testing & integration

- **Week 3:** Exception Handling
  - Days 1-2: Create exception hierarchy
  - Day 3: Refactor existing code
  - Days 4-5: Testing

- **Week 4:** Phase 1 Integration & Deployment
  - Days 1-2: Merge and staging
  - Days 3-5: Production deployment & monitoring

### Month 2: Phase 2

- **Week 1-2:** Service Layer Refactoring
  - Days 1-2: Package structure & planning
  - Days 3-5: Extract services
  - Days 6-7: Controller refactoring

- **Week 3:** Configuration Externalization
  - Days 1-2: Create config classes
  - Days 3-4: Cloud manifests
  - Day 5: Testing

- **Week 4:** Phase 2 Integration & Deployment
  - Days 1-2: Integration testing
  - Days 3-5: Staging → Production

### Month 3+: Phase 3 (Future)

- **Planning and prioritization based on Phase 2 learnings**
- **Caching, async, events implemented incrementally**
- **Each sub-feature deployed independently**

---

## 🎓 Learning Resources

### Spring Boot Best Practices
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [12-Factor App Methodology](https://12factor.net/)
- [Microservices Patterns](https://microservices.io/)

### Cloud-Native Architecture
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Cloud Native Computing Foundation](https://www.cncf.io/)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)

### Testing Strategies
- [Martin Fowler - Test Pyramid](https://martinfowler.com/bliki/TestPyramid.html)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

---

## 📞 Support & Questions

**For questions during implementation:**
- Review this document first
- Check PRE_COMMIT_CHECKLIST.md for patterns
- Review API_CONTRACTS.md for service contracts
- Check Git history for similar changes

**Emergency rollback:**
- Follow rollback procedure in section 💡 Tips
- Notify team via communication channel
- Document incident for post-mortem

---

**Document Version:** 1.0  
**Last Updated:** February 2, 2026  
**Next Review:** After Phase 1 Completion
