package io.life.order.service;

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

    private final ProductionOrderRepository productionOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;

    public ProductionOrderService(ProductionOrderRepository productionOrderRepository,
                                 WarehouseOrderRepository warehouseOrderRepository) {
        this.productionOrderRepository = productionOrderRepository;
        this.warehouseOrderRepository = warehouseOrderRepository;
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
                customerOrderId = warehouseOrder.getSourceCustomerOrderId();
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
        if (warehouseOrder != null && warehouseOrder.getWarehouseOrderItems() != null) {
            List<ProductionOrderItem> productionOrderItems = new ArrayList<>();
            for (WarehouseOrderItem woItem : warehouseOrder.getWarehouseOrderItems()) {
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
     */
    public ProductionOrderDTO completeProductionOrder(Long id) {
        @SuppressWarnings("null")
        ProductionOrder productionOrder = productionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTION_ORDER_NOT_FOUND + id));

        productionOrder.setStatus("COMPLETED");
        productionOrder.setActualCompletionTime(LocalDateTime.now());

        ProductionOrder updated = productionOrderRepository.save(productionOrder);
        logger.info("Marked production order {} as completed", id);

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
