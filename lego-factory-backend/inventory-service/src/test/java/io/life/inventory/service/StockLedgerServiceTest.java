package io.life.inventory.service;

import io.life.inventory.dto.StockAdjustmentRequest;
import io.life.inventory.dto.StockLedgerEntryDto;
import io.life.inventory.entity.StockLedgerEntry;
import io.life.inventory.entity.StockRecord;
import io.life.inventory.repository.StockLedgerRepository;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockLedgerService.
 * 
 * Key behaviors tested:
 * - Stock adjustments (credit/debit)
 * - Ledger entry creation
 * - History queries
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockLedgerService Tests")
class StockLedgerServiceTest {

    @Mock
    private StockRecordRepository stockRecordRepository;

    @Mock
    private StockLedgerRepository stockLedgerRepository;

    @InjectMocks
    private StockLedgerService stockLedgerService;

    private StockRecord testStockRecord;
    private StockLedgerEntry testLedgerEntry;
    private StockAdjustmentRequest testAdjustmentRequest;

    @BeforeEach
    void setUp() {
        testStockRecord = new StockRecord();
        testStockRecord.setId(1L);
        testStockRecord.setWorkstationId(7L);
        testStockRecord.setItemType("PRODUCT");
        testStockRecord.setItemId(1L);
        testStockRecord.setQuantity(100);
        testStockRecord.setLastUpdated(LocalDateTime.now());

        testLedgerEntry = new StockLedgerEntry();
        testLedgerEntry.setId(1L);
        testLedgerEntry.setWorkstationId(7L);
        testLedgerEntry.setItemType("PRODUCT");
        testLedgerEntry.setItemId(1L);
        testLedgerEntry.setDelta(10);
        testLedgerEntry.setBalanceAfter(110);
        testLedgerEntry.setReasonCode("ADJUSTMENT");
        testLedgerEntry.setNotes("Test adjustment");
        testLedgerEntry.setCreatedAt(LocalDateTime.now());

        testAdjustmentRequest = new StockAdjustmentRequest();
        testAdjustmentRequest.setWorkstationId(7L);
        testAdjustmentRequest.setItemType("PRODUCT");
        testAdjustmentRequest.setItemId(1L);
        testAdjustmentRequest.setDelta(10);
        testAdjustmentRequest.setReasonCode("ADJUSTMENT");
        testAdjustmentRequest.setNotes("Test adjustment");

        // Common mock setups
        when(stockRecordRepository.save(any(StockRecord.class))).thenAnswer(invocation -> {
            StockRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(1L);
            }
            return record;
        });

        when(stockLedgerRepository.save(any(StockLedgerEntry.class))).thenAnswer(invocation -> {
            StockLedgerEntry entry = invocation.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(1L);
            }
            return entry;
        });
    }

    @Nested
    @DisplayName("Adjust Stock - Credit Tests")
    class AdjustStockCreditTests {

        @Test
        @DisplayName("SLS-001: adjustStock credits existing stock record")
        void adjustStock_creditsExistingStockRecord() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockLedgerEntryDto result = stockLedgerService.adjustStock(testAdjustmentRequest);

            assertThat(result).isNotNull();
            assertThat(testStockRecord.getQuantity()).isEqualTo(110); // 100 + 10
            verify(stockRecordRepository).save(testStockRecord);
        }

        @Test
        @DisplayName("SLS-002: adjustStock creates ledger entry for credit")
        void adjustStock_createsLedgerEntryForCredit() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockLedgerEntryDto result = stockLedgerService.adjustStock(testAdjustmentRequest);

            ArgumentCaptor<StockLedgerEntry> captor = ArgumentCaptor.forClass(StockLedgerEntry.class);
            verify(stockLedgerRepository).save(captor.capture());

            StockLedgerEntry saved = captor.getValue();
            assertThat(saved.getDelta()).isEqualTo(10);
            assertThat(saved.getBalanceAfter()).isEqualTo(110);
            assertThat(saved.getReasonCode()).isEqualTo("ADJUSTMENT");
        }

        @Test
        @DisplayName("SLS-003: adjustStock creates new record if not exists")
        void adjustStock_createsNewRecordIfNotExists() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(8L, "MODULE", 7L))
                    .thenReturn(Optional.empty());

            StockAdjustmentRequest newRequest = new StockAdjustmentRequest();
            newRequest.setWorkstationId(8L);
            newRequest.setItemType("MODULE");
            newRequest.setItemId(7L);
            newRequest.setDelta(50);
            newRequest.setReasonCode("RECEIPT");
            newRequest.setNotes("Initial stock");

            StockLedgerEntryDto result = stockLedgerService.adjustStock(newRequest);

            ArgumentCaptor<StockRecord> captor = ArgumentCaptor.forClass(StockRecord.class);
            verify(stockRecordRepository).save(captor.capture());

            StockRecord saved = captor.getValue();
            assertThat(saved.getWorkstationId()).isEqualTo(8L);
            assertThat(saved.getItemType()).isEqualTo("MODULE");
            assertThat(saved.getItemId()).isEqualTo(7L);
            assertThat(saved.getQuantity()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Adjust Stock - Debit Tests")
    class AdjustStockDebitTests {

        @Test
        @DisplayName("SLS-004: adjustStock debits existing stock record")
        void adjustStock_debitsExistingStockRecord() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockAdjustmentRequest debitRequest = new StockAdjustmentRequest();
            debitRequest.setWorkstationId(7L);
            debitRequest.setItemType("PRODUCT");
            debitRequest.setItemId(1L);
            debitRequest.setDelta(-20);
            debitRequest.setReasonCode("CONSUMPTION");
            debitRequest.setNotes("Order fulfillment");

            StockLedgerEntryDto result = stockLedgerService.adjustStock(debitRequest);

            assertThat(result).isNotNull();
            assertThat(testStockRecord.getQuantity()).isEqualTo(80); // 100 - 20
        }

        @Test
        @DisplayName("SLS-005: adjustStock creates ledger entry for debit")
        void adjustStock_createsLedgerEntryForDebit() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockAdjustmentRequest debitRequest = new StockAdjustmentRequest();
            debitRequest.setWorkstationId(7L);
            debitRequest.setItemType("PRODUCT");
            debitRequest.setItemId(1L);
            debitRequest.setDelta(-30);
            debitRequest.setReasonCode("CONSUMPTION");

            stockLedgerService.adjustStock(debitRequest);

            ArgumentCaptor<StockLedgerEntry> captor = ArgumentCaptor.forClass(StockLedgerEntry.class);
            verify(stockLedgerRepository).save(captor.capture());

            StockLedgerEntry saved = captor.getValue();
            assertThat(saved.getDelta()).isEqualTo(-30);
            assertThat(saved.getBalanceAfter()).isEqualTo(70); // 100 - 30
        }

        @Test
        @DisplayName("SLS-006: adjustStock allows negative balance (no validation)")
        void adjustStock_allowsNegativeBalance() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockAdjustmentRequest debitRequest = new StockAdjustmentRequest();
            debitRequest.setWorkstationId(7L);
            debitRequest.setItemType("PRODUCT");
            debitRequest.setItemId(1L);
            debitRequest.setDelta(-150); // More than current quantity
            debitRequest.setReasonCode("ADJUSTMENT");

            StockLedgerEntryDto result = stockLedgerService.adjustStock(debitRequest);

            assertThat(result).isNotNull();
            assertThat(testStockRecord.getQuantity()).isEqualTo(-50); // 100 - 150
        }
    }

    @Nested
    @DisplayName("Reason Code Tests")
    class ReasonCodeTests {

        @Test
        @DisplayName("SLS-007: adjustStock uses default reason code when null")
        void adjustStock_usesDefaultReasonCodeWhenNull() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockAdjustmentRequest requestNoReason = new StockAdjustmentRequest();
            requestNoReason.setWorkstationId(7L);
            requestNoReason.setItemType("PRODUCT");
            requestNoReason.setItemId(1L);
            requestNoReason.setDelta(5);
            requestNoReason.setReasonCode(null);

            stockLedgerService.adjustStock(requestNoReason);

            ArgumentCaptor<StockLedgerEntry> captor = ArgumentCaptor.forClass(StockLedgerEntry.class);
            verify(stockLedgerRepository).save(captor.capture());

            assertThat(captor.getValue().getReasonCode()).isEqualTo("ADJUSTMENT");
        }

        @Test
        @DisplayName("SLS-008: adjustStock preserves custom reason code")
        void adjustStock_preservesCustomReasonCode() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            testAdjustmentRequest.setReasonCode("PRODUCTION_COMPLETE");

            stockLedgerService.adjustStock(testAdjustmentRequest);

            ArgumentCaptor<StockLedgerEntry> captor = ArgumentCaptor.forClass(StockLedgerEntry.class);
            verify(stockLedgerRepository).save(captor.capture());

            assertThat(captor.getValue().getReasonCode()).isEqualTo("PRODUCTION_COMPLETE");
        }
    }

    @Nested
    @DisplayName("Recent Entries Tests")
    class RecentEntriesTests {

        @Test
        @DisplayName("SLS-009: recent returns last N entries")
        void recent_returnsLastNEntries() {
            StockLedgerEntry entry2 = new StockLedgerEntry();
            entry2.setId(2L);
            entry2.setWorkstationId(8L);
            entry2.setItemType("MODULE");
            entry2.setItemId(7L);
            entry2.setDelta(25);
            entry2.setBalanceAfter(25);
            entry2.setReasonCode("RECEIPT");
            entry2.setCreatedAt(LocalDateTime.now().minusMinutes(5));

            when(stockLedgerRepository.findTop100ByOrderByCreatedAtDesc())
                    .thenReturn(List.of(testLedgerEntry, entry2));

            List<StockLedgerEntryDto> result = stockLedgerService.recent(2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("SLS-010: recent limits results when more available")
        void recent_limitsResultsWhenMoreAvailable() {
            StockLedgerEntry entry2 = new StockLedgerEntry();
            entry2.setId(2L);
            entry2.setWorkstationId(8L);
            entry2.setItemType("MODULE");
            entry2.setItemId(7L);
            entry2.setDelta(25);
            entry2.setBalanceAfter(25);
            entry2.setReasonCode("RECEIPT");
            entry2.setCreatedAt(LocalDateTime.now().minusMinutes(5));

            StockLedgerEntry entry3 = new StockLedgerEntry();
            entry3.setId(3L);
            entry3.setWorkstationId(9L);
            entry3.setItemType("PART");
            entry3.setItemId(101L);
            entry3.setDelta(-10);
            entry3.setBalanceAfter(90);
            entry3.setReasonCode("CONSUMPTION");
            entry3.setCreatedAt(LocalDateTime.now().minusMinutes(10));

            when(stockLedgerRepository.findTop100ByOrderByCreatedAtDesc())
                    .thenReturn(List.of(testLedgerEntry, entry2, entry3));

            List<StockLedgerEntryDto> result = stockLedgerService.recent(2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("SLS-011: recent returns empty list when no entries")
        void recent_returnsEmptyListWhenNoEntries() {
            when(stockLedgerRepository.findTop100ByOrderByCreatedAtDesc())
                    .thenReturn(Collections.emptyList());

            List<StockLedgerEntryDto> result = stockLedgerService.recent(10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("History Query Tests")
    class HistoryQueryTests {

        @Test
        @DisplayName("SLS-012: history filters by workstation, itemType, and itemId")
        void history_filtersByWorkstationItemTypeAndItemId() {
            when(stockLedgerRepository.findByWorkstationIdAndItemTypeAndItemIdOrderByCreatedAtDesc(7L, "PRODUCT", 1L))
                    .thenReturn(List.of(testLedgerEntry));

            List<StockLedgerEntryDto> result = stockLedgerService.history(7L, "PRODUCT", 1L);

            assertThat(result).hasSize(1);
            verify(stockLedgerRepository).findByWorkstationIdAndItemTypeAndItemIdOrderByCreatedAtDesc(7L, "PRODUCT", 1L);
        }

        @Test
        @DisplayName("SLS-013: history filters by workstation only when others null")
        void history_filtersByWorkstationOnlyWhenOthersNull() {
            when(stockLedgerRepository.findByWorkstationIdOrderByCreatedAtDesc(7L))
                    .thenReturn(List.of(testLedgerEntry));

            List<StockLedgerEntryDto> result = stockLedgerService.history(7L, null, null);

            assertThat(result).hasSize(1);
            verify(stockLedgerRepository).findByWorkstationIdOrderByCreatedAtDesc(7L);
        }

        @Test
        @DisplayName("SLS-014: history filters by itemType and itemId when workstation null")
        void history_filtersByItemTypeAndItemIdWhenWorkstationNull() {
            when(stockLedgerRepository.findByItemTypeAndItemIdOrderByCreatedAtDesc("PRODUCT", 1L))
                    .thenReturn(List.of(testLedgerEntry));

            List<StockLedgerEntryDto> result = stockLedgerService.history(null, "PRODUCT", 1L);

            assertThat(result).hasSize(1);
            verify(stockLedgerRepository).findByItemTypeAndItemIdOrderByCreatedAtDesc("PRODUCT", 1L);
        }

        @Test
        @DisplayName("SLS-015: history returns recent entries when all params null")
        void history_returnsRecentEntriesWhenAllParamsNull() {
            when(stockLedgerRepository.findTop100ByOrderByCreatedAtDesc())
                    .thenReturn(List.of(testLedgerEntry));

            List<StockLedgerEntryDto> result = stockLedgerService.history(null, null, null);

            assertThat(result).hasSize(1);
            verify(stockLedgerRepository).findTop100ByOrderByCreatedAtDesc();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("SLS-016: adjustStock handles null quantity in existing record")
        void adjustStock_handlesNullQuantityInExistingRecord() {
            testStockRecord.setQuantity(null);
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            StockLedgerEntryDto result = stockLedgerService.adjustStock(testAdjustmentRequest);

            assertThat(testStockRecord.getQuantity()).isEqualTo(10); // 0 + 10
        }

        @Test
        @DisplayName("SLS-017: adjustStock preserves notes in ledger entry")
        void adjustStock_preservesNotesInLedgerEntry() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            testAdjustmentRequest.setNotes("Order #12345 fulfillment");

            stockLedgerService.adjustStock(testAdjustmentRequest);

            ArgumentCaptor<StockLedgerEntry> captor = ArgumentCaptor.forClass(StockLedgerEntry.class);
            verify(stockLedgerRepository).save(captor.capture());

            assertThat(captor.getValue().getNotes()).isEqualTo("Order #12345 fulfillment");
        }

        @Test
        @DisplayName("SLS-018: adjustStock with zero delta is allowed")
        void adjustStock_withZeroDeltaIsAllowed() {
            when(stockRecordRepository.findByWorkstationIdAndItemTypeAndItemId(7L, "PRODUCT", 1L))
                    .thenReturn(Optional.of(testStockRecord));

            testAdjustmentRequest.setDelta(0);

            StockLedgerEntryDto result = stockLedgerService.adjustStock(testAdjustmentRequest);

            assertThat(result).isNotNull();
            assertThat(testStockRecord.getQuantity()).isEqualTo(100); // Unchanged
        }
    }
}
