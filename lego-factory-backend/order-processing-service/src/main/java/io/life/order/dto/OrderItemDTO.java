package io.life.order.dto;

import io.life.order.annotation.ApiContract;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OrderItemDTO - Represents an item in a customer order.
 * 
 * <p>API Contract:</p>
 * Frontend sends items with 'requestedQuantity', but entity uses 'quantity'.
 * Dual field support ensures backward compatibility.
 * 
 * <p>Field Mappings:</p>
 * - requestedQuantity (API) → quantity (internal)
 * - Both field names accepted via @JsonProperty
 * 
 * <p>Validation (Issue #3 Fix - Feb 4, 2026):</p>
 * - itemType: Required, must be PRODUCT, MODULE, or PART
 * - itemId: Required, must be positive
 * - quantity: Required, 1-10000
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Customer order line item with quantity mapping"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    
    @NotNull(message = "Item type is required")
    @Pattern(regexp = "^(PRODUCT|MODULE|PART)$", message = "Item type must be PRODUCT, MODULE, or PART")
    private String itemType;
    
    @NotNull(message = "Item ID is required")
    @Positive(message = "Item ID must be positive")
    private Long itemId;
    
    // Accept both 'quantity' and 'requestedQuantity' from JSON
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10000, message = "Quantity cannot exceed 10,000")
    private Integer quantity;
    
    @JsonProperty("requestedQuantity")
    private void setRequestedQuantity(Integer requestedQuantity) {
        this.quantity = requestedQuantity;
    }
    
    private Integer fulfilledQuantity;
    
    @jakarta.validation.constraints.Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
