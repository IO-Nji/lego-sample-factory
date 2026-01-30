package io.life.order.dto.request;

import io.life.order.dto.SupplyOrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for requesting parts from Parts Supply Warehouse.
 * Used by control order operators to specify custom parts requirements.
 * 
 * Replaces inline RequestPartsRequest classes in:
 * - ProductionControlOrderController
 * - AssemblyControlOrderController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestPartsRequest {

    /**
     * List of specific parts being requested with quantities.
     */
    private List<SupplyOrderItemDTO> requiredParts;

    /**
     * Date/time by which parts are needed.
     */
    private LocalDateTime neededBy;

    /**
     * Additional notes for the supply request.
     */
    private String notes;
}
