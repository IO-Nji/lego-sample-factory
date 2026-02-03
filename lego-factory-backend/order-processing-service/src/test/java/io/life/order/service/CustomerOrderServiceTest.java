package io.life.order.service;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.dto.OrderItemDTO;
import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.repository.CustomerOrderRepository;
import io.life.order.repository.WarehouseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerOrderService
 * Tests core business logic for customer order management
 */
@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private WarehouseOrderRepository warehouseOrderRepository;

    @Mock
    private FinalAssemblyOrderService finalAssemblyOrderService;

    @Mock
    private OrderAuditService orderAuditService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private CustomerOrderService customerOrderService;

    private CustomerOrderDTO testOrderDTO;
    private CustomerOrder testOrder;

    @BeforeEach
    void setUp() {
        // Setup test order DTO
        testOrderDTO = new CustomerOrderDTO();
        testOrderDTO.setWorkstationId(7L);
        testOrderDTO.setNotes("Test order");

        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setItemType("PRODUCT");
        itemDTO.setItemId(1L);
        itemDTO.setQuantity(5);
        itemDTO.setFulfilledQuantity(0);
        itemDTO.setNotes("Test item");

        List<OrderItemDTO> items = new ArrayList<>();
        items.add(itemDTO);
        testOrderDTO.setOrderItems(items);

        // Setup test order entity
        testOrder = new CustomerOrder();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-TEST001");
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setStatus("PENDING");
        testOrder.setWorkstationId(7L);
        testOrder.setNotes("Test order");

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setItemType("PRODUCT");
        orderItem.setItemId(1L);
        orderItem.setQuantity(5);
        orderItem.setFulfilledQuantity(0);
        orderItem.setNotes("Test item");
        orderItem.setCustomerOrder(testOrder);

        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(orderItem);
        testOrder.setOrderItems(orderItems);
    }

    @Test
    void testCreateOrder_Success() {
        // Given
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenReturn(testOrder);

        // When
        CustomerOrderDTO result = customerOrderService.createOrder(testOrderDTO);

        // Then
        assertNotNull(result);
        assertEquals("ORD-TEST001", result.getOrderNumber());
        assertEquals("PENDING", result.getStatus());
        assertEquals(7L, result.getWorkstationId());
        assertEquals(1, result.getOrderItems().size());

        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
        verify(orderAuditService, times(1)).recordOrderEvent(
                eq("CUSTOMER"),
                anyLong(),
                eq("CREATED"),
                anyString()
        );
    }

    @Test
    void testCreateOrder_WithMultipleItems() {
        // Given
        OrderItemDTO item2 = new OrderItemDTO();
        item2.setItemType("PRODUCT");
        item2.setItemId(2L);
        item2.setQuantity(3);
        testOrderDTO.getOrderItems().add(item2);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setId(2L);
        orderItem2.setItemType("PRODUCT");
        orderItem2.setItemId(2L);
        orderItem2.setQuantity(3);
        orderItem2.setCustomerOrder(testOrder);
        testOrder.getOrderItems().add(orderItem2);

        when(customerOrderRepository.save(any(CustomerOrder.class))).thenReturn(testOrder);

        // When
        CustomerOrderDTO result = customerOrderService.createOrder(testOrderDTO);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getOrderItems().size());
        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
    }

    @Test
    void testGetOrderById_Found() {
        // Given
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Optional<CustomerOrderDTO> result = customerOrderService.getOrderById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("ORD-TEST001", result.get().getOrderNumber());
        verify(customerOrderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderById_NotFound() {
        // Given
        when(customerOrderRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<CustomerOrderDTO> result = customerOrderService.getOrderById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(customerOrderRepository, times(1)).findById(999L);
    }

    @Test
    void testGetOrderByNumber_Found() {
        // Given
        when(customerOrderRepository.findByOrderNumber("ORD-TEST001"))
                .thenReturn(Optional.of(testOrder));

        // When
        Optional<CustomerOrderDTO> result = customerOrderService.getOrderByNumber("ORD-TEST001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("ORD-TEST001", result.get().getOrderNumber());
        verify(customerOrderRepository, times(1)).findByOrderNumber("ORD-TEST001");
    }

    @Test
    void testGetAllOrders() {
        // Given
        List<CustomerOrder> orders = new ArrayList<>();
        orders.add(testOrder);
        when(customerOrderRepository.findAll()).thenReturn(orders);

        // When
        List<CustomerOrderDTO> result = customerOrderService.getAllOrders();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-TEST001", result.get(0).getOrderNumber());
        verify(customerOrderRepository, times(1)).findAll();
    }

    @Test
    void testGetOrdersByWorkstationId() {
        // Given
        List<CustomerOrder> orders = new ArrayList<>();
        orders.add(testOrder);
        when(customerOrderRepository.findByWorkstationId(7L)).thenReturn(orders);

        // When
        List<CustomerOrderDTO> result = customerOrderService.getOrdersByWorkstationId(7L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getWorkstationId());
        verify(customerOrderRepository, times(1)).findByWorkstationId(7L);
    }

    @Test
    void testGetOrdersByStatus() {
        // Given
        List<CustomerOrder> orders = new ArrayList<>();
        orders.add(testOrder);
        when(customerOrderRepository.findByStatus("PENDING")).thenReturn(orders);

        // When
        List<CustomerOrderDTO> result = customerOrderService.getOrdersByStatus("PENDING");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        verify(customerOrderRepository, times(1)).findByStatus("PENDING");
    }

    @Test
    void testUpdateOrderStatus() {
        // Given
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenReturn(testOrder);

        // When
        CustomerOrderDTO result = customerOrderService.updateOrderStatus(1L, "CONFIRMED");

        // Then
        assertNotNull(result);
        verify(customerOrderRepository, times(1)).findById(1L);
        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
        verify(orderAuditService, times(1)).recordOrderEvent(
                eq("CUSTOMER"),
                eq(1L),
                eq("STATUS_CONFIRMED"),
                anyString()
        );
    }

    @Test
    void testConfirmOrder_Success() {
        // Given
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setStatus("CONFIRMED");
            return order;
        });
        // Mock dependencies for confirmOrder
        when(systemConfigService.getLotSizeThreshold()).thenReturn(3);
        when(inventoryService.checkStock(anyLong(), anyLong(), anyInt())).thenReturn(true);

        // When
        CustomerOrderDTO result = customerOrderService.confirmOrder(1L);

        // Then
        assertNotNull(result);
        verify(customerOrderRepository, times(1)).findById(1L);
        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
        verify(orderAuditService, times(1)).recordOrderEvent(
                eq("CUSTOMER"),
                eq(1L),
                eq("CONFIRMED"),
                anyString()
        );
    }

    @Test
    void testConfirmOrder_InvalidStatus() {
        // Given
        testOrder.setStatus("COMPLETED");
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            customerOrderService.confirmOrder(1L);
        });

        verify(customerOrderRepository, times(1)).findById(1L);
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void testConfirmOrder_NotFound() {
        // Given
        when(customerOrderRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            customerOrderService.confirmOrder(999L);
        });

        verify(customerOrderRepository, times(1)).findById(999L);
    }

    @Test
    void testMarkProcessing_Success() {
        // Given
        testOrder.setStatus("CONFIRMED");
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setStatus("PROCESSING");
            return order;
        });

        // When
        CustomerOrderDTO result = customerOrderService.markProcessing(1L);

        // Then
        assertNotNull(result);
        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
    }

    @Test
    void testCompleteOrder_Success() {
        // Given
        testOrder.setStatus("PROCESSING");
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setStatus("COMPLETED");
            return order;
        });

        // When
        CustomerOrderDTO result = customerOrderService.completeOrder(1L);

        // Then
        assertNotNull(result);
        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
        verify(orderAuditService, times(1)).recordOrderEvent(
                eq("CUSTOMER"),
                eq(1L),
                eq("COMPLETED"),
                anyString()
        );
    }

    @Test
    void testCompleteOrder_InvalidStatus() {
        // Given
        testOrder.setStatus("PENDING");
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            customerOrderService.completeOrder(1L);
        });

        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void testCancelOrder_Success() {
        // Given
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setStatus("CANCELLED");
            return order;
        });

        // When
        CustomerOrderDTO result = customerOrderService.cancelOrder(1L);

        // Then
        assertNotNull(result);
        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
        verify(orderAuditService, times(1)).recordOrderEvent(
                eq("CUSTOMER"),
                eq(1L),
                eq("CANCELLED"),
                anyString()
        );
    }

    @Test
    void testCancelOrder_CompletedOrder() {
        // Given
        testOrder.setStatus("COMPLETED");
        when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            customerOrderService.cancelOrder(1L);
        });

        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void testDeleteOrder() {
        // When
        customerOrderService.deleteOrder(1L);

        // Then
        verify(customerOrderRepository, times(1)).deleteById(1L);
    }
}
