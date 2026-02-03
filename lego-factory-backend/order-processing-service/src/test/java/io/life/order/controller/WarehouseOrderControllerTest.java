package io.life.order.controller;

import io.life.order.dto.WarehouseOrderDTO;
import io.life.order.dto.WarehouseOrderItemDTO;
import io.life.order.exception.EntityNotFoundException;
import io.life.order.exception.InvalidOrderStateException;
import io.life.order.service.WarehouseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for WarehouseOrderController.
 * Tests REST API endpoints for WS-8 Modules Supermarket operations.
 * 
 * Key endpoints tested:
 * - GET /api/warehouse-orders
 * - GET /api/warehouse-orders/{id}
 * - GET /api/warehouse-orders/workstation/{workstationId}
 * - GET /api/warehouse-orders/status/{status}
 * - PUT /api/warehouse-orders/{id}/confirm
 * - PUT /api/warehouse-orders/{id}/fulfill-modules
 * - PATCH /api/warehouse-orders/{id}/status
 */
@WebMvcTest(WarehouseOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
@DisplayName("WarehouseOrderController Tests")
class WarehouseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WarehouseOrderService warehouseOrderService;

    private WarehouseOrderDTO testOrderDTO;

    @BeforeEach
    void setUp() {
        testOrderDTO = new WarehouseOrderDTO();
        testOrderDTO.setId(1L);
        testOrderDTO.setOrderNumber("WO-TEST001");
        testOrderDTO.setCustomerOrderId(100L);
        testOrderDTO.setWorkstationId(8L);
        testOrderDTO.setStatus("PENDING");
        testOrderDTO.setCreatedAt(LocalDateTime.now());

        WarehouseOrderItemDTO itemDTO = new WarehouseOrderItemDTO();
        itemDTO.setId(1L);
        itemDTO.setItemId(7L);
        itemDTO.setItemType("MODULE");
        itemDTO.setRequestedQuantity(5);
        itemDTO.setProductId(1L);

        testOrderDTO.setOrderItems(List.of(itemDTO));
    }

    @Nested
    @DisplayName("GET /api/warehouse-orders")
    class GetAllWarehouseOrdersTests {

        @Test
        @DisplayName("WOC-001: Returns all warehouse orders")
        void getAllWarehouseOrders_returnsOrderList() throws Exception {
            when(warehouseOrderService.getAllWarehouseOrders())
                    .thenReturn(List.of(testOrderDTO));

            mockMvc.perform(get("/api/warehouse-orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].orderNumber", is("WO-TEST001")))
                    .andExpect(jsonPath("$[0].status", is("PENDING")));

            verify(warehouseOrderService).getAllWarehouseOrders();
        }

        @Test
        @DisplayName("WOC-002: Returns empty list when no orders")
        void getAllWarehouseOrders_returnsEmptyList() throws Exception {
            when(warehouseOrderService.getAllWarehouseOrders())
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/warehouse-orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/warehouse-orders/{id}")
    class GetWarehouseOrderByIdTests {

        @Test
        @DisplayName("WOC-003: Returns order when found")
        void getWarehouseOrderById_returnsOrder() throws Exception {
            when(warehouseOrderService.getWarehouseOrderById(1L))
                    .thenReturn(Optional.of(testOrderDTO));

            mockMvc.perform(get("/api/warehouse-orders/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.orderNumber", is("WO-TEST001")))
                    .andExpect(jsonPath("$.workstationId", is(8)));

            verify(warehouseOrderService).getWarehouseOrderById(1L);
        }

        @Test
        @DisplayName("WOC-004: Returns 404 when order not found")
        void getWarehouseOrderById_returns404WhenNotFound() throws Exception {
            when(warehouseOrderService.getWarehouseOrderById(999L))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/warehouse-orders/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(warehouseOrderService).getWarehouseOrderById(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/warehouse-orders/workstation/{workstationId}")
    class GetOrdersByWorkstationTests {

        @Test
        @DisplayName("WOC-005: Returns orders for workstation")
        void getOrdersByWorkstation_returnsFilteredOrders() throws Exception {
            when(warehouseOrderService.getWarehouseOrdersByFulfillingWorkstationId(8L))
                    .thenReturn(List.of(testOrderDTO));

            mockMvc.perform(get("/api/warehouse-orders/workstation/8")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].workstationId", is(8)));

            verify(warehouseOrderService).getWarehouseOrdersByFulfillingWorkstationId(8L);
        }

        @Test
        @DisplayName("WOC-006: Returns empty list for workstation with no orders")
        void getOrdersByWorkstation_returnsEmptyList() throws Exception {
            when(warehouseOrderService.getWarehouseOrdersByFulfillingWorkstationId(99L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/warehouse-orders/workstation/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/warehouse-orders/status/{status}")
    class GetOrdersByStatusTests {

        @Test
        @DisplayName("WOC-007: Returns orders filtered by status")
        void getOrdersByStatus_returnsFilteredOrders() throws Exception {
            when(warehouseOrderService.getWarehouseOrdersByStatus("PENDING"))
                    .thenReturn(List.of(testOrderDTO));

            mockMvc.perform(get("/api/warehouse-orders/status/PENDING")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PENDING")));

            verify(warehouseOrderService).getWarehouseOrdersByStatus("PENDING");
        }

        @Test
        @DisplayName("WOC-008: Returns empty list for status with no orders")
        void getOrdersByStatus_returnsEmptyList() throws Exception {
            when(warehouseOrderService.getWarehouseOrdersByStatus("CANCELLED"))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/warehouse-orders/status/CANCELLED")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PUT /api/warehouse-orders/{id}/confirm")
    class ConfirmWarehouseOrderTests {

        @Test
        @DisplayName("WOC-009: Confirms warehouse order successfully")
        void confirmWarehouseOrder_confirmsOrder() throws Exception {
            WarehouseOrderDTO confirmedOrder = new WarehouseOrderDTO();
            confirmedOrder.setId(1L);
            confirmedOrder.setOrderNumber("WO-TEST001");
            confirmedOrder.setStatus("CONFIRMED");
            confirmedOrder.setTriggerScenario("DIRECT_FULFILLMENT");

            when(warehouseOrderService.confirmWarehouseOrder(1L))
                    .thenReturn(confirmedOrder);

            mockMvc.perform(put("/api/warehouse-orders/1/confirm")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")))
                    .andExpect(jsonPath("$.triggerScenario", is("DIRECT_FULFILLMENT")));

            verify(warehouseOrderService).confirmWarehouseOrder(1L);
        }

        @Test
        @DisplayName("WOC-010: Confirms with PRODUCTION_REQUIRED when insufficient stock")
        void confirmWarehouseOrder_setsProductionRequired() throws Exception {
            WarehouseOrderDTO confirmedOrder = new WarehouseOrderDTO();
            confirmedOrder.setId(1L);
            confirmedOrder.setStatus("CONFIRMED");
            confirmedOrder.setTriggerScenario("PRODUCTION_REQUIRED");

            when(warehouseOrderService.confirmWarehouseOrder(1L))
                    .thenReturn(confirmedOrder);

            mockMvc.perform(put("/api/warehouse-orders/1/confirm")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.triggerScenario", is("PRODUCTION_REQUIRED")));
        }
    }

    @Nested
    @DisplayName("PUT /api/warehouse-orders/{id}/fulfill-modules")
    class FulfillWarehouseOrderTests {

        @Test
        @DisplayName("WOC-011: Fulfills warehouse order successfully")
        void fulfillWarehouseOrder_fulfillsOrder() throws Exception {
            WarehouseOrderDTO fulfilledOrder = new WarehouseOrderDTO();
            fulfilledOrder.setId(1L);
            fulfilledOrder.setOrderNumber("WO-TEST001");
            fulfilledOrder.setStatus("FULFILLED");

            when(warehouseOrderService.fulfillWarehouseOrder(1L))
                    .thenReturn(fulfilledOrder);

            mockMvc.perform(put("/api/warehouse-orders/1/fulfill-modules")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FULFILLED")));

            verify(warehouseOrderService).fulfillWarehouseOrder(1L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/warehouse-orders/{id}/status")
    class UpdateWarehouseOrderStatusTests {

        @Test
        @DisplayName("WOC-012: Updates order status successfully")
        void updateWarehouseOrderStatus_updatesStatus() throws Exception {
            WarehouseOrderDTO updatedOrder = new WarehouseOrderDTO();
            updatedOrder.setId(1L);
            updatedOrder.setStatus("AWAITING_PRODUCTION");

            when(warehouseOrderService.updateWarehouseOrderStatus(1L, "AWAITING_PRODUCTION"))
                    .thenReturn(updatedOrder);

            mockMvc.perform(patch("/api/warehouse-orders/1/status")
                            .param("status", "AWAITING_PRODUCTION")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("AWAITING_PRODUCTION")));

            verify(warehouseOrderService).updateWarehouseOrderStatus(1L, "AWAITING_PRODUCTION");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("WOC-013: Returns proper error for service exception")
        void confirmWarehouseOrder_handlesServiceException() throws Exception {
            when(warehouseOrderService.confirmWarehouseOrder(999L))
                    .thenThrow(new EntityNotFoundException("Warehouse order not found: 999"));

            mockMvc.perform(put("/api/warehouse-orders/999/confirm")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("WOC-014: Returns proper error for invalid state transition")
        void fulfillWarehouseOrder_handlesInvalidState() throws Exception {
            when(warehouseOrderService.fulfillWarehouseOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Cannot fulfill order in PENDING status"));

            mockMvc.perform(put("/api/warehouse-orders/1/fulfill-modules")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}
