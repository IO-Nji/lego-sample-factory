package io.life.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "low_stock_thresholds",
       uniqueConstraints = @UniqueConstraint(columnNames = {"workstationId", "itemType", "itemId"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowStockThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null means global across all workstations
    private Long workstationId;

    @Column(nullable = false, length = 50)
    private String itemType;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Integer threshold;
}
