package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.entity.PartPreProductionOrder;
import io.life.order.repository.PartPreProductionOrderRepository;
import io.life.order.service.OrderOrchestrationService.WorkstationOrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PartPreProductionOrderService
 * 
 * Business logic for Parts Pre-Production (WS-2) order lifecycle management.
 * INPUT: Basic parts from WS-1 or Parts Supply Warehouse
 * OUTPUT: Pre-processed parts (machined, drilled, cut)
 * Credits Modules Supermarket (WS-8) on completion
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PartPreProductionOrderService {

    private final PartPreProductionOrderRepository partPreProductionOrderRepository;
    private final InventoryClient inventoryClient;
    private final OrderOrchestrationService orchestrationService;

    public List<PartPreProductionOrder> getOrdersForWorkstation(Long workstationId) {
        return partPreProductionOrderRepository.findByWorkstationId(workstationId);
    }

    public List<PartPreProductionOrder> getOrdersByControlOrder(Long productionControlOrderId) {
        return partPreProductionOrderRepository.findByProductionControlOrderId(productionControlOrderId);
    }

    public PartPreProductionOrder getOrderById(Long id) {
        return partPreProductionOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part pre-production order not found: " + id));
    }

    @Transactional
    public PartPreProductionOrder startOrder(Long orderId) {
        PartPreProductionOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus()) && !"WAITING_FOR_PARTS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start PENDING or WAITING_FOR_PARTS orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        PartPreProductionOrder saved = partPreProductionOrderRepository.save(order);
        log.info("Started part pre-production order: {} at WS-2", order.getOrderNumber());
        
        return saved;
    }

    @Transactional
    public PartPreProductionOrder completeOrder(Long orderId) {
        PartPreProductionOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit inventory
        creditInventory(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        PartPreProductionOrder saved = partPreProductionOrderRepository.save(order);

        log.info("Completed part pre-production order: {} - {} {} produced", 
                order.getOrderNumber(), order.getQuantity(), order.getOutputPartName());

        // Propagate status to parent control order
        propagateStatusToParent(order.getProductionControlOrderId());

        return saved;
    }

    @Transactional
    public PartPreProductionOrder haltOrder(Long orderId) {
        PartPreProductionOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        PartPreProductionOrder saved = partPreProductionOrderRepository.save(order);
        
        log.info("Halted part pre-production order: {}", order.getOrderNumber());
        return saved;
    }

    @Transactional
    public PartPreProductionOrder resumeOrder(Long orderId) {
        PartPreProductionOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        PartPreProductionOrder saved = partPreProductionOrderRepository.save(order);
        
        log.info("Resumed part pre-production order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Mark order as waiting for parts (after supply order created)
     */
    @Transactional
    public PartPreProductionOrder markWaitingForParts(Long orderId, Long supplyOrderId) {
        PartPreProductionOrder order = getOrderById(orderId);
        
        order.setStatus("WAITING_FOR_PARTS");
        order.setSupplyOrderId(supplyOrderId);
        
        PartPreProductionOrder saved = partPreProductionOrderRepository.save(order);
        log.info("Part pre-production order {} marked as waiting for parts (Supply Order: {})", 
                order.getOrderNumber(), supplyOrderId);
        
        return saved;
    }

    private void creditInventory(PartPreProductionOrder order) {
        inventoryClient.creditPartsToModulesSupermarket(
                order.getOutputPartId(),
                order.getQuantity(),
                order.getOrderNumber()
        );
        log.info("Credited {} PART {} ({}) to Modules Supermarket", 
                order.getQuantity(), order.getOutputPartId(), order.getOutputPartName());
    }

    private void propagateStatusToParent(Long productionControlOrderId) {
        orchestrationService.notifyWorkstationOrderComplete(
                WorkstationOrderType.PART_PRE_PRODUCTION,
                productionControlOrderId
        );
    }
}
