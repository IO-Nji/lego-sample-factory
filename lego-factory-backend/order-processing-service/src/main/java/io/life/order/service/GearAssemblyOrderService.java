package io.life.order.service;

import io.life.order.entity.AssemblyControlOrder;
import io.life.order.entity.GearAssemblyOrder;
import io.life.order.repository.AssemblyControlOrderRepository;
import io.life.order.repository.GearAssemblyOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GearAssemblyOrderService
 * 
 * Business logic for Gear Assembly (WS-4) order lifecycle management.
 * INPUT: Gear-specific finished parts (gears, shafts, bearings)
 * OUTPUT: Gear modules
 * Credits Modules Supermarket (WS-8) on completion
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GearAssemblyOrderService {

    private final GearAssemblyOrderRepository gearAssemblyOrderRepository;
    private final AssemblyControlOrderRepository assemblyControlOrderRepository;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service:8014";
    private static final Long MODULES_SUPERMARKET_ID = 8L;

    public List<GearAssemblyOrder> getOrdersForWorkstation(Long workstationId) {
        return gearAssemblyOrderRepository.findByWorkstationId(workstationId);
    }

    public List<GearAssemblyOrder> getOrdersByControlOrder(Long assemblyControlOrderId) {
        return gearAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId);
    }

    public GearAssemblyOrder getOrderById(Long id) {
        return gearAssemblyOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gear assembly order not found: " + id));
    }

    @Transactional
    public GearAssemblyOrder startOrder(Long orderId) {
        GearAssemblyOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus()) && !"WAITING_FOR_PARTS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start PENDING or WAITING_FOR_PARTS orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        GearAssemblyOrder saved = gearAssemblyOrderRepository.save(order);
        log.info("Started gear assembly order: {} at WS-4", order.getOrderNumber());
        
        return saved;
    }

    @Transactional
    public GearAssemblyOrder completeOrder(Long orderId) {
        GearAssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit inventory
        creditInventory(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        GearAssemblyOrder saved = gearAssemblyOrderRepository.save(order);

        log.info("Completed gear assembly order: {} - {} {} produced", 
                order.getOrderNumber(), order.getQuantity(), order.getOutputModuleName());

        // Propagate status to parent control order
        propagateStatusToParent(order.getAssemblyControlOrderId());

        return saved;
    }

    @Transactional
    public GearAssemblyOrder haltOrder(Long orderId) {
        GearAssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        GearAssemblyOrder saved = gearAssemblyOrderRepository.save(order);
        
        log.info("Halted gear assembly order: {}", order.getOrderNumber());
        return saved;
    }

    @Transactional
    public GearAssemblyOrder resumeOrder(Long orderId) {
        GearAssemblyOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        GearAssemblyOrder saved = gearAssemblyOrderRepository.save(order);
        
        log.info("Resumed gear assembly order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Mark order as waiting for parts (after supply order created)
     */
    @Transactional
    public GearAssemblyOrder markWaitingForParts(Long orderId, Long supplyOrderId) {
        GearAssemblyOrder order = getOrderById(orderId);
        
        order.setStatus("WAITING_FOR_PARTS");
        order.setSupplyOrderId(supplyOrderId);
        
        GearAssemblyOrder saved = gearAssemblyOrderRepository.save(order);
        log.info("Gear assembly order {} marked as waiting for parts (Supply Order: {})", 
                order.getOrderNumber(), supplyOrderId);
        
        return saved;
    }

    private void creditInventory(GearAssemblyOrder order) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/stock/adjust";
            Map<String, Object> request = Map.of(
                    "workstationId", MODULES_SUPERMARKET_ID,
                    "itemType", "MODULE",
                    "itemId", order.getOutputModuleId(),
                    "delta", order.getQuantity(),
                    "reasonCode", "PRODUCTION",
                    "notes", "Completed order: " + order.getOrderNumber()
            );

            restTemplate.postForObject(url, request, Void.class);
            log.info("Credited {} MODULE {} ({}) to Modules Supermarket", 
                    order.getQuantity(), order.getOutputModuleId(), order.getOutputModuleName());

        } catch (Exception e) {
            log.error("Failed to credit inventory for order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Inventory credit failed", e);
        }
    }

    private void propagateStatusToParent(Long assemblyControlOrderId) {
        try {
            long totalOrders = gearAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
            long completedOrders = gearAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    assemblyControlOrderId, "COMPLETED");

            log.info("AssemblyControlOrder {} progress: {}/{} gear assembly orders completed", 
                    assemblyControlOrderId, completedOrders, totalOrders);

            if (completedOrders == totalOrders && totalOrders > 0) {
                AssemblyControlOrder parent = assemblyControlOrderRepository.findById(assemblyControlOrderId)
                        .orElseThrow(() -> new RuntimeException("Parent control order not found"));

                parent.setStatus("COMPLETED");
                parent.setActualFinishTime(LocalDateTime.now());
                assemblyControlOrderRepository.save(parent);

                log.info("Auto-completed AssemblyControlOrder {} - all gear assembly finished", 
                        parent.getControlOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to assembly control order {}: {}", 
                    assemblyControlOrderId, e.getMessage());
        }
    }
}
