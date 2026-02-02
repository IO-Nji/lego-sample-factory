package io.life.order.controller;

import io.life.order.entity.MotorAssemblyOrder;
import io.life.order.service.MotorAssemblyOrderService;
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
 * REST Controller for Motor Assembly Orders (WS-5)
 * Handles workstation-specific orders for motor assembly operations
 * INPUT: Motor-specific parts (motors, wires, housings) → OUTPUT: Motor modules
 */
@RestController
@RequestMapping("/api/motor-assembly-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Motor Assembly Orders", description = "WS-5 Motor Assembly - Motor module assembly from parts")
public class MotorAssemblyOrderController {

    private final MotorAssemblyOrderService motorAssemblyOrderService;

    @Operation(summary = "Get all motor assembly orders", description = "Retrieve all orders for WS-5")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping
    public ResponseEntity<List<MotorAssemblyOrder>> getAllOrders() {
        List<MotorAssemblyOrder> orders = motorAssemblyOrderService.getOrdersForWorkstation(5L);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by workstation", description = "Retrieve orders for a specific workstation")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<MotorAssemblyOrder>> getOrdersByWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long workstationId) {
        List<MotorAssemblyOrder> orders = motorAssemblyOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific motor assembly order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MotorAssemblyOrder> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get orders by control order", description = "Retrieve orders under a control order")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<MotorAssemblyOrder>> getOrdersByControlOrder(
            @Parameter(description = "Assembly control order ID") @PathVariable Long controlOrderId) {
        List<MotorAssemblyOrder> orders = motorAssemblyOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Start order", description = "Begin motor assembly - changes status to IN_PROGRESS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order started"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<MotorAssemblyOrder> startOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start motor assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Complete order", description = "Complete assembly - credits Modules Supermarket")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order completed, inventory credited"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<MotorAssemblyOrder> completeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.completeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to complete motor assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            log.error("Error completing motor assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Halt order", description = "Temporarily pause assembly")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order halted"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/halt")
    public ResponseEntity<MotorAssemblyOrder> haltOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt motor assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Resume order", description = "Resume a halted order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order resumed"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition")
    })
    @PostMapping("/{id}/resume")
    public ResponseEntity<MotorAssemblyOrder> resumeOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume motor assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/mark-waiting")
    public ResponseEntity<MotorAssemblyOrder> markWaitingForParts(
            @PathVariable Long id,
            @RequestParam Long supplyOrderId) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.markWaitingForParts(id, supplyOrderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to mark motor assembly order {} as waiting: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
