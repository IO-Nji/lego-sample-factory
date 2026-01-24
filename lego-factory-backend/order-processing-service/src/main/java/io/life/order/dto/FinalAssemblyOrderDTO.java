package io.life.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
