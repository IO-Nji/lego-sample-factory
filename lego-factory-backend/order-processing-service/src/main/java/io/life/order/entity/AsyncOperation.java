package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to track the status of asynchronous operations.
 * Used for long-running tasks like SimAL scheduling and control order creation.
 * 
 * @since Phase 3 - Async Processing (February 5, 2026)
 */
@Entity
@Table(name = "async_operations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique operation ID (UUID) for external reference
     */
    @Column(name = "operation_id", unique = true, nullable = false, length = 36)
    private String operationId;

    /**
     * Type of operation: SCHEDULE_PRODUCTION, DISPATCH_CONTROL_ORDERS, etc.
     */
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    /**
     * Current status: PENDING, PROCESSING, COMPLETED, FAILED
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * Related entity ID (e.g., ProductionOrder ID)
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Related entity type (e.g., "ProductionOrder")
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * Result data as JSON (e.g., created order details)
     */
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    /**
     * Error message if operation failed
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Progress percentage (0-100)
     */
    @Column(name = "progress_percent")
    private Integer progressPercent;

    /**
     * Human-readable progress message
     */
    @Column(name = "progress_message", length = 500)
    private String progressMessage;

    /**
     * User who initiated the operation
     */
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    /**
     * When the operation was started
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * When the operation completed (success or failure)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (progressPercent == null) {
            progressPercent = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    // Operation type constants
    public static final String TYPE_SCHEDULE_PRODUCTION = "SCHEDULE_PRODUCTION";
    public static final String TYPE_DISPATCH_CONTROL_ORDERS = "DISPATCH_CONTROL_ORDERS";
    public static final String TYPE_CREATE_SUPPLY_ORDERS = "CREATE_SUPPLY_ORDERS";
}
