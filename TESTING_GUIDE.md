# Testing Guide - Overview Pages with Design System Components

**Date**: January 13, 2026  
**Purpose**: Test new overview pages and design system components before migrating existing pages

---

## Prerequisites

1. **Docker**: Docker Desktop or Docker Engine running
2. **Ports Available**: 80, 8011, 8012-8016
3. **Environment**: `.env` file configured (copy from `.env.example`)

---

## Quick Start

### 1. Start the Application

```bash
# From repository root
cd /home/nji/Documents/DEV/Java/LIFE/lego-sample-factory

# Start all services
docker-compose up -d

# Check all services are running
docker-compose ps

# Watch logs (optional)
docker-compose logs -f
```

**Expected Output**:
```
✅ nginx-root-proxy    - healthy
✅ api-gateway         - healthy  
✅ user-service        - healthy
✅ masterdata-service  - healthy
✅ inventory-service   - healthy
✅ order-processing-service - healthy
✅ simal-integration-service - healthy
✅ frontend            - healthy
```

### 2. Access Application

Open browser: **http://localhost**

---

## Test User Accounts

### Admin User
```
Username: admin
Password: admin123
```
**Access**: All overview pages

### Manager User
```
Username: manager
Password: manager123
```
**Access**: Dashboard, Manager Overview, others based on role

### Warehouse User
```
Username: warehouse
Password: warehouse123
```
**Access**: Dashboard, Warehouse Overview

### Manufacturing User
```
Username: manufacturing
Password: manufacturing123
```
**Access**: Dashboard, Manufacturing Overview

---

## Testing Checklist

### Component Visual Testing

#### ✅ StatCard Component
Navigate to any overview page and verify:

- [ ] Cards display with correct icons
- [ ] Numbers are formatted correctly
- [ ] Color variants work (primary, secondary, success, warning, danger, info)
- [ ] Hover effect shows shadow and lift
- [ ] Clicking cards navigates (if clickable)
- [ ] Threshold alerts show red/orange colors
- [ ] Icons/emojis render properly

**Test Cases**:
1. **Admin Overview** → Check "Low Stock Items" card turns red when quantity ≤ threshold
2. **Manager Overview** → Check "Critical Alerts" card animates with pulse
3. Click any clickable StatCard → Should navigate to detail page

---

#### ✅ Table Component
Navigate to any overview page, click "Recent Orders" tab:

- [ ] Table renders with all columns
- [ ] Striped rows alternate colors
- [ ] Hover effect highlights row
- [ ] Click column header to sort (ascending/descending)
- [ ] Sort icon (↑/↓) appears in sorted column
- [ ] Badges in "Status" column show correct colors
- [ ] Action buttons work (View, Restock, etc.)
- [ ] Empty state message shows when no data
- [ ] Table is responsive on mobile (stackable)

**Test Cases**:
1. Click "Order #" header → Should sort by order number
2. Click again → Should reverse sort direction
3. Click "Status" badge → Verify color matches order status
4. Resize browser to mobile → Table should stack vertically

---

#### ✅ Tabs Component
Navigate to any overview page:

- [ ] All tabs are visible
- [ ] Badge counts display correctly
- [ ] Click tab switches content smoothly
- [ ] Active tab is highlighted
- [ ] Keyboard navigation works (Tab, Arrow keys)
- [ ] Content animates when switching
- [ ] Icons in tabs render correctly

**Test Cases**:
1. Click each tab → Content should change
2. Press Tab key to focus tabs → Press Arrow Right/Left → Should navigate tabs
3. Check badge numbers match data counts

---

#### ✅ Button Component
Available on all overview pages (Refresh button):

- [ ] Button displays with icon and text
- [ ] Hover effect changes color
- [ ] Click triggers action (refresh data)
- [ ] Loading state shows spinner (if implemented)
- [ ] Different variants visible (primary, secondary, outline, etc.)
- [ ] Sizes are correct (small, medium, large)
- [ ] Focus indicator visible when tabbed

**Test Cases**:
1. Click "Refresh" button → Data should reload
2. Tab to button → Press Enter → Should activate
3. Check all button variants across pages

---

#### ✅ Card Component
Wrapper for tables and charts:

- [ ] Card has proper shadow
- [ ] Header displays if present
- [ ] Footer displays if present
- [ ] Padding is consistent
- [ ] Interactive cards show hover effect

---

#### ✅ Alert Component
Trigger by disconnecting network or causing error:

- [ ] Alert appears with correct variant color
- [ ] Icon displays (✓, ✕, ⚠, ℹ)
- [ ] Close button (X) works
- [ ] Auto-dismiss works (if configured)
- [ ] Multiple alerts stack properly
- [ ] Animation on entrance

**Test Cases**:
1. Disconnect network → Click Refresh → Error alert should appear
2. Click X button → Alert should disappear
3. Navigate to Manager Overview with critical items → Alert should show

---

#### ✅ Badge Component
Visible in tables and tabs:

- [ ] Badge displays text/number
- [ ] Color variants work (success, warning, danger, info)
- [ ] Sizes are appropriate
- [ ] Pill shape works
- [ ] Dot indicator works

**Test Cases**:
1. Check status badges in order tables
2. Check badge counts in tabs
3. Verify colors match order status

---

#### ✅ LoadingSpinner Component
Appears when page is loading:

- [ ] Spinner displays on page load
- [ ] Overlay dims background
- [ ] "Loading..." text shows
- [ ] Spinner animates smoothly
- [ ] Disappears when data loads

**Test Cases**:
1. Refresh page → Spinner should appear briefly
2. Navigate between overview pages → Spinner on each load

---

### Page-Specific Testing

#### Admin Overview (`/overview/admin`)

**Access**: Login as admin → Navigate to "Overview" menu → "Admin Overview"

**Test Checklist**:
- [ ] All 4 stat cards in first row display
- [ ] All 4 stat cards in second row display
- [ ] "Refresh Data" button works
- [ ] "Overview" tab shows statistics
- [ ] "Recent Orders" tab shows table with data
- [ ] "Low Stock Alerts" tab shows inventory table
- [ ] Tab badges show correct counts
- [ ] Low stock items highlighted in red/orange
- [ ] Status badges show correct colors
- [ ] All links navigate correctly

**Data to Check**:
- Total Customer Orders
- Total Production Orders
- Total Warehouse Orders
- Total Supply Orders
- Pending Orders
- Completed Orders
- Low Stock Items
- Total Inventory Items

---

#### Manager Overview (`/overview/manager`)

**Access**: Login as any user → Navigate to "Overview" menu → "Manager Overview"

**Test Checklist**:
- [ ] All 8 primary stat cards display
- [ ] 2 additional cards for stock alerts
- [ ] "Critical Alerts" alert shows if items ≤5 units
- [ ] Order status distribution chart visible
- [ ] Status counts are accurate
- [ ] "Recent Orders" shows combined order types
- [ ] Order type badges display (Customer, Production, etc.)
- [ ] "Critical Alerts" tab shows items ≤5 units with red pulse
- [ ] All 3 tabs functional

**Critical Alert Test**:
1. If no critical items, manually reduce inventory via API/DB
2. Refresh page
3. Critical alert banner should appear
4. Navigate to "Critical Alerts" tab
5. Items should pulse in red

---

#### Warehouse Overview (`/overview/warehouse`)

**Access**: Login as warehouse user → Navigate to "Overview" menu → "Warehouse Overview"

**Test Checklist**:
- [ ] 6 stat cards display (2 rows)
- [ ] Warehouse orders count correct
- [ ] Supply orders count correct
- [ ] Low stock items highlighted
- [ ] "Recent Orders" shows warehouse orders only
- [ ] "Low Stock" tab shows inventory items ≤10
- [ ] Warning alert appears if low stock exists

---

#### Manufacturing Overview (`/overview/manufacturing`)

**Access**: Login as manufacturing user → Navigate to "Overview" menu → "Manufacturing Overview"

**Test Checklist**:
- [ ] 6 stat cards display (2 rows)
- [ ] Production orders count correct
- [ ] Assembly orders count correct
- [ ] Pending vs completed counts accurate
- [ ] "Production Orders" tab shows production data
- [ ] "Assembly Orders" tab shows assembly data
- [ ] All badges show correct statuses

---

### Responsive Testing

#### Desktop (1400px+)
- [ ] 4-column stat card grid
- [ ] Full-width tables
- [ ] Horizontal header layout
- [ ] Navigation menu expanded

#### Tablet (768px - 1024px)
- [ ] 2-3 column stat card grid
- [ ] Condensed tables
- [ ] Stacked header
- [ ] Navigation collapsible

#### Mobile (<768px)
- [ ] Single-column layout
- [ ] Stat cards stack vertically
- [ ] Tables become stacked (vertical)
- [ ] Touch-friendly buttons (min 44px)
- [ ] Navigation hamburger menu

**Test Steps**:
1. Open Chrome DevTools (F12)
2. Toggle device toolbar (Ctrl+Shift+M)
3. Test each device size:
   - iPhone SE (375px)
   - iPad (768px)
   - Desktop (1920px)
4. Verify layouts adjust appropriately

---

### Accessibility Testing

#### Keyboard Navigation
- [ ] Tab key navigates through all interactive elements
- [ ] Enter/Space activates buttons
- [ ] Arrow keys navigate tabs
- [ ] Escape closes modals/alerts
- [ ] Focus indicators visible on all elements

**Test Steps**:
1. Click in address bar
2. Press Tab repeatedly
3. Verify focus moves to: Navigation → Tabs → Tables → Buttons
4. Press Enter on focused button → Should activate

#### Screen Reader Testing
- [ ] Headings are properly structured (H1 → H2 → H3)
- [ ] Alt text on images/icons
- [ ] ARIA labels on interactive elements
- [ ] Loading states announced
- [ ] Errors announced

**Test Steps** (with NVDA/JAWS):
1. Enable screen reader
2. Navigate through page
3. Verify all content is announced
4. Check loading states are announced

#### Color Contrast
- [ ] All text meets WCAG 2.1 AA standards
- [ ] Links distinguishable from text
- [ ] Focus indicators have 3:1 contrast
- [ ] Error messages readable

---

### Performance Testing

#### Load Time
- [ ] Initial page load < 2 seconds
- [ ] Data fetch < 1 second (with data)
- [ ] Component render < 100ms
- [ ] No layout shift during load

**Test Steps**:
1. Open Chrome DevTools → Network tab
2. Hard refresh (Ctrl+Shift+R)
3. Check "Finish" time < 2s
4. Check waterfall for slow requests

#### Memory Usage
- [ ] No memory leaks on navigation
- [ ] Reasonable heap size (<50MB for page)
- [ ] No console errors

**Test Steps**:
1. Open Chrome DevTools → Performance
2. Record for 30 seconds
3. Navigate between pages
4. Stop recording
5. Check for memory spikes

---

### Error Handling Testing

#### Network Errors
**Test Steps**:
1. Open page
2. Disconnect network (DevTools → Network → Offline)
3. Click "Refresh" button
4. Verify error alert appears: "Failed to load overview data"
5. Reconnect network
6. Click "Refresh" again
7. Verify data loads

#### API Errors
**Test Steps**:
1. Stop one backend service: `docker-compose stop inventory-service`
2. Navigate to Admin Overview
3. Verify page still loads (graceful degradation)
4. Check inventory stats show 0 or "N/A"
5. Restart service: `docker-compose start inventory-service`

#### Empty Data
**Test Steps**:
1. Clear database (reset H2): `docker-compose down -v && docker-compose up -d`
2. Navigate to overview pages
3. Verify empty state messages appear
4. Check "No data available" or "No recent orders" displays

---

### Integration Testing

#### End-to-End Workflow

**Scenario: Admin Reviews System Status**
1. Login as admin
2. Navigate to Admin Overview
3. Check total orders stat card
4. Click "Recent Orders" tab
5. Click on an order row (if clickable)
6. Verify navigation to order detail
7. Return to Admin Overview
8. Click "Low Stock Alerts" tab
9. Identify item with low stock
10. Click "Restock" button (if available)
11. Verify action triggers

**Scenario: Manager Monitors Operations**
1. Login as manager
2. Navigate to Manager Overview
3. Check for critical alerts banner
4. Review order status distribution
5. Click "Critical Alerts" tab
6. Verify items with ≤5 units pulse
7. Click "Recent Orders" tab
8. Sort by date (descending)
9. Check order type badges display correctly
10. Click refresh button
11. Verify data updates

---

### Browser Compatibility

Test on:
- [ ] Chrome (latest)
- [ ] Firefox (latest)
- [ ] Safari (latest)
- [ ] Edge (latest)

**Known Issues**:
- CSS Grid may need fallback for IE11 (not supported)
- CSS Variables require modern browsers

---

## Common Issues & Solutions

### Issue: "Failed to load overview data"
**Cause**: Backend service not running or network error  
**Solution**:
```bash
docker-compose ps  # Check service status
docker-compose logs -f api-gateway  # Check logs
docker-compose restart api-gateway  # Restart if needed
```

### Issue: Components not styled correctly
**Cause**: CSS modules not loading or cache issue  
**Solution**:
```bash
# Clear browser cache
Ctrl+Shift+Delete → Clear cache

# Rebuild frontend
docker-compose build --no-cache frontend
docker-compose up -d frontend
```

### Issue: Navigation menu doesn't show overview pages
**Cause**: App.jsx or Navigation.jsx not updated  
**Solution**:
- Verify imports in App.jsx
- Check Navigation.jsx has "Overview" submenu
- Hard refresh browser (Ctrl+Shift+R)

### Issue: Loading spinner doesn't appear
**Cause**: LoadingSpinner component not imported or CSS not loaded  
**Solution**:
- Check import statement: `import { LoadingSpinner } from '../components';`
- Verify CSS module exists: `LoadingSpinner.module.css`

### Issue: Tables don't sort
**Cause**: `sortable` prop not set on columns  
**Solution**:
- Add `sortable: true` to column definitions
- Check `handleSort` function in Table component

---

## Reporting Issues

If you encounter issues during testing:

1. **Capture Details**:
   - Browser and version
   - Screen size
   - Error message (exact text)
   - Steps to reproduce

2. **Check Console**:
   - Open DevTools → Console
   - Copy any error messages
   - Take screenshot

3. **Document**:
   - Create issue in project tracker
   - Include screenshots
   - Describe expected vs actual behavior

---

## Next Steps After Testing

Once testing is complete:

✅ **If all tests pass**:
- Proceed to Stage 3: Migrate existing dashboard pages
- Begin updating CustomerOrderCard, ProductionOrderCard, etc.
- Apply design system to all pages systematically

⚠️ **If issues found**:
- Fix critical bugs first (broken navigation, data not loading)
- Address UI issues (styling, alignment)
- Retest after fixes
- Document any known limitations

---

## Success Criteria

All overview pages are considered **production-ready** when:

- ✅ All components render correctly
- ✅ Data loads from backend APIs
- ✅ Tables sort and filter properly
- ✅ Tabs switch content smoothly
- ✅ Responsive on all screen sizes
- ✅ Accessible via keyboard and screen reader
- ✅ No console errors
- ✅ Error handling works gracefully
- ✅ Navigation functions correctly
- ✅ Performance is acceptable (<2s load)

---

**Testing Time Estimate**: 2-3 hours for comprehensive testing  
**Priority**: High (blocks Stage 3 migration)  
**Status**: Ready for testing ✅
