package io.life.order.service;

import io.life.order.entity.InjectionMoldingOrder;
import io.life.order.entity.ProductionControlOrder;
import io.life.order.repository.InjectionMoldingOrderRepository;
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
    private final ProductionControlOrderRepository productionControlOrderRepository;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service:8014";
    private static final Long MODULES_SUPERMARKET_ID = 8L;

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
        try {
            String url = INVENTORY_SERVICE_URL + "/api/stock/adjust";
            Map<String, Object> request = Map.of(
                    "workstationId", MODULES_SUPERMARKET_ID,
                    "itemType", "PART",
                    "itemId", order.getOutputPartId(),
                    "delta", order.getQuantity(),
                    "reasonCode", "PRODUCTION",
                    "notes", "Completed order: " + order.getOrderNumber()
            );

            restTemplate.postForObject(url, request, Void.class);
            log.info("Credited {} PART {} ({}) to Modules Supermarket", 
                    order.getQuantity(), order.getOutputPartId(), order.getOutputPartName());

        } catch (Exception e) {
            log.error("Failed to credit inventory for order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Inventory credit failed", e);
        }
    }

    private void propagateStatusToParent(Long productionControlOrderId) {
        try {
            long totalOrders = injectionMoldingOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
            long completedOrders = injectionMoldingOrderRepository.countByProductionControlOrderIdAndStatus(
                    productionControlOrderId, "COMPLETED");

            log.info("ProductionControlOrder {} progress: {}/{} injection molding orders completed", 
                    productionControlOrderId, completedOrders, totalOrders);

            if (completedOrders == totalOrders && totalOrders > 0) {
                ProductionControlOrder parent = productionControlOrderRepository.findById(productionControlOrderId)
                        .orElseThrow(() -> new RuntimeException("Parent control order not found"));

                parent.setStatus("COMPLETED");
                parent.setActualFinishTime(LocalDateTime.now());
                productionControlOrderRepository.save(parent);

                log.info("Auto-completed ProductionControlOrder {} - all injection molding finished", 
                        parent.getControlOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to parent control order {}: {}", 
                    productionControlOrderId, e.getMessage());
        }
    }
}
