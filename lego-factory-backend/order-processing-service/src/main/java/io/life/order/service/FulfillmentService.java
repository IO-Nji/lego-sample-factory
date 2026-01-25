package io.life.order.service;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
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
     * Scenario 2: Warehouse Order
     * No items available locally. Create a warehouse order for all items.
     * Also automatically create a production order for the shortfall.
     * Order status changes to PROCESSING.
     */
    private CustomerOrderDTO scenario2_WarehouseOrder(CustomerOrder order) {
        logger.info("Scenario 2: Warehouse Order for order {}", order.getOrderNumber());

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
        List<WarehouseOrderItem> warehouseOrderItems = new ArrayList<>();
        java.util.Map<Long, Integer> moduleRequirements = new java.util.HashMap<>();
        
        for (OrderItem item : order.getOrderItems()) {
            if ("PRODUCT".equalsIgnoreCase(item.getItemType())) {
                // Use BOM to get module requirements for this product
                logger.info("BOM Lookup: Converting product {} (qty {}) to modules", item.getItemId(), item.getQuantity());
                java.util.Map<Long, Integer> productModules = masterdataService.getModuleRequirementsForProduct(
                    item.getItemId(), 
                    item.getQuantity()
                );
                
                // Aggregate module requirements
                for (java.util.Map.Entry<Long, Integer> entry : productModules.entrySet()) {
                    Long moduleId = entry.getKey();
                    Integer requiredQty = entry.getValue();
                    moduleRequirements.merge(moduleId, requiredQty, Integer::sum);
                    logger.info("  - Product {} requires Module {} qty {}", item.getItemId(), moduleId, requiredQty);
                }
            } else {
                // For non-products, add directly (fallback for legacy data)
                moduleRequirements.merge(item.getItemId(), item.getQuantity(), Integer::sum);
            }
        }
        
        // Create warehouse order items from aggregated module requirements
        for (java.util.Map.Entry<Long, Integer> entry : moduleRequirements.entrySet()) {
            Long moduleId = entry.getKey();
            Integer totalQty = entry.getValue();
            
            WarehouseOrderItem woItem = new WarehouseOrderItem();
            woItem.setWarehouseOrder(warehouseOrder);
            woItem.setItemId(moduleId);
            
            // Fetch module name from masterdata-service
            String moduleName = masterdataService.getItemName("MODULE", moduleId);
            woItem.setItemName(moduleName);
            woItem.setRequestedQuantity(totalQty);
            woItem.setFulfilledQuantity(0);
            woItem.setItemType("MODULE"); // Warehouse orders always deal with MODULEs
            warehouseOrderItems.add(woItem);
            
            logger.info("  - Warehouse order item: Module {} ({}) qty {}", moduleId, moduleName, totalQty);
        }
        
        warehouseOrder.setOrderItems(warehouseOrderItems);

        // Persist warehouse order
        warehouseOrderRepository.save(warehouseOrder);
        logger.info("Created warehouse order {} for customer order {}", warehouseOrder.getOrderNumber(), order.getOrderNumber());
        orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "WAREHOUSE_ORDER_CREATED",
            "Warehouse order " + warehouseOrder.getOrderNumber() + " created (Scenario 2)");

        // NOTE: Production order is NOT auto-triggered.
        // Modules Supermarket will confirm the order and manually decide:
        // - Fulfill directly if modules are in stock
        // - Request production if modules are not available

        // Update customer order status
        order.setStatus(PROCESSING);
        order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + "Scenario 2: Warehouse order " + warehouseOrder.getOrderNumber() + " created (awaiting Modules Supermarket confirmation)");
        orderAuditService.recordOrderEvent(CUSTOMER, order.getId(), "STATUS_PROCESSING", "Scenario 2 processing (waiting on warehouse)");

        return mapToDTO(customerOrderRepository.save(order));
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
        java.util.Map<Long, Integer> moduleRequirements = new java.util.HashMap<>();
        
        // Fulfill available items and collect BOM requirements for unavailable ones
        for (OrderItem item : order.getOrderItems()) {
            if ("PRODUCT".equalsIgnoreCase(item.getItemType())) {
                // Check if product is available at Plant Warehouse
                if (inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity())) {
                    // Fulfill from local stock
                    inventoryService.updateStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
                    item.setFulfilledQuantity((item.getFulfilledQuantity() == null ? 0 : item.getFulfilledQuantity()) + item.getQuantity());
                    logger.info("  - Product {} fulfilled from local stock", item.getItemId());
                } else {
                    // Product not available - use BOM to convert to module requirements
                    logger.info("BOM Lookup: Converting unavailable product {} (qty {}) to modules", item.getItemId(), item.getQuantity());
                    java.util.Map<Long, Integer> productModules = masterdataService.getModuleRequirementsForProduct(
                        item.getItemId(), 
                        item.getQuantity()
                    );
                    
                    // Aggregate module requirements
                    for (java.util.Map.Entry<Long, Integer> entry : productModules.entrySet()) {
                        Long moduleId = entry.getKey();
                        Integer requiredQty = entry.getValue();
                        moduleRequirements.merge(moduleId, requiredQty, Integer::sum);
                        logger.info("  - Product {} requires Module {} qty {}", item.getItemId(), moduleId, requiredQty);
                    }
                }
            } else {
                // For non-products, check availability and add to warehouse order if missing
                if (inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity())) {
                    inventoryService.updateStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
                    item.setFulfilledQuantity((item.getFulfilledQuantity() == null ? 0 : item.getFulfilledQuantity()) + item.getQuantity());
                    logger.info("  - Item {} fulfilled from local stock", item.getItemId());
                } else {
                    moduleRequirements.merge(item.getItemId(), item.getQuantity(), Integer::sum);
                }
            }
        }
        
        // Create warehouse order items from aggregated module requirements
        List<WarehouseOrderItem> warehouseOrderItems = new ArrayList<>();
        for (java.util.Map.Entry<Long, Integer> entry : moduleRequirements.entrySet()) {
            Long moduleId = entry.getKey();
            Integer totalQty = entry.getValue();
            
            WarehouseOrderItem woItem = new WarehouseOrderItem();
            woItem.setWarehouseOrder(warehouseOrder);
            woItem.setItemId(moduleId);
            
            // Fetch module name from masterdata-service
            String moduleName = masterdataService.getItemName("MODULE", moduleId);
            woItem.setItemName(moduleName);
            woItem.setRequestedQuantity(totalQty);
            woItem.setFulfilledQuantity(0);
            woItem.setItemType("MODULE"); // Warehouse orders always deal with MODULEs
            warehouseOrderItems.add(woItem);
            
            logger.info("  - Module {} ({}) requested from Modules Supermarket qty {}", moduleId, moduleName, totalQty);
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
