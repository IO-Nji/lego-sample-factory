package io.life.order.dto.masterdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BomEntryDTO - Typed representation of BOM (Bill of Materials) entry.
 * 
 * Used for product → module and module → part relationships.
 * Provides compile-time type safety for BOM lookups.
 */
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
