package io.life.order.service;

import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.SupplyOrderItemDTO;
import io.life.order.dto.request.AssemblyControlOrderCreateRequest;
import io.life.order.entity.AssemblyControlOrder;
import io.life.order.entity.CustomerOrder;
import io.life.order.repository.AssemblyControlOrderRepository;
import io.life.order.repository.CustomerOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing AssemblyControlOrder entities.
 * Handles control orders assigned to Assembly Control workstations.
 */
@Service
@Transactional
public class AssemblyControlOrderService implements WorkstationOrderOperations<AssemblyControlOrderDTO> {

    private static final Logger logger = LoggerFactory.getLogger(AssemblyControlOrderService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_HALTED = "HALTED";

    private final AssemblyControlOrderRepository repository;
    private final SupplyOrderService supplyOrderService;
    private final InventoryService inventoryService;
    private final CustomerOrderRepository customerOrderRepository;
    private final SimALNotificationService simalNotificationService;

    @Value("${modules.supermarket.workstation.id:8}")
    private Long modulesSupermarketWorkstationId;

    @Value("${plant.warehouse.workstation.id:7}")
    private Long plantWarehouseWorkstationId;

    public AssemblyControlOrderService(AssemblyControlOrderRepository repository, 
                                      SupplyOrderService supplyOrderService,
                                      InventoryService inventoryService,
                                      CustomerOrderRepository customerOrderRepository,
                                      SimALNotificationService simalNotificationService) {
        this.repository = repository;
        this.supplyOrderService = supplyOrderService;
        this.inventoryService = inventoryService;
        this.customerOrderRepository = customerOrderRepository;
        this.simalNotificationService = simalNotificationService;
    }

    /**
     * Create an assembly control order using a request DTO.
     * This is the preferred method - eliminates parameter explosion.
     * 
     * @param request The request DTO containing all order details
     * @return The created assembly control order DTO
     */
    public AssemblyControlOrderDTO createControlOrder(AssemblyControlOrderCreateRequest request) {
        LocalDateTime targetStart = request.getTargetStartTime() != null 
                ? LocalDateTime.parse(request.getTargetStartTime()) : null;
        LocalDateTime targetEnd = request.getTargetCompletionTime() != null 
                ? LocalDateTime.parse(request.getTargetCompletionTime()) : null;

        return createControlOrder(
                request.getSourceProductionOrderId(),
                request.getAssignedWorkstationId(),
                request.getSimalScheduleId(),
                request.getPriority(),
                targetStart,
                targetEnd,
                request.getAssemblyInstructions(),
                request.getQualityCheckpoints(),
                request.getTestingProcedures(),
                request.getPackagingRequirements(),
                request.getEstimatedDurationMinutes(),
                request.getItemId(),
                request.getItemType(),
                request.getQuantity()
        );
    }

    /**
     * Create a new assembly control order (backwards compatible version without item info).
     * Uses default values for itemId (1L), itemType ("MODULE"), quantity (1).
     * @deprecated Use {@link #createControlOrder(AssemblyControlOrderCreateRequest)} instead
     */
    @Deprecated
    public AssemblyControlOrderDTO createControlOrder(
            Long sourceProductionOrderId,
            Long assignedWorkstationId,
            String simalScheduleId,
            String priority,
            LocalDateTime targetStartTime,
            LocalDateTime targetCompletionTime,
            String assemblyInstructions,
            String qualityCheckpoints,
            String testingProcedures,
            String packagingRequirements,
            Integer estimatedDurationMinutes) {
        
        // Call the full method with default item values
        return createControlOrder(
                sourceProductionOrderId, assignedWorkstationId, simalScheduleId,
                priority, targetStartTime, targetCompletionTime,
                assemblyInstructions, qualityCheckpoints, testingProcedures,
                packagingRequirements, estimatedDurationMinutes,
                1L, "MODULE", 1  // Default values for backwards compatibility
        );
    }

    /**
     * Create a new assembly control order (full version with item info).
     * @deprecated Use {@link #createControlOrder(AssemblyControlOrderCreateRequest)} instead
     */
    @Deprecated
    public AssemblyControlOrderDTO createControlOrder(
            Long sourceProductionOrderId,
            Long assignedWorkstationId,
            String simalScheduleId,
            String priority,
            LocalDateTime targetStartTime,
            LocalDateTime targetCompletionTime,
            String assemblyInstructions,
            String qualityCheckpoints,
            String testingProcedures,
            String packagingRequirements,
            Integer estimatedDurationMinutes,
            Long itemId,
            String itemType,
            Integer quantity) {

        String controlOrderNumber = generateControlOrderNumber();

        AssemblyControlOrder order = AssemblyControlOrder.builder()
                .controlOrderNumber(controlOrderNumber)
                .sourceProductionOrderId(sourceProductionOrderId)
                .assignedWorkstationId(assignedWorkstationId)
                .simalScheduleId(simalScheduleId)
                .status(STATUS_PENDING)
                .priority(priority)
                .targetStartTime(targetStartTime)
                .targetCompletionTime(targetCompletionTime)
                .assemblyInstructions(assemblyInstructions)
                .qualityCheckpoints(qualityCheckpoints)
                .testingProcedures(testingProcedures)
                .packagingRequirements(packagingRequirements)
                .estimatedDurationMinutes(estimatedDurationMinutes)
                .itemId(itemId)
                .itemType(itemType)
                .quantity(quantity)
                .build();

        @SuppressWarnings("null")
        AssemblyControlOrder saved = repository.save(order);
        logger.info("Created assembly control order {} (ID: {}) with status PENDING for production order {}",
                    controlOrderNumber, saved.getId(), sourceProductionOrderId);

        return mapToDTO(saved);
    }

    /**
     * Get all control orders.
     */
    public List<AssemblyControlOrderDTO> getAllOrders() {
        return repository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all control orders for a workstation.
     */
    @Override
    public List<AssemblyControlOrderDTO> getOrdersByWorkstation(Long workstationId) {
        return repository.findByAssignedWorkstationId(workstationId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active control orders for a workstation.
     */
    @Override
    public List<AssemblyControlOrderDTO> getActiveOrdersByWorkstation(Long workstationId) {
        return repository.findByAssignedWorkstationIdAndStatus(workstationId, STATUS_IN_PROGRESS).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all unassigned control orders (status = ASSIGNED).
     */
    @Override
    public List<AssemblyControlOrderDTO> getUnassignedOrders(Long workstationId) {
        return repository.findByAssignedWorkstationIdAndStatus(workstationId, STATUS_ASSIGNED).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get control order by ID.
     */
    @Override
    @SuppressWarnings("null")
    public Optional<AssemblyControlOrderDTO> getOrderById(Long id) {
        return repository.findById(id).map(this::mapToDTO);
    }

    /**
     * Get control order by control order number.
     */
    @Override
    public Optional<AssemblyControlOrderDTO> getOrderByNumber(String controlOrderNumber) {
        return repository.findByControlOrderNumber(controlOrderNumber).map(this::mapToDTO);
    }

    /**
     * Start assembly on a control order.
     */
    public AssemblyControlOrderDTO startAssembly(Long id) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        if (!STATUS_ASSIGNED.equals(order.getStatus())) {
            throw new IllegalStateException("Cannot start assembly - order status is " + order.getStatus());
        }

        order.setStatus(STATUS_IN_PROGRESS);
        order.setActualStartTime(LocalDateTime.now());

        AssemblyControlOrder updated = repository.save(order);
        logger.info("Started assembly on control order {}", order.getControlOrderNumber());

        return mapToDTO(updated);
    }

    /**
     * Complete assembly on a control order.
     */
    public AssemblyControlOrderDTO completeAssembly(Long id) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        order.setStatus(STATUS_COMPLETED);
        order.setActualCompletionTime(LocalDateTime.now());
        
        if (order.getActualStartTime() != null) {
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    order.getActualStartTime(),
                    order.getActualCompletionTime()
            );
            order.setActualDurationMinutes((int) minutes);
        }

        AssemblyControlOrder updated = repository.save(order);
        logger.info("Completed assembly on control order {}", order.getControlOrderNumber());

        return mapToDTO(updated);
    }

    /**
     * Complete assembly production with SimAL and Modules Supermarket inventory integration.
     * Used for Gear Assembly and Motor Assembly workstations.
     * Credits Modules Supermarket (WS-8) with one module unit.
     */
    public AssemblyControlOrderDTO completeAssemblyProduction(Long id) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        if (!STATUS_IN_PROGRESS.equals(order.getStatus())) {
            throw new IllegalStateException("Cannot complete - order status is " + order.getStatus());
        }

        // Step 1: Update control order status and timestamps
        order.setStatus(STATUS_COMPLETED);
        order.setActualCompletionTime(LocalDateTime.now());
        
        if (order.getActualStartTime() != null) {
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    order.getActualStartTime(),
                    order.getActualCompletionTime()
            );
            order.setActualDurationMinutes((int) minutes);
        }

        AssemblyControlOrder updated = repository.save(order);
        logger.info("Completed assembly production on control order {}", order.getControlOrderNumber());

        // Step 2: Call SimAL to update schedule status (fire-and-forget)
        simalNotificationService.tryUpdateScheduleStatus(order.getSimalScheduleId(), "COMPLETED");

        // Step 3: Credit Modules Supermarket inventory (fire-and-forget)
        try {
            creditModulesSupermarket(order.getSourceProductionOrderId(), 1);
        } catch (Exception e) {
            logger.warn("Failed to credit Modules Supermarket for order {}: {}", order.getSourceProductionOrderId(), e.getMessage());
            // Don't throw - completion already succeeded, inventory update is secondary
        }

        return mapToDTO(updated);
    }

    /**
     * Complete final assembly with SimAL and Plant Warehouse inventory integration.
     * Used only for Final Assembly workstations.
     * Credits Plant Warehouse (WS-7) with one finished product unit instead of Modules Supermarket.
     * This represents the completion of the entire product ready for shipping.
     * Also updates the source customer order status to CONFIRMED so Plant Warehouse can fulfill it.
     */
    public AssemblyControlOrderDTO completeFinalAssembly(Long id) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        if (!STATUS_IN_PROGRESS.equals(order.getStatus())) {
            throw new IllegalStateException("Cannot complete - order status is " + order.getStatus());
        }

        // Step 1: Update control order status and timestamps
        order.setStatus(STATUS_COMPLETED);
        order.setActualCompletionTime(LocalDateTime.now());
        
        if (order.getActualStartTime() != null) {
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    order.getActualStartTime(),
                    order.getActualCompletionTime()
            );
            order.setActualDurationMinutes((int) minutes);
        }

        AssemblyControlOrder updated = repository.save(order);
        logger.info("Completed final assembly on control order {}", order.getControlOrderNumber());

        // Step 2: Call SimAL to update schedule status (fire-and-forget)
        simalNotificationService.tryUpdateScheduleStatus(order.getSimalScheduleId(), STATUS_COMPLETED);

        // Step 3: Credit Plant Warehouse inventory (fire-and-forget) - FINAL ASSEMBLY ONLY
        try {
            creditPlantWarehouse(order.getItemId(), order.getQuantity());
        } catch (Exception e) {
            logger.warn("Failed to credit Plant Warehouse for order {}: {}", order.getSourceProductionOrderId(), e.getMessage());
            // Don't throw - completion already succeeded, inventory update is secondary
        }

        // Step 4: Update customer order status to CONFIRMED so Plant Warehouse can fulfill it
        try {
            updateCustomerOrderAfterAssembly(order.getSourceProductionOrderId());
        } catch (Exception e) {
            logger.warn("Failed to update customer order for production order {}: {}", order.getSourceProductionOrderId(), e.getMessage());
            // Don't throw - completion already succeeded, customer order update is secondary
        }

        return mapToDTO(updated);
    }

    /**
     * Halt assembly on a control order.
     */
    public AssemblyControlOrderDTO haltAssembly(Long id, String reason) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        if (!STATUS_IN_PROGRESS.equals(order.getStatus())) {
            throw new IllegalStateException("Cannot halt - order status is " + order.getStatus());
        }

        order.setStatus(STATUS_HALTED);
        if (reason != null && !reason.isBlank()) {
            order.setOperatorNotes("Halted: " + reason);
        }
        AssemblyControlOrder updated = repository.save(order);
        logger.warn("Halted assembly on control order {}: {}", order.getControlOrderNumber(), reason);

        return mapToDTO(updated);
    }

    /**
     * Update operator notes.
     */
    @Override
    public AssemblyControlOrderDTO updateOperatorNotes(Long id, String notes) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        order.setOperatorNotes(notes);
        AssemblyControlOrder updated = repository.save(order);

        return mapToDTO(updated);
    }

    /**
     * Update defect information.
     */
    public AssemblyControlOrderDTO updateDefects(Long id, Integer defectsFound, Integer defectsReworked, Boolean reworkRequired) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        order.setDefectsFound(defectsFound);
        order.setDefectsReworked(defectsReworked);
        order.setReworkRequired(reworkRequired);

        AssemblyControlOrder updated = repository.save(order);
        logger.info("Updated defect info for control order {}: found={}, reworked={}", 
                order.getControlOrderNumber(), defectsFound, defectsReworked);

        return mapToDTO(updated);
    }

    /**
     * Update shipping notes.
     */
    public AssemblyControlOrderDTO updateShippingNotes(Long id, String shippingNotes) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + id));

        order.setShippingNotes(shippingNotes);
        AssemblyControlOrder updated = repository.save(order);

        return mapToDTO(updated);
    }

    /**
     * Request parts/supplies for this assembly control order.
     * Creates a SupplyOrder that will be sent to the Parts Supply Warehouse.
     */
    public SupplyOrderDTO requestSupplies(
            Long controlOrderId,
            List<SupplyOrderItemDTO> requiredParts,
            LocalDateTime neededBy,
            String notes) {
        
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(controlOrderId)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + controlOrderId));

        return supplyOrderService.createSupplyOrder(
                controlOrderId,
                "ASSEMBLY",
                order.getAssignedWorkstationId(),
                order.getPriority(),
                neededBy,
                requiredParts,
                notes
        );
    }

    /**
     * Check if control order has a fulfilled supply order.
     */
    public boolean hasFulfilledSupplyOrder(Long controlOrderId) {
        List<SupplyOrderDTO> supplyOrders = supplyOrderService.getSupplyOrdersForControlOrder(controlOrderId, "ASSEMBLY");
        return supplyOrders.stream()
                .anyMatch(so -> "FULFILLED".equals(so.getStatus()));
    }

    /**
     * Check if control order has an active (pending) supply order.
     */
    public boolean hasActiveSupplyOrder(Long controlOrderId) {
        List<SupplyOrderDTO> supplyOrders = supplyOrderService.getSupplyOrdersForControlOrder(controlOrderId, "ASSEMBLY");
        return supplyOrders.stream()
                .anyMatch(so -> "PENDING".equals(so.getStatus()) || "IN_PROGRESS".equals(so.getStatus()));
    }

    /**
     * Get supply orders for this control order.
     */
    public List<SupplyOrderDTO> getSupplyOrders(Long controlOrderId) {
        return supplyOrderService.getSupplyOrdersForControlOrder(controlOrderId, "ASSEMBLY");
    }

    /**
     * Dispatch control order to workstation.
     * Validates that supply order is fulfilled before dispatching.
     * Changes status from ASSIGNED to IN_PROGRESS.
     */
    public AssemblyControlOrderDTO dispatchToWorkstation(Long controlOrderId) {
        @SuppressWarnings("null")
        AssemblyControlOrder order = repository.findById(controlOrderId)
                .orElseThrow(() -> new RuntimeException("Control order not found: " + controlOrderId));
        
        // Validate supply order is fulfilled (if one exists)
        List<SupplyOrderDTO> supplyOrders = getSupplyOrders(controlOrderId);
        if (!supplyOrders.isEmpty() && !hasFulfilledSupplyOrder(controlOrderId)) {
            throw new RuntimeException("Cannot dispatch order - supply order not fulfilled");
        }
        
        // Validate current status
        if (!"ASSIGNED".equals(order.getStatus())) {
            throw new RuntimeException("Cannot dispatch order with status: " + order.getStatus());
        }
        
        // Update status to IN_PROGRESS
        order.setStatus("IN_PROGRESS");
        order.setUpdatedAt(LocalDateTime.now());
        
        AssemblyControlOrder saved = repository.save(order);
        logger.info("Dispatched assembly control order {} to workstation {}", 
                    order.getControlOrderNumber(), order.getAssignedWorkstationId());
        
        return mapToDTO(saved);
    }

    /**
     * Generate unique control order number.
     */
    private String generateControlOrderNumber() {
        long count = repository.count();
        return "ACO-" + String.format("%04d", count + 1);
    }

    /**
     * Map entity to DTO.
     */
    private AssemblyControlOrderDTO mapToDTO(AssemblyControlOrder order) {
        return AssemblyControlOrderDTO.builder()
                .id(order.getId())
                .controlOrderNumber(order.getControlOrderNumber())
                .sourceProductionOrderId(order.getSourceProductionOrderId())
                .assignedWorkstationId(order.getAssignedWorkstationId())
                .simalScheduleId(order.getSimalScheduleId())
                .status(order.getStatus())
                .targetStartTime(order.getTargetStartTime())
                .targetCompletionTime(order.getTargetCompletionTime())
                .actualStartTime(order.getActualStartTime())
                .actualCompletionTime(order.getActualCompletionTime())
                .priority(order.getPriority())
                .assemblyInstructions(order.getAssemblyInstructions())
                .qualityCheckpoints(order.getQualityCheckpoints())
                .testingProcedures(order.getTestingProcedures())
                .packagingRequirements(order.getPackagingRequirements())
                .estimatedDurationMinutes(order.getEstimatedDurationMinutes())
                .actualDurationMinutes(order.getActualDurationMinutes())
                .defectsFound(order.getDefectsFound())
                .defectsReworked(order.getDefectsReworked())
                .reworkRequired(order.getReworkRequired())
                .reworkNotes(order.getReworkNotes())
                .operatorNotes(order.getOperatorNotes())
                .shippingNotes(order.getShippingNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }

    /**
     * Credit Modules Supermarket inventory when assembly completes.
     * Awards one module unit to Modules Supermarket (workstation 8).
     */
    private void creditModulesSupermarket(Long productionOrderId, Integer quantity) {
        try {
            inventoryService.updateStock(modulesSupermarketWorkstationId, 1L, quantity);
            logger.info("Credited Modules Supermarket with {} units for production order {}", quantity, productionOrderId);
        } catch (Exception e) {
            logger.error("Failed to credit Modules Supermarket for production order {}", productionOrderId, e);
            throw new RuntimeException("Inventory credit failed: " + e.getMessage(), e);
        }
    }

    /**
     * Credit Plant Warehouse inventory when final assembly completes.
     * Awards finished product units to Plant Warehouse (workstation 7).
     * Only used for Final Assembly workstations.
     */
    private void creditPlantWarehouse(Long productId, Integer quantity) {
        try {
            inventoryService.updateStock(plantWarehouseWorkstationId, productId, quantity);
            logger.info("Credited Plant Warehouse with {} units of Product #{}", quantity, productId);
        } catch (Exception e) {
            logger.error("Failed to credit Plant Warehouse with Product #{}", productId, e);
            throw new RuntimeException("Inventory credit failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update customer order status to CONFIRMED after Final Assembly completion.
     * This allows Plant Warehouse to fulfill the customer order now that stock is available.
     * The sourceProductionOrderId in AssemblyControlOrder actually references the customer order ID
     * (due to how warehouse orders are created - they use customer order ID as source).
     */
    private void updateCustomerOrderAfterAssembly(Long customerOrderId) {
        try {
            @SuppressWarnings("null")
            Optional<CustomerOrder> customerOrderOpt = customerOrderRepository.findById(customerOrderId);
            
            if (customerOrderOpt.isPresent()) {
                CustomerOrder customerOrder = customerOrderOpt.get();
                
                // Only update if order is in PROCESSING status (waiting for assembly)
                if ("PROCESSING".equals(customerOrder.getStatus())) {
                    customerOrder.setStatus("CONFIRMED");
                    customerOrder.setNotes((customerOrder.getNotes() != null ? customerOrder.getNotes() + " | " : "") 
                            + "Final Assembly completed - products ready in Plant Warehouse - ready to fulfill");
                    customerOrder.setUpdatedAt(LocalDateTime.now());
                    customerOrderRepository.save(customerOrder);
                    
                    logger.info("✓ Customer order {} updated to CONFIRMED - ready for Plant Warehouse fulfillment", 
                            customerOrder.getOrderNumber());
                } else {
                    logger.warn("Customer order {} is in status {} (expected PROCESSING) - skipping status update",
                            customerOrder.getOrderNumber(), customerOrder.getStatus());
                }
            } else {
                logger.warn("Customer order {} not found - cannot update status after assembly", customerOrderId);
            }
        } catch (Exception e) {
            logger.error("✗ Failed to update customer order {} after assembly: {}", customerOrderId, e.getMessage());
            throw new RuntimeException("Customer order update failed: " + e.getMessage(), e);
        }
    }

    // ========================================================================
    // WorkstationOrderOperations interface implementation (adapter methods)
    // ========================================================================

    /**
     * Start work on an order (interface adapter).
     * Delegates to startAssembly().
     */
    @Override
    public AssemblyControlOrderDTO startWork(Long id) {
        return startAssembly(id);
    }

    /**
     * Complete work on an order (interface adapter).
     * Delegates to completeAssemblyProduction() which handles inventory credits.
     */
    @Override
    public AssemblyControlOrderDTO completeWork(Long id) {
        return completeAssemblyProduction(id);
    }

    /**
     * Halt work on an order (interface adapter).
     * Delegates to haltAssembly().
     */
    @Override
    public AssemblyControlOrderDTO haltWork(Long id, String reason) {
        return haltAssembly(id, reason);
    }
}

