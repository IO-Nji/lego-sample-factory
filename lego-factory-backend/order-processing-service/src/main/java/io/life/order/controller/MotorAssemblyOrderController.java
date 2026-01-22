package io.life.order.controller;

import io.life.order.entity.MotorAssemblyOrder;
import io.life.order.service.MotorAssemblyOrderService;
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
public class MotorAssemblyOrderController {

    private final MotorAssemblyOrderService motorAssemblyOrderService;

    @GetMapping
    public ResponseEntity<List<MotorAssemblyOrder>> getAllOrders() {
        List<MotorAssemblyOrder> orders = motorAssemblyOrderService.getOrdersForWorkstation(5L);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<MotorAssemblyOrder>> getOrdersByWorkstation(@PathVariable Long workstationId) {
        List<MotorAssemblyOrder> orders = motorAssemblyOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MotorAssemblyOrder> getOrderById(@PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<MotorAssemblyOrder>> getOrdersByControlOrder(@PathVariable Long controlOrderId) {
        List<MotorAssemblyOrder> orders = motorAssemblyOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<MotorAssemblyOrder> startOrder(@PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start motor assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<MotorAssemblyOrder> completeOrder(@PathVariable Long id) {
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

    @PostMapping("/{id}/halt")
    public ResponseEntity<MotorAssemblyOrder> haltOrder(@PathVariable Long id) {
        try {
            MotorAssemblyOrder order = motorAssemblyOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt motor assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<MotorAssemblyOrder> resumeOrder(@PathVariable Long id) {
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
