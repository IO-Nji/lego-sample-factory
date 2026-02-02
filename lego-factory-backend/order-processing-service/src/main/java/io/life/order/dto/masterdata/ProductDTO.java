package io.life.order.dto.masterdata;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductDTO - Typed representation of Product from masterdata-service.
 * 
 * <p>API Contract:</p>
 * Consumed from masterdata-service GET /api/masterdata/products/{id}
 * Field names match source API exactly - no mapping needed.
 * 
 * <p>Used by MasterdataClient to provide compile-time type safety
 * instead of Map<String, Object> responses.</p>
 * 
 * @see io.life.masterdata.dto.ProductDto (masterdata-service origin)
 */
@ApiContract(
    version = "v1",
    externalSource = "masterdata-service",
    description = "Product master data with pricing and timing information"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer estimatedTimeMinutes;
}
