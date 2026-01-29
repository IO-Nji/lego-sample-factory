package io.life.order.controller;

import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.service.AssemblyControlOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Final Assembly Station (Assembly Workstation).
 * Handles assembly control orders assigned to final assembly and product completion.
 * This is the final assembly stage before products ship to customers.
 * 
 * Extends AbstractWorkstationController to inherit all standard workstation order endpoints,
 * but OVERRIDES the complete endpoint because Final Assembly credits Plant Warehouse
 * instead of Modules Supermarket.
 * 
 * Endpoints inherited from base class:
 * - GET /workstation/{workstationId} - Get all orders for workstation
 * - GET /workstation/{workstationId}/active - Get active orders
 * - GET /workstation/{workstationId}/unassigned - Get unassigned orders
 * - GET /{id} - Get order by ID
 * - GET /number/{orderNumber} - Get order by number
 * - POST /{id}/start - Start work on order
 * - POST /{id}/halt - Halt work on order
 * - PATCH /{id}/notes - Update operator notes
 * 
 * Overridden endpoint:
 * - PUT /{id}/complete - Complete order (credits Plant Warehouse, not Modules Supermarket)
 * 
 * Error handling: Exceptions propagate to GlobalExceptionHandler for consistent responses.
 */
@RestController
@RequestMapping("/api/assembly/final-assembly")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FinalAssemblyStationController 
        extends AbstractWorkstationController<AssemblyControlOrderDTO, AssemblyControlOrderService> {

    private final AssemblyControlOrderService assemblyControlOrderService;

    public FinalAssemblyStationController(AssemblyControlOrderService assemblyControlOrderService) {
        super(assemblyControlOrderService);
        this.assemblyControlOrderService = assemblyControlOrderService;
    }

    /**
     * Complete final assembly with SimAL and inventory integration.
     * OVERRIDES base class method because final assembly credits Plant Warehouse
     * instead of Modules Supermarket. This is special because final assembly 
     * completes the entire product and triggers customer order fulfillment.
     */
    @Override
    @PutMapping("/{id}/complete")
    public ResponseEntity<AssemblyControlOrderDTO> completeWork(@PathVariable Long id) {
        // Use special final assembly completion that credits Plant Warehouse
        AssemblyControlOrderDTO order = assemblyControlOrderService.completeFinalAssembly(id);
        return ResponseEntity.ok(order);
    }
}
