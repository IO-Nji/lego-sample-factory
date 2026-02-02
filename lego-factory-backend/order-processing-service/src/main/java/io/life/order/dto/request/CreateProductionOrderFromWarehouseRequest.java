package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for creating production orders from warehouse orders.
 * Used in Scenario 3 when modules are not available and production is required.
 * 
 * Extracted from ProductionOrderController.CreateProductionOrderFromWarehouseRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductionOrderFromWarehouseRequest {

    /**
     * Source customer order ID (for traceability).
     */
    private Long sourceCustomerOrderId;

    /**
     * Source warehouse order ID that triggered this production request.
     */
    private Long sourceWarehouseOrderId;

    /**
     * Priority level: LOW, NORMAL, HIGH, URGENT
     */
    private String priority;

    /**
     * Due date for production completion.
     */
    private LocalDateTime dueDate;

    /**
     * Notes or special instructions for production.
     */
    private String notes;

    /**
     * Workstation ID that created this order (for audit trail).
     */
    private Long createdByWorkstationId;

    /**
     * Workstation ID to assign production to.
     */
    private Long assignedWorkstationId;
}
