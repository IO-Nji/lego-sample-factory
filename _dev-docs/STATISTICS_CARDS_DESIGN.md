# Statistics Cards Design System

> **Last Updated:** February 5, 2026  
> **Purpose:** Comprehensive design reference for statistics cards across all LIFE dashboards. Use this document when implementing new dashboards, debugging stat displays, or extending the statistics system.
> **Mockup:** [StatisticsCard.mockup.html](../lego-factory-frontend/src/components/statistics/StatisticsCard.mockup.html)

---

## Design Principles

### Glassmorphism Foundation
All statistics cards follow the homepage glassmorphism aesthetic:
- Semi-transparent backgrounds with `backdrop-filter: blur(10px)`
- Soft shadows creating depth without harsh edges
- Gradient accents and color-coded borders
- Smooth hover transitions with subtle lift effects

### Color Semantics
| Variant | Color | Use Case |
|---------|-------|----------|
| `primary` | Blue `#3b82f6` | Default metrics, totals |
| `success` | Green `#10b981` | Completed, positive outcomes |
| `warning` | Amber `#f59e0b` | Pending, attention needed |
| `danger` | Red `#ef4444` | Critical, errors, out of stock |
| `info` | Cyan `#06b6d4` | In progress, informational |
| `neutral` | Gray `#64748b` | Inactive, secondary info |

---

## Card Type Reference

### Type 1: Basic Metric Card
**Component:** `StatCard` (existing)  
**Use Case:** Simple metrics like Total Orders, Pending Count, Stock Levels  
**Dashboards:** All workstations for basic order/inventory counts

```jsx
<StatCard
  value={142}
  label="Total Orders"
  variant="primary"
  icon="📦"
/>
```

**Features:**
- Top accent border with gradient
- Icon + value header layout
- Uppercase label below value
- Hover lift animation

---

### Type 2: Metric with Trend Indicator
**Component:** `StatCardTrend` (new)  
**Use Case:** Metrics with comparison data (vs yesterday, vs last week)  
**Dashboards:** Admin, Production Planning, Warehouse fulfillment stats

```jsx
<StatCardTrend
  value="847"
  label="Orders This Week"
  trend={{ value: 12, direction: 'up' }}
  comparison="vs 756 last week"
  icon="📦"
/>
```

**Features:**
- Trend badge (up/down/neutral) with percentage
- Comparison text for context
- Larger icon with gradient background

---

### Type 3: Progress/Gauge Card
**Component:** `StatCardProgress` (new)  
**Use Case:** Completion rates, utilization percentages, capacity  
**Dashboards:** Production Planning (utilization), Workstations (progress)

```jsx
<StatCardProgress
  value={78}
  label="Production Utilization"
  detail="7/9 workstations active"
  target={85}
  variant="primary" // or 'success', 'warning', 'danger' for bar color
/>
```

**Features:**
- Horizontal progress bar with gradient fill
- Value displayed prominently
- Detail text below bar
- Auto-color based on threshold proximity

---

### Type 4: Sparkline Trend Card
**Component:** `StatCardSparkline` (new)  
**Use Case:** Trending metrics with historical visualization  
**Dashboards:** Admin (orders/hour), Production Planning (throughput)

```jsx
<StatCardSparkline
  value={24}
  label="Orders / Hour"
  data={[45, 60, 35, 80, 65, 90, 75, 100]}
  trend={{ value: 15, direction: 'up' }}
/>
```

**Features:**
- Mini bar chart showing recent history
- Trend badge in header
- Hover effects on individual bars
- Responsive bar width

---

### Type 5: Threshold Alert Card
**Component:** `StatCardThreshold` (new)  
**Use Case:** Stock levels with threshold warnings  
**Dashboards:** Warehouses (WS-7, 8, 9), Inventory monitoring

```jsx
<StatCardThreshold
  value={3}
  label="Gear Module Stock"
  status="critical" // 'critical', 'low', 'ok'
  threshold={10}
  maxValue={20}
  icon="🔧"
/>
```

**Features:**
- Status badge (Critical/Low/OK) with color
- Visual threshold indicator line
- Pulsing border animation for critical status
- Background gradient shift for alerts

---

### Type 6: Compact Inline Card
**Component:** `StatCardCompact` (new)  
**Use Case:** Dense information display, sidebar stats  
**Dashboards:** Compact layouts, mobile views, sidebars

```jsx
<StatCardCompact
  value={142}
  label="Total Orders"
  icon="📦"
  variant="primary"
/>
```

**Features:**
- Horizontal layout (icon | label + value)
- Minimal padding
- Good for stacking vertically
- Icon variants by semantic color

---

### Type 7: Multi-Metric Card
**Component:** `StatCardMulti` (new)  
**Use Case:** Related metrics grouped together  
**Dashboards:** Inventory summary, Order breakdown by type

```jsx
<StatCardMulti
  title="Order Summary"
  icon="📦"
  metrics={[
    { value: 142, label: 'Total' },
    { value: 23, label: 'Pending' },
    { value: 35, label: 'Active' },
    { value: 84, label: 'Done' },
  ]}
/>
```

**Features:**
- Header with icon and title
- 2x2 grid of mini metrics
- Shared card container
- Individual metric backgrounds

---

### Type 8: Status Timeline Card
**Component:** `StatCardTimeline` (new)  
**Use Case:** Order pipeline visualization, status distribution  
**Dashboards:** Production Planning, Admin overview

```jsx
<StatCardTimeline
  title="Customer Order Pipeline"
  total={142}
  steps={[
    { label: 'Pending', value: 23, status: 'pending' },
    { label: 'Confirmed', value: 18, status: 'confirmed' },
    { label: 'Processing', value: 35, status: 'in-progress' },
    { label: 'Completed', value: 66, status: 'completed' },
  ]}
/>
```

**Features:**
- Horizontal flow with arrows
- Status-colored bars showing counts
- Labels below each step
- Total count in header

---

### Type 9: Donut Chart Card
**Component:** `StatCardDonut` (new)  
**Use Case:** Distribution visualization, completion percentage  
**Dashboards:** Admin (order types), Inventory (stock distribution)

```jsx
<StatCardDonut
  title="Inventory by Type"
  centerValue="2.4k"
  centerLabel="Items"
  segments={[
    { value: 40, label: 'Parts', color: '#10b981' },
    { value: 35, label: 'Modules', color: '#3b82f6' },
    { value: 25, label: 'Products', color: '#f59e0b' },
  ]}
/>
```

**Features:**
- SVG donut chart with gradient strokes
- Center value display
- Legend with colored indicators
- Side-by-side layout (chart | content)

---

### Type 10: KPI Hero Card
**Component:** `StatCardHero` (new)  
**Use Case:** Primary dashboard metric, featured KPI  
**Dashboards:** Admin main metric, Revenue, Critical alerts

```jsx
<StatCardHero
  title="Today's Revenue"
  value="$47,850"
  subtitle="From 156 completed orders"
  icon="💰"
  trend={{ value: 18, direction: 'up' }}
/>
```

**Features:**
- Premium glassmorphism with shimmer animation
- Rainbow gradient top border
- Extra-large value with gradient text
- Prominent icon with shadow
- Trend badge inline with subtitle

---

### Type 11: Workstation Status Card
**Component:** `StatCardWorkstation` (new)  
**Use Case:** Workstation operational status, active/idle indicators  
**Dashboards:** Admin, Production Control, Assembly Control

```jsx
<StatCardWorkstation
  name="WS-1 Injection Molding"
  icon="💉"
  status="active" // 'active', 'idle', 'alert'
  detail="Active since 08:15"
  metrics={{ orders: 12, time: '2h 15m' }}
/>
```

**Features:**
- Status indicator with pulsing animation (active)
- Alert border animation (maintenance needed)
- Workstation name and detail
- Right-aligned metrics

---

### Type 12: Scenario Breakdown Card
**Component:** `StatCardScenario` (new)  
**Use Case:** Business scenario distribution (Scenario 1/2/3/4)  
**Dashboards:** Plant Warehouse, Admin analytics

```jsx
<StatCardScenario
  title="Order Fulfillment Scenarios"
  scenarios={[
    { id: 1, name: 'Direct Fulfillment', count: 45 },
    { id: 2, name: 'Via Warehouse', count: 38 },
    { id: 3, name: 'Full Production', count: 42 },
    { id: 4, name: 'High Volume', count: 17 },
  ]}
/>
```

**Features:**
- Scenario badges with unique colors
- 2x2 grid layout
- Count display per scenario
- Hover effect on items

---

## Use Cases by Dashboard

### Plant Warehouse (WS-7)
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Basic (×4) | Total Orders, Pending, Processing, Completed | Order statistics row |
| Basic (×4) | Product Types, Total Stock, Low Stock, Out of Stock | Inventory statistics row |
| Scenario | Scenario 1/2/3/4 distribution | Fulfillment breakdown |
| Threshold | Critical stock items | Alert for low products |

### Modules Supermarket (WS-8)
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Basic (×7) | Order statuses | Full warehouse order breakdown |
| Threshold (×3) | Module stock levels | Per-module stock alerts |
| Multi | Inventory Health | Types, total qty, low, critical |

### Parts Supply (WS-9)
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Basic (×5) | Total, Pending, In Progress, Fulfilled, Rejected | Supply order statistics |
| Threshold | Low Stock Parts | Parts below threshold |
| Multi | Parts Summary | Types, total qty |

### Manufacturing Workstations (WS-1, 2, 3)
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Basic (×6) | Standard order stats | Total through Submitted |
| Progress | Daily completion rate | Target vs actual |
| Sparkline | Parts produced trend | Hourly production chart |

### Assembly Workstations (WS-4, 5, 6)
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Basic (×6) | Standard order stats | Total through Submitted |
| Progress | Assembly completion | Today's target progress |
| Compact | Modules assembled today | Quick metric |

### Production Planning
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Multi | Production Orders | Created, Scheduled, Active, Done |
| Progress | Utilization | Workstation utilization % |
| Timeline | Order Pipeline | Status distribution flow |
| Sparkline | Throughput | Orders/hour trend |
| Workstation (×9) | WS Status | All workstation operational status |

### Production/Assembly Control
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Basic (×4) | Control order stats | Pending, Confirmed, Assigned, Completed |
| Compact | Supply orders waiting | Pending parts requests |
| Workstation (×3/6) | Target WS status | Manufacturing or Assembly stations |

### Admin Dashboard
| Card Type | Metric | Description |
|-----------|--------|-------------|
| Hero (×2) | Revenue, Efficiency | Primary KPIs |
| Trend (×4) | Orders, Stations, Users, Alerts | System-wide comparisons |
| Timeline | System Order Pipeline | All orders flow |
| Donut | Inventory Distribution | Parts/Modules/Products |
| Scenario | Scenario Performance | Avg times by scenario |

---

## Implementation Patterns

### Creating Statistics Data
```javascript
// In dashboard component
const stats = useMemo(() => [
  { value: orders.length, label: 'Total Orders', variant: 'primary', icon: '📦' },
  { value: orders.filter(o => o.status === 'PENDING').length, label: 'Pending', variant: 'warning', icon: '⏳' },
  { value: orders.filter(o => o.status === 'COMPLETED').length, label: 'Completed', variant: 'success', icon: '✅' },
], [orders]);
```

### Using StatisticsGrid
```jsx
import StatisticsGrid from '../../components/statistics/StatisticsGrid';

<StatisticsGrid stats={stats} columns={4} />
```

### Threshold Calculations
```javascript
const getThresholdStatus = (value, lowThreshold, criticalThreshold) => {
  if (value <= criticalThreshold) return 'critical';
  if (value <= lowThreshold) return 'low';
  return 'ok';
};
```

### Trend Calculations
```javascript
const calculateTrend = (current, previous) => ({
  value: previous > 0 ? Math.round(((current - previous) / previous) * 100) : 0,
  direction: current > previous ? 'up' : current < previous ? 'down' : 'neutral'
});
```

---

## CSS Design Tokens

```css
/* Brand Colors */
--brand-primary: #2c5aa0;
--brand-primary-light: #3b82f6;
--brand-secondary: #33a7d9;
--brand-accent: #f59e0b;
--brand-cyan: #06b6d4;

/* Semantic Colors */
--color-success: #10b981;
--color-warning: #f59e0b;
--color-danger: #ef4444;
--color-info: #3b82f6;
--color-neutral: #64748b;

/* Glassmorphism */
--glass-bg: rgba(255, 255, 255, 0.85);
--glass-bg-strong: rgba(255, 255, 255, 0.92);
--glass-border: rgba(255, 255, 255, 0.3);
--glass-blur: blur(10px);

/* Shadows */
--shadow-md: 0 4px 24px rgba(44, 90, 160, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
--shadow-lg: 0 12px 40px rgba(44, 90, 160, 0.15), 0 4px 12px rgba(0, 0, 0, 0.08);

/* Border Radius */
--radius-sm: 8px;
--radius-md: 12px;
--radius-lg: 16px;
--radius-xl: 20px;

/* Transitions */
--transition-fast: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
--transition-normal: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
```

---

## File Structure

```
lego-factory-frontend/src/components/statistics/
├── index.js                      # Exports all statistics components
├── StatisticsCard.mockup.html    # Visual design reference
├── StatCard.jsx                  # Type 1: Basic Metric Card (existing)
├── StatCard.module.css           # Basic card styles (existing)
├── StatCardTrend.jsx             # Type 2: With Trend
├── StatCardProgress.jsx          # Type 3: Progress/Gauge
├── StatCardSparkline.jsx         # Type 4: Sparkline Chart
├── StatCardThreshold.jsx         # Type 5: Threshold Alert
├── StatCardCompact.jsx           # Type 6: Compact Inline
├── StatCardMulti.jsx             # Type 7: Multi-Metric
├── StatCardTimeline.jsx          # Type 8: Status Timeline
├── StatCardDonut.jsx             # Type 9: Donut Chart
├── StatCardHero.jsx              # Type 10: KPI Hero
├── StatCardWorkstation.jsx       # Type 11: Workstation Status
├── StatCardScenario.jsx          # Type 12: Scenario Breakdown
├── StatisticsGrid.jsx            # Grid layout wrapper
└── statistics.module.css         # Shared statistics styles
```

---

## Migration Guide

### From Old StatCard to New System
1. **Basic metrics**: Continue using existing `StatCard` component
2. **With trends**: Replace manual trend display with `StatCardTrend`
3. **Progress bars**: Use `StatCardProgress` instead of custom implementations
4. **Stock alerts**: Replace conditional styling with `StatCardThreshold`

### Adding New Dashboard Statistics
1. Identify metrics needed for the dashboard role
2. Choose appropriate card types from this reference
3. Use `StatisticsGrid` for responsive layouts
4. Follow naming conventions: `stats` array with `value`, `label`, `variant`, `icon`

---

## Version History

| Date | Change |
|------|--------|
| Feb 5, 2026 | Initial design system documentation with 12 card types |
| Feb 5, 2026 | Created StatisticsCard.mockup.html with full visual reference |
| Feb 5, 2026 | Defined use cases by dashboard type |
