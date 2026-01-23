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

    @Column(name = "customer_order_id")
    private Long customerOrderId; // Link to the customer order that triggered this

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, PROCESSING, COMPLETED, CANCELLED

    @Column(name = "trigger_scenario")
    private String triggerScenario; // DIRECT_FULFILLMENT, PRODUCTION_REQUIRED

    // Items needed for this warehouse order (modules)
    @OneToMany(mappedBy = "warehouseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<WarehouseOrderItem> orderItems;

    @Column(nullable = false)
    private Long workstationId; // WS-8 (Modules Supermarket)

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
            workstationId = 8L; // Default to WS-8 Modules Supermarket
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
