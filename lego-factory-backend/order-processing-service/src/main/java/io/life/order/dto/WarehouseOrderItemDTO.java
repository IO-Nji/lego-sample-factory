package io.life.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WarehouseOrderItemDTO - Line item for warehouse orders.
 * 
 * <p>Validation (Issue #3 Fix - Feb 4, 2026):</p>
 * - itemId: Required, must be positive
 * - itemType: Required, must be MODULE or PART
 * - requestedQuantity: Required, 1-10000
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseOrderItemDTO {
    private Long id;
    
    @NotNull(message = "Item ID is required")
    @Positive(message = "Item ID must be positive")
    private Long itemId;
    
    @Positive(message = "Product ID must be positive if provided")
    private Long productId; // Track which product this module is for
    
    @Size(max = 200, message = "Item name cannot exceed 200 characters")
    private String itemName;
    
    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Requested quantity must be at least 1")
    @Max(value = 10000, message = "Requested quantity cannot exceed 10,000")
    private Integer requestedQuantity;
    
    @Min(value = 0, message = "Fulfilled quantity cannot be negative")
    private Integer fulfilledQuantity;
    
    @NotNull(message = "Item type is required")
    @Pattern(regexp = "^(MODULE|PART)$", message = "Item type must be MODULE or PART")
    private String itemType;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
