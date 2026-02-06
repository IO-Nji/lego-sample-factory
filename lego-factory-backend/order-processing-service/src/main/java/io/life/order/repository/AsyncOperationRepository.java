package io.life.order.repository;

import io.life.order.entity.AsyncOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AsyncOperation entities.
 * Provides methods for tracking and querying async operation status.
 * 
 * @since Phase 3 - Async Processing (February 5, 2026)
 */
@Repository
public interface AsyncOperationRepository extends JpaRepository<AsyncOperation, Long> {

    /**
     * Find an operation by its unique operation ID.
     */
    Optional<AsyncOperation> findByOperationId(String operationId);

    /**
     * Find all operations for a specific entity (e.g., all operations for ProductionOrder #5).
     */
    List<AsyncOperation> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId);

    /**
     * Find all operations by type (e.g., all SCHEDULE_PRODUCTION operations).
     */
    List<AsyncOperation> findByOperationTypeOrderByCreatedAtDesc(String operationType);

    /**
     * Find all operations by status (e.g., all PROCESSING operations).
     */
    List<AsyncOperation> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find all pending or processing operations (for monitoring).
     */
    @Query("SELECT a FROM AsyncOperation a WHERE a.status IN ('PENDING', 'PROCESSING') ORDER BY a.createdAt ASC")
    List<AsyncOperation> findActiveOperations();

    /**
     * Find recent operations for a user.
     */
    List<AsyncOperation> findByInitiatedByOrderByCreatedAtDesc(String username);

    /**
     * Find operations created within a time range.
     */
    List<AsyncOperation> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Count operations by status (for metrics).
     */
    long countByStatus(String status);

    /**
     * Find the most recent operation for an entity.
     */
    @Query("SELECT a FROM AsyncOperation a WHERE a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.createdAt DESC LIMIT 1")
    Optional<AsyncOperation> findLatestForEntity(
            @Param("entityType") String entityType, 
            @Param("entityId") Long entityId);

    /**
     * Delete old completed/failed operations (for cleanup).
     */
    void deleteByStatusInAndCompletedAtBefore(List<String> statuses, LocalDateTime before);
}
