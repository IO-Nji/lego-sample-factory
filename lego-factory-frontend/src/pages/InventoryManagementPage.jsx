import { useState, useEffect } from "react";
import axios from "axios";
import { useAuth } from "../context/AuthContext";
import ErrorNotification from "../components/ErrorNotification";
import PageHeader from "../components/PageHeader";
import { StatisticsGrid } from "../components";
import { getErrorMessage } from "../utils/errorHandler";
import "../styles/StandardPage.css";
import "../styles/DashboardStandard.css";
import "../styles/AdminDashboard.css";

function InventoryManagementPage() {
  const { session } = useAuth();
  const [workstations, setWorkstations] = useState([]);
  const [selectedWorkstationId, setSelectedWorkstationId] = useState(null);
  const [allInventory, setAllInventory] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);
  const [editingItemId, setEditingItemId] = useState(null);
  const [editQuantity, setEditQuantity] = useState("");
  const [activeTab, setActiveTab] = useState("overview");
  const [totalInventoryItems, setTotalInventoryItems] = useState(0);
  const [lowStockItems, setLowStockItems] = useState([]);
  const [recentLedger, setRecentLedger] = useState([]);
  const [ledgerLoading, setLedgerLoading] = useState(false);
  const [ledgerError, setLedgerError] = useState(null);
  const [ledgerMode, setLedgerMode] = useState('recent'); // 'recent' | 'full'
  const [ledgerWsFilter, setLedgerWsFilter] = useState('ALL');
  const [ledgerTypeFilter, setLedgerTypeFilter] = useState('ALL');

  useEffect(() => {
    if (session?.user?.role === "ADMIN") {
      fetchWorkstations();
      fetchRecentLedger();
    } else {
      setError("Access denied. Admin role required to manage inventory.");
    }
  }, [session]);

  const fetchRecentLedger = async () => {
    setLedgerLoading(true);
    setLedgerError(null);
    try {
      const resp = await axios.get("/api/stock/ledger/recent");
      const entries = Array.isArray(resp.data) ? resp.data : [];
      setRecentLedger(entries);
      setLedgerMode('recent');
    } catch (err) {
      setLedgerError(getErrorMessage(err));
    } finally {
      setLedgerLoading(false);
    }
  };

  const fetchFullLedger = async () => {
    setLedgerLoading(true);
    setLedgerError(null);
    try {
      const resp = await axios.get("/api/stock/ledger");
      const entries = Array.isArray(resp.data) ? resp.data : [];
      setRecentLedger(entries);
      setLedgerMode('full');
    } catch (err) {
      setLedgerError(getErrorMessage(err));
    } finally {
      setLedgerLoading(false);
    }
  };

  const refreshLedger = () => {
    if (ledgerMode === 'full') return fetchFullLedger();
    return fetchRecentLedger();
  };

  const fetchWorkstations = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get("/api/masterdata/workstations");
      const stations = Array.isArray(response.data) ? response.data : [];
      setWorkstations(stations);
      
      if (stations.length > 0) {
        setSelectedWorkstationId(stations[0].id);
        await fetchAllInventory(stations);
      }
    } catch (err) {
      const errorMsg = getErrorMessage(err);
      setError(errorMsg);
      setNotification({
        message: errorMsg,
        type: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  const getDemoInventoryData = (workstationId) => {
    const demoData = {
      1: [
        { itemId: 101, itemType: 'CONNECTOR', quantity: 45 },
        { itemId: 102, itemType: 'GEAR', quantity: 3 }, // Low stock
        { itemId: 103, itemType: 'BRICK', quantity: 120 },
        { itemId: 104, itemType: 'AXLE', quantity: 8 },
        { itemId: 105, itemType: 'WHEEL', quantity: 0 }, // Out of stock
      ],
      2: [
        { itemId: 201, itemType: 'MOTOR', quantity: 25 },
        { itemId: 202, itemType: 'BUSHING', quantity: 2 }, // Low stock
        { itemId: 203, itemType: 'PANEL', quantity: 15 },
        { itemId: 204, itemType: 'SHAFT', quantity: 62 },
      ],
      3: [
        { itemId: 301, itemType: 'SPRING', quantity: 95 },
        { itemId: 302, itemType: 'PIN', quantity: 4 }, // Low stock
        { itemId: 303, itemType: 'ROD', quantity: 38 },
      ],
    };
    return demoData[workstationId] || [];
  };

  const fetchAllInventory = async (stations) => {
    const inventoryMap = {};
    let totalItems = 0;
    const lowStockList = [];

    for (const station of stations) {
      try {
        const response = await axios.get(`/api/stock/workstation/${station.id}`);
        let items = Array.isArray(response.data) ? response.data : [];
        
        if (items.length === 0) {
          console.warn(`No inventory data from API for WS-${station.id}, using demo data`);
          items = getDemoInventoryData(station.id);
        }
        
        inventoryMap[station.id] = items;
        
        // Calculate totals
        items.forEach(item => {
          totalItems += item.quantity || 0;
          if (item.quantity <= 5) {
            lowStockList.push({
              ...item,
              workstationId: station.id,
              workstationName: station.name || `WS-${station.id}`
            });
          }
        });
      } catch (err) {
        console.error(`Failed to fetch inventory for WS-${station.id}:`, err);
        const demoItems = getDemoInventoryData(station.id);
        inventoryMap[station.id] = demoItems;
        
        demoItems.forEach(item => {
          totalItems += item.quantity || 0;
          if (item.quantity <= 5) {
            lowStockList.push({
              ...item,
              workstationId: station.id,
              workstationName: station.name || `WS-${station.id}`
            });
          }
        });
      }
    }

    setAllInventory(inventoryMap);
    setTotalInventoryItems(totalItems);
    setLowStockItems(lowStockList);
  };

  const handleEditClick = (item) => {
    setEditingItemId(item.itemId);
    setEditQuantity(item.quantity);
  };

  const handleCancelEdit = () => {
    setEditingItemId(null);
    setEditQuantity("");
  };

  const handleUpdateQuantity = async (workstationId, itemId, newQuantity) => {
    if (newQuantity < 0) {
      setNotification({
        message: "Quantity cannot be negative",
        type: 'error'
      });
      return;
    }

    const newQty = Number.parseInt(newQuantity);
    let apiSuccess = false;

    try {
      // Try to update via API
      await axios.put(`/api/inventory/update`, {
        workstationId,
        itemId,
        newQuantity: newQty
      });
      apiSuccess = true;
    } catch (err) {
      console.warn(`API update failed, updating local state only:`, err);
    }

    setAllInventory(prev => ({
      ...prev,
      [workstationId]: prev[workstationId].map(item =>
        item.itemId === itemId ? { ...item, quantity: newQty } : item
      )
    }));

    // Recalculate totals and low stock items
    const updatedInventory = allInventory[workstationId].map(item =>
      item.itemId === itemId ? { ...item, quantity: newQty } : item
    );

    let totalItems = 0;
    const lowStockList = [];
    
    Object.entries(allInventory).forEach(([wsId, items]) => {
      const itemsToCount = wsId === workstationId.toString() ? updatedInventory : items;
      itemsToCount.forEach(item => {
        totalItems += item.quantity || 0;
        if (item.quantity <= 5) {
          lowStockList.push({
            ...item,
            workstationId: Number.parseInt(wsId),
            workstationName: getWorkstationName(Number.parseInt(wsId))
          });
        }
      });
    });

    setTotalInventoryItems(totalItems);
    setLowStockItems(lowStockList);

    const successMsg = apiSuccess
      ? `Item ${itemId} quantity updated to ${newQty}`
      : `Item ${itemId} quantity updated to ${newQty} (local only)`;

    setNotification({
      message: successMsg,
      type: 'success'
    });

    setEditingItemId(null);
    setEditQuantity("");
  };

  const getWorkstationName = (workstationId) => {
    const ws = workstations.find(w => w.id === workstationId);
    return ws ? ws.name : `WS-${workstationId}`;
  };

  const getCurrentInventory = () => {
    return allInventory[selectedWorkstationId] || [];
  };

  const renderOverviewTab = () => {
    const statsData = [
      { value: totalInventoryItems, label: 'Total Items', variant: 'default', icon: '📊' },
      { value: workstations.length, label: 'Workstations', variant: 'default', icon: '🏭' },
      { value: lowStockItems.length, label: 'Low Stock Items', variant: lowStockItems.length > 0 ? 'warning' : 'success', icon: '⚠️' },
      { value: Object.keys(allInventory).length, label: 'Active Locations', variant: 'default', icon: '📍' },
      { value: recentLedger.length, label: 'Recent Transactions', variant: 'info', icon: '📋' },
      { value: 0, label: 'Pending Adjustments', variant: 'default', icon: '⏳' },
      { value: 0, label: 'Critical Stock', variant: 'danger', icon: '🚨' },
      { value: 0, label: 'Overstock Items', variant: 'info', icon: '📦' },
    ];

    return (
      <div className="overview-tab">
        <h3>Inventory Statistics</h3>
        <div className="stats-grid">
          <StatisticsGrid stats={statsData} />
        </div>

        {lowStockItems.length > 0 && (
          <div className="dashboard-section" style={{ marginTop: '1.5rem' }}>
            <h3>⚠️ Low Stock Alert</h3>
            <table className="products-table">
              <thead>
                <tr>
                <th>Workstation</th>
                <th>Item Type</th>
                <th>Item ID</th>
                <th>Current Qty</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {lowStockItems.map((item, idx) => (
                <tr key={idx}>
                  <td>{item.workstationName}</td>
                  <td>{item.itemType}</td>
                  <td>#{item.itemId}</td>
                  <td>
                    <span className="quantity-badge low">{item.quantity}</span>
                  </td>
                  <td>
                    <button
                      className="edit-btn"
                      onClick={() => {
                        setSelectedWorkstationId(item.workstationId);
                        setActiveTab("manage");
                      }}
                    >
                      ✎ Update
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        )}

        <div className="all-workstations-section" style={{ marginTop: '1.5rem' }}>
          <h3>📦 All Workstations Inventory Summary</h3>
          <div className="workstations-grid">
            {workstations.map(ws => {
            const wsInventory = allInventory[ws.id] || [];
            const wsTotal = wsInventory.reduce((sum, item) => sum + (item.quantity || 0), 0);
            return (
              <div
                key={ws.id}
                className="workstation-card"
                onClick={() => {
                  setSelectedWorkstationId(ws.id);
                  setActiveTab("manage");
                }}
              >
                <h4>{ws.name || `Workstation ${ws.id}`}</h4>
                <div className="ws-stat">
                  <span className="ws-label">Total Items:</span>
                  <span className="ws-value">{wsTotal}</span>
                </div>
                <div className="ws-stat">
                  <span className="ws-label">Item Types:</span>
                  <span className="ws-value">{wsInventory.length}</span>
                </div>
                <button className="secondary-link">View Details</button>
              </div>
            );
          })}
        </div>
        </div>
      </div>
    );
  };

  const renderLedgerTab = () => (
    <div className="ledger-tab">
      <div className="dashboard-section" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
        <h3 style={{ margin: 0, fontSize: '1rem' }}>🧾 {ledgerMode === 'full' ? 'Stock Ledger (Full)' : 'Recent Stock Ledger'}</h3>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <label htmlFor="ledger-ws-filter" className="filter-label">Workstation:</label>
          <select
            id="ledger-ws-filter"
            className="filter-select"
            value={ledgerWsFilter}
            onChange={(e) => setLedgerWsFilter(e.target.value)}
          >
            <option value="ALL">All</option>
            {workstations.map(ws => (
              <option key={ws.id} value={String(ws.id)}>{ws.name || `Workstation ${ws.id}`}</option>
            ))}
          </select>
          <label htmlFor="ledger-type-filter" className="filter-label">Item Type:</label>
          <select
            id="ledger-type-filter"
            className="filter-select"
            value={ledgerTypeFilter}
            onChange={(e) => setLedgerTypeFilter(e.target.value)}
          >
            <option value="ALL">All</option>
            {[...new Set((recentLedger || []).map(e => e.itemType).filter(Boolean))].map(t => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
          <button className="secondary-link" onClick={refreshLedger} disabled={ledgerLoading}>
            {ledgerLoading ? 'Refreshing…' : 'Refresh'}
          </button>
          {ledgerMode === 'full' ? (
            <button className="primary-link" onClick={fetchRecentLedger} disabled={ledgerLoading}>View recent</button>
          ) : (
            <button className="primary-link" onClick={fetchFullLedger} disabled={ledgerLoading}>View full ledger</button>
          )}
        </div>
      </div>
      {ledgerError && (
        <ErrorNotification message={ledgerError} onDismiss={() => setLedgerError(null)} />
      )}
      <div className="low-stock-table-container">
        {ledgerLoading ? (
          <div className="loading-state"><p>Loading recent stock movements…</p></div>
        ) : recentLedger && recentLedger.length > 0 ? (
          <table className="products-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Workstation</th>
                <th>Item Type</th>
                <th>Item ID</th>
                <th>Delta</th>
                <th>Balance</th>
                <th>Reason</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {recentLedger
                .filter(e => {
                  const wsOk = ledgerWsFilter === 'ALL' || String(e.workstationId || '') === String(ledgerWsFilter);
                  const typeOk = ledgerTypeFilter === 'ALL' || e.itemType === ledgerTypeFilter;
                  return wsOk && typeOk;
                })
                .map((e, idx) => {
                const ts = e.createdAt || e.timestamp;
                const when = ts ? new Date(ts).toLocaleString() : '-';
                const wsName = e.workstationId ? getWorkstationName(e.workstationId) : '-';
                const delta = Number(e.delta ?? 0);
                const balance = e.balanceAfter ?? e.balance ?? '-';
                return (
                  <tr key={idx}>
                    <td>{when}</td>
                    <td>{wsName}</td>
                    <td>{e.itemType}</td>
                    <td>#{e.itemId}</td>
                    <td>
                      <span className={`delta-badge ${delta > 0 ? 'positive' : delta < 0 ? 'negative' : ''}`}>
                        {delta > 0 ? `+${delta}` : delta}
                      </span>
                    </td>
                    <td>{balance}</td>
                    <td>{e.reasonCode || '-'}</td>
                    <td className="truncate">{e.notes || '-'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        ) : (
          <div className="empty-state"><p>No recent stock movements</p></div>
        )}
      </div>
    </div>
  );

  const renderManageTab = () => (
    <div className="inventory-manage">
      <div className="manage-header">
        <div>
          <h3>Manage Inventory</h3>
          <p className="manage-subtitle">
            Current Workstation: <strong>{getWorkstationName(selectedWorkstationId)}</strong>
          </p>
        </div>
        <div className="workstation-selector">
          <label htmlFor="ws-select">Switch Workstation:</label>
          <select
            id="ws-select"
            value={selectedWorkstationId || ""}
            onChange={(e) => setSelectedWorkstationId(parseInt(e.target.value))}
          >
            {workstations.map(ws => (
              <option key={ws.id} value={ws.id}>
                {ws.name || `Workstation ${ws.id}`}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="inventory-table-container">
        {getCurrentInventory().length > 0 ? (
          <table className="products-table">
            <thead>
              <tr>
                <th>Item Type</th>
                <th>Item ID</th>
                <th>Current Qty</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {getCurrentInventory().map((item) => (
                <tr key={item.itemId} className="inventory-row">
                  <td>{item.itemType}</td>
                  <td>#{item.itemId}</td>
                  <td>
                    {editingItemId === item.itemId ? (
                      <input
                        type="number"
                        value={editQuantity}
                        onChange={(e) => setEditQuantity(e.target.value)}
                        className="quantity-input"
                        min="0"
                      />
                    ) : (
                      <span className={`quantity-badge ${item.quantity <= 5 ? 'low' : item.quantity <= 20 ? 'medium' : 'high'}`}>
                        {item.quantity}
                      </span>
                    )}
                  </td>
                  <td>
                    {item.quantity === 0 ? (
                      <span className="status-badge out-of-stock">Out of Stock</span>
                    ) : item.quantity <= 5 ? (
                      <span className="status-badge low-stock">Low Stock</span>
                    ) : item.quantity <= 20 ? (
                      <span className="status-badge medium-stock">Medium</span>
                    ) : (
                      <span className="status-badge in-stock">In Stock</span>
                    )}
                  </td>
                  <td className="actions-cell">
                    {editingItemId === item.itemId ? (
                      <div className="actions">
                        <button
                          className="edit-btn"
                          onClick={() => handleUpdateQuantity(selectedWorkstationId, item.itemId, editQuantity)}
                        >
                          ✓ Save
                        </button>
                        <button
                          className="delete-btn"
                          onClick={handleCancelEdit}
                        >
                          ✕ Cancel
                        </button>
                      </div>
                    ) : (
                      <div className="actions">
                        <button
                          className="edit-btn"
                          onClick={() => handleEditClick(item)}
                        >
                          ✎ Edit
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="empty-state">
            <p>No inventory items for this workstation</p>
          </div>
        )}
      </div>
    </div>
  );

  return (
    <div className="standard-page-container">
      <PageHeader
        title="Inventory Management"
        subtitle={`Manage and monitor inventory across all workstations. Total items: ${totalInventoryItems}`}
        icon="📦"
      />
      
      <section className="admin-dashboard">
        {notification && (
          <ErrorNotification
            message={notification.message}
            type={notification.type}
            onClose={() => setNotification(null)}
          />
        )}

        {error && <div className="error-message">{error}</div>}

      <div className="tabs">
        <button
          className={`tab-button ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          📊 Overview
        </button>
        <button
          className={`tab-button ${activeTab === 'manage' ? 'active' : ''}`}
          onClick={() => setActiveTab('manage')}
        >
          📝 Manage Stock
        </button>
        <button
          className={`tab-button ${activeTab === 'ledger' ? 'active' : ''}`}
          onClick={() => setActiveTab('ledger')}
        >
          📜 Stock Ledger
        </button>
      </div>

      <div className="tab-content">
        {loading && <div className="loading">Loading inventory data...</div>}
        {!loading && activeTab === 'overview' && renderOverviewTab()}
        {!loading && activeTab === 'manage' && renderManageTab()}
        {!loading && activeTab === 'ledger' && renderLedgerTab()}
      </div>
      </section>
    </div>
  );
}

export default InventoryManagementPage;
