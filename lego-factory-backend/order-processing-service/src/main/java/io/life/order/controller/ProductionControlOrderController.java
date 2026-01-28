package io.life.order.controller;

import io.life.order.dto.ProductionControlOrderDTO;
import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.request.DefectsRequest;
import io.life.order.dto.request.HaltRequest;
import io.life.order.dto.request.NotesRequest;
import io.life.order.dto.request.ProductionControlOrderCreateRequest;
import io.life.order.dto.request.RequestPartsRequest;
import io.life.order.service.ProductionControlOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for ProductionControlOrder management.
 * Exposes endpoints for Production Control workstations to view and manage their assigned orders.
 */
@RestController
@RequestMapping("/api/production-control-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductionControlOrderController {

    private final ProductionControlOrderService productionControlOrderService;

    public ProductionControlOrderController(ProductionControlOrderService productionControlOrderService) {
        this.productionControlOrderService = productionControlOrderService;
    }

    /**
     * Get all control orders
     */
    @GetMapping
    public ResponseEntity<List<ProductionControlOrderDTO>> getAllOrders() {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get all control orders for a workstation
     */
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<ProductionControlOrderDTO>> getOrdersByWorkstation(
            @PathVariable Long workstationId) {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get active (in progress) control orders for a workstation
     */
    @GetMapping("/workstation/{workstationId}/active")
    public ResponseEntity<List<ProductionControlOrderDTO>> getActiveOrdersByWorkstation(
            @PathVariable Long workstationId) {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getActiveOrdersByWorkstation(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get unassigned (status=ASSIGNED) control orders for a workstation
     */
    @GetMapping("/workstation/{workstationId}/unassigned")
    public ResponseEntity<List<ProductionControlOrderDTO>> getUnassignedOrders(
            @PathVariable Long workstationId) {
        List<ProductionControlOrderDTO> orders = productionControlOrderService.getUnassignedOrders(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get control order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductionControlOrderDTO> getOrderById(@PathVariable Long id) {
        Optional<ProductionControlOrderDTO> order = productionControlOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get control order by control order number
     */
    @GetMapping("/number/{controlOrderNumber}")
    public ResponseEntity<ProductionControlOrderDTO> getOrderByNumber(
            @PathVariable String controlOrderNumber) {
        Optional<ProductionControlOrderDTO> order = productionControlOrderService.getOrderByNumber(controlOrderNumber);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Start production on a control order
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<ProductionControlOrderDTO> startProduction(@PathVariable Long id) {
        try {
            ProductionControlOrderDTO order = productionControlOrderService.startProduction(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Complete production on a control order
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ProductionControlOrderDTO> completeProduction(@PathVariable Long id) {
        try {
            ProductionControlOrderDTO order = productionControlOrderService.completeProduction(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Halt production on a control order
     */
    @PostMapping("/{id}/halt")
    public ResponseEntity<ProductionControlOrderDTO> haltProduction(
            @PathVariable Long id,
            @RequestBody HaltRequest request) {
        try {
            ProductionControlOrderDTO order = productionControlOrderService.haltProduction(id, request.getReason());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update operator notes
     */
    @PatchMapping("/{id}/notes")
    public ResponseEntity<ProductionControlOrderDTO> updateNotes(
            @PathVariable Long id,
            @RequestBody NotesRequest request) {
        try {
            ProductionControlOrderDTO order = productionControlOrderService.updateOperatorNotes(id, request.getNotes());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Request parts for a production control order.
     * Creates a supply order to Parts Supply Warehouse.
     */
    @PostMapping("/{id}/request-parts")
    public ResponseEntity<SupplyOrderDTO> requestParts(
            @PathVariable Long id,
            @RequestBody RequestPartsRequest request) {
        try {
            SupplyOrderDTO supplyOrder = productionControlOrderService.requestSupplies(
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
     * Dispatch production control order to workstation.
     * Requires supply order to be fulfilled first (if one exists).
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<ProductionControlOrderDTO> dispatchToWorkstation(@PathVariable Long id) {
        try {
            ProductionControlOrderDTO order = productionControlOrderService.dispatchToWorkstation(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get supply orders for a production control order.
     */
    @GetMapping("/{id}/supply-orders")
    public ResponseEntity<List<SupplyOrderDTO>> getSupplyOrders(@PathVariable Long id) {
        List<SupplyOrderDTO> supplyOrders = productionControlOrderService.getSupplyOrders(id);
        return ResponseEntity.ok(supplyOrders);
    }

    /**
     * Update defect information
     */
    @PatchMapping("/{id}/defects")
    public ResponseEntity<ProductionControlOrderDTO> updateDefects(
            @PathVariable Long id,
            @RequestBody DefectsRequest request) {
        try {
            ProductionControlOrderDTO order = productionControlOrderService.updateDefects(
                    id,
                    request.getDefectsFound(),
                    request.getDefectsReworked(),
                    request.getReworkRequired()
            );
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Create a production control order from SimAL schedule data.
     * This endpoint is called by the SimAL integration service when a production
     * order has been scheduled and assigned to a production workstation.
     */
    @PostMapping
    public ResponseEntity<ProductionControlOrderDTO> createControlOrder(
            @RequestBody ProductionControlOrderCreateRequest request) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime targetStart = LocalDateTime.parse(request.getTargetStartTime(), formatter);
            LocalDateTime targetCompletion = LocalDateTime.parse(request.getTargetCompletionTime(), formatter);

            ProductionControlOrderDTO order = productionControlOrderService.createControlOrder(
                    request.getSourceProductionOrderId(),
                    request.getAssignedWorkstationId(),
                    request.getSimalScheduleId(),
                    request.getPriority(),
                    targetStart,
                    targetCompletion,
                    request.getProductionInstructions(),
                    request.getQualityCheckpoints(),
                    "Standard safety procedures apply",
                    120,  // Default 2-hour estimate
                    request.getItemId(),
                    request.getItemType(),
                    request.getQuantity()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
