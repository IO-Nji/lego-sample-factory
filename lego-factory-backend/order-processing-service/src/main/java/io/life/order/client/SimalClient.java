package io.life.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * SimalClient
 * 
 * REST client for communicating with SimAL Integration Service.
 * Used to update scheduled task status when workstation orders change status.
 */
@Component
@Slf4j
public class SimalClient {

    private final RestTemplate restTemplate;
    private final String simalServiceUrl;

    public SimalClient(RestTemplate restTemplate,
                      @Value("${simal.service.url:http://simal-integration-service:8016}") String simalServiceUrl) {
        this.restTemplate = restTemplate;
        this.simalServiceUrl = simalServiceUrl;
    }

    /**
     * Update scheduled task status in SimAL service.
     * Called when workstation orders change status (START, COMPLETE, HALT).
     * 
     * @param taskId The scheduled task ID (workstation-{wsId}-{orderNumber})
     * @param newStatus The new status (IN_PROGRESS, COMPLETED, HALTED)
     */
    public void updateTaskStatus(String taskId, String newStatus) {
        try {
            String url = simalServiceUrl + "/api/simal/tasks/" + taskId + "/status";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Send status update as JSON body
            String requestBody = "{\"status\":\"" + newStatus + "\"}";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    request,
                    Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✓ Updated task {} status to {} in SimAL", taskId, newStatus);
            } else {
                log.warn("Failed to update task {} status: HTTP {}", taskId, response.getStatusCode());
            }
            
        } catch (Exception e) {
            // Don't fail the workstation order if task update fails
            log.error("Error updating task {} status in SimAL: {}", taskId, e.getMessage());
        }
    }

    /**
     * Generate task ID from workstation order information.
     * Format: workstation-{wsId}-{orderNumber}
     * 
     * @param workstationId The workstation ID (1-9)
     * @param orderNumber The order number (e.g., "IMO-001")
     * @return Task ID string
     */
    public static String generateTaskId(Long workstationId, String orderNumber) {
        return String.format("workstation-%d-%s", workstationId, orderNumber);
    }
}
