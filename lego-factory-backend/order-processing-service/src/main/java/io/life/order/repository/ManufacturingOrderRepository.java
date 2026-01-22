package io.life.order.repository;

import io.life.order.entity.ManufacturingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ManufacturingOrderRepository
 * 
 * @deprecated As of Phase 6 (January 2026), replaced by workstation-specific repositories:
 * - {@link InjectionMoldingOrderRepository} for WS-1
 * - {@link PartPreProductionOrderRepository} for WS-2
 * - {@link PartFinishingOrderRepository} for WS-3
 * 
 * Data access layer for ManufacturingOrder entities.
 * Provides queries for workstation-based filtering and parent order relationships.
 */
@Deprecated(since = "Phase 6", forRemoval = false)
@Repository
public interface ManufacturingOrderRepository extends JpaRepository<ManufacturingOrder, Long> {

    /**
     * Find all manufacturing orders for a specific workstation
     * Used by manufacturing operators to see only their assigned orders
     */
    List<ManufacturingOrder> findByWorkstationId(Long workstationId);

    /**
     * Find all manufacturing orders by parent ProductionControlOrder
     * Used by Production Control to monitor child order progress
     */
    List<ManufacturingOrder> findByProductionControlOrderId(Long productionControlOrderId);

    /**
     * Find manufacturing orders by workstation and status
     * Used for filtering active/pending orders on operator dashboards
     */
    List<ManufacturingOrder> findByWorkstationIdAndStatus(Long workstationId, String status);

    /**
     * Find manufacturing orders by status
     * Used for system-wide monitoring and reporting
     */
    List<ManufacturingOrder> findByStatus(String status);

    /**
     * Count completed orders for a production control order
     * Used for status propagation logic
     */
    long countByProductionControlOrderIdAndStatus(Long productionControlOrderId, String status);

    /**
     * Count total orders for a production control order
     * Used for completion percentage calculations
     */
    long countByProductionControlOrderId(Long productionControlOrderId);
}
