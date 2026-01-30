package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for scheduling production (Scenario 3 flow).
 * Contains scheduling parameters including Gantt chart reference.
 * 
 * Extracted from ProductionOrderController.ScheduleProductionRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleProductionRequest {

    /**
     * Scheduled start time for production.
     */
    private LocalDateTime scheduledStartTime;

    /**
     * Scheduled end time for production.
     */
    private LocalDateTime scheduledEndTime;

    /**
     * Reference to the Gantt chart visualization ID.
     */
    private String ganttChartId;
}
