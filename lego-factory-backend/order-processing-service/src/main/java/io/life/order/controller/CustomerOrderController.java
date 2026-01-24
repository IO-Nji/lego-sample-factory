package io.life.order.controller;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.service.CustomerOrderService;
import io.life.order.service.FulfillmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer-orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final FulfillmentService fulfillmentService;

    public CustomerOrderController(CustomerOrderService customerOrderService, FulfillmentService fulfillmentService) {
        this.customerOrderService = customerOrderService;
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping
    public ResponseEntity<CustomerOrderDTO> createOrder(@RequestBody CustomerOrderDTO orderDTO) {
        CustomerOrderDTO createdOrder = customerOrderService.createOrder(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping
    public ResponseEntity<List<CustomerOrderDTO>> getAllOrders() {
        List<CustomerOrderDTO> orders = customerOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOrderDTO> getOrderById(@PathVariable Long id) {
        Optional<CustomerOrderDTO> order = customerOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<CustomerOrderDTO> getOrderByNumber(@PathVariable String orderNumber) {
        Optional<CustomerOrderDTO> order = customerOrderService.getOrderByNumber(orderNumber);
        return order.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<CustomerOrderDTO>> getOrdersByWorkstationId(@PathVariable Long workstationId) {
        List<CustomerOrderDTO> orders = customerOrderService.getOrdersByWorkstationId(workstationId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CustomerOrderDTO>> getOrdersByStatus(@PathVariable String status) {
        List<CustomerOrderDTO> orders = customerOrderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CustomerOrderDTO> updateOrderStatus(
        @PathVariable Long id,
        @RequestParam String status) {
        CustomerOrderDTO updatedOrder = customerOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        customerOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/fulfill")
    public ResponseEntity<CustomerOrderDTO> fulfillOrder(@PathVariable Long id) {
        CustomerOrderDTO fulfilledOrder = fulfillmentService.fulfillOrder(id);
        return ResponseEntity.ok(fulfilledOrder);
    }

    /**
     * GET /api/customer-orders/{id}/can-complete
     * Check if a customer order can be completed.
     * 
     * Returns true if:
     * 1. Order is in PROCESSING status
     * 2. All associated Final Assembly orders are SUBMITTED
     */
    @GetMapping("/{id}/can-complete")
    public ResponseEntity<Boolean> canComplete(@PathVariable Long id) {
        boolean canComplete = customerOrderService.canCompleteOrder(id);
        return ResponseEntity.ok(canComplete);
    }

    // --- Explicit status transition endpoints ---
    @PutMapping("/{id}/confirm")
    public ResponseEntity<CustomerOrderDTO> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.confirmOrder(id));
    }

    @PutMapping("/{id}/processing")
    public ResponseEntity<CustomerOrderDTO> markProcessing(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.markProcessing(id));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<CustomerOrderDTO> complete(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.completeOrder(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<CustomerOrderDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.cancelOrder(id));
    }
}
