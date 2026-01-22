package io.life.order.controller;

import io.life.order.entity.InjectionMoldingOrder;
import io.life.order.service.InjectionMoldingOrderService;
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
public class InjectionMoldingOrderController {

    private final InjectionMoldingOrderService injectionMoldingOrderService;

    @GetMapping
    public ResponseEntity<List<InjectionMoldingOrder>> getAllOrders() {
        List<InjectionMoldingOrder> orders = injectionMoldingOrderService.getOrdersForWorkstation(1L);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<InjectionMoldingOrder>> getOrdersByWorkstation(@PathVariable Long workstationId) {
        List<InjectionMoldingOrder> orders = injectionMoldingOrderService.getOrdersForWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InjectionMoldingOrder> getOrderById(@PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/control-order/{controlOrderId}")
    public ResponseEntity<List<InjectionMoldingOrder>> getOrdersByControlOrder(@PathVariable Long controlOrderId) {
        List<InjectionMoldingOrder> orders = injectionMoldingOrderService.getOrdersByControlOrder(controlOrderId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<InjectionMoldingOrder> startOrder(@PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.startOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to start injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<InjectionMoldingOrder> completeOrder(@PathVariable Long id) {
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

    @PostMapping("/{id}/halt")
    public ResponseEntity<InjectionMoldingOrder> haltOrder(@PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.haltOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to halt injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<InjectionMoldingOrder> resumeOrder(@PathVariable Long id) {
        try {
            InjectionMoldingOrder order = injectionMoldingOrderService.resumeOrder(id);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to resume injection molding order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
