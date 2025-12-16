package io.life.order.repository;

import io.life.order.entity.OrderAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderAuditRepository extends JpaRepository<OrderAudit, Long> {
    List<OrderAudit> findByOrderTypeAndOrderIdOrderByCreatedAtDesc(String orderType, Long orderId);
}
