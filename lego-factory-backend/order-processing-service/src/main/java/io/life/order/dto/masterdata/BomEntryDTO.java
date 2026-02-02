package io.life.order.dto.masterdata;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BomEntryDTO - Typed representation of BOM (Bill of Materials) entry.
 * 
 * Used for product → module and module → part relationships.
 * Provides compile-time type safety for BOM lookups.
 * 
 * API Contract: Cross-service contract with masterdata-service.
 * Field names MUST match masterdata-service BOM response format.
 * 
 * CRITICAL: componentId (not moduleId) - matches API response
 */
@ApiContract(
    version = "v1",
    externalSource = "masterdata-service",
    description = "BOM entry for product/module decomposition"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomEntryDTO {

    /** ID of the component (module or part) */
    private Long componentId;
    
    /** Name of the component */
    private String componentName;
    
    /** Type of component: "MODULE" or "PART" */
    private String componentType;
    
    /** Quantity required per parent item */
    private Integer quantity;
}
