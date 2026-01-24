package io.life.order.repository;

import io.life.order.entity.WarehouseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseOrderRepository extends JpaRepository<WarehouseOrder, Long> {
    Optional<WarehouseOrder> findByOrderNumber(String orderNumber);
    List<WarehouseOrder> findByWorkstationId(Long workstationId);
    List<WarehouseOrder> findByStatus(String status);
    List<WarehouseOrder> findByCustomerOrderId(Long customerOrderId);
}
