package io.life.order.service;

import io.life.order.entity.AssemblyOrder;
import io.life.order.entity.AssemblyControlOrder;
import io.life.order.repository.AssemblyOrderRepository;
import io.life.order.repository.AssemblyControlOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AssemblyOrderService
 * 
 * @deprecated As of Phase 6 (January 2026), replaced by workstation-specific services:
 * - {@link GearAssemblyOrderService} for WS-4
 * - {@link MotorAssemblyOrderService} for WS-5
 * - {@link FinalAssemblyOrderService} for WS-6
 * 
 * Business logic for assembly order lifecycle management.
 * Handles operator actions (start, complete, halt) and inventory integration.
 * Implements automatic status propagation to parent AssemblyControlOrder.
 * 
 * Special handling for Final Assembly (WS-6):
 * - Credits Plant Warehouse (WS-7) with Product Variants
 */
@Deprecated(since = "Phase 6", forRemoval = false)
@Service
@Slf4j
@RequiredArgsConstructor
public class AssemblyOrderService {

    private final AssemblyOrderRepository assemblyOrderRepository;
    private final AssemblyControlOrderRepository assemblyControlOrderRepository;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service:8014";
    private static final Long PLANT_WAREHOUSE_ID = 7L;
    private static final Long MODULES_SUPERMARKET_ID = 8L;
    private static final Long FINAL_ASSEMBLY_WORKSTATION_ID = 6L;

    /**
     * Get all assembly orders for a workstation
     * Used by assembly operator dashboards
     */
    public List<AssemblyOrder> getOrdersForWorkstation(Long workstationId) {
        return assemblyOrderRepository.findByWorkstationId(workstationId);
    }

    /**
     * Get assembly orders by parent control order
     * Used by Assembly Control for monitoring
     */
    public List<AssemblyOrder> getOrdersByControlOrder(Long assemblyControlOrderId) {
        return assemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId);
    }

    /**
     * Get assembly order by ID
     */
    public AssemblyOrder getOrderById(Long id) {
        return assemblyOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assembly order not found: " + id));
    }

    /**
     * Start assembly order
     * Updates status to IN_PROGRESS and records actual start time
     */
    @Transactional
    public AssemblyOrder startOrder(Long orderId) {
        AssemblyOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start PENDING orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        AssemblyOrder saved = assemblyOrderRepository.save(order);
        log.info("Started assembly order: {} at workstation {}", order.getOrderNumber(), order.getWorkstationId());
        
        return saved;
    }

    /**
     * Complete assembly order
     * Credits inventory and propagates status to parent control order
     * Special case: Final Assembly (WS-6) credits Plant Warehouse
     */
    @Transactional
    public AssemblyOrder completeOrder(Long orderId) {
        AssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit inventory (location depends on workstation)
        creditInventory(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        AssemblyOrder saved = assemblyOrderRepository.save(order);

        log.info("Completed assembly order: {} - {} {} produced", 
                order.getOrderNumber(), order.getQuantity(), order.getItemType());

        // Propagate status to parent control order
        propagateStatusToParent(order.getAssemblyControlOrderId());

        return saved;
    }

    /**
     * Halt assembly order
     * Temporarily pauses work
     */
    @Transactional
    public AssemblyOrder haltOrder(Long orderId) {
        AssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        AssemblyOrder saved = assemblyOrderRepository.save(order);
        
        log.info("Halted assembly order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Resume halted assembly order
     */
    @Transactional
    public AssemblyOrder resumeOrder(Long orderId) {
        AssemblyOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        AssemblyOrder saved = assemblyOrderRepository.save(order);
        
        log.info("Resumed assembly order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Credit inventory after assembly completion
     * - Final Assembly (WS-6) → Plant Warehouse (WS-7) - Product Variants
     * - Other assembly (WS-4, WS-5) → Modules Supermarket (WS-8) - Modules
     */
    private void creditInventory(AssemblyOrder order) {
        try {
            Long targetWorkstationId;
            String location;

            if (FINAL_ASSEMBLY_WORKSTATION_ID.equals(order.getWorkstationId())) {
                targetWorkstationId = PLANT_WAREHOUSE_ID;
                location = "Plant Warehouse";
            } else {
                targetWorkstationId = MODULES_SUPERMARKET_ID;
                location = "Modules Supermarket";
            }

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
            log.info("Credited {} {} {} to {}", 
                    order.getQuantity(), order.getItemType(), order.getItemId(), location);

        } catch (Exception e) {
            log.error("Failed to credit inventory for assembly order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Inventory credit failed", e);
        }
    }

    /**
     * Propagate status to parent AssemblyControlOrder
     * When all child assembly orders complete, parent auto-completes
     */
    private void propagateStatusToParent(Long assemblyControlOrderId) {
        try {
            long totalOrders = assemblyOrderRepository.countByAssemblyControlOrderId(assemblyControlOrderId);
            long completedOrders = assemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    assemblyControlOrderId, "COMPLETED");

            log.info("AssemblyControlOrder {} progress: {}/{} assembly orders completed", 
                    assemblyControlOrderId, completedOrders, totalOrders);

            // If all child orders completed, update parent to COMPLETED
            if (completedOrders == totalOrders && totalOrders > 0) {
                AssemblyControlOrder parent = assemblyControlOrderRepository.findById(assemblyControlOrderId)
                        .orElseThrow(() -> new RuntimeException("Parent control order not found"));

                parent.setStatus("COMPLETED");
                parent.setActualFinishTime(LocalDateTime.now());
                assemblyControlOrderRepository.save(parent);

                log.info("Auto-completed AssemblyControlOrder {} - all assembly orders finished", 
                        parent.getControlOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to parent control order {}: {}", 
                    assemblyControlOrderId, e.getMessage());
            // Don't fail the assembly order completion if status propagation fails
        }
    }
}
