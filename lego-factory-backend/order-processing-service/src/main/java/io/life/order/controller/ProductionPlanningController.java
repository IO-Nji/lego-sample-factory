package io.life.order.controller;

import io.life.order.dto.ProductionOrderDTO;
import io.life.order.service.ProductionOrderService;
import io.life.order.service.ProductionPlanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Production Planning operations.
 * Coordinates production order submission to SimAL and tracks production progress.
 * 
 * Error handling: Exceptions propagate to GlobalExceptionHandler for consistent responses.
 */
@RestController
@RequestMapping("/api/production-planning")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductionPlanningController {

    private final ProductionPlanningService productionPlanningService;
    private final ProductionOrderService productionOrderService;

    public ProductionPlanningController(ProductionPlanningService productionPlanningService,
                                        ProductionOrderService productionOrderService) {
        this.productionPlanningService = productionPlanningService;
        this.productionOrderService = productionOrderService;
    }

    /**
     * Confirm a production order (CREATED -> CONFIRMED).
     * Must be done before scheduling.
     */
    @PutMapping("/{productionOrderId}/confirm")
    public ResponseEntity<ProductionOrderDTO> confirmProductionOrder(@PathVariable Long productionOrderId) {
        ProductionOrderDTO order = productionOrderService.confirmProductionOrder(productionOrderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Submit a production order to SimAL for scheduling
     */
    @PostMapping("/{productionOrderId}/submit-to-simal")
    public ResponseEntity<ProductionOrderDTO> submitToSimal(@PathVariable Long productionOrderId) {
        ProductionOrderDTO order = productionPlanningService.submitProductionOrderToSimal(productionOrderId);
        return ResponseEntity.ok(order);
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
        ProductionOrderDTO order = productionPlanningService.dispatchProduction(productionOrderId);
        return ResponseEntity.ok(order);
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
        ProductionOrderDTO order = productionPlanningService.completeProduction(productionOrderId);
        return ResponseEntity.ok(order);
    }
    
    /**
     * Submit a completed production order for Final Assembly creation (Scenario 4 workflow).
     * This is a MANUAL step performed by Production Planning AFTER production completes.
     * Creates Final Assembly orders for the products in the original customer order.
     * 
     * POST /api/production-planning/{id}/submit-for-final-assembly
     */
    @PostMapping("/{productionOrderId}/submit-for-final-assembly")
    public ResponseEntity<ProductionOrderDTO> submitForFinalAssembly(@PathVariable Long productionOrderId) {
        ProductionOrderDTO order = productionPlanningService.submitForFinalAssembly(productionOrderId);
        return ResponseEntity.ok(order);
    }
}
