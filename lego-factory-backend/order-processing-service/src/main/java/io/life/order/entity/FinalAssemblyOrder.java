package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FinalAssemblyOrder represents the final assembly workstation orders (WS-6)
 * 
 * Creation paths:
 * - Scenario 2: Created from WarehouseOrder when modules are available
 * - Scenario 3: Created from AssemblyControlOrder in full production flow
 * 
 * Completion credits Plant Warehouse (WS-7) with finished products
 */
@Entity
@Table(name = "final_assembly_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalAssemblyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Parent references - at least one must be set
    @Column(name = "warehouse_order_id")
    private Long warehouseOrderId; // Scenario 2: Direct from warehouse order

    @Column(name = "assembly_control_order_id")
    private Long assemblyControlOrderId; // Scenario 3/4: From production flow

    @Column(nullable = false)
    private Long workstationId; // Always 6L (Final Assembly)

    // Output specification
    @Column(nullable = false)
    private Long outputProductId; // Product to be assembled

    @Column(nullable = false)
    private Integer outputQuantity; // Quantity to produce

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, SUBMITTED, CANCELLED

    private LocalDateTime startTime; // When assembly started
    
    private LocalDateTime completionTime; // When assembly completed
    
    private LocalDateTime submitTime; // When product was submitted to Plant Warehouse

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
            workstationId = 6L; // Default to Final Assembly
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
