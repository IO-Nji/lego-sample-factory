package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AssemblyOrder Entity
 * 
 * @deprecated As of Phase 6 (January 2026), replaced by workstation-specific order types:
 * - {@link GearAssemblyOrder} for WS-4 (Gear Assembly)
 * - {@link MotorAssemblyOrder} for WS-5 (Motor Assembly)
 * - {@link FinalAssemblyOrder} for WS-6 (Final Assembly)
 * 
 * This generic entity lacks workstation-specific fields and validation.
 * FinalAssemblyOrder particularly important as it consumes MODULES not PARTS.
 * Use the specific order types above for new implementations.
 * 
 * Represents an individual assembly task assigned to an assembly workstation.
 * Created when AssemblyControlOrder is dispatched to workstations.
 * 
 * Hierarchy: ProductionOrder → AssemblyControlOrder → AssemblyOrder
 * 
 * Relationship: Many-to-One with AssemblyControlOrder
 * - One AssemblyControlOrder can create multiple AssemblyOrders
 * - Each AssemblyOrder assigned to specific workstation (WS-4, WS-5, or WS-6)
 * 
 * Lifecycle:
 * 1. PENDING - Created by dispatch, awaiting operator action
 * 2. IN_PROGRESS - Operator started work
 * 3. COMPLETED - Operator finished work, inventory credited
 * 4. HALTED - Temporarily paused
 * 5. ABANDONED - Cancelled/scrapped
 * 
 * Visibility:
 * - Assembly operators see only orders for their assigned workstation
 * - Assembly Control sees parent AssemblyControlOrder (not individual assembly orders)
 * 
 * Special Case - Final Assembly (WS-6):
 * - Credits Plant Warehouse with finished Product Variants
 */
@Deprecated(since = "Phase 6", forRemoval = false)
@Entity
@Table(name = "assembly_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "assembly_control_order_id", nullable = false)
    private Long assemblyControlOrderId;

    @Column(name = "workstation_id", nullable = false)
    private Long workstationId;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, HALTED, ABANDONED

    @Column(name = "priority")
    private String priority; // LOW, NORMAL, HIGH, URGENT

    @Column(name = "item_type", nullable = false)
    private String itemType; // MODULE, PRODUCT_VARIANT

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "target_start_time")
    private LocalDateTime targetStartTime;

    @Column(name = "target_completion_time")
    private LocalDateTime targetCompletionTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_finish_time")
    private LocalDateTime actualFinishTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_by_workstation_id")
    private Long createdByWorkstationId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
