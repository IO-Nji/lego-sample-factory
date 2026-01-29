package io.life.order.service;

import io.life.order.entity.AssemblyControlOrder;
import io.life.order.entity.MotorAssemblyOrder;
import io.life.order.repository.AssemblyControlOrderRepository;
import io.life.order.repository.MotorAssemblyOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MotorAssemblyOrderService
 * 
 * Business logic for Motor Assembly (WS-5) order lifecycle management.
 * INPUT: Motor-specific finished parts (motors, wires, housings, connectors)
 * OUTPUT: Motor modules
 * Credits Modules Supermarket (WS-8) on completion
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MotorAssemblyOrderService {

    private final MotorAssemblyOrderRepository motorAssemblyOrderRepository;
    private final AssemblyControlOrderRepository assemblyControlOrderRepository;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service:8014";
    private static final Long MODULES_SUPERMARKET_ID = 8L;

    public List<MotorAssemblyOrder> getOrdersForWorkstation(Long workstationId) {
        return motorAssemblyOrderRepository.findByWorkstationId(workstationId);
    }

    public List<MotorAssemblyOrder> getOrdersByControlOrder(Long assemblyControlOrderId) {
        return motorAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId);
    }

    public MotorAssemblyOrder getOrderById(Long id) {
        return motorAssemblyOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Motor assembly order not found: " + id));
    }

    @Transactional
    public MotorAssemblyOrder startOrder(Long orderId) {
        MotorAssemblyOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus()) && !"WAITING_FOR_PARTS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start PENDING or WAITING_FOR_PARTS orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        MotorAssemblyOrder saved = motorAssemblyOrderRepository.save(order);
        log.info("Started motor assembly order: {} at WS-5", order.getOrderNumber());
        
        return saved;
    }

    @Transactional
    public MotorAssemblyOrder completeOrder(Long orderId) {
        MotorAssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit inventory
        creditInventory(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        MotorAssemblyOrder saved = motorAssemblyOrderRepository.save(order);

        log.info("Completed motor assembly order: {} - {} {} produced", 
                order.getOrderNumber(), order.getQuantity(), order.getOutputModuleName());

        // Propagate status to parent control order
        propagateStatusToParent(order.getAssemblyControlOrderId());

        return saved;
    }

    @Transactional
    public MotorAssemblyOrder haltOrder(Long orderId) {
        MotorAssemblyOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        MotorAssemblyOrder saved = motorAssemblyOrderRepository.save(order);
        
        log.info("Halted motor assembly order: {}", order.getOrderNumber());
        return saved;
    }

    @Transactional
    public MotorAssemblyOrder resumeOrder(Long orderId) {
        MotorAssemblyOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        MotorAssemblyOrder saved = motorAssemblyOrderRepository.save(order);
        
        log.info("Resumed motor assembly order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Mark order as waiting for parts (after supply order created)
     */
    @Transactional
    public MotorAssemblyOrder markWaitingForParts(Long orderId, Long supplyOrderId) {
        MotorAssemblyOrder order = getOrderById(orderId);
        
        order.setStatus("WAITING_FOR_PARTS");
        order.setSupplyOrderId(supplyOrderId);
        
        MotorAssemblyOrder saved = motorAssemblyOrderRepository.save(order);
        log.info("Motor assembly order {} marked as waiting for parts (Supply Order: {})", 
                order.getOrderNumber(), supplyOrderId);
        
        return saved;
    }

    private void creditInventory(MotorAssemblyOrder order) {
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
            long totalOrders = motorAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
            long completedOrders = motorAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    assemblyControlOrderId, "COMPLETED");

            log.info("AssemblyControlOrder {} progress: {}/{} motor assembly orders completed", 
                    assemblyControlOrderId, completedOrders, totalOrders);

            if (completedOrders == totalOrders && totalOrders > 0) {
                AssemblyControlOrder parent = assemblyControlOrderRepository.findById(assemblyControlOrderId)
                        .orElseThrow(() -> new RuntimeException("Parent control order not found"));

                parent.setStatus("COMPLETED");
                parent.setActualFinishTime(LocalDateTime.now());
                assemblyControlOrderRepository.save(parent);

                log.info("Auto-completed AssemblyControlOrder {} - all motor assembly finished", 
                        parent.getControlOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to assembly control order {}: {}", 
                    assemblyControlOrderId, e.getMessage());
        }
    }
}
