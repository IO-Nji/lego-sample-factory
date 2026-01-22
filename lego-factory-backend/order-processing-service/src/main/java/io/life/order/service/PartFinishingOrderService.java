package io.life.order.service;

import io.life.order.entity.PartFinishingOrder;
import io.life.order.entity.ProductionControlOrder;
import io.life.order.repository.PartFinishingOrderRepository;
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
 * PartFinishingOrderService
 * 
 * Business logic for Part Finishing (WS-3) order lifecycle management.
 * INPUT: Pre-processed parts from WS-2 or Parts Supply Warehouse
 * OUTPUT: Finished parts ready for assembly (polished, coated, inspected)
 * Credits Modules Supermarket (WS-8) on completion
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PartFinishingOrderService {

    private final PartFinishingOrderRepository partFinishingOrderRepository;
    private final ProductionControlOrderRepository productionControlOrderRepository;
    private final RestTemplate restTemplate;

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service:8014";
    private static final Long MODULES_SUPERMARKET_ID = 8L;

    public List<PartFinishingOrder> getOrdersForWorkstation(Long workstationId) {
        return partFinishingOrderRepository.findByWorkstationId(workstationId);
    }

    public List<PartFinishingOrder> getOrdersByControlOrder(Long productionControlOrderId) {
        return partFinishingOrderRepository.findByProductionControlOrderId(productionControlOrderId);
    }

    public PartFinishingOrder getOrderById(Long id) {
        return partFinishingOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part finishing order not found: " + id));
    }

    @Transactional
    public PartFinishingOrder startOrder(Long orderId) {
        PartFinishingOrder order = getOrderById(orderId);
        
        if (!"PENDING".equals(order.getStatus()) && !"WAITING_FOR_PARTS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only start PENDING or WAITING_FOR_PARTS orders. Current status: " + order.getStatus());
        }

        order.setStatus("IN_PROGRESS");
        order.setActualStartTime(LocalDateTime.now());
        
        PartFinishingOrder saved = partFinishingOrderRepository.save(order);
        log.info("Started part finishing order: {} at WS-3", order.getOrderNumber());
        
        return saved;
    }

    @Transactional
    public PartFinishingOrder completeOrder(Long orderId) {
        PartFinishingOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only complete IN_PROGRESS orders. Current status: " + order.getStatus());
        }

        // Credit inventory
        creditInventory(order);

        // Update order status
        order.setStatus("COMPLETED");
        order.setActualFinishTime(LocalDateTime.now());
        PartFinishingOrder saved = partFinishingOrderRepository.save(order);

        log.info("Completed part finishing order: {} - {} {} produced", 
                order.getOrderNumber(), order.getQuantity(), order.getOutputPartName());

        // Propagate status to parent control order
        propagateStatusToParent(order.getProductionControlOrderId());

        return saved;
    }

    @Transactional
    public PartFinishingOrder haltOrder(Long orderId) {
        PartFinishingOrder order = getOrderById(orderId);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new IllegalStateException("Can only halt IN_PROGRESS orders");
        }

        order.setStatus("HALTED");
        PartFinishingOrder saved = partFinishingOrderRepository.save(order);
        
        log.info("Halted part finishing order: {}", order.getOrderNumber());
        return saved;
    }

    @Transactional
    public PartFinishingOrder resumeOrder(Long orderId) {
        PartFinishingOrder order = getOrderById(orderId);
        
        if (!"HALTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only resume HALTED orders");
        }

        order.setStatus("IN_PROGRESS");
        PartFinishingOrder saved = partFinishingOrderRepository.save(order);
        
        log.info("Resumed part finishing order: {}", order.getOrderNumber());
        return saved;
    }

    /**
     * Mark order as waiting for parts (after supply order created)
     */
    @Transactional
    public PartFinishingOrder markWaitingForParts(Long orderId, Long supplyOrderId) {
        PartFinishingOrder order = getOrderById(orderId);
        
        order.setStatus("WAITING_FOR_PARTS");
        order.setSupplyOrderId(supplyOrderId);
        
        PartFinishingOrder saved = partFinishingOrderRepository.save(order);
        log.info("Part finishing order {} marked as waiting for parts (Supply Order: {})", 
                order.getOrderNumber(), supplyOrderId);
        
        return saved;
    }

    private void creditInventory(PartFinishingOrder order) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/stock/credit";
            Map<String, Object> request = Map.of(
                    "workstationId", MODULES_SUPERMARKET_ID,
                    "itemType", "PART",
                    "itemId", order.getOutputPartId(),
                    "quantity", order.getQuantity()
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
            long totalOrders = partFinishingOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
            long completedOrders = partFinishingOrderRepository.countByProductionControlOrderIdAndStatus(
                    productionControlOrderId, "COMPLETED");

            log.info("ProductionControlOrder {} progress: {}/{} part finishing orders completed", 
                    productionControlOrderId, completedOrders, totalOrders);

            if (completedOrders == totalOrders && totalOrders > 0) {
                ProductionControlOrder parent = productionControlOrderRepository.findById(productionControlOrderId)
                        .orElseThrow(() -> new RuntimeException("Parent control order not found"));

                parent.setStatus("COMPLETED");
                parent.setActualFinishTime(LocalDateTime.now());
                productionControlOrderRepository.save(parent);

                log.info("Auto-completed ProductionControlOrder {} - all part finishing finished", 
                        parent.getControlOrderNumber());
            }

        } catch (Exception e) {
            log.error("Failed to propagate status to parent control order {}: {}", 
                    productionControlOrderId, e.getMessage());
        }
    }
}
