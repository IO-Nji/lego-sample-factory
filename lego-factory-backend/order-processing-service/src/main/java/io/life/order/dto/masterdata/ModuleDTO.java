package io.life.order.dto.masterdata;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModuleDTO - Typed representation of Module from masterdata-service.
 * 
 * Used by MasterdataClient to provide compile-time type safety
 * instead of Map<String, Object> responses.
 * 
 * API Contract: Cross-service contract with masterdata-service.
 * Field names MUST match io.life.masterdata.dto.ModuleDto exactly.
 * 
 * @see io.life.masterdata.dto.ModuleDto (masterdata-service origin)
 */
@ApiContract(
    version = "v1",
    externalSource = "masterdata-service",
    description = "Module master data from masterdata-service"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDTO {

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
