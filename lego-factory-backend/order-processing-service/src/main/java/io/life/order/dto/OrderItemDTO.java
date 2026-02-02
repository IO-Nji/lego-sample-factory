package io.life.order.dto;

import io.life.order.annotation.ApiContract;
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
    private String itemType;
    private Long itemId;
    
    // Accept both 'quantity' and 'requestedQuantity' from JSON
    private Integer quantity;
    
    @JsonProperty("requestedQuantity")
    private void setRequestedQuantity(Integer requestedQuantity) {
        this.quantity = requestedQuantity;
    }
    
    private Integer fulfilledQuantity;
    private String notes;
}
