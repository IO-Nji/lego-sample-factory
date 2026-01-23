package io.life.order.controller;

import io.life.order.dto.FinalAssemblyOrderDTO;
import io.life.order.service.FinalAssemblyOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/final-assembly-orders")
public class FinalAssemblyOrderController {

    private final FinalAssemblyOrderService finalAssemblyOrderService;

    public FinalAssemblyOrderController(FinalAssemblyOrderService finalAssemblyOrderService) {
        this.finalAssemblyOrderService = finalAssemblyOrderService;
    }

    /**
     * GET /api/final-assembly-orders
     * Retrieve all Final Assembly orders
     */
    @GetMapping
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getAllOrders() {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/final-assembly-orders/{id}
     * Retrieve a specific Final Assembly order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<FinalAssemblyOrderDTO> getOrderById(@PathVariable Long id) {
        Optional<FinalAssemblyOrderDTO> order = finalAssemblyOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/final-assembly-orders/workstation/{workstationId}
     * Retrieve Final Assembly orders for a specific workstation (typically WS-6)
     */
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getOrdersByWorkstationId(@PathVariable Long workstationId) {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getOrdersByWorkstationId(workstationId);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/final-assembly-orders/status/{status}
     * Retrieve Final Assembly orders by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getOrdersByStatus(@PathVariable String status) {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/final-assembly-orders/warehouse-order/{warehouseOrderId}
     * Retrieve Final Assembly orders created from a specific warehouse order
     */
    @GetMapping("/warehouse-order/{warehouseOrderId}")
    public ResponseEntity<List<FinalAssemblyOrderDTO>> getOrdersByWarehouseOrderId(@PathVariable Long warehouseOrderId) {
        List<FinalAssemblyOrderDTO> orders = finalAssemblyOrderService.getOrdersByWarehouseOrderId(warehouseOrderId);
        return ResponseEntity.ok(orders);
    }

    /**
     * POST /api/final-assembly-orders/{id}/start
     * Start a Final Assembly order
     * Changes status from PENDING to IN_PROGRESS
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<FinalAssemblyOrderDTO> startOrder(@PathVariable Long id) {
        FinalAssemblyOrderDTO startedOrder = finalAssemblyOrderService.startOrder(id);
        return ResponseEntity.ok(startedOrder);
    }

    /**
     * POST /api/final-assembly-orders/{id}/complete
     * Complete a Final Assembly order (Scenario 2 critical endpoint)
     * Prerequisites:
     * - Order must be IN_PROGRESS
     * Actions:
     * - Credits Plant Warehouse (WS-7) with finished products
     * - Updates order status to COMPLETED
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<FinalAssemblyOrderDTO> completeOrder(@PathVariable Long id) {
        FinalAssemblyOrderDTO completedOrder = finalAssemblyOrderService.completeOrder(id);
        return ResponseEntity.ok(completedOrder);
    }

    /**
     * PATCH /api/final-assembly-orders/{id}/status
     * Update Final Assembly order status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<FinalAssemblyOrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        FinalAssemblyOrderDTO updatedOrder = finalAssemblyOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }
}
