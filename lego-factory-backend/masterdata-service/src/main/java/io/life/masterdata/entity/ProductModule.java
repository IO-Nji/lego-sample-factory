package io.life.masterdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productVariantId;

    @Column(nullable = false)
    private Long moduleId;

    @Column(nullable = false)
    private Integer quantity;

    public ProductModule(Long productVariantId, Long moduleId, Integer quantity) {
        this.productVariantId = productVariantId;
        this.moduleId = moduleId;
        this.quantity = quantity;
    }
}
