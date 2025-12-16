package io.life.inventory.controller;

import io.life.inventory.dto.StockAdjustmentRequest;
import io.life.inventory.dto.StockLedgerEntryDto;
import io.life.inventory.service.StockLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockLedgerController {

    private final StockLedgerService ledgerService;

    @PostMapping("/adjust")
    public ResponseEntity<StockLedgerEntryDto> adjust(@RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(ledgerService.adjustStock(request));
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<StockLedgerEntryDto>> history(
            @RequestParam(required = false) Long workstationId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Long itemId
    ) {
        return ResponseEntity.ok(ledgerService.history(workstationId, itemType, itemId));
    }

    @GetMapping("/ledger/recent")
    public ResponseEntity<List<StockLedgerEntryDto>> recent(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ledgerService.recent(limit));
    }
}
