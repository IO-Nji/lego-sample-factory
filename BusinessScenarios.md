It is a translation of the "Behavioral Models" (Activity Diagrams) from the thesis (Chapter 10.1, Figures 11-14) into detailed, step-by-step descriptions, focusing on the interactions between planned microservices and user roles.
These scenarios describe the core logic for fulfilling a customer order, based on the stock levels and the order's lot-size. Each scenario builds upon the previous one, introducing more complexity in the supply chain.

**Common Elements across Scenarios:**

*   **Actors:** Customer, Plant Warehouse (PW), Modules Supermarket (MS), Production Planning (PP), Production Control (PC), Assembly Control (AC), Parts Supply Warehouse (PSW), Injection Molding (IM), Parts Pre-Production (PPP), Part Finishing (PF), Gear Assembly (GA), Motor Assembly (MA), Final Assembly (FA).
*   **Microservices involved:** `order-processing-service` (orchestrates), `inventory-service` (stock management), `simal-integration-service` (mocked SimAL.Scheduler), `masterdata-service` (item definitions).
*   **Lot-Size Threshold:** A configurable system parameter (e.g., `LOT_SIZE_THRESHOLD = 3`) that determines whether a small missing quantity should trigger production via Modules Supermarket (below threshold) or direct production planning (above threshold).
*   **Item Types:** Product Variant (final product), Module (intermediate component), Part (raw material).

---

## **Scenario 1: Sunny Day - Enough Finished Product Variants in Plant Warehouse Stock**

**Thesis Reference:** Chapter 10.1.1, Figure 11 (Activity Diagram for a Sunny Day Scenario)

**Description:** The customer places an order, and the Plant Warehouse has all the required finished product variants in sufficient quantities to fulfill it immediately.

**Trigger:**
*   A new Customer Order is received by the Plant Warehouse.

**Key Conditions:**
*   `CustomerOrder.status` is `PENDING`.
*   For every `OrderItem` in the `CustomerOrder`:
    *   The `itemType` is `PRODUCT_VARIANT`.
    *   The `inventory-service` indicates that `Plant Warehouse Stock` (for the specified `ProductVariant`) has `quantity >= OrderItem.quantity`.

**Detailed Steps (Primarily handled by `order-processing-service`):**

1.  **Customer Order Creation:**
    *   A `CUSTOMER` (simulated via UI or API) places an order 
    *   `Plant Warehouse` user creates a `CustomerOrder`for specific `ProductVariant` (via `order-processing-service API`).
    *   `CustomerOrder.status` is set to `PENDING`.
2.  **Fulfillment Request:**
    *   `Plant Warehouse` user initiates fulfillment for the `CustomerOrder` ( clicks "Confirm" button in UI, calling `PUT /customer-orders/{id}/fulfill`).
3.  **Stock Check (Orchestration):**
    *   `order-processing-service` checks `inventory-service` for `Plant Warehouse Stock` for each `ProductVariant` in the `CustomerOrder`.
4.  **Condition Met:**
    *   `order-processing-service` confirms enough stock is available for all items. Plant Warehouse User can now fulfill the Customer order by clicking "Fulfill".
5.  **Stock Deduction:**
    *   `order-processing-service` calls `inventory-service` to `debit` the `Plant Warehouse Stock` for each `ProductVariant` by the `OrderItem.quantity`.
6.  **Customer Order Fulfillment:**
    *   `CustomerOrder.status` is updated to `COMPLETED` in `order-processing-service`.
7.  **Delivery (Simulated):**
    *   A delivery record is created.

**Expected Outcome:**
*   `CustomerOrder.status` is `COMPLETE`.
*   `Plant Warehouse Stock` for the ordered `ProductVariant`s is reduced by the ordered quantities in `inventory-service`.
*    A Delivery record is created

**Key Development Takeaways:**
*   Needs robust `GET /stock` and `POST /stock/debit` endpoints in `inventory-service`.
*   `order-processing-service` acts as the orchestrator, making decisions based on `inventory-service` responses.
*   Frontend needs to display `Plant Warehouse Stock` and allow `Plant Warehouse` user to create and fulfill `CustomerOrder`s.

---

## **Scenario 2: Low Stock in Plant Warehouse, Lot-Size Below Threshold, Enough Modules MS**

**Description:** The Customer Order requires a quantity of some `ProductVariant`s that are not fully available in the `Plant Warehouse` stock. The total quantity of missing items for this `CustomerOrder` is below a predefined `Lot-Size Threshold`. This triggers an internal `Warehouse Order` to the `Modules Supermarket` for the missing ProductVariant. However, the `Modules Supermarket` *does* have enough `Modules` to assemble the missing `ProductVariant`s. 

**Trigger:**
*   A new Customer Order is received by the `Plant Warehouse`.
*   Plant Warehouse confirms receiving the Customer Order by clicking the "Confirm" button 
*   `Plant Warehouse` does not have enough quantities of the Product Variant items to fulfill the order
*   `Plant Warehouse` user initiates "processing" by clicking on the "Process" button of the order card as it does not have enough Product Variant quantities to fulfill the order.

**Key Conditions:**
*   `CustomerOrder.status` is `PENDING`.
*   For at least one `OrderItem` in the `CustomerOrder`:
    *   `Plant Warehouse Stock` for the `ProductVariant`s `quantity < OrderItem.quantity` (or completely missing).
*   The total quantity of all missing `ProductVariant`s from the `CustomerOrder` is **below** the `LOT_SIZE_THRESHOLD`.
*   For the required `Modules` (to produce the missing `ProductVariant`s):
    *   `Modules Supermarket Stock` has `quantity >= required_module_quantity`.

**Detailed Steps (Orchestrated by `order-processing-service`):**

1.  **Customer Order Creation & Initial Fulfillment Attempt:** (Same as Scenario 1, steps 1-2)
    *   `Plant Warehouse` user creates a `CustomerOrder`. `CustomerOrder.status` is `PENDING`.
    *   `Plant Warehouse` user initiates fulfillment by clicking CONFIRM.
2.  **Stock Check & Partial Fulfillment:**
    *   `order-processing-service` checks `inventory-service` for `Plant Warehouse Stock`.
    *   Identifies missing `ProductVariant`s and the quantities.
    *   (Optional: Partially fulfills available stock for the `CustomerOrder` and debits `Plant Warehouse Stock`).
3.  **Create `WarehouseOrder` for MS:**
    *   `order-processing-service` determines the total missing quantity is below `LOT_SIZE_THRESHOLD`.
    *   `order-processing-service` creates a new `WarehouseOrder` for the `Modules Supermarket`, detailing the missing `ProductVariant`s. `WarehouseOrder.status` is `PENDING`.
4.  **MS Receives `WarehouseOrder`:**
    *   `Modules Supermarket` user views incoming `WarehouseOrder`s (via `order-processing-service` API).
    *   `Modules Supermarket` user initiates fulfillment of the `WarehouseOrder` by clicking 'confirm'.
5.  **MS Stock Check (Orchestration):**
    *   `order-processing-service` checks `masterdata-service` to get the `Modules` required for the `ProductVariant`s in the `WarehouseOrder`.
    *   `order-processing-service` checks `inventory-service` for `Modules Supermarket Stock` for these `Modules`.
6.  **Condition Met (Enough Modules):**
    *   `order-processing-service` confirms enough `Modules` are available in `Modules Supermarket Stock`.
7.  **Stock Deduction (MS) & Module Transfer:**
    *   `order-processing-service` calls `inventory-service` to `debit` `Modules Supermarket Stock` for the required `Modules`.
    *   `order-processing-service` marks `WarehouseOrder.status` as `PROCESSING` and conceptually transfers the `Modules` to `Final Assembly`.
8.  **Final Assembly (FA) Process:**
    *   `Final Assembly` workstation receives and confirms an internal assembly task for the `ProductVariant`s by clicking 'confirm'.
    *   `Final Assembly` completes the assembly task and clicks "Fulfill".
    *   `order-processing-service` marks the internal assembly task as `COMPLETE`.
9.  **Product Transfer to PW:**
    *   Upon completion by `Final Assembly`, `order-processing-service` calls `inventory-service` to `credit` the `Plant Warehouse Stock` with the newly assembled `ProductVariant`s.
    *   `CustomerOrder`.status` for the corresponding WarehouseOrder is updated to `PENDING`, and the Confirm button is activated.
10. **`WarehouseOrder` Fulfillment:**
    *   Plant Warehouse Operator confirms the CustomerOrder update by clicking 'confirm' and then fulfills Order by clicking 'fulfill'
11. **Final `CustomerOrder` Fulfillment:**
    *   `order-processing-service` (or `Plant Warehouse` user) manually completes the outstanding `CustomerOrder` from the now replenished `Plant Warehouse Stock`.
    *   `CustomerOrder.status` is updated to `FULFILLED`.

**Expected Outcome:**
*   `CustomerOrder.status` is `FULFILLED`.
*   `Plant Warehouse Stock` is replenished (implicitly, then debited for the customer order).
*   `Modules Supermarket Stock` for the consumed `Modules` is reduced.
*   A `WarehouseOrder` is created and fulfilled.

**Key Development Takeaways:**
*   Introduces `WarehouseOrder` entity and its lifecycle.
*   Requires `masterdata-service` to define `ProductVariant` to `Module` composition.
*   Inter-service calls between `order-processing-service` and `inventory-service` become more complex (debiting modules, crediting products).
*   `order-processing-service` needs internal logic to orchestrate multiple steps and update several orders/stocks.
*   `Modules Supermarket` and `Final Assembly` UI roles are introduced.

---

## **Scenario 3: Low Stock PW, Lot-Size Below Threshold, Low Stock Modules MS**

**Thesis Reference:** Chapter 10.1.3, Figure 13 (Activity Diagram for Scenario 3)

**Description:** Similar to Scenario 2, but the `Modules Supermarket` does *not* have enough `Modules` to fulfill the `Warehouse Order`. This triggers a full production cycle, starting with a `Production Order` to `Production Planning`.

**Trigger:**
*   A new Customer Order is received by the `Plant Warehouse`.
*   `Plant Warehouse` user initiates fulfillment.

**Key Conditions:**
*   `CustomerOrder.status` is `PENDING`.
*   `Plant Warehouse Stock` for the `ProductVariant` is `quantity < OrderItem.quantity`.
*   The total missing quantity of `ProductVariant`s for the `CustomerOrder` is **below** the `LOT_SIZE_THRESHOLD`.
*   For the required `Modules` (to produce the missing `ProductVariant`s):
    *   `Modules Supermarket Stock` has `quantity < required_module_quantity`.

**Detailed Steps (Complex orchestration by `order-processing-service`):**

1.  **Customer Order & `WarehouseOrder` Creation:** (Same as Scenario 2, steps 1-3)
    *   `Plant Warehouse` user creates `CustomerOrder`.
    *   `order-processing-service` creates `WarehouseOrder` for `Modules Supermarket`.
2.  **MS Receives `WarehouseOrder` & Stock Check:**
    *   `Modules Supermarket` user initiates fulfillment of `WarehouseOrder`.
    *   `order-processing-service` checks `Modules Supermarket Stock` for required `Modules` and finds it insufficient.
3.  **Create `ProductionOrder` for PP:**
    *   `order-processing-service` creates a new `ProductionOrder` (for `Production Planning`) for the missing `Modules`. `ProductionOrder.status` is `PENDING`.
    *   `WarehouseOrder.status` updated to `AWAITING_PRODUCTION`.
4.  **Production Planning (PP) & SimAL Integration:**
    *   `Production Planning` user views incoming `ProductionOrder`s.
    *   `Production Planning` user initiates planning for the `ProductionOrder`.
    *   `order-processing-service` sends the `ProductionOrder` details to `simal-integration-service` (mock SimAL.Scheduler) via `POST /simal/production-order`.
    *   `simal-integration-service` responds with `SimALScheduledOrder` data (mocked `ProductionControlOrder`s and `AssemblyControlOrder`s) including estimated start/finish times.
    *   `order-processing-service` creates `ProductionControlOrder`s and `AssemblyControlOrder`s internally based on SimAL output.
    *   `ProductionOrder.status` updated to `PLANNED`.
5.  **Control Stations (PC & AC) Dispatch Sub-Orders:**
    *   `Production Control` and `Assembly Control` users view their assigned `ProductionControlOrder`s and `AssemblyControlOrder`s.
    *   **For Manufacturing Orders (from PC):**
        *   `Production Control` dispatches `InjectionMoldOrder`, `PartPreProductionOrder`, `PartFinishingOrder` for specific `Modules` to relevant workstations.
        *   `Production Control` also dispatches `Production Part Supply Order` to `Parts Supply Warehouse` for `Parts`.
    *   **For Assembly Orders (from AC):**
        *   `Assembly Control` dispatches `GearAssemblyOrder`, `MotorAssemblyOrder`, `FinalAssemblyOrder` for specific `Modules` (or `ProductVariant` for Final Assembly) to relevant workstations.
        *   `Assembly Control` also dispatches `Assembly Part Supply Order` to `Parts Supply Warehouse` for `Parts`.
    *   All these sub-orders initially have `PENDING` status.
6.  **Parts Supply Warehouse (PSW) Fulfillment:**
    *   `Parts Supply Warehouse` user views incoming `Production Part Supply Order`s and `Assembly Part Supply Order`s.
    *   `Parts Supply Warehouse` user fulfills the `SupplyOrder`.
    *   `order-processing-service` calls `inventory-service` to `debit` `Parts Supply Warehouse Stock` for the `Parts`.
    *   `SupplyOrder.status` updated to `FULFILLED`.
7.  **Manufacturing Workstation Execution:**
    *   `Injection Molding`, `Parts Pre-Production`, `Part Finishing` users view their respective orders.
    *   User initiates "Start" and "Complete" actions for each order.
    *   Upon "Complete":
        *   `order-processing-service` records `actualStartTime` and `actualFinishTime`.
        *   `order-processing-service` calls `simal-integration-service` (`POST /simal/update-time`) to update SimAL.
        *   `order-processing-service` calls `inventory-service` to `credit` `Modules Supermarket Stock` with the newly produced `Modules`.
        *   Manufacturing Order status updated to `COMPLETED`.
8.  **Assembly Workstation Execution:**
    *   `Gear Assembly`, `Motor Assembly`, `Final Assembly` users view their respective orders.
    *   User initiates "Start" and "Complete" actions for each order.
    *   Upon "Complete":
        *   `order-processing-service` records `actualStartTime` and `actualFinishTime`.
        *   `order-processing-service` calls `simal-integration-service` (`POST /simal/update-time`) to update SimAL.
        *   `order-processing-service` calls `inventory-service` to `credit` `Modules Supermarket Stock` with the newly assembled `Modules` (for GA, MA) or `Plant Warehouse Stock` with finished `ProductVariant`s (for FA).
        *   Assembly Order status updated to `COMPLETED`.
9.  **Modules Supermarket Receives Modules:**
    *   As manufacturing/assembly workstations credit `Modules Supermarket Stock`, the required `Modules` eventually become available.
10. **MS Fulfills `WarehouseOrder`:** (Same as Scenario 2, steps 5-11, now that modules are in stock)
    *   `Modules Supermarket` user re-initiates fulfillment of the `WarehouseOrder`.
    *   `order-processing-service` checks `Modules Supermarket Stock` again (now sufficient).
    *   `order-processing-service` debits `Modules Supermarket Stock` for the `Modules`.
    *   `WarehouseOrder.status` updated to `FULFILLED`.
    *   `order-processing-service` orchestrates Final Assembly and product transfer to `Plant Warehouse`.
11. **Final `CustomerOrder` Fulfillment:**
    *   `order-processing-service` (or `Plant Warehouse` user) completes the `CustomerOrder`.
    *   `CustomerOrder.status` is `FULFILLED`.

**Expected Outcome:**
*   `CustomerOrder.status` is `FULFILLED`.
*   All intermediate orders (`WarehouseOrder`, `ProductionOrder`, `ControlOrders`, `SupplyOrders`, `ManufacturingOrders`, `AssemblyOrders`) are created and transition through `PLANNED`, `IN_PROGRESS`, `COMPLETED` statuses.
*   `Plant Warehouse Stock` for the `ProductVariant`s is eventually replenished and then debited.
*   `Modules Supermarket Stock` for `Modules` is debited after replenishment from production.
*   `Parts Supply Warehouse Stock` for `Parts` is debited.
*   SimAL.Scheduler (mock) receives order and time updates.

**Key Development Takeaways:**
*   This is the most complex scenario, involving almost all microservices and actors.
*   Requires a robust order tracking system to link sub-orders back to the original `CustomerOrder`.
*   Significant use of inter-service communication (Feign Client is critical here).
*   State machines for orders (PENDING -> PLANNED -> IN_PROGRESS -> COMPLETED) are essential.
*   `masterdata-service` must define `Module` to `Part` composition and `ProductVariant` to `Module` composition.
*   Each workstation needs a UI to view and interact with its assigned tasks.

---

## **Scenario 4: Low Stock PW, Lot-Size Above Threshold**

**Thesis Reference:** Chapter 10.1.4, Figure 14 (Activity Diagram for Scenario 4)

**Description:** The customer places an order, but the `Plant Warehouse` doesn't have enough `ProductVariant`s. Crucially, the total quantity of missing items for this `CustomerOrder` is **above** the `Lot-Size Threshold`. This signifies a large production run, so the `Plant Warehouse` sends a `Production Order` *directly* to `Production Planning`, bypassing the `Modules Supermarket`'s initial module stock check.

**Trigger:**
*   A new Customer Order is received by the `Plant Warehouse`.
*   `Plant Warehouse` user initiates fulfillment.

**Key Conditions:**
*   `CustomerOrder.status` is `PENDING`.
*   `Plant Warehouse Stock` for the `ProductVariant` is `quantity < OrderItem.quantity`.
*   The total missing quantity of `ProductVariant`s for the `CustomerOrder` is **above** the `LOT_SIZE_THRESHOLD`.

**Detailed Steps (Orchestrated by `order-processing-service`):**

1.  **Customer Order Creation & Initial Fulfillment Attempt:** (Same as Scenario 1, steps 1-2)
    *   `Plant Warehouse` user creates `CustomerOrder`. `CustomerOrder.status` is `PENDING`.
    *   `Plant Warehouse` user initiates fulfillment.
2.  **Stock Check & Partial Fulfillment:** (Same as Scenario 2, step 2)
    *   `order-processing-service` checks `inventory-service` for `Plant Warehouse Stock`.
    *   Identifies missing `ProductVariant`s and quantities.
    *   (Optional: Partially fulfills available stock for the `CustomerOrder` and debits `Plant Warehouse Stock`).
3.  **Create `ProductionOrder` for PP (Direct):**
    *   `order-processing-service` determines the total missing quantity is **above** `LOT_SIZE_THRESHOLD`.
    *   `order-processing-service` creates a new `ProductionOrder` (for `Production Planning`) for the missing `ProductVariant`s directly. `ProductionOrder.status` is `PENDING`.
    *   *Note: No `WarehouseOrder` is created in this scenario.*
4.  **Production Planning (PP) & SimAL Integration:** (Same as Scenario 3, step 4)
    *   `Production Planning` user views incoming `ProductionOrder`s.
    *   `Production Planning` user initiates planning for the `ProductionOrder`.
    *   `order-processing-service` sends `ProductionOrder` details to `simal-integration-service`.
    *   `simal-integration-service` responds with `SimALScheduledOrder` data (mocked Control Orders).
    *   `order-processing-service` creates `ProductionControlOrder`s and `AssemblyControlOrder`s.
    *   `ProductionOrder.status` updated to `PLANNED`.
5.  **Control Stations (PC & AC) Dispatch Sub-Orders:** (Same as Scenario 3, step 5)
    *   `Production Control` and `Assembly Control` dispatch their respective manufacturing, assembly, and supply orders.
6.  **Parts Supply Warehouse (PSW) Fulfillment:** (Same as Scenario 3, step 6)
    *   `Parts Supply Warehouse` fulfills `SupplyOrder`s, debiting `Parts Supply Warehouse Stock`.
7.  **Manufacturing Workstation Execution:** (Same as Scenario 3, step 7)
    *   Manufacturing workstations execute, updating SimAL and crediting `Modules Supermarket Stock` with produced `Modules`.
8.  **Assembly Workstation Execution:** (Same as Scenario 3, step 8)
    *   Assembly workstations execute, updating SimAL.
    *   `Final Assembly` completion credits `Plant Warehouse Stock` directly with finished `ProductVariant`s. (Crucial difference from Scenario 3, where MS was credited with modules).
9.  **Final `CustomerOrder` Fulfillment:** (Same as Scenario 3, step 11)
    *   `order-processing-service` (or `Plant Warehouse` user) completes the `CustomerOrder` from the now replenished `Plant Warehouse Stock`.
    *   `CustomerOrder.status` is `FULFILLED`.

**Expected Outcome:**
*   `CustomerOrder.status` is `FULFILLED`.
*   All necessary production and assembly orders are created and completed.
*   `Plant Warehouse Stock` is eventually replenished with the `ProductVariant`s and then debited.
*   `Modules Supermarket Stock` is replenished by manufacturing, then modules are directly consumed by `Final Assembly` (no explicit MS stock debit for a `WarehouseOrder` here).
*   `Parts Supply Warehouse Stock` is debited.
*   SimAL.Scheduler (mock) receives order and time updates.
*   **Key difference:** No `WarehouseOrder` to `Modules Supermarket` is generated for the missing `ProductVariant`s.

**Key Development Takeaways:**
*   The lot-size threshold is a critical decision point for the `order-processing-service`.
*   The flow largely mirrors Scenario 3 but critically bypasses `WarehouseOrder` creation and MS module stock checking for the *final product*. This means `Final Assembly` will directly deliver to `Plant Warehouse` for the specific `CustomerOrder`.
*   Requires careful tracking of where produced items are ultimately credited (MS vs. PW).
