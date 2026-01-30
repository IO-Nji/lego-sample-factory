package io.life.order.dto.masterdata;

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
 * @see io.life.masterdata.dto.WorkstationDto (masterdata-service origin)
 */
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
