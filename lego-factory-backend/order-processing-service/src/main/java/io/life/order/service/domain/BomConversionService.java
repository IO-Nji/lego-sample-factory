package io.life.order.service.domain;

import io.life.order.entity.OrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.exception.OrderProcessingException;
import io.life.order.service.MasterdataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BomConversionService - Bill of Materials Conversion
 * 
 * Converts product-based order items to module-based warehouse items using BOM lookup.
 * This is a core domain service extracted from FulfillmentService for single-responsibility.
 * 
 * CRITICAL: Preserves productId through the conversion for Final Assembly tracking.
 * 
 * @see FulfillmentService - original source of this logic
 * @see MasterdataService - provides BOM data from masterdata-service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BomConversionService {

    private final MasterdataService masterdataService;

    // ========================
    // INNER CLASSES FOR RESULTS
    // ========================

    /**
     * Result of BOM conversion operation - contains list of converted module items.
     */
    public record BomConversionResult(
            List<BomItem> items,
            int totalModules,
            boolean hasErrors
    ) {
        public static BomConversionResult empty() {
            return new BomConversionResult(new ArrayList<>(), 0, false);
        }
    }

    /**
     * Individual BOM item representing a module needed for a product.
     */
    public record BomItem(
            Long moduleId,
            String moduleName,
            Integer quantity,
            Long sourceProductId,
            String sourceProductName
    ) {}

    // ========================
    // CONVERSION METHODS
    // ========================

    /**
     * Convert order items (products) to BomConversionResult.
     * This simplified method doesn't require a WarehouseOrder reference.
     * 
     * @param orderItems List of product-based order items from CustomerOrder
     * @return BomConversionResult with converted module items
     */
    public BomConversionResult convertProductsToModules(List<OrderItem> orderItems) {
        log.info("Starting BOM conversion for {} order items", orderItems.size());
        
        List<BomItem> bomItems = new ArrayList<>();
        
        for (OrderItem item : orderItems) {
            try {
                if ("PRODUCT".equalsIgnoreCase(item.getItemType())) {
                    List<BomItem> converted = convertProductItemToBomItems(item);
                    bomItems.addAll(converted);
                } else {
                    // For non-products, create a direct BomItem (legacy support)
                    String itemName = getItemNameSafe(item.getItemType(), item.getItemId());
                    bomItems.add(new BomItem(
                            item.getItemId(),
                            itemName,
                            item.getQuantity(),
                            null, // No product mapping
                            null
                    ));
                }
            } catch (Exception e) {
                log.error("Error converting item {}: {}", item.getItemId(), e.getMessage());
                throw new OrderProcessingException(
                        "BOM conversion failed for item " + item.getItemId() + ": " + e.getMessage(), e)
                        .addDetail("itemId", item.getItemId())
                        .addDetail("itemType", item.getItemType());
            }
        }
        
        int totalModules = bomItems.stream().mapToInt(BomItem::quantity).sum();
        log.info("✓ BOM conversion complete: {} products → {} modules (total qty: {})",
                orderItems.size(), bomItems.size(), totalModules);
        
        return new BomConversionResult(bomItems, totalModules, false);
    }

    /**
     * Convert a single PRODUCT item to BomItems using BOM lookup.
     */
    private List<BomItem> convertProductItemToBomItems(OrderItem item) {
        Long productId = item.getItemId();
        Integer productQty = item.getQuantity();
        
        String productName = getItemNameSafe("PRODUCT", productId);
        log.info("Processing PRODUCT item: productId={}, name={}, qty={}", 
                productId, productName, productQty);
        
        // Fetch module requirements from BOM
        Map<Long, Integer> productModules = fetchModuleRequirements(productId, productQty, productName);
        
        // Convert to BomItems
        List<BomItem> bomItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : productModules.entrySet()) {
            Long moduleId = entry.getKey();
            Integer requiredQty = entry.getValue();
            
            if (moduleId == null || requiredQty == null || requiredQty <= 0) {
                log.warn("Skipping invalid module entry: moduleId={}, qty={}", moduleId, requiredQty);
                continue;
            }
            
            String moduleName = getItemNameSafe("MODULE", moduleId);
            bomItems.add(new BomItem(moduleId, moduleName, requiredQty, productId, productName));
            log.info("  ✓ Module {} ({}) qty {} for product {} ({})", 
                    moduleId, moduleName, requiredQty, productId, productName);
        }
        
        return bomItems;
    }

    /**
     * Convert order items (products) to warehouse order items using BOM lookup.
     * LEGACY method - kept for backward compatibility with existing code.
     * 
     * @param orderItems List of product-based order items from CustomerOrder
     * @param warehouseOrder The parent warehouse order for linking
     * @param orderNumber Order number for logging and error context
     * @return List of module-based warehouse items with productId preserved
     * @throws OrderProcessingException if BOM lookup or conversion fails
     */
    public List<WarehouseOrderItem> convertProductsToModules(
            List<OrderItem> orderItems,
            WarehouseOrder warehouseOrder,
            String orderNumber) {
        
        log.info("Starting BOM conversion for order {}", orderNumber);
        
        List<WarehouseOrderItem> warehouseOrderItems = new ArrayList<>();
        
        for (OrderItem item : orderItems) {
            try {
                if ("PRODUCT".equalsIgnoreCase(item.getItemType())) {
                    List<WarehouseOrderItem> convertedItems = convertProductItem(
                            item, warehouseOrder, orderNumber);
                    warehouseOrderItems.addAll(convertedItems);
                } else {
                    // For non-products, convert directly (fallback for legacy data)
                    WarehouseOrderItem woItem = convertNonProductItem(
                            item, warehouseOrder);
                    warehouseOrderItems.add(woItem);
                }
            } catch (OrderProcessingException ope) {
                log.error("❌ Failed to process order item: {}", ope.getMessage());
                throw ope;
            } catch (Exception e) {
                log.error("❌ Unexpected error processing order item {}: {}", 
                        item.getItemId(), e.getMessage(), e);
                throw new OrderProcessingException(
                        "Failed to process order item " + item.getItemId() + ": " + e.getMessage(), e)
                        .addDetail("itemId", item.getItemId())
                        .addDetail("orderNumber", orderNumber);
            }
        }
        
        // Validate that we have at least one warehouse order item
        if (warehouseOrderItems.isEmpty()) {
            log.error("❌ No warehouse order items created for order {}. Cannot proceed.", orderNumber);
            throw new OrderProcessingException(
                    "Failed to create warehouse order: No valid items after BOM conversion")
                    .addDetail("orderNumber", orderNumber);
        }
        
        log.info("✓ BOM conversion complete: {} items → {} warehouse items",
                orderItems.size(), warehouseOrderItems.size());
        
        return warehouseOrderItems;
    }

    /**
     * Convert a single PRODUCT item to multiple MODULE warehouse items using BOM.
     */
    private List<WarehouseOrderItem> convertProductItem(
            OrderItem item,
            WarehouseOrder warehouseOrder,
            String orderNumber) {
        
        Long productId = item.getItemId();
        Integer productQty = item.getQuantity();
        
        log.info("Processing PRODUCT item: productId={}, qty={}", productId, productQty);
        
        // Fetch product name with fallback
        String productName = fetchItemName("PRODUCT", productId);
        log.info("✓ Product name retrieved: {}", productName);
        
        // BOM lookup: get module requirements for this product
        log.info("BOM Lookup: Converting product {} ({}) qty {} to modules", 
                productId, productName, productQty);
        
        Map<Long, Integer> productModules = fetchModuleRequirements(productId, productQty, productName);
        
        // Convert each module requirement to a warehouse order item
        List<WarehouseOrderItem> warehouseItems = new ArrayList<>();
        
        for (Map.Entry<Long, Integer> entry : productModules.entrySet()) {
            Long moduleId = entry.getKey();
            Integer requiredQty = entry.getValue();
            
            // Validate module data
            if (moduleId == null || requiredQty == null || requiredQty <= 0) {
                log.warn("Skipping invalid module entry: moduleId={}, qty={}. BOM data issue.", 
                        moduleId, requiredQty);
                continue;
            }
            
            // Fetch module name with fallback
            String moduleName = fetchItemName("MODULE", moduleId);
            
            WarehouseOrderItem woItem = new WarehouseOrderItem();
            woItem.setWarehouseOrder(warehouseOrder);
            woItem.setItemId(moduleId);
            woItem.setProductId(productId);  // CRITICAL: Track which product this module is for
            woItem.setItemName(moduleName);
            woItem.setRequestedQuantity(requiredQty);
            woItem.setFulfilledQuantity(0);
            woItem.setItemType("MODULE");
            woItem.setNotes("For product: " + productName + " (ID: " + productId + ")");
            warehouseItems.add(woItem);
            
            log.info("  ✓ Module {} ({}) qty {} for product {} ({})", 
                    moduleId, moduleName, requiredQty, productId, productName);
        }
        
        return warehouseItems;
    }

    /**
     * Convert a non-PRODUCT item directly to a warehouse item.
     */
    private WarehouseOrderItem convertNonProductItem(
            OrderItem item,
            WarehouseOrder warehouseOrder) {
        
        log.info("Processing non-PRODUCT item: type={}, id={}, qty={}", 
                item.getItemType(), item.getItemId(), item.getQuantity());
        
        String itemName = fetchItemName(item.getItemType(), item.getItemId());
        
        WarehouseOrderItem woItem = new WarehouseOrderItem();
        woItem.setWarehouseOrder(warehouseOrder);
        woItem.setItemId(item.getItemId());
        woItem.setProductId(null);  // No product mapping for non-product items
        woItem.setItemName(itemName);
        woItem.setRequestedQuantity(item.getQuantity());
        woItem.setFulfilledQuantity(0);
        woItem.setItemType(item.getItemType());
        
        log.info("  ✓ Added non-product item: {} ({})", itemName, item.getItemType());
        
        return woItem;
    }

    /**
     * Fetch item name from masterdata-service with fallback on failure.
     */
    private String fetchItemName(String itemType, Long itemId) {
        try {
            String itemName = masterdataService.getItemName(itemType, itemId);
            if (itemName == null || itemName.trim().isEmpty()) {
                log.warn("{} name returned null/empty for id={}. Using fallback.", itemType, itemId);
                return itemType + "-" + itemId;
            }
            return itemName;
        } catch (Exception e) {
            log.warn("Failed to fetch {} name for id={}: {}. Using fallback.", 
                    itemType, itemId, e.getMessage());
            return itemType + "-" + itemId;
        }
    }

    /**
     * Fetch module requirements from BOM with validation.
     * 
     * @throws OrderProcessingException if BOM lookup fails or returns no modules
     */
    private Map<Long, Integer> fetchModuleRequirements(Long productId, Integer productQty, String productName) {
        try {
            Map<Long, Integer> productModules = masterdataService.getModuleRequirementsForProduct(
                    productId, productQty);
            
            if (productModules == null) {
                log.error("❌ BOM lookup returned null for productId={} qty={}", productId, productQty);
                throw new OrderProcessingException(
                        "BOM lookup failed: getModuleRequirementsForProduct returned null for product " + productId)
                        .addDetail("productId", productId)
                        .addDetail("productName", productName);
            }
            
            if (productModules.isEmpty()) {
                log.error("❌ BOM lookup returned empty map for productId={} qty={}. No modules defined.", 
                        productId, productQty);
                throw new OrderProcessingException(
                        "BOM lookup failed: No modules found for product " + productId + " (" + productName + ")")
                        .addDetail("productId", productId)
                        .addDetail("productName", productName);
            }
            
            log.info("✓ BOM lookup successful: {} modules required", productModules.size());
            return productModules;
            
        } catch (OrderProcessingException ope) {
            throw ope;  // Re-throw our own exceptions
        } catch (Exception e) {
            log.error("❌ BOM lookup failed for productId={}: {}", productId, e.getMessage(), e);
            throw new OrderProcessingException(
                    "BOM lookup failed for product " + productId + " (" + productName + "): " + e.getMessage(), e)
                    .addDetail("productId", productId)
                    .addDetail("productName", productName);
        }
    }

    /**
     * Check if all order items are products (as opposed to modules or parts).
     * Used to determine if BOM conversion is needed.
     */
    public boolean allItemsAreProducts(List<OrderItem> orderItems) {
        return orderItems.stream()
                .allMatch(item -> "PRODUCT".equalsIgnoreCase(item.getItemType()));
    }

    /**
     * Get the total quantity of modules needed for a list of product order items.
     * Useful for capacity planning and validation.
     */
    public int getTotalModuleCount(List<OrderItem> orderItems) {
        int total = 0;
        for (OrderItem item : orderItems) {
            if ("PRODUCT".equalsIgnoreCase(item.getItemType())) {
                try {
                    Map<Long, Integer> modules = masterdataService.getModuleRequirementsForProduct(
                            item.getItemId(), item.getQuantity());
                    if (modules != null) {
                        total += modules.values().stream().mapToInt(Integer::intValue).sum();
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch module count for product {}: {}", 
                            item.getItemId(), e.getMessage());
                }
            }
        }
        return total;
    }

    /**
     * Get item name from masterdata with safe fallback.
     * Used by the simplified BOM conversion methods.
     */
    private String getItemNameSafe(String itemType, Long itemId) {
        try {
            String name = masterdataService.getItemName(itemType, itemId);
            if (name == null || name.trim().isEmpty()) {
                return itemType + "-" + itemId;
            }
            return name;
        } catch (Exception e) {
            log.warn("Could not fetch {} name for id {}: {}", itemType, itemId, e.getMessage());
            return itemType + "-" + itemId;
        }
    }
}
