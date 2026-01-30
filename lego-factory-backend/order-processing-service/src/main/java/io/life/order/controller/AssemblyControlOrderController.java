package io.life.order.controller;

import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.request.AssemblyControlOrderCreateRequest;
import io.life.order.dto.request.RequestPartsRequest;
import io.life.order.service.AssemblyControlOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for AssemblyControlOrder management.
 * Exposes endpoints for Assembly Control workstations to view and manage their assigned orders.
 */
@RestController
@RequestMapping("/api/assembly-control-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AssemblyControlOrderController {

    private final AssemblyControlOrderService assemblyControlOrderService;

    public AssemblyControlOrderController(AssemblyControlOrderService assemblyControlOrderService) {
        this.assemblyControlOrderService = assemblyControlOrderService;
    }

    /**
     * Get all control orders
     */
    @GetMapping
    public ResponseEntity<List<AssemblyControlOrderDTO>> getAllOrders() {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get all control orders for a workstation
     */
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<AssemblyControlOrderDTO>> getOrdersByWorkstation(
            @PathVariable Long workstationId) {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get active (in progress) control orders for a workstation
     */
    @GetMapping("/workstation/{workstationId}/active")
    public ResponseEntity<List<AssemblyControlOrderDTO>> getActiveOrdersByWorkstation(
            @PathVariable Long workstationId) {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getActiveOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get unassigned (status=ASSIGNED) control orders for a workstation
     */
    @GetMapping("/workstation/{workstationId}/unassigned")
    public ResponseEntity<List<AssemblyControlOrderDTO>> getUnassignedOrders(
            @PathVariable Long workstationId) {
        List<AssemblyControlOrderDTO> orders = assemblyControlOrderService.getUnassignedOrders(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get control order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssemblyControlOrderDTO> getOrderById(@PathVariable Long id) {
        Optional<AssemblyControlOrderDTO> order = assemblyControlOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get control order by control order number
     */
    @GetMapping("/number/{controlOrderNumber}")
    public ResponseEntity<AssemblyControlOrderDTO> getOrderByNumber(
            @PathVariable String controlOrderNumber) {
        Optional<AssemblyControlOrderDTO> order = assemblyControlOrderService.getOrderByNumber(controlOrderNumber);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Confirm receipt of an assembly control order.
     * Changes status from PENDING to CONFIRMED.
     * This is the first step in the control order workflow.
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<AssemblyControlOrderDTO> confirmReceipt(@PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.confirmReceipt(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Start assembly on a control order
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<AssemblyControlOrderDTO> startAssembly(@PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.startAssembly(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Update control order status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AssemblyControlOrderDTO> updateOperatorNotes(
            @PathVariable Long id,
            @RequestParam String notes) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.updateOperatorNotes(id, notes);
        return ResponseEntity.ok(order);
    }

    /**
     * Complete assembly on a control order
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<AssemblyControlOrderDTO> completeAssembly(@PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.completeAssembly(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Update defects found during assembly
     */
    @PatchMapping("/{id}/defects")
    public ResponseEntity<AssemblyControlOrderDTO> updateDefects(
            @PathVariable Long id,
            @RequestParam Integer defectsFound,
            @RequestParam(required = false) Integer defectsReworked,
            @RequestParam(required = false) Boolean reworkRequired) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.updateDefects(id, defectsFound, defectsReworked, reworkRequired);
        return ResponseEntity.ok(order);
    }

    /**
     * Update shipping notes for completed order
     */
    @PutMapping("/{id}/shipping-notes")
    public ResponseEntity<AssemblyControlOrderDTO> updateShippingNotes(
            @PathVariable Long id,
            @RequestParam String shippingNotes) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.updateShippingNotes(id, shippingNotes);
        return ResponseEntity.ok(order);
    }

    /**
     * Request parts for an assembly control order.
     * Creates a supply order to Parts Supply Warehouse.
     */
    @PostMapping("/{id}/request-parts")
    public ResponseEntity<SupplyOrderDTO> requestParts(
            @PathVariable Long id,
            @RequestBody RequestPartsRequest request) {
        SupplyOrderDTO supplyOrder = assemblyControlOrderService.requestSupplies(
                id,
                request.getRequiredParts(),
                request.getNeededBy(),
                request.getNotes()
        );
        return ResponseEntity.ok(supplyOrder);
    }

    /**
     * Dispatch assembly control order to workstation.
     * Requires supply order to be fulfilled first (if one exists).
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<AssemblyControlOrderDTO> dispatchToWorkstation(@PathVariable Long id) {
        AssemblyControlOrderDTO order = assemblyControlOrderService.dispatchToWorkstation(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Get supply orders for an assembly control order.
     */
    @GetMapping("/{id}/supply-orders")
    public ResponseEntity<List<SupplyOrderDTO>> getSupplyOrders(@PathVariable Long id) {
        List<SupplyOrderDTO> supplyOrders = assemblyControlOrderService.getSupplyOrders(id);
        return ResponseEntity.ok(supplyOrders);
    }

    /**
     * Create an assembly control order from SimAL schedule data.
     * This endpoint is called by the SimAL integration service when a production
     * order has been scheduled and assigned to an assembly workstation.
     */
    @PostMapping
    public ResponseEntity<AssemblyControlOrderDTO> createControlOrder(
            @RequestBody AssemblyControlOrderCreateRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime targetStart = LocalDateTime.parse(request.getTargetStartTime(), formatter);
        LocalDateTime targetCompletion = LocalDateTime.parse(request.getTargetCompletionTime(), formatter);

        AssemblyControlOrderDTO order = assemblyControlOrderService.createControlOrder(
                request.getSourceProductionOrderId(),
                request.getAssignedWorkstationId(),
                request.getSimalScheduleId(),
                request.getPriority(),
                targetStart,
                targetCompletion,
                request.getAssemblyInstructions(),
                request.getQualityCheckpoints(),
                "Standard testing procedures apply",
                "Standard packaging requirements",
                90,  // Default 90-minute estimate
                request.getItemId(),
                request.getItemType(),
                request.getQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
