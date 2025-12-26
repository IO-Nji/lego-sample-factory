package io.life.order.service;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.dto.OrderItemDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
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
public class CustomerOrderService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String ORDER_TYPE_CUSTOMER = "CUSTOMER";
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderAuditService orderAuditService;

    public CustomerOrderService(CustomerOrderRepository customerOrderRepository, OrderAuditService orderAuditService) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderAuditService = orderAuditService;
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
        order.setStatus(STATUS_CONFIRMED);
        CustomerOrder saved = customerOrderRepository.save(order);
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, saved.getId(), STATUS_CONFIRMED, "Order confirmed");
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
