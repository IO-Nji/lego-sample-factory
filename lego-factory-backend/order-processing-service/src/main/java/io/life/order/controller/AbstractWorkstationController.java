package io.life.order.controller;

import io.life.order.dto.request.HaltRequest;
import io.life.order.dto.request.NotesRequest;
import io.life.order.service.WorkstationOrderOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base controller for workstation-specific order management.
 * Provides common CRUD and workflow operations for both manufacturing (WS-1,2,3)
 * and assembly (WS-4,5,6) workstations.
 * 
 * Subclasses only need to:
 * 1. Provide the @RequestMapping annotation with their specific path
 * 2. Inject their specific service and pass it to super constructor
 * 3. Override methods only if they need workstation-specific behavior
 * 
 * This eliminates ~95% of duplicate code across 6 station controllers.
 * 
 * @param <T> The DTO type (ProductionControlOrderDTO or AssemblyControlOrderDTO)
 * @param <S> The service type implementing WorkstationOrderOperations<T>
 */
public abstract class AbstractWorkstationController<T, S extends WorkstationOrderOperations<T>> {

    protected final S service;

    protected AbstractWorkstationController(S service) {
        this.service = service;
    }

    /**
     * Get the service instance for subclass access.
     */
    protected S getService() {
        return service;
    }

    /**
     * Get all orders for a workstation.
     */
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<T>> getOrdersByWorkstation(@PathVariable Long workstationId) {
        List<T> orders = service.getOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get active (IN_PROGRESS) orders for a workstation.
     */
    @GetMapping("/workstation/{workstationId}/active")
    public ResponseEntity<List<T>> getActiveOrdersByWorkstation(@PathVariable Long workstationId) {
        List<T> orders = service.getActiveOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get unassigned (ASSIGNED status) orders for a workstation.
     */
    @GetMapping("/workstation/{workstationId}/unassigned")
    public ResponseEntity<List<T>> getUnassignedOrders(@PathVariable Long workstationId) {
        List<T> orders = service.getUnassignedOrders(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<T> getOrderById(@PathVariable Long id) {
        Optional<T> order = service.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get order by control order number.
     */
    @GetMapping("/number/{controlOrderNumber}")
    public ResponseEntity<T> getOrderByNumber(@PathVariable String controlOrderNumber) {
        Optional<T> order = service.getOrderByNumber(controlOrderNumber);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Start work on an order.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<T> startWork(@PathVariable Long id) {
        try {
            T order = service.startWork(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Complete work on an order.
     * Handles inventory credits and SimAL notifications.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<T> completeWork(@PathVariable Long id) {
        try {
            T order = service.completeWork(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Halt work on an order.
     */
    @PostMapping("/{id}/halt")
    public ResponseEntity<T> haltWork(@PathVariable Long id, @RequestBody(required = false) HaltRequest request) {
        try {
            String reason = (request != null) ? request.getReason() : null;
            T order = service.haltWork(id, reason);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Update operator notes on an order.
     */
    @PatchMapping("/{id}/notes")
    public ResponseEntity<T> updateNotes(@PathVariable Long id, @RequestBody NotesRequest request) {
        try {
            T order = service.updateOperatorNotes(id, request.getNotes());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
