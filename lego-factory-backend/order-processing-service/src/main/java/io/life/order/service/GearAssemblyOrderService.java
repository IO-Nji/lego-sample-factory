package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.client.SimalClient;
import io.life.order.entity.GearAssemblyOrder;
import io.life.order.repository.GearAssemblyOrderRepository;
import io.life.order.service.OrderOrchestrationService.WorkstationOrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final InventoryClient inventoryClient;
    private final SimalClient simalClient;
    private final OrderOrchestrationService orchestrationService;

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
        simalClient.updateTaskStatus(SimalClient.generateTaskId(4L, order.getOrderNumber()), "IN_PROGRESS");
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

        simalClient.updateTaskStatus(SimalClient.generateTaskId(4L, order.getOrderNumber()), "COMPLETED");
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
        simalClient.updateTaskStatus(SimalClient.generateTaskId(4L, order.getOrderNumber()), "HALTED");
        
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
                WorkstationOrderType.GEAR_ASSEMBLY,
                assemblyControlOrderId
        );
    }
}
