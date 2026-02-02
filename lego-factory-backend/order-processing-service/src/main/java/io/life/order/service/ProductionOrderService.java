package io.life.order.service;

import io.life.order.dto.ProductionControlOrderDTO;
import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.dto.masterdata.BomEntryDTO;
import io.life.order.dto.masterdata.ModuleDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.entity.ProductionOrder;
import io.life.order.entity.ProductionOrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.repository.CustomerOrderRepository;
import io.life.order.repository.ProductionOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
import io.life.order.client.MasterdataClient;
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
 * Production orders are created when WarehouseOrders cannot be fulfilled (Scenario 3)
 * or when large customer orders bypass warehouse (Scenario 4).
 */
@Service
@Transactional
public class ProductionOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionOrderService.class);
    private static final String PRODUCTION_ORDER_NOT_FOUND = "Production order not found: ";

    private static final Long MODULES_SUPERMARKET_WORKSTATION_ID = 8L;

    private final ProductionOrderRepository productionOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ProductionControlOrderService productionControlOrderService;
    private final AssemblyControlOrderService assemblyControlOrderService;
    private final InventoryService inventoryService;
    private final MasterdataClient masterdataClient;

    public ProductionOrderService(ProductionOrderRepository productionOrderRepository,
                                 WarehouseOrderRepository warehouseOrderRepository,
                                 CustomerOrderRepository customerOrderRepository,
                                 ProductionControlOrderService productionControlOrderService,
                                 AssemblyControlOrderService assemblyControlOrderService,
                                 InventoryService inventoryService,
                                 MasterdataClient masterdataClient) {
        this.productionOrderRepository = productionOrderRepository;
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.productionControlOrderService = productionControlOrderService;
        this.assemblyControlOrderService = assemblyControlOrderService;
        this.inventoryService = inventoryService;
        this.masterdataClient = masterdataClient;
    }

    /**
     * Create a production order directly from a CustomerOrder (Scenario 4).
     * Used when large orders bypass the warehouse and go directly to production.
     * This is triggered when order quantity >= LOT_SIZE_THRESHOLD.
     * 
     * @param customerOrderId The source customer order ID
     * @param priority Priority level (LOW, NORMAL, HIGH, URGENT)
     * @param dueDate When the order should be completed
     * @param notes Additional notes
     * @param createdByWorkstationId Workstation ID of the operator creating the order
     * @return Created ProductionOrderDTO
     */
    public ProductionOrderDTO createFromCustomerOrder(
            Long customerOrderId,
            String priority,
            LocalDateTime dueDate,
            String notes,
            Long createdByWorkstationId) {

        CustomerOrder customerOrder = customerOrderRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Customer order not found: " + customerOrderId));

        logger.info("Creating Scenario 4 production order from customer order {} (triggerScenario: {})", 
                customerOrder.getOrderNumber(), customerOrder.getTriggerScenario());

        String productionOrderNumber = generateProductionOrderNumber();

        ProductionOrder productionOrder = ProductionOrder.builder()
                .productionOrderNumber(productionOrderNumber)
                .sourceCustomerOrderId(customerOrderId)
                .sourceWarehouseOrderId(null)  // Scenario 4: No warehouse order
                .status("CREATED")
                .priority(priority != null ? priority : "NORMAL")
                .dueDate(dueDate != null ? dueDate : LocalDateTime.now().plusDays(1))
                .triggerScenario("SCENARIO_4")
                .createdByWorkstationId(createdByWorkstationId)
                .assignedWorkstationId(6L)  // Default to Final Assembly (WS-6)
                .notes(notes != null ? notes : "Scenario 4: Direct production from customer order (high volume)")
                .build();

        // Save production order FIRST to get the ID for item relationships
        ProductionOrder saved = productionOrderRepository.save(productionOrder);
        logger.info("Created production order {} with ID {}", productionOrderNumber, saved.getId());

        // Create production order items from customer order items
        // Convert PRODUCTS to MODULES for production scheduling (same as Scenario 3)
        List<ProductionOrderItem> productionOrderItems = new ArrayList<>();
        if (customerOrder.getOrderItems() != null) {
            for (OrderItem coItem : customerOrder.getOrderItems()) {
                // Customer orders contain PRODUCT items
                // Must convert to MODULES for production planning
                Long productId = coItem.getItemId();
                Integer productQuantity = coItem.getQuantity();
                
                logger.info("Converting product {} (qty {}) to modules for Scenario 4 production", 
                        productId, productQuantity);
                
                // Get product modules via masterdata service (BOM lookup)
                List<BomEntryDTO> productModuleBom = masterdataClient.getModulesForProduct(productId);
                
                if (productModuleBom == null || productModuleBom.isEmpty()) {
                    logger.error("No modules found for product {} - cannot create production order", productId);
                    continue;
                }
                
                logger.info("Product {} requires {} modules", productId, productModuleBom.size());
                
                // For each module in the product BOM, create a production order item
                for (BomEntryDTO bomEntry : productModuleBom) {
                    Long moduleId = bomEntry.getComponentId();
                    Integer moduleQuantityPerProduct = bomEntry.getQuantity() != null ? 
                            bomEntry.getQuantity() : 1;
                    Integer totalModuleQuantity = moduleQuantityPerProduct * productQuantity;
                    
                    // Fetch full module details to get production workstation
                    Optional<ModuleDTO> moduleOpt = masterdataClient.getModuleById(moduleId);
                    if (moduleOpt.isEmpty()) {
                        logger.error("Module {} not found - skipping", moduleId);
                        continue;
                    }
                    
                    ModuleDTO module = moduleOpt.get();
                    Integer productionWorkstationId = module.getProductionWorkstationId();
                    
                    if (productionWorkstationId == null) {
                        logger.error("Module {} has no productionWorkstationId - skipping", moduleId);
                        continue;
                    }
                    
                    // Determine workstation type based on production workstation ID
                    // WS-1, WS-2, WS-3 = MANUFACTURING (produces parts/modules)
                    // WS-4, WS-5 = ASSEMBLY (assembles modules from parts)
                    String workstationType;
                    if (productionWorkstationId >= 1 && productionWorkstationId <= 3) {
                        workstationType = "MANUFACTURING";
                    } else if (productionWorkstationId >= 4 && productionWorkstationId <= 5) {
                        workstationType = "ASSEMBLY";
                    } else {
                        logger.error("Invalid productionWorkstationId {} for module {} - skipping", 
                                productionWorkstationId, moduleId);
                        continue;
                    }
                    
                    ProductionOrderItem poItem = ProductionOrderItem.builder()
                            .productionOrder(saved)  // Use SAVED production order with ID
                            .itemType("MODULE")  // Production orders always contain MODULES
                            .itemId(module.getId())
                            .itemName(module.getName())
                            .quantity(totalModuleQuantity)
                            .estimatedTimeMinutes(30)  // Default estimate per module
                            .workstationType(workstationType)
                            .build();
                    productionOrderItems.add(poItem);
                    
                    logger.info("  Added production order item: {} (MODULE ID: {}) qty {} - workstation type: {} (WS-{})", 
                            module.getName(), module.getId(), totalModuleQuantity, 
                            workstationType, productionWorkstationId);
                }
            }
        }
        saved.setProductionOrderItems(productionOrderItems);

        // Save again to persist items with CASCADE
        ProductionOrder finalSaved = productionOrderRepository.save(saved);
        logger.info("Created Scenario 4 production order {} from customer order {} with {} items", 
                productionOrderNumber, customerOrder.getOrderNumber(), productionOrderItems.size());

        // Update customer order status to PROCESSING
        customerOrder.setStatus("PROCESSING");
        customerOrder.setNotes((customerOrder.getNotes() != null ? customerOrder.getNotes() + " | " : "") 
                + "Scenario 4: Production order " + productionOrderNumber + " created");
        customerOrderRepository.save(customerOrder);
        logger.info("Updated customer order {} status to PROCESSING", customerOrder.getOrderNumber());

        return mapToDTO(finalSaved);
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

        // Save production order FIRST to get the ID for item relationships
        ProductionOrder saved = productionOrderRepository.save(productionOrder);
        logger.info("Created production order {} with ID {}", productionOrderNumber, saved.getId());

        // Create production order items from warehouse order items
        // WarehouseOrders contain MODULES that need to be produced
        // Each module has a productionWorkstationId indicating where it's produced
        List<ProductionOrderItem> productionOrderItems = new ArrayList<>();
        if (warehouseOrder != null && warehouseOrder.getOrderItems() != null) {
            for (WarehouseOrderItem woItem : warehouseOrder.getOrderItems()) {
                // WarehouseOrderItems contain MODULES
                if (!"MODULE".equals(woItem.getItemType())) {
                    logger.warn("WarehouseOrderItem has unexpected type: {} - should be MODULE", 
                            woItem.getItemType());
                    continue;
                }
                
                // Fetch module details to get production workstation
                Long moduleId = woItem.getItemId();
                Optional<ModuleDTO> moduleOpt = masterdataClient.getModuleById(moduleId);
                
                if (moduleOpt.isEmpty()) {
                    logger.error("Module not found: {} - skipping", moduleId);
                    continue;
                }
                
                ModuleDTO module = moduleOpt.get();
                Integer productionWorkstationId = module.getProductionWorkstationId();
                
                if (productionWorkstationId == null) {
                    logger.error("Module {} has no productionWorkstationId - skipping", moduleId);
                    continue;
                }
                
                // Determine workstation type based on production workstation ID
                // WS-1, WS-2, WS-3 = MANUFACTURING (produces parts/modules)
                // WS-4, WS-5 = ASSEMBLY (assembles modules from parts)
                String workstationType;
                if (productionWorkstationId >= 1 && productionWorkstationId <= 3) {
                    workstationType = "MANUFACTURING";
                } else if (productionWorkstationId >= 4 && productionWorkstationId <= 5) {
                    workstationType = "ASSEMBLY";
                } else {
                    logger.error("Invalid productionWorkstationId {} for module {} - skipping", 
                            productionWorkstationId, moduleId);
                    continue;
                }
                
                ProductionOrderItem poItem = ProductionOrderItem.builder()
                        .productionOrder(saved)  // Use SAVED production order with ID
                        .itemType("MODULE") // Production orders contain MODULES
                        .itemId(moduleId)
                        .itemName(module.getName())
                        .quantity(woItem.getRequestedQuantity())
                        .estimatedTimeMinutes(30) // Default estimate
                        .workstationType(workstationType)
                        .build();
                productionOrderItems.add(poItem);
                
                logger.info("Added production order item: {} (MODULE ID: {}) qty {} - workstation type: {} (WS-{})", 
                        module.getName(), moduleId, woItem.getRequestedQuantity(), 
                        workstationType, productionWorkstationId);
            }
        }
        saved.setProductionOrderItems(productionOrderItems);

        // Save again to persist items with CASCADE
        @SuppressWarnings("null")
        ProductionOrder finalSaved = productionOrderRepository.save(saved);
        logger.info("Created production order {} from warehouse order {} with {} items", 
                productionOrderNumber, sourceWarehouseOrderId, 
                finalSaved.getProductionOrderItems() != null ? finalSaved.getProductionOrderItems().size() : 0);

        // Update warehouse order status to AWAITING_PRODUCTION and link to this production order
        // This prevents other production runs from interfering with this order's fulfillment
        if (warehouseOrder != null) {
            warehouseOrder.setStatus("AWAITING_PRODUCTION");
            warehouseOrder.setTriggerScenario("PRODUCTION_CREATED");
            warehouseOrder.setProductionOrderId(finalSaved.getId()); // Link to production order
            warehouseOrderRepository.save(warehouseOrder);
            logger.info("Updated warehouse order {} status to AWAITING_PRODUCTION with production order {} linked", 
                    sourceWarehouseOrderId, finalSaved.getId());
        }

        return mapToDTO(finalSaved);
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

        // ✅ IMPLEMENTED (Feb 2026): OrderOrchestrationService automatically checks all control orders
        // and completes production order when all control orders are done.
        // See: OrderOrchestrationService.notifyControlOrderComplete()
        
        if ("DISPATCHED".equals(productionOrder.getStatus())) {
            productionOrder.setStatus("IN_PROGRESS");
            logger.info("Production order {} transitioned to IN_PROGRESS", id);
        }

        // Note: Production auto-completion is handled by OrderOrchestrationService
        // which monitors all control orders and triggers submitProductionOrderCompletion()
        // when everything is complete.

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
