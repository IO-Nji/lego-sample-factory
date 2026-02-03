package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.client.SimalClient;
import io.life.order.entity.MotorAssemblyOrder;
import io.life.order.repository.MotorAssemblyOrderRepository;
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
 * Unit tests for MotorAssemblyOrderService
 * 
 * Tests WS-5 (Motor Assembly) workstation order lifecycle:
 * - Order retrieval (by workstation, by assembly control order, by ID)
 * - Order lifecycle (start, complete, halt, resume)
 * - Waiting for parts functionality
 * - Inventory credit on completion (credits MODULE to WS-8)
 * - SimAL integration updates
 * - Status validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MotorAssemblyOrderService Tests")
class MotorAssemblyOrderServiceTest {

    @Mock
    private MotorAssemblyOrderRepository motorAssemblyOrderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private SimalClient simalClient;

    @Mock
    private OrderOrchestrationService orchestrationService;

    @InjectMocks
    private MotorAssemblyOrderService motorAssemblyOrderService;

    private MotorAssemblyOrder testOrder;

    @BeforeEach
    void setUp() {
        testOrder = MotorAssemblyOrder.builder()
                .id(1L)
                .orderNumber("MAO-001")
                .assemblyControlOrderId(200L)
                .workstationId(5L)
                .requiredPartIds("[30, 31, 32]")
                .requiredPartDetails("{\"parts\": [{\"id\": 30, \"name\": \"Motor\", \"qty\": 5}]}")
                .outputModuleId(60L)
                .outputModuleName("Motor Module Assembly")
                .quantity(25)
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
        @DisplayName("Should return orders for workstation 5")
        void shouldReturnOrdersForWorkstation() {
            MotorAssemblyOrder order2 = MotorAssemblyOrder.builder()
                    .id(2L)
                    .orderNumber("MAO-002")
                    .workstationId(5L)
                    .status("IN_PROGRESS")
                    .build();

            when(motorAssemblyOrderRepository.findByWorkstationId(5L))
                    .thenReturn(Arrays.asList(testOrder, order2));

            List<MotorAssemblyOrder> result = motorAssemblyOrderService.getOrdersForWorkstation(5L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(MotorAssemblyOrder::getOrderNumber)
                    .containsExactly("MAO-001", "MAO-002");
            verify(motorAssemblyOrderRepository).findByWorkstationId(5L);
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void shouldReturnEmptyListWhenNoOrders() {
            when(motorAssemblyOrderRepository.findByWorkstationId(5L))
                    .thenReturn(List.of());

            List<MotorAssemblyOrder> result = motorAssemblyOrderService.getOrdersForWorkstation(5L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOrdersByControlOrder")
    class GetOrdersByControlOrder {

        @Test
        @DisplayName("Should return orders for assembly control order")
        void shouldReturnOrdersForControlOrder() {
            when(motorAssemblyOrderRepository.findByAssemblyControlOrderId(200L))
                    .thenReturn(List.of(testOrder));

            List<MotorAssemblyOrder> result = motorAssemblyOrderService.getOrdersByControlOrder(200L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssemblyControlOrderId()).isEqualTo(200L);
            verify(motorAssemblyOrderRepository).findByAssemblyControlOrderId(200L);
        }
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("Should return order when found")
        void shouldReturnOrderWhenFound() {
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            MotorAssemblyOrder result = motorAssemblyOrderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("MAO-001");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenNotFound() {
            when(motorAssemblyOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> motorAssemblyOrderService.getOrderById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Motor assembly order not found: 999");
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
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MotorAssemblyOrder result = motorAssemblyOrderService.startOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getActualStartTime()).isNotNull();
            verify(simalClient).updateTaskStatus(anyString(), eq("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Should start WAITING_FOR_PARTS order")
        void shouldStartWaitingForPartsOrder() {
            testOrder.setStatus("WAITING_FOR_PARTS");
            testOrder.setSupplyOrderId(500L);
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MotorAssemblyOrder result = motorAssemblyOrderService.startOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getActualStartTime()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when starting COMPLETED order")
        void shouldThrowExceptionWhenStartingCompletedOrder() {
            testOrder.setStatus("COMPLETED");
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> motorAssemblyOrderService.startOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only start PENDING or WAITING_FOR_PARTS orders");
        }

        @Test
        @DisplayName("Should generate correct SimAL task ID for WS-5")
        void shouldGenerateCorrectSimalTaskId() {
            testOrder.setStatus("PENDING");
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            motorAssemblyOrderService.startOrder(1L);

            String expectedTaskId = SimalClient.generateTaskId(5L, "MAO-001");
            verify(simalClient).updateTaskStatus(eq(expectedTaskId), eq("IN_PROGRESS"));
        }
    }

    @Nested
    @DisplayName("completeOrder")
    class CompleteOrder {

        @Test
        @DisplayName("Should complete IN_PROGRESS order and credit MODULE inventory")
        void shouldCompleteOrderAndCreditInventory() {
            testOrder.setStatus("IN_PROGRESS");
            testOrder.setActualStartTime(LocalDateTime.now().minusHours(1));
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MotorAssemblyOrder result = motorAssemblyOrderService.completeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getActualFinishTime()).isNotNull();

            // Verify inventory credit - WS-5 produces MODULES (not parts)
            verify(inventoryClient).creditModulesToModulesSupermarket(
                    eq(60L),  // outputModuleId
                    eq(25),   // quantity
                    eq("MAO-001")  // orderNumber
            );
        }

        @Test
        @DisplayName("Should update SimAL task status on completion")
        void shouldUpdateSimalOnCompletion() {
            testOrder.setStatus("IN_PROGRESS");
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            motorAssemblyOrderService.completeOrder(1L);

            String expectedTaskId = SimalClient.generateTaskId(5L, "MAO-001");
            verify(simalClient).updateTaskStatus(eq(expectedTaskId), eq("COMPLETED"));
        }

        @Test
        @DisplayName("Should propagate status to parent assembly control order")
        void shouldPropagateStatusToParent() {
            testOrder.setStatus("IN_PROGRESS");
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            motorAssemblyOrderService.completeOrder(1L);

            verify(orchestrationService).notifyWorkstationOrderComplete(
                    eq(OrderOrchestrationService.WorkstationOrderType.MOTOR_ASSEMBLY),
                    eq(200L)  // assemblyControlOrderId
            );
        }

        @Test
        @DisplayName("Should throw exception when completing non-IN_PROGRESS order")
        void shouldThrowExceptionWhenCompletingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> motorAssemblyOrderService.completeOrder(1L))
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
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MotorAssemblyOrder result = motorAssemblyOrderService.haltOrder(1L);

            assertThat(result.getStatus()).isEqualTo("HALTED");
            verify(simalClient).updateTaskStatus(anyString(), eq("HALTED"));
        }

        @Test
        @DisplayName("Should throw exception when halting non-IN_PROGRESS order")
        void shouldThrowExceptionWhenHaltingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> motorAssemblyOrderService.haltOrder(1L))
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
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MotorAssemblyOrder result = motorAssemblyOrderService.resumeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should throw exception when resuming non-HALTED order")
        void shouldThrowExceptionWhenResumingNonHaltedOrder() {
            testOrder.setStatus("IN_PROGRESS");
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> motorAssemblyOrderService.resumeOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only resume HALTED orders");
        }
    }

    // ========================================================================
    // Waiting for Parts Tests
    // ========================================================================

    @Nested
    @DisplayName("markWaitingForParts")
    class MarkWaitingForParts {

        @Test
        @DisplayName("Should mark order as waiting for parts with supply order ID")
        void shouldMarkOrderAsWaitingForParts() {
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(motorAssemblyOrderRepository.save(any(MotorAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MotorAssemblyOrder result = motorAssemblyOrderService.markWaitingForParts(1L, 500L);

            assertThat(result.getStatus()).isEqualTo("WAITING_FOR_PARTS");
            assertThat(result.getSupplyOrderId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(motorAssemblyOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> motorAssemblyOrderService.markWaitingForParts(999L, 500L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Motor assembly order not found");
        }
    }

    // ========================================================================
    // Error Handling
    // ========================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle repository exception gracefully")
        void shouldHandleRepositoryException() {
            when(motorAssemblyOrderRepository.findById(1L))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> motorAssemblyOrderService.getOrderById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }
    }
}
