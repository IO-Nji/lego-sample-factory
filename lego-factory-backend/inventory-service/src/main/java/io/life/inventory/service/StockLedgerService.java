package io.life.inventory.service;

import io.life.inventory.dto.StockAdjustmentRequest;
import io.life.inventory.dto.StockLedgerEntryDto;
import io.life.inventory.entity.StockLedgerEntry;
import io.life.inventory.entity.StockRecord;
import io.life.inventory.repository.StockLedgerRepository;
import io.life.inventory.repository.StockRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockLedgerService {

    private final StockRecordRepository stockRecordRepository;
    private final StockLedgerRepository stockLedgerRepository;

    @Transactional
    public StockLedgerEntryDto adjustStock(StockAdjustmentRequest req) {
        Optional<StockRecord> existing = stockRecordRepository
                .findByWorkstationIdAndItemTypeAndItemId(req.getWorkstationId(), req.getItemType(), req.getItemId());

        StockRecord record = existing.orElseGet(() -> {
            StockRecord r = new StockRecord();
            r.setWorkstationId(req.getWorkstationId());
            r.setItemType(req.getItemType());
            r.setItemId(req.getItemId());
            r.setQuantity(0);
            return r;
        });

        int newQty = (record.getQuantity() == null ? 0 : record.getQuantity()) + req.getDelta();
        record.setQuantity(newQty);
        StockRecord saved = stockRecordRepository.save(record);

        StockLedgerEntry entry = new StockLedgerEntry();
        entry.setWorkstationId(req.getWorkstationId());
        entry.setItemType(req.getItemType());
        entry.setItemId(req.getItemId());
        entry.setDelta(req.getDelta());
        entry.setBalanceAfter(saved.getQuantity());
        entry.setReasonCode(req.getReasonCode() != null ? req.getReasonCode() : "ADJUSTMENT");
        entry.setNotes(req.getNotes());
        StockLedgerEntry savedEntry = stockLedgerRepository.save(entry);

        return toDto(savedEntry);
    }

    public List<StockLedgerEntryDto> recent(int limit) {
        List<StockLedgerEntry> list = stockLedgerRepository.findTop100ByOrderByCreatedAtDesc();
        if (limit > 0 && list.size() > limit) {
            list = list.subList(0, limit);
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<StockLedgerEntryDto> history(Long workstationId, String itemType, Long itemId) {
        List<StockLedgerEntry> list;
        if (workstationId != null && itemType != null && itemId != null) {
            list = stockLedgerRepository.findByWorkstationIdAndItemTypeAndItemIdOrderByCreatedAtDesc(workstationId, itemType, itemId);
        } else if (workstationId != null) {
            list = stockLedgerRepository.findByWorkstationIdOrderByCreatedAtDesc(workstationId);
        } else if (itemType != null && itemId != null) {
            list = stockLedgerRepository.findByItemTypeAndItemIdOrderByCreatedAtDesc(itemType, itemId);
        } else {
            list = stockLedgerRepository.findTop100ByOrderByCreatedAtDesc();
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private StockLedgerEntryDto toDto(StockLedgerEntry e) {
        return new StockLedgerEntryDto(
                e.getId(),
                e.getWorkstationId(),
                e.getItemType(),
                e.getItemId(),
                e.getDelta(),
                e.getBalanceAfter(),
                e.getReasonCode(),
                e.getNotes(),
                e.getCreatedAt()
        );
    }
}
