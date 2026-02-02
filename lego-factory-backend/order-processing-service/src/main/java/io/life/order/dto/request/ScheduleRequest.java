package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for scheduling with SimAL.
 * Used to associate a production order with a SimAL schedule and update status.
 * 
 * Extracted from ProductionOrderController.ScheduleRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    /**
     * SimAL schedule ID to associate with the order.
     */
    private String simalScheduleId;

    /**
     * New status to set (typically "SCHEDULED" or "PLANNED").
     */
    private String status;
}
