package io.life.order.service;

import io.life.order.entity.SystemConfiguration;
import io.life.order.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SystemConfigService
 * 
 * Service for managing system configuration values with in-memory caching.
 * Provides typed accessors for common configuration values like LOT_SIZE_THRESHOLD.
 * 
 * CACHING STRATEGY:
 * - Values are cached on first read
 * - Cache is invalidated on update
 * - Cache can be manually refreshed via refreshCache()
 * 
 * USAGE:
 * - systemConfigService.getLotSizeThreshold() → returns int
 * - systemConfigService.setLotSizeThreshold(5, "admin") → updates value
 * - systemConfigService.getConfigValue("CUSTOM_KEY") → returns Optional<String>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigurationRepository configRepository;

    /**
     * In-memory cache for configuration values.
     * Key: configKey, Value: configValue (as String)
     */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    // ========================
    // DEFAULT VALUES
    // ========================

    /**
     * Default lot size threshold for Scenario 4.
     * Orders with total quantity >= this value bypass warehouse.
     */
    public static final int DEFAULT_LOT_SIZE_THRESHOLD = 3;

    // ========================
    // TYPED ACCESSORS
    // ========================

    /**
     * Get the lot size threshold for Scenario 4.
     * Orders with total quantity >= this value trigger DIRECT_PRODUCTION.
     * 
     * @return Lot size threshold (default: 3)
     */
    public int getLotSizeThreshold() {
        return getIntValue(SystemConfiguration.KEY_LOT_SIZE_THRESHOLD, DEFAULT_LOT_SIZE_THRESHOLD);
    }

    /**
     * Set the lot size threshold for Scenario 4.
     * 
     * @param threshold New threshold value (must be >= 1)
     * @param updatedBy Username of admin making the change
     * @return Updated configuration
     */
    @Transactional
    public SystemConfiguration setLotSizeThreshold(int threshold, String updatedBy) {
        if (threshold < 1) {
            throw new IllegalArgumentException("Lot size threshold must be at least 1");
        }
        return setConfigValue(
                SystemConfiguration.KEY_LOT_SIZE_THRESHOLD,
                String.valueOf(threshold),
                updatedBy
        );
    }

    // ========================
    // GENERIC ACCESSORS
    // ========================

    /**
     * Get a configuration value by key.
     * 
     * @param key Configuration key
     * @return Optional containing the value, or empty if not found
     */
    public Optional<String> getConfigValue(String key) {
        // Check cache first
        if (configCache.containsKey(key)) {
            return Optional.of(configCache.get(key));
        }

        // Load from database
        Optional<SystemConfiguration> config = configRepository.findByConfigKey(key);
        config.ifPresent(c -> configCache.put(key, c.getConfigValue()));

        return config.map(SystemConfiguration::getConfigValue);
    }

    /**
     * Get a configuration value as integer.
     * 
     * @param key Configuration key
     * @param defaultValue Default value if not found or invalid
     * @return Integer value or default
     */
    public int getIntValue(String key, int defaultValue) {
        return getConfigValue(key)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid integer value for config {}: {}", key, value);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Get a configuration value as boolean.
     * 
     * @param key Configuration key
     * @param defaultValue Default value if not found
     * @return Boolean value or default
     */
    public boolean getBooleanValue(String key, boolean defaultValue) {
        return getConfigValue(key)
                .map(value -> "true".equalsIgnoreCase(value) || "1".equals(value))
                .orElse(defaultValue);
    }

    /**
     * Set a configuration value.
     * 
     * @param key Configuration key
     * @param value New value
     * @param updatedBy Username of user making the change
     * @return Updated configuration entity
     */
    @Transactional
    public SystemConfiguration setConfigValue(String key, String value, String updatedBy) {
        SystemConfiguration config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + key));

        if (!config.getEditable()) {
            throw new IllegalStateException("Configuration " + key + " is not editable");
        }

        String oldValue = config.getConfigValue();
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);

        SystemConfiguration saved = configRepository.save(config);

        // Update cache
        configCache.put(key, value);

        log.info("Configuration {} updated: {} → {} (by {})", key, oldValue, value, updatedBy);

        return saved;
    }

    // ========================
    // BULK OPERATIONS
    // ========================

    /**
     * Get all configurations.
     */
    @Transactional(readOnly = true)
    public List<SystemConfiguration> getAllConfigurations() {
        return configRepository.findAll();
    }

    /**
     * Get all configurations in a category.
     */
    @Transactional(readOnly = true)
    public List<SystemConfiguration> getConfigurationsByCategory(String category) {
        return configRepository.findByCategory(category);
    }

    /**
     * Get all editable configurations.
     */
    @Transactional(readOnly = true)
    public List<SystemConfiguration> getEditableConfigurations() {
        return configRepository.findByEditableTrue();
    }

    /**
     * Get a specific configuration by key.
     */
    @Transactional(readOnly = true)
    public Optional<SystemConfiguration> getConfiguration(String key) {
        return configRepository.findByConfigKey(key);
    }

    // ========================
    // CACHE MANAGEMENT
    // ========================

    /**
     * Refresh the configuration cache from the database.
     * Call this after bulk updates or to ensure cache consistency.
     */
    @Transactional(readOnly = true)
    public void refreshCache() {
        configCache.clear();
        List<SystemConfiguration> allConfigs = configRepository.findAll();
        allConfigs.forEach(config -> configCache.put(config.getConfigKey(), config.getConfigValue()));
        log.info("Configuration cache refreshed with {} entries", allConfigs.size());
    }

    /**
     * Clear the configuration cache.
     * Next read will load from database.
     */
    public void clearCache() {
        configCache.clear();
        log.debug("Configuration cache cleared");
    }

    /**
     * Check if a configuration exists.
     */
    public boolean configExists(String key) {
        return configCache.containsKey(key) || configRepository.existsByConfigKey(key);
    }
}
