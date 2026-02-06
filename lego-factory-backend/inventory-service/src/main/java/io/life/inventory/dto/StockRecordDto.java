package io.life.inventory.dto;

import io.life.inventory.annotation.ApiContract;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * StockRecordDto - Inventory stock levels by workstation.
 * 
 * API Contract: Frontend queries stock via GET /api/inventory
 * Also used by order-service for stock validation.
 * 
 * <p>Validation (Issue #3 Fix - Feb 4, 2026):</p>
 * - workstationId: Required, 1-9
 * - itemType: Required, PRODUCT/MODULE/PART
 * - itemId: Required, positive
 * - quantity: Required, non-negative (0-999999)
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
	
	@NotNull(message = "Workstation ID is required")
	@Min(value = 1, message = "Workstation ID must be between 1 and 9")
	@Max(value = 9, message = "Workstation ID must be between 1 and 9")
	private Long workstationId;
	
	@NotNull(message = "Item type is required")
	@Pattern(regexp = "^(PRODUCT|MODULE|PART)$", message = "Item type must be PRODUCT, MODULE, or PART")
	private String itemType;
	
	@NotNull(message = "Item ID is required")
	@Positive(message = "Item ID must be positive")
	private Long itemId;
	
	@Size(max = 200, message = "Item name cannot exceed 200 characters")
	private String itemName; // Name fetched from masterdata-service
	
	@NotNull(message = "Quantity is required")
	@Min(value = 0, message = "Quantity cannot be negative")
	@Max(value = 999999, message = "Quantity cannot exceed 999,999")
	private Integer quantity;
	
	private LocalDateTime lastUpdated;

}
