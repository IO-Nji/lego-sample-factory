package io.life.order.dto.request;

import io.life.order.dto.SupplyOrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating a new supply order.
 * Used to request parts from Parts Supply Warehouse (WS-9).
 * 
 * Replaces inline CreateSupplyOrderRequest in SupplyOrderController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyOrderCreateRequest {

    /**
     * ID of the control order requesting parts.
     */
    private Long sourceControlOrderId;

    /**
     * Type of control order: PRODUCTION or ASSEMBLY
     */
    private String sourceControlOrderType;

    /**
     * ID of the workstation requesting the parts.
     */
    private Long requestingWorkstationId;

    /**
     * Priority level: LOW, NORMAL, HIGH, URGENT
     */
    private String priority;

    /**
     * Date/time by which parts are needed.
     */
    private LocalDateTime requestedByTime;

    /**
     * List of specific parts/items being requested.
     * Can be null if parts should be determined from BOM lookup.
     */
    private List<SupplyOrderItemDTO> requiredItems;

    /**
     * Additional notes for the supply request.
     */
    private String notes;
}
