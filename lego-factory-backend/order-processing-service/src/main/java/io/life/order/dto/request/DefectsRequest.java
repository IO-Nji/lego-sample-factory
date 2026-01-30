package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared request DTO for recording defects on orders.
 * Used by production control orders.
 * 
 * Replaces inline DefectsRequest classes in:
 * - ProductionControlOrderController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectsRequest {
    
    /**
     * Number of defective items found during production.
     */
    private Integer defectsFound;
    
    /**
     * Number of defective items that were successfully reworked.
     */
    private Integer defectsReworked;
    
    /**
     * Whether the order requires rework before completion.
     */
    private Boolean reworkRequired;
}
