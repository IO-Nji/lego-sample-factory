package io.life.order.dto;

import io.life.order.annotation.ApiContract;
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
    private Long workstationId;
    private LocalDateTime orderDate;
    private String status;
    private List<WarehouseOrderItemDTO> orderItems;
    private String triggerScenario;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
