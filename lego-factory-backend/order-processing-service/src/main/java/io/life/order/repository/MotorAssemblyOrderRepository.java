package io.life.order.repository;

import io.life.order.entity.MotorAssemblyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotorAssemblyOrderRepository extends JpaRepository<MotorAssemblyOrder, Long> {
    
    Optional<MotorAssemblyOrder> findByOrderNumber(String orderNumber);
    
    List<MotorAssemblyOrder> findByAssemblyControlOrderId(Long assemblyControlOrderId);
    
    List<MotorAssemblyOrder> findByStatus(String status);
    
    List<MotorAssemblyOrder> findByWorkstationId(Long workstationId);
    
    List<MotorAssemblyOrder> findByAssemblyControlOrderIdAndStatus(Long assemblyControlOrderId, String status);
    
    long countByAssemblyControlOrderIdAndStatus(Long assemblyControlOrderId, String status);
    
    List<MotorAssemblyOrder> findBySupplyOrderId(Long supplyOrderId);
}
