package io.life.inventory.controller;

import io.life.inventory.dto.LowStockAlertDto;
import io.life.inventory.dto.LowStockThresholdDto;
import io.life.inventory.service.LowStockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock/alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final LowStockAlertService alertService;

    @GetMapping("/low")
    public ResponseEntity<List<LowStockAlertDto>> low(@RequestParam(required = false) Long workstationId) {
        return ResponseEntity.ok(alertService.getLowStockAlerts(workstationId));
    }

    @GetMapping("/thresholds")
    public ResponseEntity<List<LowStockThresholdDto>> listThresholds() {
        return ResponseEntity.ok(alertService.listThresholds());
    }

    @PutMapping("/thresholds")
    public ResponseEntity<List<LowStockThresholdDto>> upsert(@RequestBody List<LowStockThresholdDto> thresholds) {
        return ResponseEntity.ok(alertService.upsertThresholds(thresholds));
    }
}
