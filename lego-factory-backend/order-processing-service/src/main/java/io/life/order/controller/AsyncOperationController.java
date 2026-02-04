package io.life.order.controller;

import io.life.order.dto.AsyncOperationDTO;
import io.life.order.dto.ProductionOrderDTO;
import io.life.order.dto.request.ScheduleProductionRequest;
import io.life.order.service.AsyncProductionService;
import io.life.order.service.ProductionOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for asynchronous production operations.
 * Provides endpoints for:
 * - Initiating long-running operations (returns immediately with operation ID)
 * - Polling operation status
 * - Listing active/completed operations
 * 
 * When async is disabled, operations execute synchronously and return results directly.
 * 
 * @since Phase 3 - Async Processing (February 5, 2026)
 */
@RestController
@RequestMapping("/api/async")
@Tag(name = "Async Operations", description = "Asynchronous production operation management")
@RequiredArgsConstructor
@Slf4j
public class AsyncOperationController {

    private final AsyncProductionService asyncProductionService;
    private final ProductionOrderService productionOrderService;

    // ================================================
    // Async Operation Status Endpoints
    // ================================================

    @Operation(summary = "Check if async mode is enabled",
               description = "Returns whether async processing is enabled for the system")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAsyncStatus() {
        return ResponseEntity.ok(Map.of(
                "asyncEnabled", asyncProductionService.isAsyncEnabled(),
                "message", asyncProductionService.isAsyncEnabled() 
                        ? "Async processing is enabled. Long operations will return operation IDs."
                        : "Async processing is disabled. Operations execute synchronously."
        ));
    }

    @Operation(summary = "Get operation status by ID",
               description = "Poll the status of an async operation using its operation ID")
    @ApiResponse(responseCode = "200", description = "Operation status returned")
    @ApiResponse(responseCode = "404", description = "Operation not found")
    @GetMapping("/operations/{operationId}")
    public ResponseEntity<AsyncOperationDTO> getOperationStatus(
            @Parameter(description = "Async operation ID (UUID)")
            @PathVariable String operationId) {
        
        AsyncOperationDTO status = asyncProductionService.getOperationStatus(operationId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "List active operations",
               description = "Get all pending or processing async operations")
    @GetMapping("/operations/active")
    public ResponseEntity<List<AsyncOperationDTO>> getActiveOperations() {
        return ResponseEntity.ok(asyncProductionService.getActiveOperations());
    }

    @Operation(summary = "List operations for entity",
               description = "Get all async operations for a specific entity (e.g., ProductionOrder)")
    @GetMapping("/operations/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AsyncOperationDTO>> getOperationsForEntity(
            @Parameter(description = "Entity type (e.g., ProductionOrder)")
            @PathVariable String entityType,
            @Parameter(description = "Entity ID")
            @PathVariable Long entityId) {
        
        return ResponseEntity.ok(asyncProductionService.getOperationsForEntity(entityType, entityId));
    }

    // ================================================
    // Async Production Scheduling
    // ================================================

    @Operation(summary = "Schedule production (async or sync)",
               description = "Schedule a production order with SimAL. If async is enabled, returns operation ID immediately. " +
                            "If async is disabled, executes synchronously and returns result.")
    @ApiResponse(responseCode = "202", description = "Async operation accepted (async mode)")
    @ApiResponse(responseCode = "200", description = "Schedule completed (sync mode)")
    @PostMapping("/production-orders/{id}/schedule")
    public ResponseEntity<?> scheduleProduction(
            @Parameter(description = "Production order ID") @PathVariable Long id,
            @RequestBody ScheduleProductionRequest request,
            @RequestHeader(value = "X-Authenticated-User", required = false) String username) {

        if (asyncProductionService.isAsyncEnabled()) {
            // Async mode: return operation ID immediately
            AsyncOperationDTO operation = asyncProductionService.initiateScheduleProduction(
                    id,
                    request.getScheduledStartTime(),
                    request.getScheduledEndTime(),
                    request.getGanttChartId(),
                    username != null ? username : "system"
            );
            
            log.info("Initiated async schedule production for order {}, operation: {}", 
                    id, operation.getOperationId());
            
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                            "async", true,
                            "operationId", operation.getOperationId(),
                            "status", operation.getStatus(),
                            "message", "Schedule production initiated. Poll /api/async/operations/" + 
                                      operation.getOperationId() + " for status.",
                            "pollUrl", "/api/async/operations/" + operation.getOperationId()
                    ));
        } else {
            // Sync mode: execute immediately
            ProductionOrderDTO result = productionOrderService.scheduleProduction(
                    id,
                    request.getScheduledStartTime(),
                    request.getScheduledEndTime(),
                    request.getGanttChartId()
            );
            
            return ResponseEntity.ok(Map.of(
                    "async", false,
                    "result", result
            ));
        }
    }

    // ================================================
    // Async Control Order Dispatch
    // ================================================

    @Operation(summary = "Dispatch to control stations (async or sync)",
               description = "Dispatch a production order to control stations. Creates ProductionControlOrder " +
                            "and AssemblyControlOrder entities. If async is enabled, returns operation ID immediately.")
    @ApiResponse(responseCode = "202", description = "Async operation accepted (async mode)")
    @ApiResponse(responseCode = "200", description = "Dispatch completed (sync mode)")
    @PostMapping("/production-orders/{id}/dispatch")
    public ResponseEntity<?> dispatchControlOrders(
            @Parameter(description = "Production order ID") @PathVariable Long id,
            @RequestHeader(value = "X-Authenticated-User", required = false) String username) {

        if (asyncProductionService.isAsyncEnabled()) {
            // Async mode: return operation ID immediately
            AsyncOperationDTO operation = asyncProductionService.initiateDispatchControlOrders(
                    id,
                    username != null ? username : "system"
            );
            
            log.info("Initiated async dispatch control orders for order {}, operation: {}", 
                    id, operation.getOperationId());
            
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                            "async", true,
                            "operationId", operation.getOperationId(),
                            "status", operation.getStatus(),
                            "message", "Control order dispatch initiated. Poll /api/async/operations/" + 
                                      operation.getOperationId() + " for status.",
                            "pollUrl", "/api/async/operations/" + operation.getOperationId()
                    ));
        } else {
            // Sync mode: execute immediately
            ProductionOrderDTO result = productionOrderService.dispatchToControlStations(id);
            
            return ResponseEntity.ok(Map.of(
                    "async", false,
                    "result", result
            ));
        }
    }
}
