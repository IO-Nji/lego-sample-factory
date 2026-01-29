package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * PartPreProductionOrder - Workstation-specific order for WS-2 (Parts Pre-Production)
 * 
 * INPUT: Basic parts from Injection Molding (WS-1) or Parts Supply Warehouse
 * OUTPUT: Pre-processed parts (machined, drilled, cut)
 * SUPPLY: Requires basic parts - different from WS-3
 * 
 * This workstation performs initial processing operations on basic parts.
 */
@Entity
@Table(name = "part_preproduction_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartPreProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Parent control order reference
    @Column(nullable = false)
    private Long productionControlOrderId;

    // Fixed workstation - always WS-2
    @Column(nullable = false)
    private Long workstationId = 2L;

    // Input: Required basic parts (JSON array of part IDs)
    @Column(nullable = false, length = 1000)
    private String requiredPartIds; // e.g., "[1, 5, 12]"

    @Column(length = 2000)
    private String requiredPartDetails; // JSON with quantities

    // Output: Part being produced
    @Column(nullable = false)
    private Long outputPartId;

    @Column(nullable = false)
    private String outputPartName;

    @Column(nullable = false)
    private Integer quantity;

    // Status: PENDING, WAITING_FOR_PARTS, IN_PROGRESS, COMPLETED, HALTED, ABANDONED
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(length = 50)
    private String priority; // LOW, NORMAL, HIGH, URGENT

    // Supply order tracking
    private Long supplyOrderId; // Reference to SupplyOrder if parts requested

    // Timing fields
    private LocalDateTime targetStartTime;
    private LocalDateTime targetCompletionTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualFinishTime;

    // Processing specifications
    @Column(length = 2000)
    private String processingOperations; // Drilling, cutting, machining details

    @Column(length = 2000)
    private String qualityChecks;

    @Column(length = 2000)
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
            workstationId = 2L;
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
