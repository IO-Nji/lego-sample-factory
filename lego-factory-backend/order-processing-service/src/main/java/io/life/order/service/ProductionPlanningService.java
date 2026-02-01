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
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_DISPATCHED = "DISPATCHED";
    private static final String STATUS_IN_PRODUCTION = "IN_PRODUCTION";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ProductionOrderService productionOrderService;
    private final ProductionControlOrderService productionControlOrderService;
    private final AssemblyControlOrderService assemblyControlOrderService;
    private final OrderOrchestrationService orderOrchestrationService;
    private final RestTemplate restTemplate;

    @Value("${simal.api.base-url:http://localhost:8016/api}")
    private String simalApiBaseUrl;

    @Value("${simal.api.scheduled-orders-path:/simal/scheduled-orders/}")
    private String simalScheduledOrdersPath;

    public ProductionPlanningService(
            ProductionOrderService productionOrderService,
            ProductionControlOrderService productionControlOrderService,
            AssemblyControlOrderService assemblyControlOrderService,
            OrderOrchestrationService orderOrchestrationService,
            RestTemplate restTemplate) {
        this.productionOrderService = productionOrderService;
        this.productionControlOrderService = productionControlOrderService;
        this.assemblyControlOrderService = assemblyControlOrderService;
        this.orderOrchestrationService = orderOrchestrationService;
        this.restTemplate = restTemplate;
    }

    /**
     * Submit a production order to SimAL for scheduling.
     * Sends order details and receives a schedule ID and estimated duration.
     * Order must be in CONFIRMED status to be scheduled.
     */
    public ProductionOrderDTO submitProductionOrderToSimal(Long productionOrderId) {
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));

        // Check if already submitted or scheduled
        if (STATUS_SUBMITTED.equals(order.getStatus()) || STATUS_SCHEDULED.equals(order.getStatus())) {
            logger.warn("Production order {} already submitted or scheduled", order.getProductionOrderNumber());
            return order;
        }

        // Must be in CONFIRMED status to schedule
        if (!STATUS_CONFIRMED.equals(order.getStatus())) {
            throw new ProductionPlanningException("Production order must be CONFIRMED before scheduling. Current status: " + order.getStatus());
        }

        try {
            // Create request payload for SimAL
            SimalProductionOrderRequest request = new SimalProductionOrderRequest();
            request.setOrderNumber(order.getProductionOrderNumber());
            request.setDueDate(order.getDueDate() != null ? order.getDueDate().toString().substring(0, 10) : null);
            request.setPriority(order.getPriority());
            request.setNotes(order.getNotes());
            
            // Add line items with workstation types
            if (order.getProductionOrderItems() != null) {
                List<SimalLineItem> lineItems = new ArrayList<>();
                for (ProductionOrderDTO.ProductionOrderItemDTO item : order.getProductionOrderItems()) {
                    SimalLineItem lineItem = new SimalLineItem();
                    lineItem.setItemId(String.valueOf(item.getItemId()));
                    lineItem.setItemName(item.getItemName());
                    lineItem.setQuantity(item.getQuantity());
                    lineItem.setEstimatedDuration(item.getEstimatedTimeMinutes());
                    lineItem.setWorkstationType(item.getWorkstationType());
                    lineItems.add(lineItem);
                }
                request.setLineItems(lineItems);
            }

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
     * Dispatch production to workstations by creating control orders.
     * This moves the order from SCHEDULED to DISPATCHED status.
     * Control orders (ProductionControlOrder and AssemblyControlOrder) are automatically created.
     */
    public ProductionOrderDTO dispatchProduction(Long productionOrderId) {
        logger.info("=== DISPATCH PRODUCTION REQUESTED FOR ORDER ID: {} ===", productionOrderId);
        
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));

        logger.info("Found production order: {} with status: {} and scheduleId: {}",
                order.getProductionOrderNumber(), order.getStatus(), order.getSimalScheduleId());

        if (!STATUS_SCHEDULED.equals(order.getStatus())) {
            logger.error("Cannot dispatch - wrong status. Expected SCHEDULED, got: {}", order.getStatus());
            throw new IllegalStateException("Cannot dispatch production - order must be SCHEDULED, current status: " + order.getStatus());
        }

        try {
            // Call SimAL to create control orders from the schedule
            String url = simalApiBaseUrl + "/simal/scheduled-orders/" + order.getSimalScheduleId() + "/create-control-orders";
            String fullUrl = url + "?productionOrderId=" + productionOrderId;
            
            logger.info("Calling SimAL create-control-orders endpoint: {}", fullUrl);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, new HashMap<>(), Map.class);

            logger.info("SimAL response status: {}, body: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                // Update status to DISPATCHED
                order = productionOrderService.updateProductionOrderStatus(productionOrderId, STATUS_DISPATCHED);
                logger.info("✓ Dispatched production for order {} - control orders created", order.getProductionOrderNumber());
                return order;
            } else {
                logger.error("SimAL returned non-2xx status: {}", response.getStatusCode());
                throw new ProductionPlanningException("SimAL API returned error: " + response.getStatusCode());
            }
        } catch (ProductionPlanningException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error dispatching production for order {}: {}", order.getProductionOrderNumber(), e.getMessage(), e);
            throw new ProductionPlanningException("Failed to dispatch production for order " +
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
     * Manually submit a completed production order for downstream processing.
     * This is called by Production Planning AFTER production is complete.
     * 
     * SCENARIO 3: Credits Modules Supermarket inventory and updates Warehouse Order
     * SCENARIO 4: Creates Final Assembly orders for the customer order
     * 
     * @param productionOrderId The completed production order ID
     * @return Production order DTO with updated status
     */
    public ProductionOrderDTO submitForFinalAssembly(Long productionOrderId) {
        ProductionOrderDTO order = productionOrderService.getProductionOrderById(productionOrderId)
                .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));
        
        // Must be in COMPLETED status to submit
        if (!STATUS_COMPLETED.equals(order.getStatus())) {
            throw new ProductionPlanningException(
                    "Production order must be COMPLETED before submission. Current status: " + order.getStatus());
        }
        
        try {
            // Delegate to OrderOrchestrationService for downstream processing
            orderOrchestrationService.submitProductionOrderCompletion(productionOrderId);
            
            logger.info("Submitted production order {} for downstream processing", 
                    order.getProductionOrderNumber());
            
            // Return the updated production order
            return productionOrderService.getProductionOrderById(productionOrderId)
                    .orElseThrow(() -> new ProductionPlanningException(PRODUCTION_ORDER_NOT_FOUND + productionOrderId));
            
        } catch (Exception e) {
            logger.error("Error submitting production order {} for downstream processing: {}", 
                    order.getProductionOrderNumber(), e.getMessage(), e);
            throw new ProductionPlanningException(
                    "Failed to submit production order: " + e.getMessage(), e);
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
     * Matches the format expected by simal-integration-service
     */
    public static class SimalProductionOrderRequest {
        private String orderNumber;
        private String customerName;
        private String dueDate;  // ISO 8601 format: YYYY-MM-DD
        private String priority;
        private String notes;
        private List<SimalLineItem> lineItems;

        // Getters and Setters
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public List<SimalLineItem> getLineItems() { return lineItems; }
        public void setLineItems(List<SimalLineItem> lineItems) { this.lineItems = lineItems; }
    }

    /**
     * Inner class for SimAL Line Item
     */
    public static class SimalLineItem {
        private String itemId;
        private String itemName;
        private Integer quantity;
        private Integer estimatedDuration;
        private String workstationType;

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Integer getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

        public String getWorkstationType() { return workstationType; }
        public void setWorkstationType(String workstationType) { this.workstationType = workstationType; }
    }
}
