package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared request DTO for updating operator notes on orders.
 * Used across all workstation controllers (Manufacturing WS-1,2,3 and Assembly WS-4,5,6).
 * 
 * Replaces inline NotesRequest classes in:
 * - InjectionMoldingStationController
 * - PartsPreProductionStationController
 * - PartFinishingStationController
 * - GearAssemblyStationController
 * - MotorAssemblyStationController
 * - FinalAssemblyStationController
 * - ProductionControlOrderController
 * - AssemblyControlOrderController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotesRequest {
    
    /**
     * Operator notes or comments about the order.
     * Can be used for tracking production issues, observations, etc.
     */
    private String notes;
}
