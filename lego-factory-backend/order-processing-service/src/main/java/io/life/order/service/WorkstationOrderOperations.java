package io.life.order.service;

import java.util.List;
import java.util.Optional;

/**
 * Common interface for workstation control order operations.
 * Implemented by ProductionControlOrderService and AssemblyControlOrderService.
 * 
 * This interface enables generic workstation controllers to work with either
 * production (WS-1,2,3) or assembly (WS-4,5,6) control orders.
 * 
 * @param <T> The DTO type (ProductionControlOrderDTO or AssemblyControlOrderDTO)
 */
public interface WorkstationOrderOperations<T> {

    /**
     * Get all orders for a specific workstation.
     * @param workstationId The workstation ID
     * @return List of orders assigned to the workstation
     */
    List<T> getOrdersByWorkstation(Long workstationId);

    /**
     * Get active (IN_PROGRESS) orders for a workstation.
     * @param workstationId The workstation ID
     * @return List of active orders
     */
    List<T> getActiveOrdersByWorkstation(Long workstationId);

    /**
     * Get unassigned (ASSIGNED status) orders for a workstation.
     * @param workstationId The workstation ID
     * @return List of unassigned orders
     */
    List<T> getUnassignedOrders(Long workstationId);

    /**
     * Get an order by its ID.
     * @param id The order ID
     * @return Optional containing the order if found
     */
    Optional<T> getOrderById(Long id);

    /**
     * Get an order by its order number.
     * @param orderNumber The control order number
     * @return Optional containing the order if found
     */
    Optional<T> getOrderByNumber(String orderNumber);

    /**
     * Start work on an order (transitions to IN_PROGRESS).
     * @param id The order ID
     * @return The updated order DTO
     */
    T startWork(Long id);

    /**
     * Complete work on an order (transitions to COMPLETED).
     * Handles inventory credits and SimAL notifications.
     * @param id The order ID
     * @return The updated order DTO
     */
    T completeWork(Long id);

    /**
     * Halt work on an order (transitions to HALTED).
     * @param id The order ID
     * @param reason Optional reason for halting
     * @return The updated order DTO
     */
    T haltWork(Long id, String reason);

    /**
     * Update operator notes on an order.
     * @param id The order ID
     * @param notes The notes to set
     * @return The updated order DTO
     */
    T updateOperatorNotes(Long id, String notes);
}
