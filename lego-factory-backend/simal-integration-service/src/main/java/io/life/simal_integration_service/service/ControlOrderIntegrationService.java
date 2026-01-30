package io.life.simal_integration_service.service;

import io.life.simal_integration_service.dto.SimalAssemblyControlOrderRequest;
import io.life.simal_integration_service.dto.SimalProductionControlOrderRequest;
import io.life.simal_integration_service.dto.SimalScheduledOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service to integrate SimAL production schedules with Control Order system.
 * When SimAL produces a production schedule, this service creates ProductionControlOrder
 * and AssemblyControlOrder entities in the order-processing-service.
 * 
 * Uses typed DTOs (SimalProductionControlOrderRequest, SimalAssemblyControlOrderRequest)
 * instead of Map<String, Object> for compile-time safety and better maintainability.
 */
@Service
@Slf4j
public class ControlOrderIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${simal.api.base-url:http://localhost:8016/api}")
    private String simalApiBaseUrl;

    @Value("${order-processing.api.base-url:http://localhost:8015/api}")
    private String orderProcessingApiBaseUrl;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public ControlOrderIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Process a SimAL production schedule and create corresponding control orders.
     * Segregates tasks by workstation type and creates appropriate control orders.
     *
     * @param schedule The SimAL scheduled order response
     * @param productionOrderId The production order ID in order-processing-service
     * @return Map of control order numbers created, keyed by workstation ID
     */
    public Map<String, String> createControlOrdersFromSchedule(
            SimalScheduledOrderResponse schedule,
            Long productionOrderId) {

        log.info("Creating control orders from SimAL schedule: {}", schedule.getScheduleId());

        Map<String, String> createdControlOrders = new HashMap<>();

        if (schedule.getScheduledTasks() == null || schedule.getScheduledTasks().isEmpty()) {
            log.warn("Schedule has no tasks: {}", schedule.getScheduleId());
            return createdControlOrders;
        }

        // Group tasks by workstation
        Map<String, List<SimalScheduledOrderResponse.ScheduledTask>> tasksByWorkstation =
                groupTasksByWorkstation(schedule.getScheduledTasks());

        // Create control orders for each workstation
        for (Map.Entry<String, List<SimalScheduledOrderResponse.ScheduledTask>> entry :
                tasksByWorkstation.entrySet()) {

            String workstationId = entry.getKey();
            List<SimalScheduledOrderResponse.ScheduledTask> tasks = entry.getValue();

            try {
                String controlOrderType = determineControlOrderType(workstationId);

                if ("PRODUCTION".equals(controlOrderType)) {
                    String controlOrderNumber = createProductionControlOrder(
                            productionOrderId,
                            workstationId,
                            schedule,
                            tasks
                    );
                    createdControlOrders.put(workstationId, controlOrderNumber);
                    log.info("Created ProductionControlOrder: {} for workstation: {}",
                            controlOrderNumber, workstationId);

                } else if ("ASSEMBLY".equals(controlOrderType)) {
                    String controlOrderNumber = createAssemblyControlOrder(
                            productionOrderId,
                            workstationId,
                            schedule,
                            tasks
                    );
                    createdControlOrders.put(workstationId, controlOrderNumber);
                    log.info("Created AssemblyControlOrder: {} for workstation: {}",
                            controlOrderNumber, workstationId);
                }

            } catch (RestClientException e) {
                log.error("Failed to create control order for workstation: {}", workstationId, e);
            }
        }

        return createdControlOrders;
    }

    /**
     * Create a ProductionControlOrder in the order-processing-service.
     * Uses typed SimalProductionControlOrderRequest DTO for compile-time safety.
     */
    private String createProductionControlOrder(
            Long productionOrderId,
            String workstationId,
            SimalScheduledOrderResponse schedule,
            List<SimalScheduledOrderResponse.ScheduledTask> tasks) {

        SimalScheduledOrderResponse.ScheduledTask firstTask = tasks.get(0);
        
        // Parse itemId safely
        Long itemId = null;
        if (firstTask.getItemId() != null) {
            try {
                itemId = Long.parseLong(firstTask.getItemId());
            } catch (NumberFormatException e) {
                log.warn("Could not parse itemId: {}", firstTask.getItemId());
            }
        }

        // Build typed request DTO
        SimalProductionControlOrderRequest request = SimalProductionControlOrderRequest.builder()
                .sourceProductionOrderId(productionOrderId)
                .assignedWorkstationId(parseWorkstationId(workstationId))
                .simalScheduleId(schedule.getScheduleId())
                .targetStartTime(firstTask.getStartTime())
                .targetCompletionTime(calculateCompletionTime(tasks))
                .priority(determinePriority(schedule.getOrderNumber()))
                .productionInstructions(buildProductionInstructions(tasks))
                .qualityCheckpoints(buildQualityCheckpoints(tasks))
                .itemId(itemId)
                .itemType("PART") // Manufacturing workstations produce PARTs
                .quantity(firstTask.getQuantity())
                .build();

        String url = orderProcessingApiBaseUrl + "/production-control-orders";
        log.debug("Posting ProductionControlOrder to: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SimalProductionControlOrderRequest> entity = new HttpEntity<>(request, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("controlOrderNumber")) {
                return response.get("controlOrderNumber").toString();
            }
        } catch (Exception e) {
            log.error("Error creating ProductionControlOrder", e);
        }

        return "PCO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Create an AssemblyControlOrder in the order-processing-service.
     * Uses typed SimalAssemblyControlOrderRequest DTO for compile-time safety.
     */
    private String createAssemblyControlOrder(
            Long productionOrderId,
            String workstationId,
            SimalScheduledOrderResponse schedule,
            List<SimalScheduledOrderResponse.ScheduledTask> tasks) {

        SimalScheduledOrderResponse.ScheduledTask firstTask = tasks.get(0);
        
        // Parse itemId safely
        Long itemId = null;
        if (firstTask.getItemId() != null) {
            try {
                itemId = Long.parseLong(firstTask.getItemId());
            } catch (NumberFormatException e) {
                log.warn("Could not parse itemId: {}", firstTask.getItemId());
            }
        }

        // Build typed request DTO
        SimalAssemblyControlOrderRequest request = SimalAssemblyControlOrderRequest.builder()
                .sourceProductionOrderId(productionOrderId)
                .assignedWorkstationId(parseWorkstationId(workstationId))
                .simalScheduleId(schedule.getScheduleId())
                .targetStartTime(firstTask.getStartTime())
                .targetCompletionTime(calculateCompletionTime(tasks))
                .priority(determinePriority(schedule.getOrderNumber()))
                .assemblyInstructions(buildAssemblyInstructions(tasks))
                .qualityCheckpoints(buildQualityStandards(tasks))
                .itemId(itemId)
                .itemType("MODULE") // Assembly workstations produce MODULEs
                .quantity(firstTask.getQuantity())
                .build();

        String url = orderProcessingApiBaseUrl + "/assembly-control-orders";
        log.debug("Posting AssemblyControlOrder to: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SimalAssemblyControlOrderRequest> entity = new HttpEntity<>(request, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("controlOrderNumber")) {
                return response.get("controlOrderNumber").toString();
            }
        } catch (Exception e) {
            log.error("Error creating AssemblyControlOrder", e);
        }

        return "ACO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Group scheduled tasks by workstation.
     */
    private Map<String, List<SimalScheduledOrderResponse.ScheduledTask>> groupTasksByWorkstation(
            List<SimalScheduledOrderResponse.ScheduledTask> tasks) {

        Map<String, List<SimalScheduledOrderResponse.ScheduledTask>> grouped = new LinkedHashMap<>();

        for (SimalScheduledOrderResponse.ScheduledTask task : tasks) {
            grouped.computeIfAbsent(task.getWorkstationId(), k -> new ArrayList<>()).add(task);
        }

        return grouped;
    }

    /**
     * Determine control order type based on workstation ID.
     * WS-1, WS-2, WS-3 = PRODUCTION (Manufacturing)
     * WS-4, WS-5, WS-6 = ASSEMBLY
     * WS-7, WS-8, WS-9 = Warehouses (handled separately)
     */
    private String determineControlOrderType(String workstationId) {
        if (workstationId.matches("WS-[1-3]")) {
            return "PRODUCTION";
        } else if (workstationId.matches("WS-[4-6]")) {
            return "ASSEMBLY";
        }
        return "UNKNOWN";
    }

    /**
     * Extract numeric ID from workstation ID (e.g., "WS-1" -> 1).
     */
    private Long parseWorkstationId(String workstationId) {
        try {
            return Long.parseLong(workstationId.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.warn("Could not parse workstation ID: {}", workstationId);
            return 1L;
        }
    }

    /**
     * Determine priority from order number or other criteria.
     */
    private String determinePriority(String orderNumber) {
        // In a real system, this would check customer priority, due date, etc.
        return "MEDIUM";
    }

    /**
     * Calculate completion time from task list.
     */
    private String calculateCompletionTime(List<SimalScheduledOrderResponse.ScheduledTask> tasks) {
        if (tasks.isEmpty()) {
            return ISO_FORMATTER.format(LocalDateTime.now().plusHours(1));
        }
        SimalScheduledOrderResponse.ScheduledTask lastTask = tasks.get(tasks.size() - 1);
        return lastTask.getEndTime();
    }

    /**
     * Build production instructions from tasks.
     */
    private String buildProductionInstructions(
            List<SimalScheduledOrderResponse.ScheduledTask> tasks) {

        StringBuilder instructions = new StringBuilder();
        instructions.append("Production Schedule:\n");

        for (int i = 0; i < tasks.size(); i++) {
            SimalScheduledOrderResponse.ScheduledTask task = tasks.get(i);
            instructions.append("\nStep ").append(i + 1).append(":\n");
            instructions.append("  Item: ").append(task.getItemName()).append("\n");
            instructions.append("  Quantity: ").append(task.getQuantity()).append("\n");
            instructions.append("  Duration: ").append(task.getDuration()).append(" minutes\n");
            instructions.append("  Time: ").append(task.getStartTime()).append(" to ")
                    .append(task.getEndTime()).append("\n");
        }

        return instructions.toString();
    }

    /**
     * Build quality checkpoints from tasks.
     */
    private String buildQualityCheckpoints(
            List<SimalScheduledOrderResponse.ScheduledTask> tasks) {

        StringBuilder checkpoints = new StringBuilder();
        checkpoints.append("Quality Checkpoints:\n");

        for (int i = 0; i < tasks.size(); i++) {
            SimalScheduledOrderResponse.ScheduledTask task = tasks.get(i);
            checkpoints.append("\nAfter Step ").append(i + 1).append(":\n");
            checkpoints.append("  - Verify ").append(task.getItemName())
                    .append(" dimensions\n");
            checkpoints.append("  - Check quality standards\n");
            checkpoints.append("  - Document completion time\n");
        }

        return checkpoints.toString();
    }

    /**
     * Build assembly instructions from tasks.
     */
    private String buildAssemblyInstructions(
            List<SimalScheduledOrderResponse.ScheduledTask> tasks) {

        StringBuilder instructions = new StringBuilder();
        instructions.append("Assembly Instructions:\n");

        for (int i = 0; i < tasks.size(); i++) {
            SimalScheduledOrderResponse.ScheduledTask task = tasks.get(i);
            instructions.append("\nStep ").append(i + 1).append(":\n");
            instructions.append("  Component: ").append(task.getItemName()).append("\n");
            instructions.append("  Quantity: ").append(task.getQuantity()).append("\n");
            instructions.append("  Estimated Time: ").append(task.getDuration())
                    .append(" minutes\n");
        }

        return instructions.toString();
    }

    /**
     * Build quality standards from tasks.
     */
    private String buildQualityStandards(
            List<SimalScheduledOrderResponse.ScheduledTask> tasks) {

        return "Assembly Quality Standards:\n" +
                "- All components must be properly aligned\n" +
                "- Torque specifications must be followed\n" +
                "- Final assembly must pass visual inspection\n" +
                "- Test functionality before completion\n" +
                "- Document any defects or rework required";
    }

    /**
     * Schedule a production order by fetching it from order-processing-service
     * and submitting to SimAL with production order items (or warehouse order items as fallback).
     *
     * @param productionOrderId The production order ID
     * @param controller The SimalController instance to call submitProductionOrder
     * @return The scheduled order response from SimAL
     */
    public io.life.simal_integration_service.dto.SimalScheduledOrderResponse scheduleProductionOrderById(
            Long productionOrderId,
            Object controller) {

        log.info("Fetching production order {} to schedule", productionOrderId);

        // 1. Fetch production order from order-processing-service
        String productionOrderUrl = orderProcessingApiBaseUrl + "/production-orders/" + productionOrderId;
        Map<String, Object> productionOrder = restTemplate.getForObject(productionOrderUrl, Map.class);

        if (productionOrder == null) {
            throw new IllegalArgumentException("Production order not found: " + productionOrderId);
        }

        log.info("Retrieved production order: {}", productionOrder.get("productionOrderNumber"));

        // 2. Check if production order has its own items first
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productionOrderItems =
                (List<Map<String, Object>>) productionOrder.get("productionOrderItems");

        List<io.life.simal_integration_service.dto.SimalProductionOrderRequest.OrderLineItem> lineItems =
                new ArrayList<>();

        if (productionOrderItems != null && !productionOrderItems.isEmpty()) {
            // Use production order items directly
            log.info("Using {} items from production order", productionOrderItems.size());
            
            for (Map<String, Object> item : productionOrderItems) {
                io.life.simal_integration_service.dto.SimalProductionOrderRequest.OrderLineItem lineItem =
                        new io.life.simal_integration_service.dto.SimalProductionOrderRequest.OrderLineItem();

                Long itemId = getLongValue(item.get("itemId"));
                String itemType = (String) item.get("itemType");
                String workstationType = (String) item.get("workstationType");
                
                lineItem.setItemId(itemId != null ? itemId.toString() : null);
                
                // Fetch actual item name from masterdata-service
                String actualItemName = null;
                if (itemId != null && itemType != null && controller instanceof io.life.simal_integration_service.controller.SimalController) {
                    io.life.simal_integration_service.controller.SimalController simalController =
                            (io.life.simal_integration_service.controller.SimalController) controller;
                    actualItemName = simalController.getItemName(itemId, itemType);
                }
                
                lineItem.setItemName(actualItemName != null ? actualItemName : (String) item.get("itemName"));
                lineItem.setQuantity(getIntValue(item.get("quantity")));
                lineItem.setEstimatedDuration(getIntValue(item.get("estimatedTimeMinutes")) != null ? 
                        getIntValue(item.get("estimatedTimeMinutes")) : 30);
                lineItem.setWorkstationType(workstationType != null ? workstationType : "MANUFACTURING");

                lineItems.add(lineItem);
            }
        } else {
            // Fallback: Get items from warehouse order
            Object warehouseOrderIdObj = productionOrder.get("sourceWarehouseOrderId");
            if (warehouseOrderIdObj == null) {
                throw new IllegalArgumentException(
                        "Production order has no items and no associated warehouse order: " + productionOrderId);
            }

            Long warehouseOrderId = warehouseOrderIdObj instanceof Number ?
                    ((Number) warehouseOrderIdObj).longValue() : Long.parseLong(warehouseOrderIdObj.toString());

            // Fetch warehouse order with items
            String warehouseOrderUrl = orderProcessingApiBaseUrl + "/warehouse-orders/" + warehouseOrderId;
            Map<String, Object> warehouseOrder = restTemplate.getForObject(warehouseOrderUrl, Map.class);

            if (warehouseOrder == null) {
                throw new IllegalArgumentException("Warehouse order not found: " + warehouseOrderId);
            }

            // Extract warehouse order items (field is "orderItems" in DTO)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> warehouseOrderItems =
                    (List<Map<String, Object>>) warehouseOrder.get("orderItems");

            if (warehouseOrderItems == null || warehouseOrderItems.isEmpty()) {
                throw new IllegalArgumentException(
                        "Warehouse order has no items: " + warehouseOrderId);
            }

            log.info("Using {} items from warehouse order (fallback)", warehouseOrderItems.size());

            for (Map<String, Object> item : warehouseOrderItems) {
                io.life.simal_integration_service.dto.SimalProductionOrderRequest.OrderLineItem lineItem =
                        new io.life.simal_integration_service.dto.SimalProductionOrderRequest.OrderLineItem();

                Long itemId = getLongValue(item.get("itemId"));
                String itemType = (String) item.get("itemType");
                
                lineItem.setItemId(itemId != null ? itemId.toString() : null);
                
                // Fetch actual item name from masterdata-service
                String actualItemName = null;
                if (itemId != null && itemType != null && controller instanceof io.life.simal_integration_service.controller.SimalController) {
                    io.life.simal_integration_service.controller.SimalController simalController =
                            (io.life.simal_integration_service.controller.SimalController) controller;
                    actualItemName = simalController.getItemName(itemId, itemType);
                }
                
                lineItem.setItemName(actualItemName != null ? actualItemName : (String) item.get("itemName"));
                lineItem.setQuantity(getIntValue(item.get("requestedQuantity")));
                lineItem.setEstimatedDuration(30);
                lineItem.setWorkstationType("MANUFACTURING");

                lineItems.add(lineItem);
            }
        }

        // Build SimAL production order request
        io.life.simal_integration_service.dto.SimalProductionOrderRequest request =
                new io.life.simal_integration_service.dto.SimalProductionOrderRequest();

        request.setOrderNumber((String) productionOrder.get("productionOrderNumber"));
        request.setPriority((String) productionOrder.get("priority"));
        request.setDueDate((String) productionOrder.get("dueDate"));
        request.setLineItems(lineItems);

        // Submit to SimAL via controller
        try {
            io.life.simal_integration_service.controller.SimalController simalController =
                    (io.life.simal_integration_service.controller.SimalController) controller;

            org.springframework.http.ResponseEntity<io.life.simal_integration_service.dto.SimalScheduledOrderResponse> response =
                    simalController.submitProductionOrder(request);

            log.info("Successfully scheduled production order {} in SimAL", productionOrderId);

            // 8. Automatically create control orders from the schedule
            io.life.simal_integration_service.dto.SimalScheduledOrderResponse scheduleResponse = response.getBody();
            if (scheduleResponse != null && scheduleResponse.getScheduleId() != null) {
                try {
                    log.info("Auto-creating control orders for schedule: {}", scheduleResponse.getScheduleId());
                    Map<String, String> createdOrders = createControlOrdersFromSchedule(
                            scheduleResponse, productionOrderId);
                    log.info("Created {} control orders for production order {}", 
                            createdOrders.size(), productionOrderId);
                } catch (Exception controlOrderError) {
                    log.error("Failed to auto-create control orders (schedule still created): {}", 
                            controlOrderError.getMessage());
                    // Don't fail the whole operation if control order creation fails
                }
            }

            return scheduleResponse;

        } catch (Exception e) {
            log.error("Error submitting production order to SimAL", e);
            throw new RuntimeException("Failed to schedule production order in SimAL: " + e.getMessage(), e);
        }
    }

    private Long getLongValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return value != null ? Long.parseLong(value.toString()) : null;
    }

    private Integer getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return value != null ? Integer.parseInt(value.toString()) : null;
    }
}