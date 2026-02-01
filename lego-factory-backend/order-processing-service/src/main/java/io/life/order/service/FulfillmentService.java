package io.life.order.service;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.exception.OrderProcessingException;
import io.life.order.repository.CustomerOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fulfillment Service implementing the 4 scenarios for order processing.
 *
 * Scenario 1: Direct Fulfillment (All items available locally)
 *   - Check inventory at the order's workstation
 *   - If all items available: Deduct from local inventory, mark order as COMPLETED
 *
 * Scenario 2: Warehouse Order (Items not available locally, need warehouse supply)
 *   - Create WarehouseOrder for missing items
 *   - Mark CustomerOrder as PROCESSING (waiting for warehouse supply)
 *   - When warehouse supplies: Update inventory and complete order
 *
 * Scenario 3: Modules Supermarket (Partially available, request modules)
 *   - For items available: Direct fulfill locally
 *   - For missing modules: Create request to Modules Supermarket
 *   - Mark order as PROCESSING until all items arrive
 *
 * Scenario 4: Production Planning (Complex/Custom items)
 *   - For items requiring production: Route to Production Planning
 *   - Standard items: Process locally if available
 *   - Mark order as PROCESSING with production status
 */
import org.springframework.context.annotation.Lazy;
@Service
public class FulfillmentService {

    private static final Logger logger = LoggerFactory.getLogger(FulfillmentService.class);
    private static final Long MODULES_SUPERMARKET_WORKSTATION_ID = 8L;
    private static final String CUSTOMER = "CUSTOMER";
    private static final String PROCESSING = "PROCESSING";

    private final CustomerOrderRepository customerOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final InventoryService inventoryService;
    private final ProductionOrderService productionOrderService;
    private final OrderAuditService orderAuditService;
    private final MasterdataService masterdataService;

    public FulfillmentService(CustomerOrderRepository customerOrderRepository,
                            WarehouseOrderRepository warehouseOrderRepository,
                            InventoryService inventoryService,
                            ProductionOrderService productionOrderService,
                            @Lazy OrderAuditService orderAuditService,
                            MasterdataService masterdataService) {
        this.customerOrderRepository = customerOrderRepository;
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.inventoryService = inventoryService;
        this.productionOrderService = productionOrderService;
        this.orderAuditService = orderAuditService;
        this.masterdataService = masterdataService;
    }

    /**
     * Process fulfillment for a customer order.
     * Determines which scenario applies and executes the appropriate logic.
     */
    @Transactional
    public CustomerOrderDTO fulfillOrder(Long orderId) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        CustomerOrder order = orderOpt.get();
        orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "FULFILLMENT_STARTED",
            "Fulfillment started for order " + order.getOrderNumber());
        logger.info("Starting fulfillment for order {} ({})", order.getId(), order.getOrderNumber());

        // Re-check stock at fulfill time (may have changed since confirmation)
        boolean allItemsAvailable = order.getOrderItems().stream()
                .allMatch(item -> {
                    boolean hasStock = inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
                    logger.info("  Stock check at fulfill: item {} qty {} - available: {}", 
                        item.getItemId(), item.getQuantity(), hasStock);
                    return hasStock;
                });

        if (allItemsAvailable) {
            // Scenario 1: Direct Fulfillment
            logger.info("All items available - proceeding with direct fulfillment");
            return scenario1_DirectFulfillment(order);
        } else {
            logger.info("Stock insufficient at fulfill time - stock changed since confirmation");
            // Check if any items are available
            boolean anyItemsAvailable = order.getOrderItems().stream()
                    .anyMatch(item -> inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity()));

            if (anyItemsAvailable) {
                // Scenario 3: Modules Supermarket (partial availability)
                logger.info("Partial stock available - routing to Scenario 3 (Modules Supermarket)");
                return scenario3_ModulesSupermarket(order);
            } else {
                // Scenario 2: Warehouse Order (nothing available locally)
                logger.info("No stock available - routing to Scenario 2 (Warehouse Order)");
                return scenario2_WarehouseOrder(order);
            }
        }
    }

    /**
     * Scenario 1: Direct Fulfillment
     * All items are available at the order's workstation.
     * Deduct inventory and complete the order immediately.
     */
    private CustomerOrderDTO scenario1_DirectFulfillment(CustomerOrder order) {
        logger.info("Scenario 1: Direct Fulfillment for order {}", order.getOrderNumber());

        // Deduct all items from inventory
        boolean allUpdatesSuccessful = order.getOrderItems().stream()
                .allMatch(item -> {
                    boolean ok = inventoryService.updateStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
                    if (ok) {
                        item.setFulfilledQuantity(item.getQuantity());
                    }
                    return ok;
                });

        if (allUpdatesSuccessful) {
            order.setStatus("COMPLETED");
            logger.info("Order {} fulfilled directly. Inventory updated.", order.getOrderNumber());
            orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "COMPLETED", "Order fulfilled directly (Scenario 1)");
            
            // Update triggerScenario for other CONFIRMED orders at this workstation
            updateOtherConfirmedOrdersScenario(order.getWorkstationId(), order.getId());
        } else {
            order.setStatus("CANCELLED");
            logger.warn("Order {} fulfillment failed during inventory update.", order.getOrderNumber());
            orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "CANCELLED", "Inventory update failed during direct fulfillment");
        }

        return mapToDTO(customerOrderRepository.save(order));
    }

    /**
     * Scenario 2: Warehouse Order (via Modules Supermarket)
     * When products are not available in Plant Warehouse, create a WarehouseOrder
     * that will be routed to Modules Supermarket (WS-8).
     * 
     * BOM INTEGRATION: Converts products to modules using Bill of Materials lookup.
     * Tracks productId in WarehouseOrderItem for Final Assembly.
     * 
     * ENHANCED WITH DEFENSIVE ERROR HANDLING (Feb 2026):
     * - Try-catch around entire method
     * - Null checks for all masterdataService responses
     * - Fallback values for product/module names if service fails
     * - Empty map validation after BOM lookup
     * - Invalid moduleId/quantity filtering
     * - Detailed logging at each step
     */
    private CustomerOrderDTO scenario2_WarehouseOrder(CustomerOrder order) {
        logger.info("Scenario 2: Warehouse Order for order {}", order.getOrderNumber());

        try {
            // Create a new WarehouseOrder
            WarehouseOrder warehouseOrder = new WarehouseOrder();
            warehouseOrder.setOrderNumber("WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            warehouseOrder.setCustomerOrderId(order.getId());
            warehouseOrder.setWorkstationId(MODULES_SUPERMARKET_WORKSTATION_ID); // Modules Supermarket (8)
            warehouseOrder.setOrderDate(LocalDateTime.now());
            warehouseOrder.setStatus("PENDING");
            warehouseOrder.setTriggerScenario("SCENARIO_2");
            warehouseOrder.setNotes("Auto-generated from customer order " + order.getOrderNumber());

            // BOM INTEGRATION: Convert products to modules using Bill of Materials
            // Track which product each module group belongs to (for Final Assembly)
            List<WarehouseOrderItem> warehouseOrderItems = new ArrayList<>();
            
            // Process each customer order item separately to preserve product-to-module mapping
            for (OrderItem item : order.getOrderItems()) {
                try {
                    if ("PRODUCT".equalsIgnoreCase(item.getItemType())) {
                        Long productId = item.getItemId();
                        Integer productQty = item.getQuantity();
                        
                        logger.info("Processing PRODUCT item: productId={}, qty={}", productId, productQty);
                        
                        // Fetch product name from masterdata-service with null check
                        String productName;
                        try {
                            productName = masterdataService.getItemName("PRODUCT", productId);
                            if (productName == null || productName.trim().isEmpty()) {
                                logger.warn("Product name returned null/empty for productId={}. Using fallback.", productId);
                                productName = "Product-" + productId;
                            }
                            logger.info("✓ Product name retrieved: {}", productName);
                        } catch (Exception e) {
                            logger.error("Failed to fetch product name for productId={}: {}", productId, e.getMessage());
                            productName = "Product-" + productId;
                        }
                        
                        // Use BOM to get module requirements for this specific product
                        logger.info("BOM Lookup: Converting product {} ({}) qty {} to modules", productId, productName, productQty);
                        
                        java.util.Map<Long, Integer> productModules;
                        try {
                            productModules = masterdataService.getModuleRequirementsForProduct(productId, productQty);
                            
                            // Validate BOM response is not null or empty
                            if (productModules == null) {
                                logger.error("❌ BOM lookup returned null for productId={} qty={}. Cannot convert to modules.", productId, productQty);
                                throw new OrderProcessingException(
                                    "BOM lookup failed: getModuleRequirementsForProduct returned null for product " + productId
                                );
                            }
                            
                            if (productModules.isEmpty()) {
                                logger.error("❌ BOM lookup returned empty map for productId={} qty={}. No modules defined for this product.", productId, productQty);
                                throw new OrderProcessingException(
                                    "BOM lookup failed: No modules found for product " + productId + " (" + productName + ")"
                                );
                            }
                            
                            logger.info("✓ BOM lookup successful: {} modules required", productModules.size());
                        } catch (OrderProcessingException ope) {
                            throw ope; // Re-throw our own exceptions
                        } catch (Exception e) {
                            logger.error("❌ BOM lookup failed for productId={}: {}", productId, e.getMessage(), e);
                            throw new OrderProcessingException(
                                "BOM lookup failed for product " + productId + " (" + productName + "): " + e.getMessage(),
                                e
                            );
                        }
                        
                        // Create warehouse order items for each module, tracking the product ID
                        for (java.util.Map.Entry<Long, Integer> entry : productModules.entrySet()) {
                            Long moduleId = entry.getKey();
                            Integer requiredQty = entry.getValue();
                            
                            // Validate module data
                            if (moduleId == null || requiredQty == null || requiredQty <= 0) {
                                logger.warn("Skipping invalid module entry: moduleId={}, qty={}. This indicates BOM data issue.", moduleId, requiredQty);
                                continue;
                            }
                            
                            // Fetch module name from masterdata-service with null check
                            String moduleName;
                            try {
                                moduleName = masterdataService.getItemName("MODULE", moduleId);
                                if (moduleName == null || moduleName.trim().isEmpty()) {
                                    logger.warn("Module name returned null/empty for moduleId={}. Using fallback.", moduleId);
                                    moduleName = "Module-" + moduleId;
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to fetch module name for moduleId={}: {}. Using fallback.", moduleId, e.getMessage());
                                moduleName = "Module-" + moduleId;
                            }
                            
                            WarehouseOrderItem woItem = new WarehouseOrderItem();
                            woItem.setWarehouseOrder(warehouseOrder);
                            woItem.setItemId(moduleId);
                            woItem.setProductId(productId); // Track which product this module is for
                            woItem.setItemName(moduleName);
                            woItem.setRequestedQuantity(requiredQty);
                            woItem.setFulfilledQuantity(0);
                            woItem.setItemType("MODULE");
                            woItem.setNotes("For product: " + productName + " (ID: " + productId + ")");
                            warehouseOrderItems.add(woItem);
                            
                            logger.info("  ✓ Module {} ({}) qty {} for product {} ({})", moduleId, moduleName, requiredQty, productId, productName);
                        }
                    } else {
                        // For non-products, add directly (fallback for legacy data)
                        logger.info("Processing non-PRODUCT item: type={}, id={}, qty={}", item.getItemType(), item.getItemId(), item.getQuantity());
                        
                        String itemName;
                        try {
                            itemName = masterdataService.getItemName(item.getItemType(), item.getItemId());
                            if (itemName == null || itemName.trim().isEmpty()) {
                                logger.warn("Item name returned null/empty. Using fallback.");
                                itemName = item.getItemType() + "-" + item.getItemId();
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to fetch item name: {}. Using fallback.", e.getMessage());
                            itemName = item.getItemType() + "-" + item.getItemId();
                        }
                        
                        WarehouseOrderItem woItem = new WarehouseOrderItem();
                        woItem.setWarehouseOrder(warehouseOrder);
                        woItem.setItemId(item.getItemId());
                        woItem.setProductId(null); // No product mapping for non-product items
                        woItem.setItemName(itemName);
                        woItem.setRequestedQuantity(item.getQuantity());
                        woItem.setFulfilledQuantity(0);
                        woItem.setItemType(item.getItemType());
                        warehouseOrderItems.add(woItem);
                        
                        logger.info("  ✓ Added non-product item: {} ({})", itemName, item.getItemType());
                    }
                } catch (OrderProcessingException ope) {
                    // Log and re-throw processing exceptions
                    logger.error("❌ Failed to process order item: {}", ope.getMessage());
                    throw ope;
                } catch (Exception e) {
                    // Wrap unexpected exceptions
                    logger.error("❌ Unexpected error processing order item {}: {}", item.getItemId(), e.getMessage(), e);
                    throw new OrderProcessingException(
                        "Failed to process order item " + item.getItemId() + ": " + e.getMessage(),
                        e
                    );
                }
            }
            
            // Validate that we have at least one warehouse order item
            if (warehouseOrderItems.isEmpty()) {
                logger.error("❌ No warehouse order items created for customer order {}. Cannot proceed.", order.getOrderNumber());
                throw new OrderProcessingException(
                    "Failed to create warehouse order: No valid items after BOM conversion"
                );
            }
            
            warehouseOrder.setOrderItems(warehouseOrderItems);

            // Persist warehouse order
            warehouseOrderRepository.save(warehouseOrder);
            logger.info("✓ Created warehouse order {} with {} items for customer order {}", 
                       warehouseOrder.getOrderNumber(), warehouseOrderItems.size(), order.getOrderNumber());
            orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "WAREHOUSE_ORDER_CREATED",
                "Warehouse order " + warehouseOrder.getOrderNumber() + " created (Scenario 2)");

            // NOTE: Production order is NOT auto-triggered.
            // Modules Supermarket will confirm the order and manually decide:
            // - Fulfill directly if modules are in stock
            // - Request production if modules are not available

            // Update customer order status
            order.setStatus(PROCESSING);
            order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + 
                          "Scenario 2: Warehouse order " + warehouseOrder.getOrderNumber() + 
                          " created (awaiting Modules Supermarket confirmation)");
            orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "STATUS_PROCESSING", 
                                             "Scenario 2 processing (waiting on warehouse)");

            logger.info("✓ Scenario 2 completed successfully for order {}", order.getOrderNumber());
            return mapToDTO(customerOrderRepository.save(order));
            
        } catch (OrderProcessingException ope) {
            // Log business logic errors
            logger.error("❌ Scenario 2 failed for order {}: {}", order.getOrderNumber(), ope.getMessage());
            orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "FULFILLMENT_ERROR",
                "Scenario 2 failed: " + ope.getMessage());
            throw ope;
        } catch (Exception e) {
            // Log unexpected errors with full stack trace
            logger.error("❌ Unexpected error in Scenario 2 for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "FULFILLMENT_ERROR",
                "Scenario 2 unexpected error: " + e.getMessage());
            throw new OrderProcessingException(
                "Failed to create warehouse order for customer order " + order.getOrderNumber() + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Scenario 3: Modules Supermarket
     * Some items available locally, others need to come from Modules Supermarket.
     * Fulfill what's available, request missing items via warehouse order.
     * Auto-trigger production order for items not in Modules Supermarket.
     */
    private CustomerOrderDTO scenario3_ModulesSupermarket(CustomerOrder order) {
        logger.info("Scenario 3: Modules Supermarket for order {}", order.getOrderNumber());

        // Create warehouse order for unavailable items
        WarehouseOrder warehouseOrder = new WarehouseOrder();
        warehouseOrder.setOrderNumber("WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        warehouseOrder.setCustomerOrderId(order.getId());
        warehouseOrder.setWorkstationId(MODULES_SUPERMARKET_WORKSTATION_ID); // Modules Supermarket (8)
        warehouseOrder.setOrderDate(LocalDateTime.now());
        warehouseOrder.setStatus("PENDING");
        warehouseOrder.setTriggerScenario("SCENARIO_3");
        warehouseOrder.setNotes("Auto-generated from customer order " + order.getOrderNumber() + " (partial fulfillment)");

        // BOM INTEGRATION: Convert unavailable products to module requirements
        // Track which product each module group belongs to
        List<WarehouseOrderItem> warehouseOrderItems = new ArrayList<>();
        
        // Fulfill available items and collect BOM requirements for unavailable ones
        for (OrderItem item : order.getOrderItems()) {
            if ("PRODUCT".equalsIgnoreCase(item.getItemType())) {
                Long productId = item.getItemId();
                Integer productQty = item.getQuantity();
                
                // Fetch product name from masterdata-service
                String productName = masterdataService.getItemName("PRODUCT", productId);
                
                // Check if product is available at Plant Warehouse
                if (inventoryService.checkStock(order.getWorkstationId(), productId, productQty)) {
                    // Fulfill from local stock
                    inventoryService.updateStock(order.getWorkstationId(), productId, productQty);
                    item.setFulfilledQuantity((item.getFulfilledQuantity() == null ? 0 : item.getFulfilledQuantity()) + productQty);
                    logger.info("  - Product {} ({}) fulfilled from local stock", productId, productName);
                } else {
                    // Product not available - use BOM to convert to module requirements
                    logger.info("BOM Lookup: Converting unavailable product {} ({}) qty {} to modules", productId, productName, productQty);
                    java.util.Map<Long, Integer> productModules = masterdataService.getModuleRequirementsForProduct(
                        productId, 
                        productQty
                    );
                    
                    // Create warehouse order items for each module, tracking the product ID
                    for (java.util.Map.Entry<Long, Integer> entry : productModules.entrySet()) {
                        Long moduleId = entry.getKey();
                        Integer requiredQty = entry.getValue();
                        
                        WarehouseOrderItem woItem = new WarehouseOrderItem();
                        woItem.setWarehouseOrder(warehouseOrder);
                        woItem.setItemId(moduleId);
                        woItem.setProductId(productId); // Track which product this module is for
                        
                        // Fetch module name from masterdata-service
                        String moduleName = masterdataService.getItemName("MODULE", moduleId);
                        woItem.setItemName(moduleName);
                        woItem.setRequestedQuantity(requiredQty);
                        woItem.setFulfilledQuantity(0);
                        woItem.setItemType("MODULE");
                        woItem.setNotes("For product: " + productName + " (ID: " + productId + ")");
                        warehouseOrderItems.add(woItem);
                        
                        logger.info("  - Module {} ({}) qty {} for product {} ({})", moduleId, moduleName, requiredQty, productId, productName);
                    }
                }
            } else {
                // For non-products, check availability and add to warehouse order if missing
                if (inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity())) {
                    inventoryService.updateStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
                    item.setFulfilledQuantity((item.getFulfilledQuantity() == null ? 0 : item.getFulfilledQuantity()) + item.getQuantity());
                    logger.info("  - Item {} fulfilled from local stock", item.getItemId());
                } else {
                    String itemName = masterdataService.getItemName(item.getItemType(), item.getItemId());
                    
                    WarehouseOrderItem woItem = new WarehouseOrderItem();
                    woItem.setWarehouseOrder(warehouseOrder);
                    woItem.setItemId(item.getItemId());
                    woItem.setProductId(null); // No product mapping for non-product items
                    woItem.setItemName(itemName);
                    woItem.setRequestedQuantity(item.getQuantity());
                    woItem.setFulfilledQuantity(0);
                    woItem.setItemType(item.getItemType());
                    warehouseOrderItems.add(woItem);
                }
            }
        }

        // Only save warehouse order if there are items to request
        if (!warehouseOrderItems.isEmpty()) {
            warehouseOrder.setOrderItems(warehouseOrderItems);
            warehouseOrderRepository.save(warehouseOrder);
                logger.info("Created warehouse order {} for customer order {}", warehouseOrder.getOrderNumber(), order.getOrderNumber());
                orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "WAREHOUSE_ORDER_CREATED",
                    "Warehouse order " + warehouseOrder.getOrderNumber() + " created (Scenario 3)");

            // NOTE: Production order is NOT auto-triggered.
            // Modules Supermarket will confirm the order and manually decide:
            // - Fulfill directly if modules are in stock
            // - Request production if modules are not available
        }

        order.setStatus(PROCESSING);
        String notes = "Scenario 3: Partial fulfillment from local + Modules Supermarket";
        if (!warehouseOrderItems.isEmpty()) {
            notes += " (warehouse order: " + warehouseOrder.getOrderNumber() + " + Production order auto-triggered)";
        }
        order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + notes);
        orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "STATUS_PROCESSING", "Scenario 3 processing (partial local fulfillment)");

        return mapToDTO(customerOrderRepository.save(order));
    }

    /**
     * Scenario 4: Production Planning (Future enhancement)
     * Complex items that require production routing.
     */
    private CustomerOrderDTO scenario4_ProductionPlanning(CustomerOrder order) {
        logger.info("Scenario 4: Production Planning for order {}", order.getOrderNumber());

        // Fulfill available items
        for (OrderItem item : order.getOrderItems()) {
            if (inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity())) {
                inventoryService.updateStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
            }
        }

        order.setStatus(PROCESSING);
        order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + "Scenario 4: Routed to Production Planning for custom items");

        return mapToDTO(customerOrderRepository.save(order));
    }

    /**
     * Update triggerScenario for all other CONFIRMED orders at the same workstation.
     * Call this after a successful fulfillment that changed stock levels.
     * This ensures other orders show the correct buttons based on current stock.
     */
    private void updateOtherConfirmedOrdersScenario(Long workstationId, Long excludeOrderId) {
        logger.info("Updating triggerScenario for other CONFIRMED orders at workstation {}", workstationId);
        
        // Find all CONFIRMED orders at this workstation (excluding the just-fulfilled order)
        List<CustomerOrder> confirmedOrders = customerOrderRepository.findAll().stream()
            .filter(o -> "CONFIRMED".equals(o.getStatus()))
            .filter(o -> workstationId.equals(o.getWorkstationId()))
            .filter(o -> !excludeOrderId.equals(o.getId()))
            .toList();
        
        logger.info("Found {} other CONFIRMED orders to update", confirmedOrders.size());
        
        for (CustomerOrder otherOrder : confirmedOrders) {
            // Re-check stock for this order
            boolean allItemsAvailable = otherOrder.getOrderItems().stream()
                .allMatch(item -> inventoryService.checkStock(
                    otherOrder.getWorkstationId(), 
                    item.getItemId(), 
                    item.getQuantity()
                ));
            
            String newScenario;
            if (allItemsAvailable) {
                newScenario = "DIRECT_FULFILLMENT";
            } else {
                // Check if any items available for partial fulfillment
                boolean anyItemsAvailable = otherOrder.getOrderItems().stream()
                    .anyMatch(item -> inventoryService.checkStock(
                        otherOrder.getWorkstationId(), 
                        item.getItemId(), 
                        item.getQuantity()
                    ));
                newScenario = anyItemsAvailable ? "PARTIAL_FULFILLMENT" : "WAREHOUSE_ORDER_NEEDED";
            }
            
            // Only update if scenario changed
            if (!newScenario.equals(otherOrder.getTriggerScenario())) {
                logger.info("Order {} triggerScenario updated: {} → {}", 
                    otherOrder.getOrderNumber(), 
                    otherOrder.getTriggerScenario(), 
                    newScenario);
                otherOrder.setTriggerScenario(newScenario);
                customerOrderRepository.save(otherOrder);
            }
        }
    }

    private CustomerOrderDTO mapToDTO(CustomerOrder order) {
        CustomerOrderDTO dto = new CustomerOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setWorkstationId(order.getWorkstationId());
        dto.setNotes(order.getNotes());
        // Map orderItems if needed
        return dto;
    }
}
