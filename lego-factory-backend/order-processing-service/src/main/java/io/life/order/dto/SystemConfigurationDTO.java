package io.life.order.dto;

import io.life.order.annotation.ApiContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for SystemConfiguration entity.
 * 
 * API Contract: Admin panel uses this for system-wide configuration.
 * Key configurations: LOT_SIZE_THRESHOLD (Scenario 4 trigger)
 * 
 * Security: Only ADMIN role can modify editable=true configurations
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "System configuration settings for admin panel"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfigurationDTO {

    private Long id;
    private String configKey;
    private String configValue;
    private String valueType;
    private String description;
    private String category;
    private Boolean editable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
