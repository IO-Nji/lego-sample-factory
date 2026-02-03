package io.life.order.service;

import io.life.order.client.InventoryClient;
import io.life.order.config.OrderProcessingConfig;
import io.life.order.entity.*;
import io.life.order.repository.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderOrchestrationService
 * 
 * Tests the centralized order flow orchestration including:
 * - Workstation order completion notifications
 * - Control order completion propagation
 * - Production order completion and submission
 * - Progress tracking for control orders and production orders
 * - Scenario 3 (via Warehouse) and Scenario 4 (direct) flows
 */
@ExtendWith(MockitoExtension.class)
class OrderOrchestrationServiceTest {

    // ========================
    // MOCKS
    // ========================
    
    @Mock
    private OrderProcessingConfig config;
    
    @Mock
    private OrderProcessingConfig.Workstations workstationsConfig;
    
    @Mock
    private ProductionControlOrderRepository productionControlOrderRepository;
    
    @Mock
    private AssemblyControlOrderRepository assemblyControlOrderRepository;
    
    @Mock
    private ProductionOrderRepository productionOrderRepository;
    
    @Mock
    private WarehouseOrderRepository warehouseOrderRepository;
    
    @Mock
    private CustomerOrderRepository customerOrderRepository;
    
    @Mock
    private InventoryClient inventoryClient;
    
    @Mock
    private FinalAssemblyOrderService finalAssemblyOrderService;
    
    @Mock
    private InjectionMoldingOrderRepository injectionMoldingOrderRepository;
    
    @Mock
    private PartPreProductionOrderRepository partPreProductionOrderRepository;
    
    @Mock
    private PartFinishingOrderRepository partFinishingOrderRepository;
    
    @Mock
    private GearAssemblyOrderRepository gearAssemblyOrderRepository;
    
    @Mock
    private MotorAssemblyOrderRepository motorAssemblyOrderRepository;
    
    @Mock
    private FinalAssemblyOrderRepository finalAssemblyOrderRepository;
    
    @InjectMocks
    private OrderOrchestrationService orchestrationService;

    // ========================
    // TEST FIXTURES
    // ========================
    
    private ProductionControlOrder testProductionControlOrder;
    private AssemblyControlOrder testAssemblyControlOrder;
    private ProductionOrder testProductionOrder;
    private WarehouseOrder testWarehouseOrder;
    private CustomerOrder testCustomerOrder;
    
    private static final Long PRODUCTION_CONTROL_ORDER_ID = 1L;
    private static final Long ASSEMBLY_CONTROL_ORDER_ID = 2L;
    private static final Long PRODUCTION_ORDER_ID = 3L;
    private static final Long WAREHOUSE_ORDER_ID = 4L;
    private static final Long CUSTOMER_ORDER_ID = 5L;
    
    private static final Long WS_FINAL_ASSEMBLY = 6L;
    private static final Long WS_PLANT_WAREHOUSE = 7L;
    private static final Long WS_MODULES_SUPERMARKET = 8L;
    private static final Long WS_PARTS_SUPPLY = 9L;

    @BeforeEach
    void setUp() {
        // Setup test Production Control Order
        testProductionControlOrder = new ProductionControlOrder();
        testProductionControlOrder.setId(PRODUCTION_CONTROL_ORDER_ID);
        testProductionControlOrder.setControlOrderNumber("PCO-TEST001");
        testProductionControlOrder.setStatus("IN_PROGRESS");
        testProductionControlOrder.setSourceProductionOrderId(PRODUCTION_ORDER_ID);
        
        // Setup test Assembly Control Order
        testAssemblyControlOrder = new AssemblyControlOrder();
        testAssemblyControlOrder.setId(ASSEMBLY_CONTROL_ORDER_ID);
        testAssemblyControlOrder.setControlOrderNumber("ACO-TEST001");
        testAssemblyControlOrder.setStatus("IN_PROGRESS");
        testAssemblyControlOrder.setSourceProductionOrderId(PRODUCTION_ORDER_ID);
        
        // Setup test Production Order
        testProductionOrder = new ProductionOrder();
        testProductionOrder.setId(PRODUCTION_ORDER_ID);
        testProductionOrder.setProductionOrderNumber("PRO-TEST001");
        testProductionOrder.setStatus("IN_PROGRESS");
        testProductionOrder.setSourceWarehouseOrderId(WAREHOUSE_ORDER_ID);
        
        // Setup test Warehouse Order
        testWarehouseOrder = new WarehouseOrder();
        testWarehouseOrder.setId(WAREHOUSE_ORDER_ID);
        testWarehouseOrder.setOrderNumber("WO-TEST001");
        testWarehouseOrder.setStatus("AWAITING_PRODUCTION");
        testWarehouseOrder.setCustomerOrderId(CUSTOMER_ORDER_ID);
        
        // Setup test Customer Order
        testCustomerOrder = new CustomerOrder();
        testCustomerOrder.setId(CUSTOMER_ORDER_ID);
        testCustomerOrder.setOrderNumber("CO-TEST001");
        testCustomerOrder.setStatus("PROCESSING");
        
        // Setup order items for production order
        ProductionOrderItem item1 = new ProductionOrderItem();
        item1.setId(1L);
        item1.setItemId(10L);
        item1.setItemName("Gear Module");
        item1.setQuantity(5);
        
        ProductionOrderItem item2 = new ProductionOrderItem();
        item2.setId(2L);
        item2.setItemId(11L);
        item2.setItemName("Motor Module");
        item2.setQuantity(5);
        
        testProductionOrder.setProductionOrderItems(List.of(item1, item2));
        
        // Setup order items for customer order
        OrderItem customerItem = new OrderItem();
        customerItem.setId(1L);
        customerItem.setItemType("PRODUCT");
        customerItem.setItemId(1L);
        customerItem.setQuantity(5);
        testCustomerOrder.setOrderItems(List.of(customerItem));
    }

    // ========================
    // WORKSTATION ORDER COMPLETION TESTS
    // ========================
    
    @Nested
    @DisplayName("Workstation Order Completion Tests")
    class WorkstationOrderCompletionTests {
        
        @Test
        @DisplayName("OOS-001: Complete InjectionMolding order updates control order progress")
        void testNotifyWorkstationOrderComplete_InjectionMolding_UpdatesProgress() {
            // Given
            List<InjectionMoldingOrder> orders = List.of(new InjectionMoldingOrder(), new InjectionMoldingOrder());
            when(injectionMoldingOrderRepository.findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(orders);
            when(injectionMoldingOrderRepository.countByProductionControlOrderIdAndStatus(
                    PRODUCTION_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(1L); // 1 of 2 complete
            
            // When
            orchestrationService.notifyWorkstationOrderComplete(
                    OrderOrchestrationService.WorkstationOrderType.INJECTION_MOLDING,
                    PRODUCTION_CONTROL_ORDER_ID);
            
            // Then
            verify(injectionMoldingOrderRepository).findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID);
            verify(injectionMoldingOrderRepository).countByProductionControlOrderIdAndStatus(
                    PRODUCTION_CONTROL_ORDER_ID, "COMPLETED");
            // Should NOT complete control order (only 1 of 2)
            verify(productionControlOrderRepository, never()).findById(any());
        }
        
        @Test
        @DisplayName("OOS-002: Complete last workstation order completes control order")
        void testNotifyWorkstationOrderComplete_LastInChain_CompletesControlOrder() {
            // Given
            List<InjectionMoldingOrder> orders = List.of(new InjectionMoldingOrder(), new InjectionMoldingOrder());
            when(injectionMoldingOrderRepository.findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(orders);
            when(injectionMoldingOrderRepository.countByProductionControlOrderIdAndStatus(
                    PRODUCTION_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(2L); // All 2 complete
            when(productionControlOrderRepository.findById(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(Optional.of(testProductionControlOrder));
            when(productionControlOrderRepository.save(any())).thenReturn(testProductionControlOrder);
            
            // Setup for control order completion check
            when(productionControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testProductionControlOrder));
            when(assemblyControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(Collections.emptyList());
            
            // When
            orchestrationService.notifyWorkstationOrderComplete(
                    OrderOrchestrationService.WorkstationOrderType.INJECTION_MOLDING,
                    PRODUCTION_CONTROL_ORDER_ID);
            
            // Then
            verify(productionControlOrderRepository).findById(PRODUCTION_CONTROL_ORDER_ID);
            verify(productionControlOrderRepository).save(argThat(order -> 
                    "COMPLETED".equals(order.getStatus()) && 
                    order.getActualFinishTime() != null
            ));
        }
        
        @Test
        @DisplayName("OOS-003: Complete GearAssembly order updates assembly control progress")
        void testNotifyWorkstationOrderComplete_GearAssembly_UpdatesAssemblyProgress() {
            // Given
            List<GearAssemblyOrder> orders = List.of(new GearAssemblyOrder(), new GearAssemblyOrder());
            when(gearAssemblyOrderRepository.findByAssemblyControlOrderId(ASSEMBLY_CONTROL_ORDER_ID))
                    .thenReturn(orders);
            when(gearAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    ASSEMBLY_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(1L); // 1 of 2 complete
            
            // When
            orchestrationService.notifyWorkstationOrderComplete(
                    OrderOrchestrationService.WorkstationOrderType.GEAR_ASSEMBLY,
                    ASSEMBLY_CONTROL_ORDER_ID);
            
            // Then
            verify(gearAssemblyOrderRepository).findByAssemblyControlOrderId(ASSEMBLY_CONTROL_ORDER_ID);
            verify(gearAssemblyOrderRepository).countByAssemblyControlOrderIdAndStatus(
                    ASSEMBLY_CONTROL_ORDER_ID, "COMPLETED");
        }
        
        @Test
        @DisplayName("OOS-004: Complete PartFinishing order checks correct repository")
        void testNotifyWorkstationOrderComplete_PartFinishing_ChecksCorrectRepository() {
            // Given
            List<PartFinishingOrder> orders = List.of(new PartFinishingOrder());
            when(partFinishingOrderRepository.findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(orders);
            when(partFinishingOrderRepository.countByProductionControlOrderIdAndStatus(
                    PRODUCTION_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(0L);
            
            // When
            orchestrationService.notifyWorkstationOrderComplete(
                    OrderOrchestrationService.WorkstationOrderType.PART_FINISHING,
                    PRODUCTION_CONTROL_ORDER_ID);
            
            // Then
            verify(partFinishingOrderRepository).findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID);
            verify(injectionMoldingOrderRepository, never()).findByProductionControlOrderId(any());
            verify(gearAssemblyOrderRepository, never()).findByAssemblyControlOrderId(any());
        }
        
        @Test
        @DisplayName("OOS-005: Complete FinalAssembly order checks assembly control")
        void testNotifyWorkstationOrderComplete_FinalAssembly_ChecksAssemblyControl() {
            // Given
            List<FinalAssemblyOrder> orders = List.of(new FinalAssemblyOrder());
            when(finalAssemblyOrderRepository.findByAssemblyControlOrderId(ASSEMBLY_CONTROL_ORDER_ID))
                    .thenReturn(orders);
            when(finalAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    ASSEMBLY_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(0L);
            
            // When
            orchestrationService.notifyWorkstationOrderComplete(
                    OrderOrchestrationService.WorkstationOrderType.FINAL_ASSEMBLY,
                    ASSEMBLY_CONTROL_ORDER_ID);
            
            // Then
            verify(finalAssemblyOrderRepository).findByAssemblyControlOrderId(ASSEMBLY_CONTROL_ORDER_ID);
            verify(productionControlOrderRepository, never()).findById(any());
        }
    }

    // ========================
    // CONTROL ORDER COMPLETION TESTS
    // ========================
    
    @Nested
    @DisplayName("Control Order Completion Tests")
    class ControlOrderCompletionTests {
        
        @Test
        @DisplayName("OOS-006: Complete ProductionControlOrder propagates to ProductionOrder")
        void testCompleteProductionControlOrder_PropagatesToProductionOrder() {
            // Given
            when(productionControlOrderRepository.findById(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(Optional.of(testProductionControlOrder));
            when(productionControlOrderRepository.save(any())).thenReturn(testProductionControlOrder);
            when(productionControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testProductionControlOrder));
            when(assemblyControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(Collections.emptyList());
            
            // When
            orchestrationService.completeProductionControlOrder(PRODUCTION_CONTROL_ORDER_ID);
            
            // Then
            verify(productionControlOrderRepository).save(argThat(order ->
                    "COMPLETED".equals(order.getStatus())
            ));
        }
        
        @Test
        @DisplayName("OOS-007: Already completed control order skips processing")
        void testCompleteProductionControlOrder_AlreadyCompleted_Skips() {
            // Given
            testProductionControlOrder.setStatus("COMPLETED");
            when(productionControlOrderRepository.findById(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(Optional.of(testProductionControlOrder));
            
            // When
            orchestrationService.completeProductionControlOrder(PRODUCTION_CONTROL_ORDER_ID);
            
            // Then
            verify(productionControlOrderRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("OOS-008: Complete AssemblyControlOrder sets actualFinishTime")
        void testCompleteAssemblyControlOrder_SetsActualFinishTime() {
            // Given
            when(assemblyControlOrderRepository.findById(ASSEMBLY_CONTROL_ORDER_ID))
                    .thenReturn(Optional.of(testAssemblyControlOrder));
            when(assemblyControlOrderRepository.save(any())).thenReturn(testAssemblyControlOrder);
            when(productionControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(Collections.emptyList());
            when(assemblyControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testAssemblyControlOrder));
            
            // When
            orchestrationService.completeAssemblyControlOrder(ASSEMBLY_CONTROL_ORDER_ID);
            
            // Then
            verify(assemblyControlOrderRepository).save(argThat(order ->
                    order.getActualFinishTime() != null
            ));
        }
        
        @Test
        @DisplayName("OOS-009: Control order without productionOrderId does not propagate")
        void testCompleteControlOrder_NoProductionOrderId_NoPropagation() {
            // Given
            testProductionControlOrder.setSourceProductionOrderId(null);
            when(productionControlOrderRepository.findById(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(Optional.of(testProductionControlOrder));
            when(productionControlOrderRepository.save(any())).thenReturn(testProductionControlOrder);
            
            // When
            orchestrationService.completeProductionControlOrder(PRODUCTION_CONTROL_ORDER_ID);
            
            // Then
            verify(productionControlOrderRepository).save(any());
            // Should not check for production order completion
            verify(productionControlOrderRepository, never()).findBySourceProductionOrderId(any());
        }
    }

    // ========================
    // PRODUCTION ORDER COMPLETION TESTS
    // ========================
    
    @Nested
    @DisplayName("Production Order Completion Tests")
    class ProductionOrderCompletionTests {
        
        @Test
        @DisplayName("OOS-010: All control orders complete triggers production completion")
        void testNotifyControlOrderComplete_AllComplete_TriggersProductionCompletion() {
            // Given - all control orders completed
            testProductionControlOrder.setStatus("COMPLETED");
            testAssemblyControlOrder.setStatus("COMPLETED");
            
            when(productionControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testProductionControlOrder));
            when(assemblyControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testAssemblyControlOrder));
            when(productionOrderRepository.findById(PRODUCTION_ORDER_ID))
                    .thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any())).thenReturn(testProductionOrder);
            when(warehouseOrderRepository.findById(WAREHOUSE_ORDER_ID))
                    .thenReturn(Optional.of(testWarehouseOrder));
            when(warehouseOrderRepository.save(any())).thenReturn(testWarehouseOrder);
            when(customerOrderRepository.findById(CUSTOMER_ORDER_ID))
                    .thenReturn(Optional.of(testCustomerOrder));
            when(config.getWorkstations()).thenReturn(workstationsConfig);
            when(workstationsConfig.getModulesSupermarket()).thenReturn(WS_MODULES_SUPERMARKET);
            
            // When
            orchestrationService.notifyControlOrderComplete(
                    OrderOrchestrationService.ControlOrderType.PRODUCTION_CONTROL,
                    PRODUCTION_ORDER_ID);
            
            // Then
            verify(productionOrderRepository).save(argThat(order ->
                    "COMPLETED".equals(order.getStatus())
            ));
        }
        
        @Test
        @DisplayName("OOS-011: Partial control order completion does not trigger production completion")
        void testNotifyControlOrderComplete_PartialComplete_NoProductionCompletion() {
            // Given - only one control order completed
            testProductionControlOrder.setStatus("COMPLETED");
            testAssemblyControlOrder.setStatus("IN_PROGRESS");
            
            when(productionControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testProductionControlOrder));
            when(assemblyControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testAssemblyControlOrder));
            
            // When
            orchestrationService.notifyControlOrderComplete(
                    OrderOrchestrationService.ControlOrderType.PRODUCTION_CONTROL,
                    PRODUCTION_ORDER_ID);
            
            // Then
            verify(productionOrderRepository, never()).findById(any());
            verify(productionOrderRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("OOS-012: Production completion auto-submits for Scenario 3")
        void testCompleteProductionOrder_Scenario3_CreditsModulesSupermarket() {
            // Given
            testProductionOrder.setStatus("IN_PROGRESS");
            when(productionOrderRepository.findById(PRODUCTION_ORDER_ID))
                    .thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any())).thenReturn(testProductionOrder);
            when(warehouseOrderRepository.findById(WAREHOUSE_ORDER_ID))
                    .thenReturn(Optional.of(testWarehouseOrder));
            when(warehouseOrderRepository.save(any())).thenReturn(testWarehouseOrder);
            when(customerOrderRepository.findById(CUSTOMER_ORDER_ID))
                    .thenReturn(Optional.of(testCustomerOrder));
            when(config.getWorkstations()).thenReturn(workstationsConfig);
            when(workstationsConfig.getModulesSupermarket()).thenReturn(WS_MODULES_SUPERMARKET);
            
            // When
            orchestrationService.completeProductionOrder(PRODUCTION_ORDER_ID);
            
            // Then
            // Verify inventory credit for modules
            verify(inventoryClient, times(2)).creditStock(
                    eq(WS_MODULES_SUPERMARKET),
                    eq(InventoryClient.ITEM_TYPE_MODULE),
                    anyLong(),
                    anyInt(),
                    eq(InventoryClient.REASON_PRODUCTION),
                    contains("PRO-TEST001")
            );
        }
        
        @Test
        @DisplayName("OOS-013: Production completion auto-submits for Scenario 4")
        void testCompleteProductionOrder_Scenario4_CreditsFinalAssembly() {
            // Given - Scenario 4: Direct from customer order
            testProductionOrder.setSourceWarehouseOrderId(null);
            testProductionOrder.setSourceCustomerOrderId(CUSTOMER_ORDER_ID);
            testProductionOrder.setStatus("IN_PROGRESS");
            
            when(productionOrderRepository.findById(PRODUCTION_ORDER_ID))
                    .thenReturn(Optional.of(testProductionOrder));
            when(productionOrderRepository.save(any())).thenReturn(testProductionOrder);
            when(customerOrderRepository.findById(CUSTOMER_ORDER_ID))
                    .thenReturn(Optional.of(testCustomerOrder));
            when(config.getWorkstations()).thenReturn(workstationsConfig);
            when(workstationsConfig.getFinalAssembly()).thenReturn(WS_FINAL_ASSEMBLY);
            
            // When
            orchestrationService.completeProductionOrder(PRODUCTION_ORDER_ID);
            
            // Then
            // Verify inventory credit for final assembly
            verify(inventoryClient, times(2)).creditStock(
                    eq(WS_FINAL_ASSEMBLY),
                    eq(InventoryClient.ITEM_TYPE_MODULE),
                    anyLong(),
                    anyInt(),
                    eq(InventoryClient.REASON_PRODUCTION),
                    contains("direct")
            );
            // Verify Final Assembly order creation
            verify(finalAssemblyOrderService).createFromProductionOrder(
                    eq(testProductionOrder),
                    eq(1L), // product ID
                    eq(5)   // quantity
            );
        }
        
        @Test
        @DisplayName("OOS-014: Already completed production order skips processing")
        void testCompleteProductionOrder_AlreadyCompleted_Skips() {
            // Given
            testProductionOrder.setStatus("COMPLETED");
            when(productionOrderRepository.findById(PRODUCTION_ORDER_ID))
                    .thenReturn(Optional.of(testProductionOrder));
            
            // When
            orchestrationService.completeProductionOrder(PRODUCTION_ORDER_ID);
            
            // Then
            verify(productionOrderRepository, never()).save(any());
            verify(inventoryClient, never()).creditStock(anyLong(), anyString(), anyLong(), anyInt(), anyString(), anyString());
        }
        
        @Test
        @DisplayName("OOS-015: Submit incomplete production order throws exception")
        void testSubmitProductionOrderCompletion_NotCompleted_ThrowsException() {
            // Given
            testProductionOrder.setStatus("IN_PROGRESS");
            when(productionOrderRepository.findById(PRODUCTION_ORDER_ID))
                    .thenReturn(Optional.of(testProductionOrder));
            
            // When/Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    orchestrationService.submitProductionOrderCompletion(PRODUCTION_ORDER_ID)
            );
            
            assertTrue(exception.getMessage().contains("must be COMPLETED"));
        }
    }

    // ========================
    // PROGRESS TRACKING TESTS
    // ========================
    
    @Nested
    @DisplayName("Progress Tracking Tests")
    class ProgressTrackingTests {
        
        @Test
        @DisplayName("OOS-016: Get production control order progress calculates correctly")
        void testGetProductionControlOrderProgress_CalculatesCorrectly() {
            // Given
            List<InjectionMoldingOrder> injectionOrders = List.of(new InjectionMoldingOrder(), new InjectionMoldingOrder());
            List<PartPreProductionOrder> preProductionOrders = List.of(new PartPreProductionOrder());
            List<PartFinishingOrder> finishingOrders = List.of(new PartFinishingOrder());
            
            when(injectionMoldingOrderRepository.findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(injectionOrders);
            when(injectionMoldingOrderRepository.countByProductionControlOrderIdAndStatus(
                    PRODUCTION_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(2L);
            when(partPreProductionOrderRepository.findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(preProductionOrders);
            when(partPreProductionOrderRepository.countByProductionControlOrderIdAndStatus(
                    PRODUCTION_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(1L);
            when(partFinishingOrderRepository.findByProductionControlOrderId(PRODUCTION_CONTROL_ORDER_ID))
                    .thenReturn(finishingOrders);
            when(partFinishingOrderRepository.countByProductionControlOrderIdAndStatus(
                    PRODUCTION_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(0L);
            
            // When
            OrderOrchestrationService.OrderProgress progress = 
                    orchestrationService.getProductionControlOrderProgress(PRODUCTION_CONTROL_ORDER_ID);
            
            // Then
            assertEquals(4, progress.getTotalOrders());
            assertEquals(3, progress.getCompletedOrders());
            assertEquals(75.0, progress.getPercentComplete(), 0.01);
            assertFalse(progress.isComplete());
        }
        
        @Test
        @DisplayName("OOS-017: Get assembly control order progress calculates correctly")
        void testGetAssemblyControlOrderProgress_CalculatesCorrectly() {
            // Given
            List<GearAssemblyOrder> gearOrders = List.of(new GearAssemblyOrder());
            List<MotorAssemblyOrder> motorOrders = List.of(new MotorAssemblyOrder());
            List<FinalAssemblyOrder> finalOrders = List.of(new FinalAssemblyOrder());
            
            when(gearAssemblyOrderRepository.findByAssemblyControlOrderId(ASSEMBLY_CONTROL_ORDER_ID))
                    .thenReturn(gearOrders);
            when(gearAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    ASSEMBLY_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(1L);
            when(motorAssemblyOrderRepository.findByAssemblyControlOrderId(ASSEMBLY_CONTROL_ORDER_ID))
                    .thenReturn(motorOrders);
            when(motorAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    ASSEMBLY_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(1L);
            when(finalAssemblyOrderRepository.findByAssemblyControlOrderId(ASSEMBLY_CONTROL_ORDER_ID))
                    .thenReturn(finalOrders);
            when(finalAssemblyOrderRepository.countByAssemblyControlOrderIdAndStatus(
                    ASSEMBLY_CONTROL_ORDER_ID, "COMPLETED"))
                    .thenReturn(1L);
            
            // When
            OrderOrchestrationService.OrderProgress progress = 
                    orchestrationService.getAssemblyControlOrderProgress(ASSEMBLY_CONTROL_ORDER_ID);
            
            // Then
            assertEquals(3, progress.getTotalOrders());
            assertEquals(3, progress.getCompletedOrders());
            assertEquals(100.0, progress.getPercentComplete(), 0.01);
            assertTrue(progress.isComplete());
        }
        
        @Test
        @DisplayName("OOS-018: Get production order progress aggregates control orders")
        void testGetProductionOrderProgress_AggregatesControlOrders() {
            // Given
            testProductionControlOrder.setStatus("COMPLETED");
            testAssemblyControlOrder.setStatus("IN_PROGRESS");
            
            when(productionControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testProductionControlOrder));
            when(assemblyControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(List.of(testAssemblyControlOrder));
            
            // When
            OrderOrchestrationService.OrderProgress progress = 
                    orchestrationService.getProductionOrderProgress(PRODUCTION_ORDER_ID);
            
            // Then
            assertEquals(2, progress.getTotalOrders());
            assertEquals(1, progress.getCompletedOrders());
            assertEquals(50.0, progress.getPercentComplete(), 0.01);
            assertFalse(progress.isComplete());
        }
        
        @Test
        @DisplayName("OOS-019: Empty orders returns zero progress")
        void testGetProgress_NoOrders_ReturnsZero() {
            // Given
            when(productionControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(Collections.emptyList());
            when(assemblyControlOrderRepository.findBySourceProductionOrderId(PRODUCTION_ORDER_ID))
                    .thenReturn(Collections.emptyList());
            
            // When
            OrderOrchestrationService.OrderProgress progress = 
                    orchestrationService.getProductionOrderProgress(PRODUCTION_ORDER_ID);
            
            // Then
            assertEquals(0, progress.getTotalOrders());
            assertEquals(0, progress.getCompletedOrders());
            assertEquals(0.0, progress.getPercentComplete(), 0.01);
            assertFalse(progress.isComplete()); // 0/0 is NOT complete
        }
    }

    // ========================
    // WAREHOUSE ORDER NOTIFICATION TESTS
    // ========================
    
    @Nested
    @DisplayName("Warehouse Order Notification Tests")
    class WarehouseOrderNotificationTests {
        
        @Test
        @DisplayName("OOS-020: Notify production complete updates warehouse order")
        void testNotifyProductionOrderComplete_UpdatesWarehouseOrder() {
            // Given
            when(warehouseOrderRepository.findById(WAREHOUSE_ORDER_ID))
                    .thenReturn(Optional.of(testWarehouseOrder));
            when(warehouseOrderRepository.save(any())).thenReturn(testWarehouseOrder);
            when(customerOrderRepository.findById(CUSTOMER_ORDER_ID))
                    .thenReturn(Optional.of(testCustomerOrder));
            
            // When
            orchestrationService.notifyProductionOrderComplete(WAREHOUSE_ORDER_ID);
            
            // Then
            verify(warehouseOrderRepository).save(argThat(order ->
                    "CONFIRMED".equals(order.getStatus()) &&
                    "DIRECT_FULFILLMENT".equals(order.getTriggerScenario())
            ));
        }
        
        @Test
        @DisplayName("OOS-021: Warehouse order with CONFIRMED status gets updated")
        void testNotifyProductionOrderComplete_ConfirmedStatus_Updates() {
            // Given
            testWarehouseOrder.setStatus("CONFIRMED");
            when(warehouseOrderRepository.findById(WAREHOUSE_ORDER_ID))
                    .thenReturn(Optional.of(testWarehouseOrder));
            when(warehouseOrderRepository.save(any())).thenReturn(testWarehouseOrder);
            when(customerOrderRepository.findById(CUSTOMER_ORDER_ID))
                    .thenReturn(Optional.of(testCustomerOrder));
            
            // When
            orchestrationService.notifyProductionOrderComplete(WAREHOUSE_ORDER_ID);
            
            // Then
            verify(warehouseOrderRepository).save(argThat(order ->
                    "DIRECT_FULFILLMENT".equals(order.getTriggerScenario())
            ));
        }
        
        @Test
        @DisplayName("OOS-022: Warehouse order with PROCESSING status gets updated")
        void testNotifyProductionOrderComplete_ProcessingStatus_Updates() {
            // Given
            testWarehouseOrder.setStatus("PROCESSING");
            when(warehouseOrderRepository.findById(WAREHOUSE_ORDER_ID))
                    .thenReturn(Optional.of(testWarehouseOrder));
            when(warehouseOrderRepository.save(any())).thenReturn(testWarehouseOrder);
            when(customerOrderRepository.findById(CUSTOMER_ORDER_ID))
                    .thenReturn(Optional.of(testCustomerOrder));
            
            // When
            orchestrationService.notifyProductionOrderComplete(WAREHOUSE_ORDER_ID);
            
            // Then
            verify(warehouseOrderRepository).save(any());
        }
        
        @Test
        @DisplayName("OOS-023: Warehouse order with unexpected status logs warning")
        void testNotifyProductionOrderComplete_UnexpectedStatus_LogsWarning() {
            // Given
            testWarehouseOrder.setStatus("COMPLETED"); // Unexpected status
            when(warehouseOrderRepository.findById(WAREHOUSE_ORDER_ID))
                    .thenReturn(Optional.of(testWarehouseOrder));
            when(customerOrderRepository.findById(CUSTOMER_ORDER_ID))
                    .thenReturn(Optional.of(testCustomerOrder));
            
            // When
            orchestrationService.notifyProductionOrderComplete(WAREHOUSE_ORDER_ID);
            
            // Then
            verify(warehouseOrderRepository, never()).save(any());
        }
    }

    // ========================
    // ERROR HANDLING TESTS
    // ========================
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("OOS-024: Production control order not found throws exception")
        void testCompleteProductionControlOrder_NotFound_ThrowsException() {
            // Given
            when(productionControlOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());
            
            // When/Then
            assertThrows(RuntimeException.class, () ->
                    orchestrationService.completeProductionControlOrder(999L)
            );
        }
        
        @Test
        @DisplayName("OOS-025: Assembly control order not found throws exception")
        void testCompleteAssemblyControlOrder_NotFound_ThrowsException() {
            // Given
            when(assemblyControlOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());
            
            // When/Then
            assertThrows(RuntimeException.class, () ->
                    orchestrationService.completeAssemblyControlOrder(999L)
            );
        }
        
        @Test
        @DisplayName("OOS-026: Production order not found throws exception")
        void testCompleteProductionOrder_NotFound_ThrowsException() {
            // Given
            when(productionOrderRepository.findById(999L))
                    .thenReturn(Optional.empty());
            
            // When/Then
            assertThrows(RuntimeException.class, () ->
                    orchestrationService.completeProductionOrder(999L)
            );
        }
        
        @Test
        @DisplayName("OOS-027: Submit with no source order throws exception")
        void testSubmitProductionOrderCompletion_NoSourceOrder_ThrowsException() {
            // Given
            testProductionOrder.setStatus("COMPLETED");
            testProductionOrder.setSourceWarehouseOrderId(null);
            testProductionOrder.setSourceCustomerOrderId(null);
            when(productionOrderRepository.findById(PRODUCTION_ORDER_ID))
                    .thenReturn(Optional.of(testProductionOrder));
            
            // When/Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    orchestrationService.submitProductionOrderCompletion(PRODUCTION_ORDER_ID)
            );
            
            assertTrue(exception.getMessage().contains("neither warehouse nor customer"));
        }
    }

    // ========================
    // INVENTORY CREDIT TESTS
    // ========================
    
    @Nested
    @DisplayName("Inventory Credit Tests")
    class InventoryCreditTests {
        
        @Test
        @DisplayName("OOS-028: Credit Modules Supermarket with correct items")
        void testCreditModulesSupermarketFromProduction_CreditsAllItems() {
            // Given
            when(config.getWorkstations()).thenReturn(workstationsConfig);
            when(workstationsConfig.getModulesSupermarket()).thenReturn(WS_MODULES_SUPERMARKET);
            
            // When
            orchestrationService.creditModulesSupermarketFromProduction(testProductionOrder);
            
            // Then
            verify(inventoryClient, times(2)).creditStock(
                    eq(WS_MODULES_SUPERMARKET),
                    eq(InventoryClient.ITEM_TYPE_MODULE),
                    anyLong(),
                    eq(5), // quantity from test fixture
                    eq(InventoryClient.REASON_PRODUCTION),
                    anyString()
            );
        }
        
        @Test
        @DisplayName("OOS-029: Credit Final Assembly with correct items")
        void testCreditFinalAssemblyFromProduction_CreditsAllItems() {
            // Given
            when(config.getWorkstations()).thenReturn(workstationsConfig);
            when(workstationsConfig.getFinalAssembly()).thenReturn(WS_FINAL_ASSEMBLY);
            
            // When
            orchestrationService.creditFinalAssemblyFromProduction(testProductionOrder);
            
            // Then
            verify(inventoryClient, times(2)).creditStock(
                    eq(WS_FINAL_ASSEMBLY),
                    eq(InventoryClient.ITEM_TYPE_MODULE),
                    anyLong(),
                    eq(5),
                    eq(InventoryClient.REASON_PRODUCTION),
                    contains("direct")
            );
        }
        
        @Test
        @DisplayName("OOS-030: Empty production items logs warning, no credit")
        void testCreditFromProduction_EmptyItems_NoCredit() {
            // Given
            testProductionOrder.setProductionOrderItems(Collections.emptyList());
            // Note: No config mock needed - method returns early when items empty
            
            // When
            orchestrationService.creditModulesSupermarketFromProduction(testProductionOrder);
            
            // Then
            verify(inventoryClient, never()).creditStock(anyLong(), anyString(), anyLong(), anyInt(), anyString(), anyString());
        }
    }

    // ========================
    // ORDER PROGRESS INNER CLASS TESTS
    // ========================
    
    @Nested
    @DisplayName("OrderProgress Class Tests")
    class OrderProgressClassTests {
        
        @Test
        @DisplayName("OrderProgress calculates percentage correctly")
        void testOrderProgress_PercentCalculation() {
            OrderOrchestrationService.OrderProgress progress = 
                    new OrderOrchestrationService.OrderProgress(10, 7);
            
            assertEquals(10, progress.getTotalOrders());
            assertEquals(7, progress.getCompletedOrders());
            assertEquals(70.0, progress.getPercentComplete(), 0.01);
            assertFalse(progress.isComplete());
        }
        
        @Test
        @DisplayName("OrderProgress 100% is complete")
        void testOrderProgress_FullComplete() {
            OrderOrchestrationService.OrderProgress progress = 
                    new OrderOrchestrationService.OrderProgress(5, 5);
            
            assertEquals(100.0, progress.getPercentComplete(), 0.01);
            assertTrue(progress.isComplete());
        }
        
        @Test
        @DisplayName("OrderProgress zero total returns 0%")
        void testOrderProgress_ZeroTotal() {
            OrderOrchestrationService.OrderProgress progress = 
                    new OrderOrchestrationService.OrderProgress(0, 0);
            
            assertEquals(0.0, progress.getPercentComplete(), 0.01);
            assertFalse(progress.isComplete());
        }
        
        @Test
        @DisplayName("OrderProgress toString formats correctly")
        void testOrderProgress_ToString() {
            OrderOrchestrationService.OrderProgress progress = 
                    new OrderOrchestrationService.OrderProgress(4, 3);
            
            assertEquals("3/4 (75.0%)", progress.toString());
        }
    }
}
