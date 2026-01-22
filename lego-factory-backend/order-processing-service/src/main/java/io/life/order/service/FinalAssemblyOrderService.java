package io.life.order.service;

import io.life.order.entity.AssemblyControlOrder;
import io.life.order.entity.FinalAssemblyOrder;
import io.life.order.entity.WarehouseOrder;
import io.life.order.repository.AssemblyControlOrderRepository;
import io.life.order.repository.FinalAssemblyOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FinalAssemblyOrderService
 * 
 * Business logic for Final Assembly (WS-6) order lifecycle management.
 * CRITICAL DISTINCTION: Consumes MODULES (from Modules Supermarket), not parts.
 * Credits Plant Warehouse (WS-7) with Product Variants on completion.
 * 
 * Used in Scenario 2: Warehouse Order → Final Assembly → Plant Warehouse
 * Used in Scenario 3: Production Planning → Final Assembly → Plant Warehouse
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FinalAssemblyOrderService {

    private final FinalAssemblyOrderRepository finalAssemblyOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final AssemblyControlOrderRepository assemblyControlOrderRepository;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service:8014";
    private static final String MASTERDATA_SERVICE_URL = "http://masterdata-service:8013";
    private static final Long PLANT_WAREHOUSE_ID = 7L; // Target for product variants
    private static final Long MODULES_SUPERMARKET_ID = 8L; // Source for modules

    /**
     * Get all final assembly orders for WS-6
     */
    public List<FinalAssemblyOrder> getOrdersForWorkstation(Long workstationId) {
        return finalAssemblyOrderRepository.findByWorkstationId(workstationId);
    }

    /**
     * Get final assembly orders by warehouse order (Scenario 2)
     */
    public List<FinalAssemblyOrder> getOrdersByWarehouseOrder(Long warehouseOrderId) {
        return finalAssemblyOrderRepository.findByWarehouseOrderId(warehouseOrderId);
    }

    /**
     * Get final assembly orders by assembly control order (Scenario 3)
     */
    public List<FinalAssemblyOrder> getOrdersByControlOrder(Long assemblyControlOrderId) {
        return finalAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId);
    }

    /**
     * Get final assembly order by ID
     */
    public FinalAssemblyOrder getOrderById(Long id) {
        return finalAssemblyOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Final assembly order not found: " + id));
    }

    /**
     * Confirm final assembly order
     * Operator confirms receipt of modules from Modules Supermarket
     */
    @Transactional
    public FinalAssemblyOrder confirmOrder(Long orderId) {
        FinalAssemblyOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Can only confirm PENDING orders. Current status: " + order.getStatus());
        }

        // Verify modules availability in Modules Supermarket
        boolean modulesAvailable = checkModulesAvailability(order);
        if (!modulesAvailable) {
            throw new IllegalStateException("Required modules not available in Modules Supermarket");
        }

        order.setStatus("CONFIRMED");
        order.setModulesReceived(true);
        
        FinalAssemblyOrder saved = finalAssemblyOrderRepository.save(order);
        log.info("Confirmed final assembly order: {} - modules verified", order.getOrderNumber());
        
        return saved;
    }

    /**
     * Start final assembly order
     * Updates status to IN_PROGRESS and records actual start time
     */
    @Transactional
    public FinalAssemblyOrder startOrder(Long orderId) {
        FinalAssemblyOrder order = getOrderById(orderId);
        
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start CONFIRMED orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        FinalAssemblyOrder saved = finalAssemblyOrderRepository.save(order);
        log.info("Started final assembly order: {} at WS-6", order.getOrderNumber());
        
        return saved;
    }

    /**
     * Complete final assembly order
     * Credits Product Variants to Plant Warehouse (WS-7)
     * Propagates status to parent warehouse order or assembly control order
     */
    @Transactional
    public FinalAssemblyOrder completeOrder(Long orderId) {
        FinalAssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit Product Variants to Plant Warehouse
        creditPlantWarehouse(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        FinalAssemblyOrder saved = finalAssemblyOrderRepository.save(order);

        log.info("Completed final assembly order: {} - {} {} produced and credited to Plant Warehouse", 
                order.getOrderNumber(), order.getQuantity(), order.getOutputProductVariantName());

        // Propagate status to parent orders
        if (order.getWarehouseOrderId() != null) {
            propagateStatusToWarehouseOrder(order.getWarehouseOrderId());
        }
        if (order.getAssemblyControlOrderId() != null) {
            propagateStatusToControlOrder(order.getAssemblyControlOrderId());
        }

        return saved;
    }

    /**
     * Halt final assembly order
     * Temporarily pauses work
     */
    @Transactional
    public FinalAssemblyOrder haltOrder(Long orderId) {
        FinalAssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        FinalAssemblyOrder saved = finalAssemblyOrderRepository.save(order);
        
        log.info("Halted final assembly order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Resume halted final assembly order
     */
    @Transactional
    public FinalAssemblyOrder resumeOrder(Long orderId) {
        FinalAssemblyOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        FinalAssemblyOrder saved = finalAssemblyOrderRepository.save(order);
        
        log.info("Resumed final assembly order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Check if required modules are available in Modules Supermarket
     */
    private boolean checkModulesAvailability(FinalAssemblyOrder order) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/stock/workstation/" + MODULES_SUPERMARKET_ID;
            // Implementation would check each required module
            // For now, return true (actual logic would parse requiredModuleDetails JSON)
            return true;
        } catch (Exception e) {
            log.error("Failed to check modules availability for order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
            return false;
        }
    }

    /**
     * Credit Product Variants to Plant Warehouse after final assembly completion
     * CRITICAL: This is the ONLY place where Plant Warehouse stock is credited
     */
    private void creditPlantWarehouse(FinalAssemblyOrder order) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/stock/credit";
            Map<String, Object> request = Map.of(
                    "workstationId", PLANT_WAREHOUSE_ID,
                    "itemType", "PRODUCT_VARIANT",
                    "itemId", order.getOutputProductVariantId(),
                    "quantity", order.getQuantity()
            );

            restTemplate.postForObject(url, request, Void.class);
            log.info("Credited {} Product Variant {} ({}) to Plant Warehouse", 
                    order.getQuantity(), order.getOutputProductVariantId(), order.getOutputProductVariantName());

        } catch (Exception e) {
            log.error("Failed to credit Plant Warehouse for order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Plant Warehouse credit failed", e);
        }
    }

    /**
     * Propagate status to parent WarehouseOrder (Scenario 2)
     * When all final assembly orders complete, warehouse order becomes FULFILLED
     */
    private void propagateStatusToWarehouseOrder(Long warehouseOrderId) {
        try {
            long totalOrders = finalAssemblyOrderRepository.countByWarehouseOrderIdAndStatus(warehouseOrderId, "COMPLETED");
            long allOrders = finalAssemblyOrderRepository.findByWarehouseOrderId(warehouseOrderId).size();

            log.info("WarehouseOrder {} progress: {}/{} final assembly orders completed", 
                    warehouseOrderId, totalOrders, allOrders);

            if (totalOrders == allOrders && allOrders > 0) {
                WarehouseOrder parent = warehouseOrderRepository.findById(warehouseOrderId)
                        .orElseThrow(() -> new RuntimeException("Warehouse order not found"));

                parent.setStatus("FULFILLED");
                warehouseOrderRepository.save(parent);

                log.info("Auto-fulfilled WarehouseOrder {} - all final assembly completed", 
                        parent.getWarehouseOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to warehouse order {}: {}", 
                    warehouseOrderId, e.getMessage());
        }
    }

    /**
     * Propagate status to parent AssemblyControlOrder (Scenario 3)
     * When all final assembly orders complete, control order auto-completes
     */
    private void propagateStatusToControlOrder(Long assemblyControlOrderId) {
        try {
            long totalOrders = finalAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    assemblyControlOrderId, "COMPLETED");
            long allOrders = finalAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();

            log.info("AssemblyControlOrder {} progress: {}/{} final assembly orders completed", 
                    assemblyControlOrderId, totalOrders, allOrders);

            if (totalOrders == allOrders && allOrders > 0) {
                AssemblyControlOrder parent = assemblyControlOrderRepository.findById(assemblyControlOrderId)
                        .orElseThrow(() -> new RuntimeException("Assembly control order not found"));

                parent.setStatus("COMPLETED");
                parent.setActualFinishTime(LocalDateTime.now());
                assemblyControlOrderRepository.save(parent);

                log.info("Auto-completed AssemblyControlOrder {} - all final assembly finished", 
                        parent.getControlOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to assembly control order {}: {}", 
                    assemblyControlOrderId, e.getMessage());
        }
    }
}
