package io.life.order.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StockAdjustmentRequest - Request DTO for inventory stock adjustments.
 * 
 * Used by InventoryClient to provide compile-time type safety
 * instead of Map<String, Object> requests.
 * 
 * DELTA CONVENTION:
 * - Positive delta = credit (add to inventory)
 * - Negative delta = debit (remove from inventory)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    /** Target workstation ID (7=Plant, 8=Modules, 9=Parts) */
    private Long workstationId;
    
    /** Item type: PRODUCT, MODULE, or PART */
    private String itemType;
    
    /** Item ID within the type */
    private Long itemId;
    
    /** Quantity change: positive for credit, negative for debit */
    private Integer delta;
    
    /** Reason code: PRODUCTION, CONSUMPTION, FULFILLMENT, ADJUSTMENT */
    private String reasonCode;
    
    /** Optional notes for audit trail */
    private String notes;
}
