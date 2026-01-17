package io.life.simal_integration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequest {
    private String workstationId;
    private LocalDateTime scheduledStartTime;
    private Integer duration;
    private String priority;
    private String reason;
}
