package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * MotorAssemblyOrder - Workstation-specific order for WS-5 (Motor Assembly)
 * 
 * INPUT: Motor-specific finished parts (motors, wires, housings, connectors)
 * OUTPUT: Motor modules
 * SUPPLY: Requires motor parts from Parts Supply Warehouse
 * 
 * This workstation assembles electrical motor modules.
 */
@Entity
@Table(name = "motor_assembly_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotorAssemblyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Parent control order reference
    @Column(nullable = false)
    private Long assemblyControlOrderId;

    // Fixed workstation - always WS-5
    @Column(nullable = false)
    private Long workstationId = 5L;

    // Input: Required motor parts (JSON array of part IDs)
    @Column(nullable = false, length = 1000)
    private String requiredPartIds; // e.g., "[30, 31, 32]" - motors, wires, housings

    @Column(length = 2000)
    private String requiredPartDetails; // JSON with part names and quantities

    // Output: Motor module being assembled
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
    private Long supplyOrderId; // Reference to SupplyOrder for motor parts

    // Timing fields
    private LocalDateTime targetStartTime;
    private LocalDateTime targetCompletionTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualFinishTime;

    // Assembly specifications
    @Column(length = 2000)
    private String assemblyInstructions;

    @Column(length = 2000)
    private String electricalSpecifications; // Voltage, current, wiring diagram

    @Column(length = 2000)
    private String qualityChecks; // Electrical testing, motor rotation

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
            workstationId = 5L;
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
