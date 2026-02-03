package io.life.order.service;

import io.life.order.client.MasterdataClient;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.dto.masterdata.BomEntryDTO;
import io.life.order.dto.masterdata.ModuleDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.entity.ProductionOrder;
import io.life.order.entity.ProductionOrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.repository.CustomerOrderRepository;
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
 * Unit tests for ProductionOrderService
 * 
 * Test Coverage:
 * - Order creation from CustomerOrder (Scenario 4) - POS-001 to POS-004
 * - Order creation from WarehouseOrder (Scenario 3) - POS-005 to POS-008
 * - Order retrieval - POS-009 to POS-012
 * - Status transitions - POS-013 to POS-016
 * - Completion flow - POS-017 to POS-020
 * 
 * @see _dev-docs/TEST_PLAN.md for test case specifications
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductionOrderService Tests")
class ProductionOrderServiceTest {

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private WarehouseOrderRepository warehouseOrderRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ProductionControlOrderService productionControlOrderService;

    @Mock
    private AssemblyControlOrderService assemblyControlOrderService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private MasterdataClient masterdataClient;

    @InjectMocks
    private ProductionOrderService productionOrderService;

    // Test fixtures
    private ProductionOrder testProductionOrder;
    private CustomerOrder testCustomerOrder;
    private WarehouseOrder testWarehouseOrder;
    private BomEntryDTO testBomEntry;
    private ModuleDTO testModule;

    @BeforeEach
    void setUp() {
        // Setup test production order
        testProductionOrder = ProductionOrder.builder()
                .id(1L)
                .productionOrderNumber("PO-TEST001")
                .sourceCustomerOrderId(100L)
                .sourceWarehouseOrderId(null)
                .status("CREATED")
                .priority("NORMAL")
                .dueDate(LocalDateTime.now().plusDays(1))
                .triggerScenario("SCENARIO_4")
                .createdByWorkstationId(7L)
                .assignedWorkstationId(6L)
                .build();

        // Setup test customer order
        testCustomerOrder = new CustomerOrder();
        testCustomerOrder.setId(100L);
        testCustomerOrder.setOrderNumber("CO-TEST001");
        testCustomerOrder.setStatus("CONFIRMED");
        testCustomerOrder.setTriggerScenario("DIRECT_PRODUCTION");
        testCustomerOrder.setWorkstationId(7L);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setItemId(1L); // Product ID
        orderItem.setItemType("PRODUCT");
        orderItem.setQuantity(5);
        orderItem.setCustomerOrder(testCustomerOrder);

        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        testCustomerOrder.setOrderItems(items);

        // Setup test warehouse order
        testWarehouseOrder = new WarehouseOrder();
        testWarehouseOrder.setId(50L);
        testWarehouseOrder.setOrderNumber("WO-TEST001");
        testWarehouseOrder.setCustomerOrderId(100L);
        testWarehouseOrder.setStatus("CONFIRMED");
        testWarehouseOrder.setWorkstationId(8L);

        WarehouseOrderItem woItem = new WarehouseOrderItem();
        woItem.setId(1L);
        woItem.setItemId(10L); // Module ID
        woItem.setItemType("MODULE");
        woItem.setRequestedQuantity(3);
        woItem.setProductId(1L);
        woItem.setWarehouseOrder(testWarehouseOrder);

        List<WarehouseOrderItem> woItems = new ArrayList<>();
        woItems.add(woItem);
        testWarehouseOrder.setOrderItems(woItems);

        // Setup BOM entry for masterdata client
        testBomEntry = new BomEntryDTO();
        testBomEntry.setComponentId(10L); // Module ID
        testBomEntry.setQuantity(2);

        // Setup module for masterdata client
        testModule = new ModuleDTO();
        testModule.setId(10L);
        testModule.setName("Test Module");
        testModule.setProductionWorkstationId(4); // Assembly workstation
    }

    // ========================================
    // Retrieval Tests
    // ========================================
    @Nested
    @DisplayName("Order Retrieval Tests")
    class OrderRetrievalTests {

        @Test
        @DisplayName("getAllProductionOrders returns all orders")
        void getAllProductionOrders_ReturnsAllOrders() {
            // Given
            when(productionOrderRepository.findAll()).thenReturn(List.of(testProductionOrder));

            // When
            List<ProductionOrderDTO> result = productionOrderService.getAllProductionOrders();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductionOrderNumber()).isEqualTo("PO-TEST001");
        }

        @Test
        @DisplayName("getProductionOrderById returns order when found")
        void getProductionOrderById_Found_ReturnsOrder() {
            // Given
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));

            // When
            Optional<ProductionOrderDTO> result = productionOrderService.getProductionOrderById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getProductionOrderNumber()).isEqualTo("PO-TEST001");
        }

        @Test
        @DisplayName("getProductionOrderById returns empty when not found")
        void getProductionOrderById_NotFound_ReturnsEmpty() {
            // Given
            when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<ProductionOrderDTO> result = productionOrderService.getProductionOrderById(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getProductionOrderByNumber returns order when found")
        void getProductionOrderByNumber_Found_ReturnsOrder() {
            // Given
            when(productionOrderRepository.findByProductionOrderNumber("PO-TEST001"))
                    .thenReturn(Optional.of(testProductionOrder));

            // When
            Optional<ProductionOrderDTO> result = productionOrderService.getProductionOrderByNumber("PO-TEST001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getProductionOrdersByStatus returns filtered orders")
        void getProductionOrdersByStatus_ReturnsFilteredOrders() {
            // Given
            when(productionOrderRepository.findByStatus("CREATED")).thenReturn(List.of(testProductionOrder));

            // When
            List<ProductionOrderDTO> result = productionOrderService.getProductionOrdersByStatus("CREATED");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("CREATED");
        }

        @Test
        @DisplayName("getProductionOrdersByCustomerOrder returns related orders")
        void getProductionOrdersByCustomerOrder_ReturnsRelatedOrders() {
            // Given
            when(productionOrderRepository.findBySourceCustomerOrderId(100L))
                    .thenReturn(List.of(testProductionOrder));

            // When
            List<ProductionOrderDTO> result = productionOrderService.getProductionOrdersByCustomerOrder(100L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceCustomerOrderId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("getProductionOrdersByWarehouseOrder returns related orders")
        void getProductionOrdersByWarehouseOrder_ReturnsRelatedOrders() {
            // Given
            testProductionOrder.setSourceWarehouseOrderId(50L);
            when(productionOrderRepository.findBySourceWarehouseOrderId(50L))
                    .thenReturn(List.of(testProductionOrder));

            // When
            List<ProductionOrderDTO> result = productionOrderService.getProductionOrdersByWarehouseOrder(50L);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getProductionOrdersByPriority returns filtered orders")
        void getProductionOrdersByPriority_ReturnsFilteredOrders() {
            // Given
            when(productionOrderRepository.findByPriority("HIGH")).thenReturn(Collections.emptyList());

            // When
            List<ProductionOrderDTO> result = productionOrderService.getProductionOrdersByPriority("HIGH");

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ========================================
    // Scenario 4 Creation Tests (POS-001 to POS-004)
    // ========================================
    @Nested
    @DisplayName("Scenario 4 Creation Tests (POS-001 to POS-004)")
    class Scenario4CreationTests {

        @Test
        @DisplayName("POS-001: Create from CustomerOrder creates production order")
        void createFromCustomerOrder_Success() {
            // Given
            when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(testCustomerOrder));
            when(masterdataClient.getModulesForProduct(1L)).thenReturn(List.of(testBomEntry));
            when(masterdataClient.getModuleById(10L)).thenReturn(Optional.of(testModule));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> {
                        ProductionOrder po = inv.getArgument(0);
                        po.setId(1L);
                        return po;
                    });

            // When
            ProductionOrderDTO result = productionOrderService.createFromCustomerOrder(
                    100L, "NORMAL", LocalDateTime.now().plusDays(1), "Test notes", 7L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSourceCustomerOrderId()).isEqualTo(100L);
            assertThat(result.getTriggerScenario()).isEqualTo("SCENARIO_4");
            verify(customerOrderRepository).save(any(CustomerOrder.class));
        }

        @Test
        @DisplayName("POS-002: Create from CustomerOrder with no modules handles gracefully")
        void createFromCustomerOrder_NoModules_HandlesGracefully() {
            // Given
            when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(testCustomerOrder));
            when(masterdataClient.getModulesForProduct(1L)).thenReturn(Collections.emptyList());
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> {
                        ProductionOrder po = inv.getArgument(0);
                        po.setId(1L);
                        return po;
                    });

            // When
            ProductionOrderDTO result = productionOrderService.createFromCustomerOrder(
                    100L, "NORMAL", null, null, 7L);

            // Then
            assertThat(result).isNotNull();
            // No items created but order still created
        }

        @Test
        @DisplayName("POS-003: Create from CustomerOrder updates customer order status")
        void createFromCustomerOrder_UpdatesCustomerOrderStatus() {
            // Given
            when(customerOrderRepository.findById(100L)).thenReturn(Optional.of(testCustomerOrder));
            when(masterdataClient.getModulesForProduct(1L)).thenReturn(List.of(testBomEntry));
            when(masterdataClient.getModuleById(10L)).thenReturn(Optional.of(testModule));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> {
                        ProductionOrder po = inv.getArgument(0);
                        po.setId(1L);
                        return po;
                    });

            // When
            productionOrderService.createFromCustomerOrder(100L, "NORMAL", null, null, 7L);

            // Then
            ArgumentCaptor<CustomerOrder> captor = ArgumentCaptor.forClass(CustomerOrder.class);
            verify(customerOrderRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("PROCESSING");
        }

        @Test
        @DisplayName("POS-004: Create from non-existent CustomerOrder throws exception")
        void createFromCustomerOrder_NotFound_ThrowsException() {
            // Given
            when(customerOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productionOrderService.createFromCustomerOrder(
                    999L, "NORMAL", null, null, 7L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Customer order not found");
        }
    }

    // ========================================
    // Scenario 3 Creation Tests (POS-005 to POS-008)
    // ========================================
    @Nested
    @DisplayName("Scenario 3 Creation Tests (POS-005 to POS-008)")
    class Scenario3CreationTests {

        @Test
        @DisplayName("POS-005: Create from WarehouseOrder creates production order")
        void createFromWarehouseOrder_Success() {
            // Given
            when(warehouseOrderRepository.findById(50L)).thenReturn(Optional.of(testWarehouseOrder));
            when(masterdataClient.getModuleById(10L)).thenReturn(Optional.of(testModule));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> {
                        ProductionOrder po = inv.getArgument(0);
                        po.setId(1L);
                        return po;
                    });

            // When
            ProductionOrderDTO result = productionOrderService.createProductionOrderFromWarehouse(
                    100L, 50L, "NORMAL", LocalDateTime.now().plusDays(1), 
                    "Test notes", 8L, 8L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSourceWarehouseOrderId()).isEqualTo(50L);
            assertThat(result.getTriggerScenario()).isEqualTo("SCENARIO_3");
        }

        @Test
        @DisplayName("POS-006: Create from WarehouseOrder fetches customerOrderId from warehouse order")
        void createFromWarehouseOrder_FetchesCustomerOrderId() {
            // Given
            when(warehouseOrderRepository.findById(50L)).thenReturn(Optional.of(testWarehouseOrder));
            when(masterdataClient.getModuleById(10L)).thenReturn(Optional.of(testModule));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> {
                        ProductionOrder po = inv.getArgument(0);
                        po.setId(1L);
                        return po;
                    });

            // When - pass null for customerOrderId
            ProductionOrderDTO result = productionOrderService.createProductionOrderFromWarehouse(
                    null, 50L, "NORMAL", null, null, 8L, 8L);

            // Then - should fetch from warehouse order
            assertThat(result.getSourceCustomerOrderId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("POS-007: Create from WarehouseOrder links to warehouse order")
        void createFromWarehouseOrder_LinksToWarehouseOrder() {
            // Given
            when(warehouseOrderRepository.findById(50L)).thenReturn(Optional.of(testWarehouseOrder));
            when(masterdataClient.getModuleById(10L)).thenReturn(Optional.of(testModule));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> {
                        ProductionOrder po = inv.getArgument(0);
                        po.setId(1L);
                        return po;
                    });

            // When
            ProductionOrderDTO result = productionOrderService.createProductionOrderFromWarehouse(
                    100L, 50L, "HIGH", null, null, 8L, 8L);

            // Then
            assertThat(result.getSourceWarehouseOrderId()).isEqualTo(50L);
            assertThat(result.getPriority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("POS-008: Create from non-existent WarehouseOrder throws exception")
        void createFromWarehouseOrder_NotFound_ThrowsException() {
            // Given
            when(warehouseOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productionOrderService.createProductionOrderFromWarehouse(
                    100L, 999L, "NORMAL", null, null, 8L, 8L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Warehouse order not found");
        }
    }

    // ========================================
    // Status Update Tests (POS-009 to POS-012)
    // ========================================
    @Nested
    @DisplayName("Status Update Tests (POS-009 to POS-012)")
    class StatusUpdateTests {

        @Test
        @DisplayName("POS-009: Update status changes status successfully")
        void updateStatus_Success() {
            // Given
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            ProductionOrderDTO result = productionOrderService.updateProductionOrderStatus(1L, "SCHEDULED");

            // Then
            assertThat(result.getStatus()).isEqualTo("SCHEDULED");
        }

        @Test
        @DisplayName("POS-010: Confirm order changes status to CONFIRMED")
        void confirmOrder_Success() {
            // Given
            testProductionOrder.setStatus("CREATED");
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            ProductionOrderDTO result = productionOrderService.confirmProductionOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("POS-011: Confirm non-CREATED order throws exception")
        void confirmOrder_InvalidStatus_ThrowsException() {
            // Given
            testProductionOrder.setStatus("SCHEDULED");
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));

            // When/Then
            assertThatThrownBy(() -> productionOrderService.confirmProductionOrder(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("must be in CREATED status");
        }

        @Test
        @DisplayName("POS-012: Cancel order changes status to CANCELLED")
        void cancelOrder_Success() {
            // Given
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            ProductionOrderDTO result = productionOrderService.cancelProductionOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }
    }

    // ========================================
    // Completion Tests (POS-013 to POS-016)
    // ========================================
    @Nested
    @DisplayName("Completion Tests (POS-013 to POS-016)")
    class CompletionTests {

        @Test
        @DisplayName("POS-013: Complete order sets status to COMPLETED")
        void completeOrder_SetsStatus() {
            // Given
            testProductionOrder.setProductionOrderItems(Collections.emptyList());
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            ProductionOrderDTO result = productionOrderService.completeProductionOrder(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("POS-014: Complete order sets actual completion time")
        void completeOrder_SetsCompletionTime() {
            // Given
            testProductionOrder.setProductionOrderItems(Collections.emptyList());
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            ProductionOrderDTO result = productionOrderService.completeProductionOrder(1L);

            // Then
            assertThat(result.getActualCompletionTime()).isNotNull();
        }

        @Test
        @DisplayName("POS-015: Complete order credits Modules Supermarket")
        void completeOrder_CreditsModulesSupermarket() {
            // Given
            ProductionOrderItem item = ProductionOrderItem.builder()
                    .itemId(10L)
                    .itemType("MODULE")
                    .quantity(5)
                    .itemName("Test Module")
                    .build();
            testProductionOrder.setProductionOrderItems(List.of(item));
            
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(inventoryService.creditProductionStock(eq(8L), eq("MODULE"), eq(10L), eq(5), anyString()))
                    .thenReturn(true);

            // When
            productionOrderService.completeProductionOrder(1L);

            // Then
            verify(inventoryService).creditProductionStock(eq(8L), eq("MODULE"), eq(10L), eq(5), anyString());
        }

        @Test
        @DisplayName("POS-016: Complete non-existent order throws exception")
        void completeOrder_NotFound_ThrowsException() {
            // Given
            when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productionOrderService.completeProductionOrder(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Production order not found");
        }
    }

    // ========================================
    // SimAL Integration Tests
    // ========================================
    @Nested
    @DisplayName("SimAL Integration Tests")
    class SimalIntegrationTests {

        @Test
        @DisplayName("Link to SimAL schedule updates schedule info")
        void linkToSimalSchedule_UpdatesScheduleInfo() {
            // Given
            when(productionOrderRepository.findById(1L)).thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any(ProductionOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime expectedCompletion = LocalDateTime.now().plusHours(8);

            // When
            ProductionOrderDTO result = productionOrderService.linkToSimalSchedule(
                    1L, "SIMAL-001", 480, expectedCompletion);

            // Then
            assertThat(result.getSimalScheduleId()).isEqualTo("SIMAL-001");
            assertThat(result.getEstimatedDuration()).isEqualTo(480);
            assertThat(result.getStatus()).isEqualTo("SCHEDULED");
        }

        @Test
        @DisplayName("Link to SimAL with non-existent order throws exception")
        void linkToSimalSchedule_NotFound_ThrowsException() {
            // Given
            when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productionOrderService.linkToSimalSchedule(
                    999L, "SIMAL-001", 480, LocalDateTime.now()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Production order not found");
        }
    }

    // ========================================
    // Workstation Assignment Tests
    // ========================================
    @Nested
    @DisplayName("Workstation Assignment Tests")
    class WorkstationAssignmentTests {

        @Test
        @DisplayName("Get orders by assigned workstation returns filtered orders")
        void getByAssignedWorkstation_ReturnsFiltered() {
            // Given
            when(productionOrderRepository.findByAssignedWorkstationId(6L))
                    .thenReturn(List.of(testProductionOrder));

            // When
            List<ProductionOrderDTO> result = productionOrderService.getProductionOrdersByAssignedWorkstation(6L);

            // Then
            assertThat(result).hasSize(1);
            // Note: assignedWorkstationId is not exposed in DTO, just verify the filter works
        }

        @Test
        @DisplayName("Get orders by created workstation returns filtered orders")
        void getByCreatedWorkstation_ReturnsFiltered() {
            // Given
            when(productionOrderRepository.findByCreatedByWorkstationId(7L))
                    .thenReturn(List.of(testProductionOrder));

            // When
            List<ProductionOrderDTO> result = productionOrderService.getProductionOrdersByWorkstation(7L);

            // Then
            assertThat(result).hasSize(1);
        }
    }
}
