package io.life.order.dto;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FinalAssemblyOrderDTO - Final Assembly workstation orders (WS-6).
 * 
 * API Contract: Frontend displays these orders at WS-6 Final Assembly dashboard.
 * Completion credits Plant Warehouse (WS-7) with finished products.
 * 
 * Key Fields:
 * - outputProductId: PRODUCT ID to credit (not module ID)
 * - warehouseOrderId: Source warehouse order (Scenario 2)
 * - assemblyControlOrderId: Parent control order (Scenario 3)
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Final Assembly orders for WS-6 workstation (Scenario 2/3)"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalAssemblyOrderDTO {
    private Long id;
    private String orderNumber;
    private Long warehouseOrderId;
    private Long assemblyControlOrderId;
    private Long workstationId;
    private Long outputProductId;
    private Integer outputQuantity;
    private LocalDateTime orderDate;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime completionTime;
    private LocalDateTime submitTime;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
