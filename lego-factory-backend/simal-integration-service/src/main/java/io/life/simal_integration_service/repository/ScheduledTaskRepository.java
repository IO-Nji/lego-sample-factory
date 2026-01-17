package io.life.simal_integration_service.repository;

import io.life.simal_integration_service.entity.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {
    Optional<ScheduledTask> findByTaskId(String taskId);
    List<ScheduledTask> findByWorkstationId(String workstationId);
    List<ScheduledTask> findByStatus(String status);
    List<ScheduledTask> findByManuallyAdjusted(Boolean manuallyAdjusted);
}
