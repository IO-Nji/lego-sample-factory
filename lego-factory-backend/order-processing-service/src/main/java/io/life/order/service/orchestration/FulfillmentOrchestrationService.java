package io.life.order.service.orchestration;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.exception.EntityNotFoundException;
import io.life.order.exception.InvalidOrderStateException;
import io.life.order.repository.CustomerOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
import io.life.order.service.InventoryService;
import io.life.order.service.OrderAuditService;
import io.life.order.service.domain.BomConversionService;
import io.life.order.service.validation.OrderValidator;
import io.life.order.service.validation.StockValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FulfillmentOrchestrationService - Coordinates order fulfillment workflow
 * 
 * This service implements the Orchestrator pattern for order fulfillment,
 * delegating to specialized domain services for specific operations.
 * 
 * SCENARIOS:
 * - Scenario 1: Direct Fulfillment (all items available at Plant Warehouse)
 * - Scenario 2: Warehouse Order (items not available, create WarehouseOrder to WS-8)
 * - Scenario 3: Partial Fulfillment (some items available, others need warehouse)
 * - Scenario 4: Production Planning (high-volume orders bypass warehouse)
 * 
 * EXTRACTED FROM: FulfillmentService.java (574 lines → ~200 lines orchestration)
 * 
 * @see BomConversionService - BOM lookups and product-to-module conversion
 * @see OrderValidator - Order validation logic
 * @see StockValidator - Stock availability checks
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FulfillmentOrchestrationService {

    private static final Long MODULES_SUPERMARKET_WORKSTATION_ID = 8L;
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String ORDER_TYPE_CUSTOMER = "CUSTOMER";

    private final CustomerOrderRepository customerOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final InventoryService inventoryService;
    private final BomConversionService bomConversionService;
    private final OrderValidator orderValidator;
    private final StockValidator stockValidator;
    @Lazy
    private final OrderAuditService orderAuditService;

    /**
     * Main entry point for order fulfillment.
     * Determines the appropriate scenario and delegates execution.
     * 
     * @param orderId The customer order ID to fulfill
     * @return CustomerOrderDTO with updated status
     * @throws EntityNotFoundException if order not found
     * @throws InvalidOrderStateException if order cannot be fulfilled
     */
    @Transactional
    public CustomerOrderDTO fulfillOrder(Long orderId) {
        log.info("Starting fulfillment orchestration for order ID: {}", orderId);

        // Load and validate order
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Customer order not found")
                        .addDetail("orderId", orderId));

        // Validate order state
        orderValidator.validateForFulfillment(order);

        // Record fulfillment start
        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, order.getId(), 
                "FULFILLMENT_STARTED", "Fulfillment started for order " + order.getOrderNumber());

        // Determine scenario based on current stock
        FulfillmentScenario scenario = determineScenario(order);
        log.info("Order {} - Scenario determined: {}", order.getOrderNumber(), scenario);

        // Execute appropriate scenario
        return switch (scenario) {
            case DIRECT_FULFILLMENT -> executeDirectFulfillment(order);
            case WAREHOUSE_ORDER -> executeWarehouseOrder(order);
            case PARTIAL_FULFILLMENT -> executePartialFulfillment(order);
            case PRODUCTION_PLANNING -> executeProductionPlanning(order);
        };
    }

    /**
     * Determine which fulfillment scenario applies based on stock availability.
     */
    private FulfillmentScenario determineScenario(CustomerOrder order) {
        List<OrderItem> items = order.getOrderItems();
        Long workstationId = order.getWorkstationId();

        boolean allAvailable = stockValidator.checkAllItemsAvailable(workstationId, items);
        if (allAvailable) {
            return FulfillmentScenario.DIRECT_FULFILLMENT;
        }

        boolean anyAvailable = stockValidator.checkAnyItemsAvailable(workstationId, items);
        if (anyAvailable) {
            return FulfillmentScenario.PARTIAL_FULFILLMENT;
        }

        return FulfillmentScenario.WAREHOUSE_ORDER;
    }

    /**
     * Scenario 1: Direct Fulfillment
     * All items available at the order's workstation - deduct and complete.
     */
    private CustomerOrderDTO executeDirectFulfillment(CustomerOrder order) {
        log.info("Scenario 1: Direct Fulfillment for order {}", order.getOrderNumber());

        boolean allUpdatesSuccessful = order.getOrderItems().stream()
                .allMatch(item -> {
                    boolean ok = inventoryService.updateStock(
                            order.getWorkstationId(), 
                            item.getItemId(), 
                            item.getQuantity());
                    if (ok) {
                        item.setFulfilledQuantity(item.getQuantity());
                    }
                    return ok;
                });

        if (allUpdatesSuccessful) {
            order.setStatus(STATUS_COMPLETED);
            log.info("Order {} fulfilled directly. Inventory updated.", order.getOrderNumber());
            orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, order.getId(), 
                    "COMPLETED", "Order fulfilled directly (Scenario 1)");

            // Update other confirmed orders at this workstation
            updateOtherConfirmedOrdersScenario(order.getWorkstationId(), order.getId());
        } else {
            order.setStatus(STATUS_CANCELLED);
            log.warn("Order {} fulfillment failed during inventory update.", order.getOrderNumber());
            orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, order.getId(), 
                    "CANCELLED", "Inventory update failed during direct fulfillment");
        }

        return mapToDTO(customerOrderRepository.save(order));
    }

    /**
     * Scenario 2: Warehouse Order
     * Create WarehouseOrder with BOM-converted modules for Modules Supermarket.
     */
    private CustomerOrderDTO executeWarehouseOrder(CustomerOrder order) {
        log.info("Scenario 2: Warehouse Order for order {}", order.getOrderNumber());

        // Convert products to modules using BOM
        BomConversionService.BomConversionResult bomResult = 
                bomConversionService.convertProductsToModules(order.getOrderItems());

        // Validate BOM conversion result
        orderValidator.validateBomResult(bomResult, order.getOrderNumber());

        // Create warehouse order
        WarehouseOrder warehouseOrder = createWarehouseOrder(order, bomResult, "SCENARIO_2");

        // Save warehouse order
        warehouseOrderRepository.save(warehouseOrder);
        log.info("Created warehouse order {} with {} items for customer order {}",
                warehouseOrder.getOrderNumber(), 
                warehouseOrder.getOrderItems().size(), 
                order.getOrderNumber());

        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, order.getId(), 
                "WAREHOUSE_ORDER_CREATED",
                "Warehouse order " + warehouseOrder.getOrderNumber() + " created (Scenario 2)");

        // Update customer order status
        order.setStatus(STATUS_PROCESSING);
        order.setNotes(appendNote(order.getNotes(), 
                "Scenario 2: Warehouse order " + warehouseOrder.getOrderNumber() + 
                " created (awaiting Modules Supermarket confirmation)"));

        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, order.getId(), 
                "STATUS_PROCESSING", "Scenario 2 processing (waiting on warehouse)");

        return mapToDTO(customerOrderRepository.save(order));
    }

    /**
     * Scenario 3: Partial Fulfillment
     * Fulfill available items locally, create warehouse order for unavailable.
     */
    private CustomerOrderDTO executePartialFulfillment(CustomerOrder order) {
        log.info("Scenario 3: Partial Fulfillment for order {}", order.getOrderNumber());

        List<OrderItem> availableItems = order.getOrderItems().stream()
                .filter(item -> inventoryService.checkStock(
                        order.getWorkstationId(), item.getItemId(), item.getQuantity()))
                .toList();

        List<OrderItem> unavailableItems = order.getOrderItems().stream()
                .filter(item -> !inventoryService.checkStock(
                        order.getWorkstationId(), item.getItemId(), item.getQuantity()))
                .toList();

        // Fulfill available items
        for (OrderItem item : availableItems) {
            if (inventoryService.updateStock(order.getWorkstationId(), item.getItemId(), item.getQuantity())) {
                int currentFulfilled = item.getFulfilledQuantity() != null ? item.getFulfilledQuantity() : 0;
                item.setFulfilledQuantity(currentFulfilled + item.getQuantity());
                log.info("  - Item {} fulfilled from local stock", item.getItemId());
            }
        }

        // Create warehouse order for unavailable items
        WarehouseOrder warehouseOrder = null;
        if (!unavailableItems.isEmpty()) {
            BomConversionService.BomConversionResult bomResult = 
                    bomConversionService.convertProductsToModules(unavailableItems);

            if (!bomResult.items().isEmpty()) {
                warehouseOrder = createWarehouseOrder(order, bomResult, "SCENARIO_3");
                warehouseOrderRepository.save(warehouseOrder);
                log.info("Created warehouse order {} for partial fulfillment", 
                        warehouseOrder.getOrderNumber());
                orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, order.getId(), 
                        "WAREHOUSE_ORDER_CREATED",
                        "Warehouse order " + warehouseOrder.getOrderNumber() + " created (Scenario 3)");
            }
        }

        // Update order status
        order.setStatus(STATUS_PROCESSING);
        String notes = "Scenario 3: Partial fulfillment from local + Modules Supermarket";
        if (warehouseOrder != null) {
            notes += " (warehouse order: " + warehouseOrder.getOrderNumber() + ")";
        }
        order.setNotes(appendNote(order.getNotes(), notes));

        orderAuditService.recordOrderEvent(ORDER_TYPE_CUSTOMER, order.getId(), 
                "STATUS_PROCESSING", "Scenario 3 processing (partial local fulfillment)");

        return mapToDTO(customerOrderRepository.save(order));
    }

    /**
     * Scenario 4: Production Planning
     * For high-volume or custom orders that require production.
     */
    private CustomerOrderDTO executeProductionPlanning(CustomerOrder order) {
        log.info("Scenario 4: Production Planning for order {}", order.getOrderNumber());

        // Fulfill any available items
        for (OrderItem item : order.getOrderItems()) {
            if (inventoryService.checkStock(order.getWorkstationId(), item.getItemId(), item.getQuantity())) {
                inventoryService.updateStock(order.getWorkstationId(), item.getItemId(), item.getQuantity());
            }
        }

        order.setStatus(STATUS_PROCESSING);
        order.setNotes(appendNote(order.getNotes(), 
                "Scenario 4: Routed to Production Planning for custom items"));

        return mapToDTO(customerOrderRepository.save(order));
    }

    /**
     * Create a WarehouseOrder from BOM conversion result.
     */
    private WarehouseOrder createWarehouseOrder(CustomerOrder sourceOrder, 
                                                 BomConversionService.BomConversionResult bomResult,
                                                 String triggerScenario) {
        WarehouseOrder warehouseOrder = new WarehouseOrder();
        warehouseOrder.setOrderNumber("WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        warehouseOrder.setCustomerOrderId(sourceOrder.getId());
        warehouseOrder.setWorkstationId(MODULES_SUPERMARKET_WORKSTATION_ID);
        warehouseOrder.setOrderDate(LocalDateTime.now());
        warehouseOrder.setStatus("PENDING");
        warehouseOrder.setTriggerScenario(triggerScenario);
        warehouseOrder.setNotes("Auto-generated from customer order " + sourceOrder.getOrderNumber());

        // Convert BOM items to WarehouseOrderItems
        List<WarehouseOrderItem> warehouseItems = bomResult.items().stream()
                .map(bomItem -> {
                    WarehouseOrderItem woItem = new WarehouseOrderItem();
                    woItem.setWarehouseOrder(warehouseOrder);
                    woItem.setItemId(bomItem.moduleId());
                    woItem.setProductId(bomItem.sourceProductId());
                    woItem.setItemName(bomItem.moduleName());
                    woItem.setRequestedQuantity(bomItem.quantity());
                    woItem.setFulfilledQuantity(0);
                    woItem.setItemType("MODULE");
                    woItem.setNotes("For product: " + bomItem.sourceProductName() + 
                            " (ID: " + bomItem.sourceProductId() + ")");
                    return woItem;
                })
                .toList();

        warehouseOrder.setOrderItems(new java.util.ArrayList<>(warehouseItems));
        return warehouseOrder;
    }

    /**
     * Update triggerScenario for other CONFIRMED orders at the same workstation.
     */
    private void updateOtherConfirmedOrdersScenario(Long workstationId, Long excludeOrderId) {
        log.info("Updating triggerScenario for other CONFIRMED orders at workstation {}", workstationId);

        List<CustomerOrder> confirmedOrders = customerOrderRepository.findAll().stream()
                .filter(o -> "CONFIRMED".equals(o.getStatus()))
                .filter(o -> workstationId.equals(o.getWorkstationId()))
                .filter(o -> !excludeOrderId.equals(o.getId()))
                .toList();

        log.info("Found {} other CONFIRMED orders to update", confirmedOrders.size());

        for (CustomerOrder otherOrder : confirmedOrders) {
            String newScenario = determineScenarioString(otherOrder);

            if (!newScenario.equals(otherOrder.getTriggerScenario())) {
                log.info("Order {} triggerScenario updated: {} → {}",
                        otherOrder.getOrderNumber(),
                        otherOrder.getTriggerScenario(),
                        newScenario);
                otherOrder.setTriggerScenario(newScenario);
                customerOrderRepository.save(otherOrder);
            }
        }
    }

    /**
     * Determine scenario string for trigger display.
     */
    private String determineScenarioString(CustomerOrder order) {
        List<OrderItem> items = order.getOrderItems();
        Long workstationId = order.getWorkstationId();

        boolean allAvailable = stockValidator.checkAllItemsAvailable(workstationId, items);
        if (allAvailable) {
            return "DIRECT_FULFILLMENT";
        }

        boolean anyAvailable = stockValidator.checkAnyItemsAvailable(workstationId, items);
        return anyAvailable ? "PARTIAL_FULFILLMENT" : "WAREHOUSE_ORDER_NEEDED";
    }

    private String appendNote(String existingNotes, String newNote) {
        return (existingNotes != null ? existingNotes + " | " : "") + newNote;
    }

    private CustomerOrderDTO mapToDTO(CustomerOrder order) {
        CustomerOrderDTO dto = new CustomerOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setWorkstationId(order.getWorkstationId());
        dto.setNotes(order.getNotes());
        return dto;
    }

    /**
     * Enumeration of fulfillment scenarios.
     */
    public enum FulfillmentScenario {
        DIRECT_FULFILLMENT,    // Scenario 1: All items available
        WAREHOUSE_ORDER,       // Scenario 2: No items available locally
        PARTIAL_FULFILLMENT,   // Scenario 3: Some items available
        PRODUCTION_PLANNING    // Scenario 4: High-volume/custom orders
    }
}
