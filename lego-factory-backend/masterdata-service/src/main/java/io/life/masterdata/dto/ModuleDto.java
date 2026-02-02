package io.life.masterdata.dto;

import io.life.masterdata.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModuleDto - Module master data (SOURCE).
 * 
 * API Contract: This is the SOURCE DTO consumed by:
 * - order-service (io.life.order.dto.masterdata.ModuleDTO)
 * - frontend
 * 
 * Field names MUST remain stable - clients depend on exact field names.
 */
@ApiContract(
    version = "v1",
    externalSource = "order-service, frontend",
    description = "Module master data with production workstation (SOURCE)"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDto {

    private Long id;
    private String name;
    private String description;
    private String type;
    
    /**
     * Production workstation ID where this module is manufactured/assembled.
     * 1 = Injection Molding, 2 = Parts Pre-Production, 3 = Part Finishing,
     * 4 = Gear Assembly, 5 = Motor Assembly
     */
    private Integer productionWorkstationId;
}
