package io.life.order.controller;

import io.life.order.dto.ProductionControlOrderDTO;
import io.life.order.service.ProductionControlOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Part Finishing Station (Manufacturing Workstation).
 * Handles production control orders assigned to part finishing and quality control.
 * These orders involve final finishing, polishing, and quality inspection.
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
@RequestMapping("/api/manufacturing/part-finishing")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PartFinishingStationController 
        extends AbstractWorkstationController<ProductionControlOrderDTO, ProductionControlOrderService> {

    public PartFinishingStationController(ProductionControlOrderService productionControlOrderService) {
        super(productionControlOrderService);
    }

    // All standard workstation endpoints are inherited from AbstractWorkstationController.
    // Add any part finishing-specific endpoints below if needed.
}
