package io.life.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.life.inventory.dto.LowStockAlertDto;
import io.life.inventory.dto.LowStockThresholdDto;
import io.life.inventory.service.LowStockAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for StockAlertController.
 * Tests REST API endpoints for low stock alerts and threshold management.
 * 
 * Alerts help operators identify items that need restocking.
 */
@WebMvcTest(StockAlertController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StockAlertController Tests")
class StockAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LowStockAlertService lowStockAlertService;

    private LowStockAlertDto alert1;
    private LowStockAlertDto alert2;
    private LowStockThresholdDto threshold1;
    private LowStockThresholdDto threshold2;

    @BeforeEach
    void setUp() {
        Mockito.reset(lowStockAlertService);

        // Low stock alert: Product at WS-7 below threshold
        alert1 = new LowStockAlertDto();
        alert1.setWorkstationId(7L);
        alert1.setItemType("PRODUCT");
        alert1.setItemId(1L);
        alert1.setQuantity(5);
        alert1.setThreshold(20);
        alert1.setDeficit(15);

        // Low stock alert: Module at WS-8 below threshold
        alert2 = new LowStockAlertDto();
        alert2.setWorkstationId(8L);
        alert2.setItemType("MODULE");
        alert2.setItemId(7L);
        alert2.setQuantity(8);
        alert2.setThreshold(30);
        alert2.setDeficit(22);

        // Threshold configuration for products at WS-7
        threshold1 = new LowStockThresholdDto();
        threshold1.setId(1L);
        threshold1.setWorkstationId(7L);
        threshold1.setItemType("PRODUCT");
        threshold1.setItemId(1L);
        threshold1.setThreshold(20);

        // Global threshold for all modules
        threshold2 = new LowStockThresholdDto();
        threshold2.setId(2L);
        threshold2.setWorkstationId(null); // Global
        threshold2.setItemType("MODULE");
        threshold2.setItemId(null); // All modules
        threshold2.setThreshold(30);
    }

    // =========================================================================
    // GET /api/stock/alerts/low
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock/alerts/low")
    class GetLowStockAlertsTests {

        @Test
        @DisplayName("Should return all low stock alerts")
        void getLowStockAlerts_All() throws Exception {
            when(lowStockAlertService.getLowStockAlerts(null))
                    .thenReturn(Arrays.asList(alert1, alert2));

            mockMvc.perform(get("/api/stock/alerts/low"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].workstationId", is(7)))
                    .andExpect(jsonPath("$[0].deficit", is(15)))
                    .andExpect(jsonPath("$[1].workstationId", is(8)));

            verify(lowStockAlertService).getLowStockAlerts(null);
        }

        @Test
        @DisplayName("Should filter alerts by workstation")
        void getLowStockAlerts_FilterByWorkstation() throws Exception {
            when(lowStockAlertService.getLowStockAlerts(7L))
                    .thenReturn(Collections.singletonList(alert1));

            mockMvc.perform(get("/api/stock/alerts/low")
                            .param("workstationId", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].workstationId", is(7)))
                    .andExpect(jsonPath("$[0].itemType", is("PRODUCT")));

            verify(lowStockAlertService).getLowStockAlerts(7L);
        }

        @Test
        @DisplayName("Should return empty list when no alerts")
        void getLowStockAlerts_Empty() throws Exception {
            when(lowStockAlertService.getLowStockAlerts(null))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/stock/alerts/low"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return alerts for Parts Supply (WS-9)")
        void getLowStockAlerts_PartsSupply() throws Exception {
            LowStockAlertDto partAlert = new LowStockAlertDto();
            partAlert.setWorkstationId(9L);
            partAlert.setItemType("PART");
            partAlert.setItemId(101L);
            partAlert.setQuantity(10);
            partAlert.setThreshold(100);
            partAlert.setDeficit(90);

            when(lowStockAlertService.getLowStockAlerts(9L))
                    .thenReturn(Collections.singletonList(partAlert));

            mockMvc.perform(get("/api/stock/alerts/low")
                            .param("workstationId", "9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].itemType", is("PART")))
                    .andExpect(jsonPath("$[0].deficit", is(90)));
        }
    }

    // =========================================================================
    // GET /api/stock/alerts/thresholds
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock/alerts/thresholds")
    class ListThresholdsTests {

        @Test
        @DisplayName("Should return all threshold configurations")
        void listThresholds_All() throws Exception {
            when(lowStockAlertService.listThresholds())
                    .thenReturn(Arrays.asList(threshold1, threshold2));

            mockMvc.perform(get("/api/stock/alerts/thresholds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].workstationId", is(7)))
                    .andExpect(jsonPath("$[0].threshold", is(20)))
                    .andExpect(jsonPath("$[1].workstationId", nullValue())); // Global threshold
        }

        @Test
        @DisplayName("Should return empty list when no thresholds configured")
        void listThresholds_Empty() throws Exception {
            when(lowStockAlertService.listThresholds())
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/stock/alerts/thresholds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // PUT /api/stock/alerts/thresholds
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/stock/alerts/thresholds")
    class UpsertThresholdsTests {

        @Test
        @DisplayName("Should create new thresholds")
        void upsertThresholds_Create() throws Exception {
            LowStockThresholdDto newThreshold = new LowStockThresholdDto();
            newThreshold.setWorkstationId(6L);
            newThreshold.setItemType("PRODUCT");
            newThreshold.setItemId(2L);
            newThreshold.setThreshold(15);

            LowStockThresholdDto savedThreshold = new LowStockThresholdDto();
            savedThreshold.setId(10L);
            savedThreshold.setWorkstationId(6L);
            savedThreshold.setItemType("PRODUCT");
            savedThreshold.setItemId(2L);
            savedThreshold.setThreshold(15);

            when(lowStockAlertService.upsertThresholds(anyList()))
                    .thenReturn(Collections.singletonList(savedThreshold));

            mockMvc.perform(put("/api/stock/alerts/thresholds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Collections.singletonList(newThreshold))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(10)))
                    .andExpect(jsonPath("$[0].threshold", is(15)));
        }

        @Test
        @DisplayName("Should update existing thresholds")
        void upsertThresholds_Update() throws Exception {
            LowStockThresholdDto updateThreshold = new LowStockThresholdDto();
            updateThreshold.setId(1L);
            updateThreshold.setWorkstationId(7L);
            updateThreshold.setItemType("PRODUCT");
            updateThreshold.setItemId(1L);
            updateThreshold.setThreshold(25); // Updated from 20

            when(lowStockAlertService.upsertThresholds(anyList()))
                    .thenReturn(Collections.singletonList(updateThreshold));

            mockMvc.perform(put("/api/stock/alerts/thresholds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Collections.singletonList(updateThreshold))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].threshold", is(25)));
        }

        @Test
        @DisplayName("Should upsert multiple thresholds")
        void upsertThresholds_Multiple() throws Exception {
            when(lowStockAlertService.upsertThresholds(anyList()))
                    .thenReturn(Arrays.asList(threshold1, threshold2));

            mockMvc.perform(put("/api/stock/alerts/thresholds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Arrays.asList(threshold1, threshold2))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(lowStockAlertService).upsertThresholds(anyList());
        }

        @Test
        @DisplayName("Should handle empty threshold list")
        void upsertThresholds_Empty() throws Exception {
            when(lowStockAlertService.upsertThresholds(anyList()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(put("/api/stock/alerts/thresholds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should create global threshold (null workstationId)")
        void upsertThresholds_GlobalThreshold() throws Exception {
            LowStockThresholdDto globalThreshold = new LowStockThresholdDto();
            globalThreshold.setWorkstationId(null); // Global
            globalThreshold.setItemType("PART");
            globalThreshold.setItemId(null); // All parts
            globalThreshold.setThreshold(50);

            LowStockThresholdDto savedGlobal = new LowStockThresholdDto();
            savedGlobal.setId(100L);
            savedGlobal.setWorkstationId(null);
            savedGlobal.setItemType("PART");
            savedGlobal.setItemId(null);
            savedGlobal.setThreshold(50);

            when(lowStockAlertService.upsertThresholds(anyList()))
                    .thenReturn(Collections.singletonList(savedGlobal));

            mockMvc.perform(put("/api/stock/alerts/thresholds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Collections.singletonList(globalThreshold))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].workstationId", nullValue()))
                    .andExpect(jsonPath("$[0].itemId", nullValue()));
        }
    }
}
