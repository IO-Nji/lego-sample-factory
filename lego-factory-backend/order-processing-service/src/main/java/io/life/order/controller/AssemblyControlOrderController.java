package io.life.order.controller;

import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.SupplyOrderItemDTO;
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
     * Start assembly on a control order
     */
    @PutMapping("/{id}/start")
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
    @PutMapping("/{id}/complete")
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
        try {
            SupplyOrderDTO supplyOrder = assemblyControlOrderService.requestSupplies(
                    id,
                    request.getRequiredParts(),
                    request.getNeededBy(),
                    request.getNotes()
            );
            return ResponseEntity.ok(supplyOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Dispatch assembly control order to workstation.
     * Requires supply order to be fulfilled first (if one exists).
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<AssemblyControlOrderDTO> dispatchToWorkstation(@PathVariable Long id) {
        try {
            AssemblyControlOrderDTO order = assemblyControlOrderService.dispatchToWorkstation(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
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
            @RequestBody CreateControlOrderRequest request) {
        try {
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
                    90  // Default 90-minute estimate
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Request class for creating an assembly control order from SimAL
     */
    public static class CreateControlOrderRequest {
        private Long sourceProductionOrderId;
        private Long assignedWorkstationId;
        private String simalScheduleId;
        private String targetStartTime;
        private String targetCompletionTime;
        private String priority;
        private String assemblyInstructions;
        private String qualityCheckpoints;

        // Getters and setters
        public Long getSourceProductionOrderId() { return sourceProductionOrderId; }
        public void setSourceProductionOrderId(Long sourceProductionOrderId) {
            this.sourceProductionOrderId = sourceProductionOrderId;
        }

        public Long getAssignedWorkstationId() { return assignedWorkstationId; }
        public void setAssignedWorkstationId(Long assignedWorkstationId) {
            this.assignedWorkstationId = assignedWorkstationId;
        }

        public String getSimalScheduleId() { return simalScheduleId; }
        public void setSimalScheduleId(String simalScheduleId) { this.simalScheduleId = simalScheduleId; }

        public String getTargetStartTime() { return targetStartTime; }
        public void setTargetStartTime(String targetStartTime) { this.targetStartTime = targetStartTime; }

        public String getTargetCompletionTime() { return targetCompletionTime; }
        public void setTargetCompletionTime(String targetCompletionTime) {
            this.targetCompletionTime = targetCompletionTime;
        }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getAssemblyInstructions() { return assemblyInstructions; }
        public void setAssemblyInstructions(String assemblyInstructions) {
            this.assemblyInstructions = assemblyInstructions;
        }

        public String getQualityCheckpoints() { return qualityCheckpoints; }
        public void setQualityCheckpoints(String qualityCheckpoints) {
            this.qualityCheckpoints = qualityCheckpoints;
        }
    }

    // Request DTO for requesting parts
    public static class RequestPartsRequest {
        private List<SupplyOrderItemDTO> requiredParts;
        private LocalDateTime neededBy;
        private String notes;

        public List<SupplyOrderItemDTO> getRequiredParts() { return requiredParts; }
        public void setRequiredParts(List<SupplyOrderItemDTO> requiredParts) {
            this.requiredParts = requiredParts;
        }

        public LocalDateTime getNeededBy() { return neededBy; }
        public void setNeededBy(LocalDateTime neededBy) { this.neededBy = neededBy; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
