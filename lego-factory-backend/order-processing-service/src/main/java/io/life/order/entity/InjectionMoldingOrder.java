package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * InjectionMoldingOrder - Workstation-specific order for WS-1 (Injection Molding)
 * 
 * INPUT: Raw materials (pellets, resins) - NOT parts from warehouse
 * OUTPUT: Basic molded parts
 * SUPPLY: None - uses raw materials directly
 * 
 * This workstation is the starting point of manufacturing and does not require
 * parts from the Parts Supply Warehouse.
 */
@Entity
@Table(name = "injection_molding_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InjectionMoldingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Parent control order reference
    @Column(nullable = false)
    private Long productionControlOrderId;

    // Fixed workstation - always WS-1
    @Column(nullable = false)
    private Long workstationId = 1L;

    // Output: Part being produced
    @Column(nullable = false)
    private Long outputPartId;

    @Column(nullable = false)
    private String outputPartName;

    @Column(nullable = false)
    private Integer quantity;

    // Status: PENDING, IN_PROGRESS, COMPLETED, HALTED, ABANDONED
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(length = 50)
    private String priority; // LOW, NORMAL, HIGH, URGENT

    // Timing fields
    private LocalDateTime targetStartTime;
    private LocalDateTime targetCompletionTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualFinishTime;

    // Raw material specifications (JSON or comma-separated)
    @Column(length = 1000)
    private String rawMaterialSpecs; // e.g., "Resin Type A, Color: Red, Weight: 50g"

    // Production parameters
    @Column(length = 500)
    private String moldingParameters; // Temperature, pressure, cycle time, etc.

    @Column(length = 500)
    private String qualityChecks;

    @Column(length = 500)
    private String operatorNotes;

    // Audit fields
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (workstationId == null) {
            workstationId = 1L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if ("COMPLETED".equals(status) && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }
}
