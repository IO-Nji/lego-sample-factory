package io.life.order.repository;

import io.life.order.entity.InjectionMoldingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InjectionMoldingOrderRepository extends JpaRepository<InjectionMoldingOrder, Long> {
    
    Optional<InjectionMoldingOrder> findByOrderNumber(String orderNumber);
    
    List<InjectionMoldingOrder> findByProductionControlOrderId(Long productionControlOrderId);
    
    List<InjectionMoldingOrder> findByStatus(String status);
    
    List<InjectionMoldingOrder> findByWorkstationId(Long workstationId);
    
    List<InjectionMoldingOrder> findByProductionControlOrderIdAndStatus(Long productionControlOrderId, String status);
    
    long countByProductionControlOrderIdAndStatus(Long productionControlOrderId, String status);
}
