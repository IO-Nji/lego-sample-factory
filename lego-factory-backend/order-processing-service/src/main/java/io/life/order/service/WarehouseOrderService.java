package io.life.order.service;

import io.life.order.dto.WarehouseOrderDTO;
import io.life.order.dto.WarehouseOrderItemDTO;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.repository.WarehouseOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WarehouseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseOrderService.class);
    private static final String WAREHOUSE_AUDIT_SOURCE = "WAREHOUSE";

    private final WarehouseOrderRepository warehouseOrderRepository;
    private final InventoryService inventoryService;
    private final ProductionOrderService productionOrderService;
    private final OrderAuditService orderAuditService;
    private final FinalAssemblyOrderService finalAssemblyOrderService;

    public WarehouseOrderService(WarehouseOrderRepository warehouseOrderRepository,
                                 InventoryService inventoryService,
                                 ProductionOrderService productionOrderService,
                                 OrderAuditService orderAuditService,
                                 FinalAssemblyOrderService finalAssemblyOrderService) {
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.inventoryService = inventoryService;
        this.productionOrderService = productionOrderService;
        this.orderAuditService = orderAuditService;
        this.finalAssemblyOrderService = finalAssemblyOrderService;
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
        List<WarehouseOrder> orders = warehouseOrderRepository.findByWorkstationId(fulfillingWorkstationId);
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
        return warehouseOrderRepository.findByWorkstationId(requestingWorkstationId).stream()
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
     * STOCK CHECK REFINEMENT: Check MODULE stock at Modules Supermarket (WS-8) DURING confirmation
     * Sets triggerScenario: DIRECT_FULFILLMENT or PRODUCTION_REQUIRED
     * Changes status from PENDING to CONFIRMED
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

        // STOCK CHECK REFINEMENT: Check MODULE stock at Modules Supermarket (WS-8) DURING confirmation
        // This determines the scenario path: direct fulfillment or production required
        boolean hasAllModules = order.getOrderItems().stream()
            .allMatch(item -> {
                // Modules Supermarket (WS-8) checks MODULE stock
                return inventoryService.checkStock(
                    order.getWorkstationId(), // Modules Supermarket workstation ID (8)
                    item.getItemId(),
                    item.getRequestedQuantity()
                );
            });
        
        // Set triggerScenario based on MODULE stock check
        if (hasAllModules) {
            order.setTriggerScenario("DIRECT_FULFILLMENT");
            logger.info("Warehouse order {} confirmed - DIRECT_FULFILLMENT (sufficient MODULE stock)", order.getOrderNumber());
        } else {
            order.setTriggerScenario("PRODUCTION_REQUIRED");
            logger.info("Warehouse order {} confirmed - PRODUCTION_REQUIRED (insufficient MODULE stock)", order.getOrderNumber());
        }

        // Update status to CONFIRMED
        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        warehouseOrderRepository.save(order);

        logger.info("Warehouse order {} confirmed - Scenario: {}", order.getOrderNumber(), order.getTriggerScenario());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "CONFIRMED",
                "Warehouse order confirmed - Scenario: " + order.getTriggerScenario());

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
        
        // Validate order can be fulfilled (must be CONFIRMED)
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new IllegalStateException("Only CONFIRMED warehouse orders can be fulfilled. Current status: " + order.getStatus() + ". Please confirm the order first.");
        }

        logger.info("Processing warehouse order {} from Modules Supermarket (WS-8)", order.getOrderNumber());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLMENT_STARTED",
            "Warehouse order fulfillment started: " + order.getOrderNumber());

        // STEP 1: Check which items are available at Modules Supermarket (workstation 8)
        boolean allItemsAvailable = order.getOrderItems().stream()
                .allMatch(item -> inventoryService.checkStock(
                        order.getWorkstationId(),
                        item.getItemId(),
                        item.getRequestedQuantity()
                ));

        if (allItemsAvailable) {
            // SCENARIO A: All items available - Direct fulfillment
            logger.info("Scenario A: All items available in Modules Supermarket - Direct fulfillment");
            return fulfillAllItems(order);
        } else {
            // Check if ANY items are available
            boolean anyItemsAvailable = order.getOrderItems().stream()
                    .anyMatch(item -> inventoryService.checkStock(
                            order.getWorkstationId(),
                            item.getItemId(),
                            item.getRequestedQuantity()
                    ));

            if (anyItemsAvailable) {
                // SCENARIO B: Partial items available - Partial fulfillment + Auto-create production order
                logger.info("Scenario B: Partial items available in Modules Supermarket");
                return fulfillPartialAndTriggerProduction(order);
            } else {
                // SCENARIO C: No items available - Auto-create production order for all
                logger.info("Scenario C: No items available in Modules Supermarket - Creating production order for complete order");
                return fulfillNoneAndTriggerProduction(order);
            }
        }
    }

    /**
     * Scenario A: All items available - Deduct from Modules Supermarket, complete order
     * PROPER Scenario 2: Create AssemblyControlOrders for Final Assembly station
     * Final Assembly will credit Plant Warehouse when they complete their work
     */
    private WarehouseOrderDTO fulfillAllItems(WarehouseOrder order) {
        logger.info("Fulfilling all items for warehouse order {}", order.getOrderNumber());

        boolean allItemsFulfilled = true;
        for (WarehouseOrderItem item : order.getOrderItems()) {
            try {
                // Deduct from Modules Supermarket stock
                boolean deducted = inventoryService.updateStock(
                        order.getWorkstationId(),
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
            logger.info("Warehouse order {} fulfilled - creating Final Assembly orders", order.getOrderNumber());
            
            // CREATE FINAL ASSEMBLY ORDERS (Proper Scenario 2 workflow)
            // Modules Supermarket debited, now Final Assembly needs to assemble products
            // Final Assembly completion will credit Plant Warehouse
            createFinalAssemblyOrdersFromWarehouseOrder(order);
            
            orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLED",
                    "Modules fulfilled, Final Assembly orders created - awaiting assembly completion");
        } else {
            order.setStatus("PARTIALLY_FULFILLED");
            logger.warn("Warehouse order {} partially fulfilled due to inventory errors", order.getOrderNumber());
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
        logger.info("Fulfilling partial items for warehouse order {}", order.getOrderNumber());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "PARTIAL_FULFILLMENT",
            "Partial items fulfilled for warehouse order " + order.getOrderNumber());

        List<WarehouseOrderItem> itemsToProduceLater = new ArrayList<>();

        // Fulfill available items only
        for (WarehouseOrderItem item : order.getOrderItems()) {
            if (inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getRequestedQuantity())) {
                try {
                    // Deduct from Modules Supermarket stock
                    boolean deducted = inventoryService.updateStock(
                            order.getWorkstationId(),
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
        triggerProductionOrderForShortfall(order, order.getOrderItems());

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
                    order.getCustomerOrderId(),
                    order.getId(),
                    priority,
                    order.getOrderDate().plusDays(7), // Due 7 days from order date
                    "Auto-created from warehouse order " + order.getOrderNumber() + 
                    " - Modules Supermarket shortfall (" + shortfallItems.size() + " item(s))",
                    order.getWorkstationId(),
                    order.getWorkstationId()  // Assign back to Modules Supermarket for completion
            );
            
                logger.info("✓ Production order AUTO-CREATED for warehouse order {} with {} shortfall item(s)", 
                    order.getOrderNumber(), shortfallItems.size());
                orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "PRODUCTION_ORDER_CREATED",
                    "Production order auto-created for shortfall items");
            
            // Update notes with auto-trigger confirmation
            order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + 
                    "Production order auto-triggered for Modules Supermarket shortfall");
            
        } catch (Exception e) {
            logger.error("✗ Failed to auto-create production order for warehouse order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
        }
    }

    /**
     * Create AssemblyControlOrders for Final Assembly station.
     * PROPER Scenario 2 workflow - Assembly station will credit Plant Warehouse when complete.
     * 
     * Each fulfilled item in the warehouse order becomes a separate assembly order.
     * The Final Assembly workstation will complete these orders, which will:
     * 1. Update SimAL schedule status
     * 2. Credit Plant Warehouse with finished products  
     * 3. Allow customer order to be fulfilled at Plant Warehouse
     */
    /**
     * Create FinalAssemblyOrders from WarehouseOrder (Scenario 2)
     * Called after modules are debited from Modules Supermarket
     * Creates one FinalAssemblyOrder per warehouse order item
     */
    private void createFinalAssemblyOrdersFromWarehouseOrder(WarehouseOrder order) {
        try {
            logger.info("Creating Final Assembly orders for warehouse order {}", order.getOrderNumber());
            
            for (WarehouseOrderItem item : order.getOrderItems()) {
                if (item.getFulfilledQuantity() > 0) {
                    try {
                        Long productId = item.getItemId();
                        Integer quantity = item.getFulfilledQuantity();
                        
                        // Create Final Assembly order using new service (Scenario 2)
                        finalAssemblyOrderService.createFromWarehouseOrder(order, productId, quantity);
                        
                        logger.info("✓ Final Assembly order created for Product #{} ({}) qty {}", 
                                productId, item.getItemName(), quantity);
                        
                    } catch (Exception e) {
                        logger.error("✗ Failed to create Final Assembly order for item {}: {}", item.getItemId(), e.getMessage());
                        // Continue with other items even if one fails
                    }
                }
            }
            
            logger.info("✓ All Final Assembly orders created for warehouse order {}", order.getOrderNumber());
            
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
        logger.info("Updated warehouse order {} status to {}", order.getOrderNumber(), status);

        return mapToDTO(warehouseOrderRepository.save(order));
    }

    /**
     * Map WarehouseOrder entity to DTO
     */
    private WarehouseOrderDTO mapToDTO(WarehouseOrder order) {
        WarehouseOrderDTO dto = new WarehouseOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCustomerOrderId(order.getCustomerOrderId());
        dto.setWorkstationId(order.getWorkstationId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setTriggerScenario(order.getTriggerScenario());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Map warehouse order items
        if (order.getOrderItems() != null) {
            logger.info("Mapping warehouse order {} - Found {} items", 
                order.getOrderNumber(), order.getOrderItems().size());
            dto.setOrderItems(order.getOrderItems().stream()
                    .map(this::mapItemToDTO)
                    .collect(Collectors.toList()));
        } else {
            logger.warn("Mapping warehouse order {} - Items are NULL", order.getOrderNumber());
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
