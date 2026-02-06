package io.life.order.dto;

import io.life.order.entity.AsyncOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for AsyncOperation responses.
 * Used to communicate async operation status to the frontend.
 * 
 * @since Phase 3 - Async Processing (February 5, 2026)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncOperationDTO {

    private String operationId;
    private String operationType;
    private String status;
    private Long entityId;
    private String entityType;
    private String resultData;
    private String errorMessage;
    private Integer progressPercent;
    private String progressMessage;
    private String initiatedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    /**
     * Convert entity to DTO.
     */
    public static AsyncOperationDTO fromEntity(AsyncOperation entity) {
        return AsyncOperationDTO.builder()
                .operationId(entity.getOperationId())
                .operationType(entity.getOperationType())
                .status(entity.getStatus())
                .entityId(entity.getEntityId())
                .entityType(entity.getEntityType())
                .resultData(entity.getResultData())
                .errorMessage(entity.getErrorMessage())
                .progressPercent(entity.getProgressPercent())
                .progressMessage(entity.getProgressMessage())
                .initiatedBy(entity.getInitiatedBy())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Check if operation is complete (success or failure).
     */
    public boolean isComplete() {
        return AsyncOperation.STATUS_COMPLETED.equals(status) || 
               AsyncOperation.STATUS_FAILED.equals(status);
    }

    /**
     * Check if operation succeeded.
     */
    public boolean isSuccessful() {
        return AsyncOperation.STATUS_COMPLETED.equals(status);
    }

    /**
     * Check if operation failed.
     */
    public boolean isFailed() {
        return AsyncOperation.STATUS_FAILED.equals(status);
    }

    /**
     * Check if operation is still in progress.
     */
    public boolean isInProgress() {
        return AsyncOperation.STATUS_PENDING.equals(status) || 
               AsyncOperation.STATUS_PROCESSING.equals(status);
    }
}
