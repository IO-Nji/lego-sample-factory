package io.life.inventory.service;

import io.life.inventory.dto.StockRecordDto;
import io.life.inventory.entity.StockRecord;
import io.life.inventory.repository.StockRecordRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockRecordService.
 * 
 * Key behaviors tested:
 * - Stock record retrieval (by ID, workstation, item)
 * - Stock record creation and updates
 * - DTO mapping with masterdata enrichment
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockRecordService Tests")
class StockRecordServiceTest {

    @Mock
    private StockRecordRepository repository;

    @Mock
    private MasterdataClient masterdataClient;

    @InjectMocks
    private StockRecordService stockRecordService;

    private StockRecord testStockRecord;
    private StockRecordDto testStockRecordDto;

    @BeforeEach
    void setUp() {
        testStockRecord = new StockRecord();
        testStockRecord.setId(1L);
        testStockRecord.setWorkstationId(7L);
        testStockRecord.setItemType("PRODUCT");
        testStockRecord.setItemId(1L);
        testStockRecord.setQuantity(100);
        testStockRecord.setLastUpdated(LocalDateTime.now());

        testStockRecordDto = new StockRecordDto(
                1L, 7L, "PRODUCT", 1L, "Test Product", 100, LocalDateTime.now()
        );

        // Common mock setup
        when(masterdataClient.getItemName(anyString(), anyLong())).thenReturn("Test Item");
        when(repository.save(any(StockRecord.class))).thenAnswer(invocation -> {
            StockRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(1L);
            }
            return record;
        });
    }

    @Nested
    @DisplayName("Find All Tests")
    class FindAllTests {

        @Test
        @DisplayName("SRS-001: findAll returns all stock records")
        void findAll_returnsAllStockRecords() {
            StockRecord record2 = new StockRecord();
            record2.setId(2L);
            record2.setWorkstationId(8L);
            record2.setItemType("MODULE");
            record2.setItemId(7L);
            record2.setQuantity(50);
            record2.setLastUpdated(LocalDateTime.now());

            when(repository.findAll()).thenReturn(List.of(testStockRecord, record2));

            List<StockRecordDto> result = stockRecordService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getWorkstationId()).isEqualTo(7L);
            assertThat(result.get(1).getWorkstationId()).isEqualTo(8L);
        }

        @Test
        @DisplayName("SRS-002: findAll returns empty list when no records")
        void findAll_returnsEmptyListWhenNoRecords() {
            when(repository.findAll()).thenReturn(Collections.emptyList());

            List<StockRecordDto> result = stockRecordService.findAll();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("SRS-003: findAll enriches item names from masterdata")
        void findAll_enrichesItemNamesFromMasterdata() {
            when(repository.findAll()).thenReturn(List.of(testStockRecord));
            when(masterdataClient.getItemName("PRODUCT", 1L)).thenReturn("LEGO Car Model");

            List<StockRecordDto> result = stockRecordService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getItemName()).isEqualTo("LEGO Car Model");
            verify(masterdataClient).getItemName("PRODUCT", 1L);
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("SRS-004: findById returns record when exists")
        void findById_returnsRecordWhenExists() {
            when(repository.findById(1L)).thenReturn(Optional.of(testStockRecord));

            StockRecordDto result = stockRecordService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("SRS-005: findById returns null when not found")
        void findById_returnsNullWhenNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            StockRecordDto result = stockRecordService.findById(999L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("SRS-006: findById enriches item name from masterdata")
        void findById_enrichesItemName() {
            when(repository.findById(1L)).thenReturn(Optional.of(testStockRecord));
            when(masterdataClient.getItemName("PRODUCT", 1L)).thenReturn("Blue LEGO Car");

            StockRecordDto result = stockRecordService.findById(1L);

            assertThat(result.getItemName()).isEqualTo("Blue LEGO Car");
        }
    }

    @Nested
    @DisplayName("Get Stock By Workstation Tests")
    class GetStockByWorkstationTests {

        @Test
        @DisplayName("SRS-007: getStockByWorkstationId returns records for workstation")
        void getStockByWorkstationId_returnsRecordsForWorkstation() {
            when(repository.findByWorkstationId(7L)).thenReturn(List.of(testStockRecord));

            List<StockRecordDto> result = stockRecordService.getStockByWorkstationId(7L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkstationId()).isEqualTo(7L);
            verify(repository).findByWorkstationId(7L);
        }

        @Test
        @DisplayName("SRS-008: getStockByWorkstationId returns empty list for workstation with no stock")
        void getStockByWorkstationId_returnsEmptyListForNoStock() {
            when(repository.findByWorkstationId(99L)).thenReturn(Collections.emptyList());

            List<StockRecordDto> result = stockRecordService.getStockByWorkstationId(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Stock By Workstation And Item Tests")
    class GetStockByWorkstationAndItemTests {

        @Test
        @DisplayName("SRS-009: getStockByWorkstationAndItem returns record when exists")
        void getStockByWorkstationAndItem_returnsRecordWhenExists() {
            when(repository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockRecordDto result = stockRecordService.getStockByWorkstationAndItem(7L, "PRODUCT", 1L);

            assertThat(result).isNotNull();
            assertThat(result.getWorkstationId()).isEqualTo(7L);
            assertThat(result.getItemType()).isEqualTo("PRODUCT");
            assertThat(result.getItemId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("SRS-010: getStockByWorkstationAndItem returns null when not found")
        void getStockByWorkstationAndItem_returnsNullWhenNotFound() {
            when(repository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 999L))
                    .thenReturn(Optional.empty());

            StockRecordDto result = stockRecordService.getStockByWorkstationAndItem(7L, "PRODUCT", 999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Update Stock Tests")
    class UpdateStockTests {

        @Test
        @DisplayName("SRS-011: updateStock updates existing record")
        void updateStock_updatesExistingRecord() {
            when(repository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockRecordDto result = stockRecordService.updateStock(7L, "PRODUCT", 1L, 150);

            assertThat(result).isNotNull();
            assertThat(testStockRecord.getQuantity()).isEqualTo(150);
            verify(repository).save(testStockRecord);
        }

        @Test
        @DisplayName("SRS-012: updateStock creates new record when not exists")
        void updateStock_createsNewRecordWhenNotExists() {
            when(repository.findByWorkstationIdAndItemTypeAndItemId(8L, "MODULE", 7L))
                    .thenReturn(Optional.empty());

            StockRecordDto result = stockRecordService.updateStock(8L, "MODULE", 7L, 25);

            assertThat(result).isNotNull();
            ArgumentCaptor<StockRecord> captor = ArgumentCaptor.forClass(StockRecord.class);
            verify(repository).save(captor.capture());
            
            StockRecord saved = captor.getValue();
            assertThat(saved.getWorkstationId()).isEqualTo(8L);
            assertThat(saved.getItemType()).isEqualTo("MODULE");
            assertThat(saved.getItemId()).isEqualTo(7L);
            assertThat(saved.getQuantity()).isEqualTo(25);
        }

        @Test
        @DisplayName("SRS-013: updateStock sets lastUpdated timestamp")
        void updateStock_setsLastUpdatedTimestamp() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            when(repository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            stockRecordService.updateStock(7L, "PRODUCT", 1L, 200);

            assertThat(testStockRecord.getLastUpdated()).isAfter(before);
        }
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("SRS-014: save creates new stock record from DTO")
        void save_createsNewStockRecordFromDto() {
            StockRecordDto newDto = new StockRecordDto(
                    null, 9L, "PART", 101L, "Gear Part", 500, null
            );

            StockRecordDto result = stockRecordService.save(newDto);

            assertThat(result).isNotNull();
            ArgumentCaptor<StockRecord> captor = ArgumentCaptor.forClass(StockRecord.class);
            verify(repository).save(captor.capture());

            StockRecord saved = captor.getValue();
            assertThat(saved.getWorkstationId()).isEqualTo(9L);
            assertThat(saved.getItemType()).isEqualTo("PART");
            assertThat(saved.getItemId()).isEqualTo(101L);
            assertThat(saved.getQuantity()).isEqualTo(500);
        }

        @Test
        @DisplayName("SRS-015: save sets lastUpdated on new record")
        void save_setsLastUpdatedOnNewRecord() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            StockRecordDto newDto = new StockRecordDto(
                    null, 9L, "PART", 101L, null, 100, null
            );

            stockRecordService.save(newDto);

            ArgumentCaptor<StockRecord> captor = ArgumentCaptor.forClass(StockRecord.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getLastUpdated()).isAfter(before);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("SRS-016: deleteById calls repository delete")
        void deleteById_callsRepositoryDelete() {
            doNothing().when(repository).deleteById(1L);

            stockRecordService.deleteById(1L);

            verify(repository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("SRS-017: handles null item name from masterdata gracefully")
        void handlesNullItemNameFromMasterdata() {
            when(repository.findById(1L)).thenReturn(Optional.of(testStockRecord));
            when(masterdataClient.getItemName("PRODUCT", 1L)).thenReturn(null);

            StockRecordDto result = stockRecordService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getItemName()).isNull();
        }

        @Test
        @DisplayName("SRS-018: updateStock with zero quantity is allowed")
        void updateStock_withZeroQuantityIsAllowed() {
            when(repository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockRecordDto result = stockRecordService.updateStock(7L, "PRODUCT", 1L, 0);

            assertThat(result).isNotNull();
            assertThat(testStockRecord.getQuantity()).isEqualTo(0);
        }
    }
}
