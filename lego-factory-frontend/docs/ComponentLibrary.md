# Component Library - Design System Implementation

This document provides a comprehensive guide to all standardized components in the LIFE application frontend. All components follow the design system defined in [DesignSystem.md](./DesignSystem.md) and use CSS Modules for styling.

## Table of Contents

1. [Button](#button)
2. [Card](#card)
3. [StatCard](#statcard)
4. [Table](#table)
5. [Input](#input)
6. [Select](#select)
7. [Tabs](#tabs)
8. [LoadingSpinner](#loadingspinner)
9. [Alert](#alert)
10. [Badge](#badge)
11. [Usage Guidelines](#usage-guidelines)
12. [Migration Guide](#migration-guide)

---

## Button

A versatile button component with multiple variants, sizes, and states.

### Import
```jsx
import Button from '../components/ButtonNew';
// Note: Will replace Button.jsx after migration
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `children` | `node` | *required* | Button content |
| `variant` | `'primary' \| 'secondary' \| 'success' \| 'danger' \| 'warning' \| 'outline' \| 'ghost' \| 'link'` | `'primary'` | Visual style |
| `size` | `'small' \| 'medium' \| 'large'` | `'medium'` | Button size |
| `disabled` | `boolean` | `false` | Disabled state |
| `loading` | `boolean` | `false` | Loading state |
| `fullWidth` | `boolean` | `false` | Full width |
| `icon` | `node` | `null` | Optional icon |
| `onClick` | `function` | - | Click handler |
| `type` | `'button' \| 'submit' \| 'reset'` | `'button'` | HTML type |

### Examples

```jsx
// Primary button
<Button variant="primary" onClick={handleSubmit}>
  Submit Order
</Button>

// Button with icon
<Button variant="success" icon={<CheckIcon />}>
  Confirm
</Button>

// Loading button
<Button loading={isProcessing}>
  Processing...
</Button>

// Danger outline button
<Button variant="danger" size="small">
  Delete
</Button>
```

---

## Card

A flexible container component for grouping related content.

### Import
```jsx
import Card from '../components/Card';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `children` | `node` | *required* | Card content |
| `variant` | `'default' \| 'elevated' \| 'outlined'` | `'default'` | Visual style |
| `header` | `node` | `null` | Header content |
| `footer` | `node` | `null` | Footer content |
| `interactive` | `boolean` | `false` | Enable hover effects |
| `onClick` | `function` | - | Click handler |
| `padding` | `'none' \| 'small' \| 'normal' \| 'large'` | `'normal'` | Padding size |

### Examples

```jsx
// Basic card
<Card>
  <h3>Order #12345</h3>
  <p>Status: Pending</p>
</Card>

// Card with header and footer
<Card 
  header={<h3>Customer Details</h3>}
  footer={
    <Button variant="primary">View More</Button>
  }
>
  <p>Customer information goes here</p>
</Card>

// Interactive card
<Card 
  variant="elevated" 
  interactive 
  onClick={() => navigate('/order/123')}
>
  <p>Click to view order details</p>
</Card>
```

---

## StatCard

Displays key metrics with optional icons, trends, and threshold alerts.

### Import
```jsx
import StatCard from '../components/StatCardNew';
// Note: Will replace StatCard.jsx after migration
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `value` | `number \| string` | *required* | Metric value |
| `label` | `string` | *required* | Metric label |
| `variant` | `'primary' \| 'secondary' \| 'success' \| 'warning' \| 'danger' \| 'info'` | `'primary'` | Color variant |
| `icon` | `node` | `null` | Icon or emoji |
| `trend` | `object` | `null` | `{ value: number, direction: 'up'\|'down'\|'neutral' }` |
| `threshold` | `number` | `null` | Alert threshold |
| `thresholdType` | `'low' \| 'high'` | `'high'` | Threshold comparison |
| `onClick` | `function` | - | Click handler |
| `compact` | `boolean` | `false` | Compact layout |

### Examples

```jsx
// Basic stat card
<StatCard
  value={142}
  label="Total Orders"
  variant="primary"
  icon="📦"
/>

// With trend indicator
<StatCard
  value="$12,450"
  label="Revenue"
  icon="💰"
  trend={{ value: 12.5, direction: 'up' }}
  variant="success"
/>

// Low stock alert
<StatCard
  value={5}
  label="Low Stock Items"
  variant="danger"
  threshold={10}
  thresholdType="low"
/>

// Clickable stat card
<StatCard
  value={23}
  label="Pending Orders"
  onClick={() => navigate('/orders?status=pending')}
/>
```

---

## Table

A feature-rich table component with sorting, filtering, and custom rendering.

### Import
```jsx
import Table from '../components/Table';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `columns` | `array` | *required* | Column definitions |
| `data` | `array` | *required* | Data rows |
| `variant` | `'default' \| 'bordered' \| 'minimal'` | `'default'` | Visual style |
| `striped` | `boolean` | `false` | Alternate row colors |
| `hoverable` | `boolean` | `true` | Hover effects |
| `compact` | `boolean` | `false` | Compact spacing |
| `sortable` | `boolean` | `true` | Enable sorting |
| `onRowClick` | `function` | - | Row click handler |
| `emptyMessage` | `string` | `'No data available'` | Empty state message |

### Column Definition

```typescript
{
  key: string;          // Data property key
  label: string;        // Column header
  sortable?: boolean;   // Enable sorting
  width?: string;       // Column width (CSS)
  render?: (row) => JSX; // Custom renderer
}
```

### Examples

```jsx
const columns = [
  { key: 'id', label: 'Order ID', sortable: true },
  { key: 'customer', label: 'Customer', sortable: true },
  { key: 'status', label: 'Status', render: (row) => (
    <Badge variant={getStatusVariant(row.status)}>
      {row.status}
    </Badge>
  )},
  { key: 'total', label: 'Total', sortable: true }
];

<Table
  columns={columns}
  data={orders}
  striped
  onRowClick={(row) => viewOrder(row.id)}
/>
```

---

## Input

A flexible text input component with validation support.

### Import
```jsx
import Input from '../components/Input';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `label` | `string` | - | Input label |
| `type` | `string` | `'text'` | Input type (text, email, password, etc.) |
| `value` | `string \| number` | - | Input value |
| `onChange` | `function` | - | Change handler |
| `error` | `string` | - | Error message |
| `helperText` | `string` | - | Helper text |
| `disabled` | `boolean` | `false` | Disabled state |
| `required` | `boolean` | `false` | Required field |
| `placeholder` | `string` | - | Placeholder text |
| `prefix` | `node` | - | Prefix content |
| `suffix` | `node` | - | Suffix content |
| `fullWidth` | `boolean` | `false` | Full width |

### Examples

```jsx
// Basic input
<Input
  label="Email"
  type="email"
  value={email}
  onChange={(e) => setEmail(e.target.value)}
  placeholder="Enter your email"
/>

// With error
<Input
  label="Password"
  type="password"
  value={password}
  onChange={(e) => setPassword(e.target.value)}
  error="Password must be at least 8 characters"
  required
/>

// With prefix/suffix
<Input
  label="Amount"
  type="number"
  prefix="$"
  suffix="USD"
  helperText="Enter amount in dollars"
/>

// With React Hook Form
<Input
  label="Username"
  {...register('username', { required: true })}
  error={errors.username?.message}
/>
```

---

## Select

A styled select dropdown component.

### Import
```jsx
import Select from '../components/Select';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `label` | `string` | - | Select label |
| `options` | `array` | *required* | Options array |
| `value` | `string \| number` | - | Selected value |
| `onChange` | `function` | - | Change handler |
| `placeholder` | `string` | - | Placeholder option |
| `error` | `string` | - | Error message |
| `helperText` | `string` | - | Helper text |
| `disabled` | `boolean` | `false` | Disabled state |
| `required` | `boolean` | `false` | Required field |
| `fullWidth` | `boolean` | `false` | Full width |

### Examples

```jsx
const statusOptions = [
  { value: 'pending', label: 'Pending' },
  { value: 'processing', label: 'Processing' },
  { value: 'completed', label: 'Completed' }
];

<Select
  label="Order Status"
  options={statusOptions}
  value={status}
  onChange={(e) => setStatus(e.target.value)}
  placeholder="Select status..."
/>

// With error
<Select
  label="Country"
  options={countries}
  error="Please select a country"
  required
/>
```

---

## Tabs

A tabbed interface component with keyboard navigation.

### Import
```jsx
import Tabs from '../components/Tabs';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `tabs` | `array` | *required* | Tab definitions |
| `activeTab` | `string` | - | Active tab ID (controlled) |
| `onTabChange` | `function` | - | Tab change handler |
| `variant` | `'default' \| 'pills' \| 'underline'` | `'default'` | Visual style |
| `fullWidth` | `boolean` | `false` | Full width tabs |

### Tab Definition

```typescript
{
  id: string;           // Unique tab ID
  label: string;        // Tab label
  content: JSX;         // Tab content
  icon?: JSX;           // Optional icon
  badge?: string|number;// Optional badge
  disabled?: boolean;   // Disabled state
}
```

### Examples

```jsx
const tabs = [
  {
    id: 'orders',
    label: 'Orders',
    icon: '📦',
    badge: 5,
    content: <OrdersView />
  },
  {
    id: 'products',
    label: 'Products',
    content: <ProductsView />
  },
  {
    id: 'inventory',
    label: 'Inventory',
    content: <InventoryView />
  }
];

// Uncontrolled
<Tabs tabs={tabs} />

// Controlled
<Tabs
  tabs={tabs}
  activeTab={activeTab}
  onTabChange={setActiveTab}
  variant="pills"
/>
```

---

## LoadingSpinner

A loading indicator with overlay support.

### Import
```jsx
import LoadingSpinner from '../components/LoadingSpinner';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `size` | `'small' \| 'medium' \| 'large'` | `'medium'` | Spinner size |
| `variant` | `'primary' \| 'secondary' \| 'white'` | `'primary'` | Color variant |
| `text` | `string` | - | Loading text |
| `overlay` | `boolean` | `false` | Show as overlay |

### Examples

```jsx
// Inline spinner
<LoadingSpinner />

// With text
<LoadingSpinner size="large" text="Loading data..." />

// Full page overlay
<LoadingSpinner 
  overlay 
  text="Processing your request..." 
/>

// Conditional rendering
{isLoading && <LoadingSpinner overlay />}
```

---

## Alert

Contextual feedback messages with auto-dismiss.

### Import
```jsx
import Alert from '../components/Alert';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `children` | `node` | *required* | Alert message |
| `variant` | `'success' \| 'danger' \| 'warning' \| 'info'` | `'info'` | Alert type |
| `title` | `string` | - | Alert title |
| `onClose` | `function` | - | Close handler |
| `autoDismiss` | `number` | - | Auto-dismiss in ms |
| `onDismiss` | `function` | - | Dismiss callback |
| `icon` | `node` | - | Custom icon |

### Examples

```jsx
// Success alert
<Alert variant="success" title="Success!">
  Your order has been created successfully.
</Alert>

// Error with close
<Alert 
  variant="danger" 
  onClose={() => setError(null)}
>
  An error occurred while processing your request.
</Alert>

// Auto-dismiss
<Alert 
  variant="info" 
  autoDismiss={5000}
  onDismiss={handleDismiss}
>
  This message will disappear in 5 seconds.
</Alert>

// Warning
<Alert variant="warning" title="Low Stock">
  Some items are running low on inventory.
</Alert>
```

---

## Badge

Small labels for status, counts, or categories.

### Import
```jsx
import Badge from '../components/Badge';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `children` | `node` | - | Badge content |
| `variant` | `'primary' \| 'secondary' \| 'success' \| 'danger' \| 'warning' \| 'info' \| 'gray'` | `'primary'` | Color variant |
| `size` | `'small' \| 'medium' \| 'large'` | `'medium'` | Badge size |
| `pill` | `boolean` | `false` | Pill shape |
| `dot` | `boolean` | `false` | Dot indicator |

### Examples

```jsx
// Status badge
<Badge variant="success">Active</Badge>
<Badge variant="danger">Inactive</Badge>

// Count badge
<Badge variant="primary" pill>5</Badge>

// Dot indicator
<Badge variant="danger" dot />

// In combination
<div>
  Notifications
  <Badge variant="danger" size="small">12</Badge>
</div>
```

---

## Usage Guidelines

### Component Selection

- **Button**: Use for all clickable actions (submit, cancel, navigate)
- **Card**: Use for grouping related content (orders, products, user info)
- **StatCard**: Use for dashboard metrics and KPIs
- **Table**: Use for displaying tabular data with sorting needs
- **Input/Select**: Use for all form inputs
- **Tabs**: Use when content has multiple views/sections
- **LoadingSpinner**: Use during async operations
- **Alert**: Use for user feedback (success, error, warning, info)
- **Badge**: Use for status labels, counts, or tags

### Best Practices

1. **Consistency**: Always use design system components instead of custom styles
2. **Accessibility**: All components include ARIA attributes and keyboard navigation
3. **Responsive**: Components adapt to mobile screens automatically
4. **Props**: Use appropriate variants and sizes for context
5. **Composition**: Combine components (e.g., Badge in Table cells, Button in Card footer)

### CSS Modules

All components use CSS Modules for scoped styling:

```jsx
// Import styles
import styles from './Component.module.css';

// Use in JSX
<div className={styles.container}>
  <span className={styles.text}>Hello</span>
</div>

// Combine classes
<div className={`${styles.card} ${styles.active}`}>
```

---

## Migration Guide

### Step 1: Update Imports

Replace old imports with new design system components:

```jsx
// Old
import Button from '../components/Button';
import StatCard from '../components/StatCard';

// New
import Button from '../components/ButtonNew';
import StatCard from '../components/StatCardNew';
```

### Step 2: Update Component Usage

**Button Migration:**
```jsx
// Old
<button className="btn btn-primary" onClick={handleClick}>
  Submit
</button>

// New
<Button variant="primary" onClick={handleClick}>
  Submit
</Button>
```

**StatCard Migration:**
```jsx
// Old
<StatCard 
  title="Total Orders" 
  value={142} 
  icon="📦" 
  color="primary"
/>

// New
<StatCard 
  label="Total Orders" 
  value={142} 
  icon="📦" 
  variant="primary"
/>
```

### Step 3: Update Styling

Replace custom CSS with design system utilities:

```jsx
// Old
<div style={{ display: 'flex', gap: '16px', marginTop: '20px' }}>
  <button className="custom-button">Action</button>
</div>

// New
<div className="flex gap-4 mt-5">
  <Button>Action</Button>
</div>
```

### Step 4: Test Components

After migration, test:
- Visual appearance matches design
- Interactive states (hover, focus, active)
- Accessibility (keyboard navigation, screen readers)
- Responsive behavior on mobile

---

## Component Checklist

Use this checklist when creating new pages:

- [ ] Import design system components
- [ ] Use CSS variables from `variables.css`
- [ ] Apply utility classes where appropriate
- [ ] Include loading states (`LoadingSpinner`)
- [ ] Add error handling (`Alert` component)
- [ ] Test keyboard navigation
- [ ] Verify mobile responsiveness
- [ ] Check accessibility (WCAG 2.1 AA)

---

## Support

For questions or issues with components:

1. Check this documentation
2. Review [DesignSystem.md](./DesignSystem.md) for design tokens
3. Inspect component source code in `/src/components/`
4. Test component in isolation before using in pages

---

**Last Updated**: January 2026  
**Version**: 1.0.0  
**Components**: 10 standardized components
