package io.life.order.controller;

import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.request.AssemblyControlOrderCreateRequest;
import io.life.order.dto.request.RequestPartsRequest;
import io.life.order.service.AssemblyControlOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for AssemblyControlOrder management.
 * Exposes endpoints for Assembly Control workstations to view and manage their assigned orders.
 */
@RestController
@RequestMapping("/api/assembly-control-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Assembly Control Orders", description = "Assembly Control - Manage assembly orders for WS-4, WS-5, WS-6")
public class AssemblyControlOrderController {

    private final AssemblyControlOrderService assemblyControlOrderService;

    public AssemblyControlOrderController(AssemblyControlOrderService assemblyControlOrderService) {
        this.assemblyControlOrderService = assemblyControlOrderService;
    }

    @Operation(summary = "Get all assembly control orders", 
               description = "Retrieve all assembly control orders in the system")
    @ApiResponse(responseCode = "200", description = "List of assembly control orders")
    @GetMapping
    public ResponseEntity<List<AssemblyControlOrderDTO>> getAllOrders() {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", 
               description = "Retrieve assembly control orders assigned to a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders for workstation")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<AssemblyControlOrderDTO>> getOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get active orders by workstation", 
               description = "Retrieve in-progress assembly control orders for a workstation")
    @ApiResponse(responseCode = "200", description = "List of active orders")
    @GetMapping("/workstation/{workstationId}/active")
    public ResponseEntity<List<AssemblyControlOrderDTO>> getActiveOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getActiveOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get unassigned orders", 
               description = "Retrieve orders with ASSIGNED status waiting to be started at a workstation")
    @ApiResponse(responseCode = "200", description = "List of unassigned orders")
    @GetMapping("/workstation/{workstationId}/unassigned")
    public ResponseEntity<List<AssemblyControlOrderDTO>> getUnassignedOrders(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getUnassignedOrders(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", 
               description = "Retrieve an assembly control order by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AssemblyControlOrderDTO> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        Optional<AssemblyControlOrderDTO> order = assemblyControlOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get order by number", 
               description = "Retrieve an assembly control order by its order number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/number/{controlOrderNumber}")
    public ResponseEntity<AssemblyControlOrderDTO> getOrderByNumber(
            @Parameter(description = "Control order number (e.g., ACO-00001)") @PathVariable String controlOrderNumber) {
        Optional<AssemblyControlOrderDTO> order = assemblyControlOrderService.getOrderByNumber(controlOrderNumber);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Confirm receipt", 
               description = "Confirm receipt of an assembly control order (PENDING -> CONFIRMED)")
    @ApiResponse(responseCode = "200", description = "Order confirmed")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<AssemblyControlOrderDTO> confirmReceipt(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.confirmReceipt(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Start assembly", 
               description = "Start assembly on a control order (CONFIRMED -> IN_PROGRESS)")
    @ApiResponse(responseCode = "200", description = "Assembly started")
    @PostMapping("/{id}/start")
    public ResponseEntity<AssemblyControlOrderDTO> startAssembly(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.startAssembly(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Update operator notes", 
               description = "Update the operator notes for a control order")
    @ApiResponse(responseCode = "200", description = "Notes updated")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AssemblyControlOrderDTO> updateOperatorNotes(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Operator notes") @RequestParam String notes) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.updateOperatorNotes(id, notes);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Complete assembly", 
               description = "Complete assembly on a control order and propagate status upward")
    @ApiResponse(responseCode = "200", description = "Assembly completed")
    @PostMapping("/{id}/complete")
    public ResponseEntity<AssemblyControlOrderDTO> completeAssembly(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.completeAssembly(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Update defects", 
               description = "Update defects found during assembly")
    @ApiResponse(responseCode = "200", description = "Defects updated")
    @PatchMapping("/{id}/defects")
    public ResponseEntity<AssemblyControlOrderDTO> updateDefects(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Number of defects found") @RequestParam Integer defectsFound,
            @Parameter(description = "Number of defects reworked") @RequestParam(required = false) Integer defectsReworked,
            @Parameter(description = "Whether rework is required") @RequestParam(required = false) Boolean reworkRequired) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.updateDefects(id, defectsFound, defectsReworked, reworkRequired);
        return ResponseEntity.ok(order);
    }

    /**
     * Update shipping notes for completed order
     */
    @PutMapping("/{id}/shipping-notes")
    public ResponseEntity<AssemblyControlOrderDTO> updateShippingNotes(
            @PathVariable Long id,
            @RequestParam String shippingNotes) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.updateShippingNotes(id, shippingNotes);
        return ResponseEntity.ok(order);
    }

    /**
     * Request parts for an assembly control order.
     * Creates a supply order to Parts Supply Warehouse.
     */
    @PostMapping("/{id}/request-parts")
    public ResponseEntity<SupplyOrderDTO> requestParts(
            @PathVariable Long id,
            @RequestBody RequestPartsRequest request) {
        SupplyOrderDTO supplyOrder = assemblyControlOrderService.requestSupplies(
                id,
                request.getRequiredParts(),
                request.getNeededBy(),
                request.getNotes()
        );
        return ResponseEntity.ok(supplyOrder);
    }

    /**
     * Dispatch assembly control order to workstation.
     * Requires supply order to be fulfilled first (if one exists).
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<AssemblyControlOrderDTO> dispatchToWorkstation(@PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.dispatchToWorkstation(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Get supply orders for an assembly control order.
     */
    @GetMapping("/{id}/supply-orders")
    public ResponseEntity<List<SupplyOrderDTO>> getSupplyOrders(@PathVariable Long id) {
        List<SupplyOrderDTO> supplyOrders = assemblyControlOrderService.getSupplyOrders(id);
        return ResponseEntity.ok(supplyOrders);
    }

    /**
     * Create an assembly control order from SimAL schedule data.
     * This endpoint is called by the SimAL integration service when a production
     * order has been scheduled and assigned to an assembly workstation.
     */
    @PostMapping
    public ResponseEntity<AssemblyControlOrderDTO> createControlOrder(
            @RequestBody AssemblyControlOrderCreateRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime targetStart = LocalDateTime.parse(request.getTargetStartTime(), formatter);
        LocalDateTime targetCompletion = LocalDateTime.parse(request.getTargetCompletionTime(), formatter);

        AssemblyControlOrderDTO order = assemblyControlOrderService.createControlOrder(
                request.getSourceProductionOrderId(),
                request.getAssignedWorkstationId(),
                request.getSimalScheduleId(),
                request.getPriority(),
                targetStart,
                targetCompletion,
                request.getAssemblyInstructions(),
                request.getQualityCheckpoints(),
                "Standard testing procedures apply",
                "Standard packaging requirements",
                90,  // Default 90-minute estimate
                request.getItemId(),
                request.getItemType(),
                request.getQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
