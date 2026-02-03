package io.life.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.life.inventory.dto.StockAdjustmentRequest;
import io.life.inventory.dto.StockLedgerEntryDto;
import io.life.inventory.service.StockLedgerService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for StockLedgerController.
 * Tests REST API endpoints for stock adjustment and transaction history.
 * 
 * Stock ledger provides audit trail for all inventory changes.
 * CRITICAL: delta field is signed integer (positive=credit, negative=debit)
 */
@WebMvcTest(StockLedgerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StockLedgerController Tests")
class StockLedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockLedgerService stockLedgerService;

    private StockLedgerEntryDto creditEntry;
    private StockLedgerEntryDto debitEntry;
    private StockLedgerEntryDto adjustmentEntry;

    @BeforeEach
    void setUp() {
        Mockito.reset(stockLedgerService);

        creditEntry = new StockLedgerEntryDto();
        creditEntry.setId(1L);
        creditEntry.setWorkstationId(7L);
        creditEntry.setItemType("PRODUCT");
        creditEntry.setItemId(1L);
        creditEntry.setDelta(10);
        creditEntry.setBalanceAfter(60);
        creditEntry.setReasonCode("PRODUCTION_COMPLETE");
        creditEntry.setNotes("Final assembly completed");
        creditEntry.setCreatedAt(LocalDateTime.now());

        debitEntry = new StockLedgerEntryDto();
        debitEntry.setId(2L);
        debitEntry.setWorkstationId(7L);
        debitEntry.setItemType("PRODUCT");
        debitEntry.setItemId(1L);
        debitEntry.setDelta(-5);
        debitEntry.setBalanceAfter(55);
        debitEntry.setReasonCode("FULFILLMENT");
        debitEntry.setNotes("Customer order fulfilled");
        debitEntry.setCreatedAt(LocalDateTime.now());

        adjustmentEntry = new StockLedgerEntryDto();
        adjustmentEntry.setId(3L);
        adjustmentEntry.setWorkstationId(8L);
        adjustmentEntry.setItemType("MODULE");
        adjustmentEntry.setItemId(7L);
        adjustmentEntry.setDelta(-2);
        adjustmentEntry.setBalanceAfter(98);
        adjustmentEntry.setReasonCode("ADJUSTMENT");
        adjustmentEntry.setNotes("Inventory correction");
        adjustmentEntry.setCreatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // POST /api/stock/adjust
    // =========================================================================

    @Nested
    @DisplayName("POST /api/stock/adjust")
    class AdjustStockTests {

        @Test
        @DisplayName("Should credit stock (positive delta)")
        void adjust_Credit() throws Exception {
            StockAdjustmentRequest request = new StockAdjustmentRequest();
            request.setWorkstationId(7L);
            request.setItemType("PRODUCT");
            request.setItemId(1L);
            request.setDelta(10);
            request.setReasonCode("PRODUCTION_COMPLETE");
            request.setNotes("Final assembly completed");

            when(stockLedgerService.adjustStock(any(StockAdjustmentRequest.class)))
                    .thenReturn(creditEntry);

            mockMvc.perform(post("/api/stock/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.delta", is(10)))
                    .andExpect(jsonPath("$.balanceAfter", is(60)))
                    .andExpect(jsonPath("$.reasonCode", is("PRODUCTION_COMPLETE")));

            verify(stockLedgerService).adjustStock(any(StockAdjustmentRequest.class));
        }

        @Test
        @DisplayName("Should debit stock (negative delta)")
        void adjust_Debit() throws Exception {
            StockAdjustmentRequest request = new StockAdjustmentRequest();
            request.setWorkstationId(7L);
            request.setItemType("PRODUCT");
            request.setItemId(1L);
            request.setDelta(-5);
            request.setReasonCode("FULFILLMENT");
            request.setNotes("Customer order fulfilled");

            when(stockLedgerService.adjustStock(any(StockAdjustmentRequest.class)))
                    .thenReturn(debitEntry);

            mockMvc.perform(post("/api/stock/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.delta", is(-5)))
                    .andExpect(jsonPath("$.balanceAfter", is(55)))
                    .andExpect(jsonPath("$.reasonCode", is("FULFILLMENT")));
        }

        @Test
        @DisplayName("Should adjust module stock at WS-8")
        void adjust_ModuleStock() throws Exception {
            StockAdjustmentRequest request = new StockAdjustmentRequest();
            request.setWorkstationId(8L);
            request.setItemType("MODULE");
            request.setItemId(7L);
            request.setDelta(-2);
            request.setReasonCode("ADJUSTMENT");

            when(stockLedgerService.adjustStock(any(StockAdjustmentRequest.class)))
                    .thenReturn(adjustmentEntry);

            mockMvc.perform(post("/api/stock/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workstationId", is(8)))
                    .andExpect(jsonPath("$.itemType", is("MODULE")));
        }
    }

    // =========================================================================
    // GET /api/stock/ledger
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock/ledger")
    class GetStockHistoryTests {

        @Test
        @DisplayName("Should return all ledger entries without filters")
        void history_AllEntries() throws Exception {
            when(stockLedgerService.history(null, null, null))
                    .thenReturn(Arrays.asList(creditEntry, debitEntry, adjustmentEntry));

            mockMvc.perform(get("/api/stock/ledger"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].reasonCode", is("PRODUCTION_COMPLETE")))
                    .andExpect(jsonPath("$[1].reasonCode", is("FULFILLMENT")));
        }

        @Test
        @DisplayName("Should filter by workstation ID")
        void history_FilterByWorkstation() throws Exception {
            when(stockLedgerService.history(7L, null, null))
                    .thenReturn(Arrays.asList(creditEntry, debitEntry));

            mockMvc.perform(get("/api/stock/ledger")
                            .param("workstationId", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].workstationId", is(7)));
        }

        @Test
        @DisplayName("Should filter by item type")
        void history_FilterByItemType() throws Exception {
            when(stockLedgerService.history(null, "MODULE", null))
                    .thenReturn(Collections.singletonList(adjustmentEntry));

            mockMvc.perform(get("/api/stock/ledger")
                            .param("itemType", "MODULE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].itemType", is("MODULE")));
        }

        @Test
        @DisplayName("Should filter by item ID")
        void history_FilterByItemId() throws Exception {
            when(stockLedgerService.history(null, null, 1L))
                    .thenReturn(Arrays.asList(creditEntry, debitEntry));

            mockMvc.perform(get("/api/stock/ledger")
                            .param("itemId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should filter by all parameters")
        void history_FilterByAll() throws Exception {
            when(stockLedgerService.history(7L, "PRODUCT", 1L))
                    .thenReturn(Arrays.asList(creditEntry, debitEntry));

            mockMvc.perform(get("/api/stock/ledger")
                            .param("workstationId", "7")
                            .param("itemType", "PRODUCT")
                            .param("itemId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should return empty list when no history found")
        void history_Empty() throws Exception {
            when(stockLedgerService.history(99L, null, null))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/stock/ledger")
                            .param("workstationId", "99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // GET /api/stock/ledger/recent
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock/ledger/recent")
    class GetRecentEntriesTests {

        @Test
        @DisplayName("Should return recent entries with default limit")
        void recent_DefaultLimit() throws Exception {
            when(stockLedgerService.recent(20))
                    .thenReturn(Arrays.asList(creditEntry, debitEntry));

            mockMvc.perform(get("/api/stock/ledger/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(stockLedgerService).recent(20);
        }

        @Test
        @DisplayName("Should return recent entries with custom limit")
        void recent_CustomLimit() throws Exception {
            when(stockLedgerService.recent(5))
                    .thenReturn(Collections.singletonList(creditEntry));

            mockMvc.perform(get("/api/stock/ledger/recent")
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(stockLedgerService).recent(5);
        }

        @Test
        @DisplayName("Should return empty list when no recent entries")
        void recent_Empty() throws Exception {
            when(stockLedgerService.recent(20))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/stock/ledger/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return large batch with high limit")
        void recent_LargeLimit() throws Exception {
            when(stockLedgerService.recent(100))
                    .thenReturn(Arrays.asList(creditEntry, debitEntry, adjustmentEntry));

            mockMvc.perform(get("/api/stock/ledger/recent")
                            .param("limit", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }
    }
}
