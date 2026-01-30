package io.life.order.controller;

import io.life.order.entity.PartFinishingOrder;
import io.life.order.service.PartFinishingOrderService;
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
 * REST Controller for Part Finishing Orders (WS-3)
 * Handles workstation-specific orders for part finishing operations
 * INPUT: Pre-processed parts → OUTPUT: Finished parts (polished, coated, inspected)
 */
@RestController
@RequestMapping("/api/part-finishing-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Part Finishing Orders", description = "WS-3 Part Finishing - Final finishing, polishing, and quality inspection")
public class PartFinishingOrderController {

    private final PartFinishingOrderService partFinishingOrderService;

    @Operation(summary = "Get all part finishing orders", description = "Retrieve all orders for WS-3")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping
    public ResponseEntity<List<PartFinishingOrder>> getAllOrders() {
        List<PartFinishingOrder> orders = partFinishingOrderService.getOrdersForWorkstation(3L);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", description = "Retrieve orders for a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<PartFinishingOrder>> getOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<PartFinishingOrder> orders = partFinishingOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific part finishing order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PartFinishingOrder> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get orders by control order", description = "Retrieve orders under a control order")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<PartFinishingOrder>> getOrdersByControlOrder(
            @Parameter(description = "Production control order ID") @PathVariable Long controlOrderId) {
        List<PartFinishingOrder> orders = partFinishingOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Start order", description = "Begin finishing - changes status to IN_PROGRESS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order started"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<PartFinishingOrder> startOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Complete order", description = "Complete finishing - credits Parts Supply Warehouse")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order completed, inventory credited"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<PartFinishingOrder> completeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.completeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to complete part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            log.error("Error completing part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Halt order", description = "Temporarily pause production")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order halted"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/halt")
    public ResponseEntity<PartFinishingOrder> haltOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Resume order", description = "Resume a halted order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order resumed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/resume")
    public ResponseEntity<PartFinishingOrder> resumeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Mark waiting for parts", description = "Mark order as waiting for supply order fulfillment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order marked as waiting"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/{id}/mark-waiting")
    public ResponseEntity<PartFinishingOrder> markWaitingForParts(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Supply order ID to wait for") @RequestParam Long supplyOrderId) {
        try {
            PartFinishingOrder order = partFinishingOrderService.markWaitingForParts(id, supplyOrderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to mark part finishing order {} as waiting: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
