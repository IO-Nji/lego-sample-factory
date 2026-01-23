package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * FinalAssemblyOrder - Workstation-specific order for WS-6 (Final Assembly)
 * 
 * INPUT: MODULES (from WS-4, WS-5, and other assembly stations) - NOT PARTS!
 * OUTPUT: Product Variants (finished products)
 * SUPPLY: Requires MODULES from Modules Supermarket (WS-8) - NOT parts!
 * 
 * CRITICAL DISTINCTION: This is the ONLY workstation that consumes modules instead of parts.
 * Used in Scenario 2 workflow when Modules Supermarket fulfills warehouse orders.
 */
@Entity
@Table(name = "final_assembly_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalAssemblyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Parent reference - can be from WarehouseOrder (Scenario 2) or AssemblyControlOrder (Scenario 3)
    private Long warehouseOrderId; // For Scenario 2: Direct from warehouse order
    private Long assemblyControlOrderId; // For Scenario 3: From production planning

    // Fixed workstation - always WS-6
    @Column(nullable = false)
    private Long workstationId = 6L;

    // Input: Required MODULES (JSON array of module IDs) - NOT PARTS!
    @Column(nullable = false, length = 1000)
    private String requiredModuleIds; // e.g., "[5, 8, 12]" - gear modules, motor modules, etc.

    @Column(length = 2000)
    private String requiredModuleDetails; // JSON with module names and quantities

    // Output: Product Variant being assembled
    @Column(nullable = false)
    private Long outputProductVariantId;

    @Column(nullable = false)
    private String outputProductVariantName;

    @Column(nullable = false)
    private Integer quantity;

    // Status: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, HALTED, ABANDONED
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(length = 50)
    private String priority; // LOW, NORMAL, HIGH, URGENT

    // Module transfer tracking (modules are "transferred" from Modules Supermarket)
    @Column(nullable = false)
    private Boolean modulesReceived = false; // True when modules confirmed at workstation

    // Timing fields
    private LocalDateTime targetStartTime;
    private LocalDateTime targetCompletionTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualFinishTime;

    // Assembly specifications
    @Column(length = 500)
    private String assemblyInstructions;

    @Column(length = 500)
    private String finalQualityChecks; // Complete product testing

    @Column(length = 500)
    private String packagingRequirements;

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
            workstationId = 6L;
        }
        if (modulesReceived == null) {
            modulesReceived = false;
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
