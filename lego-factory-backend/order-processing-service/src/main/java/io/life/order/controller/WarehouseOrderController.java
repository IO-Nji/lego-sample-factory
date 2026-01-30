package io.life.order.controller;

import io.life.order.dto.WarehouseOrderDTO;
import io.life.order.service.WarehouseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/warehouse-orders")
@Tag(name = "Warehouse Orders", description = "WS-8 Modules Supermarket - Internal warehouse order management")
public class WarehouseOrderController {

    private final WarehouseOrderService warehouseOrderService;

    public WarehouseOrderController(WarehouseOrderService warehouseOrderService) {
        this.warehouseOrderService = warehouseOrderService;
    }

    @Operation(summary = "Get all warehouse orders", 
               description = "Retrieve all warehouse orders in the system")
    @ApiResponse(responseCode = "200", description = "List of warehouse orders returned")
    @GetMapping
    public ResponseEntity<List<WarehouseOrderDTO>> getAllWarehouseOrders() {
        List<WarehouseOrderDTO> orders = warehouseOrderService.getAllWarehouseOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get warehouse order by ID", 
               description = "Retrieve a specific warehouse order by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warehouse order found"),
        @ApiResponse(responseCode = "404", description = "Warehouse order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseOrderDTO> getWarehouseOrderById(
            @Parameter(description = "Warehouse order ID") @PathVariable Long id) {
        Optional<WarehouseOrderDTO> order = warehouseOrderService.getWarehouseOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get warehouse orders by workstation", 
               description = "Retrieve warehouse orders for a specific workstation (typically WS-8 Modules Supermarket)")
    @ApiResponse(responseCode = "200", description = "List of warehouse orders for workstation")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<WarehouseOrderDTO>> getWarehouseOrdersByWorkstationId(
            @Parameter(description = "Workstation ID (typically 8 for Modules Supermarket)") @PathVariable Long workstationId) {
        List<WarehouseOrderDTO> orders = warehouseOrderService.getWarehouseOrdersByFulfillingWorkstationId(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get warehouse orders by status", 
               description = "Retrieve warehouse orders filtered by status (PENDING, CONFIRMED, FULFILLED, etc.)")
    @ApiResponse(responseCode = "200", description = "List of warehouse orders with specified status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<WarehouseOrderDTO>> getWarehouseOrdersByStatus(
            @Parameter(description = "Order status (PENDING, CONFIRMED, PROCESSING, FULFILLED, CANCELLED)") @PathVariable String status) {
        List<WarehouseOrderDTO> orders = warehouseOrderService.getWarehouseOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Confirm warehouse order", 
               description = "Confirm a pending warehouse order. Checks module availability and sets triggerScenario (DIRECT_FULFILLMENT or PRODUCTION_REQUIRED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warehouse order confirmed"),
        @ApiResponse(responseCode = "404", description = "Warehouse order not found")
    })
    @PutMapping("/{id}/confirm")
    public ResponseEntity<WarehouseOrderDTO> confirmWarehouseOrder(
            @Parameter(description = "Warehouse order ID") @PathVariable Long id) {
        WarehouseOrderDTO confirmedOrder = warehouseOrderService.confirmWarehouseOrder(id);
        return ResponseEntity.ok(confirmedOrder);
    }

    @Operation(summary = "Fulfill warehouse order", 
               description = "Fulfill a warehouse order by debiting modules from WS-8 and creating Final Assembly orders for WS-6")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warehouse order fulfilled, Final Assembly orders created"),
        @ApiResponse(responseCode = "404", description = "Warehouse order not found"),
        @ApiResponse(responseCode = "400", description = "Insufficient module stock")
    })
    @PutMapping("/{id}/fulfill-modules")
    public ResponseEntity<WarehouseOrderDTO> fulfillWarehouseOrder(
            @Parameter(description = "Warehouse order ID") @PathVariable Long id) {
        WarehouseOrderDTO fulfilledOrder = warehouseOrderService.fulfillWarehouseOrder(id);
        return ResponseEntity.ok(fulfilledOrder);
    }

    @Operation(summary = "Update warehouse order status", 
               description = "Manually update the status of a warehouse order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "404", description = "Warehouse order not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<WarehouseOrderDTO> updateWarehouseOrderStatus(
            @Parameter(description = "Warehouse order ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam String status) {
        WarehouseOrderDTO updatedOrder = warehouseOrderService.updateWarehouseOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }
}
