package io.life.order.controller;

import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.request.SupplyOrderCreateRequest;
import io.life.order.dto.request.SupplyOrderFromControlRequest;
import io.life.order.service.SupplyOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for Supply Order management.
 * Handles supply orders for Parts Supply Warehouse.
 */
@RestController
@RequestMapping("/api/supply-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Supply Orders", description = "WS-9 Parts Supply Warehouse - Supply order management for parts distribution")
public class SupplyOrderController {

    private static final Logger logger = LoggerFactory.getLogger(SupplyOrderController.class);
    private final SupplyOrderService supplyOrderService;

    public SupplyOrderController(SupplyOrderService supplyOrderService) {
        this.supplyOrderService = supplyOrderService;
    }

    @Operation(summary = "Create supply order", 
               description = "Create a new supply order to request parts from Parts Supply Warehouse")
    @ApiResponse(responseCode = "201", description = "Supply order created")
    @PostMapping
    public ResponseEntity<SupplyOrderDTO> createSupplyOrder(@RequestBody SupplyOrderCreateRequest request) {
        SupplyOrderDTO order = supplyOrderService.createSupplyOrder(
                request.getSourceControlOrderId(),
                request.getSourceControlOrderType(),
                request.getRequestingWorkstationId(),
                request.getPriority(),
                request.getRequestedByTime(),
                request.getRequiredItems(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Create from control order", 
               description = "Create a supply order from a control order with automatic BOM lookup for required parts")
    @ApiResponse(responseCode = "201", description = "Supply order created")
    @PostMapping("/from-control-order")
    public ResponseEntity<SupplyOrderDTO> createFromControlOrder(@RequestBody SupplyOrderFromControlRequest request) {
        logger.info("Creating supply order from control order: controlOrderId={}, type={}, priority={}",
            request.getControlOrderId(), request.getControlOrderType(), request.getPriority());
        SupplyOrderDTO order = supplyOrderService.createSupplyOrderFromControlOrder(
                request.getControlOrderId(),
                request.getControlOrderType(),
                request.getPriority()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Get warehouse orders", 
               description = "Get all supply orders for the Parts Supply Warehouse dashboard")
    @ApiResponse(responseCode = "200", description = "List of supply orders")
    @GetMapping("/warehouse")
    public ResponseEntity<List<SupplyOrderDTO>> getWarehouseOrders(
            @Parameter(description = "Filter by status (PENDING, IN_PROGRESS, FULFILLED, REJECTED, CANCELLED)") 
            @RequestParam(required = false) String status) {
        List<SupplyOrderDTO> orders = supplyOrderService.getOrdersForSupplyWarehouse(status);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", 
               description = "Get all supply orders requested by a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of supply orders")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<SupplyOrderDTO>> getOrdersByWorkstation(
            @Parameter(description = "Requesting workstation ID") @PathVariable Long workstationId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status) {
        List<SupplyOrderDTO> orders = supplyOrderService.getOrdersByRequestingWorkstation(workstationId, status);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get supply order by ID", 
               description = "Retrieve a specific supply order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Supply order found"),
        @ApiResponse(responseCode = "404", description = "Supply order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SupplyOrderDTO> getSupplyOrder(
            @Parameter(description = "Supply order ID") @PathVariable Long id) {
        SupplyOrderDTO order = supplyOrderService.getSupplyOrder(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get by source control order", 
               description = "Get supply orders created from a specific control order")
    @ApiResponse(responseCode = "200", description = "List of supply orders")
    @GetMapping("/source/{controlOrderId}")
    public ResponseEntity<List<SupplyOrderDTO>> getBySourceControlOrder(
            @Parameter(description = "Source control order ID") @PathVariable Long controlOrderId,
            @Parameter(description = "Control order type (PRODUCTION or ASSEMBLY)") @RequestParam String type) {
        List<SupplyOrderDTO> orders = supplyOrderService.getBySourceControlOrder(controlOrderId, type);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Fulfill supply order", 
               description = "Fulfill a supply order and debit parts from WS-9 inventory")
    @ApiResponse(responseCode = "200", description = "Supply order fulfilled")
    @PutMapping("/{id}/fulfill")
    public ResponseEntity<SupplyOrderDTO> fulfillSupplyOrder(
            @Parameter(description = "Supply order ID") @PathVariable Long id) {
        SupplyOrderDTO order = supplyOrderService.fulfillSupplyOrder(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Update status", 
               description = "Update the status of a supply order")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PatchMapping("/{id}/status")
    public ResponseEntity<SupplyOrderDTO> updateStatus(
            @Parameter(description = "Supply order ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        SupplyOrderDTO order = supplyOrderService.updateStatus(id, newStatus);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Reject supply order", 
               description = "Reject a supply order due to insufficient stock or other reason")
    @ApiResponse(responseCode = "200", description = "Supply order rejected")
    @PutMapping("/{id}/reject")
    public ResponseEntity<SupplyOrderDTO> rejectSupplyOrder(
            @Parameter(description = "Supply order ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        SupplyOrderDTO order = supplyOrderService.rejectSupplyOrder(id, reason);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancel supply order", 
               description = "Cancel a supply order")
    @ApiResponse(responseCode = "200", description = "Supply order cancelled")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<SupplyOrderDTO> cancelSupplyOrder(
            @Parameter(description = "Supply order ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        SupplyOrderDTO order = supplyOrderService.cancelSupplyOrder(id, reason);
        return ResponseEntity.ok(order);
    }
}
