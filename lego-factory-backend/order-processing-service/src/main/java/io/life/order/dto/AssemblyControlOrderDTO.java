package io.life.order.dto;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * AssemblyControlOrderDTO - Assembly control orders (WS-4, WS-5, WS-6).
 * 
 * API Contract: Frontend Assembly Control dashboard displays these orders.
 * Manages assembly workstation orders through orchestration.
 * 
 * Completion Criteria: ALL child workstation orders COMPLETED → Control order auto-completes
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Assembly control orders managing WS-4,5,6 assembly workstations"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssemblyControlOrderDTO {

    private Long id;
    private String controlOrderNumber;
    private Long sourceProductionOrderId;
    private Long assignedWorkstationId;
    private String simalScheduleId;
    private String status;
    private Long itemId;
    private String itemType;
    private Integer quantity;
    private LocalDateTime targetStartTime;
    private LocalDateTime targetCompletionTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualCompletionTime;
    private String priority;
    private String assemblyInstructions;
    private String qualityCheckpoints;
    private String testingProcedures;
    private String packagingRequirements;
    private Integer estimatedDurationMinutes;
    private Integer actualDurationMinutes;
    private Integer defectsFound;
    private Integer defectsReworked;
    private Boolean reworkRequired;
    private String reworkNotes;
    private String operatorNotes;
    private String shippingNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
