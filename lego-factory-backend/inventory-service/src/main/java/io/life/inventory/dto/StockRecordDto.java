package io.life.inventory.dto;

import io.life.inventory.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * StockRecordDto - Inventory stock levels by workstation.
 * 
 * API Contract: Frontend queries stock via GET /api/inventory
 * Also used by order-service for stock validation.
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend, order-service",
    description = "Stock levels by workstation, item type, and item ID"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockRecordDto {

	private Long id;
	private Long workstationId;
	private String itemType;
	private Long itemId;
	private String itemName; // Name fetched from masterdata-service
	private Integer quantity;
	private LocalDateTime lastUpdated;

}
