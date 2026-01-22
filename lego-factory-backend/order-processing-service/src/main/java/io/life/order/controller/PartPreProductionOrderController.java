package io.life.order.controller;

import io.life.order.entity.PartPreProductionOrder;
import io.life.order.service.PartPreProductionOrderService;
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
public class PartPreProductionOrderController {

    private final PartPreProductionOrderService partPreProductionOrderService;

    @GetMapping
    public ResponseEntity<List<PartPreProductionOrder>> getAllOrders() {
        List<PartPreProductionOrder> orders = partPreProductionOrderService.getOrdersForWorkstation(2L);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<PartPreProductionOrder>> getOrdersByWorkstation(@PathVariable Long workstationId) {
        List<PartPreProductionOrder> orders = partPreProductionOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartPreProductionOrder> getOrderById(@PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<PartPreProductionOrder>> getOrdersByControlOrder(@PathVariable Long controlOrderId) {
        List<PartPreProductionOrder> orders = partPreProductionOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<PartPreProductionOrder> startOrder(@PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<PartPreProductionOrder> completeOrder(@PathVariable Long id) {
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

    @PostMapping("/{id}/halt")
    public ResponseEntity<PartPreProductionOrder> haltOrder(@PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<PartPreProductionOrder> resumeOrder(@PathVariable Long id) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume part pre-production order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/mark-waiting")
    public ResponseEntity<PartPreProductionOrder> markWaitingForParts(
            @PathVariable Long id,
            @RequestParam Long supplyOrderId) {
        try {
            PartPreProductionOrder order = partPreProductionOrderService.markWaitingForParts(id, supplyOrderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to mark part pre-production order {} as waiting: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
