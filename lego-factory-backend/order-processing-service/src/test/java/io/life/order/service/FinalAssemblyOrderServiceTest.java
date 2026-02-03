package io.life.order.service;

import io.life.order.config.OrderProcessingConfig;
import io.life.order.dto.FinalAssemblyOrderDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.FinalAssemblyOrder;
import io.life.order.entity.ProductionOrder;
import io.life.order.entity.WarehouseOrder;
import io.life.order.repository.CustomerOrderRepository;
import io.life.order.repository.FinalAssemblyOrderRepository;
import io.life.order.repository.ProductionOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FinalAssemblyOrderService
 * 
 * Test Coverage:
 * - Order creation from WarehouseOrder (Scenario 2/3)
 * - Order creation from ProductionOrder (Scenario 4)
 * - 4-step workflow: confirm → start → complete → submit
 * - Status transitions and validation
 * - Inventory credit on submit (Plant Warehouse WS-7)
 * - Cascade updates to parent orders
 * 
 * @see _dev-docs/TEST_PLAN.md for test case specifications
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FinalAssemblyOrderService Tests")
class FinalAssemblyOrderServiceTest {

    @Mock
    private OrderProcessingConfig config;

    @Mock
    private OrderProcessingConfig.Workstations workstations;

    @Mock
    private OrderProcessingConfig.OrderNumbers orderNumbers;

    @Mock
    private FinalAssemblyOrderRepository finalAssemblyOrderRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private WarehouseOrderRepository warehouseOrderRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderAuditService orderAuditService;

    @InjectMocks
    private FinalAssemblyOrderService finalAssemblyOrderService;

    // Test fixtures
    private FinalAssemblyOrder testOrder;
    private WarehouseOrder testWarehouseOrder;
    private ProductionOrder testProductionOrder;
    private CustomerOrder testCustomerOrder;

    @BeforeEach
    void setUp() {
        // Configure mock config
        when(config.getWorkstations()).thenReturn(workstations);
        when(config.getOrderNumbers()).thenReturn(orderNumbers);
        when(workstations.getFinalAssembly()).thenReturn(6L);
        when(workstations.getPlantWarehouse()).thenReturn(7L);
        when(orderNumbers.getFinalAssemblyOrderPrefix()).thenReturn("FA-");

        // Setup test order
        testOrder = new FinalAssemblyOrder();
        testOrder.setId(1L);
        testOrder.setOrderNumber("FA-TEST001");
        testOrder.setWarehouseOrderId(50L);
        testOrder.setWorkstationId(6L);
        testOrder.setOutputProductId(1L);
        testOrder.setOutputQuantity(5);
        testOrder.setStatus("PENDING");
        testOrder.setOrderDate(LocalDateTime.now());

        // Setup test warehouse order
        testWarehouseOrder = new WarehouseOrder();
        testWarehouseOrder.setId(50L);
        testWarehouseOrder.setOrderNumber("WO-TEST001");
        testWarehouseOrder.setCustomerOrderId(100L);
        testWarehouseOrder.setStatus("CONFIRMED");
        testWarehouseOrder.setWorkstationId(8L);

        // Setup test production order (for Scenario 4)
        testProductionOrder = ProductionOrder.builder()
                .id(10L)
                .productionOrderNumber("PO-TEST001")
                .sourceCustomerOrderId(100L)
                .status("COMPLETED")
                .build();

        // Setup test customer order
        testCustomerOrder = new CustomerOrder();
        testCustomerOrder.setId(100L);
        testCustomerOrder.setOrderNumber("CO-TEST001");
        testCustomerOrder.setStatus("PROCESSING");
    }

    // ========================================
    // Order Retrieval Tests
    // ========================================
    @Nested
    @DisplayName("Order Retrieval Tests")
    class OrderRetrievalTests {

        @Test
        @DisplayName("getAllOrders returns all orders")
        void getAllOrders_ReturnsAllOrders() {
            // Given
            when(finalAssemblyOrderRepository.findAll()).thenReturn(List.of(testOrder));

            // When
            List<FinalAssemblyOrderDTO> result = finalAssemblyOrderService.getAllOrders();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrderNumber()).isEqualTo("FA-TEST001");
        }

        @Test
        @DisplayName("getOrderById returns order when found")
        void getOrderById_Found_ReturnsOrder() {
            // Given
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When
            Optional<FinalAssemblyOrderDTO> result = finalAssemblyOrderService.getOrderById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getOrderNumber()).isEqualTo("FA-TEST001");
        }

        @Test
        @DisplayName("getOrderById returns empty when not found")
        void getOrderById_NotFound_ReturnsEmpty() {
            // Given
            when(finalAssemblyOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<FinalAssemblyOrderDTO> result = finalAssemblyOrderService.getOrderById(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getOrdersByWorkstationId returns filtered orders")
        void getOrdersByWorkstationId_ReturnsFiltered() {
            // Given
            when(finalAssemblyOrderRepository.findByWorkstationId(6L)).thenReturn(List.of(testOrder));

            // When
            List<FinalAssemblyOrderDTO> result = finalAssemblyOrderService.getOrdersByWorkstationId(6L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkstationId()).isEqualTo(6L);
        }

        @Test
        @DisplayName("getOrdersByStatus returns filtered orders")
        void getOrdersByStatus_ReturnsFiltered() {
            // Given
            when(finalAssemblyOrderRepository.findByStatus("PENDING")).thenReturn(List.of(testOrder));

            // When
            List<FinalAssemblyOrderDTO> result = finalAssemblyOrderService.getOrdersByStatus("PENDING");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("getOrdersByWarehouseOrderId returns filtered orders")
        void getOrdersByWarehouseOrderId_ReturnsFiltered() {
            // Given
            when(finalAssemblyOrderRepository.findByWarehouseOrderId(50L)).thenReturn(List.of(testOrder));

            // When
            List<FinalAssemblyOrderDTO> result = finalAssemblyOrderService.getOrdersByWarehouseOrderId(50L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWarehouseOrderId()).isEqualTo(50L);
        }
    }

    // ========================================
    // Order Creation Tests
    // ========================================
    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {

        @Test
        @DisplayName("Create from WarehouseOrder creates order correctly")
        void createFromWarehouseOrder_Success() {
            // Given
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> {
                        FinalAssemblyOrder order = inv.getArgument(0);
                        order.setId(1L);
                        return order;
                    });

            // When
            FinalAssemblyOrderDTO result = finalAssemblyOrderService.createFromWarehouseOrder(
                    testWarehouseOrder, 1L, 5);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWarehouseOrderId()).isEqualTo(50L);
            assertThat(result.getOutputProductId()).isEqualTo(1L);
            assertThat(result.getOutputQuantity()).isEqualTo(5);
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getWorkstationId()).isEqualTo(6L);
        }

        @Test
        @DisplayName("Create from WarehouseOrder records audit event")
        void createFromWarehouseOrder_RecordsAudit() {
            // Given
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> {
                        FinalAssemblyOrder order = inv.getArgument(0);
                        order.setId(1L);
                        return order;
                    });

            // When
            finalAssemblyOrderService.createFromWarehouseOrder(testWarehouseOrder, 1L, 5);

            // Then
            verify(orderAuditService).recordOrderEvent(
                    eq("FINAL_ASSEMBLY"), eq(1L), eq("CREATED"), anyString());
        }

        @Test
        @DisplayName("Create from ProductionOrder creates order correctly")
        void createFromProductionOrder_Success() {
            // Given
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> {
                        FinalAssemblyOrder order = inv.getArgument(0);
                        order.setId(1L);
                        return order;
                    });

            // When
            FinalAssemblyOrderDTO result = finalAssemblyOrderService.createFromProductionOrder(
                    testProductionOrder, 2L, 3);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOutputProductId()).isEqualTo(2L);
            assertThat(result.getOutputQuantity()).isEqualTo(3);
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }
    }

    // ========================================
    // Confirm Order Tests
    // ========================================
    @Nested
    @DisplayName("Confirm Order Tests")
    class ConfirmOrderTests {

        @Test
        @DisplayName("Confirm PENDING order changes status to CONFIRMED")
        void confirmOrder_Success() {
            // Given
            testOrder.setStatus("PENDING");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            FinalAssemblyOrderDTO result = finalAssemblyOrderService.confirmOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("Confirm non-PENDING order throws exception")
        void confirmOrder_NonPending_ThrowsException() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.confirmOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only PENDING orders can be confirmed");
        }

        @Test
        @DisplayName("Confirm non-existent order throws exception")
        void confirmOrder_NotFound_ThrowsException() {
            // Given
            when(finalAssemblyOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.confirmOrder(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Final Assembly order not found");
        }
    }

    // ========================================
    // Start Order Tests
    // ========================================
    @Nested
    @DisplayName("Start Order Tests")
    class StartOrderTests {

        @Test
        @DisplayName("Start CONFIRMED order changes status to IN_PROGRESS")
        void startOrder_Success() {
            // Given
            testOrder.setStatus("CONFIRMED");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            FinalAssemblyOrderDTO result = finalAssemblyOrderService.startOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getStartTime()).isNotNull();
        }

        @Test
        @DisplayName("Start non-CONFIRMED order throws exception")
        void startOrder_NonConfirmed_ThrowsException() {
            // Given
            testOrder.setStatus("PENDING");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.startOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only CONFIRMED orders can be started");
        }

        @Test
        @DisplayName("Start non-existent order throws exception")
        void startOrder_NotFound_ThrowsException() {
            // Given
            when(finalAssemblyOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.startOrder(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Final Assembly order not found");
        }
    }

    // ========================================
    // Complete Order Tests
    // ========================================
    @Nested
    @DisplayName("Complete Order Tests")
    class CompleteOrderTests {

        @Test
        @DisplayName("Complete IN_PROGRESS order changes status to COMPLETED")
        void completeOrder_Success() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            FinalAssemblyOrderDTO result = finalAssemblyOrderService.completeOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getCompletionTime()).isNotNull();
        }

        @Test
        @DisplayName("Complete non-IN_PROGRESS order throws exception")
        void completeOrder_NonInProgress_ThrowsException() {
            // Given
            testOrder.setStatus("CONFIRMED");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.completeOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only IN_PROGRESS orders can be completed");
        }

        @Test
        @DisplayName("Complete order does NOT credit inventory (happens on submit)")
        void completeOrder_DoesNotCreditInventory() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            finalAssemblyOrderService.completeOrder(1L);

            // Then - inventory credit happens on submit, not complete
            verify(inventoryService, never()).creditStock(anyLong(), anyLong(), anyInt());
        }
    }

    // ========================================
    // Submit Order Tests
    // ========================================
    @Nested
    @DisplayName("Submit Order Tests")
    class SubmitOrderTests {

        @Test
        @DisplayName("Submit COMPLETED order changes status to SUBMITTED")
        void submitOrder_Success() {
            // Given
            testOrder.setStatus("COMPLETED");
            testOrder.setWarehouseOrderId(null); // No cascade update
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(inventoryService.creditStock(7L, 1L, 5)).thenReturn(true);

            // When
            FinalAssemblyOrderDTO result = finalAssemblyOrderService.submitOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("SUBMITTED");
            assertThat(result.getSubmitTime()).isNotNull();
        }

        @Test
        @DisplayName("Submit order credits Plant Warehouse (WS-7)")
        void submitOrder_CreditsPlantWarehouse() {
            // Given
            testOrder.setStatus("COMPLETED");
            testOrder.setWarehouseOrderId(null); // No cascade update
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(inventoryService.creditStock(7L, 1L, 5)).thenReturn(true);

            // When
            finalAssemblyOrderService.submitOrder(1L);

            // Then
            verify(inventoryService).creditStock(7L, 1L, 5);
        }

        @Test
        @DisplayName("Submit non-COMPLETED order throws exception")
        void submitOrder_NonCompleted_ThrowsException() {
            // Given
            testOrder.setStatus("IN_PROGRESS");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.submitOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only COMPLETED orders can be submitted");
        }

        @Test
        @DisplayName("Submit order with failed credit throws exception")
        void submitOrder_CreditFailed_ThrowsException() {
            // Given
            testOrder.setStatus("COMPLETED");
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.creditStock(7L, 1L, 5)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.submitOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to credit Plant Warehouse");
        }
    }

    // ========================================
    // Cascade Update Tests
    // ========================================
    @Nested
    @DisplayName("Cascade Update Tests")
    class CascadeUpdateTests {

        @Test
        @DisplayName("Submit last FA order updates customer order status")
        void submitOrder_AllSubmitted_UpdatesCustomerOrder() {
            // Given
            testOrder.setStatus("COMPLETED");
            testOrder.setWarehouseOrderId(50L);
            FinalAssemblyOrder submittedOrder = new FinalAssemblyOrder();
            submittedOrder.setStatus("SUBMITTED");
            
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> {
                        FinalAssemblyOrder order = inv.getArgument(0);
                        order.setStatus("SUBMITTED");
                        return order;
                    });
            when(inventoryService.creditStock(7L, 1L, 5)).thenReturn(true);
            
            // All related orders are now submitted
            when(finalAssemblyOrderRepository.findByWarehouseOrderId(50L))
                    .thenReturn(List.of(testOrder)); // testOrder will be SUBMITTED after save
            when(warehouseOrderRepository.findById(50L)).thenReturn(Optional.of(testWarehouseOrder));
            when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(testCustomerOrder));

            // When
            finalAssemblyOrderService.submitOrder(1L);

            // Then - Customer order should be updated
            ArgumentCaptor<CustomerOrder> captor = ArgumentCaptor.forClass(CustomerOrder.class);
            verify(customerOrderRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("CONFIRMED");
            assertThat(captor.getValue().getTriggerScenario()).isEqualTo("DIRECT_FULFILLMENT");
        }

        @Test
        @DisplayName("Submit non-last FA order does not update customer order")
        void submitOrder_NotAllSubmitted_NoCustomerOrderUpdate() {
            // Given
            testOrder.setStatus("COMPLETED");
            testOrder.setWarehouseOrderId(50L);
            
            FinalAssemblyOrder pendingOrder = new FinalAssemblyOrder();
            pendingOrder.setStatus("PENDING");
            
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> {
                        FinalAssemblyOrder order = inv.getArgument(0);
                        order.setStatus("SUBMITTED");
                        return order;
                    });
            when(inventoryService.creditStock(7L, 1L, 5)).thenReturn(true);
            
            // Not all related orders are submitted
            when(finalAssemblyOrderRepository.findByWarehouseOrderId(50L))
                    .thenReturn(List.of(testOrder, pendingOrder));

            // When
            finalAssemblyOrderService.submitOrder(1L);

            // Then - Customer order should NOT be updated
            verify(customerOrderRepository, never()).save(any());
        }
    }

    // ========================================
    // Update Status Tests
    // ========================================
    @Nested
    @DisplayName("Update Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Update status changes status correctly")
        void updateStatus_Success() {
            // Given
            when(finalAssemblyOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(finalAssemblyOrderRepository.save(any(FinalAssemblyOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            FinalAssemblyOrderDTO result = finalAssemblyOrderService.updateOrderStatus(1L, "IN_PROGRESS");

            // Then
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Update status on non-existent order throws exception")
        void updateStatus_NotFound_ThrowsException() {
            // Given
            when(finalAssemblyOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> finalAssemblyOrderService.updateOrderStatus(999L, "COMPLETED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Final Assembly order not found");
        }
    }

    // ========================================
    // Helper Method Tests
    // ========================================
    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("areAllOrdersSubmittedForWarehouseOrder returns true when all submitted")
        void areAllSubmitted_AllSubmitted_ReturnsTrue() {
            // Given
            FinalAssemblyOrder submitted1 = new FinalAssemblyOrder();
            submitted1.setStatus("SUBMITTED");
            FinalAssemblyOrder submitted2 = new FinalAssemblyOrder();
            submitted2.setStatus("SUBMITTED");
            
            when(finalAssemblyOrderRepository.findByWarehouseOrderId(50L))
                    .thenReturn(List.of(submitted1, submitted2));

            // When
            boolean result = finalAssemblyOrderService.areAllOrdersSubmittedForWarehouseOrder(50L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("areAllOrdersSubmittedForWarehouseOrder returns false when some pending")
        void areAllSubmitted_SomePending_ReturnsFalse() {
            // Given
            FinalAssemblyOrder submitted = new FinalAssemblyOrder();
            submitted.setStatus("SUBMITTED");
            FinalAssemblyOrder pending = new FinalAssemblyOrder();
            pending.setStatus("PENDING");
            
            when(finalAssemblyOrderRepository.findByWarehouseOrderId(50L))
                    .thenReturn(List.of(submitted, pending));

            // When
            boolean result = finalAssemblyOrderService.areAllOrdersSubmittedForWarehouseOrder(50L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("areAllOrdersSubmittedForWarehouseOrder returns false when no orders")
        void areAllSubmitted_NoOrders_ReturnsFalse() {
            // Given
            when(finalAssemblyOrderRepository.findByWarehouseOrderId(50L))
                    .thenReturn(Collections.emptyList());

            // When
            boolean result = finalAssemblyOrderService.areAllOrdersSubmittedForWarehouseOrder(50L);

            // Then
            assertThat(result).isFalse();
        }
    }
}
