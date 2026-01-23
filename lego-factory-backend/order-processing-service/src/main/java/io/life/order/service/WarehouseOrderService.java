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
    private static final Long FINAL_ASSEMBLY_WORKSTATION_ID = 6L;
    private static final String WAREHOUSE_AUDIT_SOURCE = "WAREHOUSE";

    private final WarehouseOrderRepository warehouseOrderRepository;
    private final InventoryService inventoryService;
    private final ProductionOrderService productionOrderService;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderAuditService orderAuditService;
    private final AssemblyControlOrderService assemblyControlOrderService;

    public WarehouseOrderService(WarehouseOrderRepository warehouseOrderRepository,
                                 InventoryService inventoryService,
                                 ProductionOrderService productionOrderService,
                                 CustomerOrderRepository customerOrderRepository,
                                 OrderAuditService orderAuditService,
                                 AssemblyControlOrderService assemblyControlOrderService) {
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.inventoryService = inventoryService;
        this.productionOrderService = productionOrderService;
        this.customerOrderRepository = customerOrderRepository;
        this.orderAuditService = orderAuditService;
        this.assemblyControlOrderService = assemblyControlOrderService;
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
     * Get warehouse orders by workstation ID
     * Used to retrieve orders for Modules Supermarket (WS-8)
     */
    public List<WarehouseOrderDTO> getWarehouseOrdersByWorkstationId(Long workstationId) {
        List<WarehouseOrder> orders = warehouseOrderRepository.findByWorkstationId(workstationId);
        logger.info("Fetching warehouse orders for workstation {} - found {} orders", workstationId, orders.size());
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get warehouse orders by customer order ID
     * Used to find warehouse orders spawned from a specific customer order
     */
    public List<WarehouseOrderDTO> getWarehouseOrdersByCustomerOrderId(Long customerOrderId) {
        return warehouseOrderRepository.findByCustomerOrderId(customerOrderId).stream()
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
     * Confirm a warehouse order (Scenario 2 Critical Logic)
     * DURING confirmation, checks MODULE stock at Modules Supermarket and sets triggerScenario:
     * - DIRECT_FULFILLMENT: All modules available → can fulfill immediately
     * - PRODUCTION_REQUIRED: Insufficient modules → need to trigger production (Scenario 3)
     * Status changes from PENDING to CONFIRMED
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

        // CRITICAL: Check MODULE stock at Modules Supermarket (WS-8) DURING confirmation
        boolean allModulesAvailable = order.getOrderItems().stream()
                .allMatch(item -> inventoryService.checkStock(
                        order.getWorkstationId(), // Should be 8L for Modules Supermarket
                        item.getItemId(),
                        item.getRequestedQuantity()
                ));

        // Set triggerScenario based on stock check result
        if (allModulesAvailable) {
            order.setTriggerScenario("DIRECT_FULFILLMENT");
            logger.info("Warehouse order {} confirmed - All modules available (DIRECT_FULFILLMENT)", order.getOrderNumber());
        } else {
            order.setTriggerScenario("PRODUCTION_REQUIRED");
            logger.info("Warehouse order {} confirmed - Modules unavailable (PRODUCTION_REQUIRED)", order.getOrderNumber());
        }

        // Update status to CONFIRMED
        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        warehouseOrderRepository.save(order);

        logger.info("Warehouse order {} confirmed - triggerScenario: {}", order.getOrderNumber(), order.getTriggerScenario());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "CONFIRMED",
                "Warehouse order confirmed - triggerScenario: " + order.getTriggerScenario());

        return mapToDTO(order);
    }

    /**
     * Fulfill a warehouse order (Scenario 2 - DIRECT_FULFILLMENT path)
     * 
     * Prerequisites:
     * - Order must be CONFIRMED with triggerScenario = "DIRECT_FULFILLMENT"
     * - Modules must be available at Modules Supermarket (checked during confirmation)
     * 
     * Actions:
     * 1. Debit module stock from Modules Supermarket (WS-8)
     * 2. Create Final Assembly order at WS-6
     * 3. Update status to PROCESSING (awaiting assembly completion)
     * 
     * Note: Final Assembly completion will credit Plant Warehouse (WS-7) with finished products
     */
    public WarehouseOrderDTO fulfillWarehouseOrder(Long warehouseOrderId) {
        @SuppressWarnings("null")
        Optional<WarehouseOrder> orderOpt = warehouseOrderRepository.findById(warehouseOrderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Warehouse order not found: " + warehouseOrderId);
        }

        WarehouseOrder order = orderOpt.get();
        
        // Validate order status
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new IllegalStateException("Only CONFIRMED warehouse orders can be fulfilled. Current status: " + order.getStatus());
        }

        // Validate triggerScenario
        if (!"DIRECT_FULFILLMENT".equals(order.getTriggerScenario())) {
            throw new IllegalStateException("Only DIRECT_FULFILLMENT orders can be fulfilled directly. Current triggerScenario: " + order.getTriggerScenario());
        }

        logger.info("Fulfilling warehouse order {} - Scenario 2 DIRECT_FULFILLMENT path", order.getOrderNumber());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLMENT_STARTED",
            "Warehouse order fulfillment started: " + order.getOrderNumber());

        // Debit module stock from Modules Supermarket
        boolean allItemsDebited = true;
        for (WarehouseOrderItem item : order.getOrderItems()) {
            try {
                boolean debited = inventoryService.updateStock(
                        order.getWorkstationId(), // WS-8 Modules Supermarket
                        item.getItemId(),
                        item.getRequestedQuantity()
                );

                if (debited) {
                    item.setFulfilledQuantity(item.getRequestedQuantity());
                    logger.info("✓ Module {} qty {} debited from Modules Supermarket", item.getItemId(), item.getRequestedQuantity());
                } else {
                    allItemsDebited = false;
                    logger.error("✗ Failed to debit module {} from inventory", item.getItemId());
                }
            } catch (Exception e) {
                allItemsDebited = false;
                logger.error("✗ Error debiting module {}: {}", item.getItemId(), e.getMessage());
            }
        }

        if (!allItemsDebited) {
            throw new IllegalStateException("Failed to debit all modules from inventory. Warehouse order cannot be fulfilled.");
        }

        // Create Final Assembly orders (WS-6)
        createFinalAssemblyOrdersFromWarehouseOrder(order);

        // Update status to PROCESSING (awaiting Final Assembly completion)
        order.setStatus("PROCESSING");
        order.setUpdatedAt(LocalDateTime.now());
        warehouseOrderRepository.save(order);

        logger.info("✓ Warehouse order {} fulfilled - Modules debited, Final Assembly orders created", order.getOrderNumber());
        orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLED",
                "Modules debited from Modules Supermarket, Final Assembly orders created - awaiting assembly completion");

        return mapToDTO(order);
    }

    /**
     * Create AssemblyControlOrders for Final Assembly station (WS-6).
     * Scenario 2 workflow - Assembly station will credit Plant Warehouse (WS-7) when complete.
     * 
     * Each fulfilled item in the warehouse order becomes a separate assembly order.
     * The Final Assembly workstation will complete these orders, which will:
     * 1. Update SimAL schedule status
     * 2. Credit Plant Warehouse with finished products  
     * 3. Allow customer order to be fulfilled at Plant Warehouse
     */
    private void createFinalAssemblyOrdersFromWarehouseOrder(WarehouseOrder order) {
        try {
            logger.info("Creating Final Assembly orders for warehouse order {}", order.getOrderNumber());
            
            for (WarehouseOrderItem item : order.getOrderItems()) {
                if (item.getFulfilledQuantity() > 0) {
                    try {
                        Long productId = item.getItemId();
                        Integer quantity = item.getFulfilledQuantity();
                        
                        // Calculate target times (30 minutes prep time, 1 hour assembly time)
                        LocalDateTime targetStart = LocalDateTime.now().plusMinutes(30);
                        LocalDateTime targetCompletion = targetStart.plusHours(1);
                        
                        // Calculate estimated duration in minutes
                        Integer estimatedDuration = 60; // 1 hour for final assembly
                        
                        // Create assembly control order using the existing service method
                        assemblyControlOrderService.createControlOrder(
                                order.getCustomerOrderId(), // Use customer order ID as source
                                FINAL_ASSEMBLY_WORKSTATION_ID,    // Assign to Final Assembly (WS-6)
                                "WO-" + order.getOrderNumber(), // Use warehouse order as schedule ID
                                "MEDIUM",                          // Priority
                                targetStart,
                                targetCompletion,
                                String.format("Assemble %s (Product #%d) - Qty: %d from warehouse order %s", 
                                        item.getItemName(), productId, quantity, order.getOrderNumber()),
                                "Check product quality and specifications",
                                "Perform final product testing",
                                "Package for Plant Warehouse delivery",
                                estimatedDuration
                        );
                        
                        logger.info("✓ Final Assembly order created for Product #{} ({}) qty {}", 
                                productId, item.getItemName(), quantity);
                        
                        orderAuditService.recordOrderEvent("FINAL_ASSEMBLY", order.getId(), "ASSEMBLY_ORDER_CREATED",
                                String.format("Final Assembly order created for Product #%d (%s) qty %d from warehouse order %s",
                                        productId, item.getItemName(), quantity, order.getOrderNumber()));
                    } catch (Exception e) {
                        logger.error("✗ Failed to create assembly order for item {}: {}", item.getItemId(), e.getMessage());
                        // Continue with other items even if one fails
                    }
                }
            }
            
            logger.info("✓ All Final Assembly orders created for warehouse order {}", order.getOrderNumber());
            
        } catch (Exception e) {
            logger.error("✗ Failed to create Final Assembly orders: {}", e.getMessage());
        }
    }
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
