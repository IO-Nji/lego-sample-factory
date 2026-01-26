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
    private Long productId;

    @Column(nullable = false)
    private Long moduleId;

    @Column(nullable = false)
    private Integer quantity;

    public ProductModule(Long productId, Long moduleId, Integer quantity) {
        this.productId = productId;
        this.moduleId = moduleId;
        this.quantity = quantity;
    }
}
