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

    // ... [Keep all existing methods up to fulfillAllItems unchanged] ...

    /**
     * Scenario A: All items available - Deduct from Modules Supermarket, create Final Assembly orders
     * PROPER Scenario 2: Create AssemblyControlOrders for Final Assembly station
     * Final Assembly will credit Plant Warehouse when they complete their work
     */
    private WarehouseOrderDTO fulfillAllItems(WarehouseOrder order) {
        logger.info("Fulfilling all items for warehouse order {}", order.getWarehouseOrderNumber());

        boolean allItemsFulfilled = true;
        for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
            try {
                // Deduct from Modules Supermarket stock
                boolean deducted = inventoryService.updateStock(
                        order.getFulfillingWorkstationId(),
                        item.getItemId(),
                        item.getRequestedQuantity()
                );

                if (deducted) {
                    item.setFulfilledQuantity(item.getRequestedQuantity());
                    logger.info("  ✓ Item {} qty {} fulfilled", item.getItemId(), item.getRequestedQuantity());
                } else {
                    allItemsFulfilled = false;
                    logger.warn("  ✗ Failed to deduct item {} from inventory", item.getItemId());
                }
            } catch (Exception e) {
                allItemsFulfilled = false;
                logger.error("  ✗ Error fulfilling item {}: {}", item.getItemId(), e.getMessage());
            }
        }

        if (allItemsFulfilled) {
            // Mark as FULFILLED
            order.setStatus("FULFILLED");
            logger.info("Warehouse order {} fulfilled - creating Final Assembly orders", order.getWarehouseOrderNumber());
            
            // CREATE FINAL ASSEMBLY ORDERS (Proper Scenario 2 workflow)
            // Modules Supermarket debited, now Final Assembly needs to assemble products
            // Final Assembly completion will credit Plant Warehouse
            createFinalAssemblyOrdersFromWarehouseOrder(order);
            
            orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "FULFILLED",
                    "Modules fulfilled, Final Assembly orders created - awaiting assembly completion");
        } else {
            order.setStatus("PARTIALLY_FULFILLED");
            logger.warn("Warehouse order {} partially fulfilled due to inventory errors", order.getWarehouseOrderNumber());
            orderAuditService.recordOrderEvent(WAREHOUSE_AUDIT_SOURCE, order.getId(), "PARTIALLY_FULFILLED",
                    "Partial fulfillment due to inventory errors");
        }

        order.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(warehouseOrderRepository.save(order));
    }

    /**
     * Create AssemblyControlOrders for Final Assembly station.
     * PROPER Scenario 2 workflow - Assembly station will credit Plant Warehouse when complete.
     * 
     * Each fulfilled item in the warehouse order becomes a separate assembly order.
     * The Final Assembly workstation will complete these orders, which will:
     * 1. Update SimAL schedule status
     * 2. Credit Plant Warehouse with finished products
     * 3. Update customer order status to ready for fulfillment
     */
    private void createFinalAssemblyOrdersFromWarehouseOrder(WarehouseOrder order) {
        try {
            logger.info("Creating Final Assembly orders for warehouse order {}", order.getWarehouseOrderNumber());
            
            for (WarehouseOrderItem item : order.getWarehouseOrderItems()) {
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
                                order.getSourceCustomerOrderId(), // Use customer order ID as source
                                FINAL_ASSEMBLY_WORKSTATION_ID,    // Assign to Final Assembly (WS-6)
                                "WO-" + order.getWarehouseOrderNumber(), // Use warehouse order as schedule ID
                                "MEDIUM",                          // Priority
                                targetStart,
                                targetCompletion,
                                String.format("Assemble %s (Product #%d) - Qty: %d from warehouse order %s", 
                                        item.getItemName(), productId, quantity, order.getWarehouseOrderNumber()),
                                "Check product quality and specifications",
                                "Perform final product testing",
                                "Package for Plant Warehouse delivery",
                                estimatedDuration
                        );
                        
                        logger.info("✓ Final Assembly order created for Product #{} ({}) qty {}", 
                                productId, item.getItemName(), quantity);
                        
                        orderAuditService.recordOrderEvent("FINAL_ASSEMBLY", order.getId(), "ASSEMBLY_ORDER_CREATED",
                                String.format("Final Assembly order created for Product #%d (%s) qty %d from warehouse order %s",
                                        productId, item.getItemName(), quantity, order.getWarehouseOrderNumber()));
                    } catch (Exception e) {
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
}
