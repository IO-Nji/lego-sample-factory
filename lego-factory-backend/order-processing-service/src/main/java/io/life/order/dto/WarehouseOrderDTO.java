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
 * WarehouseOrderDTO - Order for Modules Supermarket (WS-8).
 * 
 * <p>API Contract:</p>
 * Created from CustomerOrder when modules needed for final assembly.
 * Contains WarehouseOrderItemDTO list with module information.
 * 
 * <p>Linked to Production:</p>
 * - productionOrderId field prevents multiple orders from interfering
 * - Orders with productionOrderId set bypass stock checks during fulfillment
 * 
 * <p>Validation (Issue #3 Fix - Feb 4, 2026):</p>
 * - orderItems: Required for creation, 1-100 items
 * - workstationId: Validated 1-9 if provided
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Warehouse order for module fulfillment (Scenario 2/3)"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseOrderDTO {
    private Long id;
    private String orderNumber;
    private Long customerOrderId;
    private Long productionOrderId;
    
    @Min(value = 1, message = "Workstation ID must be between 1 and 9")
    @Max(value = 9, message = "Workstation ID must be between 1 and 9")
    private Long workstationId;
    
    private LocalDateTime orderDate;
    private String status;
    
    @Size(max = 100, message = "Order cannot exceed 100 items")
    private List<@Valid WarehouseOrderItemDTO> orderItems;
    
    private String triggerScenario;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
