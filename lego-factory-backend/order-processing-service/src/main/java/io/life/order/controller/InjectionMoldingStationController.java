package io.life.order.controller;

import io.life.order.dto.ProductionControlOrderDTO;
import io.life.order.service.ProductionControlOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Injection Molding Station (Manufacturing Workstation).
 * Handles production control orders assigned to injection molding equipment.
 * These are specialized production orders focused on plastic injection molding operations.
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
@RequestMapping("/api/manufacturing/injection-molding")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InjectionMoldingStationController 
        extends AbstractWorkstationController<ProductionControlOrderDTO, ProductionControlOrderService> {

    public InjectionMoldingStationController(ProductionControlOrderService productionControlOrderService) {
        super(productionControlOrderService);
    }

    // All standard workstation endpoints are inherited from AbstractWorkstationController.
    // Add any injection molding-specific endpoints below if needed.
}
