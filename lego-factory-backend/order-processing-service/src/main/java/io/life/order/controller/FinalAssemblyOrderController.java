package io.life.order.controller;

import io.life.order.entity.FinalAssemblyOrder;
import io.life.order.service.FinalAssemblyOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Final Assembly Orders (WS-6)
 * Handles workstation-specific orders for final assembly operations
 * INPUT: MODULES (from Modules Supermarket) → OUTPUT: Product Variants
 * CRITICAL: Only workstation that consumes MODULES instead of PARTS
 */
@RestController
@RequestMapping("/api/final-assembly-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class FinalAssemblyOrderController {

    private final FinalAssemblyOrderService finalAssemblyOrderService;

    @GetMapping
    public ResponseEntity<List<FinalAssemblyOrder>> getAllOrders() {
        List<FinalAssemblyOrder> orders = finalAssemblyOrderService.getOrdersForWorkstation(6L);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<FinalAssemblyOrder>> getOrdersByWorkstation(@PathVariable Long workstationId) {
        List<FinalAssemblyOrder> orders = finalAssemblyOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinalAssemblyOrder> getOrderById(@PathVariable Long id) {
        try {
            FinalAssemblyOrder order = finalAssemblyOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/warehouse-order/{warehouseOrderId}")
    public ResponseEntity<List<FinalAssemblyOrder>> getOrdersByWarehouseOrder(@PathVariable Long warehouseOrderId) {
        List<FinalAssemblyOrder> orders = finalAssemblyOrderService.getOrdersByWarehouseOrder(warehouseOrderId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<FinalAssemblyOrder>> getOrdersByControlOrder(@PathVariable Long controlOrderId) {
        List<FinalAssemblyOrder> orders = finalAssemblyOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<FinalAssemblyOrder> confirmOrder(@PathVariable Long id) {
        try {
            FinalAssemblyOrder order = finalAssemblyOrderService.confirmOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to confirm final assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<FinalAssemblyOrder> startOrder(@PathVariable Long id) {
        try {
            FinalAssemblyOrder order = finalAssemblyOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start final assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<FinalAssemblyOrder> completeOrder(@PathVariable Long id) {
        try {
            FinalAssemblyOrder order = finalAssemblyOrderService.completeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to complete final assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            log.error("Error completing final assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/halt")
    public ResponseEntity<FinalAssemblyOrder> haltOrder(@PathVariable Long id) {
        try {
            FinalAssemblyOrder order = finalAssemblyOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt final assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<FinalAssemblyOrder> resumeOrder(@PathVariable Long id) {
        try {
            FinalAssemblyOrder order = finalAssemblyOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume final assembly order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
