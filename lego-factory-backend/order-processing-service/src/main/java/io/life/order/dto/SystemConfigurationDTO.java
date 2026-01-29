package io.life.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for SystemConfiguration entity.
 */
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
