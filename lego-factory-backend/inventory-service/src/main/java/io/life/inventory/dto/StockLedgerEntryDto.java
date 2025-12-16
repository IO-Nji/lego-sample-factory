package io.life.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLedgerEntryDto {
    private Long id;
    private Long workstationId;
    private String itemType;
    private Long itemId;
    private Integer delta;
    private Integer balanceAfter;
    private String reasonCode;
    private String notes;
    private LocalDateTime createdAt;
}
