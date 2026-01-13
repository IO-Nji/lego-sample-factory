# LIFE Application - Design System Documentation

**Version:** 1.0  
**Last Updated:** January 13, 2026  
**Maintainer:** Development Team

---

## Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Color Palette](#color-palette)
4. [Typography](#typography)
5. [Spacing System](#spacing-system)
6. [Components](#components)
7. [Layouts](#layouts)
8. [Best Practices](#best-practices)

---

## Introduction

This design system provides a comprehensive set of design tokens, components, and guidelines to ensure consistency across the LIFE application. It promotes reusability, maintainability, and a cohesive user experience.

### Design Principles

1. **Consistency** - Unified visual language across all interfaces
2. **Accessibility** - WCAG 2.1 AA compliant components
3. **Efficiency** - Reusable components reduce development time
4. **Scalability** - Flexible system that grows with the application

### File Structure

```
src/styles/
├── variables.css      # All design tokens (colors, spacing, typography)
├── utilities.css      # Utility classes for common patterns
├── Button.css         # Component-specific styles
└── ...
```

---

## Getting Started

### Importing Design System

In your component or page CSS file:

```css
@import '../styles/variables.css';
@import '../styles/utilities.css';
```

### Using CSS Variables

```css
.my-component {
  color: var(--color-primary);
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
}
```

### Using Utility Classes

```jsx
<div className="d-flex justify-between align-center gap-4 p-4">
  <h2 className="text-xl font-semibold text-primary">Title</h2>
  <button className="btn btn-primary">Action</button>
</div>
```

---

## Color Palette

### Primary Colors

Used for primary actions, links, and brand elements.

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-primary` | #2c5aa0 | Primary buttons, active states |
| `--color-primary-dark` | #1e3d66 | Hover states, emphasis |
| `--color-primary-light` | #4a7bc8 | Backgrounds, disabled states |
| `--color-primary-lightest` | #eff6ff | Light backgrounds |

**Example:**
```jsx
<button className="btn btn-primary">Primary Action</button>
```

### Secondary Colors

Used for secondary actions and neutral elements.

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-secondary` | #6c757d | Secondary buttons, text |
| `--color-secondary-dark` | #5a6268 | Hover states |
| `--color-secondary-light` | #868e96 | Borders, dividers |

### Status Colors

#### Success (Green)

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-success` | #28a745 | Success messages, completed states |
| `--color-success-dark` | #0f5132 | Emphasis |
| `--color-success-light` | #d1e7dd | Background |
| `--color-success-lightest` | #f0fdf4 | Alert backgrounds |

#### Warning (Yellow/Orange)

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-warning` | #ffc107 | Warning messages, pending states |
| `--color-warning-dark` | #856404 | Emphasis |
| `--color-warning-light` | #fff3cd | Background |

#### Danger (Red)

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-danger` | #dc3545 | Error messages, destructive actions |
| `--color-danger-dark` | #842029 | Emphasis |
| `--color-danger-light` | #f8d7da | Background |

#### Info (Blue)

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-info` | #0dcaf0 | Info messages, tooltips |
| `--color-info-dark` | #084298 | Emphasis |
| `--color-info-light` | #cfe2ff | Background |

### Order Status Colors

Specific colors for order statuses in the manufacturing workflow.

| Token | Hex | Status |
|-------|-----|--------|
| `--color-status-pending` | #ffc107 | PENDING |
| `--color-status-in-progress` | #2196f3 | IN_PROGRESS |
| `--color-status-completed` | #4caf50 | COMPLETED |
| `--color-status-cancelled` | #f44336 | CANCELLED |
| `--color-status-halted` | #ff9800 | HALTED |

**Example:**
```jsx
<span className="badge" style={{ backgroundColor: 'var(--color-status-pending)' }}>
  Pending
</span>
```

### Grayscale

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-gray-900` | #212121 | Primary text |
| `--color-gray-700` | #616161 | Secondary text |
| `--color-gray-500` | #9e9e9e | Muted text |
| `--color-gray-300` | #e0e0e0 | Borders |
| `--color-gray-100` | #f5f5f5 | Backgrounds |
| `--color-white` | #ffffff | Surface |

### Text Colors

| Token | Usage |
|-------|-------|
| `--color-text-primary` | Body text, headings |
| `--color-text-secondary` | Secondary text, labels |
| `--color-text-muted` | Disabled text, placeholders |

**Utility Classes:**
```html
<p class="text-primary">Primary text</p>
<p class="text-secondary">Secondary text</p>
<p class="text-muted">Muted text</p>
```

---

## Typography

### Font Family

- **Base:** System font stack (optimized for each OS)
- **Monospace:** For code snippets and technical data

```css
--font-family-base: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', ...
--font-family-monospace: 'Courier New', Courier, monospace;
```

### Font Sizes

| Token | Size | Usage |
|-------|------|-------|
| `--font-size-xs` | 0.75rem (12px) | Captions, meta text |
| `--font-size-sm` | 0.875rem (14px) | Small text, labels |
| `--font-size-base` | 1rem (16px) | Body text (default) |
| `--font-size-md` | 1.125rem (18px) | Large body text |
| `--font-size-lg` | 1.25rem (20px) | Subheadings |
| `--font-size-xl` | 1.5rem (24px) | H3 headings |
| `--font-size-2xl` | 1.875rem (30px) | H2 headings |
| `--font-size-3xl` | 2.25rem (36px) | H1 headings |
| `--font-size-4xl` | 3rem (48px) | Hero text |

**Utility Classes:**
```html
<h1 class="text-3xl font-bold">Main Heading</h1>
<p class="text-base">Body paragraph</p>
<small class="text-xs text-muted">Small caption</small>
```

### Font Weights

| Token | Value | Usage |
|-------|-------|-------|
| `--font-weight-light` | 300 | Light emphasis |
| `--font-weight-normal` | 400 | Body text |
| `--font-weight-medium` | 500 | Medium emphasis |
| `--font-weight-semibold` | 600 | Subheadings, buttons |
| `--font-weight-bold` | 700 | Headings, strong emphasis |

**Utility Classes:**
```html
<span class="font-normal">Normal text</span>
<span class="font-semibold">Semibold text</span>
<span class="font-bold">Bold text</span>
```

### Line Height

| Token | Value | Usage |
|-------|-------|-------|
| `--line-height-tight` | 1.25 | Headings |
| `--line-height-normal` | 1.5 | Body text |
| `--line-height-relaxed` | 1.75 | Long-form content |

### Typography Scale Example

```jsx
<div>
  <h1 style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 'var(--font-weight-bold)' }}>
    Main Page Title
  </h1>
  <h2 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 'var(--font-weight-semibold)' }}>
    Section Heading
  </h2>
  <p style={{ fontSize: 'var(--font-size-base)', lineHeight: 'var(--line-height-normal)' }}>
    Body paragraph with normal line height for readability.
  </p>
</div>
```

---

## Spacing System

Based on a **4px (0.25rem) grid** for consistent spacing throughout the application.

### Spacing Scale

| Token | Size | Pixels | Usage |
|-------|------|--------|-------|
| `--spacing-0` | 0 | 0px | No spacing |
| `--spacing-1` | 0.25rem | 4px | Tiny gaps |
| `--spacing-2` | 0.5rem | 8px | Small padding, icon spacing |
| `--spacing-3` | 0.75rem | 12px | Compact padding |
| `--spacing-4` | 1rem | 16px | Default padding, margins |
| `--spacing-5` | 1.25rem | 20px | Medium spacing |
| `--spacing-6` | 1.5rem | 24px | Large spacing |
| `--spacing-8` | 2rem | 32px | Extra large spacing |
| `--spacing-12` | 3rem | 48px | Section spacing |
| `--spacing-16` | 4rem | 64px | Page-level spacing |

### Semantic Spacing (Aliases)

| Token | Equivalent | Usage |
|-------|------------|-------|
| `--spacing-xs` | `--spacing-1` | 4px |
| `--spacing-sm` | `--spacing-2` | 8px |
| `--spacing-md` | `--spacing-4` | 16px |
| `--spacing-lg` | `--spacing-6` | 24px |
| `--spacing-xl` | `--spacing-8` | 32px |

### Utility Classes

**Margin:**
```html
<div class="m-4">Margin all sides (16px)</div>
<div class="mt-6">Margin top (24px)</div>
<div class="mx-auto">Margin horizontal auto (centering)</div>
```

**Padding:**
```html
<div class="p-4">Padding all sides (16px)</div>
<div class="px-6">Padding horizontal (24px)</div>
<div class="py-2">Padding vertical (8px)</div>
```

**Gap (for Flexbox/Grid):**
```html
<div class="d-flex gap-4">Items with 16px gap</div>
<div class="d-grid grid-cols-3 gap-6">Grid with 24px gap</div>
```

---

## Components

### Buttons

#### Variants

1. **Primary** - Main call-to-action
2. **Secondary** - Secondary actions
3. **Danger** - Destructive actions
4. **Outline** - Less prominent actions
5. **Ghost** - Minimal style, text-like

#### Sizes

- **Small** - `btn-sm` - Compact buttons
- **Medium** - `btn` (default) - Standard buttons
- **Large** - `btn-lg` - Prominent buttons

#### States

- **Default** - Normal state
- **Hover** - Mouse hover
- **Active** - Click/press state
- **Disabled** - Disabled state
- **Loading** - Processing state

#### Example Usage

```jsx
import Button from '../components/Button';

// Primary button
<Button variant="primary" size="md" onClick={handleClick}>
  Save Changes
</Button>

// Secondary button
<Button variant="secondary" onClick={handleCancel}>
  Cancel
</Button>

// Danger button
<Button variant="danger" onClick={handleDelete}>
  Delete Order
</Button>

// Disabled button
<Button variant="primary" disabled>
  Cannot Submit
</Button>

// Loading button
<Button variant="primary" loading>
  Processing...
</Button>
```

#### CSS Example

```css
.btn {
  padding: var(--spacing-2) var(--spacing-4);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
  transition: var(--transition-base);
  cursor: pointer;
}

.btn-primary {
  background-color: var(--color-primary);
  color: var(--color-white);
  border: 2px solid var(--color-primary);
}

.btn-primary:hover:not(:disabled) {
  background-color: var(--color-primary-dark);
  box-shadow: var(--shadow-md);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

---

### Cards

Cards are containers for grouped content.

#### Variants

1. **Default** - White background, subtle border
2. **Elevated** - With shadow for emphasis
3. **Bordered** - Prominent border

#### Card Structure

```jsx
<div className="card">
  <div className="card-header">
    <h3 className="card-title">Card Title</h3>
    <button className="btn btn-sm">Action</button>
  </div>
  <div className="card-body">
    <p>Card content goes here...</p>
  </div>
  <div className="card-footer">
    <button className="btn btn-secondary btn-sm">Cancel</button>
    <button className="btn btn-primary btn-sm">Confirm</button>
  </div>
</div>
```

#### CSS Example

```css
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border-secondary);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.card-elevated {
  box-shadow: var(--shadow-md);
  border: none;
}

.card-header {
  padding: var(--spacing-4);
  border-bottom: 1px solid var(--color-border-secondary);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-body {
  padding: var(--spacing-4);
}

.card-footer {
  padding: var(--spacing-4);
  border-top: 1px solid var(--color-border-secondary);
  display: flex;
  gap: var(--spacing-2);
  justify-content: flex-end;
}
```

---

### Tables

#### Variants

1. **Default** - Basic table
2. **Striped** - Alternating row colors
3. **Bordered** - With borders
4. **Compact** - Reduced padding

#### Example Usage

```jsx
<table className="table table-striped">
  <thead>
    <tr>
      <th>Order ID</th>
      <th>Product</th>
      <th>Quantity</th>
      <th>Status</th>
      <th>Actions</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>#12345</td>
      <td>Technic Truck Yellow</td>
      <td>10</td>
      <td><span className="badge badge-success">Completed</span></td>
      <td>
        <button className="btn btn-sm btn-primary">View</button>
      </td>
    </tr>
  </tbody>
</table>
```

#### CSS Example

```css
.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  padding: var(--spacing-3) var(--spacing-4);
  text-align: left;
  border-bottom: 1px solid var(--color-border-secondary);
}

.table thead th {
  background-color: var(--color-gray-100);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.table-striped tbody tr:nth-child(even) {
  background-color: var(--color-gray-50);
}

.table tbody tr:hover {
  background-color: var(--color-background-hover);
}
```

---

### Forms

#### Form Elements

1. **Text Input**
2. **Select Dropdown**
3. **Checkbox**
4. **Radio Button**
5. **Textarea**

#### Example Usage

```jsx
<form className="form">
  <div className="form-group">
    <label htmlFor="productName" className="form-label">
      Product Name
    </label>
    <input 
      type="text" 
      id="productName" 
      className="form-input" 
      placeholder="Enter product name"
    />
    <small className="form-help">Enter the full product variant name</small>
  </div>

  <div className="form-group">
    <label htmlFor="quantity" className="form-label">
      Quantity
    </label>
    <input 
      type="number" 
      id="quantity" 
      className="form-input" 
      min="1"
    />
  </div>

  <div className="form-group">
    <label htmlFor="workstation" className="form-label">
      Workstation
    </label>
    <select id="workstation" className="form-select">
      <option value="">Select workstation</option>
      <option value="1">Plant Warehouse</option>
      <option value="2">Modules Supermarket</option>
    </select>
  </div>

  <div className="form-group">
    <label className="form-checkbox">
      <input type="checkbox" />
      <span>Mark as urgent</span>
    </label>
  </div>

  <div className="form-actions">
    <button type="button" className="btn btn-secondary">Cancel</button>
    <button type="submit" className="btn btn-primary">Create Order</button>
  </div>
</form>
```

#### CSS Example

```css
.form-group {
  margin-bottom: var(--spacing-4);
}

.form-label {
  display: block;
  margin-bottom: var(--spacing-2);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.form-input,
.form-select {
  width: 100%;
  padding: var(--spacing-2) var(--spacing-3);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  transition: var(--transition-base);
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: var(--input-focus-ring);
}

.form-help {
  display: block;
  margin-top: var(--spacing-1);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.form-actions {
  display: flex;
  gap: var(--spacing-2);
  justify-content: flex-end;
  margin-top: var(--spacing-6);
}
```

---

### Badges & Tags

Small labels for status, categories, or counts.

#### Example Usage

```jsx
<span className="badge badge-primary">New</span>
<span className="badge badge-success">Active</span>
<span className="badge badge-warning">Pending</span>
<span className="badge badge-danger">Error</span>
```

#### CSS Example

```css
.badge {
  display: inline-flex;
  align-items: center;
  padding: var(--spacing-1) var(--spacing-2);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-base);
  text-transform: uppercase;
  letter-spacing: var(--letter-spacing-wide);
}

.badge-primary {
  background-color: var(--color-primary-lightest);
  color: var(--color-primary-dark);
}

.badge-success {
  background-color: var(--color-success-lightest);
  color: var(--color-success-dark);
}
```

---

### Loading Spinner

#### Example Usage

```jsx
<div className="spinner">
  <div className="spinner-circle"></div>
</div>
```

#### CSS Example

```css
.spinner {
  display: inline-block;
  width: 24px;
  height: 24px;
}

.spinner-circle {
  width: 100%;
  height: 100%;
  border: 3px solid var(--color-gray-300);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
```

---

## Layouts

### Page Container

Standard page wrapper with max-width and padding.

```jsx
<div className="page-container">
  <PageHeader title="Dashboard" subtitle="Overview of operations" />
  <div className="page-content">
    {/* Page content */}
  </div>
</div>
```

```css
.page-container {
  max-width: var(--page-max-width);
  margin: 0 auto;
  padding: var(--page-padding);
}

@media (max-width: 768px) {
  .page-container {
    padding: var(--page-padding-mobile);
  }
}
```

### Grid Layouts

**Two-column:**
```html
<div class="d-grid grid-cols-2 gap-4">
  <div>Column 1</div>
  <div>Column 2</div>
</div>
```

**Three-column:**
```html
<div class="d-grid grid-cols-3 gap-6">
  <div>Column 1</div>
  <div>Column 2</div>
  <div>Column 3</div>
</div>
```

**Responsive Grid:**
```html
<div class="d-grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
  <div>Item 1</div>
  <div>Item 2</div>
  <div>Item 3</div>
</div>
```

---

## Best Practices

### 1. Always Use Design Tokens

❌ **Don't:**
```css
.my-component {
  color: #2c5aa0;
  padding: 16px;
  border-radius: 6px;
}
```

✅ **Do:**
```css
.my-component {
  color: var(--color-primary);
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
}
```

### 2. Prefer Utility Classes for Simple Styling

❌ **Don't:**
```css
.centered-flex {
  display: flex;
  justify-content: center;
  align-items: center;
}
```

✅ **Do:**
```jsx
<div className="d-flex justify-center align-center">
  {/* content */}
</div>
```

### 3. Semantic Color Usage

- Use `--color-primary` for primary actions
- Use `--color-success`, `--color-warning`, `--color-danger` for status
- Use `--color-text-*` for text colors
- Avoid using raw hex colors

### 4. Consistent Spacing

- Stick to the 4px grid (`--spacing-1` through `--spacing-16`)
- Use semantic spacing tokens (`xs`, `sm`, `md`, `lg`, `xl`) for common patterns

### 5. Accessibility

- Ensure contrast ratios meet WCAG AA standards
- Provide focus states for interactive elements
- Use semantic HTML
- Include ARIA labels where appropriate

### 6. Responsive Design

- Design mobile-first
- Use responsive utility classes (`sm:`, `md:`, `lg:`)
- Test on multiple screen sizes

### 7. Performance

- Avoid inline styles when possible
- Reuse existing components
- Minimize custom CSS

---

## Migration Guide

### Updating Existing Components

1. **Import design system:**
   ```css
   @import '../styles/variables.css';
   @import '../styles/utilities.css';
   ```

2. **Replace hard-coded values:**
   - Colors: `#2c5aa0` → `var(--color-primary)`
   - Spacing: `16px` → `var(--spacing-4)`
   - Border radius: `6px` → `var(--radius-md)`

3. **Use utility classes where appropriate:**
   ```jsx
   // Before
   <div style={{ display: 'flex', gap: '16px' }}>

   // After
   <div className="d-flex gap-4">
   ```

4. **Test thoroughly** - Ensure visual consistency and no regressions

---

## Support & Contribution

### Questions or Issues?

- Check existing components in `src/components/`
- Review this documentation
- Ask the development team

### Contributing

1. Follow the established patterns
2. Document new components
3. Test across browsers
4. Submit for code review

---

**End of Design System Documentation**

*For implementation details, see `/src/styles/variables.css` and `/src/styles/utilities.css`*
