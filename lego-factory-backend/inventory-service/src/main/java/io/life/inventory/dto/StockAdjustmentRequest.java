package io.life.inventory.dto;

import lombok.Data;

@Data
public class StockAdjustmentRequest {
    private Long workstationId;
    private String itemType;
    private Long itemId;
    private Integer delta; // positive or negative
    private String reasonCode; // e.g. ADJUSTMENT, RECEIPT, CONSUMPTION
    private String notes;
}
