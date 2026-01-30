package io.life.simal_integration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Typed DTO for creating an AssemblyControlOrder via REST API.
 * Replaces Map<String, Object> in ControlOrderIntegrationService.createAssemblyControlOrder().
 * 
 * This DTO matches the contract expected by order-processing-service at:
 * POST /api/assembly-control-orders
 * 
 * Using typed DTOs provides:
 * - Compile-time validation of field names
 * - IDE autocomplete and refactoring support
 * - Clearer API documentation
 * - Reduced risk of runtime errors from typos in magic strings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimalAssemblyControlOrderRequest {

    /**
     * Source production order ID that this control order is part of.
     */
    private Long sourceProductionOrderId;

    /**
     * Workstation ID where this order will be executed (4, 5, or 6 for assembly).
     */
    private Long assignedWorkstationId;

    /**
     * SimAL schedule ID for tracking in the scheduling system.
     */
    private String simalScheduleId;

    /**
     * Target start time (ISO-8601 format string from SimAL).
     */
    private String targetStartTime;

    /**
     * Target completion time (ISO-8601 format string from SimAL).
     */
    private String targetCompletionTime;

    /**
     * Priority level: LOW, MEDIUM, HIGH, URGENT
     */
    private String priority;

    /**
     * Instructions for the assembly operation.
     */
    private String assemblyInstructions;

    /**
     * Quality checkpoints/standards for assembly verification.
     */
    private String qualityCheckpoints;

    /**
     * Item ID (module) being assembled.
     */
    private Long itemId;

    /**
     * Item type: MODULE for assembly workstations.
     */
    private String itemType;

    /**
     * Quantity to assemble.
     */
    private Integer quantity;
}
