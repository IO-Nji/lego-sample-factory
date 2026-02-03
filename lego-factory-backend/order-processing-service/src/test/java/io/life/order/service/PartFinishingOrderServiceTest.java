package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.client.SimalClient;
import io.life.order.entity.PartFinishingOrder;
import io.life.order.repository.PartFinishingOrderRepository;
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
 * Unit tests for PartFinishingOrderService
 * 
 * Tests WS-3 (Part Finishing) workstation order lifecycle:
 * - Order retrieval (by workstation, by control order, by ID)
 * - Order lifecycle (start, complete, halt, resume)
 * - Waiting for parts functionality
 * - Inventory credit on completion
 * - SimAL integration updates
 * - Status validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PartFinishingOrderService Tests")
class PartFinishingOrderServiceTest {

    @Mock
    private PartFinishingOrderRepository partFinishingOrderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private SimalClient simalClient;

    @Mock
    private OrderOrchestrationService orchestrationService;

    @InjectMocks
    private PartFinishingOrderService partFinishingOrderService;

    private PartFinishingOrder testOrder;

    @BeforeEach
    void setUp() {
        testOrder = PartFinishingOrder.builder()
                .id(1L)
                .orderNumber("PFO-001")
                .productionControlOrderId(100L)
                .workstationId(3L)
                .requiredPartIds("[20, 21]")
                .requiredPartDetails("{\"parts\": [{\"id\": 20, \"qty\": 10}]}")
                .outputPartId(30L)
                .outputPartName("Polished Gear Housing")
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
        @DisplayName("Should return orders for workstation 3")
        void shouldReturnOrdersForWorkstation() {
            PartFinishingOrder order2 = PartFinishingOrder.builder()
                    .id(2L)
                    .orderNumber("PFO-002")
                    .workstationId(3L)
                    .status("IN_PROGRESS")
                    .build();

            when(partFinishingOrderRepository.findByWorkstationId(3L))
                    .thenReturn(Arrays.asList(testOrder, order2));

            List<PartFinishingOrder> result = partFinishingOrderService.getOrdersForWorkstation(3L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PartFinishingOrder::getOrderNumber)
                    .containsExactly("PFO-001", "PFO-002");
            verify(partFinishingOrderRepository).findByWorkstationId(3L);
        }

        @Test
        @DisplayName("Should return empty list when no orders exist")
        void shouldReturnEmptyListWhenNoOrders() {
            when(partFinishingOrderRepository.findByWorkstationId(3L))
                    .thenReturn(List.of());

            List<PartFinishingOrder> result = partFinishingOrderService.getOrdersForWorkstation(3L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOrdersByControlOrder")
    class GetOrdersByControlOrder {

        @Test
        @DisplayName("Should return orders for production control order")
        void shouldReturnOrdersForControlOrder() {
            when(partFinishingOrderRepository.findByProductionControlOrderId(100L))
                    .thenReturn(List.of(testOrder));

            List<PartFinishingOrder> result = partFinishingOrderService.getOrdersByControlOrder(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductionControlOrderId()).isEqualTo(100L);
            verify(partFinishingOrderRepository).findByProductionControlOrderId(100L);
        }
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("Should return order when found")
        void shouldReturnOrderWhenFound() {
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            PartFinishingOrder result = partFinishingOrderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("PFO-001");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenNotFound() {
            when(partFinishingOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> partFinishingOrderService.getOrderById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Part finishing order not found: 999");
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
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartFinishingOrder result = partFinishingOrderService.startOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getActualStartTime()).isNotNull();
            verify(simalClient).updateTaskStatus(anyString(), eq("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Should start WAITING_FOR_PARTS order")
        void shouldStartWaitingForPartsOrder() {
            testOrder.setStatus("WAITING_FOR_PARTS");
            testOrder.setSupplyOrderId(500L);
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartFinishingOrder result = partFinishingOrderService.startOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getActualStartTime()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when starting COMPLETED order")
        void shouldThrowExceptionWhenStartingCompletedOrder() {
            testOrder.setStatus("COMPLETED");
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partFinishingOrderService.startOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Can only start PENDING or WAITING_FOR_PARTS orders");
        }

        @Test
        @DisplayName("Should generate correct SimAL task ID for WS-3")
        void shouldGenerateCorrectSimalTaskId() {
            testOrder.setStatus("PENDING");
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            partFinishingOrderService.startOrder(1L);

            String expectedTaskId = SimalClient.generateTaskId(3L, "PFO-001");
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
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartFinishingOrder result = partFinishingOrderService.completeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getActualFinishTime()).isNotNull();

            // Verify inventory credit to Modules Supermarket (WS-8)
            verify(inventoryClient).creditPartsToModulesSupermarket(
                    eq(30L),  // outputPartId
                    eq(50),   // quantity
                    eq("PFO-001")  // orderNumber
            );
        }

        @Test
        @DisplayName("Should update SimAL task status on completion")
        void shouldUpdateSimalOnCompletion() {
            testOrder.setStatus("IN_PROGRESS");
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            partFinishingOrderService.completeOrder(1L);

            String expectedTaskId = SimalClient.generateTaskId(3L, "PFO-001");
            verify(simalClient).updateTaskStatus(eq(expectedTaskId), eq("COMPLETED"));
        }

        @Test
        @DisplayName("Should propagate status to parent control order")
        void shouldPropagateStatusToParent() {
            testOrder.setStatus("IN_PROGRESS");
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            partFinishingOrderService.completeOrder(1L);

            verify(orchestrationService).notifyWorkstationOrderComplete(
                    eq(OrderOrchestrationService.WorkstationOrderType.PART_FINISHING),
                    eq(100L)  // productionControlOrderId
            );
        }

        @Test
        @DisplayName("Should throw exception when completing non-IN_PROGRESS order")
        void shouldThrowExceptionWhenCompletingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partFinishingOrderService.completeOrder(1L))
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
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartFinishingOrder result = partFinishingOrderService.haltOrder(1L);

            assertThat(result.getStatus()).isEqualTo("HALTED");
            verify(simalClient).updateTaskStatus(anyString(), eq("HALTED"));
        }

        @Test
        @DisplayName("Should throw exception when halting non-IN_PROGRESS order")
        void shouldThrowExceptionWhenHaltingNonInProgressOrder() {
            testOrder.setStatus("PENDING");
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partFinishingOrderService.haltOrder(1L))
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
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartFinishingOrder result = partFinishingOrderService.resumeOrder(1L);

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should throw exception when resuming non-HALTED order")
        void shouldThrowExceptionWhenResumingNonHaltedOrder() {
            testOrder.setStatus("IN_PROGRESS");
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> partFinishingOrderService.resumeOrder(1L))
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
            when(partFinishingOrderRepository.findById(1L))
                    .thenReturn(Optional.of(testOrder));
            when(partFinishingOrderRepository.save(any(PartFinishingOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PartFinishingOrder result = partFinishingOrderService.markWaitingForParts(1L, 500L);

            assertThat(result.getStatus()).isEqualTo("WAITING_FOR_PARTS");
            assertThat(result.getSupplyOrderId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(partFinishingOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> partFinishingOrderService.markWaitingForParts(999L, 500L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Part finishing order not found");
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
            when(partFinishingOrderRepository.findById(1L))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> partFinishingOrderService.getOrderById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }
    }
}
