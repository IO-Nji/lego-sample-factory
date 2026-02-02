package io.life.order.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Centralized configuration for the Order Processing Service.
 * 
 * All business rules, thresholds, timeouts, and feature flags are externalized
 * here for easy management across different environments (dev, prod, cloud).
 * 
 * Configuration is loaded from application.properties with environment variable
 * overrides supported via ${ENV_VAR:default} syntax.
 * 
 * @see application.properties for default values
 * @see application-dev.properties for development overrides
 * @see application-prod.properties for production overrides
 * @see application-cloud.properties for cloud deployment
 */
@Configuration
@ConfigurationProperties(prefix = "life.order-processing")
@Validated
@Data
public class OrderProcessingConfig {

    /**
     * Order processing thresholds and business rules
     */
    private Thresholds thresholds = new Thresholds();

    /**
     * Timeout configurations for external service calls
     */
    private Timeouts timeouts = new Timeouts();

    /**
     * Workstation identifiers (WS-1 to WS-9)
     */
    private Workstations workstations = new Workstations();

    /**
     * Feature flags for gradual rollout and environment-specific behavior
     */
    private Features features = new Features();

    /**
     * Order number generation settings
     */
    private OrderNumbers orderNumbers = new OrderNumbers();

    // ========================================
    // THRESHOLDS - Business Rules
    // ========================================
    @Data
    public static class Thresholds {
        
        /**
         * Lot size threshold for Scenario 4 (Direct Production).
         * Orders with total quantity >= this value bypass Warehouse Order
         * and go directly to Production.
         * 
         * Default: 3 (configurable via Admin UI or LOT_SIZE_THRESHOLD env var)
         */
        @Min(1)
        private int lotSizeThreshold = 3;

        /**
         * Maximum number of items allowed in a single order.
         * Prevents excessively large orders that could cause performance issues.
         */
        @Min(1)
        @Max(1000)
        private int maxOrderItems = 100;

        /**
         * Duration (in minutes) to reserve stock during order processing.
         * Used to prevent overselling during concurrent order confirmations.
         */
        @Min(1)
        private int stockReserveMinutes = 30;

        /**
         * Safety margin percentage added to stock calculations.
         * Helps prevent stockouts due to timing issues.
         */
        @Min(0)
        @Max(100)
        private int stockSafetyMarginPercent = 10;

        /**
         * Maximum retries for failed order operations before giving up.
         */
        @Min(0)
        @Max(10)
        private int maxRetries = 3;
    }

    // ========================================
    // TIMEOUTS - External Service Calls
    // ========================================
    @Data
    public static class Timeouts {
        
        /**
         * Timeout for reading from Masterdata Service (products, BOM, modules).
         * This is typically fast as masterdata is cached.
         */
        @Min(500)
        private int masterdataReadMs = 5000;

        /**
         * Timeout for writing to Masterdata Service (rare operations).
         */
        @Min(1000)
        private int masterdataWriteMs = 10000;

        /**
         * Timeout for reading from Inventory Service (stock levels).
         */
        @Min(500)
        private int inventoryReadMs = 3000;

        /**
         * Timeout for writing to Inventory Service (stock adjustments).
         * Longer timeout as this involves database transactions.
         */
        @Min(1000)
        private int inventoryWriteMs = 10000;

        /**
         * Timeout for SimAL schedule generation.
         * This is the longest operation as it involves complex optimization.
         */
        @Min(5000)
        private int simalScheduleMs = 30000;

        /**
         * Timeout for SimAL task status updates.
         */
        @Min(1000)
        private int simalTaskUpdateMs = 5000;

        /**
         * Timeout for User Service authentication/authorization calls.
         */
        @Min(500)
        private int userServiceMs = 3000;
    }

    // ========================================
    // WORKSTATIONS - Workstation IDs
    // ========================================
    @Data
    public static class Workstations {
        
        // Manufacturing Workstations (WS-1, WS-2, WS-3)
        private Long injectionMolding = 1L;
        private Long partsPreProduction = 2L;
        private Long partFinishing = 3L;

        // Assembly Workstations (WS-4, WS-5, WS-6)
        private Long gearAssembly = 4L;
        private Long motorAssembly = 5L;
        private Long finalAssembly = 6L;

        // Warehouse Workstations (WS-7, WS-8, WS-9)
        private Long plantWarehouse = 7L;
        private Long modulesSupermarket = 8L;
        private Long partsSupply = 9L;
    }

    // ========================================
    // FEATURES - Feature Flags
    // ========================================
    @Data
    public static class Features {
        
        /**
         * Enable Redis caching for masterdata and BOM lookups.
         * Requires Redis to be available.
         */
        private boolean enableCaching = false;

        /**
         * Enable async processing for long-running operations.
         * Uses ThreadPoolTaskExecutor for background tasks.
         */
        private boolean enableAsyncProcessing = false;

        /**
         * Enable Resilience4j circuit breaker for external services.
         * Provides graceful degradation when services are unavailable.
         */
        private boolean enableCircuitBreaker = false;

        /**
         * Enable Micrometer metrics export (Prometheus, CloudWatch, etc.).
         */
        private boolean enableMetrics = true;

        /**
         * Enable detailed DEBUG-level logging for troubleshooting.
         * Should be disabled in production for performance.
         */
        private boolean enableDetailedLogging = true;

        /**
         * Enable automatic status propagation after order completion.
         * When true, completing a workstation order automatically 
         * checks and updates parent order status.
         */
        private boolean enableAutoStatusPropagation = true;
    }

    // ========================================
    // ORDER NUMBERS - Generation Settings
    // ========================================
    @Data
    public static class OrderNumbers {
        
        /**
         * Prefix for Customer Order numbers (e.g., "ORD-")
         */
        private String customerOrderPrefix = "ORD-";

        /**
         * Prefix for Warehouse Order numbers (e.g., "WO-")
         */
        private String warehouseOrderPrefix = "WO-";

        /**
         * Prefix for Production Order numbers (e.g., "PO-")
         */
        private String productionOrderPrefix = "PO-";

        /**
         * Prefix for Final Assembly Order numbers (e.g., "FA-")
         */
        private String finalAssemblyOrderPrefix = "FA-";

        /**
         * Prefix for Supply Order numbers (e.g., "SO-")
         */
        private String supplyOrderPrefix = "SO-";

        /**
         * Length of the random suffix in order numbers (default: 8)
         */
        @Min(4)
        @Max(16)
        private int randomSuffixLength = 8;
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Check if an order qualifies for Scenario 4 (Direct Production).
     * 
     * @param totalQuantity Total quantity across all order items
     * @return true if quantity >= lotSizeThreshold
     */
    public boolean isDirectProductionEligible(int totalQuantity) {
        return totalQuantity >= thresholds.getLotSizeThreshold();
    }

    /**
     * Get the workstation ID by type for cleaner code.
     * 
     * @param type Workstation type name (e.g., "PLANT_WAREHOUSE")
     * @return Workstation ID
     */
    public Long getWorkstationId(String type) {
        return switch (type.toUpperCase()) {
            case "INJECTION_MOLDING", "WS1", "WS-1" -> workstations.getInjectionMolding();
            case "PARTS_PRE_PRODUCTION", "WS2", "WS-2" -> workstations.getPartsPreProduction();
            case "PART_FINISHING", "WS3", "WS-3" -> workstations.getPartFinishing();
            case "GEAR_ASSEMBLY", "WS4", "WS-4" -> workstations.getGearAssembly();
            case "MOTOR_ASSEMBLY", "WS5", "WS-5" -> workstations.getMotorAssembly();
            case "FINAL_ASSEMBLY", "WS6", "WS-6" -> workstations.getFinalAssembly();
            case "PLANT_WAREHOUSE", "WS7", "WS-7" -> workstations.getPlantWarehouse();
            case "MODULES_SUPERMARKET", "WS8", "WS-8" -> workstations.getModulesSupermarket();
            case "PARTS_SUPPLY", "WS9", "WS-9" -> workstations.getPartsSupply();
            default -> throw new IllegalArgumentException("Unknown workstation type: " + type);
        };
    }
}
