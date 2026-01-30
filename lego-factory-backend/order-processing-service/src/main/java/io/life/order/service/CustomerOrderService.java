package io.life.order.service;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.dto.OrderItemDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.repository.CustomerOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
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
public class CustomerOrderService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String ORDER_TYPE_CUSTOMER = "CUSTOMER";
    
    // Trigger scenario constants
    private static final String SCENARIO_DIRECT_FULFILLMENT = "DIRECT_FULFILLMENT";
    private static final String SCENARIO_WAREHOUSE_ORDER_NEEDED = "WAREHOUSE_ORDER_NEEDED";
    private static final String SCENARIO_DIRECT_PRODUCTION = "DIRECT_PRODUCTION";
    
    private final CustomerOrderRepository customerOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final FinalAssemblyOrderService finalAssemblyOrderService;
    private final OrderAuditService orderAuditService;
    private final InventoryService inventoryService;
    private final SystemConfigService systemConfigService;

    public CustomerOrderService(CustomerOrderRepository customerOrderRepository, 
                                WarehouseOrderRepository warehouseOrderRepository,
                                FinalAssemblyOrderService finalAssemblyOrderService,
                                OrderAuditService orderAuditService,
                                InventoryService inventoryService,
                                SystemConfigService systemConfigService) {
        this.customerOrderRepository = customerOrderRepository;
        this.warehouseOrderRepository = warehouseOrderRepository;
        this.finalAssemblyOrderService = finalAssemblyOrderService;
        this.orderAuditService = orderAuditService;
        this.inventoryService = inventoryService;
        this.systemConfigService = systemConfigService;
        // Custom exception for mapping errors is now a static nested class below
    }

    // Custom exception for mapping errors
    public static class OrderMappingException extends RuntimeException {
        public OrderMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Transactional
    public CustomerOrderDTO createOrder(CustomerOrderDTO orderDTO) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(STATUS_PENDING);
        order.setWorkstationId(orderDTO.getWorkstationId());
        order.setNotes(orderDTO.getNotes());

        // Convert DTOs to entities
        List<OrderItem> orderItems = orderDTO.getOrderItems().stream()
            .map(itemDTO -> {
                OrderItem item = new OrderItem();
                item.setItemType(itemDTO.getItemType());
                item.setItemId(itemDTO.getItemId());
                item.setQuantity(itemDTO.getQuantity());
                item.setFulfilledQuantity(itemDTO.getFulfilledQuantity() == null ? 0 : itemDTO.getFulfilledQuantity());
                item.setNotes(itemDTO.getNotes());
                item.setCustomerOrder(order);
                return item;
            })
            .collect(Collectors.toList());

        order.setOrderItems(orderItems);
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        CustomerOrderDTO dto = mapToDTO(savedOrder);
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, dto.getId(), "CREATED", "Customer order created: " + dto.getOrderNumber());
        return dto;
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public Optional<CustomerOrderDTO> getOrderById(Long id) {
        return customerOrderRepository.findById(id)
            .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerOrderDTO> getOrderByNumber(String orderNumber) {
        return customerOrderRepository.findByOrderNumber(orderNumber)
            .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderDTO> getAllOrders() {
        try {
            logger.info("Fetching all customer orders");
            List<CustomerOrder> orders = customerOrderRepository.findAll();
            logger.info("Found {} total customer orders", orders.size());
            return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching all customer orders", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderDTO> getOrdersByWorkstationId(Long workstationId) {
        try {
            logger.info("Fetching orders for workstation: {}", workstationId);
            List<CustomerOrder> orders = customerOrderRepository.findByWorkstationId(workstationId);
            logger.info("Found {} orders for workstation {}", orders.size(), workstationId);
            return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching orders for workstation: {}", workstationId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderDTO> getOrdersByStatus(String status) {
        return customerOrderRepository.findByStatus(status).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public CustomerOrderDTO updateOrderStatus(Long id, String newStatus) {
        @SuppressWarnings("null")
        CustomerOrder order = customerOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        order.setStatus(newStatus);
        CustomerOrder updatedOrder = customerOrderRepository.save(order);

        CustomerOrderDTO dto = mapToDTO(updatedOrder);
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, dto.getId(), "STATUS_" + newStatus, "Order status changed to " + newStatus);
        return dto;
    }

    @SuppressWarnings("null")
    @Transactional
    public void deleteOrder(Long id) {
        customerOrderRepository.deleteById(id);
    }

    // --- Explicit transitions with basic validation ---
    @Transactional
    public CustomerOrderDTO confirmOrder(Long id) {
        CustomerOrder order = getOrThrow(id);
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("Only PENDING orders can be confirmed");
        }
        
        // STOCK CHECK DURING CONFIRMATION: Check PRODUCT stock at Plant Warehouse (WS-7)
        // This determines the scenario path: direct fulfillment, warehouse order, or direct production
        logger.info("=== CUSTOMER ORDER CONFIRMATION: Checking stock for order {} at workstation {} ===", 
            order.getOrderNumber(), order.getWorkstationId());
        
        // Calculate total order quantity for Scenario 4 threshold check
        int totalQuantity = order.getOrderItems().stream()
            .mapToInt(OrderItem::getQuantity)
            .sum();
        
        int lotSizeThreshold = systemConfigService.getLotSizeThreshold();
        logger.info("Order {} total quantity: {}, Scenario 4 threshold: {}", 
            order.getOrderNumber(), totalQuantity, lotSizeThreshold);
        
        boolean hasAllStock = order.getOrderItems().stream()
            .allMatch(item -> {
                boolean stockAvailable = inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
                logger.info("  Item {} (type: {}, qty: {}) - Stock available: {}", 
                    item.getItemId(), item.getItemType(), item.getQuantity(), stockAvailable);
                return stockAvailable;
            });
        
        // Determine trigger scenario based on stock availability AND lot size threshold
        if (hasAllStock) {
            order.setTriggerScenario(SCENARIO_DIRECT_FULFILLMENT);
            logger.info("Customer order {} confirmed - DIRECT_FULFILLMENT (sufficient PRODUCT stock)", order.getOrderNumber());
        } else if (totalQuantity >= lotSizeThreshold) {
            // SCENARIO 4: Large order with insufficient stock → bypass warehouse, go direct to production
            order.setTriggerScenario(SCENARIO_DIRECT_PRODUCTION);
            logger.info("Customer order {} confirmed - DIRECT_PRODUCTION (qty {} >= threshold {}, insufficient stock)", 
                order.getOrderNumber(), totalQuantity, lotSizeThreshold);
        } else {
            order.setTriggerScenario(SCENARIO_WAREHOUSE_ORDER_NEEDED);
            logger.info("Customer order {} confirmed - WAREHOUSE_ORDER_NEEDED (insufficient PRODUCT stock, qty {} < threshold {})", 
                order.getOrderNumber(), totalQuantity, lotSizeThreshold);
        }
        
        order.setStatus(STATUS_CONFIRMED);
        CustomerOrder saved = customerOrderRepository.save(order);
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, saved.getId(), STATUS_CONFIRMED, 
            "Order confirmed - Scenario: " + saved.getTriggerScenario());
        return mapToDTO(saved);
    }

    @Transactional
    public CustomerOrderDTO markProcessing(Long id) {
        CustomerOrder order = getOrThrow(id);
        if (!STATUS_CONFIRMED.equals(order.getStatus()) && !STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("Only PENDING/CONFIRMED orders can be set to PROCESSING");
        }
        order.setStatus("PROCESSING");
        CustomerOrder saved = customerOrderRepository.save(order);
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, saved.getId(), "STATUS_PROCESSING", "Order moved to PROCESSING");
        return mapToDTO(saved);
    }

    @Transactional
    public CustomerOrderDTO completeOrder(Long id) {
        CustomerOrder order = getOrThrow(id);
        if (!"PROCESSING".equals(order.getStatus()) && !STATUS_CONFIRMED.equals(order.getStatus())) {
            throw new IllegalStateException("Only PROCESSING/CONFIRMED orders can be completed");
        }
        order.setStatus(STATUS_COMPLETED);
        CustomerOrder saved = customerOrderRepository.save(order);
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, saved.getId(), STATUS_COMPLETED, "Order completed");
        return mapToDTO(saved);
    }

    /**
     * Check the current triggerScenario for a CONFIRMED order based on real-time stock.
     * This dynamically re-evaluates stock availability, accounting for changes since confirmation.
     * Also considers the Scenario 4 lot size threshold.
     * 
     * @param id Customer order ID
     * @return Current trigger scenario ("DIRECT_FULFILLMENT", "WAREHOUSE_ORDER_NEEDED", or "DIRECT_PRODUCTION")
     */
    @Transactional(readOnly = true)
    public String checkCurrentTriggerScenario(Long id) {
        CustomerOrder order = getOrThrow(id);
        
        // Only check for CONFIRMED orders
        if (!STATUS_CONFIRMED.equals(order.getStatus())) {
            logger.debug("Order {} is not CONFIRMED, returning stored triggerScenario: {}", id, order.getTriggerScenario());
            return order.getTriggerScenario();
        }
        
        // Re-check stock availability in real-time
        logger.info("=== DYNAMIC STOCK CHECK: Re-evaluating stock for order {} ===", order.getOrderNumber());
        
        // Calculate total quantity for Scenario 4 threshold
        int totalQuantity = order.getOrderItems().stream()
            .mapToInt(OrderItem::getQuantity)
            .sum();
        int lotSizeThreshold = systemConfigService.getLotSizeThreshold();
        
        boolean hasAllStock = order.getOrderItems().stream()
            .allMatch(item -> {
                boolean stockAvailable = inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
                logger.info("  Item {} (qty: {}) - Current stock available: {}", 
                    item.getItemId(), item.getQuantity(), stockAvailable);
                return stockAvailable;
            });
        
        // Determine current scenario based on stock and threshold
        String currentScenario;
        if (hasAllStock) {
            currentScenario = SCENARIO_DIRECT_FULFILLMENT;
        } else if (totalQuantity >= lotSizeThreshold) {
            currentScenario = SCENARIO_DIRECT_PRODUCTION;
        } else {
            currentScenario = SCENARIO_WAREHOUSE_ORDER_NEEDED;
        }
        
        // Log if scenario has changed since confirmation
        if (!currentScenario.equals(order.getTriggerScenario())) {
            logger.warn("Order {} scenario changed: {} → {} (stock levels changed)", 
                order.getOrderNumber(), order.getTriggerScenario(), currentScenario);
        }
        
        return currentScenario;
    }

    /**
     * Check if a customer order can be completed.
     * 
     * A customer order can be completed when:
     * 1. The order is in PROCESSING status
     * 2. All associated warehouse orders exist
     * 3. All Final Assembly orders for those warehouse orders are SUBMITTED
     * 
     * @param id Customer order ID
     * @return true if the order can be completed, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean canCompleteOrder(Long id) {
        CustomerOrder order = getOrThrow(id);
        
        // Only PROCESSING orders can potentially be completed
        if (!STATUS_PROCESSING.equals(order.getStatus())) {
            logger.debug("Order {} is not in PROCESSING status, cannot complete", id);
            return false;
        }
        
        // Find all warehouse orders for this customer order
        List<WarehouseOrder> warehouseOrders = warehouseOrderRepository.findByCustomerOrderId(id);
        
        if (warehouseOrders.isEmpty()) {
            logger.debug("No warehouse orders found for customer order {}", id);
            return false;
        }
        
        // Check if all Final Assembly orders for all warehouse orders are SUBMITTED
        for (WarehouseOrder warehouseOrder : warehouseOrders) {
            boolean allSubmitted = finalAssemblyOrderService.areAllOrdersSubmittedForWarehouseOrder(warehouseOrder.getId());
            if (!allSubmitted) {
                logger.debug("Warehouse order {} has unsubmitted Final Assembly orders", warehouseOrder.getId());
                return false;
            }
        }
        
        logger.info("Customer order {} can be completed - all Final Assembly orders are SUBMITTED", id);
        return true;
    }

    @Transactional
    public CustomerOrderDTO cancelOrder(Long id) {
        CustomerOrder order = getOrThrow(id);
        if (STATUS_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("Completed orders cannot be cancelled");
        }
        order.setStatus("CANCELLED");
        CustomerOrder saved = customerOrderRepository.save(order);
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, saved.getId(), "CANCELLED", "Order cancelled");
        return mapToDTO(saved);
    }

    @SuppressWarnings("null")
    private CustomerOrder getOrThrow(Long id) {
        return customerOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    private CustomerOrderDTO mapToDTO(CustomerOrder order) {
        try {
            CustomerOrderDTO dto = new CustomerOrderDTO();
            dto.setId(order.getId());
            dto.setOrderNumber(order.getOrderNumber());
            dto.setOrderDate(order.getOrderDate());
            dto.setStatus(order.getStatus());
            dto.setTriggerScenario(order.getTriggerScenario());
            dto.setWorkstationId(order.getWorkstationId());
            dto.setNotes(order.getNotes());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setUpdatedAt(order.getUpdatedAt());

            if (order.getOrderItems() != null) {
                dto.setOrderItems(order.getOrderItems().stream()
                    .map(this::mapItemToDTO)
                    .collect(Collectors.toList()));
            }

            return dto;
        } catch (Exception e) {
            logger.error("Error mapping order to DTO: {}", order.getId(), e);
            logger.error("OrderMappingException: Error mapping order to DTO: {}", e.getMessage(), e);
            throw new OrderMappingException("Error mapping order to DTO: " + e.getMessage(), e);
        }
    }

    private OrderItemDTO mapItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setItemType(item.getItemType());
        dto.setItemId(item.getItemId());
        dto.setQuantity(item.getQuantity());
        dto.setFulfilledQuantity(item.getFulfilledQuantity());
        dto.setNotes(item.getNotes());
        return dto;
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
