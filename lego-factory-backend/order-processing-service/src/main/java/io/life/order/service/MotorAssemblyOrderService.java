package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.entity.MotorAssemblyOrder;
import io.life.order.repository.MotorAssemblyOrderRepository;
import io.life.order.service.OrderOrchestrationService.WorkstationOrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final InventoryClient inventoryClient;
    private final OrderOrchestrationService orchestrationService;

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
        inventoryClient.creditModulesToModulesSupermarket(
                order.getOutputModuleId(),
                order.getQuantity(),
                order.getOrderNumber()
        );
        log.info("Credited {} MODULE {} ({}) to Modules Supermarket", 
                order.getQuantity(), order.getOutputModuleId(), order.getOutputModuleName());
    }

    private void propagateStatusToParent(Long assemblyControlOrderId) {
        orchestrationService.notifyWorkstationOrderComplete(
                WorkstationOrderType.MOTOR_ASSEMBLY,
                assemblyControlOrderId
        );
    }
}
