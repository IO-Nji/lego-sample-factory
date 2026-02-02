package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SystemConfiguration entity for storing configurable system parameters.
 * 
 * Used for runtime-configurable values like:
 * - LOT_SIZE_THRESHOLD: Minimum order quantity to trigger Scenario 4 (direct production)
 * - Future: other configurable business rules
 * 
 * Key naming convention: UPPERCASE_WITH_UNDERSCORES (e.g., LOT_SIZE_THRESHOLD)
 */
@Entity
@Table(name = "system_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration key (unique identifier).
     * Examples: LOT_SIZE_THRESHOLD, DEFAULT_PRIORITY, MAX_ORDER_QUANTITY
     */
    @Column(nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * Configuration value (stored as string, parsed by service layer).
     */
    @Column(nullable = false, length = 500)
    private String configValue;

    /**
     * Value type hint for frontend/parsing.
     * Examples: INTEGER, STRING, BOOLEAN, DECIMAL
     */
    @Column(nullable = false, length = 20)
    private String valueType;

    /**
     * Human-readable description of this configuration.
     */
    @Column(length = 500)
    private String description;

    /**
     * Category for grouping configurations in UI.
     * Examples: SCENARIO_4, ORDER_PROCESSING, INVENTORY, SYSTEM
     */
    @Column(length = 50)
    private String category;

    /**
     * Whether this configuration can be modified by admin.
     * Some configurations may be read-only system values.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean editable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================
    // CONFIGURATION KEY CONSTANTS
    // ========================

    /**
     * Scenario 4 threshold: Orders with total quantity >= this value
     * bypass warehouse and go directly to production.
     * Default: 3
     */
    public static final String KEY_LOT_SIZE_THRESHOLD = "LOT_SIZE_THRESHOLD";

    /**
     * Category for Scenario 4 related configurations.
     */
    public static final String CATEGORY_SCENARIO_4 = "SCENARIO_4";

    /**
     * Category for order processing configurations.
     */
    public static final String CATEGORY_ORDER_PROCESSING = "ORDER_PROCESSING";
}
