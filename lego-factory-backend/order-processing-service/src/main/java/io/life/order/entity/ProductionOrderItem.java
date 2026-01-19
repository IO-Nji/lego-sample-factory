package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductionOrderItem represents a line item in a production order.
 * Tracks which modules or parts need to be produced, their quantities,
 * and the workstation type required for production.
 */
@Entity
@Table(name = "production_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the parent production order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    /**
     * Type of item to produce: MODULE or PART.
     */
    @Column(nullable = false)
    private String itemType;

    /**
     * ID of the module or part in the masterdata service.
     */
    @Column(nullable = false)
    private Long itemId;

    /**
     * Name of the item (for display purposes).
     */
    @Column(nullable = false)
    private String itemName;

    /**
     * Quantity to produce.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Estimated production time in minutes per unit.
     */
    @Column(nullable = true)
    private Integer estimatedTimeMinutes;

    /**
     * Workstation type required: MANUFACTURING (for parts) or ASSEMBLY (for modules).
     * Parts are produced at manufacturing cells (WS-1, WS-2, WS-3).
     * Modules are assembled at assembly stations (WS-4, WS-5, WS-6).
     */
    @Column(nullable = false)
    private String workstationType; // MANUFACTURING, ASSEMBLY
}
