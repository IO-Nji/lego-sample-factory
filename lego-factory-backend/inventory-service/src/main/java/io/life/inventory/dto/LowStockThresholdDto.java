package io.life.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockThresholdDto {
    private Long id;
    private Long workstationId; // null for global
    private String itemType;
    private Long itemId;
    private Integer threshold;
}
