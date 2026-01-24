package io.life.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for communicating with the inventory-service.
 * Handles stock checks and updates during order fulfillment.
 */
@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:${INVENTORY_SERVICE_URL:http://inventory-service:8014}}")
    private String inventoryServiceUrl;

    public InventoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check if sufficient stock is available for a product at a workstation.
     *
     * @param workstationId The workstation ID
     * @param itemId        The product/item ID
     * @param quantity      The quantity needed
     * @return true if stock is available, false otherwise
     */
    public boolean checkStock(Long workstationId, Long itemId, Integer quantity) {
        try {
            // Determine item type based on workstation
            // Parts Supply Warehouse (ID 9) deals with PARTS
            // Modules Supermarket (ID 8) deals with MODULES
            // Plant Warehouse (ID 7) deals with PRODUCTS (not PRODUCT_VARIANT)
            String itemType;
            if (workstationId == 9L) {
                itemType = "PART";
            } else if (workstationId == 8L) {
                itemType = "MODULE";
            } else {
                itemType = "PRODUCT";
            }
            
            String url = inventoryServiceUrl + "/api/stock/workstation/" + workstationId
                    + "/item?itemType=" + itemType + "&itemId=" + itemId;
            Map<?,?> dto = restTemplate.getForObject(url, Map.class);
            Integer available = null;
            if (dto != null) {
                Object q = dto.get("quantity");
                if (q instanceof Number number) available = number.intValue();
                else if (q != null) available = Integer.parseInt(q.toString());
            }
            boolean ok = available != null && available >= quantity;
            logger.info("Stock check for workstation {} item {} ({}) qty {}: {} (available={})", 
                    workstationId, itemId, itemType, quantity, ok, available);
            return ok;
        } catch (RestClientException e) {
            logger.error("Failed to check stock with inventory-service for workstation {} item {}: {}", 
                    workstationId, itemId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if Modules Supermarket has all required modules in stock.
     * Used for Scenario 2 pure implementation.
     * 
     * @param moduleRequirements Map of module ID to required quantity
     * @return true if all modules are available in sufficient quantities
     */
    public boolean checkModulesAvailability(Map<Long, Integer> moduleRequirements) {
        logger.info("Checking availability for {} different modules in Modules Supermarket", moduleRequirements.size());
        
        for (Map.Entry<Long, Integer> entry : moduleRequirements.entrySet()) {
            Long moduleId = entry.getKey();
            Integer requiredQty = entry.getValue();
            
            if (!checkStock(8L, moduleId, requiredQty)) {
                logger.info("Module {} insufficient: required {}", moduleId, requiredQty);
                return false;
            }
        }
        
        logger.info("All required modules available in Modules Supermarket");
        return true;
    }

    /**
     * Update inventory after fulfillment.
     *
     * @param workstationId The workstation ID
     * @param itemId        The product/item ID
     * @param quantity      The quantity to deduct (positive number)
     * @return true if update was successful, false otherwise
     */
    public boolean updateStock(Long workstationId, Long itemId, Integer quantity) {
        try {
            String url = inventoryServiceUrl + "/api/stock/adjust";
            
            // Determine item type based on workstation
            // Parts Supply Warehouse (ID 9) deals with PARTS
            // Modules Supermarket (ID 8) deals with MODULES
            // Plant Warehouse (ID 7) deals with PRODUCT
            String itemType;
            if (workstationId == 9L) {
                itemType = "PART";
            } else if (workstationId == 8L) {
                itemType = "MODULE";
            } else {
                itemType = "PRODUCT";
            }
            
            Map<String, Object> request = new HashMap<>();
            request.put("workstationId", workstationId);
            request.put("itemType", itemType);
            request.put("itemId", itemId);
            request.put("delta", -Math.abs(quantity)); // Negative to deduct from stock
            request.put("reason", "ORDER_FULFILLMENT");
            request.put("notes", String.format("Stock deduction for %s fulfillment (workstation %d)", 
                    itemType.toLowerCase(), workstationId));

            restTemplate.postForObject(url, request, Map.class);
            logger.info("Stock adjusted for workstation {} item {} ({}) delta {}", 
                    workstationId, itemId, itemType, -Math.abs(quantity));
            return true;
        } catch (RestClientException e) {
            logger.error("Failed to update stock with inventory-service for workstation {} item {}: {}", 
                    workstationId, itemId, e.getMessage());
            return false;
        }
    }

    /**
     * Credit stock (add to inventory).
     * Used when Final Assembly completes and credits Plant Warehouse with finished products.
     *
     * @param workstationId The workstation ID
     * @param itemId        The product/item ID
     * @param quantity      The quantity to add (positive number)
     * @return true if update was successful, false otherwise
     */
    public boolean creditStock(Long workstationId, Long itemId, Integer quantity) {
        try {
            String url = inventoryServiceUrl + "/api/stock/adjust";
            
            // Determine item type based on workstation
            // Modules Supermarket (ID 8) deals with MODULES
            // Plant Warehouse (ID 7) deals with PRODUCTS
            String itemType = (workstationId == 8L) ? "MODULE" : "PRODUCT";
            
            Map<String, Object> request = new HashMap<>();
            request.put("workstationId", workstationId);
            request.put("itemType", itemType);
            request.put("itemId", itemId);
            request.put("delta", Math.abs(quantity)); // Positive to credit stock
            request.put("reason", "PRODUCTION_COMPLETION");
            request.put("notes", String.format("Stock credit for %s completion (workstation %d)", 
                    itemType.toLowerCase(), workstationId));

            restTemplate.postForObject(url, request, Map.class);
            logger.info("Stock credited for workstation {} item {} ({}) delta +{}", 
                    workstationId, itemId, itemType, Math.abs(quantity));
            return true;
        } catch (RestClientException e) {
            logger.error("Failed to credit stock with inventory-service for workstation {} item {}: {}", 
                    workstationId, itemId, e.getMessage());
            return false;
        }
    }

    /**
     * Get available stock for an item at a workstation.
     *
     * @param workstationId The workstation ID
     * @param itemId        The product/item ID
     * @return Available quantity, or -1 if unable to check
     */
    public Integer getAvailableStock(Long workstationId, Long itemId) {
        try {
            String url = inventoryServiceUrl + "/api/stock/workstation/" + workstationId
                    + "/item?itemType=PRODUCT&itemId=" + itemId;
            Map<?,?> dto = restTemplate.getForObject(url, Map.class);
            Integer available = null;
            if (dto != null) {
                Object q = dto.get("quantity");
                if (q instanceof Number number) available = number.intValue();
                else if (q != null) available = Integer.parseInt(q.toString());
            }
            logger.info("Available stock for workstation {} item {}: {}", workstationId, itemId, available);
            return available != null ? available : 0;
        } catch (RestClientException e) {
            logger.error("Failed to get available stock from inventory-service", e);
            return -1;
        }
    }

    /**
     * Adjust stock using a custom request map (for crediting stock).
     * Used for warehouse replenishment when Plant Warehouse needs to receive stock.
     *
     * @param request Map containing workstationId, itemType, itemId, delta, reason, notes
     * @return true if adjustment was successful, false otherwise
     */
    public boolean adjustStock(Map<String, Object> request) {
        try {
            String url = inventoryServiceUrl + "/api/stock/adjust";
            restTemplate.postForObject(url, request, Map.class);
            logger.info("Stock adjusted: {}", request);
            return true;
        } catch (RestClientException e) {
            logger.error("Failed to adjust stock with inventory-service", e);
            return false;
        }
    }

    /**
     * Get the inventory service base URL (for use by other services).
     */
    public String getInventoryServiceUrl() {
        return inventoryServiceUrl;
    }
}
