package io.life.order.controller;

import io.life.order.dto.ProductionOrderDTO;
import io.life.order.service.ProductionPlanningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Production Planning operations.
 * Coordinates production order submission to SimAL and tracks production progress.
 */
@RestController
@RequestMapping("/api/production-planning")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductionPlanningController {

    private final ProductionPlanningService productionPlanningService;

    public ProductionPlanningController(ProductionPlanningService productionPlanningService) {
        this.productionPlanningService = productionPlanningService;
    }

    /**
     * Submit a production order to SimAL for scheduling
     */
    @PostMapping("/{productionOrderId}/submit-to-simal")
    public ResponseEntity<ProductionOrderDTO> submitToSimal(@PathVariable Long productionOrderId) {
        try {
            ProductionOrderDTO order = productionPlanningService.submitProductionOrderToSimal(productionOrderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get scheduled tasks for a production order
     */
    @GetMapping("/scheduled-tasks/{simalScheduleId}")
    public ResponseEntity<List<Map<String, Object>>> getScheduledTasks(@PathVariable String simalScheduleId) {
        List<Map<String, Object>> tasks = productionPlanningService.getScheduledTasks(simalScheduleId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Update production progress (sync with SimAL)
     */
    @PostMapping("/{productionOrderId}/update-progress")
    public ResponseEntity<ProductionOrderDTO> updateProgress(@PathVariable Long productionOrderId) {
        ProductionOrderDTO order = productionPlanningService.updateProductionProgress(productionOrderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Dispatch production - creates control orders and sends to workstations.
     * Replaces startProduction - Production Planning dispatches, workstations start.
     * POST /api/production-planning/{id}/dispatch
     */
    @PostMapping("/{productionOrderId}/dispatch")
    public ResponseEntity<ProductionOrderDTO> dispatchProduction(@PathVariable Long productionOrderId) {
        try {
            ProductionOrderDTO order = productionPlanningService.dispatchProduction(productionOrderId);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @deprecated Use dispatchProduction instead. Kept for backward compatibility.
     * Start production in SimAL
     */
    @Deprecated
    @PostMapping("/{productionOrderId}/start")
    public ResponseEntity<ProductionOrderDTO> startProduction(@PathVariable Long productionOrderId) {
        // Forward to dispatch endpoint
        return dispatchProduction(productionOrderId);
    }

    /**
     * Complete production in SimAL
     */
    @PostMapping("/{productionOrderId}/complete")
    public ResponseEntity<ProductionOrderDTO> completeProduction(@PathVariable Long productionOrderId) {
        try {
            ProductionOrderDTO order = productionPlanningService.completeProduction(productionOrderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
