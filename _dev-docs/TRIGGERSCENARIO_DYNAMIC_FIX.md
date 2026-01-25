# TriggerScenario Dynamic Stock Fix

> **Date:** January 25, 2026  
> **Commit:** 52980ef  
> **Issue:** Order buttons (Fulfill vs Process) showed stale values when stock changed between confirmation and fulfillment

## Problem Statement

When multiple customer orders were confirmed while stock was available, they all received `triggerScenario=DIRECT_FULFILLMENT`. However, when one order was fulfilled and depleted stock, the other confirmed orders still showed the "Fulfill" button instead of "Process" (create warehouse order).

### Reproduction Steps

1. Create two identical customer orders (e.g., both for Product X, qty 5)
2. Confirm both orders when stock is available (5 units in Plant Warehouse)
3. Both orders get `triggerScenario=DIRECT_FULFILLMENT` → show "Fulfill" button
4. Fulfill Order #1 → stock debited to 0
5. **BUG**: Order #2 still shows "Fulfill" button (should show "Process" button)
6. Click "Fulfill" on Order #2 → backend realizes stock is gone, creates warehouse order
7. **Issue**: UI showed wrong button because `triggerScenario` was stale

## Root Cause

`triggerScenario` was set **once** during order confirmation based on stock at that moment. It was never updated when:
- Other orders consumed the same stock
- Stock levels changed due to fulfillments
- Time passed between confirmation and fulfillment

This created a **snapshot vs dynamic data** problem:
- `triggerScenario` = snapshot taken at confirmation time
- Stock levels = dynamic data that changes frequently
- UI displayed buttons based on outdated snapshot

## Solution Implemented

Added logic in `FulfillmentService.fulfillOrder()` to update `triggerScenario` for all other CONFIRMED orders at the same workstation after a successful fulfillment.

### Changes Made

**File:** `lego-factory-backend/order-processing-service/src/main/java/io/life/order/service/FulfillmentService.java`

1. **Enhanced logging in `fulfillOrder()` method:**
   ```java
   // Re-check stock at fulfill time (may have changed since confirmation)
   boolean allItemsAvailable = order.getOrderItems().stream()
       .allMatch(item -> {
           boolean hasStock = inventoryService.checkStock(...);
           logger.info("  Stock check at fulfill: item {} qty {} - available: {}", 
               item.getItemId(), item.getQuantity(), hasStock);
           return hasStock;
       });
   ```

2. **Call helper method after successful fulfillment:**
   ```java
   if (allUpdatesSuccessful) {
       order.setStatus("COMPLETED");
       // ... existing code ...
       
       // Update triggerScenario for other CONFIRMED orders at this workstation
       updateOtherConfirmedOrdersScenario(order.getWorkstationId(), order.getId());
   }
   ```

3. **New helper method `updateOtherConfirmedOrdersScenario()`:**
   - Finds all CONFIRMED orders at the same workstation (excluding current order)
   - Re-checks stock for each order's items
   - Updates `triggerScenario` based on current stock:
     - All items available → `DIRECT_FULFILLMENT`
     - Some items available → `PARTIAL_FULFILLMENT`
     - No items available → `WAREHOUSE_ORDER_NEEDED`
   - Saves updated orders to database
   - Logs scenario changes

## How It Works Now

### Order Confirmation (unchanged)
1. User confirms Order #1 → Stock check → `triggerScenario=DIRECT_FULFILLMENT`
2. User confirms Order #2 → Stock check → `triggerScenario=DIRECT_FULFILLMENT`
3. Both orders show "Fulfill" button ✅

### Order Fulfillment (NEW BEHAVIOR)
1. User fulfills Order #1:
   - Stock debited (5 → 0)
   - Order #1 status = COMPLETED
   - **NEW**: System finds Order #2 (also CONFIRMED at WS-7)
   - **NEW**: Re-checks stock for Order #2 items
   - **NEW**: Updates Order #2 `triggerScenario=WAREHOUSE_ORDER_NEEDED`
   - **NEW**: Saves Order #2 with updated scenario

2. Frontend fetches orders after fulfillment:
   - Order #1 = COMPLETED (hidden from list)
   - Order #2 = CONFIRMED + `triggerScenario=WAREHOUSE_ORDER_NEEDED`
   - Order #2 now shows "Process" button ✅

### Logging Output

```
Starting fulfillment for order 1 (CO-ABC123)
  Stock check at fulfill: item 1 qty 5 - available: true
All items available - proceeding with direct fulfillment
Order CO-ABC123 fulfilled directly. Inventory updated.
Updating triggerScenario for other CONFIRMED orders at workstation 7
Found 1 other CONFIRMED orders to update
Order CO-DEF456 triggerScenario updated: DIRECT_FULFILLMENT → WAREHOUSE_ORDER_NEEDED
```

## Edge Cases Handled

1. **Multiple orders at different workstations:** Only updates orders at the same workstation (stock is workstation-specific)
2. **Partial stock availability:** Sets `PARTIAL_FULFILLMENT` if some items available
3. **No scenario change:** Only saves to database if `triggerScenario` actually changed (avoids unnecessary writes)
4. **Order already fulfilled/processing:** Only updates CONFIRMED orders (won't touch orders in other statuses)

## Testing on Live Server

### Test Scenario
1. SSH into live server
2. Pull latest changes: `git pull origin prod`
3. Rebuild service: `docker-compose build --no-cache order-processing-service`
4. Restart service: `docker-compose up -d order-processing-service`
5. Watch logs: `docker-compose logs -f order-processing-service`

### Validation Steps
1. Create 2 identical customer orders (same products, same quantities)
2. Confirm both orders → Both show "Fulfill" button
3. Click "Fulfill" on first order → Watch logs for scenario update
4. Refresh page or wait for auto-refresh (30s)
5. **Expected**: Second order now shows "Process" button (not "Fulfill")
6. Click "Process" on second order → Should create warehouse order

### What to Look For in Logs
```
[order-processing-service] Starting fulfillment for order 1 (CO-...)
[order-processing-service]   Stock check at fulfill: item X qty Y - available: true
[order-processing-service] Order CO-... fulfilled directly. Inventory updated.
[order-processing-service] Updating triggerScenario for other CONFIRMED orders at workstation 7
[order-processing-service] Found 1 other CONFIRMED orders to update
[order-processing-service] Order CO-... triggerScenario updated: DIRECT_FULFILLMENT → WAREHOUSE_ORDER_NEEDED
```

## Future Improvements (Optional)

1. **Real-time UI updates:** Add WebSocket to push scenario changes to frontend immediately (currently requires page refresh or 30s auto-refresh)
2. **Computed field approach:** Make `triggerScenario` a computed field in the DTO (calculate on-demand) instead of storing it in database
3. **Background job:** Add scheduled task to recalculate scenarios for all CONFIRMED orders periodically
4. **Warehouse orders:** Apply same pattern to `WarehouseOrderService.fulfillWarehouseOrder()` for Modules Supermarket

## Related Files

- `FulfillmentService.java` - Main fix location
- `CustomerOrderService.java` - Sets initial triggerScenario during confirmation
- `PlantWarehouseDashboard.jsx` - Frontend component that shows Fulfill/Process buttons
- `CustomerOrderCard.jsx` - Renders buttons based on triggerScenario
- `useWorkstationOrders.js` - Hook that fetches orders (already calls fetchOrders after fulfillment)

## Git History

```bash
# Commit that added initial logging
git show 5381e05

# Commit that fixed the dynamic scenario update
git show 52980ef

# View the actual changes
git diff 5381e05..52980ef lego-factory-backend/order-processing-service/src/main/java/io/life/order/service/FulfillmentService.java
```

---

**Status:** ✅ Fixed and deployed to both `dev` and `prod` branches  
**Next:** Test on live server to validate fix with real user workflow
