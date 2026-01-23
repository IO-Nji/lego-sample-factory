package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "warehouse_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Long customerOrderId; // Link to the customer order that triggered this

    @Column(nullable = false)
    private Long workstationId; // Modules Supermarket (workstation 8)

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, PROCESSING, COMPLETED, CANCELLED

    // Items needed for this warehouse order (modules or parts)
    @OneToMany(mappedBy = "warehouseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<WarehouseOrderItem> orderItems;

    // Fulfillment scenario that triggered this order
    private String triggerScenario; // DIRECT_FULFILLMENT, PRODUCTION_REQUIRED

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (workstationId == null) {
            workstationId = 8L; // Default to Modules Supermarket
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
