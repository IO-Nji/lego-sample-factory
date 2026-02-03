package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.entity.PartPreProductionOrder;
import io.life.order.repository.PartPreProductionOrderRepository;
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
 * Unit tests for PartPreProductionOrderService
 * 
 * Tests WS-2 (Parts Pre-Production) workstation order lifecycle:
 * - Order retrieval (by workstation, by control order, by ID)
 * - Order lifecycle (start, complete, halt, resume)
 * - Waiting for parts functionality
 * - Inventory credit on completion
 * - Status validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PartPreProductionOrderService Tests")
class PartPreProductionOrderServiceTest {

    @Mock
    private PartPreProductionOrderRepository partPreProductionOrderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderOrchestrationService orchestrationService;

    @InjectMocks
    private PartPreProductionOrderService partPreProductionOrderService;

    private PartPreProductionOrder testOrder;

    @BeforeEach
    void setUp() {
        testOrder = PartPreProductionOrder.builder()
                .id(1L)
                .orderNumber("PPO-001")
                .productionControlOrderId(100L)
                .workstationId(2L)
                .requiredPartIds("[1, 5, 12]")
                .requiredPartDetails("{\"parts\": [{\"id\": 1, \"qty\": 10}]}")
                .outputPartId(20L)
                .outputPartName("Machined Gear Housing")
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
        @DisplayName("Should return orders for workstation 2")
        void shouldReturnOrdersForWorkstation() {
            PartPreProductionOrder order2 = PartPreProductionOrder.builder()
                    .id(2L)
                    .orderNumber("PPO-002")
                    .workstationId(2L)
                    .status("IN_PROGRESS")
                    .build();

            when(partPreProductionOrderRepository.findByWorkstationId(2L))
                    .thenReturn(Arrays.asList(testOrder, order2));

            List<PartPreProductionOrder> result = partPreProductionOrderService.getOrdersForWorkstation(2L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PartPreProductionOrder::getOrderNumber)
                    .containsExactly("PPO-001", "PPO-002");
            verify(partPreProductionOrderRepository).findByWorkstationId(2L);
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void shouldReturnEmptyListWhenNoOrders() {
            when(partPreProductionOrderRepository.findByWorkstationId(2L))
                    .thenReturn(List.of());

            List<PartPreProductionOrder> result = partPreProductionOrderService.getOrdersForWorkstation(2L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOrdersByControlOrder")
    class GetOrdersByControlOrder {

        @Test
        @DisplayName("Should return orders for production control order")
        void shouldReturnOrdersForControlOrder() {
            when(partPreProductionOrderRepository.findByProductionControlOrderId(100L))
                    .thenReturn(List.of(testOrder));

            List<PartPreProductionOrder> result = partPreProductionOrderService.getOrdersByControlOrder(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductionControlOrderId()).isEqualTo(100L);
            verify(partPreProductionOrderRepository).findByProductionControlOrderId(100L);
        }
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("Should return order when found")
        void shouldReturnOrderWhenFound() {
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            PartPreProductionOrder result = partPreProductionOrderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("PPO-001");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenNotFound() {
            when(partPreProductionOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> partPreProductionOrderService.getOrderById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Part pre-production order not found: 999");
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
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partPreProductionOrderRepository.save(any(PartPreProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartPreProductionOrder result = partPreProductionOrderService.startOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getActualStartTime()).isNotNull();
        }

        @Test
        @DisplayName("Should start WAITING_FOR_PARTS order")
        void shouldStartWaitingForPartsOrder() {
            testOrder.setStatus("WAITING_FOR_PARTS");
            testOrder.setSupplyOrderId(500L);
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partPreProductionOrderRepository.save(any(PartPreProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartPreProductionOrder result = partPreProductionOrderService.startOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getActualStartTime()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when starting COMPLETED order")
        void shouldThrowExceptionWhenStartingCompletedOrder() {
            testOrder.setStatus("COMPLETED");
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partPreProductionOrderService.startOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only start PENDING or WAITING_FOR_PARTS orders");
        }

        @Test
        @DisplayName("Should throw exception when starting IN_PROGRESS order")
        void shouldThrowExceptionWhenStartingInProgressOrder() {
            testOrder.setStatus("IN_PROGRESS");
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partPreProductionOrderService.startOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only start PENDING or WAITING_FOR_PARTS orders");
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
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partPreProductionOrderRepository.save(any(PartPreProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartPreProductionOrder result = partPreProductionOrderService.completeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getActualFinishTime()).isNotNull();

            // Verify inventory credit to Modules Supermarket (WS-8)
            verify(inventoryClient).creditPartsToModulesSupermarket(
                    eq(20L),  // outputPartId
                    eq(50),   // quantity
                    eq("PPO-001")  // orderNumber
            );
        }

        @Test
        @DisplayName("Should propagate status to parent control order")
        void shouldPropagateStatusToParent() {
            testOrder.setStatus("IN_PROGRESS");
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partPreProductionOrderRepository.save(any(PartPreProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            partPreProductionOrderService.completeOrder(1L);

            verify(orchestrationService).notifyWorkstationOrderComplete(
                    eq(OrderOrchestrationService.WorkstationOrderType.PART_PRE_PRODUCTION),
                    eq(100L)  // productionControlOrderId
            );
        }

        @Test
        @DisplayName("Should throw exception when completing non-IN_PROGRESS order")
        void shouldThrowExceptionWhenCompletingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partPreProductionOrderService.completeOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only complete IN_PROGRESS orders");
        }

        @Test
        @DisplayName("Should throw exception when completing WAITING_FOR_PARTS order")
        void shouldThrowExceptionWhenCompletingWaitingOrder() {
            testOrder.setStatus("WAITING_FOR_PARTS");
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partPreProductionOrderService.completeOrder(1L))
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
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partPreProductionOrderRepository.save(any(PartPreProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartPreProductionOrder result = partPreProductionOrderService.haltOrder(1L);

            assertThat(result.getStatus()).isEqualTo("HALTED");
        }

        @Test
        @DisplayName("Should throw exception when halting non-IN_PROGRESS order")
        void shouldThrowExceptionWhenHaltingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partPreProductionOrderService.haltOrder(1L))
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
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partPreProductionOrderRepository.save(any(PartPreProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartPreProductionOrder result = partPreProductionOrderService.resumeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should throw exception when resuming non-HALTED order")
        void shouldThrowExceptionWhenResumingNonHaltedOrder() {
            testOrder.setStatus("IN_PROGRESS");
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partPreProductionOrderService.resumeOrder(1L))
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
            when(partPreProductionOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partPreProductionOrderRepository.save(any(PartPreProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartPreProductionOrder result = partPreProductionOrderService.markWaitingForParts(1L, 500L);

            assertThat(result.getStatus()).isEqualTo("WAITING_FOR_PARTS");
            assertThat(result.getSupplyOrderId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(partPreProductionOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> partPreProductionOrderService.markWaitingForParts(999L, 500L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Part pre-production order not found");
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
            when(partPreProductionOrderRepository.findById(1L))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> partPreProductionOrderService.getOrderById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }
    }
}
