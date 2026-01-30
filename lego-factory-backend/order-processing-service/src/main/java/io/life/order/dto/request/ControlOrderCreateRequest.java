package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified request DTO for creating control orders (Production and Assembly).
 * Used by SimAL integration service to create control orders via REST API.
 * 
 * Replaces inline CreateControlOrderRequest classes in:
 * - ProductionControlOrderController
 * - AssemblyControlOrderController
 * 
 * This DTO supports both Production Control (WS-1,2,3) and Assembly Control (WS-4,5,6)
 * order creation with a unified field set. The `controlOrderType` field determines
 * how the order is processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlOrderCreateRequest {

    /**
     * Type of control order: PRODUCTION or ASSEMBLY
     * Determines which service handles the order creation.
     */
    private String controlOrderType;

    /**
     * Source production order ID that triggered this control order.
     */
    private Long sourceProductionOrderId;

    /**
     * Workstation ID where this order will be executed.
     * WS-1,2,3 for Production Control; WS-4,5,6 for Assembly Control.
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
     * Instructions for the production/assembly operation.
     * For Production Control: productionInstructions
     * For Assembly Control: assemblyInstructions
     */
    private String instructions;

    /**
     * Quality checkpoints to verify during production.
     */
    private String qualityCheckpoints;

    /**
     * Item ID (part or module) being produced/assembled.
     * Required for supply order creation.
     */
    private Long itemId;

    /**
     * Item type: PART or MODULE
     * Required for supply order creation and inventory operations.
     */
    private String itemType;

    /**
     * Quantity to produce/assemble.
     */
    private Integer quantity;

    // === Legacy field mappings for backward compatibility ===
    
    /**
     * Alias for instructions - used by Production Control.
     * @deprecated Use {@link #instructions} instead
     */
    public String getProductionInstructions() {
        return instructions;
    }

    public void setProductionInstructions(String productionInstructions) {
        this.instructions = productionInstructions;
    }

    /**
     * Alias for instructions - used by Assembly Control.
     * @deprecated Use {@link #instructions} instead
     */
    public String getAssemblyInstructions() {
        return instructions;
    }

    public void setAssemblyInstructions(String assemblyInstructions) {
        this.instructions = assemblyInstructions;
    }
}
