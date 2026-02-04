package io.life.order.dto;

import io.life.order.annotation.ApiContract;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CustomerOrderDTO - Main order entity for Plant Warehouse (WS-7).
 * 
 * <p>API Contract:</p>
 * Used by frontend for order creation, confirmation, and fulfillment.
 * Contains nested OrderItemDTO list with quantity field mappings.
 * 
 * <p>Workflow States:</p>
 * - PENDING → CONFIRMED → PROCESSING → COMPLETED
 * - triggerScenario determines downstream actions (Scenario 1-4)
 * 
 * <p>Validation (Issue #3 Fix - Feb 4, 2026):</p>
 * - workstationId: Optional for creation (defaults to 7), validated 1-9 if provided
 * - orderItems: Required, 1-100 items, each item validated
 * - notes: Optional, max 500 characters
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Customer order from Plant Warehouse with scenario routing"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderDTO {
    private Long id;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String status;
    private String triggerScenario;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Size(min = 1, max = 100, message = "Order must contain 1-100 items")
    private List<@Valid OrderItemDTO> orderItems;
    
    @Min(value = 1, message = "Workstation ID must be between 1 and 9")
    @Max(value = 9, message = "Workstation ID must be between 1 and 9")
    private Long workstationId;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
