package io.life.order.config;

import io.life.order.entity.SystemConfiguration;
import io.life.order.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataInitializer for Order Processing Service.
 * 
 * Seeds initial system configuration values on startup if they don't exist.
 * Uses check-then-insert pattern to avoid duplicate key errors.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SystemConfigurationRepository configRepository;

    /**
     * Default lot size threshold for Scenario 4.
     * Orders with total quantity >= this value bypass warehouse
     * and go directly to production.
     */
    private static final int DEFAULT_LOT_SIZE_THRESHOLD = 3;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing Order Processing Service data...");
        
        initializeSystemConfigurations();
        
        log.info("Order Processing Service data initialization complete.");
    }

    /**
     * Initialize system configuration values.
     * Only creates configurations that don't already exist.
     */
    private void initializeSystemConfigurations() {
        // Scenario 4: LOT_SIZE_THRESHOLD
        if (!configRepository.existsByConfigKey(SystemConfiguration.KEY_LOT_SIZE_THRESHOLD)) {
            SystemConfiguration lotSizeConfig = SystemConfiguration.builder()
                    .configKey(SystemConfiguration.KEY_LOT_SIZE_THRESHOLD)
                    .configValue(String.valueOf(DEFAULT_LOT_SIZE_THRESHOLD))
                    .valueType("INTEGER")
                    .description("Minimum order quantity to trigger Scenario 4 (direct production). " +
                            "Orders with total quantity >= this value bypass warehouse and go directly to production.")
                    .category(SystemConfiguration.CATEGORY_SCENARIO_4)
                    .editable(true)
                    .updatedBy("SYSTEM")
                    .build();
            
            configRepository.save(lotSizeConfig);
            log.info("Created default LOT_SIZE_THRESHOLD configuration: {}", DEFAULT_LOT_SIZE_THRESHOLD);
        } else {
            log.info("LOT_SIZE_THRESHOLD configuration already exists, skipping initialization.");
        }
    }
}
