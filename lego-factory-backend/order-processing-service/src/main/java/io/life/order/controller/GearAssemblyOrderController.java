package io.life.order.controller;

import io.life.order.entity.GearAssemblyOrder;
import io.life.order.service.GearAssemblyOrderService;
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
 * REST Controller for Gear Assembly Orders (WS-4)
 * Handles workstation-specific orders for gear assembly operations
 * INPUT: Gear-specific parts (gears, shafts, bearings) → OUTPUT: Gear modules
 */
@RestController
@RequestMapping("/api/gear-assembly-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gear Assembly Orders", description = "WS-4 Gear Assembly - Gear module assembly from parts")
public class GearAssemblyOrderController {

    private final GearAssemblyOrderService gearAssemblyOrderService;

    @Operation(summary = "Get all gear assembly orders", description = "Retrieve all orders for WS-4")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping
    public ResponseEntity<List<GearAssemblyOrder>> getAllOrders() {
        List<GearAssemblyOrder> orders = gearAssemblyOrderService.getOrdersForWorkstation(4L);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", description = "Retrieve orders for a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<GearAssemblyOrder>> getOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<GearAssemblyOrder> orders = gearAssemblyOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific gear assembly order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<GearAssemblyOrder> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get orders by control order", description = "Retrieve orders under a control order")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<GearAssemblyOrder>> getOrdersByControlOrder(
            @Parameter(description = "Assembly control order ID") @PathVariable Long controlOrderId) {
        List<GearAssemblyOrder> orders = gearAssemblyOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Start order", description = "Begin gear assembly - changes status to IN_PROGRESS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order started"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<GearAssemblyOrder> startOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start gear assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Complete order", description = "Complete assembly - credits Modules Supermarket")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order completed, inventory credited"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<GearAssemblyOrder> completeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.completeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to complete gear assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            log.error("Error completing gear assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Halt order", description = "Temporarily pause assembly")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order halted"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/halt")
    public ResponseEntity<GearAssemblyOrder> haltOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt gear assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Resume order", description = "Resume a halted order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order resumed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/resume")
    public ResponseEntity<GearAssemblyOrder> resumeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume gear assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/mark-waiting")
    public ResponseEntity<GearAssemblyOrder> markWaitingForParts(
            @PathVariable Long id,
            @RequestParam Long supplyOrderId) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.markWaitingForParts(id, supplyOrderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to mark gear assembly order {} as waiting: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
