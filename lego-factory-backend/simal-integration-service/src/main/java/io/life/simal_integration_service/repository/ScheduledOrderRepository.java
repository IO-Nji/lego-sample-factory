package io.life.simal_integration_service.repository;

import io.life.simal_integration_service.entity.ScheduledOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledOrderRepository extends JpaRepository<ScheduledOrder, Long> {
    Optional<ScheduledOrder> findByScheduleId(String scheduleId);
    List<ScheduledOrder> findByStatus(String status);
    Optional<ScheduledOrder> findByOrderNumber(String orderNumber);
    List<ScheduledOrder> findAllByOrderByCreatedAtDesc();
}
