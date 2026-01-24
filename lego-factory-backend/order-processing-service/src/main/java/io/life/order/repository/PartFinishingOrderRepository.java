package io.life.order.repository;

import io.life.order.entity.PartFinishingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartFinishingOrderRepository extends JpaRepository<PartFinishingOrder, Long> {
    
    Optional<PartFinishingOrder> findByOrderNumber(String orderNumber);
    
    List<PartFinishingOrder> findByProductionControlOrderId(Long productionControlOrderId);
    
    List<PartFinishingOrder> findByStatus(String status);
    
    List<PartFinishingOrder> findByWorkstationId(Long workstationId);
    
    List<PartFinishingOrder> findByProductionControlOrderIdAndStatus(Long productionControlOrderId, String status);
    
    long countByProductionControlOrderIdAndStatus(Long productionControlOrderId, String status);
    
    List<PartFinishingOrder> findBySupplyOrderId(Long supplyOrderId);
}
