package io.life.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
