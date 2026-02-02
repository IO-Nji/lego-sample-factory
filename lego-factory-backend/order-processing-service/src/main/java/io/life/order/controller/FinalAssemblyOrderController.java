package io.life.order.controller;

import io.life.order.dto.FinalAssemblyOrderDTO;
import io.life.order.service.FinalAssemblyOrderService;
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
@RequestMapping("/api/final-assembly-orders")
@Tag(name = "Final Assembly Orders", description = "WS-6 Final Assembly - Product assembly from modules")
public class FinalAssemblyOrderController {

    private final FinalAssemblyOrderService finalAssemblyOrderService;

    public FinalAssemblyOrderController(FinalAssemblyOrderService finalAssemblyOrderService) {
        this.finalAssemblyOrderService = finalAssemblyOrderService;
    }

    @Operation(summary = "Get all final assembly orders", description = "Retrieve all orders for WS-6")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getAllOrders() {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific final assembly order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FinalAssemblyOrderDTO> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        Optional<FinalAssemblyOrderDTO> order = finalAssemblyOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get orders by workstation", description = "Retrieve orders for a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getOrdersByWorkstationId(
            @Parameter(description = "Workstation ID (typically 6)") @PathVariable Long workstationId) {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getOrdersByWorkstationId(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by status", description = "Filter orders by status")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getOrdersByStatus(
            @Parameter(description = "Order status (PENDING, CONFIRMED, IN_PROGRESS, etc.)") @PathVariable String status) {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by warehouse order", description = "Retrieve orders created from a warehouse order")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/warehouse-order/{warehouseOrderId}")
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getOrdersByWarehouseOrderId(
            @Parameter(description = "Source warehouse order ID") @PathVariable Long warehouseOrderId) {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getOrdersByWarehouseOrderId(warehouseOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Check all orders submitted", 
               description = "Check if all final assembly orders for a warehouse order are submitted")
    @ApiResponse(responseCode = "200", description = "Boolean result")
    @GetMapping("/warehouse-order/{warehouseOrderId}/all-submitted")
    public ResponseEntity<Boolean> areAllOrdersSubmitted(
            @Parameter(description = "Warehouse order ID") @PathVariable Long warehouseOrderId) {
        boolean allSubmitted = finalAssemblyOrderService.areAllOrdersSubmittedForWarehouseOrder(warehouseOrderId);
        return ResponseEntity.ok(allSubmitted);
    }

    @Operation(summary = "Confirm order", description = "Change status from PENDING to CONFIRMED")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order confirmed"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{id}/confirm")
    public ResponseEntity<FinalAssemblyOrderDTO> confirmOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        FinalAssemblyOrderDTO confirmedOrder = finalAssemblyOrderService.confirmOrder(id);
        return ResponseEntity.ok(confirmedOrder);
    }

    @Operation(summary = "Start order", description = "Change status from CONFIRMED to IN_PROGRESS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order started"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<FinalAssemblyOrderDTO> startOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        FinalAssemblyOrderDTO startedOrder = finalAssemblyOrderService.startOrder(id);
        return ResponseEntity.ok(startedOrder);
    }

    @Operation(summary = "Complete order", description = "Complete assembly and update status to COMPLETED")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order completed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<FinalAssemblyOrderDTO> completeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        FinalAssemblyOrderDTO completedOrder = finalAssemblyOrderService.completeOrder(id);
        return ResponseEntity.ok(completedOrder);
    }

    @Operation(summary = "Submit order", 
               description = "Submit completed order - credits Plant Warehouse with finished products")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order submitted, inventory credited"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/submit")
    public ResponseEntity<FinalAssemblyOrderDTO> submitOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        FinalAssemblyOrderDTO submittedOrder = finalAssemblyOrderService.submitOrder(id);
        return ResponseEntity.ok(submittedOrder);
    }

    @Operation(summary = "Update order status", description = "Manually update order status")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PatchMapping("/{id}/status")
    public ResponseEntity<FinalAssemblyOrderDTO> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam String status) {
        FinalAssemblyOrderDTO updatedOrder = finalAssemblyOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }
}
