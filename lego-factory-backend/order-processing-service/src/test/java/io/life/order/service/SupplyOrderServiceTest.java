package io.life.order.service;

import io.life.order.dto.SupplyOrderDTO;
import io.life.order.dto.SupplyOrderItemDTO;
import io.life.order.entity.AssemblyControlOrder;
import io.life.order.entity.ProductionControlOrder;
import io.life.order.entity.SupplyOrder;
import io.life.order.entity.SupplyOrderItem;
import io.life.order.repository.AssemblyControlOrderRepository;
import io.life.order.repository.ProductionControlOrderRepository;
import io.life.order.repository.ProductionOrderRepository;
import io.life.order.repository.SupplyOrderRepository;
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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SupplyOrderService.
 * 
 * Key behaviors tested:
 * - Supply order creation from control orders
 * - Order retrieval (by ID, workstation, control order)
 * - Order fulfillment workflow
 * - Order rejection and cancellation
 * - Status updates
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SupplyOrderService Tests")
class SupplyOrderServiceTest {

    private static final Long PARTS_SUPPLY_WAREHOUSE_ID = 9L;

    @Mock
    private SupplyOrderRepository repository;

    @Mock
    private AssemblyControlOrderRepository assemblyControlOrderRepository;

    @Mock
    private ProductionControlOrderRepository productionControlOrderRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private MasterdataService masterdataService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SupplyOrderService supplyOrderService;

    private SupplyOrder testSupplyOrder;
    private AssemblyControlOrder testAssemblyControlOrder;
    private ProductionControlOrder testProductionControlOrder;

    @BeforeEach
    void setUp() {
        // Set up test supply order with items
        testSupplyOrder = SupplyOrder.builder()
                .id(1L)
                .supplyOrderNumber("SO-123456-0001")
                .sourceControlOrderId(10L)
                .sourceControlOrderType("ASSEMBLY")
                .requestingWorkstationId(4L)
                .supplyWarehouseWorkstationId(PARTS_SUPPLY_WAREHOUSE_ID)
                .status("PENDING")
                .priority("MEDIUM")
                .createdAt(LocalDateTime.now())
                .requestedByTime(LocalDateTime.now().plusHours(2))
                .notes("Test supply order")
                .supplyOrderItems(createTestSupplyOrderItems())
                .build();

        // Set up assembly control order
        testAssemblyControlOrder = new AssemblyControlOrder();
        testAssemblyControlOrder.setId(10L);
        testAssemblyControlOrder.setControlOrderNumber("ACO-001");
        testAssemblyControlOrder.setItemId(7L); // Module ID
        testAssemblyControlOrder.setItemType("MODULE");
        testAssemblyControlOrder.setQuantity(5);
        testAssemblyControlOrder.setAssignedWorkstationId(4L);
        testAssemblyControlOrder.setTargetStartTime(LocalDateTime.now().plusHours(1));

        // Set up production control order
        testProductionControlOrder = new ProductionControlOrder();
        testProductionControlOrder.setId(20L);
        testProductionControlOrder.setControlOrderNumber("PCO-001");
        testProductionControlOrder.setItemId(15L); // Part ID
        testProductionControlOrder.setItemType("PART");
        testProductionControlOrder.setQuantity(10);
        testProductionControlOrder.setAssignedWorkstationId(1L);
        testProductionControlOrder.setTargetStartTime(LocalDateTime.now().plusHours(1));

        // Common mock setups
        when(repository.save(any(SupplyOrder.class))).thenAnswer(invocation -> {
            SupplyOrder order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(1L);
            }
            return order;
        });
    }

    private List<SupplyOrderItem> createTestSupplyOrderItems() {
        List<SupplyOrderItem> items = new ArrayList<>();
        SupplyOrderItem item1 = new SupplyOrderItem(1L, testSupplyOrder, 101L, 10, null, "piece", "Part 1");
        SupplyOrderItem item2 = new SupplyOrderItem(2L, testSupplyOrder, 102L, 5, null, "piece", "Part 2");
        items.add(item1);
        items.add(item2);
        return items;
    }

    @Nested
    @DisplayName("Order Retrieval Tests")
    class OrderRetrievalTests {

        @Test
        @DisplayName("SOS-001: getSupplyOrder returns order when exists")
        void getSupplyOrder_whenExists_returnsDTO() {
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            SupplyOrderDTO result = supplyOrderService.getSupplyOrder(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSupplyOrderNumber()).isEqualTo("SO-123456-0001");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getSupplyOrderItems()).hasSize(2);
        }

        @Test
        @DisplayName("SOS-002: getSupplyOrder throws when not found")
        void getSupplyOrder_whenNotFound_throwsException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplyOrderService.getSupplyOrder(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Supply order not found: 999");
        }

        @Test
        @DisplayName("SOS-003: getOrdersForSupplyWarehouse returns all orders when no status filter")
        void getOrdersForSupplyWarehouse_noStatusFilter_returnsAllOrders() {
            List<SupplyOrder> orders = List.of(testSupplyOrder);
            when(repository.findBySupplyWarehouseWorkstationId(PARTS_SUPPLY_WAREHOUSE_ID))
                    .thenReturn(orders);

            List<SupplyOrderDTO> result = supplyOrderService.getOrdersForSupplyWarehouse(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSupplyOrderNumber()).isEqualTo("SO-123456-0001");
            verify(repository).findBySupplyWarehouseWorkstationId(PARTS_SUPPLY_WAREHOUSE_ID);
        }

        @Test
        @DisplayName("SOS-004: getOrdersForSupplyWarehouse filters by status when provided")
        void getOrdersForSupplyWarehouse_withStatusFilter_returnsFilteredOrders() {
            List<SupplyOrder> orders = List.of(testSupplyOrder);
            when(repository.findBySupplyWarehouseWorkstationIdAndStatus(PARTS_SUPPLY_WAREHOUSE_ID, "PENDING"))
                    .thenReturn(orders);

            List<SupplyOrderDTO> result = supplyOrderService.getOrdersForSupplyWarehouse("PENDING");

            assertThat(result).hasSize(1);
            verify(repository).findBySupplyWarehouseWorkstationIdAndStatus(PARTS_SUPPLY_WAREHOUSE_ID, "PENDING");
        }

        @Test
        @DisplayName("SOS-005: getOrdersByRequestingWorkstation returns orders for workstation")
        void getOrdersByRequestingWorkstation_returnsOrdersForWorkstation() {
            when(repository.findByRequestingWorkstationId(4L)).thenReturn(List.of(testSupplyOrder));

            List<SupplyOrderDTO> result = supplyOrderService.getOrdersByRequestingWorkstation(4L, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRequestingWorkstationId()).isEqualTo(4L);
        }

        @Test
        @DisplayName("SOS-006: getSupplyOrdersForControlOrder returns orders for control order")
        void getSupplyOrdersForControlOrder_returnsMatchingOrders() {
            when(repository.findBySourceControlOrderIdAndSourceControlOrderType(10L, "ASSEMBLY"))
                    .thenReturn(List.of(testSupplyOrder));

            List<SupplyOrderDTO> result = supplyOrderService.getSupplyOrdersForControlOrder(10L, "ASSEMBLY");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceControlOrderId()).isEqualTo(10L);
            assertThat(result.get(0).getSourceControlOrderType()).isEqualTo("ASSEMBLY");
        }
    }

    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {

        @Test
        @DisplayName("SOS-007: createSupplyOrder creates order with items")
        void createSupplyOrder_createsOrderWithItems() {
            List<SupplyOrderItemDTO> items = List.of(
                    SupplyOrderItemDTO.builder().partId(101L).quantityRequested(10).unit("piece").build()
            );

            SupplyOrderDTO result = supplyOrderService.createSupplyOrder(
                    10L,
                    "ASSEMBLY",
                    4L,
                    "HIGH",
                    LocalDateTime.now().plusHours(2),
                    items,
                    "Test order"
            );

            assertThat(result).isNotNull();
            assertThat(result.getSourceControlOrderId()).isEqualTo(10L);
            assertThat(result.getSourceControlOrderType()).isEqualTo("ASSEMBLY");
            assertThat(result.getRequestingWorkstationId()).isEqualTo(4L);
            assertThat(result.getSupplyWarehouseWorkstationId()).isEqualTo(PARTS_SUPPLY_WAREHOUSE_ID);
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getPriority()).isEqualTo("HIGH");

            verify(repository, times(2)).save(any(SupplyOrder.class));
        }

        @Test
        @DisplayName("SOS-008: createSupplyOrder generates unique order number")
        void createSupplyOrder_generatesUniqueOrderNumber() {
            List<SupplyOrderItemDTO> items = List.of(
                    SupplyOrderItemDTO.builder().partId(101L).quantityRequested(5).build()
            );

            SupplyOrderDTO result = supplyOrderService.createSupplyOrder(
                    10L, "ASSEMBLY", 4L, "MEDIUM", LocalDateTime.now(), items, null
            );

            assertThat(result.getSupplyOrderNumber()).startsWith("SO-");
            assertThat(result.getSupplyOrderNumber()).matches("SO-\\d+-\\d{4}");
        }
    }

    @Nested
    @DisplayName("Create from Assembly Control Order Tests")
    class CreateFromAssemblyControlOrderTests {

        @Test
        @DisplayName("SOS-009: createSupplyOrderFromControlOrder ASSEMBLY creates order with BOM parts")
        void createFromAssemblyControlOrder_createOrderWithBOMParts() {
            when(assemblyControlOrderRepository.findById(10L))
                    .thenReturn(Optional.of(testAssemblyControlOrder));
            when(masterdataService.getPartRequirementsForModule(7L, 5))
                    .thenReturn(Map.of(101L, 25, 102L, 15)); // 5 modules × parts per module

            SupplyOrderDTO result = supplyOrderService.createSupplyOrderFromControlOrder(10L, "ASSEMBLY", "HIGH");

            assertThat(result).isNotNull();
            assertThat(result.getSourceControlOrderId()).isEqualTo(10L);
            assertThat(result.getSourceControlOrderType()).isEqualTo("ASSEMBLY");
            assertThat(result.getRequestingWorkstationId()).isEqualTo(4L);
            assertThat(result.getPriority()).isEqualTo("HIGH");

            ArgumentCaptor<SupplyOrder> captor = ArgumentCaptor.forClass(SupplyOrder.class);
            verify(repository, atLeastOnce()).save(captor.capture());
        }

        @Test
        @DisplayName("SOS-010: createSupplyOrderFromControlOrder ASSEMBLY throws if control order not found")
        void createFromAssemblyControlOrder_throwsIfNotFound() {
            when(assemblyControlOrderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> 
                    supplyOrderService.createSupplyOrderFromControlOrder(999L, "ASSEMBLY", "MEDIUM"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Assembly control order not found: 999");
        }

        @Test
        @DisplayName("SOS-011: createSupplyOrderFromControlOrder ASSEMBLY throws if not MODULE type")
        void createFromAssemblyControlOrder_throwsIfNotModuleType() {
            testAssemblyControlOrder.setItemType("PRODUCT"); // Invalid type
            when(assemblyControlOrderRepository.findById(10L))
                    .thenReturn(Optional.of(testAssemblyControlOrder));

            assertThatThrownBy(() -> 
                    supplyOrderService.createSupplyOrderFromControlOrder(10L, "ASSEMBLY", "MEDIUM"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("item type must be MODULE");
        }

        @Test
        @DisplayName("SOS-012: createSupplyOrderFromControlOrder ASSEMBLY throws if no BOM parts found")
        void createFromAssemblyControlOrder_throwsIfNoBOMParts() {
            when(assemblyControlOrderRepository.findById(10L))
                    .thenReturn(Optional.of(testAssemblyControlOrder));
            when(masterdataService.getPartRequirementsForModule(7L, 5))
                    .thenReturn(Collections.emptyMap());

            assertThatThrownBy(() -> 
                    supplyOrderService.createSupplyOrderFromControlOrder(10L, "ASSEMBLY", "MEDIUM"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No parts found in BOM for module: 7");
        }
    }

    @Nested
    @DisplayName("Create from Production Control Order Tests")
    class CreateFromProductionControlOrderTests {

        @Test
        @DisplayName("SOS-013: createSupplyOrderFromControlOrder PRODUCTION creates order for part")
        void createFromProductionControlOrder_createsOrderForPart() {
            when(productionControlOrderRepository.findById(20L))
                    .thenReturn(Optional.of(testProductionControlOrder));

            SupplyOrderDTO result = supplyOrderService.createSupplyOrderFromControlOrder(20L, "PRODUCTION", "HIGH");

            assertThat(result).isNotNull();
            assertThat(result.getSourceControlOrderId()).isEqualTo(20L);
            assertThat(result.getSourceControlOrderType()).isEqualTo("PRODUCTION");
            assertThat(result.getRequestingWorkstationId()).isEqualTo(1L); // WS-1 Injection Molding
            assertThat(result.getPriority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("SOS-014: createSupplyOrderFromControlOrder PRODUCTION throws if control order not found")
        void createFromProductionControlOrder_throwsIfNotFound() {
            when(productionControlOrderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> 
                    supplyOrderService.createSupplyOrderFromControlOrder(999L, "PRODUCTION", "MEDIUM"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Production control order not found: 999");
        }

        @Test
        @DisplayName("SOS-015: createSupplyOrderFromControlOrder PRODUCTION throws if no item ID")
        void createFromProductionControlOrder_throwsIfNoItemId() {
            testProductionControlOrder.setItemId(null);
            when(productionControlOrderRepository.findById(20L))
                    .thenReturn(Optional.of(testProductionControlOrder));

            assertThatThrownBy(() -> 
                    supplyOrderService.createSupplyOrderFromControlOrder(20L, "PRODUCTION", "MEDIUM"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has no item ID");
        }

        @Test
        @DisplayName("SOS-016: createSupplyOrderFromControlOrder with invalid type throws")
        void createFromControlOrder_invalidType_throwsException() {
            assertThatThrownBy(() -> 
                    supplyOrderService.createSupplyOrderFromControlOrder(10L, "INVALID", "MEDIUM"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid control order type: INVALID");
        }
    }

    @Nested
    @DisplayName("Order Fulfillment Tests")
    class OrderFulfillmentTests {

        @Test
        @DisplayName("SOS-017: fulfillSupplyOrder fulfills order and sets quantitySupplied")
        void fulfillSupplyOrder_fulfillsOrderAndSetsQuantity() {
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            SupplyOrderDTO result = supplyOrderService.fulfillSupplyOrder(1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("FULFILLED");
            assertThat(testSupplyOrder.getFulfilledAt()).isNotNull();

            // Verify items have quantitySupplied set
            for (SupplyOrderItem item : testSupplyOrder.getSupplyOrderItems()) {
                assertThat(item.getQuantitySupplied()).isEqualTo(item.getQuantityRequested());
            }

            verify(repository).save(any(SupplyOrder.class));
        }

        @Test
        @DisplayName("SOS-018: fulfillSupplyOrder throws if already fulfilled")
        void fulfillSupplyOrder_throwsIfAlreadyFulfilled() {
            testSupplyOrder.setStatus("FULFILLED");
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            assertThatThrownBy(() -> supplyOrderService.fulfillSupplyOrder(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot fulfill supply order in status: FULFILLED");
        }

        @Test
        @DisplayName("SOS-019: fulfillSupplyOrder throws if rejected")
        void fulfillSupplyOrder_throwsIfRejected() {
            testSupplyOrder.setStatus("REJECTED");
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            assertThatThrownBy(() -> supplyOrderService.fulfillSupplyOrder(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot fulfill supply order in status: REJECTED");
        }

        @Test
        @DisplayName("SOS-020: fulfillSupplyOrder throws if not found")
        void fulfillSupplyOrder_throwsIfNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplyOrderService.fulfillSupplyOrder(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Supply order not found: 999");
        }
    }

    @Nested
    @DisplayName("Order Rejection Tests")
    class OrderRejectionTests {

        @Test
        @DisplayName("SOS-021: rejectSupplyOrder sets status to REJECTED with reason")
        void rejectSupplyOrder_setsStatusWithReason() {
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            SupplyOrderDTO result = supplyOrderService.rejectSupplyOrder(1L, "Insufficient stock");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
            assertThat(testSupplyOrder.getRejectedAt()).isNotNull();
            assertThat(testSupplyOrder.getNotes()).contains("Rejected: Insufficient stock");

            verify(repository).save(any(SupplyOrder.class));
        }

        @Test
        @DisplayName("SOS-022: rejectSupplyOrder appends reason to existing notes")
        void rejectSupplyOrder_appendsReasonToExistingNotes() {
            testSupplyOrder.setNotes("Original notes");
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            supplyOrderService.rejectSupplyOrder(1L, "Out of stock");

            assertThat(testSupplyOrder.getNotes()).contains("Original notes");
            assertThat(testSupplyOrder.getNotes()).contains("Rejected: Out of stock");
        }

        @Test
        @DisplayName("SOS-023: rejectSupplyOrder throws if not found")
        void rejectSupplyOrder_throwsIfNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplyOrderService.rejectSupplyOrder(999L, "reason"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Supply order not found: 999");
        }
    }

    @Nested
    @DisplayName("Order Cancellation Tests")
    class OrderCancellationTests {

        @Test
        @DisplayName("SOS-024: cancelSupplyOrder sets status to CANCELLED with reason")
        void cancelSupplyOrder_setsStatusWithReason() {
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            SupplyOrderDTO result = supplyOrderService.cancelSupplyOrder(1L, "Production cancelled");

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(testSupplyOrder.getCancelledAt()).isNotNull();
            assertThat(testSupplyOrder.getNotes()).contains("Cancelled: Production cancelled");

            verify(repository).save(any(SupplyOrder.class));
        }

        @Test
        @DisplayName("SOS-025: cancelSupplyOrder handles null reason")
        void cancelSupplyOrder_handlesNullReason() {
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            SupplyOrderDTO result = supplyOrderService.cancelSupplyOrder(1L, null);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(testSupplyOrder.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("SOS-026: cancelSupplyOrder throws if not found")
        void cancelSupplyOrder_throwsIfNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplyOrderService.cancelSupplyOrder(999L, "reason"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Supply order not found: 999");
        }
    }

    @Nested
    @DisplayName("Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("SOS-027: updateStatus updates order status")
        void updateStatus_updatesOrderStatus() {
            when(repository.findById(1L)).thenReturn(Optional.of(testSupplyOrder));

            SupplyOrderDTO result = supplyOrderService.updateStatus(1L, "IN_PROGRESS");

            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            verify(repository).save(any(SupplyOrder.class));
        }

        @Test
        @DisplayName("SOS-028: updateStatus throws if not found")
        void updateStatus_throwsIfNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplyOrderService.updateStatus(999L, "IN_PROGRESS"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Supply order not found: 999");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("SOS-029: getOrdersForSupplyWarehouse returns empty list when no orders")
        void getOrdersForSupplyWarehouse_returnsEmptyListWhenNoOrders() {
            when(repository.findBySupplyWarehouseWorkstationId(PARTS_SUPPLY_WAREHOUSE_ID))
                    .thenReturn(Collections.emptyList());

            List<SupplyOrderDTO> result = supplyOrderService.getOrdersForSupplyWarehouse(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("SOS-030: createSupplyOrder with empty item list creates order with no items")
        void createSupplyOrder_withEmptyItemList_createsOrderWithNoItems() {
            List<SupplyOrderItemDTO> items = Collections.emptyList();

            SupplyOrderDTO result = supplyOrderService.createSupplyOrder(
                    10L, "ASSEMBLY", 4L, "MEDIUM", LocalDateTime.now(), items, null
            );

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("SOS-031: getBySourceControlOrder delegates to repository correctly")
        void getBySourceControlOrder_delegatesToRepository() {
            when(repository.findBySourceControlOrderIdAndSourceControlOrderType(10L, "ASSEMBLY"))
                    .thenReturn(List.of(testSupplyOrder));

            List<SupplyOrderDTO> result = supplyOrderService.getBySourceControlOrder(10L, "ASSEMBLY");

            assertThat(result).hasSize(1);
            verify(repository).findBySourceControlOrderIdAndSourceControlOrderType(10L, "ASSEMBLY");
        }

        @Test
        @DisplayName("SOS-032: createSupplyOrderFromControlOrder uses default priority if null")
        void createFromControlOrder_usesDefaultPriorityIfNull() {
            when(productionControlOrderRepository.findById(20L))
                    .thenReturn(Optional.of(testProductionControlOrder));

            SupplyOrderDTO result = supplyOrderService.createSupplyOrderFromControlOrder(20L, "PRODUCTION", null);

            assertThat(result.getPriority()).isEqualTo("MEDIUM");
        }
    }
}
