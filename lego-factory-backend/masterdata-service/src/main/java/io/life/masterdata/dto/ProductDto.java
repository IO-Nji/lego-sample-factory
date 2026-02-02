package io.life.masterdata.dto;

import io.life.masterdata.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductDto - Product master data (SOURCE).
 * 
 * API Contract: This is the SOURCE DTO consumed by:
 * - order-service (io.life.order.dto.masterdata.ProductDTO)
 * - frontend
 * 
 * Field names MUST remain stable - clients depend on exact field names.
 */
@ApiContract(
    version = "v1",
    externalSource = "order-service, frontend",
    description = "Product master data with pricing and timing (SOURCE)"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer estimatedTimeMinutes;
}
