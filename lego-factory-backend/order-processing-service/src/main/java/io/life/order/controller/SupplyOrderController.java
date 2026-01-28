package io.life.order.controller;

import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.SupplyOrderItemDTO;
import io.life.order.service.SupplyOrderService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for Supply Order management.
 * Handles supply orders for Parts Supply Warehouse.
 */
@RestController
@RequestMapping("/api/supply-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SupplyOrderController {

    private static final Logger logger = LoggerFactory.getLogger(SupplyOrderController.class);
    private final SupplyOrderService supplyOrderService;

    public SupplyOrderController(SupplyOrderService supplyOrderService) {
        this.supplyOrderService = supplyOrderService;
    }

    /**
     * Request object for creating a new supply order.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateSupplyOrderRequest {
        private Long sourceControlOrderId;
        private String sourceControlOrderType; // PRODUCTION or ASSEMBLY
        private Long requestingWorkstationId;
        private String priority;
        private LocalDateTime requestedByTime;
        private List<SupplyOrderItemDTO> requiredItems;
        private String notes;
    }

    /**
     * Create a new supply order.
     * Called by production/assembly control operators to request parts from warehouse.
     */
    @PostMapping
    public ResponseEntity<SupplyOrderDTO> createSupplyOrder(@RequestBody CreateSupplyOrderRequest request) {
        SupplyOrderDTO order = supplyOrderService.createSupplyOrder(
                request.getSourceControlOrderId(),
                request.getSourceControlOrderType(),
                request.getRequestingWorkstationId(),
                request.getPriority(),
                request.getRequestedByTime(),
                request.getRequiredItems(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Request object for creating a supply order from a control order.
     * Parts are automatically determined from BOM.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFromControlOrderRequest {
        private Long controlOrderId;
        private String controlOrderType; // ASSEMBLY or PRODUCTION
        private String priority;
    }

    /**
     * Create a supply order from a control order with automatic BOM lookup.
     * Parts needed are automatically determined from the module's BOM.
     */
    @PostMapping("/from-control-order")
    public ResponseEntity<?> createFromControlOrder(@RequestBody CreateFromControlOrderRequest request) {
        logger.info("Creating supply order from control order: controlOrderId={}, type={}, priority={}",
            request.getControlOrderId(), request.getControlOrderType(), request.getPriority());
        try {
            SupplyOrderDTO order = supplyOrderService.createSupplyOrderFromControlOrder(
                    request.getControlOrderId(),
                    request.getControlOrderType(),
                    request.getPriority()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException creating supply order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException creating supply order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Exception creating supply order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all supply orders for the Parts Supply Warehouse dashboard.
     * Optional status filter: PENDING, IN_PROGRESS, FULFILLED, REJECTED, CANCELLED
     */
    @GetMapping("/warehouse")
    public ResponseEntity<List<SupplyOrderDTO>> getWarehouseOrders(
            @RequestParam(required = false) String status) {
        List<SupplyOrderDTO> orders = supplyOrderService.getOrdersForSupplyWarehouse(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get all supply orders for a specific workstation (requesting workstation).
     * Used by production/assembly control operators to see their pending supplies.
     */
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<SupplyOrderDTO>> getOrdersByWorkstation(
            @PathVariable Long workstationId,
            @RequestParam(required = false) String status) {
        List<SupplyOrderDTO> orders = supplyOrderService.getOrdersByRequestingWorkstation(workstationId, status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get a specific supply order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupplyOrderDTO> getSupplyOrder(@PathVariable Long id) {
        SupplyOrderDTO order = supplyOrderService.getSupplyOrder(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Get supply orders for a specific source control order.
     */
    @GetMapping("/source/{controlOrderId}")
    public ResponseEntity<List<SupplyOrderDTO>> getBySourceControlOrder(
            @PathVariable Long controlOrderId,
            @RequestParam String type) { // PRODUCTION or ASSEMBLY
        List<SupplyOrderDTO> orders = supplyOrderService.getBySourceControlOrder(controlOrderId, type);
        return ResponseEntity.ok(orders);
    }

    /**
     * Fulfill a supply order.
     * Changes status from PENDING/IN_PROGRESS to FULFILLED.
     * Debits from inventory-service.
     */
    @PutMapping("/{id}/fulfill")
    public ResponseEntity<SupplyOrderDTO> fulfillSupplyOrder(@PathVariable Long id) {
        SupplyOrderDTO order = supplyOrderService.fulfillSupplyOrder(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Update supply order status.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SupplyOrderDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        SupplyOrderDTO order = supplyOrderService.updateStatus(id, newStatus);
        return ResponseEntity.ok(order);
    }

    /**
     * Reject a supply order due to insufficient stock or other reason.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<SupplyOrderDTO> rejectSupplyOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        SupplyOrderDTO order = supplyOrderService.rejectSupplyOrder(id, reason);
        return ResponseEntity.ok(order);
    }

    /**
     * Cancel a supply order.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<SupplyOrderDTO> cancelSupplyOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        SupplyOrderDTO order = supplyOrderService.cancelSupplyOrder(id, reason);
        return ResponseEntity.ok(order);
    }
}
