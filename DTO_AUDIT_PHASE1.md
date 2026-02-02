# DTO Audit - Phase 1: API Contract Standardization

**Date:** February 2, 2026  
**Branch:** feature/phase1-api-contracts  
**Objective:** Identify field name mismatches between DTOs and external APIs to prevent silent deserialization failures

---

## Audit Methodology

1. **Scan all DTO files** across all services
2. **Identify external API sources** (frontend, cross-service calls)
3. **Compare field names** (API response vs Java field name)
4. **Document mismatches** requiring @JsonProperty annotations
5. **Mark already fixed** DTOs (componentId/moduleId, requestedQuantity/quantity)

---

## DTO Inventory by Service

### Order Processing Service (17 DTOs)

#### Main DTOs (11 files)
- [x] `CustomerOrderDTO.java` - ✅ **ANNOTATED** (frontend, customer orders)
- [x] `WarehouseOrderDTO.java` - ✅ **ANNOTATED** (frontend, warehouse orders)
- [x] `ProductionOrderDTO.java` - ✅ **ANNOTATED** (frontend, production with SimAL)
- [x] `FinalAssemblyOrderDTO.java` - ✅ **ANNOTATED** (frontend, WS-6 assembly)
- [x] `OrderItemDTO.java` - ✅ **FIXED + ANNOTATED** (requestedQuantity → quantity)
- [x] `WarehouseOrderItemDTO.java` - ⏭️ **SKIPPED** (nested in WarehouseOrderDTO)
- [x] `ProductionControlOrderDTO.java` - ✅ **ANNOTATED** (frontend, manufacturing control)
- [x] `AssemblyControlOrderDTO.java` - ✅ **ANNOTATED** (frontend, assembly control)
- [x] `SupplyOrderDTO.java` - ✅ **ANNOTATED** (frontend, WS-9 supply)
- [x] `SupplyOrderItemDTO.java` - ⏭️ **SKIPPED** (nested in SupplyOrderDTO)
- [x] `SystemConfigurationDTO.java` - ✅ **ANNOTATED** (frontend, admin config)

#### Masterdata Client DTOs (5 files)
- [x] `masterdata/ProductDTO.java` - ✅ **ANNOTATED** (masterdata-service)
- [x] `masterdata/ModuleDTO.java` - ✅ **ANNOTATED** (masterdata-service)
- [x] `masterdata/PartDTO.java` - ✅ **ANNOTATED** (masterdata-service)
- [x] `masterdata/BomEntryDTO.java` - ✅ **ANNOTATED** (masterdata-service, BOM)
- [x] `masterdata/WorkstationDTO.java` - ✅ **ANNOTATED** (masterdata-service)

**AUDIT COMPLETE: 14/17 annotated, 2 skipped (nested), 1 already fixed**

### Other Services
- [ ] User Service DTOs - **TODO: Scan**
- [ ] Inventory Service DTOs - **TODO: Scan**
- [ ] Masterdata Service DTOs (source) - **TODO: Scan**
- [ ] SimAL Integration Service DTOs - **TODO: Scan**

---

## Detailed Audit Results

### ✅ ALREADY FIXED

#### 1. OrderItemDTO
**Location:** `order-processing-service/src/main/java/io/life/order/dto/OrderItemDTO.java`

**Issue:** Frontend sends `requestedQuantity` but DTO expects `quantity`

**Fix Applied:**
```java
@JsonProperty("requestedQuantity")
private void setRequestedQuantity(Integer requestedQuantity) {
    this.quantity = requestedQuantity;
}
```

**Status:** ✅ Fixed on February 1, 2026

---

#### 2. ProductModuleDTO (in MasterdataService.java)
**Location:** `order-processing-service/src/main/java/io/life/order/client/MasterdataService.java`

**Issue:** masterdata-service API returns `componentId` but DTO expects `moduleId`

**Fix Applied:**
```java
private Long moduleId;
private Long componentId;

public Long getModuleId() {
    return moduleId != null ? moduleId : componentId;
}
```

**Status:** ✅ Fixed on February 1, 2026

---

## 🔍 AUDIT IN PROGRESS

### Scanning Strategy

1. **Read each DTO file**
2. **Identify all fields**
3. **Determine API source:**
   - Frontend API (POST/PUT requests from React)
   - Cross-service API (GET/POST from other microservices)
   - Internal only (no external exposure)
4. **Check for naming convention mismatches:**
   - snake_case API → camelCase Java
   - Different terminology (componentId vs moduleId)
   - Quantity field variations (requestedQty, qty, quantity)

### Current Progress: 2/17 DTOs audited

---

## Next Steps

1. ✅ Create feature branch `feature/phase1-api-contracts`
2. ⏳ **IN PROGRESS:** Audit all DTOs (starting with order-processing-service)
3. ⏳ Create @ApiContract annotation
4. ⏳ Add @JsonProperty annotations where needed
5. ⏳ Create API_CONTRACTS.md documentation
6. ⏳ Write integration tests
7. ⏳ Build and test

---

## Notes

- This audit document is for development use only (not committed to repo)
- Findings will be documented in API_CONTRACTS.md for official reference
- All changes will be backward compatible (dual field support)

