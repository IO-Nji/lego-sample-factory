package io.life.inventory.controller;

import io.life.inventory.dto.StockRecordDto;
import io.life.inventory.service.StockRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Stock Records", description = "Inventory stock management - track stock levels across all workstations")
public class StockRecordController {

	private final StockRecordService service;

	@Operation(summary = "Get all stock records", 
	           description = "Retrieve all stock records across all workstations")
	@ApiResponse(responseCode = "200", description = "List of all stock records")
	@GetMapping
	public ResponseEntity<List<StockRecordDto>> getAllStockRecords() {
		return ResponseEntity.ok(service.findAll());
	}

	@Operation(summary = "Get stock record by ID", 
	           description = "Retrieve a specific stock record by its ID")
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Stock record found"),
	    @ApiResponse(responseCode = "404", description = "Stock record not found")
	})
	@GetMapping("/{id}")
	public ResponseEntity<StockRecordDto> getStockRecordById(
	        @Parameter(description = "Stock record ID") @PathVariable Long id) {
		StockRecordDto dto = service.findById(id);
		if (dto != null) {
			return ResponseEntity.ok(dto);
		}
		return ResponseEntity.notFound().build();
	}

	@Operation(summary = "Get stock by workstation", 
	           description = "Retrieve all stock records for a specific workstation (WS-1 to WS-9)")
	@ApiResponse(responseCode = "200", description = "List of stock records for workstation")
	@GetMapping("/workstation/{workstationId}")
	public ResponseEntity<List<StockRecordDto>> getStockByWorkstation(
	        @Parameter(description = "Workstation ID (1-9)") @PathVariable Long workstationId) {
		return ResponseEntity.ok(service.getStockByWorkstationId(workstationId));
	}

	@Operation(summary = "Get stock by workstation and item", 
	           description = "Retrieve stock for a specific item at a specific workstation")
	@ApiResponses({
	    @ApiResponse(responseCode = "200", description = "Stock record found"),
	    @ApiResponse(responseCode = "404", description = "Stock record not found")
	})
	@GetMapping("/workstation/{workstationId}/item")
	public ResponseEntity<StockRecordDto> getStockByWorkstationAndItem(
			@Parameter(description = "Workstation ID (1-9)") @PathVariable Long workstationId,
			@Parameter(description = "Item type (PRODUCT, MODULE, PART)") @RequestParam String itemType,
			@Parameter(description = "Item ID") @RequestParam Long itemId) {
		StockRecordDto dto = service.getStockByWorkstationAndItem(workstationId, itemType, itemId);
		if (dto != null) {
			return ResponseEntity.ok(dto);
		}
		return ResponseEntity.notFound().build();
	}

	@Operation(summary = "Update stock quantity", 
	           description = "Update stock quantity for a specific item at a workstation. Creates record if not exists.")
	@ApiResponse(responseCode = "200", description = "Stock updated successfully")
	@PostMapping("/update")
	public ResponseEntity<StockRecordDto> updateStock(
			@Parameter(description = "Workstation ID (1-9)") @RequestParam Long workstationId,
			@Parameter(description = "Item type (PRODUCT, MODULE, PART)") @RequestParam String itemType,
			@Parameter(description = "Item ID") @RequestParam Long itemId,
			@Parameter(description = "New quantity") @RequestParam Integer quantity) {
		StockRecordDto updated = service.updateStock(workstationId, itemType, itemId, quantity);
		return ResponseEntity.ok(updated);
	}

	@Operation(summary = "Create stock record", 
	           description = "Create a new stock record")
	@ApiResponse(responseCode = "200", description = "Stock record created")
	@PostMapping
	public ResponseEntity<StockRecordDto> createStockRecord(@RequestBody StockRecordDto dto) {
		StockRecordDto created = service.save(dto);
		return ResponseEntity.ok(created);
	}

	@Operation(summary = "Delete stock record", 
	           description = "Delete a stock record by ID")
	@ApiResponse(responseCode = "204", description = "Stock record deleted")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteStockRecord(
	        @Parameter(description = "Stock record ID") @PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}

}
