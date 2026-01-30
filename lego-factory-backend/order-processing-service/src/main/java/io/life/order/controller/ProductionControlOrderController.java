package io.life.order.controller;

import io.life.order.dto.ProductionControlOrderDTO;
import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.request.DefectsRequest;
import io.life.order.dto.request.HaltRequest;
import io.life.order.dto.request.NotesRequest;
import io.life.order.dto.request.ProductionControlOrderCreateRequest;
import io.life.order.dto.request.RequestPartsRequest;
import io.life.order.service.ProductionControlOrderService;
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
 * REST Controller for ProductionControlOrder management.
 * Exposes endpoints for Production Control workstations to view and manage their assigned orders.
 */
@RestController
@RequestMapping("/api/production-control-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Production Control Orders", description = "Production Control - Manage manufacturing orders for WS-1, WS-2, WS-3")
public class ProductionControlOrderController {

    private final ProductionControlOrderService productionControlOrderService;

    public ProductionControlOrderController(ProductionControlOrderService productionControlOrderService) {
        this.productionControlOrderService = productionControlOrderService;
    }

    @Operation(summary = "Get all production control orders", 
               description = "Retrieve all production control orders in the system")
    @ApiResponse(responseCode = "200", description = "List of production control orders")
    @GetMapping
    public ResponseEntity<List<ProductionControlOrderDTO>> getAllOrders() {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", 
               description = "Retrieve production control orders assigned to a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders for workstation")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<ProductionControlOrderDTO>> getOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get active orders by workstation", 
               description = "Retrieve in-progress production control orders for a workstation")
    @ApiResponse(responseCode = "200", description = "List of active orders")
    @GetMapping("/workstation/{workstationId}/active")
    public ResponseEntity<List<ProductionControlOrderDTO>> getActiveOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getActiveOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get unassigned orders", 
               description = "Retrieve orders with ASSIGNED status waiting to be started at a workstation")
    @ApiResponse(responseCode = "200", description = "List of unassigned orders")
    @GetMapping("/workstation/{workstationId}/unassigned")
    public ResponseEntity<List<ProductionControlOrderDTO>> getUnassignedOrders(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getUnassignedOrders(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", 
               description = "Retrieve a production control order by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductionControlOrderDTO> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        Optional<ProductionControlOrderDTO> order = productionControlOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get order by number", 
               description = "Retrieve a production control order by its order number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/number/{controlOrderNumber}")
    public ResponseEntity<ProductionControlOrderDTO> getOrderByNumber(
            @Parameter(description = "Control order number (e.g., PCO-00001)") @PathVariable String controlOrderNumber) {
        Optional<ProductionControlOrderDTO> order = productionControlOrderService.getOrderByNumber(controlOrderNumber);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Confirm receipt", 
               description = "Confirm receipt of a production control order (PENDING -> CONFIRMED)")
    @ApiResponse(responseCode = "200", description = "Order confirmed")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ProductionControlOrderDTO> confirmReceipt(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        ProductionControlOrderDTO order = productionControlOrderService.confirmReceipt(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Start production", 
               description = "Start production on a control order (CONFIRMED -> IN_PROGRESS)")
    @ApiResponse(responseCode = "200", description = "Production started")
    @PostMapping("/{id}/start")
    public ResponseEntity<ProductionControlOrderDTO> startProduction(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        ProductionControlOrderDTO order = productionControlOrderService.startProduction(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Complete production", 
               description = "Complete production on a control order and propagate status upward")
    @ApiResponse(responseCode = "200", description = "Production completed")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ProductionControlOrderDTO> completeProduction(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        ProductionControlOrderDTO order = productionControlOrderService.completeProduction(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Halt production", 
               description = "Halt production on a control order with a reason")
    @ApiResponse(responseCode = "200", description = "Production halted")
    @PostMapping("/{id}/halt")
    public ResponseEntity<ProductionControlOrderDTO> haltProduction(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @RequestBody HaltRequest request) {
        ProductionControlOrderDTO order = productionControlOrderService.haltProduction(id, request.getReason());
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Update operator notes", 
               description = "Update the operator notes for a control order")
    @ApiResponse(responseCode = "200", description = "Notes updated")
    @PatchMapping("/{id}/notes")
    public ResponseEntity<ProductionControlOrderDTO> updateNotes(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @RequestBody NotesRequest request) {
        ProductionControlOrderDTO order = productionControlOrderService.updateOperatorNotes(id, request.getNotes());
        return ResponseEntity.ok(order);
    }

    /**
     * Request parts for a production control order.
     * Creates a supply order to Parts Supply Warehouse.
     */
    @PostMapping("/{id}/request-parts")
    public ResponseEntity<SupplyOrderDTO> requestParts(
            @PathVariable Long id,
            @RequestBody RequestPartsRequest request) {
        SupplyOrderDTO supplyOrder = productionControlOrderService.requestSupplies(
                id,
                request.getRequiredParts(),
                request.getNeededBy(),
                request.getNotes()
        );
        return ResponseEntity.ok(supplyOrder);
    }

    /**
     * Dispatch production control order to workstation.
     * Requires supply order to be fulfilled first (if one exists).
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<ProductionControlOrderDTO> dispatchToWorkstation(@PathVariable Long id) {
        ProductionControlOrderDTO order = productionControlOrderService.dispatchToWorkstation(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Get supply orders for a production control order.
     */
    @GetMapping("/{id}/supply-orders")
    public ResponseEntity<List<SupplyOrderDTO>> getSupplyOrders(@PathVariable Long id) {
        List<SupplyOrderDTO> supplyOrders = productionControlOrderService.getSupplyOrders(id);
        return ResponseEntity.ok(supplyOrders);
    }

    /**
     * Update defect information
     */
    @PatchMapping("/{id}/defects")
    public ResponseEntity<ProductionControlOrderDTO> updateDefects(
            @PathVariable Long id,
            @RequestBody DefectsRequest request) {
        ProductionControlOrderDTO order = productionControlOrderService.updateDefects(
                id,
                request.getDefectsFound(),
                request.getDefectsReworked(),
                request.getReworkRequired()
        );
        return ResponseEntity.ok(order);
    }

    /**
     * Create a production control order from SimAL schedule data.
     * This endpoint is called by the SimAL integration service when a production
     * order has been scheduled and assigned to a production workstation.
     */
    @PostMapping
    public ResponseEntity<ProductionControlOrderDTO> createControlOrder(
            @RequestBody ProductionControlOrderCreateRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime targetStart = LocalDateTime.parse(request.getTargetStartTime(), formatter);
        LocalDateTime targetCompletion = LocalDateTime.parse(request.getTargetCompletionTime(), formatter);

        ProductionControlOrderDTO order = productionControlOrderService.createControlOrder(
                request.getSourceProductionOrderId(),
                request.getAssignedWorkstationId(),
                request.getSimalScheduleId(),
                request.getPriority(),
                targetStart,
                targetCompletion,
                request.getProductionInstructions(),
                request.getQualityCheckpoints(),
                "Standard safety procedures apply",
                120,  // Default 2-hour estimate
                request.getItemId(),
                request.getItemType(),
                request.getQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
