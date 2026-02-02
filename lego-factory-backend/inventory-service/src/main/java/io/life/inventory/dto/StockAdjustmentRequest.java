package io.life.inventory.dto;

import io.life.inventory.annotation.ApiContract;
import lombok.Data;

/**
 * StockAdjustmentRequest - Credit/debit stock operations.
 * 
 * API Contract: order-service calls POST /api/stock/adjust
 * CRITICAL: delta field is signed integer (positive=credit, negative=debit)
 */
@ApiContract(
    version = "v1",
    externalSource = "order-service",
    description = "Stock adjustment for credit/debit operations"
)
@Data
public class StockAdjustmentRequest {
    private Long workstationId;
    private String itemType;
    private Long itemId;
    private Integer delta; // positive or negative
    private String reasonCode; // e.g. ADJUSTMENT, RECEIPT, CONSUMPTION
    private String notes;
}
