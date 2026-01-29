package io.life.order.service;

import io.life.order.dto.ProductionControlOrderDTO;
import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.entity.ProductionOrder;
import io.life.order.entity.ProductionOrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.repository.ProductionOrderRepository;
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

/**
 * Service for managing ProductionOrder entities.
 * Handles creation, retrieval, and status updates of production orders.
 * Production orders are created when WarehouseOrders cannot be fulfilled.
 */
@Service
@Transactional
public class ProductionOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionOrderService.class);
    private static final String PRODUCTION_ORDER_NOT_FOUND = "Production order not found: ";

    private static final Long MODULES_SUPERMARKET_WORKSTATION_ID = 8L;

    private final ProductionOrderRepository productionOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final ProductionControlOrderService productionControlOrderService;
    private final AssemblyControlOrderService assemblyControlOrderService;
    private final InventoryService inventoryService;

    public ProductionOrderService(ProductionOrderRepository productionOrderRepository,
                                 WarehouseOrderRepository warehouseOrderRepository,
                                 ProductionControlOrderService productionControlOrderService,
                                 AssemblyControlOrderService assemblyControlOrderService,
                                 InventoryService inventoryService) {
        this.productionOrderRepository = productionOrderRepository;
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.productionControlOrderService = productionControlOrderService;
        this.assemblyControlOrderService = assemblyControlOrderService;
        this.inventoryService = inventoryService;
    }

    /**
     * Create a new production order for Scenario 3 (partial fulfillment).
     * Used when WarehouseOrder cannot be fully fulfilled from Modules Supermarket.
     */
    public ProductionOrderDTO createProductionOrderFromWarehouse(
            Long sourceCustomerOrderId,
            Long sourceWarehouseOrderId,
            String priority,
            LocalDateTime dueDate,
            String notes,
            Long createdByWorkstationId,
            Long assignedWorkstationId) {

        // If sourceCustomerOrderId is null, fetch it from the warehouse order
        Long customerOrderId = sourceCustomerOrderId;
        WarehouseOrder warehouseOrder = null;
        if (sourceWarehouseOrderId != null) {
            warehouseOrder = warehouseOrderRepository.findById(sourceWarehouseOrderId)
                    .orElseThrow(() -> new RuntimeException("Warehouse order not found: " + sourceWarehouseOrderId));
            if (customerOrderId == null) {
                customerOrderId = warehouseOrder.getCustomerOrderId();
                logger.info("Fetched customer order ID {} from warehouse order {}", 
                        customerOrderId, sourceWarehouseOrderId);
            }
        }

        if (customerOrderId == null) {
            throw new RuntimeException("sourceCustomerOrderId is required and could not be determined from warehouse order");
        }

        String productionOrderNumber = generateProductionOrderNumber();

        ProductionOrder productionOrder = ProductionOrder.builder()
                .productionOrderNumber(productionOrderNumber)
                .sourceCustomerOrderId(customerOrderId)
                .sourceWarehouseOrderId(sourceWarehouseOrderId)
                .status("CREATED")
                .priority(priority)
                .dueDate(dueDate)
                .triggerScenario("SCENARIO_3")
                .createdByWorkstationId(createdByWorkstationId)
                .assignedWorkstationId(assignedWorkstationId)
                .notes(notes)
                .build();

        // Create production order items from warehouse order items
        if (warehouseOrder != null && warehouseOrder.getOrderItems() != null) {
            List<ProductionOrderItem> productionOrderItems = new ArrayList<>();
            for (WarehouseOrderItem woItem : warehouseOrder.getOrderItems()) {
                // Determine workstation type based on item type
                // MODULE items require ASSEMBLY (workstations WS-4, WS-5, WS-6)
                // PART items require MANUFACTURING (workstations WS-1, WS-2, WS-3)
                String workstationType = "MODULE".equals(woItem.getItemType()) ? "ASSEMBLY" : "MANUFACTURING";
                
                ProductionOrderItem poItem = ProductionOrderItem.builder()
                        .productionOrder(productionOrder)
                        .itemType(woItem.getItemType())
                        .itemId(woItem.getItemId())
                        .itemName(woItem.getItemName())
                        .quantity(woItem.getRequestedQuantity())
                        .estimatedTimeMinutes(30) // Default estimate, could be fetched from masterdata
                        .workstationType(workstationType)
                        .build();
                productionOrderItems.add(poItem);
                
                logger.info("  Added production order item: {} (ID: {}) qty {} - workstation type: {}", 
                        woItem.getItemName(), woItem.getItemId(), woItem.getRequestedQuantity(), workstationType);
            }
            productionOrder.setProductionOrderItems(productionOrderItems);
        }

        @SuppressWarnings("null")
        ProductionOrder saved = productionOrderRepository.save(productionOrder);
        logger.info("Created production order {} from warehouse order {} with {} items", 
                productionOrderNumber, sourceWarehouseOrderId, 
                saved.getProductionOrderItems() != null ? saved.getProductionOrderItems().size() : 0);

        // Update warehouse order status to AWAITING_PRODUCTION and triggerScenario to PRODUCTION_CREATED
        if (warehouseOrder != null) {
            warehouseOrder.setStatus("AWAITING_PRODUCTION");
            warehouseOrder.setTriggerScenario("PRODUCTION_CREATED");
            warehouseOrderRepository.save(warehouseOrder);
            logger.info("Updated warehouse order {} status to AWAITING_PRODUCTION with triggerScenario PRODUCTION_CREATED", 
                    sourceWarehouseOrderId);
        }

        return mapToDTO(saved);
    }

    /**
     * Create a standalone production order (not linked to warehouse order).
     */
    public ProductionOrderDTO createStandaloneProductionOrder(
            Long sourceCustomerOrderId,
            String priority,
            LocalDateTime dueDate,
            String notes,
            Long createdByWorkstationId) {

        String productionOrderNumber = generateProductionOrderNumber();

        ProductionOrder productionOrder = ProductionOrder.builder()
                .productionOrderNumber(productionOrderNumber)
                .sourceCustomerOrderId(sourceCustomerOrderId)
                .sourceWarehouseOrderId(null)
                .status("CREATED")
                .priority(priority)
                .dueDate(dueDate)
                .triggerScenario("STANDALONE")
                .createdByWorkstationId(createdByWorkstationId)
                .notes(notes)
                .build();

        @SuppressWarnings("null")
        ProductionOrder saved = productionOrderRepository.save(productionOrder);
        logger.info("Created standalone production order {}", productionOrderNumber);

        return mapToDTO(saved);
    }

    /**
     * Get all production orders.
     */
    public List<ProductionOrderDTO> getAllProductionOrders() {
        return productionOrderRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get production order by ID.
     */
    @SuppressWarnings("null")
    public Optional<ProductionOrderDTO> getProductionOrderById(Long id) {
        return productionOrderRepository.findById(id)
                .map(this::mapToDTO);
    }

    /**
     * Get production order by production order number.
     */
    public Optional<ProductionOrderDTO> getProductionOrderByNumber(String productionOrderNumber) {
        return productionOrderRepository.findByProductionOrderNumber(productionOrderNumber)
                .map(this::mapToDTO);
    }

    /**
     * Get all production orders from a specific customer order.
     */
    public List<ProductionOrderDTO> getProductionOrdersByCustomerOrder(Long sourceCustomerOrderId) {
        return productionOrderRepository.findBySourceCustomerOrderId(sourceCustomerOrderId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all production orders from a specific warehouse order.
     */
    public List<ProductionOrderDTO> getProductionOrdersByWarehouseOrder(Long sourceWarehouseOrderId) {
        return productionOrderRepository.findBySourceWarehouseOrderId(sourceWarehouseOrderId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get production orders by status.
     */
    public List<ProductionOrderDTO> getProductionOrdersByStatus(String status) {
        return productionOrderRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get production orders by priority.
     */
    public List<ProductionOrderDTO> getProductionOrdersByPriority(String priority) {
        return productionOrderRepository.findByPriority(priority).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get production orders created by a specific workstation (PP operator).
     */
    public List<ProductionOrderDTO> getProductionOrdersByWorkstation(Long createdByWorkstationId) {
        return productionOrderRepository.findByCreatedByWorkstationId(createdByWorkstationId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get production orders assigned to a specific workstation (assembly/completion).
     * Used by assembly operators to find orders they need to complete.
     */
    public List<ProductionOrderDTO> getProductionOrdersByAssignedWorkstation(Long assignedWorkstationId) {
        return productionOrderRepository.findByAssignedWorkstationId(assignedWorkstationId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update production order status.
     */
    public ProductionOrderDTO updateProductionOrderStatus(Long id, String newStatus) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        productionOrder.setStatus(newStatus);
        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Updated production order {} to status {}", id, newStatus);

        return mapToDTO(updated);
    }

    /**
     * Link production order to SimAL schedule.
     */
    public ProductionOrderDTO linkToSimalSchedule(Long id, String simalScheduleId, 
                                                  Integer estimatedDuration, 
                                                  LocalDateTime expectedCompletionTime) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        productionOrder.setSimalScheduleId(simalScheduleId);
        productionOrder.setEstimatedDuration(estimatedDuration);
        productionOrder.setExpectedCompletionTime(expectedCompletionTime);
        productionOrder.setStatus("SCHEDULED");

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Linked production order {} to SimAL schedule {}", id, simalScheduleId);

        return mapToDTO(updated);
    }

    /**
     * Mark production order as completed.
     * Credits Modules Supermarket (WS-8) with produced modules/parts.
     */
    public ProductionOrderDTO completeProductionOrder(Long id) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        productionOrder.setStatus("COMPLETED");
        productionOrder.setActualCompletionTime(LocalDateTime.now());

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Marked production order {} as completed", id);

        // Credit Modules Supermarket with produced items
        creditModulesSupermarket(updated);

        return mapToDTO(updated);
    }

    /**
     * Cancel production order.
     */
    public ProductionOrderDTO cancelProductionOrder(Long id) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        productionOrder.setStatus("CANCELLED");

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Cancelled production order {}", id);

        return mapToDTO(updated);
    }

    /**
     * Confirm production order (CREATED -> CONFIRMED).
     * Confirms that the production planner has reviewed the order and it's ready for scheduling.
     */
    public ProductionOrderDTO confirmProductionOrder(Long id) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        if (!"CREATED".equals(productionOrder.getStatus())) {
            throw new RuntimeException("Production order must be in CREATED status to confirm. Current status: " 
                    + productionOrder.getStatus());
        }

        productionOrder.setStatus("CONFIRMED");

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Confirmed production order {}", id);

        return mapToDTO(updated);
    }

    /**
     * Schedule production order via SimAL integration (CONFIRMED -> SCHEDULED).
     * This method integrates with SimAL to calculate timeline and generate Gantt chart.
     * 
     * @param id Production order ID
     * @param scheduledStart Scheduled start time from SimAL
     * @param scheduledEnd Scheduled end time from SimAL
     * @param ganttChartId Gantt chart ID generated by SimAL
     * @return Updated production order DTO
     */
    public ProductionOrderDTO scheduleProduction(Long id, LocalDateTime scheduledStart, 
                                                LocalDateTime scheduledEnd, String ganttChartId) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        if (!"CONFIRMED".equals(productionOrder.getStatus())) {
            throw new RuntimeException("Production order must be CONFIRMED to schedule. Current status: " 
                    + productionOrder.getStatus());
        }

        // Update scheduling information
        productionOrder.setStatus("SCHEDULED");
        productionOrder.setSimalScheduleId(ganttChartId);
        productionOrder.setExpectedCompletionTime(scheduledEnd);

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Scheduled production order {} with SimAL. Start: {}, End: {}, GanttID: {}", 
                    id, scheduledStart, scheduledEnd, ganttChartId);

        return mapToDTO(updated);
    }

    /**
     * Dispatch production order to control stations (SCHEDULED -> DISPATCHED).
     * Creates ProductionControlOrder and AssemblyControlOrder entities.
     * This is the downward dispatch step in Scenario 3 flow.
     * 
     * @param id Production order ID
     * @return Updated production order DTO
     */
    public ProductionOrderDTO dispatchToControlStations(Long id) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        if (!"SCHEDULED".equals(productionOrder.getStatus())) {
            throw new RuntimeException("Production order must be SCHEDULED to dispatch. Current status: " 
                    + productionOrder.getStatus());
        }

        // Create control orders based on production order items
        int manufacturingOrdersCreated = 0;
        int assemblyOrdersCreated = 0;
        
        if (productionOrder.getProductionOrderItems() != null && !productionOrder.getProductionOrderItems().isEmpty()) {
            logger.info("Creating control orders for {} production order items", productionOrder.getProductionOrderItems().size());
            
            for (ProductionOrderItem item : productionOrder.getProductionOrderItems()) {
                String workstationType = item.getWorkstationType();
                logger.info("  Processing item: {} (ID: {}, Type: {}, WorkstationType: {})", 
                        item.getItemName(), item.getItemId(), item.getItemType(), workstationType);
                
                if ("MANUFACTURING".equals(workstationType)) {
                    // Create Production Control Order for manufacturing workstations (WS-1, WS-2, WS-3)
                    ProductionControlOrderDTO controlOrder = productionControlOrderService.createControlOrder(
                        productionOrder.getId(),
                        null, // Will be assigned by production control station
                        productionOrder.getSimalScheduleId(),
                        productionOrder.getPriority(),
                        LocalDateTime.now(), // Start now or based on schedule
                        productionOrder.getExpectedCompletionTime(),
                        "Manufacture " + item.getItemName() + " (Qty: " + item.getQuantity() + ")",
                        "Check quality after each stage",
                        "Follow safety protocols for machinery",
                        item.getEstimatedTimeMinutes(),
                        item.getItemId(),
                        item.getItemType(),
                        item.getQuantity()
                    );
                    manufacturingOrdersCreated++;
                    logger.info("    ✓ Created Production Control Order: {} for item {} (ID: {}) qty {}", 
                            controlOrder.getControlOrderNumber(), item.getItemType(), item.getItemId(), item.getQuantity());
                    
                } else if ("ASSEMBLY".equals(workstationType)) {
                    // Create Assembly Control Order for assembly workstations (WS-4, WS-5, WS-6)
                    AssemblyControlOrderDTO assemblyOrder = assemblyControlOrderService.createControlOrder(
                        productionOrder.getId(),
                        null, // Will be assigned by assembly control station
                        productionOrder.getSimalScheduleId(),
                        productionOrder.getPriority(),
                        LocalDateTime.now(),
                        productionOrder.getExpectedCompletionTime(),
                        "Assemble " + item.getItemName() + " (Qty: " + item.getQuantity() + ")",
                        "Verify assembly quality",
                        "Test functionality after assembly",
                        "Package according to specifications",
                        item.getEstimatedTimeMinutes(),
                        item.getItemId(),
                        item.getItemType(),
                        item.getQuantity()
                    );
                    assemblyOrdersCreated++;
                    logger.info("    ✓ Created Assembly Control Order: {}", assemblyOrder.getControlOrderNumber());
                }
            }
        }
        
        logger.info("Dispatch complete: {} Manufacturing orders, {} Assembly orders created", 
                manufacturingOrdersCreated, assemblyOrdersCreated);

        productionOrder.setStatus("DISPATCHED");
        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Dispatched production order {} to control stations - created control orders", id);

        return mapToDTO(updated);
    }

    /**
     * Update production order from control order completion (upward notification).
     * Called when all control orders for this production order are fulfilled.
     * Transitions: DISPATCHED/IN_PROGRESS -> COMPLETED
     * 
     * This is part of the upward confirmation flow in Scenario 3.
     * 
     * @param id Production order ID
     * @param controlOrderId Control order that just completed (for logging)
     * @return Updated production order DTO
     */
    public ProductionOrderDTO updateFromControlOrderCompletion(Long id, Long controlOrderId) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        logger.info("Received completion notification from control order {} for production order {}", 
                    controlOrderId, id);

        // TODO: Feature 3.2 - Check if ALL control orders are complete
        // For now, we'll mark as IN_PROGRESS on first completion, COMPLETED when all done
        
        if ("DISPATCHED".equals(productionOrder.getStatus())) {
            productionOrder.setStatus("IN_PROGRESS");
            logger.info("Production order {} transitioned to IN_PROGRESS", id);
        }

        // TODO: Add logic to check if all control orders are complete
        // If all complete, transition to COMPLETED and notify warehouse order
        // For now, manual completion via completeProductionOrder()

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        return mapToDTO(updated);
    }

    /**
     * Complete production order and notify source warehouse order (final step).
     * This triggers the warehouse order to resume and create final assembly order.
     * Credits Modules Supermarket (WS-8) with produced modules.
     * Updates source WarehouseOrder status to MODULES_READY for final assembly dispatch.
     * 
     * @param id Production order ID
     * @return Updated production order DTO
     */
    public ProductionOrderDTO completeProductionOrderWithNotification(Long id) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        productionOrder.setStatus("COMPLETED");
        productionOrder.setActualCompletionTime(LocalDateTime.now());

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Completed production order {}", id);

        // Credit Modules Supermarket with produced items
        creditModulesSupermarket(updated);

        // Feature 3.7 (Orchestration) - Notify warehouse order that modules are ready
        // WarehouseOrder transitions from AWAITING_PRODUCTION to MODULES_READY for fulfillment
        notifyWarehouseOrderModulesReady(updated);

        return mapToDTO(updated);
    }

    /**
     * Notify the source warehouse order that production is complete and modules are ready.
     * Updates the warehouse order status and triggerScenario so it can proceed to fulfillment.
     * 
     * @param productionOrder The completed production order
     */
    private void notifyWarehouseOrderModulesReady(ProductionOrder productionOrder) {
        if (productionOrder.getSourceWarehouseOrderId() == null) {
            logger.info("Production order {} has no source warehouse order to notify", 
                    productionOrder.getProductionOrderNumber());
            return;
        }

        try {
            WarehouseOrder warehouseOrder = warehouseOrderRepository.findById(productionOrder.getSourceWarehouseOrderId())
                    .orElse(null);
            
            if (warehouseOrder == null) {
                logger.warn("Source warehouse order {} not found for production order {}", 
                        productionOrder.getSourceWarehouseOrderId(), productionOrder.getProductionOrderNumber());
                return;
            }

            // Update warehouse order status to indicate modules are now available
            String previousStatus = warehouseOrder.getStatus();
            warehouseOrder.setStatus("MODULES_READY");
            warehouseOrder.setTriggerScenario("PRODUCTION_COMPLETE");
            warehouseOrder.setNotes((warehouseOrder.getNotes() != null ? warehouseOrder.getNotes() + " | " : "") 
                    + "Production completed - modules credited to Modules Supermarket - ready to fulfill");
            
            warehouseOrderRepository.save(warehouseOrder);
            
            logger.info("✓ Warehouse order {} notified: {} → MODULES_READY (production order {} completed)", 
                    warehouseOrder.getOrderNumber(), previousStatus, productionOrder.getProductionOrderNumber());
            
        } catch (Exception e) {
            logger.error("Failed to notify warehouse order for production order {}: {}", 
                    productionOrder.getProductionOrderNumber(), e.getMessage());
            // Don't throw - production completion succeeded, warehouse notification is secondary
        }
    }

    /**
     * Credit Modules Supermarket (WS-8) with produced modules from a production order.
     * This is called when a production order completes to add the produced items to inventory.
     * 
     * NOTE: For Scenario 3, production orders are created from warehouse orders which contain
     * only MODULEs (after BOM conversion from Products → Modules). The implementation supports
     * PART types for future flexibility (e.g., Scenario 4 or standalone production orders),
     * but in practice Scenario 3 will only credit MODULE items to the Modules Supermarket.
     * 
     * @param productionOrder The completed production order
     */
    private void creditModulesSupermarket(ProductionOrder productionOrder) {
        if (productionOrder.getProductionOrderItems() == null || productionOrder.getProductionOrderItems().isEmpty()) {
            logger.warn("Production order {} has no items to credit", productionOrder.getProductionOrderNumber());
            return;
        }

        logger.info("=== Crediting Modules Supermarket for production order {} ===", 
                productionOrder.getProductionOrderNumber());
        
        int successCount = 0;
        int failCount = 0;
        
        for (ProductionOrderItem item : productionOrder.getProductionOrderItems()) {
            try {
                String notes = String.format("Completed production order: %s - %s", 
                        productionOrder.getProductionOrderNumber(), item.getItemName());
                
                boolean credited = inventoryService.creditProductionStock(
                        MODULES_SUPERMARKET_WORKSTATION_ID,
                        item.getItemType(),
                        item.getItemId(),
                        item.getQuantity(),
                        notes
                );
                
                if (credited) {
                    logger.info("  ✓ Credited {} {} {} ({}) to Modules Supermarket", 
                            item.getQuantity(), item.getItemType(), item.getItemId(), item.getItemName());
                    successCount++;
                } else {
                    logger.warn("  ✗ Failed to credit {} {} {} ({}) to Modules Supermarket", 
                            item.getQuantity(), item.getItemType(), item.getItemId(), item.getItemName());
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("  ✗ Error crediting {} {} {} ({}): {}", 
                        item.getQuantity(), item.getItemType(), item.getItemId(), item.getItemName(), e.getMessage());
                failCount++;
            }
        }
        
        logger.info("=== Modules Supermarket credit complete: {} succeeded, {} failed ===", successCount, failCount);
    }

    /**
     * Generate unique production order number.
     */
    private String generateProductionOrderNumber() {
        long count = productionOrderRepository.count();
        return "PO-" + String.format("%04d", count + 1);
    }

    /**
     * Map ProductionOrder entity to DTO.
     */
    private ProductionOrderDTO mapToDTO(ProductionOrder productionOrder) {
        // Map production order items
        List<ProductionOrderDTO.ProductionOrderItemDTO> itemDTOs = null;
        if (productionOrder.getProductionOrderItems() != null) {
            itemDTOs = productionOrder.getProductionOrderItems().stream()
                    .map(item -> ProductionOrderDTO.ProductionOrderItemDTO.builder()
                            .id(item.getId())
                            .itemType(item.getItemType())
                            .itemId(item.getItemId())
                            .itemName(item.getItemName())
                            .quantity(item.getQuantity())
                            .estimatedTimeMinutes(item.getEstimatedTimeMinutes())
                            .workstationType(item.getWorkstationType())
                            .build())
                    .collect(Collectors.toList());
        }
        
        return ProductionOrderDTO.builder()
                .id(productionOrder.getId())
                .productionOrderNumber(productionOrder.getProductionOrderNumber())
                .sourceCustomerOrderId(productionOrder.getSourceCustomerOrderId())
                .sourceWarehouseOrderId(productionOrder.getSourceWarehouseOrderId())
                .simalScheduleId(productionOrder.getSimalScheduleId())
                .status(productionOrder.getStatus())
                .priority(productionOrder.getPriority())
                .dueDate(productionOrder.getDueDate())
                .triggerScenario(productionOrder.getTriggerScenario())
                .createdByWorkstationId(productionOrder.getCreatedByWorkstationId())
                .notes(productionOrder.getNotes())
                .estimatedDuration(productionOrder.getEstimatedDuration())
                .expectedCompletionTime(productionOrder.getExpectedCompletionTime())
                .actualCompletionTime(productionOrder.getActualCompletionTime())
                .createdAt(productionOrder.getCreatedAt())
                .updatedAt(productionOrder.getUpdatedAt())
                .productionOrderItems(itemDTOs)
                .build();
    }
}
