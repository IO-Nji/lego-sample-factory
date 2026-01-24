package io.life.order.repository;

import io.life.order.entity.GearAssemblyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GearAssemblyOrderRepository extends JpaRepository<GearAssemblyOrder, Long> {
    
    Optional<GearAssemblyOrder> findByOrderNumber(String orderNumber);
    
    List<GearAssemblyOrder> findByAssemblyControlOrderId(Long assemblyControlOrderId);
    
    List<GearAssemblyOrder> findByStatus(String status);
    
    List<GearAssemblyOrder> findByWorkstationId(Long workstationId);
    
    List<GearAssemblyOrder> findByAssemblyControlOrderIdAndStatus(Long assemblyControlOrderId, String status);
    
    long countByAssemblyControlOrderIdAndStatus(Long assemblyControlOrderId, String status);
    
    List<GearAssemblyOrder> findBySupplyOrderId(Long supplyOrderId);
}
