package io.life.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.life.order.dto.AsyncOperationDTO;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.entity.AsyncOperation;
import io.life.order.repository.AsyncOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for handling asynchronous production operations.
 * Provides non-blocking execution for long-running tasks like:
 * - SimAL scheduling
 * - Control order dispatch
 * - Supply order creation
 * 
 * Uses Spring's @Async with ThreadPoolTaskExecutor for parallel processing.
 * Operations are tracked via AsyncOperation entity for status polling.
 * 
 * @since Phase 3 - Async Processing (February 5, 2026)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncProductionService {

    private final AsyncOperationRepository asyncOperationRepository;
    private final ProductionOrderService productionOrderService;
    private final ObjectMapper objectMapper;

    @Value("${life.order-processing.features.enable-async-processing:false}")
    private boolean asyncEnabled;

    /**
     * Check if async processing is enabled.
     */
    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    /**
     * Schedule a production order asynchronously.
     * Returns immediately with an operation ID that can be polled for status.
     * 
     * @param productionOrderId The production order to schedule
     * @param scheduledStart Scheduled start time
     * @param scheduledEnd Scheduled end time
     * @param ganttChartId Gantt chart ID from SimAL
     * @param initiatedBy Username of initiating user
     * @return AsyncOperationDTO with operation ID for status tracking
     */
    @Transactional
    public AsyncOperationDTO initiateScheduleProduction(
            Long productionOrderId,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd,
            String ganttChartId,
            String initiatedBy) {

        // Create tracking record
        AsyncOperation operation = AsyncOperation.builder()
                .operationId(UUID.randomUUID().toString())
                .operationType(AsyncOperation.TYPE_SCHEDULE_PRODUCTION)
                .status(AsyncOperation.STATUS_PENDING)
                .entityId(productionOrderId)
                .entityType("ProductionOrder")
                .progressPercent(0)
                .progressMessage("Queued for processing")
                .initiatedBy(initiatedBy)
                .build();

        operation = asyncOperationRepository.save(operation);
        log.info("Created async operation {} for scheduling production order {}", 
                operation.getOperationId(), productionOrderId);

        // Kick off async processing
        executeScheduleProductionAsync(
                operation.getOperationId(),
                productionOrderId,
                scheduledStart,
                scheduledEnd,
                ganttChartId
        );

        return AsyncOperationDTO.fromEntity(operation);
    }

    /**
     * Execute the schedule production operation asynchronously.
     * Updates the AsyncOperation record with progress and results.
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> executeScheduleProductionAsync(
            String operationId,
            Long productionOrderId,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd,
            String ganttChartId) {

        log.info("Starting async schedule production for operation {}", operationId);

        try {
            // Update status to PROCESSING
            updateOperationProgress(operationId, AsyncOperation.STATUS_PROCESSING, 10, 
                    "Initiating SimAL scheduling...");

            // Simulate SimAL processing time (in real system, this calls SimAL API)
            updateOperationProgress(operationId, AsyncOperation.STATUS_PROCESSING, 30, 
                    "Generating production schedule...");

            // Call the synchronous method
            ProductionOrderDTO result = productionOrderService.scheduleProduction(
                    productionOrderId,
                    scheduledStart,
                    scheduledEnd,
                    ganttChartId
            );

            updateOperationProgress(operationId, AsyncOperation.STATUS_PROCESSING, 70, 
                    "Schedule created, finalizing...");

            // Mark as completed
            completeOperation(operationId, result);
            log.info("Completed async schedule production for operation {}", operationId);

        } catch (Exception e) {
            log.error("Async schedule production failed for operation {}: {}", operationId, e.getMessage(), e);
            failOperation(operationId, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Dispatch control orders asynchronously.
     * Creates ProductionControlOrder and AssemblyControlOrder entities.
     * 
     * @param productionOrderId The production order to dispatch
     * @param initiatedBy Username of initiating user
     * @return AsyncOperationDTO with operation ID for status tracking
     */
    @Transactional
    public AsyncOperationDTO initiateDispatchControlOrders(
            Long productionOrderId,
            String initiatedBy) {

        // Create tracking record
        AsyncOperation operation = AsyncOperation.builder()
                .operationId(UUID.randomUUID().toString())
                .operationType(AsyncOperation.TYPE_DISPATCH_CONTROL_ORDERS)
                .status(AsyncOperation.STATUS_PENDING)
                .entityId(productionOrderId)
                .entityType("ProductionOrder")
                .progressPercent(0)
                .progressMessage("Queued for dispatch")
                .initiatedBy(initiatedBy)
                .build();

        operation = asyncOperationRepository.save(operation);
        log.info("Created async operation {} for dispatching control orders {}", 
                operation.getOperationId(), productionOrderId);

        // Kick off async processing
        executeDispatchControlOrdersAsync(operation.getOperationId(), productionOrderId);

        return AsyncOperationDTO.fromEntity(operation);
    }

    /**
     * Execute the dispatch control orders operation asynchronously.
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> executeDispatchControlOrdersAsync(
            String operationId,
            Long productionOrderId) {

        log.info("Starting async dispatch control orders for operation {}", operationId);

        try {
            updateOperationProgress(operationId, AsyncOperation.STATUS_PROCESSING, 10, 
                    "Analyzing production order items...");

            updateOperationProgress(operationId, AsyncOperation.STATUS_PROCESSING, 30, 
                    "Creating manufacturing control orders...");

            // Call the synchronous method
            ProductionOrderDTO result = productionOrderService.dispatchToControlStations(productionOrderId);

            updateOperationProgress(operationId, AsyncOperation.STATUS_PROCESSING, 80, 
                    "Control orders created, updating status...");

            // Mark as completed
            completeOperation(operationId, result);
            log.info("Completed async dispatch control orders for operation {}", operationId);

        } catch (Exception e) {
            log.error("Async dispatch control orders failed for operation {}: {}", operationId, e.getMessage(), e);
            failOperation(operationId, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get the current status of an async operation.
     * 
     * @param operationId The operation ID to query
     * @return AsyncOperationDTO or null if not found
     */
    @Transactional(readOnly = true)
    public AsyncOperationDTO getOperationStatus(String operationId) {
        return asyncOperationRepository.findByOperationId(operationId)
                .map(AsyncOperationDTO::fromEntity)
                .orElse(null);
    }

    /**
     * Get all active (pending/processing) operations.
     */
    @Transactional(readOnly = true)
    public List<AsyncOperationDTO> getActiveOperations() {
        return asyncOperationRepository.findActiveOperations()
                .stream()
                .map(AsyncOperationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all operations for a specific entity.
     */
    @Transactional(readOnly = true)
    public List<AsyncOperationDTO> getOperationsForEntity(String entityType, Long entityId) {
        return asyncOperationRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(AsyncOperationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update operation progress.
     */
    @Transactional
    public void updateOperationProgress(String operationId, String status, int progressPercent, String message) {
        asyncOperationRepository.findByOperationId(operationId)
                .ifPresent(op -> {
                    op.setStatus(status);
                    op.setProgressPercent(progressPercent);
                    op.setProgressMessage(message);
                    if (AsyncOperation.STATUS_PROCESSING.equals(status) && op.getStartedAt() == null) {
                        op.setStartedAt(LocalDateTime.now());
                    }
                    asyncOperationRepository.save(op);
                });
    }

    /**
     * Mark operation as completed.
     */
    @Transactional
    public void completeOperation(String operationId, Object result) {
        asyncOperationRepository.findByOperationId(operationId)
                .ifPresent(op -> {
                    op.setStatus(AsyncOperation.STATUS_COMPLETED);
                    op.setProgressPercent(100);
                    op.setProgressMessage("Operation completed successfully");
                    op.setCompletedAt(LocalDateTime.now());
                    
                    // Serialize result to JSON
                    try {
                        op.setResultData(objectMapper.writeValueAsString(result));
                    } catch (JsonProcessingException e) {
                        log.warn("Could not serialize result for operation {}: {}", operationId, e.getMessage());
                    }
                    
                    asyncOperationRepository.save(op);
                });
    }

    /**
     * Mark operation as failed.
     */
    @Transactional
    public void failOperation(String operationId, String errorMessage) {
        asyncOperationRepository.findByOperationId(operationId)
                .ifPresent(op -> {
                    op.setStatus(AsyncOperation.STATUS_FAILED);
                    op.setErrorMessage(errorMessage);
                    op.setCompletedAt(LocalDateTime.now());
                    asyncOperationRepository.save(op);
                });
    }
}
