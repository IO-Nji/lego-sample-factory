package io.life.inventory.service;

import io.life.inventory.dto.LowStockAlertDto;
import io.life.inventory.dto.LowStockThresholdDto;
import io.life.inventory.entity.LowStockThreshold;
import io.life.inventory.entity.StockRecord;
import io.life.inventory.repository.LowStockThresholdRepository;
import io.life.inventory.repository.StockRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LowStockAlertService {

    private final LowStockThresholdRepository thresholdRepository;
    private final StockRecordRepository stockRecordRepository;

    public List<LowStockAlertDto> getLowStockAlerts(Long workstationId) {
        List<LowStockAlertDto> alerts = new ArrayList<>();

        // Collect thresholds applicable: specific workstation + global
        List<LowStockThreshold> thresholds = new ArrayList<>();
        if (workstationId != null) {
            thresholds.addAll(thresholdRepository.findByWorkstationId(workstationId));
        }
        thresholds.addAll(thresholdRepository.findByWorkstationIdIsNull());

        for (LowStockThreshold t : thresholds) {
            int qty;
            if (t.getWorkstationId() != null) {
                // Evaluate threshold against a specific workstation
                qty = stockRecordRepository
                        .findByWorkstationIdAndItemTypeAndItemId(t.getWorkstationId(), t.getItemType(), t.getItemId())
                        .map(StockRecord::getQuantity)
                        .orElse(0);
            } else {
                // Global threshold: aggregate across all workstations for that item
                qty = stockRecordRepository.findByItemTypeAndItemId(t.getItemType(), t.getItemId())
                        .stream().mapToInt(r -> r.getQuantity() == null ? 0 : r.getQuantity()).sum();
            }
            if (qty < t.getThreshold()) {
                alerts.add(new LowStockAlertDto(
                        t.getWorkstationId(),
                        t.getItemType(),
                        t.getItemId(),
                        qty,
                        t.getThreshold(),
                        t.getThreshold() - qty
                ));
            }
        }
        return alerts;
    }

    public List<LowStockThresholdDto> listThresholds() {
        return thresholdRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<LowStockThresholdDto> upsertThresholds(List<LowStockThresholdDto> dtos) {
        List<LowStockThresholdDto> result = new ArrayList<>();
        for (LowStockThresholdDto dto : dtos) {
            Optional<LowStockThreshold> existing = (dto.getWorkstationId() == null)
                    ? thresholdRepository.findByItemTypeAndItemIdAndWorkstationIdIsNull(dto.getItemType(), dto.getItemId())
                    : thresholdRepository.findByWorkstationIdAndItemTypeAndItemId(dto.getWorkstationId(), dto.getItemType(), dto.getItemId());

            LowStockThreshold t = existing.orElseGet(LowStockThreshold::new);
            t.setWorkstationId(dto.getWorkstationId());
            t.setItemType(dto.getItemType());
            t.setItemId(dto.getItemId());
            t.setThreshold(dto.getThreshold());
            LowStockThreshold saved = thresholdRepository.save(t);
            result.add(toDto(saved));
        }
        return result;
    }

    private LowStockThresholdDto toDto(LowStockThreshold t) {
        return new LowStockThresholdDto(t.getId(), t.getWorkstationId(), t.getItemType(), t.getItemId(), t.getThreshold());
    }
}
