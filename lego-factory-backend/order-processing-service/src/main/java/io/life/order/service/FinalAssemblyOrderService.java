package io.life.order.service;

import io.life.order.config.OrderProcessingConfig;
import io.life.order.dto.FinalAssemblyOrderDTO;
import io.life.order.entity.FinalAssemblyOrder;
import io.life.order.entity.ProductionOrder;
import io.life.order.entity.ProductionOrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.CustomerOrder;
import io.life.order.repository.FinalAssemblyOrderRepository;
import io.life.order.repository.ProductionOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
import io.life.order.repository.CustomerOrderRepository;
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
    private static final String FINAL_ASSEMBLY_AUDIT_SOURCE = "FINAL_ASSEMBLY";

    private final OrderProcessingConfig config;
    private final FinalAssemblyOrderRepository finalAssemblyOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryService inventoryService;
    private final OrderAuditService orderAuditService;

    public FinalAssemblyOrderService(OrderProcessingConfig config,
                                    FinalAssemblyOrderRepository finalAssemblyOrderRepository,
                                    ProductionOrderRepository productionOrderRepository,
                                    WarehouseOrderRepository warehouseOrderRepository,
                                    CustomerOrderRepository customerOrderRepository,
                                    InventoryService inventoryService,
                                    OrderAuditService orderAuditService) {
        this.config = config;
        this.finalAssemblyOrderRepository = finalAssemblyOrderRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.customerOrderRepository = customerOrderRepository;
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
        order.setOrderNumber(config.getOrderNumbers().getFinalAssemblyOrderPrefix() + generateOrderNumber());
        order.setWarehouseOrderId(warehouseOrder.getId());
        order.setWorkstationId(config.getWorkstations().getFinalAssembly());
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
     * Create Final Assembly order from Production Order (Scenario 4 - Direct Production)
     * Called when ProductionOrder completes without a WarehouseOrder (high volume bypass)
     * 
     * @param productionOrder The completed production order
     * @param productId The target product ID
     * @param quantity The quantity to assemble
     * @return Created Final Assembly order DTO
     */
    public FinalAssemblyOrderDTO createFromProductionOrder(ProductionOrder productionOrder, Long productId, Integer quantity) {
        logger.info("Creating Final Assembly order from production order {} for product {} qty {}", 
                productionOrder.getProductionOrderNumber(), productId, quantity);

        FinalAssemblyOrder order = new FinalAssemblyOrder();
        order.setOrderNumber(config.getOrderNumbers().getFinalAssemblyOrderPrefix() + generateOrderNumber());
        order.setProductionOrderId(productionOrder.getId());
        order.setWorkstationId(config.getWorkstations().getFinalAssembly());
        order.setOutputProductId(productId);
        order.setOutputQuantity(quantity);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setNotes("Auto-created from production order " + productionOrder.getProductionOrderNumber() + " (Scenario 4 - Direct Production)");

        FinalAssemblyOrder saved = finalAssemblyOrderRepository.save(order);
        logger.info("Final Assembly order {} created for direct production", saved.getOrderNumber());
        
        orderAuditService.recordOrderEvent(FINAL_ASSEMBLY_AUDIT_SOURCE, saved.getId(), "CREATED",
                "Final Assembly order created from production order " + productionOrder.getProductionOrderNumber());

        return mapToDTO(saved);
    }
    
    /**
     * Create Final Assembly orders from Production Order using Customer Order products.
     * This overload fetches the production order and customer order, extracts products,
     * and creates Final Assembly orders for each product.
     * 
     * @param productionOrderId The completed production order ID
     * @param customerOrderId The source customer order ID
     * @throws RuntimeException if orders not found or have no items
     */
    public void createFromProductionOrder(Long productionOrderId, Long customerOrderId) {
        logger.info("Creating Final Assembly orders from production order {} for customer order {}", 
                productionOrderId, customerOrderId);
        
        // For now, create a simple placeholder implementation
        // A full implementation would:
        // 1. Fetch production order
        // 2. Fetch customer order to get product IDs
        // 3. For each product, create Final Assembly order
        
        logger.warn("createFromProductionOrder(Long, Long) is a placeholder - needs full implementation");
        throw new UnsupportedOperationException("This method requires full implementation with customer order lookup");
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
     * Confirm Final Assembly order (Step 1 of 4-step workflow)
     * Changes status from PENDING to CONFIRMED
     */
    public FinalAssemblyOrderDTO confirmOrder(Long orderId) {
        @SuppressWarnings("null")
        Optional<FinalAssemblyOrder> orderOpt = finalAssemblyOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Final Assembly order not found: " + orderId);
        }

        FinalAssemblyOrder order = orderOpt.get();
        
        // Validate current status
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Only PENDING orders can be confirmed. Current status: " + order.getStatus());
        }

        // Update status to CONFIRMED
        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        finalAssemblyOrderRepository.save(order);

        logger.info("Final Assembly order {} confirmed", order.getOrderNumber());
        orderAuditService.recordOrderEvent(FINAL_ASSEMBLY_AUDIT_SOURCE, order.getId(), "CONFIRMED",
                "Final Assembly order confirmed and ready to start");

        return mapToDTO(order);
    }

    /**
     * Start Final Assembly order (Step 2 of 4-step workflow)
     * Changes status from CONFIRMED to IN_PROGRESS
     */
    public FinalAssemblyOrderDTO startOrder(Long orderId) {
        @SuppressWarnings("null")
        Optional<FinalAssemblyOrder> orderOpt = finalAssemblyOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Final Assembly order not found: " + orderId);
        }

        FinalAssemblyOrder order = orderOpt.get();
        
        // Validate current status - must be CONFIRMED to start
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new IllegalStateException("Only CONFIRMED orders can be started. Current status: " + order.getStatus());
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
     * Complete Final Assembly order (Step 3 of 4-step workflow)
     * 
     * Actions:
     * 1. Validate order status (must be IN_PROGRESS)
     * 2. Update order status to COMPLETED
     * 3. Record completion time
     * 
     * Note: Inventory credit happens on SUBMIT, not COMPLETE
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

        logger.info("Completing Final Assembly order {} - product {} qty {}", 
                order.getOrderNumber(), order.getOutputProductId(), order.getOutputQuantity());

        // Update order status - inventory credit happens on submit
        order.setStatus("COMPLETED");
        order.setCompletionTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        finalAssemblyOrderRepository.save(order);

        logger.info("✓ Final Assembly order {} completed - awaiting submit", order.getOrderNumber());
        orderAuditService.recordOrderEvent(FINAL_ASSEMBLY_AUDIT_SOURCE, order.getId(), "COMPLETED",
                String.format("Final Assembly order completed - product %d qty %d ready for submission",
                        order.getOutputProductId(), order.getOutputQuantity()));

        return mapToDTO(order);
    }

    /**
     * Submit Final Assembly order (Step 4 of 4-step workflow)
     * 
     * Actions:
     * 1. Validate order status (must be COMPLETED)
     * 2. Credit Plant Warehouse (WS-7) with finished products
     * 3. Update order status to SUBMITTED
     * 4. Check if all Final Assembly orders for parent Warehouse Order are submitted
     * 5. If all submitted, enable customer order completion
     */
    public FinalAssemblyOrderDTO submitOrder(Long orderId) {
        @SuppressWarnings("null")
        Optional<FinalAssemblyOrder> orderOpt = finalAssemblyOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Final Assembly order not found: " + orderId);
        }

        FinalAssemblyOrder order = orderOpt.get();
        
        // Validate current status
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new IllegalStateException("Only COMPLETED orders can be submitted. Current status: " + order.getStatus());
        }

        logger.info("Submitting Final Assembly order {} - crediting Plant Warehouse with product {} qty {}", 
                order.getOrderNumber(), order.getOutputProductId(), order.getOutputQuantity());

        // Credit Plant Warehouse (WS-7) with finished products
        boolean credited = inventoryService.creditStock(
                config.getWorkstations().getPlantWarehouse(),
                order.getOutputProductId(),
                order.getOutputQuantity()
        );

        if (!credited) {
            throw new IllegalStateException("Failed to credit Plant Warehouse with assembled products");
        }

        logger.info("✓ Plant Warehouse credited with product {} qty {}", 
                order.getOutputProductId(), order.getOutputQuantity());

        // Update order status to SUBMITTED
        order.setStatus("SUBMITTED");
        order.setSubmitTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        finalAssemblyOrderRepository.save(order);

        logger.info("✓ Final Assembly order {} submitted", order.getOrderNumber());
        orderAuditService.recordOrderEvent(FINAL_ASSEMBLY_AUDIT_SOURCE, order.getId(), "SUBMITTED",
                String.format("Final Assembly order submitted - Plant Warehouse credited with product %d qty %d",
                        order.getOutputProductId(), order.getOutputQuantity()));

        // SCENARIO 2/3: Check if all Final Assembly orders for this Warehouse Order are submitted
        if (order.getWarehouseOrderId() != null) {
            checkAndUpdateWarehouseOrderStatus(order.getWarehouseOrderId());
        }
        // SCENARIO 4: Check if all Final Assembly orders for this Production Order are submitted (direct production)
        else if (order.getProductionOrderId() != null) {
            checkAndUpdateProductionOrderStatus(order.getProductionOrderId());
        }

        return mapToDTO(order);
    }

    /**
     * Check if all Final Assembly orders for a Warehouse Order are submitted
     * If all submitted, update the customer order status to enable completion
     */
    private void checkAndUpdateWarehouseOrderStatus(Long warehouseOrderId) {
        List<FinalAssemblyOrder> relatedOrders = finalAssemblyOrderRepository.findByWarehouseOrderId(warehouseOrderId);
        
        boolean allSubmitted = relatedOrders.stream()
                .allMatch(o -> "SUBMITTED".equals(o.getStatus()));
        
        if (allSubmitted) {
            logger.info("✓ All Final Assembly orders for warehouse order {} are submitted - updating customer order status", 
                    warehouseOrderId);
            orderAuditService.recordOrderEvent("WAREHOUSE_ORDER", warehouseOrderId, "ASSEMBLY_COMPLETE",
                    "All Final Assembly orders submitted - customer order can be completed");
            
            // Get warehouse order and update customer order status
            Optional<WarehouseOrder> warehouseOrderOpt = warehouseOrderRepository.findById(warehouseOrderId);
            if (warehouseOrderOpt.isPresent()) {
                WarehouseOrder warehouseOrder = warehouseOrderOpt.get();
                Long customerOrderId = warehouseOrder.getCustomerOrderId();
                
                if (customerOrderId != null) {
                    // Update customer order to show it's ready for completion
                    Optional<CustomerOrder> customerOrderOpt = customerOrderRepository.findById(customerOrderId);
                    if (customerOrderOpt.isPresent()) {
                        CustomerOrder customerOrder = customerOrderOpt.get();
                        
                        // Change status from PROCESSING to CONFIRMED to enable complete button
                        if ("PROCESSING".equals(customerOrder.getStatus())) {
                            customerOrder.setStatus("CONFIRMED");
                            customerOrder.setTriggerScenario("DIRECT_FULFILLMENT");
                            customerOrder.setUpdatedAt(LocalDateTime.now());
                            customerOrderRepository.save(customerOrder);
                            
                            logger.info("✓ Customer order {} status updated to CONFIRMED (DIRECT_FULFILLMENT) - ready for completion", 
                                    customerOrder.getOrderNumber());
                            orderAuditService.recordOrderEvent("CUSTOMER_ORDER", customerOrderId, "ASSEMBLY_COMPLETE",
                                    "All products assembled - order ready for completion");
                        }
                    }
                }
            }
        } else {
            long submitted = relatedOrders.stream().filter(o -> "SUBMITTED".equals(o.getStatus())).count();
            logger.info("Warehouse order {} has {}/{} Final Assembly orders submitted", 
                    warehouseOrderId, submitted, relatedOrders.size());
        }
    }

    /**
     * Check if all Final Assembly orders for a Production Order are submitted (Scenario 4)
     * If all submitted, update the customer order status to enable completion
     */
    private void checkAndUpdateProductionOrderStatus(Long productionOrderId) {
        List<FinalAssemblyOrder> relatedOrders = finalAssemblyOrderRepository.findByProductionOrderId(productionOrderId);
        
        boolean allSubmitted = relatedOrders.stream()
                .allMatch(o -> "SUBMITTED".equals(o.getStatus()));
        
        if (allSubmitted) {
            logger.info("✓ All Final Assembly orders for production order {} are submitted - updating customer order status", 
                    productionOrderId);
            orderAuditService.recordOrderEvent("PRODUCTION_ORDER", productionOrderId, "ASSEMBLY_COMPLETE",
                    "All Final Assembly orders submitted - customer order can be completed");
            
            // Get production order and update customer order status
            Optional<ProductionOrder> productionOrderOpt = productionOrderRepository.findById(productionOrderId);
            if (productionOrderOpt.isPresent()) {
                ProductionOrder productionOrder = productionOrderOpt.get();
                Long customerOrderId = productionOrder.getSourceCustomerOrderId();
                
                if (customerOrderId != null) {
                    // Update customer order to show it's ready for completion
                    Optional<CustomerOrder> customerOrderOpt = customerOrderRepository.findById(customerOrderId);
                    if (customerOrderOpt.isPresent()) {
                        CustomerOrder customerOrder = customerOrderOpt.get();
                        
                        // Change status from PROCESSING to CONFIRMED to enable complete button
                        if ("PROCESSING".equals(customerOrder.getStatus())) {
                            customerOrder.setStatus("CONFIRMED");
                            customerOrder.setTriggerScenario("DIRECT_FULFILLMENT");
                            customerOrder.setUpdatedAt(LocalDateTime.now());
                            customerOrderRepository.save(customerOrder);
                            
                            logger.info("✓ Customer order {} status updated to CONFIRMED (DIRECT_FULFILLMENT) - ready for completion [Scenario 4]", 
                                    customerOrder.getOrderNumber());
                            orderAuditService.recordOrderEvent("CUSTOMER_ORDER", customerOrderId, "ASSEMBLY_COMPLETE",
                                    "All products assembled - order ready for completion (Scenario 4)");
                        }
                    }
                }
            }
        } else {
            long submitted = relatedOrders.stream().filter(o -> "SUBMITTED".equals(o.getStatus())).count();
            logger.info("Production order {} has {}/{} Final Assembly orders submitted", 
                    productionOrderId, submitted, relatedOrders.size());
        }
    }

    /**
     * Check if all Final Assembly orders for a Warehouse Order are submitted
     * Used by frontend to enable/disable customer order complete button
     */
    public boolean areAllOrdersSubmittedForWarehouseOrder(Long warehouseOrderId) {
        List<FinalAssemblyOrder> relatedOrders = finalAssemblyOrderRepository.findByWarehouseOrderId(warehouseOrderId);
        
        if (relatedOrders.isEmpty()) {
            return false;
        }
        
        return relatedOrders.stream()
                .allMatch(o -> "SUBMITTED".equals(o.getStatus()));
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
        dto.setSubmitTime(order.getSubmitTime());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    private String generateOrderNumber() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
