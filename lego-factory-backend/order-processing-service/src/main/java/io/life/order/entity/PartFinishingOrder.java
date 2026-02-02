package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * PartFinishingOrder - Workstation-specific order for WS-3 (Part Finishing)
 * 
 * INPUT: Pre-processed parts from Parts Pre-Production (WS-2) or Parts Supply Warehouse
 * OUTPUT: Finished parts ready for assembly
 * SUPPLY: Requires pre-processed parts - different category from WS-2
 * 
 * This workstation performs final finishing operations: polishing, coating, inspection.
 */
@Entity
@Table(name = "part_finishing_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartFinishingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Parent control order reference
    @Column(nullable = false)
    private Long productionControlOrderId;

    // Fixed workstation - always WS-3
    @Column(nullable = false)
    private Long workstationId = 3L;

    // Input: Required pre-processed parts (JSON array of part IDs)
    @Column(nullable = false, length = 1000)
    private String requiredPartIds; // e.g., "[3, 7, 15]"

    @Column(length = 2000)
    private String requiredPartDetails; // JSON with quantities

    // Output: Finished part being produced
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

    // Finishing specifications
    @Column(length = 2000)
    private String finishingOperations; // Polishing, coating, painting, inspection

    @Column(length = 2000)
    private String surfaceQualityRequirements;

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
            workstationId = 3L;
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
