package io.life.order.controller;

import io.life.order.entity.PartPreProductionOrder;
import io.life.order.service.PartPreProductionOrderService;
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
 * REST Controller for Part Pre-Production Orders (WS-2)
 * Handles workstation-specific orders for parts pre-production operations
 * INPUT: Basic parts → OUTPUT: Pre-processed parts (machined, drilled, cut)
 */
@RestController
@RequestMapping("/api/part-preproduction-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Part Pre-Production Orders", description = "WS-2 Parts Pre-Production - Machining and preparation of molded parts")
public class PartPreProductionOrderController {

    private final PartPreProductionOrderService partPreProductionOrderService;

    @Operation(summary = "Get all pre-production orders", description = "Retrieve all orders for WS-2")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping
    public ResponseEntity<List<PartPreProductionOrder>> getAllOrders() {
        List<PartPreProductionOrder> orders = partPreProductionOrderService.getOrdersForWorkstation(2L);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", description = "Retrieve orders for a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<PartPreProductionOrder>> getOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<PartPreProductionOrder> orders = partPreProductionOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific pre-production order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PartPreProductionOrder> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get orders by control order", description = "Retrieve orders under a control order")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<PartPreProductionOrder>> getOrdersByControlOrder(
            @Parameter(description = "Production control order ID") @PathVariable Long controlOrderId) {
        List<PartPreProductionOrder> orders = partPreProductionOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Start order", description = "Begin pre-production - changes status to IN_PROGRESS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order started"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<PartPreProductionOrder> startOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Complete order", description = "Complete pre-production - passes parts to WS-3")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order completed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<PartPreProductionOrder> completeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.completeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to complete part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            log.error("Error completing part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Halt order", description = "Temporarily pause production")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order halted"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/halt")
    public ResponseEntity<PartPreProductionOrder> haltOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Resume order", description = "Resume a halted order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order resumed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/resume")
    public ResponseEntity<PartPreProductionOrder> resumeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Mark waiting for parts", description = "Mark order as waiting for supply order fulfillment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order marked as waiting"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/{id}/mark-waiting")
    public ResponseEntity<PartPreProductionOrder> markWaitingForParts(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Supply order ID to wait for") @RequestParam Long supplyOrderId) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.markWaitingForParts(id, supplyOrderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to mark part pre-production order {} as waiting: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
