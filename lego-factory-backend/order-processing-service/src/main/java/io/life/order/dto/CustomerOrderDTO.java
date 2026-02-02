package io.life.order.dto;

import io.life.order.annotation.ApiContract;
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
    private List<OrderItemDTO> orderItems;
    private Long workstationId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
