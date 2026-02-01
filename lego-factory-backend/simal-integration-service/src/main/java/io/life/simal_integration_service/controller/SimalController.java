package io.life.simal_integration_service.controller;

import io.life.simal_integration_service.dto.*;
import io.life.simal_integration_service.entity.ScheduledOrder;
import io.life.simal_integration_service.entity.ScheduledTask;
import io.life.simal_integration_service.repository.ScheduledOrderRepository;
import io.life.simal_integration_service.repository.ScheduledTaskRepository;
import io.life.simal_integration_service.service.ControlOrderIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SimAL Integration Controller with Database Persistence.
 * Provides endpoints for production order scheduling and status updates.
 * Data is persisted in database (H2/PostgreSQL).
 */
@RestController
@RequestMapping("/api/simal")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:1011", "http://localhost"})
@Tag(name = "SimAL Scheduling", description = "Production scheduling and Gantt chart integration for factory execution")
public class SimalController {

    private static final Logger log = LoggerFactory.getLogger(SimalController.class);
    private static final String SCHEDULE_ID = "scheduleId";
    private static final String STATUS = "status";

    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;
    private final ControlOrderIntegrationService controlOrderIntegrationService;
    private final ScheduledOrderRepository scheduledOrderRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final RestTemplate restTemplate;

    @Value("${masterdata.api.base-url:http://masterdata-service:8013/api}")
    private String masterdataApiBaseUrl;

    public SimalController(ControlOrderIntegrationService controlOrderIntegrationService,
                          ScheduledOrderRepository scheduledOrderRepository,
                          ScheduledTaskRepository scheduledTaskRepository,
                          RestTemplate restTemplate) {
        this.controlOrderIntegrationService = controlOrderIntegrationService;
        this.scheduledOrderRepository = scheduledOrderRepository;
        this.scheduledTaskRepository = scheduledTaskRepository;
        this.restTemplate = restTemplate;
    }

    @Operation(summary = "Submit production order for scheduling",
               description = "Generates a schedule with realistic task assignments and saves to database")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Schedule created with task assignments"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/production-order")
    public ResponseEntity<SimalScheduledOrderResponse> submitProductionOrder(
            @RequestBody SimalProductionOrderRequest request) {

        log.info("Received production order: {}", request.getOrderNumber());

        // Generate schedule ID
        String scheduleId = "SCHED-" + System.currentTimeMillis();

        // Generate scheduled tasks based on order items
        List<SimalScheduledOrderResponse.ScheduledTask> taskDtos = generateScheduledTasks(request, scheduleId);

        // Calculate total duration
        int totalDuration = taskDtos.stream()
                .mapToInt(task -> task.getDuration() != null ? task.getDuration() : 0)
                .sum();

        LocalDateTime estimatedCompletion = LocalDateTime.now().plusMinutes(totalDuration);

        // Create entity to persist
        ScheduledOrder orderEntity = ScheduledOrder.builder()
                .scheduleId(scheduleId)
                .orderNumber(request.getOrderNumber())
                .status("SCHEDULED")
                .estimatedCompletionTime(estimatedCompletion)
                .totalDuration(totalDuration)
                .build();

        // Save order first to get ID
        ScheduledOrder savedOrder = scheduledOrderRepository.save(orderEntity);
        log.info("Saved scheduled order to database: {}", scheduleId);

        // Convert DTOs to entities and set the saved order reference (use final reference for lambda)
        final ScheduledOrder finalSavedOrder = savedOrder;
        List<ScheduledTask> taskEntities = taskDtos.stream()
                .map(taskDto -> ScheduledTask.builder()
                        .taskId(taskDto.getTaskId())
                        .itemId(taskDto.getItemId())
                        .itemName(taskDto.getItemName())
                        .quantity(taskDto.getQuantity())
                        .workstationId(taskDto.getWorkstationId())
                        .workstationName(taskDto.getWorkstationName())
                        .startTime(LocalDateTime.parse(taskDto.getStartTime(), isoFormatter))
                        .endTime(LocalDateTime.parse(taskDto.getEndTime(), isoFormatter))
                        .duration(taskDto.getDuration())
                        .status(taskDto.getStatus())
                        .sequence(taskDto.getSequence())
                        .scheduledOrder(finalSavedOrder)
                        .build())
                .collect(Collectors.toList());

        savedOrder.getScheduledTasks().addAll(taskEntities);

        // Save again to persist tasks
        savedOrder = scheduledOrderRepository.save(savedOrder);
        log.info("Saved {} tasks for schedule: {}", taskEntities.size(), scheduleId);

        // Build response
        SimalScheduledOrderResponse response = SimalScheduledOrderResponse.builder()
                .scheduleId(scheduleId)
                .orderNumber(request.getOrderNumber())
                .status("SCHEDULED")
                .estimatedCompletionTime(isoFormatter.format(estimatedCompletion))
                .scheduledTasks(taskDtos)
                .totalDuration(totalDuration)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Schedule production (alias)", description = "Alias for /production-order endpoint")
    @ApiResponse(responseCode = "201", description = "Schedule created")
    @PostMapping("/schedule")
    public ResponseEntity<SimalScheduledOrderResponse> schedule(
            @RequestBody SimalProductionOrderRequest request) {
        return submitProductionOrder(request);
    }

    @Operation(summary = "Get all scheduled orders", description = "Retrieve all scheduled orders from database")
    @ApiResponse(responseCode = "200", description = "List of scheduled orders")
    @GetMapping("/scheduled-orders")
    public ResponseEntity<List<SimalScheduledOrderResponse>> getScheduledOrders() {
        List<ScheduledOrder> orders = scheduledOrderRepository.findAllByOrderByCreatedAtDesc();
        log.info("Fetching all scheduled orders from database. Total: {}", orders.size());
        
        List<SimalScheduledOrderResponse> responses = orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get scheduled order by ID", description = "Retrieve a specific scheduled order by schedule ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scheduled order found"),
        @ApiResponse(responseCode = "404", description = "Schedule not found")
    })
    @GetMapping("/scheduled-orders/{scheduleId}")
    public ResponseEntity<SimalScheduledOrderResponse> getScheduledOrder(
            @Parameter(description = "Schedule ID") @PathVariable String scheduleId) {
        log.info("Fetching schedule from database: {}", scheduleId);

        Optional<ScheduledOrder> orderOpt = scheduledOrderRepository.findByScheduleId(scheduleId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        SimalScheduledOrderResponse response = convertToResponse(orderOpt.get());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update task production time", 
               description = "Update actual execution time and status for a scheduled task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated"),
        @ApiResponse(responseCode = "404", description = "Schedule or task not found")
    })
    @PostMapping("/update-time")
    public ResponseEntity<SimalScheduledOrderResponse> updateProductionTime(
            @RequestBody SimalUpdateTimeRequest request) {

        log.info("Updating time for schedule: {}, task: {}", request.getScheduleId(), request.getTaskId());

        // Find order in database
        ScheduledOrder order = scheduledOrderRepository.findByScheduleId(request.getScheduleId())
                .orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Find and update the task
        ScheduledTask taskToUpdate = order.getScheduledTasks()
                .stream()
                .filter(task -> task.getTaskId().equals(request.getTaskId()))
                .findFirst()
                .orElse(null);

        if (taskToUpdate == null) {
            return ResponseEntity.notFound().build();
        }

        // Update task with actual times
        if (request.getActualStartTime() != null) {
            try {
                LocalDateTime startTime = LocalDateTime.parse(request.getActualStartTime(), isoFormatter);
                taskToUpdate.setStartTime(startTime);
            } catch (Exception e) {
                log.error("Failed to parse actualStartTime: {}", request.getActualStartTime(), e);
            }
        }
        if (request.getActualEndTime() != null) {
            try {
                LocalDateTime endTime = LocalDateTime.parse(request.getActualEndTime(), isoFormatter);
                taskToUpdate.setEndTime(endTime);
            } catch (Exception e) {
                log.error("Failed to parse actualEndTime: {}", request.getActualEndTime(), e);
            }
        }
        if (request.getActualDuration() != null) {
            taskToUpdate.setDuration(request.getActualDuration());
        }
        if (request.getStatus() != null) {
            taskToUpdate.setStatus(request.getStatus());
        }

        // Save updated task
        scheduledTaskRepository.save(taskToUpdate);

        // Update overall order status based on tasks
        updateOrderStatusFromTasks(order);
        scheduledOrderRepository.save(order);

        return ResponseEntity.ok(convertToResponse(order));
    }

    @Operation(summary = "Get schedule by order number", description = "Retrieve scheduled order by customer/production order number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schedule found"),
        @ApiResponse(responseCode = "404", description = "No schedule found for order")
    })
    @GetMapping("/scheduled-orders/order/{orderNumber}")
    public ResponseEntity<SimalScheduledOrderResponse> getScheduleByOrderNumber(
            @Parameter(description = "Order number (e.g., PO-001)") @PathVariable String orderNumber) {
        log.info("Fetching schedule for order: {}", orderNumber);

        ScheduledOrder order = scheduledOrderRepository.findByOrderNumber(orderNumber)
                .stream()
                .findFirst()
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToResponse(order));
    }

    /**
     * Helper method to generate scheduled tasks based on order items.
     * Creates one task per MODULE based on its productionWorkstationId.
     * 
     * IMPORTANT: Each module knows which workstation produces it via productionWorkstationId.
     * SimAL creates a task for that specific workstation, then ControlOrderIntegrationService
     * creates the appropriate control order (Production or Assembly) based on workstation ID.
     */
    private List<SimalScheduledOrderResponse.ScheduledTask> generateScheduledTasks(
            SimalProductionOrderRequest request, String scheduleId) {

        List<SimalScheduledOrderResponse.ScheduledTask> tasks = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.now();
        int sequence = 1;

        if (request.getLineItems() != null) {
            for (SimalProductionOrderRequest.OrderLineItem item : request.getLineItems()) {
                int duration = item.getEstimatedDuration() != null ? item.getEstimatedDuration() : 30;

                // Fetch module details from masterdata to get productionWorkstationId
                String workstationId;
                try {
                    String moduleUrl = masterdataApiBaseUrl + "/masterdata/modules/" + item.getItemId();
                    Map<String, Object> moduleData = restTemplate.getForObject(moduleUrl, Map.class);
                    
                    if (moduleData != null && moduleData.containsKey("productionWorkstationId")) {
                        Integer wsId = (Integer) moduleData.get("productionWorkstationId");
                        workstationId = "WS-" + wsId;
                        log.debug("Module {} assigned to {}", item.getItemName(), workstationId);
                    } else {
                        // Fallback to workstationType-based assignment
                        String workstationType = item.getWorkstationType() != null ? 
                                item.getWorkstationType() : "MANUFACTURING";
                        workstationId = assignWorkstation(workstationType, sequence);
                        log.warn("Module {} has no productionWorkstationId, using workstationType: {} -> {}",
                                item.getItemName(), workstationType, workstationId);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch module data for {}: {}", item.getItemId(), e.getMessage());
                    // Fallback to workstationType-based assignment
                    String workstationType = item.getWorkstationType() != null ? 
                            item.getWorkstationType() : "MANUFACTURING";
                    workstationId = assignWorkstation(workstationType, sequence);
                }

                SimalScheduledOrderResponse.ScheduledTask task = 
                        SimalScheduledOrderResponse.ScheduledTask.builder()
                        .taskId("TASK-" + scheduleId + "-" + sequence)
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .workstationId(workstationId)
                        .workstationName(getWorkstationName(workstationId))
                        .startTime(isoFormatter.format(currentTime))
                        .endTime(isoFormatter.format(currentTime.plusMinutes(duration)))
                        .duration(duration)
                        .status("PENDING")
                        .sequence(sequence)
                        .build();

                tasks.add(task);

                // Move to next time slot
                currentTime = currentTime.plusMinutes((long) duration + 5L); // 5 min buffer
                sequence++;
            }
        }

        return tasks;
    }

    /**
     * Helper method to assign workstation based on type.
     * MANUFACTURING: WS-1 (Injection Molding), WS-2 (Parts Pre-Production), WS-3 (Part Finishing)
     * ASSEMBLY: WS-4 (Gear Assembly), WS-5 (Motor Assembly), WS-6 (Final Assembly)
     * WAREHOUSE: WS-8 (Modules Supermarket)
     * 
     * NOTE: This is a fallback only. Modules should specify their productionWorkstationId.
     */
    private String assignWorkstation(String workstationType, int sequenceNumber) {
        return switch (workstationType) {
            case "ASSEMBLY" -> "WS-" + (4 + (sequenceNumber % 3)); // WS-4, WS-5, WS-6
            case "WAREHOUSE" -> "WS-8";
            default -> "WS-" + (1 + (sequenceNumber % 3)); // MANUFACTURING: WS-1, WS-2, WS-3
        };
    }

    /**
     * Helper method to get workstation name from masterdata-service.
     */
    private String getWorkstationName(String workstationId) {
        try {
            // Parse workstation ID (e.g., "WS-1" -> 1)
            Long id = Long.parseLong(workstationId.replace("WS-", ""));
            
            // Fetch workstation from masterdata-service
            String url = masterdataApiBaseUrl + "/masterdata/workstations/" + id;
            Map<String, Object> workstation = restTemplate.getForObject(url, Map.class);
            
            if (workstation != null && workstation.containsKey("name")) {
                return (String) workstation.get("name");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch workstation name for {}: {}", workstationId, e.getMessage());
        }
        
        // Fallback to generic name if fetch fails
        return "Workstation " + workstationId;
    }

    /**
     * Helper method to get item name from masterdata-service.
     * Fetches the actual part/module name based on itemId and itemType.
     *
     * @param itemId The ID of the item
     * @param itemType The type of item ("MODULE", "PART", or "PRODUCT")
     * @return The actual item name or a fallback name
     */
    public String getItemName(Long itemId, String itemType) {
        if (itemId == null || itemType == null) {
            return "Unknown Item";
        }

        try {
            String endpoint = switch (itemType.toUpperCase()) {
                case "MODULE" -> "/masterdata/modules/" + itemId;
                case "PART" -> "/masterdata/parts/" + itemId;
                case "PRODUCT" -> "/masterdata/products/" + itemId;
                default -> null;
            };

            if (endpoint != null) {
                String url = masterdataApiBaseUrl + endpoint;
                Map<String, Object> item = restTemplate.getForObject(url, Map.class);

                if (item != null && item.containsKey("name")) {
                    return (String) item.get("name");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch item name for {} (type: {}): {}", itemId, itemType, e.getMessage());
        }

        // Fallback to generic name if fetch fails
        return itemType + " #" + itemId;
    }

    /**
     * Helper method to calculate estimated completion time.
     */
    private String calculateCompletionTime(int totalMinutes) {
        return isoFormatter.format(LocalDateTime.now().plusMinutes(totalMinutes));
    }

    /**
     * Helper method to update overall order status based on tasks.
     */
    /**
     * Create control orders from a scheduled order.
     * Processes the SimAL schedule and generates ProductionControlOrder and AssemblyControlOrder
     * entities in the order-processing-service.
     *
     * @param scheduleId The SimAL schedule ID
     * @param productionOrderId The production order ID in order-processing-service
     * @return Map of created control order numbers by workstation
     */
    @PostMapping("/scheduled-orders/{scheduleId}/create-control-orders")
    public ResponseEntity<Map<String, Object>> createControlOrders(
            @PathVariable String scheduleId,
            @RequestParam(required = false) Long productionOrderId) {

        log.info("=== CREATE CONTROL ORDERS REQUESTED ===");
        log.info("Schedule ID: {}, Production Order ID: {}", scheduleId, productionOrderId);

        ScheduledOrder orderEntity = scheduledOrderRepository.findByScheduleId(scheduleId)
                .orElse(null);
        if (orderEntity == null) {
            log.error("❌ Schedule not found: {}", scheduleId);
            return ResponseEntity.notFound().build();
        }

        log.info("Found schedule entity: {}", orderEntity.getScheduleId());

        // Convert entity to DTO for control order integration
        SimalScheduledOrderResponse schedule = convertToResponse(orderEntity);

        if (productionOrderId == null) {
            log.warn("Production order ID not provided, using default test ID: 1");
            productionOrderId = 1L; // Default for testing
        }

        try {
            log.info("Calling ControlOrderIntegrationService to create control orders...");
            Map<String, String> createdOrders = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, productionOrderId);

            log.info("✓ Successfully created {} control orders", createdOrders.size());
            createdOrders.forEach((wsId, controlOrderNum) -> 
                log.info("  - Workstation {}: {}", wsId, controlOrderNum)
            );

            Map<String, Object> response = new HashMap<>();
            response.put(SCHEDULE_ID, scheduleId);
            response.put("productionOrderId", productionOrderId);
            response.put("controlOrdersCreated", createdOrders);
            response.put("totalControlOrders", createdOrders.size());
            response.put(STATUS, "SUCCESS");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error creating control orders: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(STATUS, "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate control orders from SimAL schedule - simplified endpoint
     * This endpoint accepts a POST body with scheduleId and productionOrderId
     */
    @PostMapping("/generate-control-orders")
    public ResponseEntity<Map<String, Object>> generateControlOrders(
            @RequestBody GenerateControlOrdersRequest request) {

        log.info("Generating control orders for schedule: {}, production order: {}", 
                request.getScheduleId(), request.getProductionOrderId());

        ScheduledOrder orderEntity = scheduledOrderRepository.findByScheduleId(request.getScheduleId())
                .orElse(null);
        if (orderEntity == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(STATUS, "ERROR");
            errorResponse.put("message", "Schedule not found: " + request.getScheduleId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Convert entity to DTO for control order integration
        SimalScheduledOrderResponse schedule = convertToResponse(orderEntity);

        try {
            Map<String, String> createdOrders = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, request.getProductionOrderId());

            // Separate production and assembly control orders
            List<String> productionControlOrders = new ArrayList<>();
            List<String> assemblyControlOrders = new ArrayList<>();

            for (Map.Entry<String, String> entry : createdOrders.entrySet()) {
                if (entry.getValue().startsWith("PCO-")) {
                    productionControlOrders.add(entry.getValue());
                } else if (entry.getValue().startsWith("ACO-")) {
                    assemblyControlOrders.add(entry.getValue());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("scheduleId", request.getScheduleId());
            response.put("productionOrderId", request.getProductionOrderId());
            response.put("productionControlOrders", productionControlOrders);
            response.put("assemblyControlOrders", assemblyControlOrders);
            response.put("totalControlOrders", createdOrders.size());
            response.put(STATUS, "SUCCESS");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating control orders: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(STATUS, "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Dispatch a scheduled order to create control orders and update status.
     * This is the endpoint called after SimAL scheduling to actually create the control orders.
     *
     * @param scheduleId The schedule ID to dispatch
     * @return Map with dispatch status and created control orders
     */
    @PostMapping("/scheduled-orders/{scheduleId}/dispatch")
    @Operation(summary = "Dispatch scheduled order", 
               description = "Dispatch a scheduled order to create control orders for workstations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schedule dispatched successfully"),
        @ApiResponse(responseCode = "404", description = "Schedule not found"),
        @ApiResponse(responseCode = "500", description = "Error during dispatch")
    })
    public ResponseEntity<Map<String, Object>> dispatchScheduledOrder(
            @PathVariable String scheduleId) {
        
        log.info("Dispatching scheduled order: {}", scheduleId);
        
        // Find schedule in database
        ScheduledOrder orderEntity = scheduledOrderRepository.findByScheduleId(scheduleId)
                .orElse(null);
        
        if (orderEntity == null) {
            log.error("Schedule not found: {}", scheduleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(STATUS, "ERROR");
            errorResponse.put("message", "Schedule not found: " + scheduleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        // Convert entity to DTO
        SimalScheduledOrderResponse schedule = convertToResponse(orderEntity);
        
        // Extract production order ID from order number or metadata
        // For now, we'll try to extract from order number (e.g., "PO-123" -> 123)
        Long productionOrderId = extractProductionOrderId(orderEntity.getOrderNumber());
        
        if (productionOrderId == null) {
            log.warn("Could not extract production order ID from: {}", orderEntity.getOrderNumber());
            // Try to get the latest production order ID from the request or default to 1
            productionOrderId = 1L;
        }
        
        try {
            // Create control orders from schedule
            Map<String, String> createdOrders = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, productionOrderId);
            
            // Update schedule status to DISPATCHED
            orderEntity.setStatus("DISPATCHED");
            scheduledOrderRepository.save(orderEntity);
            
            log.info("✓ Successfully dispatched schedule {} - created {} control orders", 
                    scheduleId, createdOrders.size());
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put(SCHEDULE_ID, scheduleId);
            response.put("productionOrderId", productionOrderId);
            response.put("controlOrders", createdOrders);
            response.put("totalControlOrders", createdOrders.size());
            response.put(STATUS, "DISPATCHED");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error dispatching schedule {}: {}", scheduleId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(STATUS, "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Extract production order ID from order number string.
     * Handles formats like: "PO-123", "PROD-123", "123"
     */
    private Long extractProductionOrderId(String orderNumber) {
        if (orderNumber == null) {
            return null;
        }
        try {
            // Remove common prefixes and try to parse
            String cleaned = orderNumber.replaceAll("[^0-9]", "");
            if (!cleaned.isEmpty()) {
                return Long.parseLong(cleaned);
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse production order ID from: {}", orderNumber);
        }
        return null;
    }

    /**
     * Batch create control orders for multiple schedules.
     * Useful for processing multiple production orders in one operation.
     *
     * @param request Batch request with list of schedule IDs and production order IDs
     * @return Results of all control order creations
     */
    @PostMapping("/create-control-orders/batch")
    public ResponseEntity<Map<String, Object>> createControlOrdersBatch(
            @RequestBody BatchControlOrderRequest request) {

        log.info("Creating control orders for batch: {} schedules", request.getScheduleIds().size());

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < request.getScheduleIds().size(); i++) {
            String scheduleId = request.getScheduleIds().get(i);
            Long productionOrderId = i < request.getProductionOrderIds().size() ?
                    request.getProductionOrderIds().get(i) : (long)(i + 1);

            ScheduledOrder orderEntity = scheduledOrderRepository.findByScheduleId(scheduleId)
                    .orElse(null);
            if (orderEntity == null) {
                failureCount++;
                results.add(Map.of(SCHEDULE_ID, scheduleId, STATUS, "NOT_FOUND"));
                continue;
            }

            // Convert entity to DTO for control order integration
            SimalScheduledOrderResponse schedule = convertToResponse(orderEntity);

            try {
                Map<String, String> createdOrders = controlOrderIntegrationService
                        .createControlOrdersFromSchedule(schedule, productionOrderId);

                successCount++;
                results.add(Map.of(
                        SCHEDULE_ID, scheduleId,
                        "productionOrderId", productionOrderId,
                        "controlOrders", createdOrders,
                        STATUS, "SUCCESS"
                ));
            } catch (Exception e) {
                failureCount++;
                results.add(Map.of(
                        SCHEDULE_ID, scheduleId,
                        STATUS, "ERROR",
                        "message", e.getMessage()
                ));
            }
        }

        Map<String, Object> batchResponse = new HashMap<>();
        batchResponse.put("totalRequests", request.getScheduleIds().size());
        batchResponse.put("successCount", successCount);
        batchResponse.put("failureCount", failureCount);
        batchResponse.put("results", results);

        return ResponseEntity.ok(batchResponse);
    }

    /**
     * DTO for batch control order creation request.
     */
    public static class BatchControlOrderRequest {
        public List<String> scheduleIds;
        public List<Long> productionOrderIds;

        public List<String> getScheduleIds() {
            return scheduleIds;
        }

        public void setScheduleIds(List<String> scheduleIds) {
            this.scheduleIds = scheduleIds;
        }

        public List<Long> getProductionOrderIds() {
            return productionOrderIds;
        }

        public void setProductionOrderIds(List<Long> productionOrderIds) {
            this.productionOrderIds = productionOrderIds;
        }
    }

    /**
     * DTO for generate control orders request.
     */
    public static class GenerateControlOrdersRequest {
        private String scheduleId;
        private Long productionOrderId;
        private List<Object> scheduledTasks; // Optional, for future use

        public String getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(String scheduleId) {
            this.scheduleId = scheduleId;
        }

        public Long getProductionOrderId() {
            return productionOrderId;
        }

        public void setProductionOrderId(Long productionOrderId) {
            this.productionOrderId = productionOrderId;
        }

        public List<Object> getScheduledTasks() {
            return scheduledTasks;
        }

        public void setScheduledTasks(List<Object> scheduledTasks) {
            this.scheduledTasks = scheduledTasks;
        }
    }

    // ========================================================================
    // NEW: Manual Scheduling Endpoint
    // ========================================================================

    /**
     * Manual reschedule endpoint - allows Production Planning to adjust task schedules.
     * Persists changes to database with audit trail.
     *
     * @param taskId Task ID to reschedule
     * @param request Reschedule request with new time/workstation
     * @param userId User making the change (from JWT header)
     * @return Updated task response
     */
    @PutMapping("/tasks/{taskId}/reschedule")
    public ResponseEntity<ScheduledTaskResponse> rescheduleTask(
            @PathVariable String taskId,
            @RequestBody RescheduleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        log.info("Manual reschedule request for task: {} by user: {}", taskId, userId);
        
        // Find task in database
        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Task not found: " + taskId
                ));
        
        // Update task with new schedule
        task.setWorkstationId(request.getWorkstationId());
        task.setWorkstationName(getWorkstationName(request.getWorkstationId()));
        task.setStartTime(request.getScheduledStartTime());
        task.setDuration(request.getDuration());
        task.setEndTime(request.getScheduledStartTime().plusMinutes(request.getDuration()));
        
        // Mark as manually adjusted
        task.setManuallyAdjusted(true);
        task.setAdjustedBy(userId != null ? userId : "system");
        task.setAdjustedAt(LocalDateTime.now());
        task.setAdjustmentReason(request.getReason() != null ? request.getReason() : "Manual reschedule");
        
        ScheduledTask savedTask = scheduledTaskRepository.save(task);
        log.info("Task {} rescheduled successfully", taskId);
        
        // Update parent order status if needed
        ScheduledOrder order = task.getScheduledOrder();
        updateOrderStatusFromTasks(order);
        scheduledOrderRepository.save(order);
        
        // Convert to response DTO
        ScheduledTaskResponse response = convertToTaskResponse(savedTask);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all manually adjusted tasks.
     *
     * @return List of manually adjusted tasks
     */
    @GetMapping("/tasks/manually-adjusted")
    public ResponseEntity<List<ScheduledTaskResponse>> getManuallyAdjustedTasks() {
        List<ScheduledTask> tasks = scheduledTaskRepository.findByManuallyAdjusted(true);
        List<ScheduledTaskResponse> responses = tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Convert ScheduledOrder entity to DTO response.
     */
    private SimalScheduledOrderResponse convertToResponse(ScheduledOrder order) {
        List<SimalScheduledOrderResponse.ScheduledTask> taskDtos = order.getScheduledTasks().stream()
                .map(task -> SimalScheduledOrderResponse.ScheduledTask.builder()
                        .taskId(task.getTaskId())
                        .itemId(task.getItemId())
                        .itemName(task.getItemName())
                        .quantity(task.getQuantity())
                        .workstationId(task.getWorkstationId())
                        .workstationName(task.getWorkstationName())
                        .startTime(isoFormatter.format(task.getStartTime()))
                        .endTime(isoFormatter.format(task.getEndTime()))
                        .duration(task.getDuration())
                        .status(task.getStatus())
                        .sequence(task.getSequence())
                        .build())
                .collect(Collectors.toList());

        return SimalScheduledOrderResponse.builder()
                .scheduleId(order.getScheduleId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .estimatedCompletionTime(isoFormatter.format(order.getEstimatedCompletionTime()))
                .scheduledTasks(taskDtos)
                .totalDuration(order.getTotalDuration())
                .build();
    }

    /**
     * Convert ScheduledTask entity to DTO response.
     */
    private ScheduledTaskResponse convertToTaskResponse(ScheduledTask task) {
        return ScheduledTaskResponse.builder()
                .taskId(task.getTaskId())
                .itemId(task.getItemId())
                .itemName(task.getItemName())
                .quantity(task.getQuantity())
                .workstationId(task.getWorkstationId())
                .workstationName(task.getWorkstationName())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .duration(task.getDuration())
                .status(task.getStatus())
                .sequence(task.getSequence())
                .manuallyAdjusted(task.getManuallyAdjusted())
                .adjustedBy(task.getAdjustedBy())
                .adjustedAt(task.getAdjustedAt())
                .adjustmentReason(task.getAdjustmentReason())
                .build();
    }

    /**
     * Update order status based on all tasks status.
     */
    private void updateOrderStatusFromTasks(ScheduledOrder order) {
        List<ScheduledTask> tasks = order.getScheduledTasks();
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        long completedTasks = tasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .count();

        long failedTasks = tasks.stream()
                .filter(t -> "FAILED".equals(t.getStatus()))
                .count();

        long totalTasks = tasks.size();

        if (failedTasks > 0) {
            order.setStatus("FAILED");
        } else if (completedTasks == totalTasks) {
            order.setStatus("COMPLETED");
        } else if (completedTasks > 0) {
            order.setStatus("IN_PROGRESS");
        } else {
            order.setStatus("SCHEDULED");
        }
    }

    /**
     * Schedule a production order by fetching it from order-processing-service.
     * This endpoint retrieves the production order and associated warehouse order items,
     * then creates a schedule in SimAL with those items.
     *
     * @param productionOrderId The production order ID to schedule
     * @return Scheduled order response with task assignments
     */
    @PostMapping("/schedule-production-order/{productionOrderId}")
    public ResponseEntity<SimalScheduledOrderResponse> scheduleProductionOrder(
            @PathVariable Long productionOrderId) {
        
        log.info("Scheduling production order ID: {}", productionOrderId);
        
        try {
            // Delegate to ControlOrderIntegrationService to fetch and schedule
            SimalScheduledOrderResponse response = controlOrderIntegrationService
                    .scheduleProductionOrderById(productionOrderId, this);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error scheduling production order {}: {}", productionOrderId, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to schedule production order: " + e.getMessage(),
                    e
            );
        }
    }
}

