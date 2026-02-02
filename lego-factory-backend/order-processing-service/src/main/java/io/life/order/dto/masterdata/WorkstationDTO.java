package io.life.order.dto.masterdata;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkstationDTO - Typed representation of Workstation from masterdata-service.
 * 
 * Used by MasterdataClient to provide compile-time type safety
 * instead of Map<String, Object> responses.
 * 
 * API Contract: Cross-service contract with masterdata-service.
 * Field names MUST match io.life.masterdata.dto.WorkstationDto exactly.
 * 
 * @see io.life.masterdata.dto.WorkstationDto (masterdata-service origin)
 */
@ApiContract(
    version = "v1",
    externalSource = "masterdata-service",
    description = "Workstation master data from masterdata-service"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkstationDTO {

    private Long id;
    private String name;
    private String workstationType;
    private String description;
    private Boolean active;
}
