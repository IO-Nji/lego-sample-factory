package io.life.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.life.order.dto.FinalAssemblyOrderDTO;
import io.life.order.exception.EntityNotFoundException;
import io.life.order.exception.InvalidOrderStateException;
import io.life.order.service.FinalAssemblyOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for FinalAssemblyOrderController.
 * Tests REST API endpoints for WS-6 Final Assembly operations.
 * 
 * Final Assembly orders:
 * - Assemble modules into finished products
 * - Credit Plant Warehouse (WS-7) upon submission
 * - Created from Warehouse Orders (Scenario 2) or Assembly Control Orders (Scenario 3)
 */
@WebMvcTest(FinalAssemblyOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
@DisplayName("FinalAssemblyOrderController Tests")
class FinalAssemblyOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FinalAssemblyOrderService finalAssemblyOrderService;

    private FinalAssemblyOrderDTO testOrder1;
    private FinalAssemblyOrderDTO testOrder2;

    @BeforeEach
    void setUp() {
        Mockito.reset(finalAssemblyOrderService);

        testOrder1 = new FinalAssemblyOrderDTO();
        testOrder1.setId(1L);
        testOrder1.setOrderNumber("FA-00001");
        testOrder1.setWarehouseOrderId(10L);
        testOrder1.setWorkstationId(6L);
        testOrder1.setOutputProductId(1L);
        testOrder1.setOutputQuantity(5);
        testOrder1.setStatus("PENDING");
        testOrder1.setCreatedAt(LocalDateTime.now());

        testOrder2 = new FinalAssemblyOrderDTO();
        testOrder2.setId(2L);
        testOrder2.setOrderNumber("FA-00002");
        testOrder2.setAssemblyControlOrderId(20L);
        testOrder2.setWorkstationId(6L);
        testOrder2.setOutputProductId(2L);
        testOrder2.setOutputQuantity(3);
        testOrder2.setStatus("IN_PROGRESS");
        testOrder2.setStartTime(LocalDateTime.now().minusHours(1));
        testOrder2.setCreatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // GET Endpoints Tests
    // =========================================================================

    @Nested
    @DisplayName("GET /api/final-assembly-orders")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should return all final assembly orders")
        void getAllOrders_ReturnsOrderList() throws Exception {
            when(finalAssemblyOrderService.getAllOrders())
                    .thenReturn(Arrays.asList(testOrder1, testOrder2));

            mockMvc.perform(get("/api/final-assembly-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].orderNumber", is("FA-00001")))
                    .andExpect(jsonPath("$[1].orderNumber", is("FA-00002")));

            verify(finalAssemblyOrderService).getAllOrders();
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void getAllOrders_ReturnsEmptyList() throws Exception {
            when(finalAssemblyOrderService.getAllOrders())
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/final-assembly-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/final-assembly-orders/{id}")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order when found by ID")
        void getOrderById_Found() throws Exception {
            when(finalAssemblyOrderService.getOrderById(1L))
                    .thenReturn(Optional.of(testOrder1));

            mockMvc.perform(get("/api/final-assembly-orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.orderNumber", is("FA-00001")))
                    .andExpect(jsonPath("$.workstationId", is(6)))
                    .andExpect(jsonPath("$.outputProductId", is(1)))
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }

        @Test
        @DisplayName("Should return 404 when order not found by ID")
        void getOrderById_NotFound() throws Exception {
            when(finalAssemblyOrderService.getOrderById(999L))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/final-assembly-orders/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/final-assembly-orders/workstation/{workstationId}")
    class GetOrdersByWorkstationTests {

        @Test
        @DisplayName("Should return orders for WS-6")
        void getByWorkstation_ReturnsOrders() throws Exception {
            when(finalAssemblyOrderService.getOrdersByWorkstationId(6L))
                    .thenReturn(Arrays.asList(testOrder1, testOrder2));

            mockMvc.perform(get("/api/final-assembly-orders/workstation/6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].workstationId", is(6)))
                    .andExpect(jsonPath("$[1].workstationId", is(6)));
        }

        @Test
        @DisplayName("Should return empty list for non-existent workstation")
        void getByWorkstation_Empty() throws Exception {
            when(finalAssemblyOrderService.getOrdersByWorkstationId(99L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/final-assembly-orders/workstation/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/final-assembly-orders/status/{status}")
    class GetOrdersByStatusTests {

        @Test
        @DisplayName("Should return orders filtered by PENDING status")
        void getByStatus_Pending() throws Exception {
            when(finalAssemblyOrderService.getOrdersByStatus("PENDING"))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/final-assembly-orders/status/PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PENDING")));
        }

        @Test
        @DisplayName("Should return orders filtered by IN_PROGRESS status")
        void getByStatus_InProgress() throws Exception {
            when(finalAssemblyOrderService.getOrdersByStatus("IN_PROGRESS"))
                    .thenReturn(Collections.singletonList(testOrder2));

            mockMvc.perform(get("/api/final-assembly-orders/status/IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("IN_PROGRESS")));
        }
    }

    @Nested
    @DisplayName("GET /api/final-assembly-orders/warehouse-order/{warehouseOrderId}")
    class GetOrdersByWarehouseOrderTests {

        @Test
        @DisplayName("Should return orders for warehouse order (Scenario 2)")
        void getByWarehouseOrder_ReturnsOrders() throws Exception {
            when(finalAssemblyOrderService.getOrdersByWarehouseOrderId(10L))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/final-assembly-orders/warehouse-order/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].warehouseOrderId", is(10)));
        }

        @Test
        @DisplayName("Should return empty list when no orders for warehouse order")
        void getByWarehouseOrder_Empty() throws Exception {
            when(finalAssemblyOrderService.getOrdersByWarehouseOrderId(999L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/final-assembly-orders/warehouse-order/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/final-assembly-orders/warehouse-order/{warehouseOrderId}/all-submitted")
    class AreAllOrdersSubmittedTests {

        @Test
        @DisplayName("Should return true when all orders are submitted")
        void allSubmitted_True() throws Exception {
            when(finalAssemblyOrderService.areAllOrdersSubmittedForWarehouseOrder(10L))
                    .thenReturn(true);

            mockMvc.perform(get("/api/final-assembly-orders/warehouse-order/10/all-submitted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(true)));
        }

        @Test
        @DisplayName("Should return false when some orders pending")
        void allSubmitted_False() throws Exception {
            when(finalAssemblyOrderService.areAllOrdersSubmittedForWarehouseOrder(10L))
                    .thenReturn(false);

            mockMvc.perform(get("/api/final-assembly-orders/warehouse-order/10/all-submitted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(false)));
        }
    }

    // =========================================================================
    // PUT/POST Endpoints (Workflow Operations)
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/final-assembly-orders/{id}/confirm")
    class ConfirmOrderTests {

        @Test
        @DisplayName("Should confirm order (PENDING -> CONFIRMED)")
        void confirm_Success() throws Exception {
            FinalAssemblyOrderDTO confirmedOrder = new FinalAssemblyOrderDTO();
            confirmedOrder.setId(1L);
            confirmedOrder.setOrderNumber("FA-00001");
            confirmedOrder.setStatus("CONFIRMED");

            when(finalAssemblyOrderService.confirmOrder(1L))
                    .thenReturn(confirmedOrder);

            mockMvc.perform(put("/api/final-assembly-orders/1/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));
        }

        @Test
        @DisplayName("Should return 404 when order not found for confirm")
        void confirm_NotFound() throws Exception {
            when(finalAssemblyOrderService.confirmOrder(999L))
                    .thenThrow(new EntityNotFoundException("Order not found"));

            mockMvc.perform(put("/api/final-assembly-orders/999/confirm"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/final-assembly-orders/{id}/start")
    class StartOrderTests {

        @Test
        @DisplayName("Should start order (CONFIRMED -> IN_PROGRESS)")
        void start_Success() throws Exception {
            FinalAssemblyOrderDTO startedOrder = new FinalAssemblyOrderDTO();
            startedOrder.setId(1L);
            startedOrder.setOrderNumber("FA-00001");
            startedOrder.setStatus("IN_PROGRESS");
            startedOrder.setStartTime(LocalDateTime.now());

            when(finalAssemblyOrderService.startOrder(1L))
                    .thenReturn(startedOrder);

            mockMvc.perform(post("/api/final-assembly-orders/1/start"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                    .andExpect(jsonPath("$.startTime", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 for invalid state transition on start")
        void start_InvalidState() throws Exception {
            when(finalAssemblyOrderService.startOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Cannot start order - not in CONFIRMED state"));

            mockMvc.perform(post("/api/final-assembly-orders/1/start"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/final-assembly-orders/{id}/complete")
    class CompleteOrderTests {

        @Test
        @DisplayName("Should complete order (IN_PROGRESS -> COMPLETED)")
        void complete_Success() throws Exception {
            FinalAssemblyOrderDTO completedOrder = new FinalAssemblyOrderDTO();
            completedOrder.setId(2L);
            completedOrder.setOrderNumber("FA-00002");
            completedOrder.setStatus("COMPLETED");
            completedOrder.setCompletionTime(LocalDateTime.now());

            when(finalAssemblyOrderService.completeOrder(2L))
                    .thenReturn(completedOrder);

            mockMvc.perform(post("/api/final-assembly-orders/2/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.completionTime", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 for invalid state transition on complete")
        void complete_InvalidState() throws Exception {
            when(finalAssemblyOrderService.completeOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Cannot complete order - not in IN_PROGRESS state"));

            mockMvc.perform(post("/api/final-assembly-orders/1/complete"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/final-assembly-orders/{id}/submit")
    class SubmitOrderTests {

        @Test
        @DisplayName("Should submit order and credit Plant Warehouse")
        void submit_Success() throws Exception {
            FinalAssemblyOrderDTO submittedOrder = new FinalAssemblyOrderDTO();
            submittedOrder.setId(2L);
            submittedOrder.setOrderNumber("FA-00002");
            submittedOrder.setStatus("SUBMITTED");
            submittedOrder.setSubmitTime(LocalDateTime.now());

            when(finalAssemblyOrderService.submitOrder(2L))
                    .thenReturn(submittedOrder);

            mockMvc.perform(post("/api/final-assembly-orders/2/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("SUBMITTED")))
                    .andExpect(jsonPath("$.submitTime", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 for invalid state transition on submit")
        void submit_InvalidState() throws Exception {
            when(finalAssemblyOrderService.submitOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Cannot submit order - not in COMPLETED state"));

            mockMvc.perform(post("/api/final-assembly-orders/1/submit"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // PATCH Endpoints (Status Update)
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/final-assembly-orders/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update order status")
        void updateStatus_Success() throws Exception {
            FinalAssemblyOrderDTO updatedOrder = new FinalAssemblyOrderDTO();
            updatedOrder.setId(1L);
            updatedOrder.setOrderNumber("FA-00001");
            updatedOrder.setStatus("HALTED");

            when(finalAssemblyOrderService.updateOrderStatus(1L, "HALTED"))
                    .thenReturn(updatedOrder);

            mockMvc.perform(patch("/api/final-assembly-orders/1/status")
                            .param("status", "HALTED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("HALTED")));
        }

        @Test
        @DisplayName("Should return 404 when order not found for status update")
        void updateStatus_NotFound() throws Exception {
            when(finalAssemblyOrderService.updateOrderStatus(999L, "HALTED"))
                    .thenThrow(new EntityNotFoundException("Order not found"));

            mockMvc.perform(patch("/api/final-assembly-orders/999/status")
                            .param("status", "HALTED"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 404 for EntityNotFoundException")
        void handleEntityNotFound() throws Exception {
            when(finalAssemblyOrderService.startOrder(999L))
                    .thenThrow(new EntityNotFoundException("Final assembly order not found"));

            mockMvc.perform(post("/api/final-assembly-orders/999/start"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for InvalidOrderStateException")
        void handleInvalidState() throws Exception {
            when(finalAssemblyOrderService.submitOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Order is not in COMPLETED state"));

            mockMvc.perform(post("/api/final-assembly-orders/1/submit"))
                    .andExpect(status().isBadRequest());
        }
    }
}
