package io.life.order.controller;

import io.life.order.config.OrderProcessingConfig;
import io.life.order.dto.CustomerOrderDTO;
import io.life.order.dto.OrderItemDTO;
import io.life.order.service.CustomerOrderService;
import io.life.order.service.FulfillmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CustomerOrderController
 * Tests REST API endpoints for customer order management
 */
@WebMvcTest(CustomerOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class CustomerOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerOrderService customerOrderService;

    @MockBean
    private FulfillmentService fulfillmentService;

    @MockBean
    private OrderProcessingConfig orderProcessingConfig;

    private CustomerOrderDTO testOrderDTO;

    @BeforeEach
    void setUp() {
        // Setup config mock for controller that needs workstation IDs
        OrderProcessingConfig.Workstations workstations = new OrderProcessingConfig.Workstations();
        workstations.setPlantWarehouse(7L);
        when(orderProcessingConfig.getWorkstations()).thenReturn(workstations);
        
        testOrderDTO = new CustomerOrderDTO();
        testOrderDTO.setId(1L);
        testOrderDTO.setOrderNumber("ORD-TEST001");
        testOrderDTO.setOrderDate(LocalDateTime.now());
        testOrderDTO.setStatus("PENDING");
        testOrderDTO.setWorkstationId(7L);
        testOrderDTO.setNotes("Test order");

        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setId(1L);
        itemDTO.setItemType("PRODUCT");
        itemDTO.setItemId(1L);
        itemDTO.setQuantity(5);
        itemDTO.setFulfilledQuantity(0);

        List<OrderItemDTO> items = new ArrayList<>();
        items.add(itemDTO);
        testOrderDTO.setOrderItems(items);
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        // Given
        when(customerOrderService.createOrder(any(CustomerOrderDTO.class)))
                .thenReturn(testOrderDTO);

        String requestBody = """
                {
                    "workstationId": 7,
                    "notes": "Test order",
                    "orderItems": [
                        {
                            "itemType": "PRODUCT",
                            "itemId": 1,
                            "quantity": 5
                        }
                    ]
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/customer-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.orderNumber", is("ORD-TEST001")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.workstationId", is(7)))
                .andExpect(jsonPath("$.orderItems", hasSize(1)));

        verify(customerOrderService, times(1)).createOrder(any(CustomerOrderDTO.class));
    }

    @Test
    void testCreateOrder_InvalidRequest() throws Exception {
        // Given - Service throws exception for invalid data
        when(customerOrderService.createOrder(any(CustomerOrderDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid order data"));

        String invalidRequestBody = """
                {
                    "notes": "Test order"
                }
                """;

        // When/Then - IllegalArgumentException should return 400 Bad Request
        mockMvc.perform(post("/api/customer-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllOrders_Success() throws Exception {
        // Given
        List<CustomerOrderDTO> orders = new ArrayList<>();
        orders.add(testOrderDTO);
        when(customerOrderService.getAllOrders()).thenReturn(orders);

        // When/Then
        mockMvc.perform(get("/api/customer-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderNumber", is("ORD-TEST001")));

        verify(customerOrderService, times(1)).getAllOrders();
    }

    @Test
    void testGetOrderById_Found() throws Exception {
        // Given
        when(customerOrderService.getOrderById(1L))
                .thenReturn(Optional.of(testOrderDTO));

        // When/Then
        mockMvc.perform(get("/api/customer-orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.orderNumber", is("ORD-TEST001")));

        verify(customerOrderService, times(1)).getOrderById(1L);
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        // Given
        when(customerOrderService.getOrderById(999L))
                .thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/customer-orders/999"))
                .andExpect(status().isNotFound());

        verify(customerOrderService, times(1)).getOrderById(999L);
    }

    @Test
    void testGetOrderByNumber_Found() throws Exception {
        // Given
        when(customerOrderService.getOrderByNumber("ORD-TEST001"))
                .thenReturn(Optional.of(testOrderDTO));

        // When/Then
        mockMvc.perform(get("/api/customer-orders/number/ORD-TEST001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber", is("ORD-TEST001")));

        verify(customerOrderService, times(1)).getOrderByNumber("ORD-TEST001");
    }

    @Test
    void testGetOrdersByWorkstation() throws Exception {
        // Given
        List<CustomerOrderDTO> orders = new ArrayList<>();
        orders.add(testOrderDTO);
        when(customerOrderService.getOrdersByWorkstationId(7L))
                .thenReturn(orders);

        // When/Then
        mockMvc.perform(get("/api/customer-orders/workstation/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].workstationId", is(7)));

        verify(customerOrderService, times(1)).getOrdersByWorkstationId(7L);
    }

    @Test
    void testGetOrdersByStatus() throws Exception {
        // Given
        List<CustomerOrderDTO> orders = new ArrayList<>();
        orders.add(testOrderDTO);
        when(customerOrderService.getOrdersByStatus("PENDING"))
                .thenReturn(orders);

        // When/Then
        mockMvc.perform(get("/api/customer-orders/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        verify(customerOrderService, times(1)).getOrdersByStatus("PENDING");
    }

    @Test
    void testConfirmOrder_Success() throws Exception {
        // Given
        testOrderDTO.setStatus("CONFIRMED");
        when(customerOrderService.confirmOrder(1L))
                .thenReturn(testOrderDTO);

        // When/Then
        mockMvc.perform(put("/api/customer-orders/1/confirm")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(customerOrderService, times(1)).confirmOrder(1L);
    }

    @Test
    void testConfirmOrder_InvalidTransition() throws Exception {
        // Given
        when(customerOrderService.confirmOrder(1L))
                .thenThrow(new IllegalStateException("Only PENDING orders can be confirmed"));

        // When/Then - IllegalStateException should return 400 Bad Request
        mockMvc.perform(put("/api/customer-orders/1/confirm")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(customerOrderService, times(1)).confirmOrder(1L);
    }

    @Test
    void testFulfillOrder_Success() throws Exception {
        // Given
        testOrderDTO.setStatus("COMPLETED");
        when(fulfillmentService.fulfillOrder(1L))
                .thenReturn(testOrderDTO);

        // When/Then
        mockMvc.perform(put("/api/customer-orders/1/fulfill")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(fulfillmentService, times(1)).fulfillOrder(1L);
    }

    @Test
    void testMarkProcessing_Success() throws Exception {
        // Given
        testOrderDTO.setStatus("PROCESSING");
        when(customerOrderService.markProcessing(1L))
                .thenReturn(testOrderDTO);

        // When/Then
        mockMvc.perform(put("/api/customer-orders/1/processing")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSING")));

        verify(customerOrderService, times(1)).markProcessing(1L);
    }

    @Test
    void testCompleteOrder_Success() throws Exception {
        // Given
        testOrderDTO.setStatus("COMPLETED");
        when(customerOrderService.completeOrder(1L))
                .thenReturn(testOrderDTO);

        // When/Then - Complete uses POST, not PUT
        mockMvc.perform(post("/api/customer-orders/1/complete")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(customerOrderService, times(1)).completeOrder(1L);
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        // Given
        testOrderDTO.setStatus("CANCELLED");
        when(customerOrderService.cancelOrder(1L))
                .thenReturn(testOrderDTO);

        // When/Then - Cancel uses POST, not PUT
        mockMvc.perform(post("/api/customer-orders/1/cancel")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        verify(customerOrderService, times(1)).cancelOrder(1L);
    }

    @Test
    void testCancelOrder_AlreadyCompleted() throws Exception {
        // Given
        when(customerOrderService.cancelOrder(1L))
                .thenThrow(new IllegalStateException("Completed orders cannot be cancelled"));

        // When/Then - Cancel uses POST, IllegalStateException returns 400
        mockMvc.perform(post("/api/customer-orders/1/cancel")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(customerOrderService, times(1)).cancelOrder(1L);
    }

    @Test
    void testUpdateOrderStatus_Success() throws Exception {
        // Given
        testOrderDTO.setStatus("CONFIRMED");
        when(customerOrderService.updateOrderStatus(1L, "CONFIRMED"))
                .thenReturn(testOrderDTO);

        // When/Then
        mockMvc.perform(patch("/api/customer-orders/1/status")
                        .with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(customerOrderService, times(1)).updateOrderStatus(1L, "CONFIRMED");
    }

    @Test
    void testDeleteOrder_Success() throws Exception {
        // Given
        doNothing().when(customerOrderService).deleteOrder(1L);

        // When/Then
        mockMvc.perform(delete("/api/customer-orders/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(customerOrderService, times(1)).deleteOrder(1L);
    }

    @Test
    void testMultipleItemsOrder_Success() throws Exception {
        // Given
        OrderItemDTO item2 = new OrderItemDTO();
        item2.setItemType("PRODUCT");
        item2.setItemId(2L);
        item2.setQuantity(3);
        testOrderDTO.getOrderItems().add(item2);

        when(customerOrderService.createOrder(any(CustomerOrderDTO.class)))
                .thenReturn(testOrderDTO);

        String requestBody = """
                {
                    "workstationId": 7,
                    "orderItems": [
                        {
                            "itemType": "PRODUCT",
                            "itemId": 1,
                            "quantity": 5
                        },
                        {
                            "itemType": "PRODUCT",
                            "itemId": 2,
                            "quantity": 3
                        }
                    ]
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/customer-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderItems", hasSize(2)));

        verify(customerOrderService, times(1)).createOrder(any(CustomerOrderDTO.class));
    }
}
