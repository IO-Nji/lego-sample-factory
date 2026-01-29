package io.life.order.controller;

import io.life.order.dto.ProductionControlOrderDTO;
import io.life.order.service.ProductionControlOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Parts Pre-Production Station (Manufacturing Workstation).
 * Handles production control orders assigned to parts preparation and assembly.
 * These orders involve parts pre-processing before final finishing.
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
@RequestMapping("/api/manufacturing/parts-pre-production")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PartsPreProductionStationController 
        extends AbstractWorkstationController<ProductionControlOrderDTO, ProductionControlOrderService> {

    public PartsPreProductionStationController(ProductionControlOrderService productionControlOrderService) {
        super(productionControlOrderService);
    }

    // All standard workstation endpoints are inherited from AbstractWorkstationController.
    // Add any parts pre-production specific endpoints below if needed.
}
