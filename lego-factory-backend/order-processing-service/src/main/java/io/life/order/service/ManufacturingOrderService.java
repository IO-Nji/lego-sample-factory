package io.life.order.service;

import io.life.order.entity.ManufacturingOrder;
import io.life.order.entity.ProductionControlOrder;
import io.life.order.repository.ManufacturingOrderRepository;
import io.life.order.repository.ProductionControlOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ManufacturingOrderService
 * 
 * @deprecated As of Phase 6 (January 2026), replaced by workstation-specific services:
 * - {@link InjectionMoldingOrderService} for WS-1
 * - {@link PartPreProductionOrderService} for WS-2
 * - {@link PartFinishingOrderService} for WS-3
 * 
 * Business logic for manufacturing order lifecycle management.
 * Handles operator actions (start, complete, halt) and inventory integration.
 * Implements automatic status propagation to parent ProductionControlOrder.
 */
@Deprecated(since = "Phase 6", forRemoval = false)
@Service
@Slf4j
@RequiredArgsConstructor
public class ManufacturingOrderService {

    private final ManufacturingOrderRepository manufacturingOrderRepository;
    private final ProductionControlOrderRepository productionControlOrderRepository;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service:8014";
    private static final String MASTERDATA_SERVICE_URL = "http://masterdata-service:8013";

    /**
     * Get all manufacturing orders for a workstation
     * Used by manufacturing operator dashboards
     */
    public List<ManufacturingOrder> getOrdersForWorkstation(Long workstationId) {
        return manufacturingOrderRepository.findByWorkstationId(workstationId);
    }

    /**
     * Get manufacturing orders by parent control order
     * Used by Production Control for monitoring
     */
    public List<ManufacturingOrder> getOrdersByControlOrder(Long productionControlOrderId) {
        return manufacturingOrderRepository.findByProductionControlOrderId(productionControlOrderId);
    }

    /**
     * Get manufacturing order by ID
     */
    public ManufacturingOrder getOrderById(Long id) {
        return manufacturingOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manufacturing order not found: " + id));
    }

    /**
     * Start manufacturing order
     * Updates status to IN_PROGRESS and records actual start time
     */
    @Transactional
    public ManufacturingOrder startOrder(Long orderId) {
        ManufacturingOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start PENDING orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        ManufacturingOrder saved = manufacturingOrderRepository.save(order);
        log.info("Started manufacturing order: {} at workstation {}", order.getOrderNumber(), order.getWorkstationId());
        
        return saved;
    }

    /**
     * Complete manufacturing order
     * Credits inventory and propagates status to parent control order
     */
    @Transactional
    public ManufacturingOrder completeOrder(Long orderId) {
        ManufacturingOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit inventory
        creditInventory(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        ManufacturingOrder saved = manufacturingOrderRepository.save(order);

        log.info("Completed manufacturing order: {} - {} {} produced", 
                order.getOrderNumber(), order.getQuantity(), order.getItemType());

        // Propagate status to parent control order
        propagateStatusToParent(order.getProductionControlOrderId());

        return saved;
    }

    /**
     * Halt manufacturing order
     * Temporarily pauses work
     */
    @Transactional
    public ManufacturingOrder haltOrder(Long orderId) {
        ManufacturingOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        ManufacturingOrder saved = manufacturingOrderRepository.save(order);
        
        log.info("Halted manufacturing order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Resume halted manufacturing order
     */
    @Transactional
    public ManufacturingOrder resumeOrder(Long orderId) {
        ManufacturingOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        ManufacturingOrder saved = manufacturingOrderRepository.save(order);
        
        log.info("Resumed manufacturing order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Credit inventory after manufacturing completion
     * Parts/Modules credited to Modules Supermarket (WS-8)
     */
    private void creditInventory(ManufacturingOrder order) {
        try {
            Long targetWorkstationId = 8L; // Modules Supermarket

            String url = INVENTORY_SERVICE_URL + "/api/stock/adjust";
            Map<String, Object> request = Map.of(
                    "workstationId", targetWorkstationId,
                    "itemType", order.getItemType(),
                    "itemId", order.getItemId(),
                    "delta", order.getQuantity(),
                    "reasonCode", "PRODUCTION",
                    "notes", "Completed order: " + order.getOrderNumber()
            );

            restTemplate.postForObject(url, request, Void.class);
            log.info("Credited {} {} {} to Modules Supermarket", 
                    order.getQuantity(), order.getItemType(), order.getItemId());

        } catch (Exception e) {
            log.error("Failed to credit inventory for manufacturing order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Inventory credit failed", e);
        }
    }

    /**
     * Propagate status to parent ProductionControlOrder
     * When all child manufacturing orders complete, parent auto-completes
     */
    private void propagateStatusToParent(Long productionControlOrderId) {
        try {
            long totalOrders = manufacturingOrderRepository.countByProductionControlOrderId(productionControlOrderId);
            long completedOrders = manufacturingOrderRepository.countByProductionControlOrderIdAndStatus(
                    productionControlOrderId, "COMPLETED");

            log.info("ProductionControlOrder {} progress: {}/{} manufacturing orders completed", 
                    productionControlOrderId, completedOrders, totalOrders);

            // If all child orders completed, update parent to COMPLETED
            if (completedOrders == totalOrders && totalOrders > 0) {
                ProductionControlOrder parent = productionControlOrderRepository.findById(productionControlOrderId)
                        .orElseThrow(() -> new RuntimeException("Parent control order not found"));

                parent.setStatus("COMPLETED");
                parent.setActualFinishTime(LocalDateTime.now());
                productionControlOrderRepository.save(parent);

                log.info("Auto-completed ProductionControlOrder {} - all manufacturing orders finished", 
                        parent.getControlOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to parent control order {}: {}", 
                    productionControlOrderId, e.getMessage());
            // Don't fail the manufacturing order completion if status propagation fails
        }
    }
}
