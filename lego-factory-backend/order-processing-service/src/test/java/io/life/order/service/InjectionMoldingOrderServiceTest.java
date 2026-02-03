package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.client.SimalClient;
import io.life.order.entity.InjectionMoldingOrder;
import io.life.order.repository.InjectionMoldingOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InjectionMoldingOrderService
 * 
 * Tests WS-1 (Injection Molding) workstation order lifecycle:
 * - Order retrieval (by workstation, by control order, by ID)
 * - Order lifecycle (start, complete, halt, resume)
 * - Inventory credit on completion
 * - SimAL integration updates
 * - Status validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InjectionMoldingOrderService Tests")
class InjectionMoldingOrderServiceTest {

    @Mock
    private InjectionMoldingOrderRepository injectionMoldingOrderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private SimalClient simalClient;

    @Mock
    private OrderOrchestrationService orchestrationService;

    @InjectMocks
    private InjectionMoldingOrderService injectionMoldingOrderService;

    private InjectionMoldingOrder testOrder;

    @BeforeEach
    void setUp() {
        testOrder = InjectionMoldingOrder.builder()
                .id(1L)
                .orderNumber("IMO-001")
                .productionControlOrderId(100L)
                .workstationId(1L)
                .outputPartId(10L)
                .outputPartName("Gear Housing Base")
                .quantity(50)
                .status("PENDING")
                .priority("NORMAL")
                .build();
    }

    // ========================================================================
    // Order Retrieval Tests
    // ========================================================================

    @Nested
    @DisplayName("getOrdersForWorkstation")
    class GetOrdersForWorkstation {

        @Test
        @DisplayName("Should return orders for workstation 1")
        void shouldReturnOrdersForWorkstation() {
            InjectionMoldingOrder order2 = InjectionMoldingOrder.builder()
                    .id(2L)
                    .orderNumber("IMO-002")
                    .workstationId(1L)
                    .status("IN_PROGRESS")
                    .build();

            when(injectionMoldingOrderRepository.findByWorkstationId(1L))
                    .thenReturn(Arrays.asList(testOrder, order2));

            List<InjectionMoldingOrder> result = injectionMoldingOrderService.getOrdersForWorkstation(1L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(InjectionMoldingOrder::getOrderNumber)
                    .containsExactly("IMO-001", "IMO-002");
            verify(injectionMoldingOrderRepository).findByWorkstationId(1L);
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void shouldReturnEmptyListWhenNoOrders() {
            when(injectionMoldingOrderRepository.findByWorkstationId(1L))
                    .thenReturn(List.of());

            List<InjectionMoldingOrder> result = injectionMoldingOrderService.getOrdersForWorkstation(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOrdersByControlOrder")
    class GetOrdersByControlOrder {

        @Test
        @DisplayName("Should return orders for production control order")
        void shouldReturnOrdersForControlOrder() {
            when(injectionMoldingOrderRepository.findByProductionControlOrderId(100L))
                    .thenReturn(List.of(testOrder));

            List<InjectionMoldingOrder> result = injectionMoldingOrderService.getOrdersByControlOrder(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductionControlOrderId()).isEqualTo(100L);
            verify(injectionMoldingOrderRepository).findByProductionControlOrderId(100L);
        }
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("Should return order when found")
        void shouldReturnOrderWhenFound() {
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            InjectionMoldingOrder result = injectionMoldingOrderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("IMO-001");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenNotFound() {
            when(injectionMoldingOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> injectionMoldingOrderService.getOrderById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Injection molding order not found: 999");
        }
    }

    // ========================================================================
    // Order Lifecycle Tests
    // ========================================================================

    @Nested
    @DisplayName("startOrder")
    class StartOrder {

        @Test
        @DisplayName("Should start PENDING order")
        void shouldStartPendingOrder() {
            testOrder.setStatus("PENDING");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(injectionMoldingOrderRepository.save(any(InjectionMoldingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InjectionMoldingOrder result = injectionMoldingOrderService.startOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getActualStartTime()).isNotNull();
            verify(simalClient).updateTaskStatus(anyString(), eq("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Should throw exception when starting non-PENDING order")
        void shouldThrowExceptionWhenStartingNonPendingOrder() {
            testOrder.setStatus("COMPLETED");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> injectionMoldingOrderService.startOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only start PENDING orders");
        }

        @Test
        @DisplayName("Should throw exception when starting IN_PROGRESS order")
        void shouldThrowExceptionWhenStartingInProgressOrder() {
            testOrder.setStatus("IN_PROGRESS");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> injectionMoldingOrderService.startOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only start PENDING orders");
        }

        @Test
        @DisplayName("Should generate correct SimAL task ID")
        void shouldGenerateCorrectSimalTaskId() {
            testOrder.setStatus("PENDING");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(injectionMoldingOrderRepository.save(any(InjectionMoldingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            injectionMoldingOrderService.startOrder(1L);

            // Verify task ID format: WS-{workstationId}-{orderNumber}
            String expectedTaskId = SimalClient.generateTaskId(1L, "IMO-001");
            verify(simalClient).updateTaskStatus(eq(expectedTaskId), eq("IN_PROGRESS"));
        }
    }

    @Nested
    @DisplayName("completeOrder")
    class CompleteOrder {

        @Test
        @DisplayName("Should complete IN_PROGRESS order and credit inventory")
        void shouldCompleteOrderAndCreditInventory() {
            testOrder.setStatus("IN_PROGRESS");
            testOrder.setActualStartTime(LocalDateTime.now().minusHours(1));
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(injectionMoldingOrderRepository.save(any(InjectionMoldingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InjectionMoldingOrder result = injectionMoldingOrderService.completeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getActualFinishTime()).isNotNull();

            // Verify inventory credit to Modules Supermarket (WS-8)
            verify(inventoryClient).creditPartsToModulesSupermarket(
                    eq(10L),  // outputPartId
                    eq(50),   // quantity
                    eq("IMO-001")  // orderNumber
            );
        }

        @Test
        @DisplayName("Should update SimAL task status on completion")
        void shouldUpdateSimalOnCompletion() {
            testOrder.setStatus("IN_PROGRESS");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(injectionMoldingOrderRepository.save(any(InjectionMoldingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            injectionMoldingOrderService.completeOrder(1L);

            String expectedTaskId = SimalClient.generateTaskId(1L, "IMO-001");
            verify(simalClient).updateTaskStatus(eq(expectedTaskId), eq("COMPLETED"));
        }

        @Test
        @DisplayName("Should propagate status to parent control order")
        void shouldPropagateStatusToParent() {
            testOrder.setStatus("IN_PROGRESS");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(injectionMoldingOrderRepository.save(any(InjectionMoldingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            injectionMoldingOrderService.completeOrder(1L);

            verify(orchestrationService).notifyWorkstationOrderComplete(
                    eq(OrderOrchestrationService.WorkstationOrderType.INJECTION_MOLDING),
                    eq(100L)  // productionControlOrderId
            );
        }

        @Test
        @DisplayName("Should throw exception when completing non-IN_PROGRESS order")
        void shouldThrowExceptionWhenCompletingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> injectionMoldingOrderService.completeOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only complete IN_PROGRESS orders");
        }

        @Test
        @DisplayName("Should throw exception when completing HALTED order")
        void shouldThrowExceptionWhenCompletingHaltedOrder() {
            testOrder.setStatus("HALTED");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> injectionMoldingOrderService.completeOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only complete IN_PROGRESS orders");
        }
    }

    @Nested
    @DisplayName("haltOrder")
    class HaltOrder {

        @Test
        @DisplayName("Should halt IN_PROGRESS order")
        void shouldHaltInProgressOrder() {
            testOrder.setStatus("IN_PROGRESS");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(injectionMoldingOrderRepository.save(any(InjectionMoldingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InjectionMoldingOrder result = injectionMoldingOrderService.haltOrder(1L);

            assertThat(result.getStatus()).isEqualTo("HALTED");
            verify(simalClient).updateTaskStatus(anyString(), eq("HALTED"));
        }

        @Test
        @DisplayName("Should throw exception when halting non-IN_PROGRESS order")
        void shouldThrowExceptionWhenHaltingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> injectionMoldingOrderService.haltOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only halt IN_PROGRESS orders");
        }
    }

    @Nested
    @DisplayName("resumeOrder")
    class ResumeOrder {

        @Test
        @DisplayName("Should resume HALTED order")
        void shouldResumeHaltedOrder() {
            testOrder.setStatus("HALTED");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(injectionMoldingOrderRepository.save(any(InjectionMoldingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InjectionMoldingOrder result = injectionMoldingOrderService.resumeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should throw exception when resuming non-HALTED order")
        void shouldThrowExceptionWhenResumingNonHaltedOrder() {
            testOrder.setStatus("IN_PROGRESS");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> injectionMoldingOrderService.resumeOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only resume HALTED orders");
        }

        @Test
        @DisplayName("Should throw exception when resuming PENDING order")
        void shouldThrowExceptionWhenResumingPendingOrder() {
            testOrder.setStatus("PENDING");
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> injectionMoldingOrderService.resumeOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only resume HALTED orders");
        }
    }

    // ========================================================================
    // Edge Cases and Error Handling
    // ========================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle repository exception gracefully")
        void shouldHandleRepositoryException() {
            when(injectionMoldingOrderRepository.findById(1L))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> injectionMoldingOrderService.getOrderById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }
    }
}
