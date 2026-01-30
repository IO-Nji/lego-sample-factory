package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * GearAssemblyOrder - Workstation-specific order for WS-4 (Gear Assembly)
 * 
 * INPUT: Gear-specific finished parts (gears, shafts, bearings)
 * OUTPUT: Gear modules
 * SUPPLY: Requires gear parts from Parts Supply Warehouse
 * 
 * This workstation assembles mechanical gear modules.
 */
@Entity
@Table(name = "gear_assembly_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GearAssemblyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Parent control order reference
    @Column(nullable = false)
    private Long assemblyControlOrderId;

    // Fixed workstation - always WS-4
    @Column(nullable = false)
    private Long workstationId = 4L;

    // Input: Required gear parts (JSON array of part IDs)
    @Column(nullable = false, length = 1000)
    private String requiredPartIds; // e.g., "[20, 21, 22]" - gears, shafts, bearings

    @Column(length = 2000)
    private String requiredPartDetails; // JSON with part names and quantities

    // Output: Gear module being assembled
    @Column(nullable = false)
    private Long outputModuleId;

    @Column(nullable = false)
    private String outputModuleName;

    @Column(nullable = false)
    private Integer quantity;

    // Status: PENDING, WAITING_FOR_PARTS, IN_PROGRESS, COMPLETED, HALTED, ABANDONED
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(length = 50)
    private String priority; // LOW, NORMAL, HIGH, URGENT

    // Supply order tracking
    private Long supplyOrderId; // Reference to SupplyOrder for gear parts

    // Timing fields
    private LocalDateTime targetStartTime;
    private LocalDateTime targetCompletionTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualFinishTime;

    // Assembly specifications
    @Column(length = 2000)
    private String assemblyInstructions;

    @Column(length = 2000)
    private String gearRatioRequirements;

    @Column(length = 2000)
    private String qualityChecks; // Gear mesh, torque testing

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
            workstationId = 4L;
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
