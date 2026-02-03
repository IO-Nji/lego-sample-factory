package io.life.simal_integration_service.service;

import io.life.simal_integration_service.dto.SimalScheduledOrderResponse;
import io.life.simal_integration_service.dto.SimalScheduledOrderResponse.ScheduledTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ControlOrderIntegrationService.
 * 
 * Tests the integration between SimAL production schedules and 
 * Control Order creation in order-processing-service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ControlOrderIntegrationService Tests")
class ControlOrderIntegrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ControlOrderIntegrationService controlOrderIntegrationService;

    private static final String ORDER_PROCESSING_API_URL = "http://localhost:8015/api";
    private static final Long PRODUCTION_ORDER_ID = 100L;

    @BeforeEach
    void setUp() {
        controlOrderIntegrationService = new ControlOrderIntegrationService(restTemplate);
        
        // Set the API base URLs using reflection
        ReflectionTestUtils.setField(controlOrderIntegrationService, 
                "orderProcessingApiBaseUrl", ORDER_PROCESSING_API_URL);
        ReflectionTestUtils.setField(controlOrderIntegrationService, 
                "simalApiBaseUrl", "http://localhost:8016/api");
    }

    // ==================== Test Data Builders ====================

    private SimalScheduledOrderResponse createSchedule(String scheduleId, List<ScheduledTask> tasks) {
        return SimalScheduledOrderResponse.builder()
                .scheduleId(scheduleId)
                .orderNumber("PO-" + PRODUCTION_ORDER_ID)
                .status("SCHEDULED")
                .estimatedCompletionTime("2026-02-03T12:00:00")
                .scheduledTasks(tasks)
                .totalDuration(120)
                .build();
    }

    private ScheduledTask createTask(String taskId, String workstationId, String itemId, 
                                     String itemName, int quantity) {
        return ScheduledTask.builder()
                .taskId(taskId)
                .workstationId(workstationId)
                .workstationName("Workstation " + workstationId)
                .itemId(itemId)
                .itemName(itemName)
                .quantity(quantity)
                .startTime("2026-02-03T09:00:00")
                .endTime("2026-02-03T10:00:00")
                .duration(60)
                .build();
    }

    // ==================== createControlOrdersFromSchedule Tests ====================

    @Nested
    @DisplayName("createControlOrdersFromSchedule()")
    class CreateControlOrdersFromScheduleTests {

        @Test
        @DisplayName("Should return empty map when schedule has no tasks")
        void shouldReturnEmptyMapWhenNoTasks() {
            SimalScheduledOrderResponse schedule = createSchedule("SCH-001", null);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).isEmpty();
            verify(restTemplate, never()).postForObject(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return empty map when schedule has empty task list")
        void shouldReturnEmptyMapWhenEmptyTasks() {
            SimalScheduledOrderResponse schedule = createSchedule("SCH-001", new ArrayList<>());
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should create ProductionControlOrder for WS-1 tasks")
        void shouldCreateProductionControlOrderForWS1() {
            ScheduledTask task = createTask("TASK-1", "WS-1", "101", "Injection Part A", 5);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-001", List.of(task));
            
            // Mock successful order creation
            Map<String, Object> response = Map.of("controlOrderNumber", "PCO-12345678");
            when(restTemplate.postForObject(
                    eq(ORDER_PROCESSING_API_URL + "/production-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-1");
            assertThat(result.get("WS-1")).isEqualTo("PCO-12345678");
            
            verify(restTemplate).postForObject(
                    eq(ORDER_PROCESSING_API_URL + "/production-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            );
        }

        @Test
        @DisplayName("Should create ProductionControlOrder for WS-2 tasks")
        void shouldCreateProductionControlOrderForWS2() {
            ScheduledTask task = createTask("TASK-2", "WS-2", "102", "Pre-production Part", 3);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-002", List.of(task));
            
            Map<String, Object> response = Map.of("controlOrderNumber", "PCO-ABCD1234");
            when(restTemplate.postForObject(
                    contains("/production-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-2");
            assertThat(result.get("WS-2")).isEqualTo("PCO-ABCD1234");
        }

        @Test
        @DisplayName("Should create ProductionControlOrder for WS-3 tasks")
        void shouldCreateProductionControlOrderForWS3() {
            ScheduledTask task = createTask("TASK-3", "WS-3", "103", "Finishing Part", 10);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-003", List.of(task));
            
            Map<String, Object> response = Map.of("controlOrderNumber", "PCO-FINISH01");
            when(restTemplate.postForObject(
                    contains("/production-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-3");
        }

        @Test
        @DisplayName("Should create AssemblyControlOrder for WS-4 tasks")
        void shouldCreateAssemblyControlOrderForWS4() {
            ScheduledTask task = createTask("TASK-4", "WS-4", "201", "Gear Module", 2);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-004", List.of(task));
            
            Map<String, Object> response = Map.of("controlOrderNumber", "ACO-GEAR0001");
            when(restTemplate.postForObject(
                    eq(ORDER_PROCESSING_API_URL + "/assembly-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-4");
            assertThat(result.get("WS-4")).isEqualTo("ACO-GEAR0001");
            
            verify(restTemplate).postForObject(
                    eq(ORDER_PROCESSING_API_URL + "/assembly-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            );
        }

        @Test
        @DisplayName("Should create AssemblyControlOrder for WS-5 tasks")
        void shouldCreateAssemblyControlOrderForWS5() {
            ScheduledTask task = createTask("TASK-5", "WS-5", "202", "Motor Module", 4);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-005", List.of(task));
            
            Map<String, Object> response = Map.of("controlOrderNumber", "ACO-MOTOR001");
            when(restTemplate.postForObject(
                    contains("/assembly-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-5");
            assertThat(result.get("WS-5")).isEqualTo("ACO-MOTOR001");
        }

        @Test
        @DisplayName("Should create AssemblyControlOrder for WS-6 tasks")
        void shouldCreateAssemblyControlOrderForWS6() {
            ScheduledTask task = createTask("TASK-6", "WS-6", "203", "Final Assembly", 1);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-006", List.of(task));
            
            Map<String, Object> response = Map.of("controlOrderNumber", "ACO-FINAL001");
            when(restTemplate.postForObject(
                    contains("/assembly-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(response);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-6");
        }

        @Test
        @DisplayName("Should group tasks by workstation and create multiple control orders")
        void shouldGroupTasksByWorkstationAndCreateMultipleControlOrders() {
            List<ScheduledTask> tasks = List.of(
                    createTask("TASK-1", "WS-1", "101", "Part A", 5),
                    createTask("TASK-2", "WS-1", "102", "Part B", 3),
                    createTask("TASK-3", "WS-4", "201", "Module A", 2),
                    createTask("TASK-4", "WS-5", "202", "Module B", 2)
            );
            SimalScheduledOrderResponse schedule = createSchedule("SCH-MULTI", tasks);
            
            // Mock responses for production and assembly control orders
            when(restTemplate.postForObject(
                    contains("/production-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(Map.of("controlOrderNumber", "PCO-WS1"));
            
            when(restTemplate.postForObject(
                    contains("/assembly-control-orders"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(
                    Map.of("controlOrderNumber", "ACO-WS4"),
                    Map.of("controlOrderNumber", "ACO-WS5")
            );
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            // Should have 3 control orders: 1 for WS-1 (grouped), 1 for WS-4, 1 for WS-5
            assertThat(result).hasSize(3);
            assertThat(result).containsKeys("WS-1", "WS-4", "WS-5");
        }

        @Test
        @DisplayName("Should handle RestClientException gracefully and continue")
        void shouldHandleRestClientExceptionGracefully() {
            ScheduledTask task = createTask("TASK-1", "WS-1", "101", "Part A", 5);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-ERR", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RestClientException("Connection refused"));
            
            // Should not throw exception - gracefully handle the error
            assertThatCode(() -> controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should generate fallback control order number when response missing number")
        void shouldGenerateFallbackControlOrderNumber() {
            ScheduledTask task = createTask("TASK-1", "WS-1", "101", "Part A", 5);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-FALLBACK", List.of(task));
            
            // Return response without controlOrderNumber
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("id", 123));
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            // Should get fallback UUID-based number
            assertThat(result).containsKey("WS-1");
            assertThat(result.get("WS-1")).startsWith("PCO-");
        }

        @Test
        @DisplayName("Should not create control orders for warehouse workstations")
        void shouldNotCreateControlOrdersForWarehouseWorkstations() {
            List<ScheduledTask> tasks = List.of(
                    createTask("TASK-7", "WS-7", "301", "Plant Warehouse Task", 1),
                    createTask("TASK-8", "WS-8", "302", "Modules Supermarket Task", 1),
                    createTask("TASK-9", "WS-9", "303", "Parts Supply Task", 1)
            );
            SimalScheduledOrderResponse schedule = createSchedule("SCH-WH", tasks);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            // Warehouse workstations should not create control orders
            assertThat(result).doesNotContainKeys("WS-7", "WS-8", "WS-9");
            verify(restTemplate, never()).postForObject(anyString(), any(), any());
        }
    }

    // ==================== Request DTO Validation Tests ====================

    @Nested
    @DisplayName("Request DTO Building")
    class RequestDTOBuildingTests {

        @Test
        @DisplayName("Should send correct data in ProductionControlOrder request")
        void shouldSendCorrectDataInProductionControlOrderRequest() {
            ScheduledTask task = createTask("TASK-1", "WS-1", "101", "Test Part", 5);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-001", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("controlOrderNumber", "PCO-TEST"));
            
            controlOrderIntegrationService.createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
            
            HttpEntity<?> entity = captor.getValue();
            assertThat(entity).isNotNull();
            assertThat(entity.getHeaders().getContentType().toString()).contains("application/json");
        }

        @Test
        @DisplayName("Should send correct data in AssemblyControlOrder request")
        void shouldSendCorrectDataInAssemblyControlOrderRequest() {
            ScheduledTask task = createTask("TASK-4", "WS-4", "201", "Test Module", 3);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-002", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("controlOrderNumber", "ACO-TEST"));
            
            controlOrderIntegrationService.createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForObject(
                    eq(ORDER_PROCESSING_API_URL + "/assembly-control-orders"),
                    captor.capture(),
                    eq(Map.class)
            );
            
            HttpEntity<?> entity = captor.getValue();
            assertThat(entity).isNotNull();
        }
    }

    // ==================== Workstation Type Determination Tests ====================

    @Nested
    @DisplayName("Workstation Type Determination")
    class WorkstationTypeDeterminationTests {

        @ParameterizedTest
        @CsvSource({
            "WS-1, production-control-orders",
            "WS-2, production-control-orders",
            "WS-3, production-control-orders"
        })
        @DisplayName("Manufacturing workstations should create ProductionControlOrder")
        void manufacturingWorkstationsShouldCreateProductionControlOrder(
                String workstationId, String expectedEndpoint) {
            
            ScheduledTask task = createTask("TASK-1", workstationId, "101", "Part", 1);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-MFG", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("controlOrderNumber", "PCO-TEST"));
            
            controlOrderIntegrationService.createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            verify(restTemplate).postForObject(
                    contains(expectedEndpoint),
                    any(HttpEntity.class),
                    eq(Map.class)
            );
        }

        @ParameterizedTest
        @CsvSource({
            "WS-4, assembly-control-orders",
            "WS-5, assembly-control-orders",
            "WS-6, assembly-control-orders"
        })
        @DisplayName("Assembly workstations should create AssemblyControlOrder")
        void assemblyWorkstationsShouldCreateAssemblyControlOrder(
                String workstationId, String expectedEndpoint) {
            
            ScheduledTask task = createTask("TASK-1", workstationId, "201", "Module", 1);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-ASM", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("controlOrderNumber", "ACO-TEST"));
            
            controlOrderIntegrationService.createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            verify(restTemplate).postForObject(
                    contains(expectedEndpoint),
                    any(HttpEntity.class),
                    eq(Map.class)
            );
        }
    }

    // ==================== Task Grouping Tests ====================

    @Nested
    @DisplayName("Task Grouping")
    class TaskGroupingTests {

        @Test
        @DisplayName("Should group multiple tasks for same workstation into single control order")
        void shouldGroupMultipleTasksForSameWorkstation() {
            List<ScheduledTask> tasks = List.of(
                    createTask("TASK-1A", "WS-1", "101", "Part A", 5),
                    createTask("TASK-1B", "WS-1", "102", "Part B", 3),
                    createTask("TASK-1C", "WS-1", "103", "Part C", 2)
            );
            SimalScheduledOrderResponse schedule = createSchedule("SCH-GROUP", tasks);
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("controlOrderNumber", "PCO-GROUPED"));
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            // Only 1 control order should be created (all tasks grouped for WS-1)
            assertThat(result).hasSize(1);
            assertThat(result).containsKey("WS-1");
            
            // Only 1 API call should be made
            verify(restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
        }

        @Test
        @DisplayName("Should maintain separate control orders for different workstations")
        void shouldMaintainSeparateControlOrdersForDifferentWorkstations() {
            List<ScheduledTask> tasks = List.of(
                    createTask("TASK-1", "WS-1", "101", "Part", 1),
                    createTask("TASK-2", "WS-2", "102", "Part", 1),
                    createTask("TASK-3", "WS-3", "103", "Part", 1)
            );
            SimalScheduledOrderResponse schedule = createSchedule("SCH-MULTI", tasks);
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(
                            Map.of("controlOrderNumber", "PCO-WS1"),
                            Map.of("controlOrderNumber", "PCO-WS2"),
                            Map.of("controlOrderNumber", "PCO-WS3")
                    );
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).hasSize(3);
            assertThat(result).containsKeys("WS-1", "WS-2", "WS-3");
            
            verify(restTemplate, times(3)).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null itemId in task")
        void shouldHandleNullItemIdInTask() {
            ScheduledTask task = ScheduledTask.builder()
                    .taskId("TASK-NULL")
                    .workstationId("WS-1")
                    .itemId(null)  // null itemId
                    .itemName("Part with no ID")
                    .quantity(1)
                    .startTime("2026-02-03T09:00:00")
                    .endTime("2026-02-03T10:00:00")
                    .build();
            SimalScheduledOrderResponse schedule = createSchedule("SCH-NULL", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("controlOrderNumber", "PCO-NULL"));
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-1");
        }

        @Test
        @DisplayName("Should handle non-numeric itemId in task")
        void shouldHandleNonNumericItemIdInTask() {
            ScheduledTask task = ScheduledTask.builder()
                    .taskId("TASK-NAN")
                    .workstationId("WS-1")
                    .itemId("NOT-A-NUMBER")  // non-numeric itemId
                    .itemName("Part with bad ID")
                    .quantity(1)
                    .startTime("2026-02-03T09:00:00")
                    .endTime("2026-02-03T10:00:00")
                    .build();
            SimalScheduledOrderResponse schedule = createSchedule("SCH-NAN", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(Map.of("controlOrderNumber", "PCO-NAN"));
            
            // Should not throw exception
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            assertThat(result).containsKey("WS-1");
        }

        @Test
        @DisplayName("Should handle null response from REST call")
        void shouldHandleNullResponseFromRestCall() {
            ScheduledTask task = createTask("TASK-1", "WS-1", "101", "Part", 1);
            SimalScheduledOrderResponse schedule = createSchedule("SCH-NULLRESP", List.of(task));
            
            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(null);
            
            Map<String, String> result = controlOrderIntegrationService
                    .createControlOrdersFromSchedule(schedule, PRODUCTION_ORDER_ID);
            
            // Should get fallback UUID-based number
            assertThat(result).containsKey("WS-1");
            assertThat(result.get("WS-1")).startsWith("PCO-");
        }
    }
}
