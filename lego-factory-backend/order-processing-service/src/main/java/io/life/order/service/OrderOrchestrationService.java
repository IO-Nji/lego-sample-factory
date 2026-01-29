package io.life.order.service;

import io.life.order.entity.*;
import io.life.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderOrchestrationService
 * 
 * Centralized service for managing order flow orchestration and status propagation.
 * Handles both downward dispatch and upward completion notifications across the order hierarchy.
 * 
 * ORDER HIERARCHY:
 * CustomerOrder → WarehouseOrder → ProductionOrder → Control Orders → Workstation Orders
 * 
 * WORKSTATION ORDER TYPES:
 * - Production Control (ProductionControlOrder) manages:
 *   - InjectionMoldingOrder (WS-1)
 *   - PartPreProductionOrder (WS-2)
 *   - PartFinishingOrder (WS-3)
 * 
 * - Assembly Control (AssemblyControlOrder) manages:
 *   - GearAssemblyOrder (WS-4)
 *   - MotorAssemblyOrder (WS-5)
 *   - FinalAssemblyOrder (WS-6)
 * 
 * This service centralizes the duplicated propagateStatusToParent logic that was
 * previously scattered across 7+ workstation order services.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderOrchestrationService {

    private final ProductionControlOrderRepository productionControlOrderRepository;
    private final AssemblyControlOrderRepository assemblyControlOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final WarehouseOrderRepository warehouseOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;
    
    // Workstation order repositories for counting completion
    private final InjectionMoldingOrderRepository injectionMoldingOrderRepository;
    private final PartPreProductionOrderRepository partPreProductionOrderRepository;
    private final PartFinishingOrderRepository partFinishingOrderRepository;
    private final GearAssemblyOrderRepository gearAssemblyOrderRepository;
    private final MotorAssemblyOrderRepository motorAssemblyOrderRepository;
    private final FinalAssemblyOrderRepository finalAssemblyOrderRepository;

    // ========================
    // STATUS CONSTANTS
    // ========================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FULFILLED = "FULFILLED";
    public static final String STATUS_HALTED = "HALTED";
    public static final String STATUS_WAITING_FOR_PARTS = "WAITING_FOR_PARTS";
    public static final String STATUS_AWAITING_PRODUCTION = "AWAITING_PRODUCTION";

    // ========================
    // WORKSTATION ORDER TYPE ENUM
    // ========================
    public enum WorkstationOrderType {
        INJECTION_MOLDING,      // WS-1
        PART_PRE_PRODUCTION,    // WS-2
        PART_FINISHING,         // WS-3
        GEAR_ASSEMBLY,          // WS-4
        MOTOR_ASSEMBLY,         // WS-5
        FINAL_ASSEMBLY          // WS-6
    }

    public enum ControlOrderType {
        PRODUCTION_CONTROL,     // Manages WS-1, WS-2, WS-3
        ASSEMBLY_CONTROL        // Manages WS-4, WS-5, WS-6
    }

    // ========================
    // UPWARD NOTIFICATION METHODS
    // ========================

    /**
     * Notify that a workstation order has completed.
     * Checks if all sibling orders are complete and propagates to parent control order.
     * 
     * @param orderType The type of workstation order
     * @param controlOrderId The parent control order ID
     */
    @Transactional
    public void notifyWorkstationOrderComplete(WorkstationOrderType orderType, Long controlOrderId) {
        log.info("Workstation order completed - Type: {}, ControlOrderId: {}", orderType, controlOrderId);

        switch (orderType) {
            case INJECTION_MOLDING:
                checkProductionControlOrderCompletion(controlOrderId, orderType);
                break;
            case PART_PRE_PRODUCTION:
                checkProductionControlOrderCompletion(controlOrderId, orderType);
                break;
            case PART_FINISHING:
                checkProductionControlOrderCompletion(controlOrderId, orderType);
                break;
            case GEAR_ASSEMBLY:
                checkAssemblyControlOrderCompletion(controlOrderId, orderType);
                break;
            case MOTOR_ASSEMBLY:
                checkAssemblyControlOrderCompletion(controlOrderId, orderType);
                break;
            case FINAL_ASSEMBLY:
                checkAssemblyControlOrderCompletion(controlOrderId, orderType);
                break;
            default:
                log.warn("Unknown workstation order type: {}", orderType);
        }
    }

    /**
     * Check if all workstation orders under a Production Control Order are complete.
     * If so, mark the control order as complete and propagate upward.
     */
    @Transactional
    public void checkProductionControlOrderCompletion(Long productionControlOrderId, WorkstationOrderType orderType) {
        try {
            long totalOrders;
            long completedOrders;

            switch (orderType) {
                case INJECTION_MOLDING:
                    totalOrders = injectionMoldingOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
                    completedOrders = injectionMoldingOrderRepository.countByProductionControlOrderIdAndStatus(
                            productionControlOrderId, STATUS_COMPLETED);
                    break;
                case PART_PRE_PRODUCTION:
                    totalOrders = partPreProductionOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
                    completedOrders = partPreProductionOrderRepository.countByProductionControlOrderIdAndStatus(
                            productionControlOrderId, STATUS_COMPLETED);
                    break;
                case PART_FINISHING:
                    totalOrders = partFinishingOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
                    completedOrders = partFinishingOrderRepository.countByProductionControlOrderIdAndStatus(
                            productionControlOrderId, STATUS_COMPLETED);
                    break;
                default:
                    log.error("Invalid order type for production control: {}", orderType);
                    return;
            }

            log.info("ProductionControlOrder {} progress: {}/{} {} orders completed",
                    productionControlOrderId, completedOrders, totalOrders, orderType);

            if (completedOrders == totalOrders && totalOrders > 0) {
                completeProductionControlOrder(productionControlOrderId);
            }

        } catch (Exception e) {
            log.error("Failed to check production control order completion {}: {}",
                    productionControlOrderId, e.getMessage(), e);
        }
    }

    /**
     * Check if all workstation orders under an Assembly Control Order are complete.
     * If so, mark the control order as complete and propagate upward.
     */
    @Transactional
    public void checkAssemblyControlOrderCompletion(Long assemblyControlOrderId, WorkstationOrderType orderType) {
        try {
            long totalOrders;
            long completedOrders;

            switch (orderType) {
                case GEAR_ASSEMBLY:
                    totalOrders = gearAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
                    completedOrders = gearAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                            assemblyControlOrderId, STATUS_COMPLETED);
                    break;
                case MOTOR_ASSEMBLY:
                    totalOrders = motorAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
                    completedOrders = motorAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                            assemblyControlOrderId, STATUS_COMPLETED);
                    break;
                case FINAL_ASSEMBLY:
                    totalOrders = finalAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
                    completedOrders = finalAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                            assemblyControlOrderId, STATUS_COMPLETED);
                    break;
                default:
                    log.error("Invalid order type for assembly control: {}", orderType);
                    return;
            }

            log.info("AssemblyControlOrder {} progress: {}/{} {} orders completed",
                    assemblyControlOrderId, completedOrders, totalOrders, orderType);

            if (completedOrders == totalOrders && totalOrders > 0) {
                completeAssemblyControlOrder(assemblyControlOrderId);
            }

        } catch (Exception e) {
            log.error("Failed to check assembly control order completion {}: {}",
                    assemblyControlOrderId, e.getMessage(), e);
        }
    }

    /**
     * Mark a Production Control Order as complete and propagate to Production Order.
     */
    @Transactional
    public void completeProductionControlOrder(Long productionControlOrderId) {
        ProductionControlOrder controlOrder = productionControlOrderRepository.findById(productionControlOrderId)
                .orElseThrow(() -> new RuntimeException("Production control order not found: " + productionControlOrderId));

        if (STATUS_COMPLETED.equals(controlOrder.getStatus())) {
            log.info("ProductionControlOrder {} already completed, skipping", productionControlOrderId);
            return;
        }

        controlOrder.setStatus(STATUS_COMPLETED);
        controlOrder.setActualFinishTime(LocalDateTime.now());
        productionControlOrderRepository.save(controlOrder);

        log.info("Auto-completed ProductionControlOrder {} - all workstation orders finished",
                controlOrder.getControlOrderNumber());

        // Propagate to Production Order if linked
        if (controlOrder.getSourceProductionOrderId() != null) {
            notifyControlOrderComplete(ControlOrderType.PRODUCTION_CONTROL, controlOrder.getSourceProductionOrderId());
        }
    }

    /**
     * Mark an Assembly Control Order as complete and propagate to Production Order.
     */
    @Transactional
    public void completeAssemblyControlOrder(Long assemblyControlOrderId) {
        AssemblyControlOrder controlOrder = assemblyControlOrderRepository.findById(assemblyControlOrderId)
                .orElseThrow(() -> new RuntimeException("Assembly control order not found: " + assemblyControlOrderId));

        if (STATUS_COMPLETED.equals(controlOrder.getStatus())) {
            log.info("AssemblyControlOrder {} already completed, skipping", assemblyControlOrderId);
            return;
        }

        controlOrder.setStatus(STATUS_COMPLETED);
        controlOrder.setActualFinishTime(LocalDateTime.now());
        assemblyControlOrderRepository.save(controlOrder);

        log.info("Auto-completed AssemblyControlOrder {} - all workstation orders finished",
                controlOrder.getControlOrderNumber());

        // Propagate to Production Order if linked
        if (controlOrder.getSourceProductionOrderId() != null) {
            notifyControlOrderComplete(ControlOrderType.ASSEMBLY_CONTROL, controlOrder.getSourceProductionOrderId());
        }
    }

    /**
     * Notify that a control order has completed.
     * Checks if all control orders under a Production Order are complete.
     */
    @Transactional
    public void notifyControlOrderComplete(ControlOrderType controlType, Long productionOrderId) {
        log.info("Control order completed - Type: {}, ProductionOrderId: {}", controlType, productionOrderId);

        try {
            // Count all control orders for this production order
            List<ProductionControlOrder> prodControlOrders = productionControlOrderRepository.findBySourceProductionOrderId(productionOrderId);
            List<AssemblyControlOrder> asmControlOrders = assemblyControlOrderRepository.findBySourceProductionOrderId(productionOrderId);

            long totalControlOrders = prodControlOrders.size() + asmControlOrders.size();
            long completedControlOrders = 
                    prodControlOrders.stream().filter(o -> STATUS_COMPLETED.equals(o.getStatus())).count() +
                    asmControlOrders.stream().filter(o -> STATUS_COMPLETED.equals(o.getStatus())).count();

            log.info("ProductionOrder {} progress: {}/{} control orders completed",
                    productionOrderId, completedControlOrders, totalControlOrders);

            if (completedControlOrders == totalControlOrders && totalControlOrders > 0) {
                completeProductionOrder(productionOrderId);
            }

        } catch (Exception e) {
            log.error("Failed to check production order completion {}: {}",
                    productionOrderId, e.getMessage(), e);
        }
    }

    /**
     * Mark a Production Order as complete and propagate to Warehouse Order.
     */
    @Transactional
    public void completeProductionOrder(Long productionOrderId) {
        ProductionOrder productionOrder = productionOrderRepository.findById(productionOrderId)
                .orElseThrow(() -> new RuntimeException("Production order not found: " + productionOrderId));

        if (STATUS_COMPLETED.equals(productionOrder.getStatus())) {
            log.info("ProductionOrder {} already completed, skipping", productionOrderId);
            return;
        }

        productionOrder.setStatus(STATUS_COMPLETED);
        productionOrder.setActualCompletionTime(LocalDateTime.now());
        productionOrderRepository.save(productionOrder);

        log.info("Auto-completed ProductionOrder {} - all control orders finished",
                productionOrder.getProductionOrderNumber());

        // Propagate to Warehouse Order if linked
        if (productionOrder.getSourceWarehouseOrderId() != null) {
            notifyProductionOrderComplete(productionOrder.getSourceWarehouseOrderId());
        }
    }

    /**
     * Notify that production has completed for a Warehouse Order.
     * The Warehouse Order can now be fulfilled.
     */
    @Transactional
    public void notifyProductionOrderComplete(Long warehouseOrderId) {
        log.info("Production completed for WarehouseOrder {}", warehouseOrderId);

        try {
            WarehouseOrder warehouseOrder = warehouseOrderRepository.findById(warehouseOrderId)
                    .orElseThrow(() -> new RuntimeException("Warehouse order not found: " + warehouseOrderId));

            // Update warehouse order status to indicate production complete
            // Note: Actual fulfillment still requires manual action at Modules Supermarket
            if (STATUS_AWAITING_PRODUCTION.equals(warehouseOrder.getStatus())) {
                warehouseOrder.setStatus(STATUS_CONFIRMED);
                warehouseOrder.setTriggerScenario("DIRECT_FULFILLMENT");
                warehouseOrderRepository.save(warehouseOrder);

                log.info("WarehouseOrder {} updated - production complete, ready for fulfillment",
                        warehouseOrder.getOrderNumber());
            }

            // Notify Customer Order if exists
            if (warehouseOrder.getCustomerOrderId() != null) {
                notifyWarehouseOrderReady(warehouseOrder.getCustomerOrderId());
            }

        } catch (Exception e) {
            log.error("Failed to update warehouse order on production completion {}: {}",
                    warehouseOrderId, e.getMessage(), e);
        }
    }

    /**
     * Notify that a Warehouse Order is ready (either fulfilled or production complete).
     * Updates Customer Order status accordingly.
     */
    @Transactional
    public void notifyWarehouseOrderReady(Long customerOrderId) {
        log.info("Warehouse order ready for CustomerOrder {}", customerOrderId);

        try {
            CustomerOrder customerOrder = customerOrderRepository.findById(customerOrderId)
                    .orElseThrow(() -> new RuntimeException("Customer order not found: " + customerOrderId));

            // Log for visibility - actual customer order completion requires explicit fulfillment
            log.info("CustomerOrder {} - warehouse order ready, pending final fulfillment",
                    customerOrder.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to notify customer order {}: {}",
                    customerOrderId, e.getMessage(), e);
        }
    }

    // ========================
    // PROGRESS QUERY METHODS
    // ========================

    /**
     * Get completion progress for a Production Control Order.
     * 
     * @param productionControlOrderId The control order ID
     * @return Progress as a map with totalOrders and completedOrders
     */
    public OrderProgress getProductionControlOrderProgress(Long productionControlOrderId) {
        long injectionTotal = injectionMoldingOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
        long injectionCompleted = injectionMoldingOrderRepository.countByProductionControlOrderIdAndStatus(
                productionControlOrderId, STATUS_COMPLETED);

        long preProductionTotal = partPreProductionOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
        long preProductionCompleted = partPreProductionOrderRepository.countByProductionControlOrderIdAndStatus(
                productionControlOrderId, STATUS_COMPLETED);

        long finishingTotal = partFinishingOrderRepository.findByProductionControlOrderId(productionControlOrderId).size();
        long finishingCompleted = partFinishingOrderRepository.countByProductionControlOrderIdAndStatus(
                productionControlOrderId, STATUS_COMPLETED);

        long total = injectionTotal + preProductionTotal + finishingTotal;
        long completed = injectionCompleted + preProductionCompleted + finishingCompleted;

        return new OrderProgress(total, completed);
    }

    /**
     * Get completion progress for an Assembly Control Order.
     * 
     * @param assemblyControlOrderId The control order ID
     * @return Progress as a map with totalOrders and completedOrders
     */
    public OrderProgress getAssemblyControlOrderProgress(Long assemblyControlOrderId) {
        long gearTotal = gearAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
        long gearCompleted = gearAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                assemblyControlOrderId, STATUS_COMPLETED);

        long motorTotal = motorAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
        long motorCompleted = motorAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                assemblyControlOrderId, STATUS_COMPLETED);

        long finalTotal = finalAssemblyOrderRepository.findByAssemblyControlOrderId(assemblyControlOrderId).size();
        long finalCompleted = finalAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                assemblyControlOrderId, STATUS_COMPLETED);

        long total = gearTotal + motorTotal + finalTotal;
        long completed = gearCompleted + motorCompleted + finalCompleted;

        return new OrderProgress(total, completed);
    }

    /**
     * Get completion progress for a Production Order (all control orders).
     * 
     * @param productionOrderId The production order ID
     * @return Progress as a map with totalOrders and completedOrders
     */
    public OrderProgress getProductionOrderProgress(Long productionOrderId) {
        List<ProductionControlOrder> prodControlOrders = productionControlOrderRepository.findBySourceProductionOrderId(productionOrderId);
        List<AssemblyControlOrder> asmControlOrders = assemblyControlOrderRepository.findBySourceProductionOrderId(productionOrderId);

        long total = prodControlOrders.size() + asmControlOrders.size();
        long completed = 
                prodControlOrders.stream().filter(o -> STATUS_COMPLETED.equals(o.getStatus())).count() +
                asmControlOrders.stream().filter(o -> STATUS_COMPLETED.equals(o.getStatus())).count();

        return new OrderProgress(total, completed);
    }

    // ========================
    // INNER CLASS FOR PROGRESS
    // ========================

    public static class OrderProgress {
        private final long totalOrders;
        private final long completedOrders;

        public OrderProgress(long totalOrders, long completedOrders) {
            this.totalOrders = totalOrders;
            this.completedOrders = completedOrders;
        }

        public long getTotalOrders() {
            return totalOrders;
        }

        public long getCompletedOrders() {
            return completedOrders;
        }

        public double getPercentComplete() {
            return totalOrders > 0 ? (double) completedOrders / totalOrders * 100.0 : 0.0;
        }

        public boolean isComplete() {
            return totalOrders > 0 && completedOrders == totalOrders;
        }

        @Override
        public String toString() {
            return String.format("%d/%d (%.1f%%)", completedOrders, totalOrders, getPercentComplete());
        }
    }
}
