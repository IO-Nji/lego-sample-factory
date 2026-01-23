package io.life.order.controller;

import io.life.order.dto.WarehouseOrderDTO;
import io.life.order.service.WarehouseOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/warehouse-orders")
public class WarehouseOrderController {

    private final WarehouseOrderService warehouseOrderService;

    public WarehouseOrderController(WarehouseOrderService warehouseOrderService) {
        this.warehouseOrderService = warehouseOrderService;
    }

    /**
     * GET /api/warehouse-orders
     * Retrieve all warehouse orders (admin only)
     */
    @GetMapping
    public ResponseEntity<List<WarehouseOrderDTO>> getAllWarehouseOrders() {
        List<WarehouseOrderDTO> orders = warehouseOrderService.getAllWarehouseOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/warehouse-orders/{id}
     * Retrieve a specific warehouse order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseOrderDTO> getWarehouseOrderById(@PathVariable Long id) {
        Optional<WarehouseOrderDTO> order = warehouseOrderService.getWarehouseOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/warehouse-orders/workstation/{workstationId}
     * Retrieve warehouse orders for a specific workstation
     * Modules Supermarket (WS-8) can retrieve orders that need to be fulfilled
     */
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<WarehouseOrderDTO>> getWarehouseOrdersByWorkstationId(@PathVariable Long workstationId) {
        List<WarehouseOrderDTO> orders = warehouseOrderService.getWarehouseOrdersByWorkstationId(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/warehouse-orders/customer-order/{customerOrderId}
     * Retrieve warehouse orders spawned from a specific customer order
     */
    @GetMapping("/customer-order/{customerOrderId}")
    public ResponseEntity<List<WarehouseOrderDTO>> getWarehouseOrdersByCustomerOrderId(@PathVariable Long customerOrderId) {
        List<WarehouseOrderDTO> orders = warehouseOrderService.getWarehouseOrdersByCustomerOrderId(customerOrderId);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/warehouse-orders/status/{status}
     * Retrieve warehouse orders by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<WarehouseOrderDTO>> getWarehouseOrdersByStatus(@PathVariable String status) {
        List<WarehouseOrderDTO> orders = warehouseOrderService.getWarehouseOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * PUT /api/warehouse-orders/{id}/confirm
     * Confirm a warehouse order (Scenario 2 critical endpoint)
     * DURING confirmation:
     * - Checks MODULE stock at Modules Supermarket (WS-8)
     * - Sets triggerScenario: DIRECT_FULFILLMENT or PRODUCTION_REQUIRED
     * - Changes status from PENDING to CONFIRMED
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<WarehouseOrderDTO> confirmWarehouseOrder(@PathVariable Long id) {
        WarehouseOrderDTO confirmedOrder = warehouseOrderService.confirmWarehouseOrder(id);
        return ResponseEntity.ok(confirmedOrder);
    }

    /**
     * POST /api/warehouse-orders/{id}/fulfill
     * Fulfill a warehouse order (Scenario 2 DIRECT_FULFILLMENT path)
     * Prerequisites:
     * - Order must be CONFIRMED with triggerScenario = DIRECT_FULFILLMENT
     * Actions:
     * - Debits module stock from Modules Supermarket (WS-8)
     * - Creates Final Assembly orders at WS-6
     * - Updates status to PROCESSING (awaiting assembly completion)
     */
    @PostMapping("/{id}/fulfill")
    public ResponseEntity<WarehouseOrderDTO> fulfillWarehouseOrder(@PathVariable Long id) {
        WarehouseOrderDTO fulfilledOrder = warehouseOrderService.fulfillWarehouseOrder(id);
        return ResponseEntity.ok(fulfilledOrder);
    }

    /**
     * PATCH /api/warehouse-orders/{id}/status
     * Update warehouse order status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<WarehouseOrderDTO> updateWarehouseOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        WarehouseOrderDTO updatedOrder = warehouseOrderService.updateWarehouseOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }
}
