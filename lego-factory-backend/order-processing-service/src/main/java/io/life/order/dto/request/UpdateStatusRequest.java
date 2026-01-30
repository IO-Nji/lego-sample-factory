package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating order status.
 * Generic status update request used across multiple order types.
 * 
 * Extracted from ProductionOrderController.UpdateStatusRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    /**
     * New status value to set.
     * Valid values depend on order type (PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, etc.)
     */
    private String status;
}
