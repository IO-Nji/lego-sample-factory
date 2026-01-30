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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for ProductionOrder management.
 * Exposes endpoints for creating, retrieving, and updating production orders.
 * Production orders are created when WarehouseOrders cannot be fully fulfilled (Scenario 3)
 * or when high-volume orders bypass warehouse entirely (Scenario 4).
 * 
 * Request DTOs are located in io.life.order.dto.request package.
 */
@RestController
@RequestMapping("/api/production-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    public ProductionOrderController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    /**
     * Get all production orders
     */
    @GetMapping
    public ResponseEntity<List<ProductionOrderDTO>> getAllProductionOrders() {
        List<ProductionOrderDTO> orders = productionOrderService.getAllProductionOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get production order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductionOrderDTO> getProductionOrderById(@PathVariable Long id) {
        Optional<ProductionOrderDTO> order = productionOrderService.getProductionOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get production order by production order number
     */
    @GetMapping("/number/{productionOrderNumber}")
    public ResponseEntity<ProductionOrderDTO> getProductionOrderByNumber(
            @PathVariable String productionOrderNumber) {
        Optional<ProductionOrderDTO> order = productionOrderService.getProductionOrderByNumber(productionOrderNumber);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get all production orders from a customer order
     */
    @GetMapping("/customer/{sourceCustomerOrderId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByCustomerOrder(
            @PathVariable Long sourceCustomerOrderId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByCustomerOrder(sourceCustomerOrderId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get all production orders from a warehouse order
     */
    @GetMapping("/warehouse/{sourceWarehouseOrderId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByWarehouseOrder(
            @PathVariable Long sourceWarehouseOrderId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByWarehouseOrder(sourceWarehouseOrderId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get production orders by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByStatus(
            @PathVariable String status) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get production orders by priority
     */
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByPriority(
            @PathVariable String priority) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByPriority(priority);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get production orders by workstation (created by)
     */
    @GetMapping("/workstation/{createdByWorkstationId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByWorkstation(
            @PathVariable Long createdByWorkstationId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByWorkstation(createdByWorkstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get production orders assigned to a workstation (for assembly/completion)
     */
    @GetMapping("/assigned/{assignedWorkstationId}")
    public ResponseEntity<List<ProductionOrderDTO>> getProductionOrdersByAssignedWorkstation(
            @PathVariable Long assignedWorkstationId) {
        List<ProductionOrderDTO> orders = productionOrderService.getProductionOrdersByAssignedWorkstation(assignedWorkstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Create a standalone production order
     */
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

    /**
     * Create a production order from warehouse order (Scenario 3)
     */
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

    /**
     * Create a production order directly from customer order (Scenario 4).
     * Used when large orders bypass warehouse and go directly to production.
     * 
     * Request body: {
     *   "customerOrderId": 123,
     *   "priority": "HIGH",
     *   "notes": "Large order - direct production"
     * }
     */
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

    /**
     * Update production order status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductionOrderDTO> updateProductionOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        ProductionOrderDTO order = productionOrderService.updateProductionOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(order);
    }

    /**
     * Link production order to SimAL schedule
     */
    @PatchMapping("/{id}/link-simal")
    public ResponseEntity<ProductionOrderDTO> linkToSimalSchedule(
            @PathVariable Long id,
            @RequestBody LinkSimalRequest request) {
        ProductionOrderDTO order = productionOrderService.linkToSimalSchedule(
                id,
                request.getSimalScheduleId(),
                request.getEstimatedDuration(),
                request.getExpectedCompletionTime()
        );
        return ResponseEntity.ok(order);
    }

    /**
     * Schedule production order with SimAL (update schedule ID and status)
     */
    @PatchMapping("/{id}/schedule")
    public ResponseEntity<ProductionOrderDTO> scheduleWithSimal(
            @PathVariable Long id,
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

    /**
     * Complete production order
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ProductionOrderDTO> completeProductionOrder(@PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.completeProductionOrder(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Confirm production order (CREATED -> CONFIRMED) - Production Planning confirms receipt
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ProductionOrderDTO> confirmProductionOrder(@PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.confirmProductionOrder(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Schedule production order (CONFIRMED -> SCHEDULED) - SimAL integration
     */
    @PostMapping("/{id}/schedule")
    public ResponseEntity<ProductionOrderDTO> scheduleProduction(
            @PathVariable Long id,
            @RequestBody ScheduleProductionRequest request) {
        ProductionOrderDTO order = productionOrderService.scheduleProduction(
                id,
                request.getScheduledStartTime(),
                request.getScheduledEndTime(),
                request.getGanttChartId()
        );
        return ResponseEntity.ok(order);
    }

    /**
     * Dispatch production order to control stations (SCHEDULED -> DISPATCHED)
     * Creates control orders for manufacturing and assembly
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<ProductionOrderDTO> dispatchToControlStations(@PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.dispatchToControlStations(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Update from control order completion (upward notification)
     * Called when a control order completes
     */
    @PostMapping("/{id}/control-completion")
    public ResponseEntity<ProductionOrderDTO> updateFromControlOrderCompletion(
            @PathVariable Long id,
            @RequestBody ControlCompletionRequest request) {
        ProductionOrderDTO order = productionOrderService.updateFromControlOrderCompletion(
                id,
                request.getControlOrderId()
        );
        return ResponseEntity.ok(order);
    }

    /**
     * Complete production order with warehouse notification (final step)
     */
    @PostMapping("/{id}/complete-with-notification")
    public ResponseEntity<ProductionOrderDTO> completeProductionOrderWithNotification(@PathVariable Long id) {
        ProductionOrderDTO order = productionOrderService.completeProductionOrderWithNotification(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Cancel production order
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ProductionOrderDTO> cancelProductionOrder(@PathVariable Long id) {
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
