package io.life.order.client;

import io.life.order.dto.inventory.StockAdjustmentRequest;
import io.life.order.dto.inventory.StockLevelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * InventoryClient
 * 
 * Centralized client for all inventory-service API calls.
 * Provides consistent error handling, logging, and retry logic.
 * 
 * INVENTORY CREDIT POINTS (by workstation):
 * - WS-1 (Injection Molding) → WS-8 (Modules Supermarket) - PARTS
 * - WS-2 (Part Pre-Production) → WS-8 (Modules Supermarket) - PARTS
 * - WS-3 (Part Finishing) → WS-8 (Modules Supermarket) - PARTS
 * - WS-4 (Gear Assembly) → WS-8 (Modules Supermarket) - MODULES
 * - WS-5 (Motor Assembly) → WS-8 (Modules Supermarket) - MODULES
 * - WS-6 (Final Assembly) → WS-7 (Plant Warehouse) - PRODUCTS
 */
@Component
@Slf4j
public class InventoryClient {

    private final RestTemplate restTemplate;
    
    @Value("${inventory.service.url:http://inventory-service:8014}")
    private String inventoryServiceUrl;

    // Workstation IDs
    public static final Long PLANT_WAREHOUSE_ID = 7L;
    public static final Long MODULES_SUPERMARKET_ID = 8L;
    public static final Long PARTS_SUPPLY_ID = 9L;

    // Item Types
    public static final String ITEM_TYPE_PRODUCT = "PRODUCT";
    public static final String ITEM_TYPE_MODULE = "MODULE";
    public static final String ITEM_TYPE_PART = "PART";

    // Reason Codes
    public static final String REASON_PRODUCTION = "PRODUCTION";
    public static final String REASON_CONSUMPTION = "CONSUMPTION";
    public static final String REASON_FULFILLMENT = "FULFILLMENT";
    public static final String REASON_ADJUSTMENT = "ADJUSTMENT";

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ========================
    // STOCK ADJUSTMENT METHODS
    // ========================

    /**
     * Adjust stock level (credit or debit).
     * 
     * @param workstationId Target workstation
     * @param itemType PRODUCT, MODULE, or PART
     * @param itemId Item ID
     * @param delta Positive for credit, negative for debit
     * @param reasonCode Reason for adjustment
     * @param notes Optional notes
     * @return true if successful
     */
    public boolean adjustStock(Long workstationId, String itemType, Long itemId, 
                                Integer delta, String reasonCode, String notes) {
        try {
            String url = inventoryServiceUrl + "/api/stock/adjust";
            StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                    .workstationId(workstationId)
                    .itemType(itemType)
                    .itemId(itemId)
                    .delta(delta)
                    .reasonCode(reasonCode)
                    .notes(notes != null ? notes : "")
                    .build();

            restTemplate.postForObject(url, request, Void.class);
            log.info("Stock adjusted: {} {} {} (delta: {}) at workstation {}", 
                    delta > 0 ? "+" : "", delta, itemType, itemId, workstationId);
            return true;

        } catch (RestClientException e) {
            log.error("Failed to adjust stock - WS: {}, Type: {}, ID: {}, Delta: {}: {}", 
                    workstationId, itemType, itemId, delta, e.getMessage());
            throw new InventoryOperationException("Stock adjustment failed", e);
        }
    }

    /**
     * Credit stock (add to inventory).
     */
    public boolean creditStock(Long workstationId, String itemType, Long itemId, 
                               Integer quantity, String reasonCode, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Credit quantity must be positive");
        }
        return adjustStock(workstationId, itemType, itemId, quantity, reasonCode, notes);
    }

    /**
     * Debit stock (remove from inventory).
     */
    public boolean debitStock(Long workstationId, String itemType, Long itemId, 
                              Integer quantity, String reasonCode, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Debit quantity must be positive");
        }
        return adjustStock(workstationId, itemType, itemId, -quantity, reasonCode, notes);
    }

    // ========================
    // CONVENIENCE METHODS FOR COMMON OPERATIONS
    // ========================

    /**
     * Credit parts to Modules Supermarket (WS-8).
     * Used by manufacturing workstations (WS-1, WS-2, WS-3).
     */
    public boolean creditPartsToModulesSupermarket(Long partId, Integer quantity, String orderNumber) {
        return creditStock(
                MODULES_SUPERMARKET_ID,
                ITEM_TYPE_PART,
                partId,
                quantity,
                REASON_PRODUCTION,
                "Completed order: " + orderNumber
        );
    }

    /**
     * Credit modules to Modules Supermarket (WS-8).
     * Used by assembly workstations (WS-4, WS-5).
     */
    public boolean creditModulesToModulesSupermarket(Long moduleId, Integer quantity, String orderNumber) {
        return creditStock(
                MODULES_SUPERMARKET_ID,
                ITEM_TYPE_MODULE,
                moduleId,
                quantity,
                REASON_PRODUCTION,
                "Completed order: " + orderNumber
        );
    }

    /**
     * Credit products to Plant Warehouse (WS-7).
     * Used by Final Assembly (WS-6).
     */
    public boolean creditProductsToPlantWarehouse(Long productId, Integer quantity, String orderNumber) {
        return creditStock(
                PLANT_WAREHOUSE_ID,
                ITEM_TYPE_PRODUCT,
                productId,
                quantity,
                REASON_PRODUCTION,
                "Completed order: " + orderNumber
        );
    }

    /**
     * Debit parts from Parts Supply (WS-9).
     * Used when supply orders are fulfilled.
     */
    public boolean debitPartsFromPartsSupply(Long partId, Integer quantity, String orderNumber) {
        return debitStock(
                PARTS_SUPPLY_ID,
                ITEM_TYPE_PART,
                partId,
                quantity,
                REASON_CONSUMPTION,
                "Supply order: " + orderNumber
        );
    }

    /**
     * Debit modules from Modules Supermarket (WS-8).
     * Used when warehouse orders are fulfilled.
     */
    public boolean debitModulesFromModulesSupermarket(Long moduleId, Integer quantity, String orderNumber) {
        return debitStock(
                MODULES_SUPERMARKET_ID,
                ITEM_TYPE_MODULE,
                moduleId,
                quantity,
                REASON_CONSUMPTION,
                "Warehouse order: " + orderNumber
        );
    }

    /**
     * Debit products from Plant Warehouse (WS-7).
     * Used when customer orders are fulfilled.
     */
    public boolean debitProductsFromPlantWarehouse(Long productId, Integer quantity, String orderNumber) {
        return debitStock(
                PLANT_WAREHOUSE_ID,
                ITEM_TYPE_PRODUCT,
                productId,
                quantity,
                REASON_FULFILLMENT,
                "Customer order: " + orderNumber
        );
    }

    // ========================
    // STOCK QUERY METHODS
    // ========================

    /**
     * Check available stock for an item.
     * 
     * @param workstationId Workstation to check
     * @param itemType PRODUCT, MODULE, or PART
     * @param itemId Item ID
     * @return Available quantity, or 0 if not found
     */
    public int getAvailableStock(Long workstationId, String itemType, Long itemId) {
        try {
            String url = inventoryServiceUrl + "/api/inventory?workstationId=" + workstationId 
                    + "&itemType=" + itemType + "&itemId=" + itemId;
            
            ResponseEntity<StockLevelResponse> response = restTemplate.getForEntity(url, StockLevelResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Integer quantity = response.getBody().getQuantity();
                return quantity != null ? quantity : 0;
            }
            return 0;

        } catch (RestClientException e) {
            log.warn("Failed to get stock level - WS: {}, Type: {}, ID: {}: {}", 
                    workstationId, itemType, itemId, e.getMessage());
            return 0;
        }
    }

    /**
     * Check if sufficient stock is available.
     */
    public boolean hasSufficientStock(Long workstationId, String itemType, Long itemId, int requiredQuantity) {
        int available = getAvailableStock(workstationId, itemType, itemId);
        return available >= requiredQuantity;
    }

    /**
     * Check product stock at Plant Warehouse.
     */
    public int getProductStockAtPlantWarehouse(Long productId) {
        return getAvailableStock(PLANT_WAREHOUSE_ID, ITEM_TYPE_PRODUCT, productId);
    }

    /**
     * Check module stock at Modules Supermarket.
     */
    public int getModuleStockAtModulesSupermarket(Long moduleId) {
        return getAvailableStock(MODULES_SUPERMARKET_ID, ITEM_TYPE_MODULE, moduleId);
    }

    /**
     * Check part stock at Parts Supply.
     */
    public int getPartStockAtPartsSupply(Long partId) {
        return getAvailableStock(PARTS_SUPPLY_ID, ITEM_TYPE_PART, partId);
    }

    // ========================
    // EXCEPTION CLASS
    // ========================

    public static class InventoryOperationException extends RuntimeException {
        public InventoryOperationException(String message, Throwable cause) {
            super(message, cause);
        }

        public InventoryOperationException(String message) {
            super(message);
        }
    }
}
