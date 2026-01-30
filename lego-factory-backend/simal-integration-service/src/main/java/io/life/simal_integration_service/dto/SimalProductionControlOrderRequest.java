package io.life.simal_integration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Typed DTO for creating a ProductionControlOrder via REST API.
 * Replaces Map<String, Object> in ControlOrderIntegrationService.createProductionControlOrder().
 * 
 * This DTO matches the contract expected by order-processing-service at:
 * POST /api/production-control-orders
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
public class SimalProductionControlOrderRequest {

    /**
     * Source production order ID that this control order is part of.
     */
    private Long sourceProductionOrderId;

    /**
     * Workstation ID where this order will be executed (1, 2, or 3 for manufacturing).
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
     * Instructions for the production operation.
     */
    private String productionInstructions;

    /**
     * Quality checkpoints to verify during production.
     */
    private String qualityCheckpoints;

    /**
     * Safety procedures for the workstation.
     */
    private String safetyProcedures;

    /**
     * Item ID (part) being produced.
     */
    private Long itemId;

    /**
     * Item type: PART for manufacturing workstations.
     */
    private String itemType;

    /**
     * Quantity to produce.
     */
    private Integer quantity;
}
