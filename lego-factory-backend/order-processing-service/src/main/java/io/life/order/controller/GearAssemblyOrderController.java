package io.life.order.controller;

import io.life.order.entity.GearAssemblyOrder;
import io.life.order.service.GearAssemblyOrderService;
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
public class GearAssemblyOrderController {

    private final GearAssemblyOrderService gearAssemblyOrderService;

    @GetMapping
    public ResponseEntity<List<GearAssemblyOrder>> getAllOrders() {
        List<GearAssemblyOrder> orders = gearAssemblyOrderService.getOrdersForWorkstation(4L);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<GearAssemblyOrder>> getOrdersByWorkstation(@PathVariable Long workstationId) {
        List<GearAssemblyOrder> orders = gearAssemblyOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GearAssemblyOrder> getOrderById(@PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<GearAssemblyOrder>> getOrdersByControlOrder(@PathVariable Long controlOrderId) {
        List<GearAssemblyOrder> orders = gearAssemblyOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<GearAssemblyOrder> startOrder(@PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start gear assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<GearAssemblyOrder> completeOrder(@PathVariable Long id) {
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

    @PostMapping("/{id}/halt")
    public ResponseEntity<GearAssemblyOrder> haltOrder(@PathVariable Long id) {
        try {
            GearAssemblyOrder order = gearAssemblyOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt gear assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<GearAssemblyOrder> resumeOrder(@PathVariable Long id) {
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
