package io.life.order.dto.masterdata;

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
 * @see io.life.masterdata.dto.PartDto (masterdata-service origin)
 */
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
