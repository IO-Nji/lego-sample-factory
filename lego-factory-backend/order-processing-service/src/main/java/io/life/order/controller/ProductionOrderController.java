package io.life.order.controller;

import io.life.order.dto.ProductionOrderDTO;
import io.life.order.dto.request.ControlCompletionRequest;
import io.life.order.dto.request.CreateFromCustomerOrderRequest;
import io.life.order.dto.request.CreateProductionOrderFromWarehouseRequest;
import io.life.order.dto.request.CreateProductionOrderRequest;
import io.life.order.dto.request.LinkSimalRequest;
import io.life.order.dto.request.ScheduleProductionRequest;
import io.life.order.dto.request.ScheduleRequest;
import io.life.order.dto.request.UpdateStatusRequest;
import io.life.order.service.ProductionOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for ProductionOrder management.
 * Production orders are created when WarehouseOrders cannot be fully fulfilled (Scenario 3)
 * or when high-volume orders bypass warehouse entirely (Scenario 4).
 */
@RestController
@RequestMapping("/api/production-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Production Orders", description = "Production Planning - Create and manage production orders for manufacturing")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    public ProductionOrderController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    @Operation(summary = "Get all production orders", 
               description = "Retrieve all production orders in the system")
    @ApiResponse(responseCode = "200", description = "List of production orders")
    @GetMapping
    public ResponseEntity<List<ProductionOrderDTO>> getAllProductionOrders() {
        List<ProductionOrderDTO> orders = productionOrderService.getAllProductionOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get production order by ID", 
               description = "Retrieve a specific production order by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Production order found"),
        @ApiResponse(responseCode = "404", description = "Production order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductionOrderDTO> getProductionOrderById(
            @Parameter(description = "Production order ID") @PathVariable Long id) {
        Optional<ProductionOrderDTO> order = productionOrderService.getProductionOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get production order by number", 
               description = "Retrieve a production order by its order number (e.g., PO-00001)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Production order found"),
        @ApiResponse(responseCode = "404", description = "Production order not found")
    })
    @GetMapping("/number/{productionOrderNumber}")
    public ResponseEntity<ProductionOrderDTO> getProductionOrderByNumber(
            @Parameter(description = "Production order number (e.g., PO-00001)") @PathVariable String productionOrderNumber) {
        Optional<ProductionOrderDTO> order = productionOrderService.getProductionOrderByNumber(productionOrderNumber);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get production orders by customer order", 
               description = "Retrieve all production orders created from a specific customer order (Scenario 4)")
    @ApiResponse(responseCode = "200", description = "List of production orders")
    @GetMapping("/customer/{sourceCustomerOrderId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByCustomerOrder(
            @Parameter(description = "Source customer order ID") @PathVariable Long sourceCustomerOrderId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByCustomerOrder(sourceCustomerOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get production orders by warehouse order", 
               description = "Retrieve all production orders created from a specific warehouse order (Scenario 3)")
    @ApiResponse(responseCode = "200", description = "List of production orders")
    @GetMapping("/warehouse/{sourceWarehouseOrderId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByWarehouseOrder(
            @Parameter(description = "Source warehouse order ID") @PathVariable Long sourceWarehouseOrderId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByWarehouseOrder(sourceWarehouseOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get production orders by status", 
               description = "Retrieve production orders filtered by status")
    @ApiResponse(responseCode = "200", description = "List of production orders")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByStatus(
            @Parameter(description = "Order status (CREATED, CONFIRMED, SCHEDULED, DISPATCHED, IN_PROGRESS, COMPLETED, CANCELLED)") @PathVariable String status) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get production orders by priority", 
               description = "Retrieve production orders filtered by priority level")
    @ApiResponse(responseCode = "200", description = "List of production orders")
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByPriority(
            @Parameter(description = "Priority level (LOW, NORMAL, HIGH, URGENT)") @PathVariable String priority) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByPriority(priority);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get production orders by creating workstation", 
               description = "Retrieve production orders created by a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of production orders")
    @GetMapping("/workstation/{createdByWorkstationId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByWorkstation(
            @Parameter(description = "Creating workstation ID") @PathVariable Long createdByWorkstationId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByWorkstation(createdByWorkstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get production orders by assigned workstation", 
               description = "Retrieve production orders assigned to a specific workstation for execution")
    @ApiResponse(responseCode = "200", description = "List of production orders")
    @GetMapping("/assigned/{assignedWorkstationId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByAssignedWorkstation(
            @Parameter(description = "Assigned workstation ID") @PathVariable Long assignedWorkstationId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByAssignedWorkstation(assignedWorkstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Create standalone production order", 
               description = "Create a new production order not linked to warehouse order")
    @ApiResponse(responseCode = "201", description = "Production order created")
    @PostMapping
    public ResponseEntity<ProductionOrderDTO> createStandaloneProductionOrder(
            @RequestBody CreateProductionOrderRequest request) {
        ProductionOrderDTO order = productionOrderService.createStandaloneProductionOrder(
                request.getSourceCustomerOrderId(),
                request.getPriority(),
                request.getDueDate(),
                request.getNotes(),
                request.getCreatedByWorkstationId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Create from warehouse order (Scenario 3)", 
               description = "Create a production order from a warehouse order when modules are unavailable")
    @ApiResponse(responseCode = "201", description = "Production order created")
    @PostMapping("/create")
    public ResponseEntity<ProductionOrderDTO> createProductionOrderFromWarehouse(
            @RequestBody CreateProductionOrderFromWarehouseRequest request) {
        ProductionOrderDTO order = productionOrderService.createProductionOrderFromWarehouse(
                request.getSourceCustomerOrderId(),
                request.getSourceWarehouseOrderId(),
                request.getPriority(),
                request.getDueDate(),
                request.getNotes(),
                request.getCreatedByWorkstationId(),
                request.getAssignedWorkstationId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Create from customer order (Scenario 4)", 
               description = "Create a production order directly from a customer order for high-volume orders that bypass warehouse")
    @ApiResponse(responseCode = "201", description = "Production order created")
    @PostMapping("/from-customer-order")
    public ResponseEntity<ProductionOrderDTO> createFromCustomerOrder(
            @RequestBody CreateFromCustomerOrderRequest request) {
        ProductionOrderDTO order = productionOrderService.createFromCustomerOrder(
                request.getCustomerOrderId(),
                request.getPriority(),
                request.getDueDate(),
                request.getNotes(),
                request.getCreatedByWorkstationId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Update status", 
               description = "Update the status of a production order")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductionOrderDTO> updateProductionOrderStatus(
            @Parameter(description = "Production order ID") @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        ProductionOrderDTO order = productionOrderService.updateProductionOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Link to SimAL schedule", 
               description = "Link a production order to a SimAL scheduling system schedule")
    @ApiResponse(responseCode = "200", description = "Linked to SimAL schedule")
    @PatchMapping("/{id}/link-simal")
    public ResponseEntity<ProductionOrderDTO> linkToSimalSchedule(
            @Parameter(description = "Production order ID") @PathVariable Long id,
            @RequestBody LinkSimalRequest request) {
        ProductionOrderDTO order = productionOrderService.linkToSimalSchedule(
                id,
                request.getSimalScheduleId(),
                request.getEstimatedDuration(),
                request.getExpectedCompletionTime()
        );
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Schedule with SimAL (PATCH)", 
               description = "Update schedule ID and set status to SCHEDULED")
    @ApiResponse(responseCode = "200", description = "Scheduled")
    @PatchMapping("/{id}/schedule")
    public ResponseEntity<ProductionOrderDTO> scheduleWithSimal(
            @Parameter(description = "Production order ID") @PathVariable Long id,
            @RequestBody ScheduleRequest request) {
        ProductionOrderDTO order = productionOrderService.linkToSimalSchedule(
                id,
                request.getSimalScheduleId(),
                null,
                null
        );
        // Also update status to SCHEDULED
        order = productionOrderService.updateProductionOrderStatus(id, "SCHEDULED");
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Complete production order (PATCH)", 
               description = "Mark a production order as completed")
    @ApiResponse(responseCode = "200", description = "Production order completed")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ProductionOrderDTO> completeProductionOrder(
            @Parameter(description = "Production order ID") @PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.completeProductionOrder(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Confirm production order", 
               description = "Confirm a production order (CREATED -> CONFIRMED) - Production Planning confirms receipt")
    @ApiResponse(responseCode = "200", description = "Production order confirmed")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ProductionOrderDTO> confirmProductionOrder(
            @Parameter(description = "Production order ID") @PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.confirmProductionOrder(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Schedule production", 
               description = "Schedule a production order (CONFIRMED -> SCHEDULED) with SimAL integration")
    @ApiResponse(responseCode = "200", description = "Production order scheduled")
    @PostMapping("/{id}/schedule")
    public ResponseEntity<ProductionOrderDTO> scheduleProduction(
            @Parameter(description = "Production order ID") @PathVariable Long id,
            @RequestBody ScheduleProductionRequest request) {
        ProductionOrderDTO order = productionOrderService.scheduleProduction(
                id,
                request.getScheduledStartTime(),
                request.getScheduledEndTime(),
                request.getGanttChartId()
        );
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Dispatch to control stations", 
               description = "Dispatch a production order to control stations (SCHEDULED -> DISPATCHED). Creates control orders for manufacturing and assembly.")
    @ApiResponse(responseCode = "200", description = "Dispatched to control stations")
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<ProductionOrderDTO> dispatchToControlStations(
            @Parameter(description = "Production order ID") @PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.dispatchToControlStations(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Notify control order completion", 
               description = "Update from control order completion (upward notification from control order)")
    @ApiResponse(responseCode = "200", description = "Updated from control completion")
    @PostMapping("/{id}/control-completion")
    public ResponseEntity<ProductionOrderDTO> updateFromControlOrderCompletion(
            @Parameter(description = "Production order ID") @PathVariable Long id,
            @RequestBody ControlCompletionRequest request) {
        ProductionOrderDTO order = productionOrderService.updateFromControlOrderCompletion(
                id,
                request.getControlOrderId()
        );
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Complete with warehouse notification", 
               description = "Complete production order and notify warehouse order (final step)")
    @ApiResponse(responseCode = "200", description = "Completed with notification")
    @PostMapping("/{id}/complete-with-notification")
    public ResponseEntity<ProductionOrderDTO> completeProductionOrderWithNotification(
            @Parameter(description = "Production order ID") @PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.completeProductionOrderWithNotification(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancel production order", 
               description = "Cancel a production order")
    @ApiResponse(responseCode = "200", description = "Production order cancelled")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ProductionOrderDTO> cancelProductionOrder(
            @Parameter(description = "Production order ID") @PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.cancelProductionOrder(id);
        return ResponseEntity.ok(order);
    }

    // All request DTOs have been extracted to io.life.order.dto.request package:
    // - CreateProductionOrderRequest
    // - CreateProductionOrderFromWarehouseRequest
    // - CreateFromCustomerOrderRequest
    // - UpdateStatusRequest
    // - LinkSimalRequest
    // - ScheduleRequest
    // - ScheduleProductionRequest
    // - ControlCompletionRequest
}
