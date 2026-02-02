package io.life.order.service.validation;

import io.life.order.entity.OrderItem;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * StockValidator - Stock availability validation
 * 
 * Provides stock checking and validation for order fulfillment.
 * Extracted from FulfillmentService and CustomerOrderService for single-responsibility.
 * 
 * @see InventoryService - provides actual stock queries
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockValidator {

    private final InventoryService inventoryService;

    /**
     * Check if all items in the list are available at the specified workstation.
     * 
     * @param workstationId The workstation to check stock at
     * @param items List of order items to check
     * @return true if ALL items have sufficient stock
     */
    public boolean checkAllItemsAvailable(Long workstationId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            log.warn("checkAllItemsAvailable called with empty items list");
            return false;
        }

        boolean allAvailable = items.stream()
                .allMatch(item -> {
                    boolean hasStock = inventoryService.checkStock(
                            workstationId, 
                            item.getItemId(), 
                            item.getQuantity());
                    log.debug("Stock check: workstation={}, item={}, qty={}, available={}",
                            workstationId, item.getItemId(), item.getQuantity(), hasStock);
                    return hasStock;
                });

        log.info("Stock availability check for workstation {}: {} items, all available: {}",
                workstationId, items.size(), allAvailable);

        return allAvailable;
    }

    /**
     * Check if ANY items in the list are available at the specified workstation.
     * Used to determine if partial fulfillment is possible.
     * 
     * @param workstationId The workstation to check stock at
     * @param items List of order items to check
     * @return true if at least one item has sufficient stock
     */
    public boolean checkAnyItemsAvailable(Long workstationId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        boolean anyAvailable = items.stream()
                .anyMatch(item -> inventoryService.checkStock(
                        workstationId,
                        item.getItemId(),
                        item.getQuantity()));

        log.info("Partial availability check for workstation {}: any available: {}",
                workstationId, anyAvailable);

        return anyAvailable;
    }

    /**
     * Check stock availability for warehouse order items (modules).
     * 
     * @param workstationId The workstation to check stock at (typically WS-8 Modules Supermarket)
     * @param items List of warehouse order items to check
     * @return true if ALL items have sufficient stock
     */
    public boolean checkWarehouseItemsAvailable(Long workstationId, List<WarehouseOrderItem> items) {
        if (items == null || items.isEmpty()) {
            log.warn("checkWarehouseItemsAvailable called with empty items list");
            return false;
        }

        boolean allAvailable = items.stream()
                .allMatch(item -> {
                    boolean hasStock = inventoryService.checkStock(
                            workstationId,
                            item.getItemId(),
                            item.getRequestedQuantity());
                    log.debug("Module stock check: workstation={}, module={}, qty={}, available={}",
                            workstationId, item.getItemId(), item.getRequestedQuantity(), hasStock);
                    return hasStock;
                });

        log.info("Module availability check for workstation {}: {} items, all available: {}",
                workstationId, items.size(), allAvailable);

        return allAvailable;
    }

    /**
     * Get a detailed stock status for each item.
     * Returns a list of items with availability status.
     * 
     * @param workstationId The workstation to check stock at
     * @param items List of order items to check
     * @return List of StockStatus objects with availability info
     */
    public List<StockStatus> getDetailedStockStatus(Long workstationId, List<OrderItem> items) {
        List<StockStatus> statusList = new ArrayList<>();

        for (OrderItem item : items) {
            boolean available = inventoryService.checkStock(
                    workstationId,
                    item.getItemId(),
                    item.getQuantity());

            statusList.add(new StockStatus(
                    item.getItemId(),
                    item.getItemType(),
                    item.getQuantity(),
                    available
            ));
        }

        return statusList;
    }

    /**
     * Get counts of available vs unavailable items.
     * 
     * @param workstationId The workstation to check stock at
     * @param items List of order items to check
     * @return StockSummary with counts
     */
    public StockSummary getStockSummary(Long workstationId, List<OrderItem> items) {
        int available = 0;
        int unavailable = 0;

        for (OrderItem item : items) {
            if (inventoryService.checkStock(workstationId, item.getItemId(), item.getQuantity())) {
                available++;
            } else {
                unavailable++;
            }
        }

        return new StockSummary(items.size(), available, unavailable);
    }

    // ========================
    // INNER CLASSES
    // ========================

    /**
     * Stock status for a single item.
     */
    public record StockStatus(
            Long itemId,
            String itemType,
            Integer requestedQuantity,
            boolean available
    ) {}

    /**
     * Summary of stock availability across multiple items.
     */
    public record StockSummary(
            int totalItems,
            int availableItems,
            int unavailableItems
    ) {
        public boolean allAvailable() {
            return unavailableItems == 0;
        }

        public boolean noneAvailable() {
            return availableItems == 0;
        }

        public boolean partiallyAvailable() {
            return availableItems > 0 && unavailableItems > 0;
        }
    }
}
