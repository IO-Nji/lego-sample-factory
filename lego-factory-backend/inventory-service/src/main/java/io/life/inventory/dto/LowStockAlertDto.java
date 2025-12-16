package io.life.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertDto {
    private Long workstationId; // null means global aggregate
    private String itemType;
    private Long itemId;
    private Integer quantity;
    private Integer threshold;
    private Integer deficit; // threshold - quantity if > 0
}
