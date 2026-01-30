package io.life.inventory.controller;

import io.life.inventory.dto.LowStockAlertDto;
import io.life.inventory.dto.LowStockThresholdDto;
import io.life.inventory.service.LowStockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock/alerts")
@RequiredArgsConstructor
@Tag(name = "Stock Alerts", description = "Low stock alerts and threshold management")
public class StockAlertController {

    private final LowStockAlertService alertService;

    @Operation(summary = "Get low stock alerts", 
               description = "Retrieve items that are below their configured low stock threshold")
    @ApiResponse(responseCode = "200", description = "List of low stock alerts")
    @GetMapping("/low")
    public ResponseEntity<List<LowStockAlertDto>> low(
            @Parameter(description = "Filter by workstation ID") @RequestParam(required = false) Long workstationId) {
        return ResponseEntity.ok(alertService.getLowStockAlerts(workstationId));
    }

    @Operation(summary = "List stock thresholds", 
               description = "Retrieve all configured low stock thresholds")
    @ApiResponse(responseCode = "200", description = "List of threshold configurations")
    @GetMapping("/thresholds")
    public ResponseEntity<List<LowStockThresholdDto>> listThresholds() {
        return ResponseEntity.ok(alertService.listThresholds());
    }

    @Operation(summary = "Update stock thresholds", 
               description = "Create or update low stock thresholds for items")
    @ApiResponse(responseCode = "200", description = "Updated threshold configurations")
    @PutMapping("/thresholds")
    public ResponseEntity<List<LowStockThresholdDto>> upsert(@RequestBody List<LowStockThresholdDto> thresholds) {
        return ResponseEntity.ok(alertService.upsertThresholds(thresholds));
    }
}
