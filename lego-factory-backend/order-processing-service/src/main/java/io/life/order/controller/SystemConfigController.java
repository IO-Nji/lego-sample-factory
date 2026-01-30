package io.life.order.controller;

import io.life.order.dto.SystemConfigurationDTO;
import io.life.order.entity.SystemConfiguration;
import io.life.order.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Configuration", description = "Admin-only system settings and configuration management")
public class SystemConfigController {

    private final SystemConfigService configService;

    @Operation(summary = "Get all configurations", description = "List all system configuration entries")
    @ApiResponse(responseCode = "200", description = "List of configurations")
    @GetMapping
    public ResponseEntity<List<SystemConfigurationDTO>> getAllConfigurations() {
        List<SystemConfiguration> configs = configService.getAllConfigurations();
        return ResponseEntity.ok(configs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get configurations by category", description = "Filter configurations by category")
    @ApiResponse(responseCode = "200", description = "List of configurations in category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SystemConfigurationDTO>> getByCategory(
            @Parameter(description = "Configuration category (e.g., SCENARIO, SYSTEM)") @PathVariable String category) {
        List<SystemConfiguration> configs = configService.getConfigurationsByCategory(category);
        return ResponseEntity.ok(configs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get configuration by key", description = "Retrieve a specific configuration value")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuration found"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @GetMapping("/{key}")
    public ResponseEntity<SystemConfigurationDTO> getConfiguration(
            @Parameter(description = "Configuration key") @PathVariable String key) {
        return configService.getConfiguration(key)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update configuration", description = "Update a configuration value (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuration updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @PutMapping("/{key}")
    public ResponseEntity<SystemConfigurationDTO> updateConfiguration(
            @Parameter(description = "Configuration key") @PathVariable String key,
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

    @Operation(summary = "Get Scenario 4 lot size threshold", 
               description = "Get the threshold for direct production (bypassing warehouse)")
    @ApiResponse(responseCode = "200", description = "Current threshold value")
    @GetMapping("/scenario4/threshold")
    public ResponseEntity<Map<String, Object>> getLotSizeThreshold() {
        int threshold = configService.getLotSizeThreshold();
        return ResponseEntity.ok(Map.of(
                "threshold", threshold,
                "key", SystemConfiguration.KEY_LOT_SIZE_THRESHOLD,
                "description", "Orders with total quantity >= this value bypass warehouse and go directly to production"
        ));
    }

    @Operation(summary = "Set Scenario 4 lot size threshold", 
               description = "Configure threshold for direct production orders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Threshold updated"),
        @ApiResponse(responseCode = "400", description = "Invalid threshold value")
    })
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

    @Operation(summary = "Refresh configuration cache", description = "Force reload of all cached configurations")
    @ApiResponse(responseCode = "200", description = "Cache refreshed")
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
