package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for control order completion notification.
 * Used to notify a production order that one of its child control orders has completed.
 * 
 * Extracted from ProductionOrderController.ControlCompletionRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlCompletionRequest {

    /**
     * ID of the control order that has completed.
     */
    private Long controlOrderId;
}
