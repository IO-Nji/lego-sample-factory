package io.life.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized service for SimAL (Scheduling and Integration) notifications.
 * 
 * This service decouples SimAL communication from individual order services,
 * providing a single point of contact for all SimAL-related operations.
 * 
 * Benefits:
 * - Single point of configuration for SimAL URL
 * - Consistent error handling and logging
 * - Easy to mock for testing
 * - Reduces duplication across services
 */
@Service
public class SimALNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SimALNotificationService.class);

    private final RestTemplate restTemplate;

    @Value("${simal.service.url:http://localhost:8018}")
    private String simalServiceUrl;

    public SimALNotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Update the status of a scheduled order in SimAL.
     * 
     * @param scheduleId The SimAL schedule ID
     * @param status The new status (e.g., "COMPLETED", "IN_PROGRESS", "HALTED")
     * @throws RuntimeException if SimAL update fails
     */
    public void updateScheduleStatus(String scheduleId, String status) {
        updateScheduleStatus(scheduleId, status, LocalDateTime.now());
    }

    /**
     * Update the status of a scheduled order in SimAL with a specific timestamp.
     * 
     * @param scheduleId The SimAL schedule ID
     * @param status The new status
     * @param timestamp The timestamp for the status change
     * @throws RuntimeException if SimAL update fails
     */
    public void updateScheduleStatus(String scheduleId, String status, LocalDateTime timestamp) {
        if (scheduleId == null || scheduleId.isBlank()) {
            logger.warn("Cannot update SimAL - no schedule ID provided");
            return;
        }

        try {
            String url = simalServiceUrl + "/api/simal/scheduled-orders/" + scheduleId + "/status";
            Map<String, Object> request = new HashMap<>();
            request.put("status", status);
            request.put("completedAt", timestamp.toString());
            
            restTemplate.postForObject(url, request, String.class);
            logger.info("✓ Updated SimAL schedule {} status to {}", scheduleId, status);
        } catch (Exception e) {
            logger.error("✗ Failed to update SimAL schedule status for {}: {}", scheduleId, e.getMessage());
            throw new RuntimeException("SimAL update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Notify SimAL that a scheduled order has been completed.
     * Convenience method that sets status to "COMPLETED".
     * 
     * @param scheduleId The SimAL schedule ID
     */
    public void notifyOrderCompleted(String scheduleId) {
        updateScheduleStatus(scheduleId, "COMPLETED");
    }

    /**
     * Notify SimAL that work has started on a scheduled order.
     * Convenience method that sets status to "IN_PROGRESS".
     * 
     * @param scheduleId The SimAL schedule ID
     */
    public void notifyOrderStarted(String scheduleId) {
        updateScheduleStatus(scheduleId, "IN_PROGRESS");
    }

    /**
     * Notify SimAL that a scheduled order has been halted.
     * Convenience method that sets status to "HALTED".
     * 
     * @param scheduleId The SimAL schedule ID
     */
    public void notifyOrderHalted(String scheduleId) {
        updateScheduleStatus(scheduleId, "HALTED");
    }

    /**
     * Silently try to update SimAL without throwing exceptions.
     * Logs warning on failure but allows caller to continue.
     * 
     * Use this for fire-and-forget updates where SimAL sync is secondary.
     * 
     * @param scheduleId The SimAL schedule ID
     * @param status The new status
     * @return true if update succeeded, false otherwise
     */
    public boolean tryUpdateScheduleStatus(String scheduleId, String status) {
        try {
            updateScheduleStatus(scheduleId, status);
            return true;
        } catch (Exception e) {
            logger.warn("SimAL update failed for schedule {} (non-critical): {}", scheduleId, e.getMessage());
            return false;
        }
    }
}
