package io.life.order.service;

import io.life.order.dto.CustomerOrderDTO;
import io.life.order.service.orchestration.FulfillmentOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fulfillment Service - Facade for order fulfillment operations.
 * 
 * REFACTORED (Phase 3): Delegates to FulfillmentOrchestrationService.
 * This class is maintained for backward compatibility with existing controllers.
 * 
 * Original size: 574 lines
 * Refactored size: ~45 lines (facade pattern)
 * 
 * Scenario 1: Direct Fulfillment (All items available locally)
 * Scenario 2: Warehouse Order (Items not available locally)
 * Scenario 3: Partial Fulfillment (Some items available)
 * Scenario 4: Production Planning (High-volume/custom orders)
 * 
 * @see FulfillmentOrchestrationService - Main orchestration logic
 * @see io.life.order.service.domain.BomConversionService - BOM conversion
 * @see io.life.order.service.validation.OrderValidator - Order validation
 * @see io.life.order.service.validation.StockValidator - Stock validation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FulfillmentService {

    private final FulfillmentOrchestrationService orchestrationService;

    /**
     * Process fulfillment for a customer order.
     * Delegates to FulfillmentOrchestrationService for scenario determination and execution.
     * 
     * @param orderId The customer order ID to fulfill
     * @return CustomerOrderDTO with updated status
     */
    @Transactional
    public CustomerOrderDTO fulfillOrder(Long orderId) {
        log.info("FulfillmentService.fulfillOrder({}) - delegating to orchestration service", orderId);
        return orchestrationService.fulfillOrder(orderId);
    }
}
