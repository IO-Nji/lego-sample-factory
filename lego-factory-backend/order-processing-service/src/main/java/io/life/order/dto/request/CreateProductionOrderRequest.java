package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for creating standalone production orders.
 * Used when creating a production order from the Production Planning dashboard.
 * 
 * Extracted from ProductionOrderController.CreateProductionOrderRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductionOrderRequest {

    /**
     * Source customer order ID (optional - may be standalone production).
     */
    private Long sourceCustomerOrderId;

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
}
