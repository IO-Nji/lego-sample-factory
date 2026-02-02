package io.life.order.dto;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for SupplyOrder entity.
 * Used for API responses and requests.
 * 
 * API Contract: Frontend displays supply orders at WS-9 Parts Supply Warehouse.
 * Supply orders MUST be fulfilled before workstation orders can start (gating logic).
 * 
 * Workflow: Control order creates supply order → WS-9 fulfills → Workstation can execute
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Supply orders from Parts Supply Warehouse (WS-9) with gating logic"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyOrderDTO {

    private Long id;

    private String supplyOrderNumber;

    private Long sourceControlOrderId;

    private String sourceControlOrderType; // PRODUCTION, ASSEMBLY

    private Long requestingWorkstationId;

    private Long supplyWarehouseWorkstationId;

    private String status; // PENDING, IN_PROGRESS, FULFILLED, REJECTED, CANCELLED

    private List<SupplyOrderItemDTO> supplyOrderItems;

    private String priority; // LOW, MEDIUM, HIGH, URGENT

    private LocalDateTime createdAt;

    private LocalDateTime requestedByTime;

    private LocalDateTime fulfilledAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime updatedAt;

    private String notes;
}
