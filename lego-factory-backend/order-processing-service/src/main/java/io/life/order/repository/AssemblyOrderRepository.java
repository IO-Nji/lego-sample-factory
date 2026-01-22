package io.life.order.repository;

import io.life.order.entity.AssemblyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AssemblyOrderRepository
 * 
 * @deprecated As of Phase 6 (January 2026), replaced by workstation-specific repositories:
 * - {@link GearAssemblyOrderRepository} for WS-4
 * - {@link MotorAssemblyOrderRepository} for WS-5
 * - {@link FinalAssemblyOrderRepository} for WS-6
 * 
 * Data access layer for AssemblyOrder entities.
 * Provides queries for workstation-based filtering and parent order relationships.
 */
@Deprecated(since = "Phase 6", forRemoval = false)
@Repository
public interface AssemblyOrderRepository extends JpaRepository<AssemblyOrder, Long> {

    /**
     * Find all assembly orders for a specific workstation
     * Used by assembly operators to see only their assigned orders
     */
    List<AssemblyOrder> findByWorkstationId(Long workstationId);

    /**
     * Find all assembly orders by parent AssemblyControlOrder
     * Used by Assembly Control to monitor child order progress
     */
    List<AssemblyOrder> findByAssemblyControlOrderId(Long assemblyControlOrderId);

    /**
     * Find assembly orders by workstation and status
     * Used for filtering active/pending orders on operator dashboards
     */
    List<AssemblyOrder> findByWorkstationIdAndStatus(Long workstationId, String status);

    /**
     * Find assembly orders by status
     * Used for system-wide monitoring and reporting
     */
    List<AssemblyOrder> findByStatus(String status);

    /**
     * Count completed orders for an assembly control order
     * Used for status propagation logic
     */
    long countByAssemblyControlOrderIdAndStatus(Long assemblyControlOrderId, String status);

    /**
     * Count total orders for an assembly control order
     * Used for completion percentage calculations
     */
    long countByAssemblyControlOrderId(Long assemblyControlOrderId);
}
