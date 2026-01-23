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

    /**
     * POST /api/customer-orders/{id}/create-warehouse-order
     * Create warehouse order from customer order (Scenario 2 integration point)
     * Prerequisites:
     * - Order must be CONFIRMED with triggerScenario = WAREHOUSE_ORDER_NEEDED
     * Actions:
     * - Creates WarehouseOrder at Modules Supermarket (WS-8)
     * - Updates customer order status to PROCESSING
     */
    @PostMapping("/{id}/create-warehouse-order")
    public ResponseEntity<CustomerOrderDTO> createWarehouseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.createWarehouseOrderFromCustomerOrder(id));
    }
}
