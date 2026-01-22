package io.life.order.controller;

import io.life.order.entity.PartFinishingOrder;
import io.life.order.service.PartFinishingOrderService;
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
public class PartFinishingOrderController {

    private final PartFinishingOrderService partFinishingOrderService;

    @GetMapping
    public ResponseEntity<List<PartFinishingOrder>> getAllOrders() {
        List<PartFinishingOrder> orders = partFinishingOrderService.getOrdersForWorkstation(3L);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<PartFinishingOrder>> getOrdersByWorkstation(@PathVariable Long workstationId) {
        List<PartFinishingOrder> orders = partFinishingOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartFinishingOrder> getOrderById(@PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<PartFinishingOrder>> getOrdersByControlOrder(@PathVariable Long controlOrderId) {
        List<PartFinishingOrder> orders = partFinishingOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<PartFinishingOrder> startOrder(@PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<PartFinishingOrder> completeOrder(@PathVariable Long id) {
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

    @PostMapping("/{id}/halt")
    public ResponseEntity<PartFinishingOrder> haltOrder(@PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<PartFinishingOrder> resumeOrder(@PathVariable Long id) {
        try {
            PartFinishingOrder order = partFinishingOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume part finishing order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/mark-waiting")
    public ResponseEntity<PartFinishingOrder> markWaitingForParts(
            @PathVariable Long id,
            @RequestParam Long supplyOrderId) {
        try {
            PartFinishingOrder order = partFinishingOrderService.markWaitingForParts(id, supplyOrderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to mark part finishing order {} as waiting: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
