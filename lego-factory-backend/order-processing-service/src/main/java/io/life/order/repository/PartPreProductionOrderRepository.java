package io.life.order.repository;

import io.life.order.entity.PartPreProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartPreProductionOrderRepository extends JpaRepository<PartPreProductionOrder, Long> {
    
    Optional<PartPreProductionOrder> findByOrderNumber(String orderNumber);
    
    List<PartPreProductionOrder> findByProductionControlOrderId(Long productionControlOrderId);
    
    List<PartPreProductionOrder> findByStatus(String status);
    
    List<PartPreProductionOrder> findByWorkstationId(Long workstationId);
    
    List<PartPreProductionOrder> findByProductionControlOrderIdAndStatus(Long productionControlOrderId, String status);
    
    long countByProductionControlOrderIdAndStatus(Long productionControlOrderId, String status);
    
    List<PartPreProductionOrder> findBySupplyOrderId(Long supplyOrderId);
}
