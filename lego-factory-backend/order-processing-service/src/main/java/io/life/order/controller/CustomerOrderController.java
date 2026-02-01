package io.life.order.controller;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.service.CustomerOrderService;
import io.life.order.service.FulfillmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer-orders")
@Tag(name = "Customer Orders", description = "WS-7 Plant Warehouse - Customer order management and fulfillment")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final FulfillmentService fulfillmentService;

    public CustomerOrderController(CustomerOrderService customerOrderService, FulfillmentService fulfillmentService) {
        this.customerOrderService = customerOrderService;
        this.fulfillmentService = fulfillmentService;
    }

    @Operation(summary = "Create a new customer order", description = "Creates a new customer order in PENDING status")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid order data")
    })
    @PostMapping
    public ResponseEntity<CustomerOrderDTO> createOrder(@RequestBody CustomerOrderDTO orderDTO) {
        // Customer orders are always created at Plant Warehouse (WS-7)
        // If workstationId is not provided, default to WS-7
        if (orderDTO.getWorkstationId() == null) {
            orderDTO.setWorkstationId(7L);
        }
        CustomerOrderDTO createdOrder = customerOrderService.createOrder(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @Operation(summary = "Get all customer orders", description = "Retrieves all customer orders in the system")
    @GetMapping
    public ResponseEntity<List<CustomerOrderDTO>> getAllOrders() {
        List<CustomerOrderDTO> orders = customerOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get customer order by ID", description = "Retrieves a specific customer order by its database ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CustomerOrderDTO> getOrderById(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        Optional<CustomerOrderDTO> order = customerOrderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get customer order by order number", description = "Retrieves a customer order by its human-readable order number (e.g., CO-001)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<CustomerOrderDTO> getOrderByNumber(
            @Parameter(description = "Order number (e.g., CO-001)") @PathVariable String orderNumber) {
        Optional<CustomerOrderDTO> order = customerOrderService.getOrderByNumber(orderNumber);
        return order.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get orders by workstation", description = "Retrieves all customer orders for a specific workstation (typically WS-7)")
    @GetMapping("/workstation/{workstationId}")
    public ResponseEntity<List<CustomerOrderDTO>> getOrdersByWorkstationId(
            @Parameter(description = "Workstation ID (1-9)") @PathVariable Long workstationId) {
        List<CustomerOrderDTO> orders = customerOrderService.getOrdersByWorkstationId(workstationId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by status", description = "Retrieves all customer orders with a specific status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CustomerOrderDTO>> getOrdersByStatus(
            @Parameter(description = "Order status (PENDING, CONFIRMED, PROCESSING, COMPLETED, CANCELLED)") @PathVariable String status) {
        List<CustomerOrderDTO> orders = customerOrderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Update order status", description = "Updates the status of a customer order")
    @PatchMapping("/{id}/status")
    public ResponseEntity<CustomerOrderDTO> updateOrderStatus(
            @Parameter(description = "Customer order ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam String status) {
        CustomerOrderDTO updatedOrder = customerOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @Operation(summary = "Delete customer order", description = "Deletes a customer order (only if in PENDING status)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "400", description = "Order cannot be deleted (not in PENDING status)")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        customerOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Fulfill customer order", 
               description = "Fulfills a customer order by debiting inventory from Plant Warehouse (WS-7). " +
                           "Only works if triggerScenario is DIRECT_FULFILLMENT and sufficient stock exists.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order fulfilled successfully"),
        @ApiResponse(responseCode = "400", description = "Insufficient stock or wrong trigger scenario"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{id}/fulfill")
    public ResponseEntity<CustomerOrderDTO> fulfillOrder(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        CustomerOrderDTO fulfilledOrder = fulfillmentService.fulfillOrder(id);
        return ResponseEntity.ok(fulfilledOrder);
    }

    @Operation(summary = "Check if order can be completed",
               description = "Returns true if the order is in PROCESSING status and all associated Final Assembly orders are SUBMITTED")
    @GetMapping("/{id}/can-complete")
    public ResponseEntity<Boolean> canComplete(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        boolean canComplete = customerOrderService.canCompleteOrder(id);
        return ResponseEntity.ok(canComplete);
    }

    @Operation(summary = "Get current trigger scenario",
               description = "Dynamically checks real-time stock levels to determine if DIRECT_FULFILLMENT or WAREHOUSE_ORDER_NEEDED")
    @GetMapping("/{id}/current-scenario")
    public ResponseEntity<Map<String, String>> getCurrentScenario(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        String scenario = customerOrderService.checkCurrentTriggerScenario(id);
        return ResponseEntity.ok(Map.of("triggerScenario", scenario));
    }

    @Operation(summary = "Confirm customer order",
               description = "Confirms a PENDING order, checks stock levels, and sets triggerScenario (DIRECT_FULFILLMENT or WAREHOUSE_ORDER_NEEDED)")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<CustomerOrderDTO> confirm(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.confirmOrder(id));
    }

    @Operation(summary = "Mark order as processing",
               description = "Marks a CONFIRMED order as PROCESSING (typically after warehouse order is created)")
    @PutMapping("/{id}/processing")
    public ResponseEntity<CustomerOrderDTO> markProcessing(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.markProcessing(id));
    }

    @Operation(summary = "Complete customer order",
               description = "Completes a PROCESSING order after all fulfillment activities are done")
    @PostMapping("/{id}/complete")
    public ResponseEntity<CustomerOrderDTO> complete(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.completeOrder(id));
    }

    @Operation(summary = "Cancel customer order",
               description = "Cancels a customer order (only if not yet COMPLETED)")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CustomerOrderDTO> cancel(
            @Parameter(description = "Customer order ID") @PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.cancelOrder(id));
    }
}
