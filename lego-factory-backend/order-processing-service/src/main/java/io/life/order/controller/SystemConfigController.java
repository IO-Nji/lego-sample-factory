package io.life.order.controller;

import io.life.order.dto.SystemConfigurationDTO;
import io.life.order.entity.SystemConfiguration;
import io.life.order.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SystemConfigController
 * 
 * REST API for managing system configurations.
 * Admin-only endpoints for viewing and modifying system settings.
 * 
 * Endpoints:
 * - GET  /api/config                    - List all configurations
 * - GET  /api/config/{key}              - Get specific configuration
 * - PUT  /api/config/{key}              - Update configuration value
 * - GET  /api/config/category/{cat}     - Get configurations by category
 * - GET  /api/config/scenario4/threshold - Get Scenario 4 threshold (convenience)
 * - PUT  /api/config/scenario4/threshold - Set Scenario 4 threshold (convenience)
 * - POST /api/config/cache/refresh      - Refresh configuration cache
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigController {

    private final SystemConfigService configService;

    /**
     * Get all system configurations.
     */
    @GetMapping
    public ResponseEntity<List<SystemConfigurationDTO>> getAllConfigurations() {
        List<SystemConfiguration> configs = configService.getAllConfigurations();
        return ResponseEntity.ok(configs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get configurations by category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SystemConfigurationDTO>> getByCategory(@PathVariable String category) {
        List<SystemConfiguration> configs = configService.getConfigurationsByCategory(category);
        return ResponseEntity.ok(configs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get a specific configuration by key.
     */
    @GetMapping("/{key}")
    public ResponseEntity<SystemConfigurationDTO> getConfiguration(@PathVariable String key) {
        return configService.getConfiguration(key)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a configuration value.
     * 
     * Request body: { "value": "newValue", "updatedBy": "adminUsername" }
     */
    @PutMapping("/{key}")
    public ResponseEntity<SystemConfigurationDTO> updateConfiguration(
            @PathVariable String key,
            @RequestBody Map<String, String> request) {

        String value = request.get("value");
        String updatedBy = request.getOrDefault("updatedBy", "system");

        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            SystemConfiguration updated = configService.setConfigValue(key, value, updatedBy);
            return ResponseEntity.ok(mapToDTO(updated));
        } catch (RuntimeException e) {
            log.error("Failed to update configuration {}: {}", key, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================
    // SCENARIO 4 CONVENIENCE ENDPOINTS
    // ========================

    /**
     * Get the current Scenario 4 lot size threshold.
     * 
     * Returns: { "threshold": 3, "description": "..." }
     */
    @GetMapping("/scenario4/threshold")
    public ResponseEntity<Map<String, Object>> getLotSizeThreshold() {
        int threshold = configService.getLotSizeThreshold();
        return ResponseEntity.ok(Map.of(
                "threshold", threshold,
                "key", SystemConfiguration.KEY_LOT_SIZE_THRESHOLD,
                "description", "Orders with total quantity >= this value bypass warehouse and go directly to production"
        ));
    }

    /**
     * Set the Scenario 4 lot size threshold.
     * 
     * Request body: { "threshold": 5, "updatedBy": "admin" }
     */
    @PutMapping("/scenario4/threshold")
    public ResponseEntity<Map<String, Object>> setLotSizeThreshold(@RequestBody Map<String, Object> request) {
        Object thresholdObj = request.get("threshold");
        String updatedBy = (String) request.getOrDefault("updatedBy", "admin");

        if (thresholdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "threshold is required"));
        }

        try {
            int threshold;
            if (thresholdObj instanceof Number) {
                threshold = ((Number) thresholdObj).intValue();
            } else {
                threshold = Integer.parseInt(thresholdObj.toString());
            }

            configService.setLotSizeThreshold(threshold, updatedBy);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "threshold", threshold,
                    "message", "Lot size threshold updated to " + threshold
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid threshold value"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // CACHE MANAGEMENT
    // ========================

    /**
     * Refresh the configuration cache.
     */
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, String>> refreshCache() {
        configService.refreshCache();
        return ResponseEntity.ok(Map.of("message", "Configuration cache refreshed"));
    }

    // ========================
    // DTO MAPPING
    // ========================

    private SystemConfigurationDTO mapToDTO(SystemConfiguration config) {
        return SystemConfigurationDTO.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .valueType(config.getValueType())
                .description(config.getDescription())
                .category(config.getCategory())
                .editable(config.getEditable())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy())
                .build();
    }
}
