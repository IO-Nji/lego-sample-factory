package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for creating production orders directly from customer orders.
 * Used in Scenario 4 (High Volume) when order quantity >= LOT_SIZE_THRESHOLD,
 * bypassing the warehouse order step entirely.
 * 
 * Extracted from ProductionOrderController.CreateFromCustomerOrderRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFromCustomerOrderRequest {

    /**
     * Customer order ID to create production for.
     * Required - this is the source order for Scenario 4 direct production.
     */
    private Long customerOrderId;

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
