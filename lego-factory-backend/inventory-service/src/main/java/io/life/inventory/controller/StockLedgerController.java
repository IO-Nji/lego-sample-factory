package io.life.inventory.controller;

import io.life.inventory.dto.StockAdjustmentRequest;
import io.life.inventory.dto.StockLedgerEntryDto;
import io.life.inventory.service.StockLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Stock Ledger", description = "Stock adjustment and transaction history")
public class StockLedgerController {

    private final StockLedgerService ledgerService;

    @Operation(summary = "Adjust stock", 
               description = "Record a stock adjustment (credit or debit) with reason. Creates ledger entry for audit trail.")
    @ApiResponse(responseCode = "200", description = "Stock adjusted and ledger entry created")
    @PostMapping("/adjust")
    public ResponseEntity<StockLedgerEntryDto> adjust(@RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(ledgerService.adjustStock(request));
    }

    @Operation(summary = "Get stock history", 
               description = "Retrieve stock adjustment history, optionally filtered by workstation, item type, and item ID")
    @ApiResponse(responseCode = "200", description = "List of ledger entries")
    @GetMapping("/ledger")
    public ResponseEntity<List<StockLedgerEntryDto>> history(
            @Parameter(description = "Filter by workstation ID") @RequestParam(required = false) Long workstationId,
            @Parameter(description = "Filter by item type (PRODUCT, MODULE, PART)") @RequestParam(required = false) String itemType,
            @Parameter(description = "Filter by item ID") @RequestParam(required = false) Long itemId
    ) {
        return ResponseEntity.ok(ledgerService.history(workstationId, itemType, itemId));
    }

    @Operation(summary = "Get recent stock changes", 
               description = "Retrieve the most recent stock adjustments across all workstations")
    @ApiResponse(responseCode = "200", description = "List of recent ledger entries")
    @GetMapping("/ledger/recent")
    public ResponseEntity<List<StockLedgerEntryDto>> recent(
            @Parameter(description = "Maximum number of entries to return") @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ledgerService.recent(limit));
    }
}
