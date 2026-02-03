package io.life.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.dto.request.*;
import io.life.order.exception.EntityNotFoundException;
import io.life.order.exception.InvalidOrderStateException;
import io.life.order.service.ProductionOrderService;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ProductionOrderController.
 * Tests REST API endpoints for Production Planning operations.
 * 
 * Production orders are created for:
 * - Scenario 3: Warehouse order needs production (insufficient modules)
 * - Scenario 4: High-volume orders bypass warehouse for direct production
 */
@WebMvcTest(ProductionOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
@DisplayName("ProductionOrderController Tests")
class ProductionOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductionOrderService productionOrderService;

    private ProductionOrderDTO testOrder1;
    private ProductionOrderDTO testOrder2;

    @BeforeEach
    void setUp() {
        Mockito.reset(productionOrderService);

        testOrder1 = ProductionOrderDTO.builder()
                .id(1L)
                .productionOrderNumber("PO-00001")
                .sourceCustomerOrderId(10L)
                .sourceWarehouseOrderId(5L)
                .status("CREATED")
                .priority("NORMAL")
                .triggerScenario("SCENARIO_3")
                .createdByWorkstationId(8L)
                .notes("Test production order 1")
                .createdAt(LocalDateTime.now())
                .build();

        testOrder2 = ProductionOrderDTO.builder()
                .id(2L)
                .productionOrderNumber("PO-00002")
                .sourceCustomerOrderId(11L)
                .status("SCHEDULED")
                .priority("HIGH")
                .triggerScenario("SCENARIO_4")
                .simalScheduleId("SIMAL-001")
                .createdByWorkstationId(7L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // GET Endpoints Tests
    // =========================================================================

    @Nested
    @DisplayName("GET /api/production-orders")
    class GetAllProductionOrdersTests {

        @Test
        @DisplayName("Should return all production orders")
        void getAllProductionOrders_ReturnsOrderList() throws Exception {
            when(productionOrderService.getAllProductionOrders())
                    .thenReturn(Arrays.asList(testOrder1, testOrder2));

            mockMvc.perform(get("/api/production-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].productionOrderNumber", is("PO-00001")))
                    .andExpect(jsonPath("$[1].productionOrderNumber", is("PO-00002")));

            verify(productionOrderService).getAllProductionOrders();
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void getAllProductionOrders_ReturnsEmptyList() throws Exception {
            when(productionOrderService.getAllProductionOrders())
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/production-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/production-orders/{id}")
    class GetProductionOrderByIdTests {

        @Test
        @DisplayName("Should return order when found by ID")
        void getProductionOrderById_Found() throws Exception {
            when(productionOrderService.getProductionOrderById(1L))
                    .thenReturn(Optional.of(testOrder1));

            mockMvc.perform(get("/api/production-orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.productionOrderNumber", is("PO-00001")))
                    .andExpect(jsonPath("$.status", is("CREATED")))
                    .andExpect(jsonPath("$.triggerScenario", is("SCENARIO_3")));
        }

        @Test
        @DisplayName("Should return 404 when order not found by ID")
        void getProductionOrderById_NotFound() throws Exception {
            when(productionOrderService.getProductionOrderById(999L))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/production-orders/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/production-orders/number/{productionOrderNumber}")
    class GetProductionOrderByNumberTests {

        @Test
        @DisplayName("Should return order when found by order number")
        void getByNumber_Found() throws Exception {
            when(productionOrderService.getProductionOrderByNumber("PO-00001"))
                    .thenReturn(Optional.of(testOrder1));

            mockMvc.perform(get("/api/production-orders/number/PO-00001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productionOrderNumber", is("PO-00001")));
        }

        @Test
        @DisplayName("Should return 404 when order number not found")
        void getByNumber_NotFound() throws Exception {
            when(productionOrderService.getProductionOrderByNumber("PO-99999"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/production-orders/number/PO-99999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/production-orders/customer/{sourceCustomerOrderId}")
    class GetByCustomerOrderTests {

        @Test
        @DisplayName("Should return orders for customer order (Scenario 4)")
        void getByCustomerOrder_ReturnsOrders() throws Exception {
            when(productionOrderService.getProductionOrdersByCustomerOrder(10L))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/production-orders/customer/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].sourceCustomerOrderId", is(10)));
        }

        @Test
        @DisplayName("Should return empty list when no orders for customer")
        void getByCustomerOrder_Empty() throws Exception {
            when(productionOrderService.getProductionOrdersByCustomerOrder(999L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/production-orders/customer/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/production-orders/warehouse/{sourceWarehouseOrderId}")
    class GetByWarehouseOrderTests {

        @Test
        @DisplayName("Should return orders for warehouse order (Scenario 3)")
        void getByWarehouseOrder_ReturnsOrders() throws Exception {
            when(productionOrderService.getProductionOrdersByWarehouseOrder(5L))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/production-orders/warehouse/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].sourceWarehouseOrderId", is(5)));
        }
    }

    @Nested
    @DisplayName("GET /api/production-orders/status/{status}")
    class GetByStatusTests {

        @Test
        @DisplayName("Should return orders filtered by SCHEDULED status")
        void getByStatus_Scheduled() throws Exception {
            when(productionOrderService.getProductionOrdersByStatus("SCHEDULED"))
                    .thenReturn(Collections.singletonList(testOrder2));

            mockMvc.perform(get("/api/production-orders/status/SCHEDULED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("SCHEDULED")));
        }

        @Test
        @DisplayName("Should return empty list for status with no orders")
        void getByStatus_Empty() throws Exception {
            when(productionOrderService.getProductionOrdersByStatus("CANCELLED"))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/production-orders/status/CANCELLED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/production-orders/priority/{priority}")
    class GetByPriorityTests {

        @Test
        @DisplayName("Should return orders filtered by HIGH priority")
        void getByPriority_High() throws Exception {
            when(productionOrderService.getProductionOrdersByPriority("HIGH"))
                    .thenReturn(Collections.singletonList(testOrder2));

            mockMvc.perform(get("/api/production-orders/priority/HIGH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].priority", is("HIGH")));
        }
    }

    @Nested
    @DisplayName("GET /api/production-orders/workstation/{createdByWorkstationId}")
    class GetByWorkstationTests {

        @Test
        @DisplayName("Should return orders created by workstation")
        void getByWorkstation_ReturnsOrders() throws Exception {
            when(productionOrderService.getProductionOrdersByWorkstation(8L))
                    .thenReturn(Collections.singletonList(testOrder1));

            mockMvc.perform(get("/api/production-orders/workstation/8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].createdByWorkstationId", is(8)));
        }
    }

    // =========================================================================
    // POST Endpoints (Create Operations)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/production-orders (Standalone)")
    class CreateStandaloneProductionOrderTests {

        @Test
        @DisplayName("Should create standalone production order")
        void createStandalone_Success() throws Exception {
            CreateProductionOrderRequest request = CreateProductionOrderRequest.builder()
                    .sourceCustomerOrderId(10L)
                    .priority("NORMAL")
                    .notes("Test production")
                    .createdByWorkstationId(8L)
                    .build();

            when(productionOrderService.createStandaloneProductionOrder(
                    eq(10L), eq("NORMAL"), any(), eq("Test production"), eq(8L)))
                    .thenReturn(testOrder1);

            mockMvc.perform(post("/api/production-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productionOrderNumber", is("PO-00001")));
        }
    }

    @Nested
    @DisplayName("POST /api/production-orders/create (From Warehouse)")
    class CreateFromWarehouseTests {

        @Test
        @DisplayName("Should create production order from warehouse order (Scenario 3)")
        void createFromWarehouse_Success() throws Exception {
            CreateProductionOrderFromWarehouseRequest request = CreateProductionOrderFromWarehouseRequest.builder()
                    .sourceCustomerOrderId(10L)
                    .sourceWarehouseOrderId(5L)
                    .priority("NORMAL")
                    .notes("Production from warehouse")
                    .createdByWorkstationId(8L)
                    .build();

            when(productionOrderService.createProductionOrderFromWarehouse(
                    eq(10L), eq(5L), eq("NORMAL"), any(), any(), eq(8L), any()))
                    .thenReturn(testOrder1);

            mockMvc.perform(post("/api/production-orders/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.triggerScenario", is("SCENARIO_3")));
        }
    }

    @Nested
    @DisplayName("POST /api/production-orders/from-customer-order (Scenario 4)")
    class CreateFromCustomerOrderTests {

        @Test
        @DisplayName("Should create production order from customer order (Scenario 4)")
        void createFromCustomerOrder_Success() throws Exception {
            CreateFromCustomerOrderRequest request = CreateFromCustomerOrderRequest.builder()
                    .customerOrderId(11L)
                    .priority("HIGH")
                    .notes("Direct production")
                    .createdByWorkstationId(7L)
                    .build();

            when(productionOrderService.createFromCustomerOrder(
                    eq(11L), eq("HIGH"), any(), eq("Direct production"), eq(7L)))
                    .thenReturn(testOrder2);

            mockMvc.perform(post("/api/production-orders/from-customer-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.triggerScenario", is("SCENARIO_4")));
        }
    }

    // =========================================================================
    // PATCH Endpoints (Status & Update Operations)
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/production-orders/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update production order status")
        void updateStatus_Success() throws Exception {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status("CONFIRMED")
                    .build();

            ProductionOrderDTO updatedOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("CONFIRMED")
                    .build();

            when(productionOrderService.updateProductionOrderStatus(1L, "CONFIRMED"))
                    .thenReturn(updatedOrder);

            mockMvc.perform(patch("/api/production-orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/production-orders/{id}/link-simal")
    class LinkToSimalTests {

        @Test
        @DisplayName("Should link production order to SimAL schedule")
        void linkToSimal_Success() throws Exception {
            LinkSimalRequest request = LinkSimalRequest.builder()
                    .simalScheduleId("SIMAL-002")
                    .estimatedDuration(120)
                    .build();

            ProductionOrderDTO linkedOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .simalScheduleId("SIMAL-002")
                    .estimatedDuration(120)
                    .build();

            when(productionOrderService.linkToSimalSchedule(eq(1L), eq("SIMAL-002"), eq(120), any()))
                    .thenReturn(linkedOrder);

            mockMvc.perform(patch("/api/production-orders/1/link-simal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.simalScheduleId", is("SIMAL-002")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/production-orders/{id}/complete")
    class CompleteProductionOrderTests {

        @Test
        @DisplayName("Should complete production order")
        void complete_Success() throws Exception {
            ProductionOrderDTO completedOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("COMPLETED")
                    .actualCompletionTime(LocalDateTime.now())
                    .build();

            when(productionOrderService.completeProductionOrder(1L))
                    .thenReturn(completedOrder);

            mockMvc.perform(patch("/api/production-orders/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/production-orders/{id}/cancel")
    class CancelProductionOrderTests {

        @Test
        @DisplayName("Should cancel production order")
        void cancel_Success() throws Exception {
            ProductionOrderDTO cancelledOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("CANCELLED")
                    .build();

            when(productionOrderService.cancelProductionOrder(1L))
                    .thenReturn(cancelledOrder);

            mockMvc.perform(patch("/api/production-orders/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));
        }
    }

    // =========================================================================
    // POST Endpoints (Workflow Operations)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/production-orders/{id}/confirm")
    class ConfirmProductionOrderTests {

        @Test
        @DisplayName("Should confirm production order (CREATED -> CONFIRMED)")
        void confirm_Success() throws Exception {
            ProductionOrderDTO confirmedOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("CONFIRMED")
                    .build();

            when(productionOrderService.confirmProductionOrder(1L))
                    .thenReturn(confirmedOrder);

            mockMvc.perform(post("/api/production-orders/1/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));
        }
    }

    @Nested
    @DisplayName("POST /api/production-orders/{id}/schedule")
    class ScheduleProductionTests {

        @Test
        @DisplayName("Should schedule production order with SimAL integration")
        void schedule_Success() throws Exception {
            ScheduleProductionRequest request = ScheduleProductionRequest.builder()
                    .scheduledStartTime(LocalDateTime.now().plusHours(1))
                    .scheduledEndTime(LocalDateTime.now().plusHours(3))
                    .ganttChartId("GANTT-001")
                    .build();

            ProductionOrderDTO scheduledOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("SCHEDULED")
                    .build();

            when(productionOrderService.scheduleProduction(eq(1L), any(), any(), eq("GANTT-001")))
                    .thenReturn(scheduledOrder);

            mockMvc.perform(post("/api/production-orders/1/schedule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("SCHEDULED")));
        }
    }

    @Nested
    @DisplayName("POST /api/production-orders/{id}/dispatch")
    class DispatchToControlStationsTests {

        @Test
        @DisplayName("Should dispatch to control stations (SCHEDULED -> DISPATCHED)")
        void dispatch_Success() throws Exception {
            ProductionOrderDTO dispatchedOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("DISPATCHED")
                    .build();

            when(productionOrderService.dispatchToControlStations(1L))
                    .thenReturn(dispatchedOrder);

            mockMvc.perform(post("/api/production-orders/1/dispatch"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("DISPATCHED")));
        }
    }

    @Nested
    @DisplayName("POST /api/production-orders/{id}/control-completion")
    class ControlCompletionTests {

        @Test
        @DisplayName("Should update from control order completion")
        void controlCompletion_Success() throws Exception {
            ControlCompletionRequest request = ControlCompletionRequest.builder()
                    .controlOrderId(100L)
                    .build();

            ProductionOrderDTO updatedOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("IN_PROGRESS")
                    .build();

            when(productionOrderService.updateFromControlOrderCompletion(1L, 100L))
                    .thenReturn(updatedOrder);

            mockMvc.perform(post("/api/production-orders/1/control-completion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/production-orders/{id}/complete-with-notification")
    class CompleteWithNotificationTests {

        @Test
        @DisplayName("Should complete and notify warehouse order")
        void completeWithNotification_Success() throws Exception {
            ProductionOrderDTO completedOrder = ProductionOrderDTO.builder()
                    .id(1L)
                    .productionOrderNumber("PO-00001")
                    .status("COMPLETED")
                    .actualCompletionTime(LocalDateTime.now())
                    .build();

            when(productionOrderService.completeProductionOrderWithNotification(1L))
                    .thenReturn(completedOrder);

            mockMvc.perform(post("/api/production-orders/1/complete-with-notification"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")));
        }
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 404 when production order not found for update")
        void updateStatus_NotFound() throws Exception {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status("CONFIRMED")
                    .build();

            when(productionOrderService.updateProductionOrderStatus(999L, "CONFIRMED"))
                    .thenThrow(new EntityNotFoundException("Production order not found"));

            mockMvc.perform(patch("/api/production-orders/999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for invalid state transition")
        void confirm_InvalidState() throws Exception {
            when(productionOrderService.confirmProductionOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Cannot confirm order in current state"));

            mockMvc.perform(post("/api/production-orders/1/confirm"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when dispatching non-existent order")
        void dispatch_NotFound() throws Exception {
            when(productionOrderService.dispatchToControlStations(999L))
                    .thenThrow(new EntityNotFoundException("Production order not found"));

            mockMvc.perform(post("/api/production-orders/999/dispatch"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when completing order in wrong state")
        void complete_InvalidState() throws Exception {
            when(productionOrderService.completeProductionOrder(1L))
                    .thenThrow(new InvalidOrderStateException("Cannot complete order - not all control orders finished"));

            mockMvc.perform(patch("/api/production-orders/1/complete"))
                    .andExpect(status().isBadRequest());
        }
    }
}
