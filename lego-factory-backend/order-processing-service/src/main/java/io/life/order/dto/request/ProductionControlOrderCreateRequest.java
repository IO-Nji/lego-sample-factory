package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a production control order from SimAL schedule data.
 * Used by SimAL integration service when scheduling production workstations (WS-1, WS-2, WS-3).
 * 
 * Replaces inline CreateControlOrderRequest in ProductionControlOrderController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionControlOrderCreateRequest {

    /**
     * Source production order ID that this control order is part of.
     */
    private Long sourceProductionOrderId;

    /**
     * Workstation ID where this order will be executed (WS-1, WS-2, or WS-3).
     */
    private Long assignedWorkstationId;

    /**
     * SimAL schedule ID for tracking in the scheduling system.
     */
    private String simalScheduleId;

    /**
     * Target start time (ISO-8601 format string).
     */
    private String targetStartTime;

    /**
     * Target completion time (ISO-8601 format string).
     */
    private String targetCompletionTime;

    /**
     * Priority level: LOW, NORMAL, HIGH, URGENT
     */
    private String priority;

    /**
     * Instructions for the production operation.
     */
    private String productionInstructions;

    /**
     * Quality checkpoints to verify during production.
     */
    private String qualityCheckpoints;

    /**
     * Safety procedures to follow during production.
     */
    private String safetyProcedures;

    /**
     * Estimated duration in minutes.
     */
    private Integer estimatedDurationMinutes;

    /**
     * Item ID (part) being produced.
     */
    private Long itemId;

    /**
     * Item type: typically PART for production control.
     */
    private String itemType;

    /**
     * Quantity to produce.
     */
    private Integer quantity;
}
