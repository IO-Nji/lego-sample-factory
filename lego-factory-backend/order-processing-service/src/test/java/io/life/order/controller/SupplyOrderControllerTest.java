package io.life.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.request.SupplyOrderCreateRequest;
import io.life.order.dto.request.SupplyOrderFromControlRequest;
import io.life.order.exception.EntityNotFoundException;
import io.life.order.exception.InvalidOrderStateException;
import io.life.order.service.SupplyOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for SupplyOrderController.
 * Tests REST API endpoints for WS-9 Parts Supply Warehouse operations.
 * 
 * Supply orders:
 * - MUST be fulfilled before workstation orders can start (gating logic)
 * - Created from Control Orders (Production/Assembly)
 * - Parts Supply Warehouse (WS-9) fulfills by debiting parts inventory
 */
@WebMvcTest(SupplyOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
@DisplayName("SupplyOrderController Tests")
class SupplyOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplyOrderService supplyOrderService;

    private SupplyOrderDTO testOrder1;
    private SupplyOrderDTO testOrder2;

    @BeforeEach
    void setUp() {
        Mockito.reset(supplyOrderService);

        testOrder1 = SupplyOrderDTO.builder()
                .id(1L)
                .supplyOrderNumber("SO-00001")
                .sourceControlOrderId(10L)
                .sourceControlOrderType("PRODUCTION")
                .requestingWorkstationId(1L)
                .supplyWarehouseWorkstationId(9L)
                .status("PENDING")
                .priority("NORMAL")
                .createdAt(LocalDateTime.now())
                .build();

        testOrder2 = SupplyOrderDTO.builder()
                .id(2L)
                .supplyOrderNumber("SO-00002")
                .sourceControlOrderId(20L)
                .sourceControlOrderType("ASSEMBLY")
                .requestingWorkstationId(4L)
                .supplyWarehouseWorkstationId(9L)
                .status("FULFILLED")
                .priority("HIGH")
                .fulfilledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    // =========================================================================
    // GET Endpoints Tests
    // =========================================================================

    @Nested
    @DisplayName("GET /api/supply-orders/warehouse")
    class GetWarehouseOrdersTests {

        @Test
        @DisplayName("Should return all supply orders for warehouse dashboard")
        void getWarehouseOrders_ReturnsAll() throws Exception {
            when(supplyOrderService.getOrdersForSupplyWarehouse(null))
                    .thenReturn(Arrays.asList(testOrder1, testOrder2));

            mockMvc.perform(get("/api/supply-orders/warehouse"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].supplyOrderNumber", is("SO-00001")))
                    .andExpect(jsonPath("$[1].supplyOrderNumber", is("SO-00002")));
        }

        @Test
        @DisplayName("Should filter orders by status")
        void getWarehouseOrders_FilterByStatus() throws Exception {
            when(supplyOrderService.getOrdersForSupplyWarehouse("PENDING"))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/supply-orders/warehouse")
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PENDING")));
        }

        @Test
        @DisplayName("Should return empty list when no orders match status")
        void getWarehouseOrders_Empty() throws Exception {
            when(supplyOrderService.getOrdersForSupplyWarehouse("CANCELLED"))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/supply-orders/warehouse")
                            .param("status", "CANCELLED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/supply-orders/{id}")
    class GetSupplyOrderByIdTests {

        @Test
        @DisplayName("Should return order when found by ID")
        void getById_Found() throws Exception {
            when(supplyOrderService.getSupplyOrder(1L))
                    .thenReturn(testOrder1);

            mockMvc.perform(get("/api/supply-orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.supplyOrderNumber", is("SO-00001")))
                    .andExpect(jsonPath("$.sourceControlOrderType", is("PRODUCTION")))
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void getById_NotFound() throws Exception {
            when(supplyOrderService.getSupplyOrder(999L))
                    .thenThrow(new EntityNotFoundException("Supply order not found"));

            mockMvc.perform(get("/api/supply-orders/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/supply-orders/workstation/{workstationId}")
    class GetOrdersByWorkstationTests {

        @Test
        @DisplayName("Should return orders for requesting workstation")
        void getByWorkstation_ReturnsOrders() throws Exception {
            when(supplyOrderService.getOrdersByRequestingWorkstation(1L, null))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/supply-orders/workstation/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].requestingWorkstationId", is(1)));
        }

        @Test
        @DisplayName("Should filter by status for workstation")
        void getByWorkstation_FilterByStatus() throws Exception {
            when(supplyOrderService.getOrdersByRequestingWorkstation(4L, "FULFILLED"))
                    .thenReturn(Collections.singletonList(testOrder2));

            mockMvc.perform(get("/api/supply-orders/workstation/4")
                            .param("status", "FULFILLED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("FULFILLED")));
        }
    }

    @Nested
    @DisplayName("GET /api/supply-orders/source/{controlOrderId}")
    class GetBySourceControlOrderTests {

        @Test
        @DisplayName("Should return orders for production control order")
        void getBySourceControlOrder_Production() throws Exception {
            when(supplyOrderService.getBySourceControlOrder(10L, "PRODUCTION"))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/supply-orders/source/10")
                            .param("type", "PRODUCTION"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].sourceControlOrderId", is(10)));
        }

        @Test
        @DisplayName("Should return orders for assembly control order")
        void getBySourceControlOrder_Assembly() throws Exception {
            when(supplyOrderService.getBySourceControlOrder(20L, "ASSEMBLY"))
                    .thenReturn(Collections.singletonList(testOrder2));

            mockMvc.perform(get("/api/supply-orders/source/20")
                            .param("type", "ASSEMBLY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].sourceControlOrderType", is("ASSEMBLY")));
        }
    }

    // =========================================================================
    // POST Endpoints (Create Operations)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/supply-orders")
    class CreateSupplyOrderTests {

        @Test
        @DisplayName("Should create supply order")
        void create_Success() throws Exception {
            SupplyOrderCreateRequest request = SupplyOrderCreateRequest.builder()
                    .sourceControlOrderId(10L)
                    .sourceControlOrderType("PRODUCTION")
                    .requestingWorkstationId(1L)
                    .priority("NORMAL")
                    .notes("Test supply order")
                    .build();

            when(supplyOrderService.createSupplyOrder(
                    eq(10L), eq("PRODUCTION"), eq(1L), eq("NORMAL"), any(), any(), eq("Test supply order")))
                    .thenReturn(testOrder1);

            mockMvc.perform(post("/api/supply-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.supplyOrderNumber", is("SO-00001")));
        }
    }

    @Nested
    @DisplayName("POST /api/supply-orders/from-control-order")
    class CreateFromControlOrderTests {

        @Test
        @DisplayName("Should create supply order from control order with BOM lookup")
        void createFromControlOrder_Success() throws Exception {
            SupplyOrderFromControlRequest request = SupplyOrderFromControlRequest.builder()
                    .controlOrderId(10L)
                    .controlOrderType("PRODUCTION")
                    .priority("HIGH")
                    .build();

            when(supplyOrderService.createSupplyOrderFromControlOrder(10L, "PRODUCTION", "HIGH"))
                    .thenReturn(testOrder1);

            mockMvc.perform(post("/api/supply-orders/from-control-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sourceControlOrderId", is(10)));
        }
    }

    // =========================================================================
    // PUT/PATCH Endpoints (Status Operations)
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/supply-orders/{id}/fulfill")
    class FulfillSupplyOrderTests {

        @Test
        @DisplayName("Should fulfill supply order and debit inventory")
        void fulfill_Success() throws Exception {
            SupplyOrderDTO fulfilledOrder = SupplyOrderDTO.builder()
                    .id(1L)
                    .supplyOrderNumber("SO-00001")
                    .status("FULFILLED")
                    .fulfilledAt(LocalDateTime.now())
                    .build();

            when(supplyOrderService.fulfillSupplyOrder(1L))
                    .thenReturn(fulfilledOrder);

            mockMvc.perform(put("/api/supply-orders/1/fulfill"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FULFILLED")))
                    .andExpect(jsonPath("$.fulfilledAt", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 for invalid state transition")
        void fulfill_InvalidState() throws Exception {
            when(supplyOrderService.fulfillSupplyOrder(2L))
                    .thenThrow(new InvalidOrderStateException("Cannot fulfill - already fulfilled"));

            mockMvc.perform(put("/api/supply-orders/2/fulfill"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void fulfill_NotFound() throws Exception {
            when(supplyOrderService.fulfillSupplyOrder(999L))
                    .thenThrow(new EntityNotFoundException("Supply order not found"));

            mockMvc.perform(put("/api/supply-orders/999/fulfill"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/supply-orders/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update supply order status")
        void updateStatus_Success() throws Exception {
            Map<String, String> request = new HashMap<>();
            request.put("status", "IN_PROGRESS");

            SupplyOrderDTO updatedOrder = SupplyOrderDTO.builder()
                    .id(1L)
                    .supplyOrderNumber("SO-00001")
                    .status("IN_PROGRESS")
                    .build();

            when(supplyOrderService.updateStatus(1L, "IN_PROGRESS"))
                    .thenReturn(updatedOrder);

            mockMvc.perform(patch("/api/supply-orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
        }
    }

    @Nested
    @DisplayName("PUT /api/supply-orders/{id}/reject")
    class RejectSupplyOrderTests {

        @Test
        @DisplayName("Should reject supply order with reason")
        void reject_Success() throws Exception {
            Map<String, String> request = new HashMap<>();
            request.put("reason", "Insufficient stock");

            SupplyOrderDTO rejectedOrder = SupplyOrderDTO.builder()
                    .id(1L)
                    .supplyOrderNumber("SO-00001")
                    .status("REJECTED")
                    .rejectedAt(LocalDateTime.now())
                    .notes("Rejected: Insufficient stock")
                    .build();

            when(supplyOrderService.rejectSupplyOrder(1L, "Insufficient stock"))
                    .thenReturn(rejectedOrder);

            mockMvc.perform(put("/api/supply-orders/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("REJECTED")));
        }
    }

    @Nested
    @DisplayName("PUT /api/supply-orders/{id}/cancel")
    class CancelSupplyOrderTests {

        @Test
        @DisplayName("Should cancel supply order with reason")
        void cancel_Success() throws Exception {
            Map<String, String> request = new HashMap<>();
            request.put("reason", "No longer needed");

            SupplyOrderDTO cancelledOrder = SupplyOrderDTO.builder()
                    .id(1L)
                    .supplyOrderNumber("SO-00001")
                    .status("CANCELLED")
                    .cancelledAt(LocalDateTime.now())
                    .notes("Cancelled: No longer needed")
                    .build();

            when(supplyOrderService.cancelSupplyOrder(1L, "No longer needed"))
                    .thenReturn(cancelledOrder);

            mockMvc.perform(put("/api/supply-orders/1/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        @DisplayName("Should return 400 for invalid state on cancel")
        void cancel_InvalidState() throws Exception {
            Map<String, String> request = new HashMap<>();
            request.put("reason", "Test reason");

            when(supplyOrderService.cancelSupplyOrder(2L, "Test reason"))
                    .thenThrow(new InvalidOrderStateException("Cannot cancel - already fulfilled"));

            mockMvc.perform(put("/api/supply-orders/2/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
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
            when(supplyOrderService.getSupplyOrder(999L))
                    .thenThrow(new EntityNotFoundException("Supply order 999 not found"));

            mockMvc.perform(get("/api/supply-orders/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for InvalidOrderStateException")
        void handleInvalidState() throws Exception {
            when(supplyOrderService.fulfillSupplyOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Order already fulfilled"));

            mockMvc.perform(put("/api/supply-orders/1/fulfill"))
                    .andExpect(status().isBadRequest());
        }
    }
}
