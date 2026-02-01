package io.life.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

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
