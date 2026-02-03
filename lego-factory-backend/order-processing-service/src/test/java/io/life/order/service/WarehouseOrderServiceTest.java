package io.life.order.service;

import io.life.order.dto.FinalAssemblyOrderDTO;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.dto.WarehouseOrderDTO;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.repository.WarehouseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Unit tests for WarehouseOrderService
 * 
 * Test Coverage:
 * - Order confirmation and triggerScenario logic (WOS-001 to WOS-005)
 * - Fulfillment flow scenarios (WOS-006 to WOS-009)
 * - Final Assembly order creation (WOS-010 to WOS-013)
 * - Production order triggering (WOS-014 to WOS-016)
 * 
 * @see _dev-docs/TEST_PLAN.md for test case specifications
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseOrderService Tests")
class WarehouseOrderServiceTest {

    @Mock
    private WarehouseOrderRepository warehouseOrderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ProductionOrderService productionOrderService;

    @Mock
    private OrderAuditService orderAuditService;

    @Mock
    private FinalAssemblyOrderService finalAssemblyOrderService;

    @InjectMocks
    private WarehouseOrderService warehouseOrderService;

    // Test fixtures
    private WarehouseOrder testOrder;
    private WarehouseOrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        // Setup test order entity
        testOrder = new WarehouseOrder();
        testOrder.setId(1L);
        testOrder.setOrderNumber("WO-TEST001");
        testOrder.setCustomerOrderId(100L);
        testOrder.setWorkstationId(8L); // Modules Supermarket
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setStatus("PENDING");
        testOrder.setCreatedAt(LocalDateTime.now());

        // Setup test order item
        testOrderItem = new WarehouseOrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setItemId(10L); // Module ID
        testOrderItem.setItemType("MODULE");
        testOrderItem.setRequestedQuantity(5);
        testOrderItem.setFulfilledQuantity(0);
        testOrderItem.setProductId(1L); // Target product ID
        testOrderItem.setWarehouseOrder(testOrder);

        List<WarehouseOrderItem> items = new ArrayList<>();
        items.add(testOrderItem);
        testOrder.setOrderItems(items);
    }

    // ========================================
    // Retrieval Tests
    // ========================================
    @Nested
    @DisplayName("Order Retrieval Tests")
    class OrderRetrievalTests {

        @Test
        @DisplayName("getAllWarehouseOrders returns all orders")
        void getAllWarehouseOrders_ReturnsAllOrders() {
            // Given
            List<WarehouseOrder> orders = List.of(testOrder);
            when(warehouseOrderRepository.findAll()).thenReturn(orders);

            // When
            List<WarehouseOrderDTO> result = warehouseOrderService.getAllWarehouseOrders();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrderNumber()).isEqualTo("WO-TEST001");
            verify(warehouseOrderRepository).findAll();
        }

        @Test
        @DisplayName("getWarehouseOrderById returns order when found")
        void getWarehouseOrderById_Found_ReturnsOrder() {
            // Given
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When
            Optional<WarehouseOrderDTO> result = warehouseOrderService.getWarehouseOrderById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getOrderNumber()).isEqualTo("WO-TEST001");
        }

        @Test
        @DisplayName("getWarehouseOrderById returns empty when not found")
        void getWarehouseOrderById_NotFound_ReturnsEmpty() {
            // Given
            when(warehouseOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<WarehouseOrderDTO> result = warehouseOrderService.getWarehouseOrderById(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getWarehouseOrdersByStatus returns filtered orders")
        void getWarehouseOrdersByStatus_ReturnsFilteredOrders() {
            // Given
            when(warehouseOrderRepository.findByStatus("PENDING")).thenReturn(List.of(testOrder));

            // When
            List<WarehouseOrderDTO> result = warehouseOrderService.getWarehouseOrdersByStatus("PENDING");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
        }
        
        @Test
        @DisplayName("getWarehouseOrdersByFulfillingWorkstationId returns orders for workstation")
        void getWarehouseOrdersByFulfillingWorkstationId_ReturnsOrders() {
            // Given
            when(warehouseOrderRepository.findByWorkstationId(8L)).thenReturn(List.of(testOrder));

            // When
            List<WarehouseOrderDTO> result = warehouseOrderService.getWarehouseOrdersByFulfillingWorkstationId(8L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkstationId()).isEqualTo(8L);
        }
    }

    // ========================================
    // Confirmation Tests (WOS-001 to WOS-005)
    // ========================================
    @Nested
    @DisplayName("Order Confirmation Tests (WOS-001 to WOS-005)")
    class OrderConfirmationTests {

        @Test
        @DisplayName("WOS-001: Confirm order with sufficient stock sets DIRECT_FULFILLMENT")
        void confirmOrder_SufficientStock_SetsDirectFulfillment() {
            // Given
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.confirmWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            assertThat(result.getTriggerScenario()).isEqualTo("DIRECT_FULFILLMENT");
            // checkStock is called during confirmation AND during mapToDTO (dynamic recalculation)
            verify(inventoryService, atLeast(1)).checkStock(8L, 10L, 5);
            verify(orderAuditService).recordOrderEvent(eq("WAREHOUSE"), eq(1L), eq("CONFIRMED"), anyString());
        }

        @Test
        @DisplayName("WOS-002: Confirm order with insufficient stock sets PRODUCTION_REQUIRED")
        void confirmOrder_InsufficientStock_SetsProductionRequired() {
            // Given
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(false);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.confirmWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            assertThat(result.getTriggerScenario()).isEqualTo("PRODUCTION_REQUIRED");
        }

        @Test
        @DisplayName("WOS-003: Confirm non-existent order throws exception")
        void confirmOrder_NotFound_ThrowsException() {
            // Given
            when(warehouseOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> warehouseOrderService.confirmWarehouseOrder(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Warehouse order not found");
        }

        @Test
        @DisplayName("WOS-004: Confirm already confirmed order throws exception")
        void confirmOrder_AlreadyConfirmed_ThrowsException() {
            // Given
            testOrder.setStatus("CONFIRMED");
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> warehouseOrderService.confirmWarehouseOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only PENDING warehouse orders can be confirmed");
        }

        @Test
        @DisplayName("WOS-005: Confirm order with multiple items - all available sets DIRECT_FULFILLMENT")
        void confirmOrder_MultipleItems_AllAvailable_SetsDirectFulfillment() {
            // Given - Add second item
            WarehouseOrderItem secondItem = new WarehouseOrderItem();
            secondItem.setId(2L);
            secondItem.setItemId(11L);
            secondItem.setItemType("MODULE");
            secondItem.setRequestedQuantity(3);
            secondItem.setWarehouseOrder(testOrder);
            testOrder.getOrderItems().add(secondItem);

            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(true);
            when(inventoryService.checkStock(8L, 11L, 3)).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.confirmWarehouseOrder(1L);

            // Then
            assertThat(result.getTriggerScenario()).isEqualTo("DIRECT_FULFILLMENT");
            // checkStock is called during confirmation AND during mapToDTO (dynamic recalculation)
            verify(inventoryService, atLeast(1)).checkStock(8L, 10L, 5);
            verify(inventoryService, atLeast(1)).checkStock(8L, 11L, 3);
        }

        @Test
        @DisplayName("WOS-005b: Confirm order with multiple items - one unavailable sets PRODUCTION_REQUIRED")
        void confirmOrder_MultipleItems_OneUnavailable_SetsProductionRequired() {
            // Given - Add second item
            WarehouseOrderItem secondItem = new WarehouseOrderItem();
            secondItem.setId(2L);
            secondItem.setItemId(11L);
            secondItem.setItemType("MODULE");
            secondItem.setRequestedQuantity(3);
            secondItem.setWarehouseOrder(testOrder);
            testOrder.getOrderItems().add(secondItem);

            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(true);
            when(inventoryService.checkStock(8L, 11L, 3)).thenReturn(false); // Second item unavailable
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.confirmWarehouseOrder(1L);

            // Then
            assertThat(result.getTriggerScenario()).isEqualTo("PRODUCTION_REQUIRED");
        }
    }

    // ========================================
    // Fulfillment Tests (WOS-006 to WOS-009)
    // ========================================
    @Nested
    @DisplayName("Order Fulfillment Tests (WOS-006 to WOS-009)")
    class OrderFulfillmentTests {

        @Test
        @DisplayName("WOS-006: Fulfill CONFIRMED order with stock deducts and creates Final Assembly")
        void fulfillOrder_ConfirmedWithStock_DebitsAndCreatesFinalAssembly() {
            // Given
            testOrder.setStatus("CONFIRMED");
            testOrder.setTriggerScenario("DIRECT_FULFILLMENT");
            testOrderItem.setFulfilledQuantity(5); // Will be set by updateStock
            testOrderItem.setNotes("For product: Test Product");
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(true);
            when(inventoryService.updateStock(8L, 10L, 5)).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(finalAssemblyOrderService.createFromWarehouseOrder(any(WarehouseOrder.class), eq(1L), eq(1)))
                    .thenReturn(new FinalAssemblyOrderDTO());

            // When
            WarehouseOrderDTO result = warehouseOrderService.fulfillWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("FULFILLED");
            verify(inventoryService).updateStock(8L, 10L, 5);
            verify(finalAssemblyOrderService).createFromWarehouseOrder(any(WarehouseOrder.class), eq(1L), eq(1));
        }

        @Test
        @DisplayName("WOS-007: Fulfill order with linked production bypasses stock check")
        void fulfillOrder_LinkedProduction_BypassesStockCheck() {
            // Given
            testOrder.setStatus("CONFIRMED");
            testOrder.setProductionOrderId(50L); // Linked production order
            testOrderItem.setFulfilledQuantity(5);
            testOrderItem.setNotes("For product: Test Product");
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.updateStock(8L, 10L, 5)).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(finalAssemblyOrderService.createFromWarehouseOrder(any(WarehouseOrder.class), eq(1L), eq(1)))
                    .thenReturn(new FinalAssemblyOrderDTO());

            // When
            WarehouseOrderDTO result = warehouseOrderService.fulfillWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("FULFILLED");
            // Should NOT call checkStock - bypassed because productionOrderId is set
            verify(inventoryService, never()).checkStock(anyLong(), anyLong(), anyInt());
            verify(inventoryService).updateStock(8L, 10L, 5);
        }

        @Test
        @DisplayName("WOS-008: Fulfill non-existent order throws exception")
        void fulfillOrder_NotFound_ThrowsException() {
            // Given
            when(warehouseOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> warehouseOrderService.fulfillWarehouseOrder(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Warehouse order not found");
        }

        @Test
        @DisplayName("WOS-009: Fulfill PENDING order throws exception")
        void fulfillOrder_PendingStatus_ThrowsException() {
            // Given - testOrder is PENDING by default
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When/Then
            assertThatThrownBy(() -> warehouseOrderService.fulfillWarehouseOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only CONFIRMED or MODULES_READY warehouse orders can be fulfilled");
        }

        @Test
        @DisplayName("WOS-009b: Fulfill MODULES_READY order is allowed")
        void fulfillOrder_ModulesReadyStatus_Succeeds() {
            // Given
            testOrder.setStatus("MODULES_READY");
            testOrderItem.setFulfilledQuantity(5);
            testOrderItem.setNotes("For product: Test Product");
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(true);
            when(inventoryService.updateStock(8L, 10L, 5)).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(finalAssemblyOrderService.createFromWarehouseOrder(any(WarehouseOrder.class), eq(1L), eq(1)))
                    .thenReturn(new FinalAssemblyOrderDTO());

            // When
            WarehouseOrderDTO result = warehouseOrderService.fulfillWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("FULFILLED");
        }
    }

    // ========================================
    // Production Trigger Tests (WOS-010 to WOS-016)
    // ========================================
    @Nested
    @DisplayName("Production Trigger Tests (WOS-010 to WOS-016)")
    class ProductionTriggerTests {

        @Test
        @DisplayName("WOS-010: Fulfill with no stock triggers production for all items")
        void fulfillOrder_NoStock_TriggersProductionForAll() {
            // Given
            testOrder.setStatus("CONFIRMED");
            testOrder.setTriggerScenario("PRODUCTION_REQUIRED");
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(false);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(productionOrderService.createProductionOrderFromWarehouse(
                    anyLong(), anyLong(), anyString(), any(LocalDateTime.class), anyString(), anyLong(), anyLong()))
                    .thenReturn(new ProductionOrderDTO());

            // When
            WarehouseOrderDTO result = warehouseOrderService.fulfillWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("PENDING_PRODUCTION");
            verify(productionOrderService).createProductionOrderFromWarehouse(
                    eq(100L), eq(1L), anyString(), any(LocalDateTime.class), anyString(), eq(8L), eq(8L));
        }

        @Test
        @DisplayName("WOS-011: Partial stock triggers production for missing items only")
        void fulfillOrder_PartialStock_TriggersProductionForMissing() {
            // Given - Add second item, only first available
            WarehouseOrderItem secondItem = new WarehouseOrderItem();
            secondItem.setId(2L);
            secondItem.setItemId(11L);
            secondItem.setItemType("MODULE");
            secondItem.setRequestedQuantity(3);
            secondItem.setFulfilledQuantity(0);
            secondItem.setWarehouseOrder(testOrder);
            testOrder.getOrderItems().add(secondItem);
            
            testOrder.setStatus("CONFIRMED");
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            // First item available, second not
            when(inventoryService.checkStock(8L, 10L, 5)).thenReturn(true);
            when(inventoryService.checkStock(8L, 11L, 3)).thenReturn(false);
            when(inventoryService.updateStock(8L, 10L, 5)).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(productionOrderService.createProductionOrderFromWarehouse(
                    anyLong(), anyLong(), anyString(), any(LocalDateTime.class), anyString(), anyLong(), anyLong()))
                    .thenReturn(new ProductionOrderDTO());

            // When
            WarehouseOrderDTO result = warehouseOrderService.fulfillWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("PROCESSING");
            // First item should be fulfilled
            verify(inventoryService).updateStock(8L, 10L, 5);
            // Production order should be created for missing item
            verify(productionOrderService).createProductionOrderFromWarehouse(
                    eq(100L), eq(1L), anyString(), any(LocalDateTime.class), anyString(), eq(8L), eq(8L));
        }
    }

    // ========================================
    // Status Update Tests
    // ========================================
    @Nested
    @DisplayName("Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("updateWarehouseOrderStatus updates status successfully")
        void updateStatus_Success() {
            // Given
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.updateWarehouseOrderStatus(1L, "CONFIRMED");

            // Then
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            verify(warehouseOrderRepository).save(any(WarehouseOrder.class));
            // Note: updateWarehouseOrderStatus does not call orderAuditService
        }

        @Test
        @DisplayName("updateWarehouseOrderStatus throws exception when order not found")
        void updateStatus_NotFound_ThrowsException() {
            // Given
            when(warehouseOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> warehouseOrderService.updateWarehouseOrderStatus(999L, "CONFIRMED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Warehouse order not found");
        }
    }

    // ========================================
    // Audit Trail Tests
    // ========================================
    @Nested
    @DisplayName("Audit Trail Tests")
    class AuditTrailTests {

        @Test
        @DisplayName("Confirmation records audit event")
        void confirmOrder_RecordsAuditEvent() {
            // Given
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(anyLong(), anyLong(), anyInt())).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            warehouseOrderService.confirmWarehouseOrder(1L);

            // Then
            verify(orderAuditService).recordOrderEvent(
                    eq("WAREHOUSE"),
                    eq(1L),
                    eq("CONFIRMED"),
                    contains("Scenario: DIRECT_FULFILLMENT"));
        }

        @Test
        @DisplayName("Fulfillment start records audit event")
        void fulfillOrder_RecordsAuditEvent() {
            // Given
            testOrder.setStatus("CONFIRMED");
            testOrder.setProductionOrderId(50L); // To bypass stock check
            testOrderItem.setFulfilledQuantity(5);
            testOrderItem.setNotes("For product: Test Product");
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.updateStock(anyLong(), anyLong(), anyInt())).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(finalAssemblyOrderService.createFromWarehouseOrder(any(WarehouseOrder.class), anyLong(), anyInt()))
                    .thenReturn(new FinalAssemblyOrderDTO());

            // When
            warehouseOrderService.fulfillWarehouseOrder(1L);

            // Then
            verify(orderAuditService).recordOrderEvent(
                    eq("WAREHOUSE"),
                    eq(1L),
                    eq("FULFILLMENT_STARTED"),
                    anyString());
        }
    }

    // ========================================
    // Edge Case Tests
    // ========================================
    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty order items list confirmation succeeds with DIRECT_FULFILLMENT")
        void confirmOrder_EmptyItems_SetsDirectFulfillment() {
            // Given - order with no items (edge case)
            testOrder.setOrderItems(Collections.emptyList());
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.confirmWarehouseOrder(1L);

            // Then
            // All items (none) are available, so DIRECT_FULFILLMENT
            assertThat(result.getTriggerScenario()).isEqualTo("DIRECT_FULFILLMENT");
        }

        @Test
        @DisplayName("Order with null notes handles confirmation gracefully")
        void confirmOrder_NullNotes_HandledGracefully() {
            // Given
            testOrder.setNotes(null);
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.checkStock(anyLong(), anyLong(), anyInt())).thenReturn(true);
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.confirmWarehouseOrder(1L);

            // Then - should not throw NPE
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }
        
        @Test
        @DisplayName("Fulfillment handles inventory update failure gracefully")
        void fulfillOrder_InventoryUpdateFails_MarksPartiallyFulfilled() {
            // Given
            testOrder.setStatus("CONFIRMED");
            testOrder.setProductionOrderId(50L); // Bypass stock check
            
            when(warehouseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(inventoryService.updateStock(8L, 10L, 5)).thenReturn(false); // Fails
            when(warehouseOrderRepository.save(any(WarehouseOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            WarehouseOrderDTO result = warehouseOrderService.fulfillWarehouseOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("PARTIALLY_FULFILLED");
        }
    }
}
