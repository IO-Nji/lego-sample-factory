# Standardized Dashboard Pattern

## Overview
All station dashboards (excluding Admin and Modules Supermarket template) now follow a standardized layout pattern using the `DashboardLayout` component.

## Layout Structure

```
┌────────────────────────────────────────────────────────────┐
│  PageHeader (Title, Subtitle, Icon)                       │
└────────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────────┐
│  Error/Success Messages (dismissible)                     │
└────────────────────────────────────────────────────────────┘
┌─────────────┬─────────────┬─────────────┬─────────────────┐
│  StatCard   │  StatCard   │  StatCard   │  StatCard      │
│  (Total)    │  (Pending)  │  (Progress) │  (Completed)   │
└─────────────┴─────────────┴─────────────┴─────────────────┘
┌──────────────────────────────────┬──────────────────────────┐
│  Primary Content (50%)           │  Secondary Content (50%) │
│  • Form (Plant Warehouse)        │  • Inventory Display     │
│  • Table (Other Stations)        │  • Stock Cards           │
│  • Order List (Modules SM)       │                          │
└──────────────────────────────────┴──────────────────────────┘
┌────────────────────────────────────────────────────────────┐
│  Orders Section (4-column grid)                            │
│  • Recent Orders                                           │
│  • Action Buttons                                          │
└────────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────────┐
│  Info Box (optional)                                       │
│  • Scenario information                                    │
│  • Usage instructions                                      │
└────────────────────────────────────────────────────────────┘
```

## Component Usage

```jsx
import { DashboardLayout, StatsCard } from '../components';

<DashboardLayout
  statsCards={renderStatsCards()}
  primaryContent={renderPrimaryContent()}
  secondaryContent={renderSecondaryContent()}
  ordersSection={renderOrdersSection()}
  infoBox={renderInfoBox()}  // optional
  messages={{ error, success }}
  onDismissError={() => setError(null)}
  onDismissSuccess={() => setSuccessMessage(null)}
/>
```

## Implementation Pattern

### 1. Stats Cards (4 columns)
```jsx
const renderStatsCards = () => (
  <>
    <StatsCard value={total} label="Total" variant="default" />
    <StatsCard value={pending} label="Pending" variant="pending" />
    <StatsCard value={inProgress} label="In Progress" variant="processing" />
    <StatsCard value={completed} label="Completed" variant="completed" />
  </>
);
```

### 2. Primary Content (50% width)
**Plant Warehouse:** Create Order Form
```jsx
const renderCreateOrderForm = () => (
  <>
    <div className="dashboard-box-header dashboard-box-header-blue">
      <h2 className="dashboard-box-header-title">➕ Create Order</h2>
    </div>
    <div style={{ overflowX: 'auto', maxHeight: '400px' }}>
      <table className="dashboard-table">
        {/* Form content */}
      </table>
    </div>
    <div className="dashboard-box-footer">
      <button>Submit</button>
    </div>
  </>
);
```

**Other Stations:** Inventory/Stock Table
```jsx
const renderInventoryTable = () => (
  <>
    <div className="dashboard-box-header dashboard-box-header-orange">
      <h2 className="dashboard-box-header-title">📦 Inventory</h2>
    </div>
    <div style={{ overflowX: 'auto' }}>
      <table className="dashboard-table">
        {/* Table content */}
      </table>
    </div>
  </>
);
```

### 3. Secondary Content (50% width)
Inventory display with grid cards:
```jsx
const renderInventoryDisplay = () => (
  <>
    <div className="dashboard-box-header dashboard-box-header-blue">
      <h2 className="dashboard-box-header-title">📦 Current Inventory</h2>
    </div>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.75rem', padding: '1rem' }}>
      {items.map(item => (
        <div key={item.id} style={{ /* card styles */ }}>
          <div>{item.name}</div>
          <div>{item.quantity}</div>
        </div>
      ))}
    </div>
  </>
);
```

### 4. Orders Section
```jsx
const renderOrdersSection = () => (
  <>
    <div className="dashboard-box-header dashboard-box-header-blue">
      <h2 className="dashboard-box-header-title">📋 Recent Orders</h2>
      <button className="dashboard-box-header-action">View All</button>
    </div>
    <div className="dashboard-box-content">
      <div className="dashboard-orders-grid">
        {orders.map(order => (
          <OrderCard key={order.id} order={order}>
            {/* Action buttons */}
          </OrderCard>
        ))}
      </div>
    </div>
  </>
);
```

### 5. Info Box (Optional)
```jsx
const renderInfoBox = () => (
  <>
    <h3>About {Station Name}</h3>
    <ul>
      <li>Usage instructions</li>
      <li>Business scenario info</li>
    </ul>
  </>
);
```

## CSS Classes Reference

### Box Headers
- `dashboard-box-header` - Base header style
- `dashboard-box-header-blue` - Blue theme (Plant Warehouse, etc.)
- `dashboard-box-header-orange` - Orange theme (Modules Supermarket)
- `dashboard-box-header-green` - Green theme (Production, Assembly)

### Content Sections
- `dashboard-box-content` - Standard padding for content areas
- `dashboard-table` - Standardized table styles
- `dashboard-orders-grid` - 4-column responsive order grid
- `dashboard-empty-state` - Empty state message styling

### Messages
- `dashboard-message` - Base message style
- `dashboard-message-error` - Red error messages
- `dashboard-message-success` - Green success messages

## Refactored Pages

### ✅ Completed
1. **ModulesSupermarketPage.jsx**
   - Primary: Warehouse Orders List (replaces form concept)
   - Secondary: Module Inventory Table
   - Orders: None (orders are primary content)
   - Info Box: Warehouse order scenarios

2. **PlantWarehousePage.jsx**
   - Primary: Create Customer Order Form (50% width)
   - Secondary: Product Inventory Cards 2x2 grid (50% width)
   - Orders: Recent Customer Orders (4-column grid)
   - Info Box: None

### 🔄 To Be Refactored
- ProductionPlanningPage.jsx
- ProductionControlPage.jsx
- AssemblyControlPage.jsx
- PartsSupplyWarehousePage.jsx
- ManufacturingWorkstationPage.jsx
- (All follow same pattern: Table/Form + Inventory + Orders)

## Key Benefits

1. **Consistency** - All dashboards share the same visual structure
2. **Modularity** - Easy to maintain and debug
3. **Responsiveness** - Built-in responsive grid system
4. **Reusability** - DashboardLayout component handles layout logic
5. **Simplified Code** - Render functions separate concerns
6. **Design System Compliance** - Uses standardized CSS classes

## Migration Guide for Other Dashboards

1. Import DashboardLayout and StatsCard:
   ```jsx
   import { DashboardLayout, StatsCard } from '../components';
   ```

2. Create render functions for each section:
   - `renderStatsCards()`
   - `renderPrimaryContent()` (form or table)
   - `renderSecondaryContent()` (inventory display)
   - `renderOrdersSection()`
   - `renderInfoBox()` (optional)

3. Replace existing JSX structure with:
   ```jsx
   <DashboardLayout
     statsCards={renderStatsCards()}
     primaryContent={renderPrimaryContent()}
     secondaryContent={renderSecondaryContent()}
     ordersSection={renderOrdersSection()}
     messages={{ error, success }}
     onDismissError={() => setError(null)}
     onDismissSuccess={() => setSuccessMessage(null)}
   />
   ```

4. Remove inline styles and old CSS imports
5. Update color theme (blue/orange/green) in box headers

## File References

- Component: [lego-factory-frontend/src/components/DashboardLayout.jsx](../lego-factory-frontend/src/components/DashboardLayout.jsx)
- Styles: [lego-factory-frontend/src/styles/DashboardLayout.css](../lego-factory-frontend/src/styles/DashboardLayout.css)
- Example 1: [lego-factory-frontend/src/pages/ModulesSupermarketPage.jsx](../lego-factory-frontend/src/pages/ModulesSupermarketPage.jsx)
- Example 2: [lego-factory-frontend/src/pages/PlantWarehousePage.jsx](../lego-factory-frontend/src/pages/PlantWarehousePage.jsx)
