package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for linking a production order to a SimAL schedule.
 * Used when SimAL scheduling service provides schedule data for an existing order.
 * 
 * Extracted from ProductionOrderController.LinkSimalRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkSimalRequest {

    /**
     * SimAL schedule ID to link to this production order.
     */
    private String simalScheduleId;

    /**
     * Estimated duration in minutes from SimAL scheduling.
     */
    private Integer estimatedDuration;

    /**
     * Expected completion time calculated by SimAL.
     */
    private LocalDateTime expectedCompletionTime;
}
