package io.life.order.service;

import io.life.order.dto.FinalAssemblyOrderDTO;
import io.life.order.entity.FinalAssemblyOrder;
import io.life.order.entity.WarehouseOrder;
import io.life.order.repository.FinalAssemblyOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinalAssemblyOrderService {

    private static final Logger logger = LoggerFactory.getLogger(FinalAssemblyOrderService.class);
    private static final Long FINAL_ASSEMBLY_WORKSTATION_ID = 6L;
    private static final Long PLANT_WAREHOUSE_WORKSTATION_ID = 7L;
    private static final String FINAL_ASSEMBLY_AUDIT_SOURCE = "FINAL_ASSEMBLY";

    private final FinalAssemblyOrderRepository finalAssemblyOrderRepository;
    private final InventoryService inventoryService;
    private final OrderAuditService orderAuditService;

    public FinalAssemblyOrderService(FinalAssemblyOrderRepository finalAssemblyOrderRepository,
                                    InventoryService inventoryService,
                                    OrderAuditService orderAuditService) {
        this.finalAssemblyOrderRepository = finalAssemblyOrderRepository;
        this.inventoryService = inventoryService;
        this.orderAuditService = orderAuditService;
    }

    /**
     * Create Final Assembly order from Warehouse Order (Scenario 2)
     * Called when WarehouseOrder is fulfilled at Modules Supermarket
     */
    public FinalAssemblyOrderDTO createFromWarehouseOrder(WarehouseOrder warehouseOrder, Long productId, Integer quantity) {
        logger.info("Creating Final Assembly order from warehouse order {} for product {} qty {}", 
                warehouseOrder.getOrderNumber(), productId, quantity);

        FinalAssemblyOrder order = new FinalAssemblyOrder();
        order.setOrderNumber("FA-" + generateOrderNumber());
        order.setWarehouseOrderId(warehouseOrder.getId());
        order.setWorkstationId(FINAL_ASSEMBLY_WORKSTATION_ID);
        order.setOutputProductId(productId);
        order.setOutputQuantity(quantity);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setNotes("Auto-created from warehouse order " + warehouseOrder.getOrderNumber());

        FinalAssemblyOrder saved = finalAssemblyOrderRepository.save(order);
        logger.info("✓ Final Assembly order {} created", saved.getOrderNumber());
        
        orderAuditService.recordOrderEvent(FINAL_ASSEMBLY_AUDIT_SOURCE, saved.getId(), "CREATED",
                "Final Assembly order created from warehouse order " + warehouseOrder.getOrderNumber());

        return mapToDTO(saved);
    }

    /**
     * Get all Final Assembly orders
     */
    public List<FinalAssemblyOrderDTO> getAllOrders() {
        List<FinalAssemblyOrder> orders = finalAssemblyOrderRepository.findAll();
        logger.info("Fetching all Final Assembly orders - found {} orders", orders.size());
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get Final Assembly order by ID
     */
    @SuppressWarnings("null")
    public Optional<FinalAssemblyOrderDTO> getOrderById(Long id) {
        return finalAssemblyOrderRepository.findById(id)
                .map(this::mapToDTO);
    }

    /**
     * Get Final Assembly orders by workstation (should always be WS-6)
     */
    public List<FinalAssemblyOrderDTO> getOrdersByWorkstationId(Long workstationId) {
        List<FinalAssemblyOrder> orders = finalAssemblyOrderRepository.findByWorkstationId(workstationId);
        logger.info("Fetching Final Assembly orders for workstation {} - found {} orders", workstationId, orders.size());
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get Final Assembly orders by status
     */
    public List<FinalAssemblyOrderDTO> getOrdersByStatus(String status) {
        return finalAssemblyOrderRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get Final Assembly orders by warehouse order ID
     */
    public List<FinalAssemblyOrderDTO> getOrdersByWarehouseOrderId(Long warehouseOrderId) {
        return finalAssemblyOrderRepository.findByWarehouseOrderId(warehouseOrderId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Start Final Assembly order
     * Changes status from PENDING to IN_PROGRESS
     */
    public FinalAssemblyOrderDTO startOrder(Long orderId) {
        @SuppressWarnings("null")
        Optional<FinalAssemblyOrder> orderOpt = finalAssemblyOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Final Assembly order not found: " + orderId);
        }

        FinalAssemblyOrder order = orderOpt.get();
        
        // Validate current status
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Only PENDING orders can be started. Current status: " + order.getStatus());
        }

        // Update status to IN_PROGRESS
        order.setStatus("IN_PROGRESS");
        order.setStartTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        finalAssemblyOrderRepository.save(order);

        logger.info("Final Assembly order {} started", order.getOrderNumber());
        orderAuditService.recordOrderEvent(FINAL_ASSEMBLY_AUDIT_SOURCE, order.getId(), "STARTED",
                "Final Assembly order started");

        return mapToDTO(order);
    }

    /**
     * Complete Final Assembly order (Scenario 2 critical endpoint)
     * 
     * Actions:
     * 1. Validate order status (must be IN_PROGRESS)
     * 2. Credit Plant Warehouse (WS-7) with finished products
     * 3. Update order status to COMPLETED
     * 
     * Note: Module stock was already debited from Modules Supermarket during warehouse order fulfillment
     */
    public FinalAssemblyOrderDTO completeOrder(Long orderId) {
        @SuppressWarnings("null")
        Optional<FinalAssemblyOrder> orderOpt = finalAssemblyOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Final Assembly order not found: " + orderId);
        }

        FinalAssemblyOrder order = orderOpt.get();
        
        // Validate current status
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Only IN_PROGRESS orders can be completed. Current status: " + order.getStatus());
        }

        logger.info("Completing Final Assembly order {} - crediting Plant Warehouse with product {} qty {}", 
                order.getOrderNumber(), order.getOutputProductId(), order.getOutputQuantity());

        // Credit Plant Warehouse (WS-7) with finished products
        boolean credited = inventoryService.creditStock(
                PLANT_WAREHOUSE_WORKSTATION_ID,
                order.getOutputProductId(),
                order.getOutputQuantity()
        );

        if (!credited) {
            throw new IllegalStateException("Failed to credit Plant Warehouse with assembled products");
        }

        logger.info("✓ Plant Warehouse credited with product {} qty {}", 
                order.getOutputProductId(), order.getOutputQuantity());

        // Update order status
        order.setStatus("COMPLETED");
        order.setCompletionTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        finalAssemblyOrderRepository.save(order);

        logger.info("✓ Final Assembly order {} completed", order.getOrderNumber());
        orderAuditService.recordOrderEvent(FINAL_ASSEMBLY_AUDIT_SOURCE, order.getId(), "COMPLETED",
                String.format("Final Assembly order completed - Plant Warehouse credited with product %d qty %d",
                        order.getOutputProductId(), order.getOutputQuantity()));

        return mapToDTO(order);
    }

    /**
     * Update Final Assembly order status
     */
    public FinalAssemblyOrderDTO updateOrderStatus(Long orderId, String status) {
        @SuppressWarnings("null")
        Optional<FinalAssemblyOrder> orderOpt = finalAssemblyOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Final Assembly order not found: " + orderId);
        }

        FinalAssemblyOrder order = orderOpt.get();
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        logger.info("Updated Final Assembly order {} status to {}", order.getOrderNumber(), status);

        return mapToDTO(finalAssemblyOrderRepository.save(order));
    }

    /**
     * Map FinalAssemblyOrder entity to DTO
     */
    private FinalAssemblyOrderDTO mapToDTO(FinalAssemblyOrder order) {
        FinalAssemblyOrderDTO dto = new FinalAssemblyOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setWarehouseOrderId(order.getWarehouseOrderId());
        dto.setAssemblyControlOrderId(order.getAssemblyControlOrderId());
        dto.setWorkstationId(order.getWorkstationId());
        dto.setOutputProductId(order.getOutputProductId());
        dto.setOutputQuantity(order.getOutputQuantity());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setStartTime(order.getStartTime());
        dto.setCompletionTime(order.getCompletionTime());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    private String generateOrderNumber() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
