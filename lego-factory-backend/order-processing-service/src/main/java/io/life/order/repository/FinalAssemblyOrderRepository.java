package io.life.order.repository;

import io.life.order.entity.FinalAssemblyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinalAssemblyOrderRepository extends JpaRepository<FinalAssemblyOrder, Long> {
    Optional<FinalAssemblyOrder> findByOrderNumber(String orderNumber);
    List<FinalAssemblyOrder> findByWorkstationId(Long workstationId);
    List<FinalAssemblyOrder> findByStatus(String status);
    List<FinalAssemblyOrder> findByWarehouseOrderId(Long warehouseOrderId);
    List<FinalAssemblyOrder> findByProductionOrderId(Long productionOrderId);  // Scenario 4 support
    List<FinalAssemblyOrder> findByAssemblyControlOrderId(Long assemblyControlOrderId);
    List<FinalAssemblyOrder> findByOutputProductId(Long outputProductId);
    
    // For OrderOrchestrationService
    List<FinalAssemblyOrder> findByAssemblyControlOrderIdAndStatus(Long assemblyControlOrderId, String status);
    long countByAssemblyControlOrderIdAndStatus(Long assemblyControlOrderId, String status);
}
