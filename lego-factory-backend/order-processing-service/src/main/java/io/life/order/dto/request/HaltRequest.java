package io.life.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared request DTO for halting order operations.
 * Used across all workstation controllers (Manufacturing WS-1,2,3 and Assembly WS-4,5,6).
 * 
 * Replaces inline HaltRequest classes in:
 * - InjectionMoldingStationController
 * - PartsPreProductionStationController
 * - PartFinishingStationController
 * - ProductionControlOrderController
 * - AssemblyControlOrderController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HaltRequest {
    
    /**
     * Reason for halting the order.
     * Should describe why production/assembly was stopped.
     * Examples: "Machine malfunction", "Waiting for parts", "Quality issue"
     */
    private String reason;
}
