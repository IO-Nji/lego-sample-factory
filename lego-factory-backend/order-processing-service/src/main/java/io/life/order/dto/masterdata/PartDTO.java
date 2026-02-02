package io.life.order.dto.masterdata;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PartDTO - Typed representation of Part from masterdata-service.
 * 
 * Used by MasterdataClient to provide compile-time type safety
 * instead of Map<String, Object> responses.
 * 
 * API Contract: Cross-service contract with masterdata-service.
 * Field names MUST match io.life.masterdata.dto.PartDto exactly.
 * 
 * @see io.life.masterdata.dto.PartDto (masterdata-service origin)
 */
@ApiContract(
    version = "v1",
    externalSource = "masterdata-service",
    description = "Part master data from masterdata-service"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartDTO {

    private Long id;
    private String name;
    private String description;
    private String category;
    private Double unitCost;
}
