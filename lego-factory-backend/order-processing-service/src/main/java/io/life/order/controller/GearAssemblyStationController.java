package io.life.order.controller;

import io.life.order.dto.AssemblyControlOrderDTO;
import io.life.order.service.AssemblyControlOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Gear Assembly Station (Assembly Workstation).
 * Handles assembly control orders assigned to gear assembly operations.
 * Specializes in gear component assembly for drill products.
 * 
 * Extends AbstractWorkstationController to inherit all standard workstation order endpoints.
 * All common CRUD and workflow operations are handled by the base class.
 * 
 * Endpoints inherited from base class:
 * - GET /workstation/{workstationId} - Get all orders for workstation
 * - GET /workstation/{workstationId}/active - Get active orders
 * - GET /workstation/{workstationId}/unassigned - Get unassigned orders
 * - GET /{id} - Get order by ID
 * - GET /number/{orderNumber} - Get order by number
 * - POST /{id}/start - Start work on order
 * - PUT /{id}/complete - Complete order (with inventory/SimAL integration)
 * - POST /{id}/halt - Halt work on order
 * - PATCH /{id}/notes - Update operator notes
 */
@RestController
@RequestMapping("/api/assembly/gear-assembly")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GearAssemblyStationController 
        extends AbstractWorkstationController<AssemblyControlOrderDTO, AssemblyControlOrderService> {

    public GearAssemblyStationController(AssemblyControlOrderService assemblyControlOrderService) {
        super(assemblyControlOrderService);
    }

    // All standard workstation endpoints are inherited from AbstractWorkstationController.
    // Add any gear assembly-specific endpoints below if needed.
}
