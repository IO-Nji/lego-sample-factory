package io.life.order.controller;

import io.life.order.entity.InjectionMoldingOrder;
import io.life.order.service.InjectionMoldingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Injection Molding Orders (WS-1)
 * Handles workstation-specific orders for injection molding operations
 * INPUT: Raw materials → OUTPUT: Basic molded parts
 */
@RestController
@RequestMapping("/api/injection-molding-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Injection Molding Orders", description = "WS-1 Injection Molding - Plastic parts production from raw materials")
public class InjectionMoldingOrderController {

    private final InjectionMoldingOrderService injectionMoldingOrderService;

    @Operation(summary = "Get all injection molding orders", description = "Retrieve all orders for WS-1")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping
    public ResponseEntity<List<InjectionMoldingOrder>> getAllOrders() {
        List<InjectionMoldingOrder> orders = injectionMoldingOrderService.getOrdersForWorkstation(1L);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", description = "Retrieve orders for a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<InjectionMoldingOrder>> getOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<InjectionMoldingOrder> orders = injectionMoldingOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific injection molding order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<InjectionMoldingOrder> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get orders by control order", description = "Retrieve orders under a control order")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<InjectionMoldingOrder>> getOrdersByControlOrder(
            @Parameter(description = "Production control order ID") @PathVariable Long controlOrderId) {
        List<InjectionMoldingOrder> orders = injectionMoldingOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Start order", description = "Begin injection molding - changes status to IN_PROGRESS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order started"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<InjectionMoldingOrder> startOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Complete order", description = "Complete molding - passes parts to WS-2")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order completed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<InjectionMoldingOrder> completeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.completeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to complete injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            log.error("Error completing injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Halt order", description = "Temporarily pause production")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order halted"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/halt")
    public ResponseEntity<InjectionMoldingOrder> haltOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Resume order", description = "Resume a halted order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order resumed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/resume")
    public ResponseEntity<InjectionMoldingOrder> resumeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
