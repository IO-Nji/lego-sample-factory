package io.life.order.dto.masterdata;

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
 * @see io.life.masterdata.dto.ModuleDto (masterdata-service origin)
 */
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
