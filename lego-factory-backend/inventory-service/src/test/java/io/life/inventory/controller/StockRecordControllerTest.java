package io.life.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.life.inventory.dto.StockRecordDto;
import io.life.inventory.service.StockRecordService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for StockRecordController.
 * Tests REST API endpoints for inventory stock management.
 * 
 * Stock records track inventory levels across all 9 workstations.
 */
@WebMvcTest(StockRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StockRecordController Tests")
class StockRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockRecordService stockRecordService;

    private StockRecordDto stockRecord1;
    private StockRecordDto stockRecord2;
    private StockRecordDto stockRecord3;

    @BeforeEach
    void setUp() {
        Mockito.reset(stockRecordService);

        stockRecord1 = new StockRecordDto();
        stockRecord1.setId(1L);
        stockRecord1.setWorkstationId(7L);
        stockRecord1.setItemType("PRODUCT");
        stockRecord1.setItemId(1L);
        stockRecord1.setItemName("LEGO Model Car - Red");
        stockRecord1.setQuantity(50);
        stockRecord1.setLastUpdated(LocalDateTime.now());

        stockRecord2 = new StockRecordDto();
        stockRecord2.setId(2L);
        stockRecord2.setWorkstationId(8L);
        stockRecord2.setItemType("MODULE");
        stockRecord2.setItemId(7L);
        stockRecord2.setItemName("Gear Module");
        stockRecord2.setQuantity(100);
        stockRecord2.setLastUpdated(LocalDateTime.now());

        stockRecord3 = new StockRecordDto();
        stockRecord3.setId(3L);
        stockRecord3.setWorkstationId(9L);
        stockRecord3.setItemType("PART");
        stockRecord3.setItemId(101L);
        stockRecord3.setItemName("Gear Shaft");
        stockRecord3.setQuantity(500);
        stockRecord3.setLastUpdated(LocalDateTime.now());
    }

    // =========================================================================
    // GET /api/stock
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock")
    class GetAllStockRecordsTests {

        @Test
        @DisplayName("Should return all stock records")
        void getAllStockRecords_ReturnsAll() throws Exception {
            when(stockRecordService.findAll())
                    .thenReturn(Arrays.asList(stockRecord1, stockRecord2, stockRecord3));

            mockMvc.perform(get("/api/stock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].workstationId", is(7)))
                    .andExpect(jsonPath("$[0].itemType", is("PRODUCT")))
                    .andExpect(jsonPath("$[1].workstationId", is(8)))
                    .andExpect(jsonPath("$[2].workstationId", is(9)));

            verify(stockRecordService).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no stock records exist")
        void getAllStockRecords_Empty() throws Exception {
            when(stockRecordService.findAll())
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/stock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // GET /api/stock/{id}
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock/{id}")
    class GetStockRecordByIdTests {

        @Test
        @DisplayName("Should return stock record when found by ID")
        void getById_Found() throws Exception {
            when(stockRecordService.findById(1L))
                    .thenReturn(stockRecord1);

            mockMvc.perform(get("/api/stock/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.workstationId", is(7)))
                    .andExpect(jsonPath("$.itemType", is("PRODUCT")))
                    .andExpect(jsonPath("$.itemId", is(1)))
                    .andExpect(jsonPath("$.itemName", is("LEGO Model Car - Red")))
                    .andExpect(jsonPath("$.quantity", is(50)));
        }

        @Test
        @DisplayName("Should return 404 when stock record not found")
        void getById_NotFound() throws Exception {
            when(stockRecordService.findById(999L))
                    .thenReturn(null);

            mockMvc.perform(get("/api/stock/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/stock/workstation/{workstationId}
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock/workstation/{workstationId}")
    class GetStockByWorkstationTests {

        @Test
        @DisplayName("Should return stock records for WS-7 Plant Warehouse")
        void getByWorkstation_PlantWarehouse() throws Exception {
            when(stockRecordService.getStockByWorkstationId(7L))
                    .thenReturn(Collections.singletonList(stockRecord1));

            mockMvc.perform(get("/api/stock/workstation/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].workstationId", is(7)))
                    .andExpect(jsonPath("$[0].itemType", is("PRODUCT")));
        }

        @Test
        @DisplayName("Should return stock records for WS-9 Parts Supply")
        void getByWorkstation_PartsSupply() throws Exception {
            when(stockRecordService.getStockByWorkstationId(9L))
                    .thenReturn(Collections.singletonList(stockRecord3));

            mockMvc.perform(get("/api/stock/workstation/9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].itemType", is("PART")));
        }

        @Test
        @DisplayName("Should return empty list for workstation with no stock")
        void getByWorkstation_Empty() throws Exception {
            when(stockRecordService.getStockByWorkstationId(1L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/stock/workstation/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // GET /api/stock/workstation/{workstationId}/item
    // =========================================================================

    @Nested
    @DisplayName("GET /api/stock/workstation/{workstationId}/item")
    class GetStockByWorkstationAndItemTests {

        @Test
        @DisplayName("Should return specific stock record for workstation and item")
        void getByWorkstationAndItem_Found() throws Exception {
            when(stockRecordService.getStockByWorkstationAndItem(7L, "PRODUCT", 1L))
                    .thenReturn(stockRecord1);

            mockMvc.perform(get("/api/stock/workstation/7/item")
                            .param("itemType", "PRODUCT")
                            .param("itemId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workstationId", is(7)))
                    .andExpect(jsonPath("$.itemType", is("PRODUCT")))
                    .andExpect(jsonPath("$.itemId", is(1)))
                    .andExpect(jsonPath("$.quantity", is(50)));
        }

        @Test
        @DisplayName("Should return 404 when item not found at workstation")
        void getByWorkstationAndItem_NotFound() throws Exception {
            when(stockRecordService.getStockByWorkstationAndItem(7L, "MODULE", 99L))
                    .thenReturn(null);

            mockMvc.perform(get("/api/stock/workstation/7/item")
                            .param("itemType", "MODULE")
                            .param("itemId", "99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should find module stock at WS-8")
        void getByWorkstationAndItem_Module() throws Exception {
            when(stockRecordService.getStockByWorkstationAndItem(8L, "MODULE", 7L))
                    .thenReturn(stockRecord2);

            mockMvc.perform(get("/api/stock/workstation/8/item")
                            .param("itemType", "MODULE")
                            .param("itemId", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itemName", is("Gear Module")))
                    .andExpect(jsonPath("$.quantity", is(100)));
        }
    }

    // =========================================================================
    // POST /api/stock/update
    // =========================================================================

    @Nested
    @DisplayName("POST /api/stock/update")
    class UpdateStockTests {

        @Test
        @DisplayName("Should update stock quantity")
        void updateStock_Success() throws Exception {
            StockRecordDto updatedRecord = new StockRecordDto();
            updatedRecord.setId(1L);
            updatedRecord.setWorkstationId(7L);
            updatedRecord.setItemType("PRODUCT");
            updatedRecord.setItemId(1L);
            updatedRecord.setQuantity(75);
            updatedRecord.setLastUpdated(LocalDateTime.now());

            when(stockRecordService.updateStock(7L, "PRODUCT", 1L, 75))
                    .thenReturn(updatedRecord);

            mockMvc.perform(post("/api/stock/update")
                            .param("workstationId", "7")
                            .param("itemType", "PRODUCT")
                            .param("itemId", "1")
                            .param("quantity", "75"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity", is(75)));

            verify(stockRecordService).updateStock(7L, "PRODUCT", 1L, 75);
        }

        @Test
        @DisplayName("Should create stock record if not exists")
        void updateStock_CreateNew() throws Exception {
            StockRecordDto newRecord = new StockRecordDto();
            newRecord.setId(10L);
            newRecord.setWorkstationId(6L);
            newRecord.setItemType("PRODUCT");
            newRecord.setItemId(2L);
            newRecord.setQuantity(20);

            when(stockRecordService.updateStock(6L, "PRODUCT", 2L, 20))
                    .thenReturn(newRecord);

            mockMvc.perform(post("/api/stock/update")
                            .param("workstationId", "6")
                            .param("itemType", "PRODUCT")
                            .param("itemId", "2")
                            .param("quantity", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workstationId", is(6)))
                    .andExpect(jsonPath("$.quantity", is(20)));
        }
    }

    // =========================================================================
    // POST /api/stock (Create)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/stock")
    class CreateStockRecordTests {

        @Test
        @DisplayName("Should create new stock record")
        void create_Success() throws Exception {
            StockRecordDto newRecord = new StockRecordDto();
            newRecord.setWorkstationId(5L);
            newRecord.setItemType("MODULE");
            newRecord.setItemId(8L);
            newRecord.setQuantity(30);

            StockRecordDto savedRecord = new StockRecordDto();
            savedRecord.setId(100L);
            savedRecord.setWorkstationId(5L);
            savedRecord.setItemType("MODULE");
            savedRecord.setItemId(8L);
            savedRecord.setQuantity(30);
            savedRecord.setLastUpdated(LocalDateTime.now());

            when(stockRecordService.save(any(StockRecordDto.class)))
                    .thenReturn(savedRecord);

            mockMvc.perform(post("/api/stock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newRecord)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(100)))
                    .andExpect(jsonPath("$.workstationId", is(5)))
                    .andExpect(jsonPath("$.quantity", is(30)));
        }
    }

    // =========================================================================
    // DELETE /api/stock/{id}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/stock/{id}")
    class DeleteStockRecordTests {

        @Test
        @DisplayName("Should delete stock record")
        void delete_Success() throws Exception {
            doNothing().when(stockRecordService).deleteById(1L);

            mockMvc.perform(delete("/api/stock/1"))
                    .andExpect(status().isNoContent());

            verify(stockRecordService).deleteById(1L);
        }

        @Test
        @DisplayName("Should return 204 even if record doesn't exist")
        void delete_NotExisting() throws Exception {
            doNothing().when(stockRecordService).deleteById(999L);

            mockMvc.perform(delete("/api/stock/999"))
                    .andExpect(status().isNoContent());
        }
    }
}
