package io.life.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseOrderDTO {
    private Long id;
    private String orderNumber;
    private Long customerOrderId;
    private Long workstationId;
    private LocalDateTime orderDate;
    private String status;
    private List<WarehouseOrderItemDTO> orderItems;
    private String triggerScenario;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
