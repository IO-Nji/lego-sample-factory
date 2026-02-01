package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.client.SimalClient;
import io.life.order.entity.InjectionMoldingOrder;
import io.life.order.repository.InjectionMoldingOrderRepository;
import io.life.order.service.OrderOrchestrationService.WorkstationOrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * InjectionMoldingOrderService
 * 
 * Business logic for Injection Molding (WS-1) order lifecycle management.
 * INPUT: Raw materials (pellets, resins) - NOT parts
 * OUTPUT: Basic molded parts
 * Credits Modules Supermarket (WS-8) on completion
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InjectionMoldingOrderService {

    private final InjectionMoldingOrderRepository injectionMoldingOrderRepository;
    private final InventoryClient inventoryClient;
    private final SimalClient simalClient;
    private final OrderOrchestrationService orchestrationService;

    public List<InjectionMoldingOrder> getOrdersForWorkstation(Long workstationId) {
        return injectionMoldingOrderRepository.findByWorkstationId(workstationId);
    }

    public List<InjectionMoldingOrder> getOrdersByControlOrder(Long productionControlOrderId) {
        return injectionMoldingOrderRepository.findByProductionControlOrderId(productionControlOrderId);
    }

    public InjectionMoldingOrder getOrderById(Long id) {
        return injectionMoldingOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Injection molding order not found: " + id));
    }

    @Transactional
    public InjectionMoldingOrder startOrder(Long orderId) {
        InjectionMoldingOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start PENDING orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        InjectionMoldingOrder saved = injectionMoldingOrderRepository.save(order);
        log.info("Started injection molding order: {} at WS-1", order.getOrderNumber());
        
        // Update task status in SimAL
        String taskId = SimalClient.generateTaskId(1L, order.getOrderNumber());
        simalClient.updateTaskStatus(taskId, "IN_PROGRESS");
        
        return saved;
    }

    @Transactional
    public InjectionMoldingOrder completeOrder(Long orderId) {
        InjectionMoldingOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit inventory
        creditInventory(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        InjectionMoldingOrder saved = injectionMoldingOrderRepository.save(order);

        log.info("Completed injection molding order: {} - {} {} produced", 
                order.getOrderNumber(), order.getQuantity(), order.getOutputPartName());

        // Update task status in SimAL
        String taskId = SimalClient.generateTaskId(1L, order.getOrderNumber());
        simalClient.updateTaskStatus(taskId, "COMPLETED");

        // Propagate status to parent control order
        propagateStatusToParent(order.getProductionControlOrderId());

        return saved;
    }

    @Transactional
    public InjectionMoldingOrder haltOrder(Long orderId) {
        InjectionMoldingOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        InjectionMoldingOrder saved = injectionMoldingOrderRepository.save(order);
        
        log.info("Halted injection molding order: {}", order.getOrderNumber());
        
        // Update task status in SimAL
        String taskId = SimalClient.generateTaskId(1L, order.getOrderNumber());
        simalClient.updateTaskStatus(taskId, "HALTED");
        
        return saved;
    }

    @Transactional
    public InjectionMoldingOrder resumeOrder(Long orderId) {
        InjectionMoldingOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        InjectionMoldingOrder saved = injectionMoldingOrderRepository.save(order);
        
        log.info("Resumed injection molding order: {}", order.getOrderNumber());
        return saved;
    }

    private void creditInventory(InjectionMoldingOrder order) {
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
                WorkstationOrderType.INJECTION_MOLDING,
                productionControlOrderId
        );
    }
}
