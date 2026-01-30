package io.life.order.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StockLevelResponse - Response DTO for inventory stock queries.
 * 
 * Used by InventoryClient to provide compile-time type safety
 * instead of Map<String, Object> responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelResponse {

    /** Workstation ID */
    private Long workstationId;
    
    /** Item type: PRODUCT, MODULE, or PART */
    private String itemType;
    
    /** Item ID */
    private Long itemId;
    
    /** Current quantity available */
    private Integer quantity;
    
    /** Item name (optional, for display) */
    private String itemName;
}
