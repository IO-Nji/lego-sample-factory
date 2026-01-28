package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an assembly control order from SimAL schedule data.
 * Used by SimAL integration service when scheduling assembly workstations (WS-4, WS-5, WS-6).
 * 
 * Replaces inline CreateControlOrderRequest in AssemblyControlOrderController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyControlOrderCreateRequest {

    /**
     * Source production order ID that this control order is part of.
     */
    private Long sourceProductionOrderId;

    /**
     * Workstation ID where this order will be executed (WS-4, WS-5, or WS-6).
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
     * Instructions for the assembly operation.
     */
    private String assemblyInstructions;

    /**
     * Quality checkpoints to verify during assembly.
     */
    private String qualityCheckpoints;

    /**
     * Item ID (module) being assembled.
     */
    private Long itemId;

    /**
     * Item type: typically MODULE for assembly control.
     */
    private String itemType;

    /**
     * Quantity to assemble.
     */
    private Integer quantity;
}
