package io.life.order.service;

import io.life.order.dto.ProductionOrderDTO;
import io.life.order.exception.ProductionPlanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing production planning and SimAL integration.
 * Coordinates with SimAL Integration Service to schedule production orders.
 * Tracks production progress and updates order status.
 */
@Service
@Transactional
public class ProductionPlanningService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionPlanningService.class);
    private static final Long PRODUCTION_CONTROL_WORKSTATION_ID = 20L; // Production Control workstation
    private static final Long ASSEMBLY_CONTROL_WORKSTATION_ID = 21L;   // Assembly Control workstation
    private static final String PRODUCTION_ORDER_NOT_FOUND = "Production order not found: ";
    
    // Production order status constants
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_IN_PRODUCTION = "IN_PRODUCTION";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ProductionOrderService productionOrderService;
    private final ProductionControlOrderService productionControlOrderService;
    private final AssemblyControlOrderService assemblyControlOrderService;
    private final RestTemplate restTemplate;

    @Value("${simal.api.base-url:http://localhost:8016/api}")
    private String simalApiBaseUrl;

    @Value("${simal.api.scheduled-orders-path:/simal/scheduled-orders/}")
    private String simalScheduledOrdersPath;

    public ProductionPlanningService(
            ProductionOrderService productionOrderService,
            ProductionControlOrderService productionControlOrderService,
            AssemblyControlOrderService assemblyControlOrderService,
            RestTemplate restTemplate) {
        this.productionOrderService = productionOrderService;
        this.productionControlOrderService = productionControlOrderService;
        this.assemblyControlOrderService = assemblyControlOrderService;
        this.restTemplate = restTemplate;
    }

    /**
     * Submit a production order to SimAL for scheduling.
     * Sends order details and receives a schedule ID and estimated duration.
     */
    public ProductionOrderDTO submitProductionOrderToSimal(Long productionOrderId) {
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));

        // Check if already submitted
        if (STATUS_SUBMITTED.equals(order.getStatus()) || STATUS_SCHEDULED.equals(order.getStatus())) {
            logger.warn("Production order {} already submitted or scheduled", order.getProductionOrderNumber());
            return order;
        }

        try {
            // Create request payload for SimAL
            SimalProductionOrderRequest request = new SimalProductionOrderRequest();
            request.setProductionOrderNumber(order.getProductionOrderNumber());
            request.setSourceCustomerOrderId(order.getSourceCustomerOrderId());
            request.setDueDate(order.getDueDate());
            request.setPriority(order.getPriority());
            request.setNotes(order.getNotes());

            // Send to SimAL API
            String url = simalApiBaseUrl + "/simal/production-order";
            HttpEntity<SimalProductionOrderRequest> requestEntity = new HttpEntity<>(request);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    String scheduleId = (String) responseBody.get("scheduleId");
                    Integer estimatedDuration = ((Number) responseBody.get("estimatedDuration")).intValue();
                    String estimatedCompletionStr = (String) responseBody.get("estimatedCompletion");

                    // Update production order with schedule information
                    productionOrderService.linkToSimalSchedule(
                            productionOrderId,
                            scheduleId,
                            estimatedDuration,
                            LocalDateTime.parse(estimatedCompletionStr)
                    );

                    // Update status to SUBMITTED
                    ProductionOrderDTO updatedOrder = productionOrderService.updateProductionOrderStatus(productionOrderId, STATUS_SUBMITTED);
                    logger.info("Submitted production order {} to SimAL with schedule {}", 
                            order.getProductionOrderNumber(), scheduleId);

                    return updatedOrder;
                } else {
                    logger.error("SimAL API returned null response body for production order {}", 
                            order.getProductionOrderNumber());
                    throw new ProductionPlanningException("SimAL API returned null response body");
                }
            } else {
                logger.error("Failed to submit production order {} to SimAL: HTTP {}", 
                        order.getProductionOrderNumber(), response.getStatusCode());
                throw new ProductionPlanningException("SimAL API returned error: " + response.getStatusCode());
            }
        } catch (ProductionPlanningException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error submitting production order {} to SimAL: {}", 
                    order.getProductionOrderNumber(), e.getMessage(), e);
            throw new ProductionPlanningException("Failed to submit production order " + 
                    order.getProductionOrderNumber() + " to SimAL: " + e.getMessage(), e);
        }
    }

    /**
     * Get scheduled tasks for a production order from SimAL.
     */
    public List<Map<String, Object>> getScheduledTasks(String simalScheduleId) {
        try {
            String url = simalApiBaseUrl + simalScheduledOrdersPath + simalScheduleId;
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> tasks = (List<Map<String, Object>>) responseBody.get("tasks");
                    logger.info("Retrieved {} scheduled tasks for schedule {}", tasks != null ? tasks.size() : 0, simalScheduleId);
                    return tasks != null ? tasks : new ArrayList<>();
                }
                return new ArrayList<>();
            } else {
                logger.warn("Failed to get scheduled tasks for schedule {}: HTTP {}", 
                        simalScheduleId, response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error retrieving scheduled tasks for schedule {}: {}", simalScheduleId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Update production order progress from SimAL.
     * Called periodically or when receiving status update notifications from SimAL.
     */
    public ProductionOrderDTO updateProductionProgress(Long productionOrderId) {
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));

        if (order.getSimalScheduleId() == null) {
            logger.warn("Production order {} not linked to SimAL schedule", order.getProductionOrderNumber());
            return order;
        }

        try {
            String url = simalApiBaseUrl + simalScheduledOrdersPath + order.getSimalScheduleId() + "/status";
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    String status = (String) responseBody.get("status");

                    // Update production order status based on SimAL status
                    String newStatus = mapSimalStatusToPOStatus(status);
                    if (!newStatus.equals(order.getStatus())) {
                        order = productionOrderService.updateProductionOrderStatus(productionOrderId, newStatus);
                        logger.info("Updated production order {} to status {} based on SimAL", 
                                order.getProductionOrderNumber(), newStatus);
                    }

                    return order;
                }
                return order;
            } else {
                logger.warn("Failed to get production progress for schedule {}: HTTP {}", 
                        order.getSimalScheduleId(), response.getStatusCode());
                return order;
            }
        } catch (Exception e) {
            logger.error("Error updating production progress for order {}: {}", 
                    order.getProductionOrderNumber(), e.getMessage(), e);
            return order;
        }
    }

    /**
     * Start production for a scheduled order in SimAL.
     */
    public ProductionOrderDTO startProduction(Long productionOrderId) {
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));

        if (!STATUS_SCHEDULED.equals(order.getStatus())) {
            throw new IllegalStateException("Cannot start production - order status is " + order.getStatus());
        }

        try {
            String url = simalApiBaseUrl + simalScheduledOrdersPath + order.getSimalScheduleId() + "/start";
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HashMap<>(), Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                order = productionOrderService.updateProductionOrderStatus(productionOrderId, STATUS_IN_PRODUCTION);
                logger.info("Started production for order {} in SimAL", order.getProductionOrderNumber());
                return order;
            } else {
                throw new ProductionPlanningException("SimAL API returned error: " + response.getStatusCode());
            }
        } catch (ProductionPlanningException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error starting production for order {}: {}", order.getProductionOrderNumber(), e.getMessage(), e);
            throw new ProductionPlanningException("Failed to start production for order " +
                    order.getProductionOrderNumber() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Complete production for an order in SimAL.
     */
    public ProductionOrderDTO completeProduction(Long productionOrderId) {
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));

        try {
            String url = simalApiBaseUrl + simalScheduledOrdersPath + order.getSimalScheduleId() + "/complete";
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HashMap<>(), Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                order = productionOrderService.completeProductionOrder(productionOrderId);
                logger.info("Completed production for order {} in SimAL", order.getProductionOrderNumber());
                return order;
            } else {
                throw new ProductionPlanningException("SimAL API returned error: " + response.getStatusCode());
            }
        } catch (ProductionPlanningException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error completing production for order {}: {}", order.getProductionOrderNumber(), e.getMessage(), e);
            throw new ProductionPlanningException("Failed to complete production for order " +
                    order.getProductionOrderNumber() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Process SimAL output to create ProductionControlOrder and AssemblyControlOrder.
     * Called when production order enters IN_PRODUCTION status.
     */
    public void createControlOrdersFromSimalSchedule(Long productionOrderId, String simalScheduleId) {
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));

        try {
            // Get scheduled tasks from SimAL
            List<Map<String, Object>> tasks = getScheduledTasks(simalScheduleId);
            
            if (tasks != null && !tasks.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startTime = order.getExpectedCompletionTime() != null ? 
                    order.getExpectedCompletionTime() : now.plusHours(1);

                // Create ProductionControlOrder
                productionControlOrderService.createControlOrder(
                        productionOrderId,
                        PRODUCTION_CONTROL_WORKSTATION_ID,
                        simalScheduleId,
                        order.getPriority(),
                        now,
                        startTime,
                        "Production process for order " + order.getProductionOrderNumber(),
                        "Check output quality, verify dimensions, inspect surface finish",
                        "Follow safety protocols, use protective equipment",
                        order.getEstimatedDuration() != null ? order.getEstimatedDuration() : 120
                );

                // Create AssemblyControlOrder to be started after production
                assemblyControlOrderService.createControlOrder(
                        productionOrderId,
                        ASSEMBLY_CONTROL_WORKSTATION_ID,
                        simalScheduleId,
                        order.getPriority(),
                        startTime.plusMinutes(order.getEstimatedDuration() != null ? order.getEstimatedDuration() : 120),
                        startTime.plusMinutes(order.getEstimatedDuration() != null ? order.getEstimatedDuration() * 2 : 240),
                        "Assembly instructions for order " + order.getProductionOrderNumber(),
                        "Verify all components assembled, test functionality",
                        "Test all features work correctly",
                        "Package according to customer requirements",
                        120
                );

                logger.info("Created production and assembly control orders for production order {} from SimAL schedule {}", 
                        order.getProductionOrderNumber(), simalScheduleId);
            }
        } catch (Exception e) {
            logger.error("Error creating control orders from SimAL schedule {}: {}", simalScheduleId, e.getMessage(), e);
            throw new ProductionPlanningException("Failed to create control orders for production order " + 
                    order.getProductionOrderNumber() + " from SimAL schedule " + simalScheduleId + 
                    ": " + e.getMessage(), e);
        }
    }

    /**
     * Map SimAL status to production order status
     */
    private String mapSimalStatusToPOStatus(String simalStatus) {
        return switch (simalStatus) {
            case STATUS_SCHEDULED -> STATUS_SCHEDULED;
            case "IN_PROGRESS" -> STATUS_IN_PRODUCTION;
            case STATUS_COMPLETED -> STATUS_COMPLETED;
            case "FAILED", STATUS_CANCELLED -> STATUS_CANCELLED;
            default -> STATUS_SCHEDULED;
        };
    }

    /**
     * Inner class for SimAL Production Order Request
     */
    public static class SimalProductionOrderRequest {
        private String productionOrderNumber;
        private Long sourceCustomerOrderId;
        private LocalDateTime dueDate;
        private String priority;
        private String notes;

        // Getters and Setters
        public String getProductionOrderNumber() { return productionOrderNumber; }
        public void setProductionOrderNumber(String productionOrderNumber) { this.productionOrderNumber = productionOrderNumber; }

        public Long getSourceCustomerOrderId() { return sourceCustomerOrderId; }
        public void setSourceCustomerOrderId(Long sourceCustomerOrderId) { this.sourceCustomerOrderId = sourceCustomerOrderId; }

        public LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
