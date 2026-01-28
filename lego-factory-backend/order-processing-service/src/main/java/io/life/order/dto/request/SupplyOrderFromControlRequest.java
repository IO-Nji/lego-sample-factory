package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a supply order from a control order with automatic BOM lookup.
 * Parts needed are automatically determined from the module's Bill of Materials.
 * 
 * Replaces inline CreateFromControlOrderRequest in SupplyOrderController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyOrderFromControlRequest {

    /**
     * ID of the control order (ProductionControlOrder or AssemblyControlOrder).
     */
    private Long controlOrderId;

    /**
     * Type of control order: PRODUCTION or ASSEMBLY
     * Determines which control order table to query for item details.
     */
    private String controlOrderType;

    /**
     * Priority level: LOW, NORMAL, HIGH, URGENT
     */
    private String priority;
}
