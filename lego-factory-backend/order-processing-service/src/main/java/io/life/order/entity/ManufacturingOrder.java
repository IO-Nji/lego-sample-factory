package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ManufacturingOrder Entity
 * 
 * @deprecated As of Phase 6 (January 2026), replaced by workstation-specific order types:
 * - {@link InjectionMoldingOrder} for WS-1 (Injection Molding)
 * - {@link PartPreProductionOrder} for WS-2 (Parts Pre-Production)
 * - {@link PartFinishingOrder} for WS-3 (Part Finishing)
 * 
 * This generic entity lacks workstation-specific fields and validation.
 * Use the specific order types above for new implementations.
 * 
 * Represents an individual manufacturing task assigned to a manufacturing workstation.
 * Created when ProductionControlOrder is dispatched to workstations.
 * 
 * Hierarchy: ProductionOrder → ProductionControlOrder → ManufacturingOrder
 * 
 * Relationship: Many-to-One with ProductionControlOrder
 * - One ProductionControlOrder can create multiple ManufacturingOrders
 * - Each ManufacturingOrder assigned to specific workstation (WS-1, WS-2, or WS-3)
 * 
 * Lifecycle:
 * 1. PENDING - Created by dispatch, awaiting operator action
 * 2. IN_PROGRESS - Operator started work
 * 3. COMPLETED - Operator finished work, inventory credited
 * 4. HALTED - Temporarily paused
 * 5. ABANDONED - Cancelled/scrapped
 * 
 * Visibility:
 * - Manufacturing operators see only orders for their assigned workstation
 * - Production Control sees parent ProductionControlOrder (not individual manufacturing orders)
 */
@Deprecated(since = "Phase 6", forRemoval = false)
@Entity
@Table(name = "manufacturing_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManufacturingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "production_control_order_id", nullable = false)
    private Long productionControlOrderId;

    @Column(name = "workstation_id", nullable = false)
    private Long workstationId;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, HALTED, ABANDONED

    @Column(name = "priority")
    private String priority; // LOW, NORMAL, HIGH, URGENT

    @Column(name = "item_type", nullable = false)
    private String itemType; // PART, MODULE

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
