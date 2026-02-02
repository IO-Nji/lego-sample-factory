package io.life.simal_integration_service.dto;

import io.life.simal_integration_service.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ScheduledTaskResponse - SimAL scheduled task for Gantt chart display.
 * 
 * API Contract: Frontend Production Planning dashboard displays these in Gantt chart.
 * Contains task timing, status, and manual adjustment tracking.
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "SimAL scheduled task for Gantt chart visualization"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskResponse {
    private String taskId;
    private String itemId; // Changed from Long to String to match entity
    private String itemName;
    private Integer quantity;
    private String workstationId;
    private String workstationName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String status;
    private Integer sequence;
    private Boolean manuallyAdjusted;
    private String adjustedBy;
    private LocalDateTime adjustedAt;
    private String adjustmentReason;
}
