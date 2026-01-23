package io.life.order.service;

import io.life.order.dto.WarehouseOrderDTO;
import io.life.order.dto.WarehouseOrderItemDTO;
import io.life.order.entity.CustomerOrder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WarehouseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseOrderService.class);
    private static final Long PLANT_WAREHOUSE_WORKSTATION_ID = 7L;
    private static final Long MODULES_SUPERMARKET_WORKSTATION_ID = 8L;
    private static final Long FINAL_ASSEMBLY_WORKSTATION_ID = 6L;
    private static final String WAREHOUSE_AUDIT_SOURCE = "WAREHOUSE";

    private final WarehouseOrderRepository warehouseOrderRepository;
    private final InventoryService inventoryService;
    private final ProductionOrderService productionOrderService;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderAuditService orderAuditService;
    private final AssemblyControlOrderService assemblyControlOrderService;
    private final FinalAssemblyOrderService finalAssemblyOrderService;
    private final MasterdataService masterdataService;

    public WarehouseOrderService(WarehouseOrderRepository warehouseOrderRepository,
                                 InventoryService inventoryService,
                                 ProductionOrderService productionOrderService,
                                 CustomerOrderRepository customerOrderRepository,
                                 OrderAuditService orderAuditService,
                                 AssemblyControlOrderService assemblyControlOrderService,
                                 FinalAssemblyOrderService finalAssemblyOrderService,
                                 MasterdataService masterdataService) {
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.inventoryService = inventoryService;
        this.productionOrderService = productionOrderService;
        this.customerOrderRepository = customerOrderRepository;
        this.orderAuditService = orderAuditService;
        this.assemblyControlOrderService = assemblyControlOrderService;
        this.finalAssemblyOrderService = finalAssemblyOrderService;
        this.masterdataService = masterdataService;
    }

    /**
     * Get all warehouse orders
     */
    public List<WarehouseOrderDTO> getAllWarehouseOrders() {
        List<WarehouseOrder> orders = warehouseOrderRepository.findAll();
        logger.info("Fetching all warehouse orders - found {} orders", orders.size());
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get warehouse order by ID
     */
    @SuppressWarnings("null")
    public Optional<WarehouseOrderDTO> getWarehouseOrderById(Long id) {
        return warehouseOrderRepository.findById(id)
                .map(this::mapToDTO);
    }

    /**
     * Get warehouse orders by fulfilling workstation ID
     * Used to retrieve orders that need to be fulfilled (e.g., Modules Supermarket)
     */
    public List<WarehouseOrderDTO> getWarehouseOrdersByFulfillingWorkstationId(Long fulfillingWorkstationId) {
        List<WarehouseOrder> orders = warehouseOrderRepository.findByFulfillingWorkstationId(fulfillingWorkstationId);
        logger.info("Fetching warehouse orders for workstation {} - found {} orders", fulfillingWorkstationId, orders.size());
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get warehouse orders by requesting workstation ID
     * Used to retrieve orders that were created by a workstation (e.g., Plant Warehouse)
     */
    public List<WarehouseOrderDTO> getWarehouseOrdersByRequestingWorkstationId(Long requestingWorkstationId) {
        return warehouseOrderRepository.findByRequestingWorkstationId(requestingWorkstationId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get warehouse orders by status
     */
    public List<WarehouseOrderDTO> getWarehouseOrdersByStatus(String status) {
        return warehouseOrderRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Confirm a warehouse order
     * Changes status from PENDING to PROCESSING (acknowledges receipt and readiness to fulfill)
     * Similar to customer order confirmation at Plant Warehouse
     * 
     * ENHANCED: Analyzes module availability and sets triggerScenario for frontend
     */
    public WarehouseOrderDTO confirmWarehouseOrder(Long warehouseOrderId) {
        @SuppressWarnings("null")
        Optional<WarehouseOrder> orderOpt = warehouseOrderRepository.findById(warehouseOrderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Warehouse order not found: " + warehouseOrderId);
        }

        WarehouseOrder order = orderOpt.get();
        
        // Validate current status
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Only PENDING warehouse orders can be confirmed. Current status: " + order.getStatus());
        }

        // Update status to PROCESSING
        order.setStatus("PROCESSING");
        order.setUpdatedAt(LocalDateTime.now());
        
        // ANALYZE MODULE AVAILABILITY to set triggerScenario for frontend button logic
        try {
            logger.info("Analyzing module availability for warehouse order {}", order.getWarehouseOrderNumber());
            
            // Get module requirements for all products in this warehouse order
            Map<Long, Integer> aggregatedModuleRequirements = new HashMap<>();
            
            for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
                if ("PRODUCT".equals(item.getItemType())) {
                    logger.info("  Product item: {} (ID: {}) qty {}", item.getItemName(), item.getItemId(), item.getRequestedQuantity());
                    
                    Map<Long, Integer> moduleReqs = masterdataService.getModuleRequirementsForProduct(
                        item.getItemId(), 
                        item.getRequestedQuantity()
                    );
                    
                    logger.info("    Requires {} different modules", moduleReqs.size());
                    for (Map.Entry<Long, Integer> entry : moduleReqs.entrySet()) {
                        aggregatedModuleRequirements.merge(entry.getKey(), entry.getValue(), Integer::sum);
                        logger.info("      Module ID {} qty {}", entry.getKey(), entry.getValue());
                    }
                }
            }
            
            logger.info("Total aggregated module requirements: {} different modules", aggregatedModuleRequirements.size());
            
            // Check if all modules are available
            boolean allModulesAvailable = inventoryService.checkModulesAvailability(aggregatedModuleRequirements);
            
            if (allModulesAvailable) {
                order.setTriggerScenario("DIRECT_FULFILLMENT");
                logger.info("✓ Warehouse order {} - All modules available → Trigger Scenario: DIRECT_FULFILLMENT", order.getWarehouseOrderNumber());
            } else {
                order.setTriggerScenario("PRODUCTION_REQUIRED");
                logger.info("✗ Warehouse order {} - Insufficient modules → Trigger Scenario: PRODUCTION_REQUIRED", order.getWarehouseOrderNumber());
            }
        } catch (Exception e) {
            logger.error("Failed to analyze module availability for warehouse order {}: {}", 
                order.getWarehouseOrderNumber(), e.getMessage(), e);
            order.setTriggerScenario("UNKNOWN");
        }
        
        warehouseOrderRepository.save(order);

        logger.info("Warehouse order {} confirmed and moved to PROCESSING", order.getWarehouseOrderNumber());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "CONFIRMED",
                "Warehouse order confirmed by Modules Supermarket - Status: PROCESSING");

        return mapToDTO(order);
    }

    /**
     * Fulfill a warehouse order (Modules Supermarket workflow)
     * 
     * ENHANCED LOGIC:
     * 1. Check Modules Supermarket stock FIRST
     * 2. Fulfill what's available (partial or full)
     * 3. If shortage exists, AUTO-TRIGGER production order for missing items
     * 4. Update source customer order status accordingly
     */
    public WarehouseOrderDTO fulfillWarehouseOrder(Long warehouseOrderId) {
        @SuppressWarnings("null")
        Optional<WarehouseOrder> orderOpt = warehouseOrderRepository.findById(warehouseOrderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Warehouse order not found: " + warehouseOrderId);
        }

        WarehouseOrder order = orderOpt.get();
        
        // Validate order can be fulfilled (must be PROCESSING)
        if (!"PROCESSING".equals(order.getStatus())) {
            throw new IllegalStateException("Only PROCESSING warehouse orders can be fulfilled. Current status: " + order.getStatus() + ". Please confirm the order first.");
        }

        logger.info("Processing warehouse order {} from Modules Supermarket (WS-8)", order.getWarehouseOrderNumber());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLMENT_STARTED",
            "Warehouse order fulfillment started: " + order.getWarehouseOrderNumber());

        // STEP 1: Resolve module requirements for all products in this warehouse order
        // Warehouse order items have itemType="PRODUCT" - need to resolve to modules
        Map<Long, Integer> aggregatedModuleRequirements = new HashMap<>();
        
        for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
            if ("PRODUCT".equals(item.getItemType())) {
                // Get module requirements for this product
                Map<Long, Integer> moduleReqs = masterdataService.getModuleRequirementsForProduct(
                    item.getItemId(), 
                    item.getRequestedQuantity()
                );
                
                // Aggregate module requirements
                for (Map.Entry<Long, Integer> entry : moduleReqs.entrySet()) {
                    aggregatedModuleRequirements.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
                
                logger.info("  Product {} (qty {}) requires {} different modules", 
                    item.getItemName(), item.getRequestedQuantity(), moduleReqs.size());
            } else {
                logger.warn("Warehouse order item is not PRODUCT type: {} - cannot resolve modules", item.getItemType());
            }
        }
        
        logger.info("Total module requirements: {} different modules", aggregatedModuleRequirements.size());
        
        // STEP 2: Check if Modules Supermarket has all required modules
        boolean allModulesAvailable = inventoryService.checkModulesAvailability(aggregatedModuleRequirements);

        if (allModulesAvailable) {
            // SCENARIO A: All modules available - Debit modules, create Final Assembly orders
            logger.info("Scenario A: All required modules available in Modules Supermarket - Direct fulfillment");
            return fulfillAllItemsWithModules(order, aggregatedModuleRequirements);
        } else {
            // Check if ANY modules are available
            int availableCount = 0;
            for (Map.Entry<Long, Integer> moduleReq : aggregatedModuleRequirements.entrySet()) {
                if (inventoryService.checkStock(MODULES_SUPERMARKET_WORKSTATION_ID, moduleReq.getKey(), moduleReq.getValue())) {
                    availableCount++;
                }
            }
            
            if (availableCount > 0 && availableCount < aggregatedModuleRequirements.size()) {
                // SCENARIO B: Some modules available - Partial fulfillment + Auto-create production order
                logger.info("Scenario B: {} of {} modules available in Modules Supermarket", availableCount, aggregatedModuleRequirements.size());
                return fulfillPartialAndTriggerProduction(order);
            } else {
                // SCENARIO C: No modules available - Auto-create production order for all
                logger.info("Scenario C: No modules available in Modules Supermarket - Creating production order");
                return fulfillNoneAndTriggerProduction(order);
            }
        }
    }

    /**
     * Scenario A: All modules available - Debit required modules, create Final Assembly orders
     * This is the NEW CORRECT implementation that debits MODULES not PRODUCTS
     */
    private WarehouseOrderDTO fulfillAllItemsWithModules(WarehouseOrder order, Map<Long, Integer> moduleRequirements) {
        logger.info("Fulfilling warehouse order {} by debiting {} modules", order.getWarehouseOrderNumber(), moduleRequirements.size());

        boolean allModulesDebited = true;
        
        // Debit each required module from Modules Supermarket (WS-8)
        for (Map.Entry<Long, Integer> moduleReq : moduleRequirements.entrySet()) {
            Long moduleId = moduleReq.getKey();
            Integer quantity = moduleReq.getValue();
            
            try {
                // Debit module inventory at Modules Supermarket (WS-8)
                // updateStock will automatically use "MODULE" itemType for workstation 8
                boolean debited = inventoryService.updateStock(
                    MODULES_SUPERMARKET_WORKSTATION_ID,  // workstation 8
                    moduleId,                             // module ID
                    quantity                              // quantity (will be converted to negative delta)
                );
                
                if (debited) {
                    logger.info("  ✓ Debited module {} qty {} from Modules Supermarket", moduleId, quantity);
                } else {
                    allModulesDebited = false;
                    logger.error("  ✗ Failed to debit module {} qty {}", moduleId, quantity);
                }
            } catch (Exception e) {
                allModulesDebited = false;
                logger.error("  ✗ Error debiting module {}: {}", moduleId, e.getMessage());
            }
        }
        
        if (allModulesDebited) {
            // Mark warehouse order items as fulfilled
            for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
                item.setFulfilledQuantity(item.getRequestedQuantity());
            }
            
            // Mark warehouse order as FULFILLED
            order.setStatus("FULFILLED");
            logger.info("Warehouse order {} fulfilled - modules debited, creating Final Assembly orders", order.getWarehouseOrderNumber());
            
            // CREATE FINAL ASSEMBLY ORDERS (Proper Scenario 2 workflow)
            // Modules debited from Modules Supermarket, now Final Assembly needs to assemble products
            // Final Assembly completion will credit Plant Warehouse
            createFinalAssemblyOrdersFromWarehouseOrder(order);
            
            orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLED",
                    "Modules fulfilled, Final Assembly orders created - awaiting assembly completion");
        } else {
            order.setStatus("PARTIALLY_FULFILLED");
            logger.warn("Warehouse order {} partially fulfilled due to inventory errors", order.getWarehouseOrderNumber());
            orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "PARTIAL_FULFILLMENT",
                    "Some modules could not be debited - check inventory logs");
        }

        return mapToDTO(warehouseOrderRepository.save(order));
    }

    /**
     * Scenario A (DEPRECATED): All items available - Deduct from Modules Supermarket, complete order
     * PROPER Scenario 2: Create AssemblyControlOrders for Final Assembly station
     * Final Assembly will credit Plant Warehouse when they complete their work
     */
    private WarehouseOrderDTO fulfillAllItems(WarehouseOrder order) {
        logger.info("Fulfilling all items for warehouse order {}", order.getWarehouseOrderNumber());

        boolean allItemsFulfilled = true;
        for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
            try {
                // Deduct from Modules Supermarket stock
                boolean deducted = inventoryService.updateStock(
                        order.getFulfillingWorkstationId(),
                        item.getItemId(),
                        item.getRequestedQuantity()
                );

                if (deducted) {
                    item.setFulfilledQuantity(item.getRequestedQuantity());
                    logger.info("  ✓ Item {} qty {} fulfilled", item.getItemId(), item.getRequestedQuantity());
                } else {
                    allItemsFulfilled = false;
                    logger.warn("  ✗ Failed to deduct item {} from inventory", item.getItemId());
                }
            } catch (Exception e) {
                allItemsFulfilled = false;
                logger.error("  ✗ Error fulfilling item {}: {}", item.getItemId(), e.getMessage());
            }
        }

        if (allItemsFulfilled) {
            // Mark as FULFILLED
            order.setStatus("FULFILLED");
            logger.info("Warehouse order {} fulfilled - creating Final Assembly orders", order.getWarehouseOrderNumber());
            
            // CREATE FINAL ASSEMBLY ORDERS (Proper Scenario 2 workflow)
            // Modules Supermarket debited, now Final Assembly needs to assemble products
            // Final Assembly completion will credit Plant Warehouse
            createFinalAssemblyOrdersFromWarehouseOrder(order);
            
            orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLED",
                    "Modules fulfilled, Final Assembly orders created - awaiting assembly completion");
        } else {
            order.setStatus("PARTIALLY_FULFILLED");
            logger.warn("Warehouse order {} partially fulfilled due to inventory errors", order.getWarehouseOrderNumber());
            orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "PARTIALLY_FULFILLED",
                    "Partial fulfillment due to inventory errors");
        }

        order.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(warehouseOrderRepository.save(order));
    }

    /**
     * Scenario B: Partial items available - Fulfill available items + Auto-trigger production for missing
     */
    private WarehouseOrderDTO fulfillPartialAndTriggerProduction(WarehouseOrder order) {
        logger.info("Fulfilling partial items for warehouse order {}", order.getWarehouseOrderNumber());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "PARTIAL_FULFILLMENT",
            "Partial items fulfilled for warehouse order " + order.getWarehouseOrderNumber());

        List<WarehouseOrderItem> itemsToProduceLater = new ArrayList<>();

        // Fulfill available items only
        for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
            if (inventoryService.checkStock(order.getFulfillingWorkstationId(), item.getItemId(), item.getRequestedQuantity())) {
                try {
                    // Deduct from Modules Supermarket stock
                    boolean deducted = inventoryService.updateStock(
                            order.getFulfillingWorkstationId(),
                            item.getItemId(),
                            item.getRequestedQuantity()
                    );

                    if (deducted) {
                        item.setFulfilledQuantity(item.getRequestedQuantity());
                        logger.info("  ✓ Item {} qty {} fulfilled from Modules Supermarket", item.getItemId(), item.getRequestedQuantity());
                    }
                } catch (Exception e) {
                    logger.error("  ✗ Error fulfilling available item {}: {}", item.getItemId(), e.getMessage());
                }
            } else {
                // Mark as to be produced later
                itemsToProduceLater.add(item);
                logger.info("  ⚠ Item {} NOT available - will trigger production order", item.getItemId());
            }
        }

        order.setStatus("PROCESSING");
        order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + 
                "Partial fulfillment: " + itemsToProduceLater.size() + " item(s) short");

        // AUTO-TRIGGER: Create production order for missing items
        if (!itemsToProduceLater.isEmpty()) {
            logger.info("AUTO-TRIGGERING production order for {} missing item(s)", itemsToProduceLater.size());
            triggerProductionOrderForShortfall(order, itemsToProduceLater);
        }

        order.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(warehouseOrderRepository.save(order));
    }

    /**
     * Scenario C: No items available - Auto-trigger production order for complete order
     */
    private WarehouseOrderDTO fulfillNoneAndTriggerProduction(WarehouseOrder order) {
        logger.info("No items available in Modules Supermarket - AUTO-TRIGGERING production order for entire warehouse order");
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "NO_STOCK_PRODUCTION_TRIGGER",
            "No stock available; production order auto-triggered");

        order.setStatus("PENDING_PRODUCTION");
        order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + 
                "No stock available - production order auto-triggered");

        // AUTO-TRIGGER: Create production order for ALL items
        triggerProductionOrderForShortfall(order, order.getWarehouseOrderItems());

        order.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(warehouseOrderRepository.save(order));
    }

    /**
     * AUTO-TRIGGER: Create production order for items not available at Modules Supermarket
     */
    private void triggerProductionOrderForShortfall(WarehouseOrder order, List<WarehouseOrderItem> shortfallItems) {
        try {
            String priority = determinePriority(order);
            
            // Create production order linked to this warehouse order
            productionOrderService.createProductionOrderFromWarehouse(
                    order.getSourceCustomerOrderId(),
                    order.getId(),
                    priority,
                    order.getOrderDate().plusDays(7), // Due 7 days from order date
                    "Auto-created from warehouse order " + order.getWarehouseOrderNumber() + 
                    " - Modules Supermarket shortfall (" + shortfallItems.size() + " item(s))",
                    order.getRequestingWorkstationId(),
                    order.getFulfillingWorkstationId()  // Assign back to Modules Supermarket for completion
            );
            
                logger.info("✓ Production order AUTO-CREATED for warehouse order {} with {} shortfall item(s)", 
                    order.getWarehouseOrderNumber(), shortfallItems.size());
                orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "PRODUCTION_ORDER_CREATED",
                    "Production order auto-created for shortfall items");
            
            // Update notes with auto-trigger confirmation
            order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + 
                    "Production order auto-triggered for Modules Supermarket shortfall");
            
        } catch (Exception e) {
            logger.error("✗ Failed to auto-create production order for warehouse order {}: {}", 
                    order.getWarehouseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Create Final Assembly Orders for Final Assembly workstation (WS-6).
     * PROPER Scenario 2 workflow - Assembly station will credit Plant Warehouse when complete.
     * 
     * NOTE: These are Final Assembly Orders, NOT Assembly Control Orders.
     * Assembly Control Orders are only created by Production Planning for production control operations.
     * 
     * Each fulfilled item in the warehouse order becomes a separate final assembly order.
     * The Final Assembly workstation will complete these orders, which will:
     * 1. Update SimAL schedule status
     * 2. Credit Plant Warehouse with finished products  
     * 3. Allow customer order to be fulfilled at Plant Warehouse
     */
    private void createFinalAssemblyOrdersFromWarehouseOrder(WarehouseOrder order) {
        try {
            logger.info("Creating Final Assembly orders for warehouse order {}", order.getWarehouseOrderNumber());
            
            for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
                if (item.getFulfilledQuantity() > 0) {
                    try {
                        Long productId = item.getItemId();
                        Integer quantity = item.getFulfilledQuantity();
                        String productName = item.getItemName();
                        
                        // Get required modules from masterdata service
                        Map<Long, Integer> moduleRequirements = masterdataService.getModuleRequirementsForProduct(
                                productId, quantity);
                        
                        // Build module IDs JSON (e.g., "[5, 8, 12]")
                        String moduleIds = "[" + String.join(", ", 
                                moduleRequirements.keySet().stream()
                                        .map(String::valueOf)
                                        .toArray(String[]::new)) + "]";
                        
                        // Build module details JSON
                        StringBuilder moduleDetails = new StringBuilder("{");
                        moduleRequirements.forEach((moduleId, qty) -> {
                            if (moduleDetails.length() > 1) moduleDetails.append(", ");
                            moduleDetails.append("\"").append(moduleId).append("\": ").append(qty);
                        });
                        moduleDetails.append("}");
                        
                        // Create FinalAssemblyOrder using the service
                        finalAssemblyOrderService.createFromWarehouseOrder(
                                order.getId(),
                                productId,
                                productName,
                                quantity,
                                moduleIds,
                                moduleDetails.toString()
                        );
                        
                        logger.info("✓ Final Assembly order created for Product #{} ({}) qty {}", 
                                productId, productName, quantity);
                        
                        orderAuditService.recordOrderEvent("FINAL_ASSEMBLY", order.getId(), "ASSEMBLY_ORDER_CREATED",
                                String.format("Final Assembly order created for Product #%d (%s) qty %d from warehouse order %s",
                                        productId, productName, quantity, order.getWarehouseOrderNumber()));
                    } catch (Exception e) {
                        logger.error("✗ Failed to create assembly order for item {}: {}", item.getItemId(), e.getMessage());
                        // Continue with other items even if one fails
                    }
                }
            }
            
            logger.info("✓ All Final Assembly orders created for warehouse order {}", order.getWarehouseOrderNumber());
            
        } catch (Exception e) {
            logger.error("✗ Failed to create Final Assembly orders: {}", e.getMessage());
        }
    }

    /**
     * Determine priority based on warehouse order urgency
     */
    private String determinePriority(WarehouseOrder order) {
        LocalDateTime now = LocalDateTime.now();
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(now, order.getOrderDate());
        
        if (daysDifference < 1) {
            return "HIGH";
        } else if (daysDifference < 3) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Update warehouse order status
     */
    public WarehouseOrderDTO updateWarehouseOrderStatus(Long warehouseOrderId, String status) {
        @SuppressWarnings("null")
        Optional<WarehouseOrder> orderOpt = warehouseOrderRepository.findById(warehouseOrderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Warehouse order not found: " + warehouseOrderId);
        }

        WarehouseOrder order = orderOpt.get();
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        logger.info("Updated warehouse order {} status to {}", order.getWarehouseOrderNumber(), status);

        return mapToDTO(warehouseOrderRepository.save(order));
    }

    /**
     * Map WarehouseOrder entity to DTO
     */
    private WarehouseOrderDTO mapToDTO(WarehouseOrder order) {
        WarehouseOrderDTO dto = new WarehouseOrderDTO();
        dto.setId(order.getId());
        dto.setWarehouseOrderNumber(order.getWarehouseOrderNumber());
        dto.setSourceCustomerOrderId(order.getSourceCustomerOrderId());
        dto.setRequestingWorkstationId(order.getRequestingWorkstationId());
        dto.setFulfillingWorkstationId(order.getFulfillingWorkstationId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setTriggerScenario(order.getTriggerScenario());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Map warehouse order items
        if (order.getWarehouseOrderItems() != null) {
            logger.info("Mapping warehouse order {} - Found {} items", 
                order.getWarehouseOrderNumber(), order.getWarehouseOrderItems().size());
            dto.setWarehouseOrderItems(order.getWarehouseOrderItems().stream()
                    .map(this::mapItemToDTO)
                    .collect(Collectors.toList()));
        } else {
            logger.warn("Mapping warehouse order {} - Items are NULL", order.getWarehouseOrderNumber());
        }

        return dto;
    }

    /**
     * Map WarehouseOrderItem entity to DTO
     */
    private WarehouseOrderItemDTO mapItemToDTO(WarehouseOrderItem item) {
        WarehouseOrderItemDTO dto = new WarehouseOrderItemDTO();
        dto.setId(item.getId());
        dto.setItemId(item.getItemId());
        dto.setItemName(item.getItemName());
        dto.setRequestedQuantity(item.getRequestedQuantity());
        dto.setFulfilledQuantity(item.getFulfilledQuantity());
        dto.setItemType(item.getItemType());
        dto.setNotes(item.getNotes());
        return dto;
    }
}
