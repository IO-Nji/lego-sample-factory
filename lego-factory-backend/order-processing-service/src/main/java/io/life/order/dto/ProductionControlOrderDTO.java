package io.life.order.dto;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ProductionControlOrderDTO - Manufacturing control orders (WS-1, WS-2, WS-3).
 * 
 * API Contract: Frontend Production Control dashboard displays these orders.
 * Manages manufacturing workstation orders through orchestration.
 * 
 * Completion Criteria: ALL child workstation orders COMPLETED → Control order auto-completes
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Production control orders managing WS-1,2,3 manufacturing workstations"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionControlOrderDTO {

    private Long id;
    private String controlOrderNumber;
    private Long sourceProductionOrderId;
    private Long assignedWorkstationId;
    private String simalScheduleId;
    private String status;
    private LocalDateTime targetStartTime;
    private LocalDateTime targetCompletionTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualCompletionTime;
    private String priority;
    private String productionInstructions;
    private String qualityCheckpoints;
    private String safetyProcedures;
    private Integer estimatedDurationMinutes;
    private Integer actualDurationMinutes;
    private Integer defectsFound;
    private Integer defectsReworked;
    private Boolean reworkRequired;
    private String reworkNotes;
    private String operatorNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
