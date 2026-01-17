package io.life.simal_integration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
