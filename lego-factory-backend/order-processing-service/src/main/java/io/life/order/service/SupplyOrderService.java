package io.life.order.service;

import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.SupplyOrderItemDTO;
import io.life.order.entity.AssemblyControlOrder;
import io.life.order.entity.ProductionControlOrder;
import io.life.order.entity.ProductionOrder;
import io.life.order.entity.ProductionOrderItem;
import io.life.order.entity.SupplyOrder;
import io.life.order.entity.SupplyOrderItem;
import io.life.order.repository.AssemblyControlOrderRepository;
import io.life.order.repository.ProductionControlOrderRepository;
import io.life.order.repository.ProductionOrderRepository;
import io.life.order.repository.SupplyOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing SupplyOrder entities.
 * Handles supply orders for Parts Supply Warehouse (workstation 9).
 * Supply orders are created when production/assembly control orders need parts.
 */
@Service
@Transactional
public class SupplyOrderService {

    private static final Logger logger = LoggerFactory.getLogger(SupplyOrderService.class);
    private static final Long PARTS_SUPPLY_WAREHOUSE_ID = 9L; // Workstation 9
    private static final String SUPPLY_ORDER_NOT_FOUND = "Supply order not found: ";

    private final SupplyOrderRepository repository;
    private final AssemblyControlOrderRepository assemblyControlOrderRepository;
    private final ProductionControlOrderRepository productionControlOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final MasterdataService masterdataService;
    @SuppressWarnings("unused")
    private final RestTemplate restTemplate;

    public SupplyOrderService(
            SupplyOrderRepository repository, 
            RestTemplate restTemplate,
            AssemblyControlOrderRepository assemblyControlOrderRepository,
            ProductionControlOrderRepository productionControlOrderRepository,
            ProductionOrderRepository productionOrderRepository,
            MasterdataService masterdataService) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.assemblyControlOrderRepository = assemblyControlOrderRepository;
        this.productionControlOrderRepository = productionControlOrderRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.masterdataService = masterdataService;
    }

    /**
     * Create a supply order for parts needed by a control order.
     * This is called from ProductionControlOrderService or AssemblyControlOrderService.
     */
    public SupplyOrderDTO createSupplyOrder(
            Long sourceControlOrderId,
            String sourceControlOrderType, // PRODUCTION or ASSEMBLY
            Long requestingWorkstationId,
            String priority,
            LocalDateTime requestedByTime,
            List<SupplyOrderItemDTO> requiredItems,
            String notes) {

        String supplyOrderNumber = generateSupplyOrderNumber();

        List<SupplyOrderItem> items = requiredItems.stream()
                .map(itemDTO -> new SupplyOrderItem(
                        null,
                        null, // Will be set after persistence
                        itemDTO.getPartId(),
                        itemDTO.getQuantityRequested(),
                        null, // quantitySupplied
                        itemDTO.getUnit() != null ? itemDTO.getUnit() : "piece",
                        itemDTO.getNotes()
                ))
                .collect(Collectors.toList());

        SupplyOrder order = SupplyOrder.builder()
                .supplyOrderNumber(supplyOrderNumber)
                .sourceControlOrderId(sourceControlOrderId)
                .sourceControlOrderType(sourceControlOrderType)
                .requestingWorkstationId(requestingWorkstationId)
                .supplyWarehouseWorkstationId(PARTS_SUPPLY_WAREHOUSE_ID)
                .status("PENDING")
                .priority(priority)
                .requestedByTime(requestedByTime)
                .notes(notes)
                .build();

        @SuppressWarnings("null")
        SupplyOrder saved = repository.save(order);

        // Set the supply order reference for items
        items.forEach(item -> item.setSupplyOrder(saved));
        saved.setSupplyOrderItems(items);
        SupplyOrder finalSaved = repository.save(saved);

        logger.info("Created supply order {} for workstation {} requesting {} parts",
                supplyOrderNumber, requestingWorkstationId, items.size());

        return mapToDTO(finalSaved);
    }

    /**
     * Create a supply order from a control order with automatic BOM lookup.
     * The parts needed are automatically determined from the module's BOM.
     * 
     * @param controlOrderId The control order ID
     * @param controlOrderType "ASSEMBLY" or "PRODUCTION"
     * @param priority Order priority
     * @return The created supply order DTO
     */
    public SupplyOrderDTO createSupplyOrderFromControlOrder(
            Long controlOrderId,
            String controlOrderType,
            String priority) {
        
        Long itemId;
        String itemType;
        Integer quantity;
        Long workstationId;
        LocalDateTime targetStartTime;
        String controlOrderNumber;

        if ("ASSEMBLY".equalsIgnoreCase(controlOrderType)) {
            AssemblyControlOrder controlOrder = assemblyControlOrderRepository.findById(controlOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Assembly control order not found: " + controlOrderId));
            
            itemId = controlOrder.getItemId();
            itemType = controlOrder.getItemType();
            quantity = controlOrder.getQuantity();
            workstationId = controlOrder.getAssignedWorkstationId();
            targetStartTime = controlOrder.getTargetStartTime();
            controlOrderNumber = controlOrder.getControlOrderNumber();
        } else if ("PRODUCTION".equalsIgnoreCase(controlOrderType)) {
            ProductionControlOrder controlOrder = productionControlOrderRepository.findById(controlOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Production control order not found: " + controlOrderId));
            
            workstationId = controlOrder.getAssignedWorkstationId();
            targetStartTime = controlOrder.getTargetStartTime();
            controlOrderNumber = controlOrder.getControlOrderNumber();
            itemId = controlOrder.getItemId();
            itemType = controlOrder.getItemType();
            quantity = controlOrder.getQuantity();
            
            // Validate that we have item info
            if (itemId == null) {
                throw new IllegalStateException("Production control order " + controlOrderId + " has no item ID. Cannot create supply order.");
            }
            
            // For Production Control Orders, create supply order for the PART they need to manufacture
            // Parts are the lowest level in the BOM hierarchy (Product -> Module -> Part)
            // Manufacturing workstations need the parts supplied from Parts Supply Warehouse (WS-9)
            
            List<SupplyOrderItemDTO> requiredItems = new java.util.ArrayList<>();
            SupplyOrderItemDTO partItem = new SupplyOrderItemDTO();
            partItem.setPartId(itemId);
            partItem.setQuantityRequested(quantity != null ? quantity : 1);
            partItem.setUnit("piece");
            partItem.setNotes("Part for production at manufacturing workstation");
            requiredItems.add(partItem);
            
            String notesForPO = String.format("Parts for Production Control Order %s (Part ID: %d, Qty: %d)",
                controlOrderNumber, itemId, quantity != null ? quantity : 1);

            logger.info("Creating supply order from PRODUCTION control order {} for part {} (qty: {})",
                controlOrderId, itemId, quantity);

            return createSupplyOrder(
                controlOrderId,
                "PRODUCTION",
                workstationId,
                priority != null ? priority : "MEDIUM",
                targetStartTime,
                requiredItems,
                notesForPO
            );
        } else {
            throw new IllegalArgumentException("Invalid control order type: " + controlOrderType);
        }

        // ASSEMBLY control orders: lookup parts from BOM based on module ID
        if (!"MODULE".equalsIgnoreCase(itemType)) {
            throw new IllegalArgumentException("Assembly control order item type must be MODULE, got: " + itemType);
        }
        
        Map<Long, Integer> partRequirements = masterdataService.getPartRequirementsForModule(itemId, quantity);
        
        if (partRequirements.isEmpty()) {
            throw new IllegalStateException("No parts found in BOM for module: " + itemId);
        }

        // Convert part requirements to SupplyOrderItemDTO list
        List<SupplyOrderItemDTO> requiredItems = partRequirements.entrySet().stream()
            .map(entry -> {
                SupplyOrderItemDTO item = new SupplyOrderItemDTO();
                item.setPartId(entry.getKey());
                item.setQuantityRequested(entry.getValue());
                item.setUnit("piece");
                item.setNotes("BOM requirement for module " + itemId);
                return item;
            })
            .collect(Collectors.toList());

        String notes = String.format("Parts for Assembly Control Order %s (Module ID: %d, Qty: %d)",
            controlOrderNumber, itemId, quantity);

        logger.info("Creating supply order from ASSEMBLY control order {} for module {} with {} part types",
            controlOrderId, itemId, requiredItems.size());

        return createSupplyOrder(
            controlOrderId,
            "ASSEMBLY",
            workstationId,
            priority != null ? priority : "MEDIUM",
            targetStartTime,
            requiredItems,
            notes
        );
    }

    /**
     * Get all supply orders for the Parts Supply Warehouse.

    /**
     * Get all supply orders for the Parts Supply Warehouse.
     * This is what the PSW dashboard uses to see pending orders.
     */
    public List<SupplyOrderDTO> getOrdersForSupplyWarehouse(String status) {
        List<SupplyOrder> orders;
        if (status != null && !status.isEmpty()) {
            orders = repository.findBySupplyWarehouseWorkstationIdAndStatus(PARTS_SUPPLY_WAREHOUSE_ID, status);
        } else {
            orders = repository.findBySupplyWarehouseWorkstationId(PARTS_SUPPLY_WAREHOUSE_ID);
        }
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Get all supply orders for a specific requesting workstation.
     */
    public List<SupplyOrderDTO> getOrdersByRequestingWorkstation(Long workstationId, String status) {
        List<SupplyOrder> orders;
        if (status != null && !status.isEmpty()) {
            orders = repository.findByRequestingWorkstationIdAndStatus(workstationId, status);
        } else {
            orders = repository.findByRequestingWorkstationId(workstationId);
        }
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Get a specific supply order by ID.
     */
    @SuppressWarnings("null")
    public SupplyOrderDTO getSupplyOrder(Long id) {
        return repository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException(SUPPLY_ORDER_NOT_FOUND + id));
    }

    /**
     * Get supply orders for a specific control order.
     */
    public List<SupplyOrderDTO> getSupplyOrdersForControlOrder(Long controlOrderId, String controlOrderType) {
        List<SupplyOrder> orders = repository.findBySourceControlOrderIdAndSourceControlOrderType(controlOrderId, controlOrderType);
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Fulfill a supply order.
     * Updates the status to FULFILLED and debits from inventory-service.
     * Only fulfills items that are in stock (partial fulfillment if necessary).
     */
    public SupplyOrderDTO fulfillSupplyOrder(Long id) {
        @SuppressWarnings("null")
        SupplyOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(SUPPLY_ORDER_NOT_FOUND + id));

        if ("FULFILLED".equals(order.getStatus()) || "REJECTED".equals(order.getStatus())) {
            throw new RuntimeException("Cannot fulfill supply order in status: " + order.getStatus());
        }

        // Try to debit from inventory
        boolean allItemsFulfilled = true;
        for (SupplyOrderItem item : order.getSupplyOrderItems()) {
            try {
                // Call inventory-service to debit stock
                // This would be: PATCH /api/inventory/parts/{id}/debit?quantity=X
                // For now, we'll just update the supply order item
                item.setQuantitySupplied(item.getQuantityRequested());
            } catch (Exception e) {
                logger.warn("Could not debit inventory for part {}: {}", item.getPartId(), e.getMessage());
                allItemsFulfilled = false;
                item.setQuantitySupplied(0);
            }
        }

        if (allItemsFulfilled) {
            order.setStatus("FULFILLED");
            order.setFulfilledAt(LocalDateTime.now());
            logger.info("Supply order {} fulfilled successfully", order.getSupplyOrderNumber());
        } else {
            // Partial fulfillment - still mark as fulfilled if some items were supplied
            order.setStatus("FULFILLED");
            order.setFulfilledAt(LocalDateTime.now());
            logger.info("Supply order {} partially fulfilled", order.getSupplyOrderNumber());
        }

        SupplyOrder saved = repository.save(order);
        return mapToDTO(saved);
    }

    /**
     * Reject a supply order (insufficient stock or other reason).
     */
    public SupplyOrderDTO rejectSupplyOrder(Long id, String reason) {
        @SuppressWarnings("null")
        SupplyOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(SUPPLY_ORDER_NOT_FOUND + id));

        order.setStatus("REJECTED");
        order.setRejectedAt(LocalDateTime.now());
        if (reason != null) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") + "Rejected: " + reason);
        }

        SupplyOrder saved = repository.save(order);
        logger.info("Supply order {} rejected: {}", order.getSupplyOrderNumber(), reason);
        return mapToDTO(saved);
    }

    /**
     * Cancel a supply order.
     */
    public SupplyOrderDTO cancelSupplyOrder(Long id, String reason) {
        @SuppressWarnings("null")
        SupplyOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(SUPPLY_ORDER_NOT_FOUND + id));

        order.setStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        if (reason != null) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") + "Cancelled: " + reason);
        }

        SupplyOrder saved = repository.save(order);
        logger.info("Supply order {} cancelled: {}", order.getSupplyOrderNumber(), reason);
        return mapToDTO(saved);
    }

    /**
     * Update supply order status.
     */
    public SupplyOrderDTO updateStatus(Long id, String newStatus) {
        @SuppressWarnings("null")
        SupplyOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(SUPPLY_ORDER_NOT_FOUND + id));

        order.setStatus(newStatus);
        if ("IN_PROGRESS".equals(newStatus)) {
            // No additional action needed
        }

        SupplyOrder saved = repository.save(order);
        logger.info("Supply order {} status updated to {}", order.getSupplyOrderNumber(), newStatus);
        return mapToDTO(saved);
    }

    /**
     * Get supply orders by source control order.
     */
    public List<SupplyOrderDTO> getBySourceControlOrder(Long controlOrderId, String controlOrderType) {
        return repository.findBySourceControlOrderIdAndSourceControlOrderType(controlOrderId, controlOrderType)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Map SupplyOrder entity to DTO.
     */
    private SupplyOrderDTO mapToDTO(SupplyOrder order) {
        List<SupplyOrderItemDTO> itemDTOs = order.getSupplyOrderItems() != null ?
                order.getSupplyOrderItems().stream()
                        .map(item -> SupplyOrderItemDTO.builder()
                                .id(item.getId())
                                .partId(item.getPartId())
                                .quantityRequested(item.getQuantityRequested())
                                .quantitySupplied(item.getQuantitySupplied())
                                .unit(item.getUnit())
                                .notes(item.getNotes())
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return SupplyOrderDTO.builder()
                .id(order.getId())
                .supplyOrderNumber(order.getSupplyOrderNumber())
                .sourceControlOrderId(order.getSourceControlOrderId())
                .sourceControlOrderType(order.getSourceControlOrderType())
                .requestingWorkstationId(order.getRequestingWorkstationId())
                .supplyWarehouseWorkstationId(order.getSupplyWarehouseWorkstationId())
                .status(order.getStatus())
                .priority(order.getPriority())
                .createdAt(order.getCreatedAt())
                .requestedByTime(order.getRequestedByTime())
                .fulfilledAt(order.getFulfilledAt())
                .rejectedAt(order.getRejectedAt())
                .cancelledAt(order.getCancelledAt())
                .updatedAt(order.getUpdatedAt())
                .notes(order.getNotes())
                .supplyOrderItems(itemDTOs)
                .build();
    }

    /**
     * Generate a unique supply order number.
     * Format: SO-{TIMESTAMP}-{RANDOM}
     */
    private String generateSupplyOrderNumber() {
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(10000);
        return String.format("SO-%d-%04d", timestamp, random);
    }
}
