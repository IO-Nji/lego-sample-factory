import { useState } from 'react';
import { Notification } from '../components';

/**
 * Notification Component Test/Demo Page
 * 
 * This page demonstrates the Notification component with sample data
 * Use this to test notification styling and behavior
 */
function NotificationTestPage() {
  const [notifications, setNotifications] = useState([
    {
      id: 1,
      message: 'Order CO-001 created successfully',
      type: 'success',
      timestamp: new Date(Date.now() - 5000).toISOString(),
      station: 'Plant Warehouse'
    },
    {
      id: 2,
      message: 'Production order PO-042 started',
      type: 'info',
      timestamp: new Date(Date.now() - 15000).toISOString(),
      station: 'Production Planning'
    },
    {
      id: 3,
      message: 'Low inventory warning: 8 items remaining',
      type: 'warning',
      timestamp: new Date(Date.now() - 45000).toISOString(),
      station: 'Modules Supermarket'
    },
    {
      id: 4,
      message: 'Failed to fulfill order: Insufficient stock',
      type: 'error',
      timestamp: new Date(Date.now() - 120000).toISOString(),
      station: 'Plant Warehouse'
    },
    {
      id: 5,
      message: 'Assembly task completed',
      type: 'success',
      timestamp: new Date(Date.now() - 180000).toISOString(),
      station: 'Final Assembly'
    }
  ]);

  const addNotification = (type) => {
    const messages = {
      success: ['Order processed', 'Task completed', 'Item shipped', 'Production finished'],
      error: ['Connection failed', 'Invalid input', 'Operation cancelled', 'System error'],
      warning: ['Low stock alert', 'Approaching deadline', 'Maintenance required', 'Resource constraint'],
      info: ['Status updated', 'Process started', 'Data synchronized', 'Schedule updated']
    };

    const randomMessage = messages[type][Math.floor(Math.random() * messages[type].length)];
    const stations = ['Plant Warehouse', 'Modules Supermarket', 'Production Planning', 'Final Assembly', 'Manufacturing'];
    
    const newNotification = {
      id: Date.now() + Math.random(),
      message: randomMessage,
      type,
      timestamp: new Date().toISOString(),
      station: stations[Math.floor(Math.random() * stations.length)]
    };

    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <h1 style={{ marginBottom: '1rem' }}>📬 Notification Component Test</h1>
      <p style={{ marginBottom: '2rem', color: '#666' }}>
        Test the notification component with different notification types
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '2rem' }}>
        {/* Test Controls */}
        <div>
          <h2 style={{ marginBottom: '1rem' }}>Test Controls</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <button 
              onClick={() => addNotification('success')}
              style={{
                padding: '0.75rem 1rem',
                backgroundColor: '#16a34a',
                color: 'white',
                border: 'none',
                borderRadius: '0.375rem',
                cursor: 'pointer',
                fontSize: '1rem'
              }}
            >
              ✓ Add Success Notification
            </button>
            <button 
              onClick={() => addNotification('error')}
              style={{
                padding: '0.75rem 1rem',
                backgroundColor: '#dc2626',
                color: 'white',
                border: 'none',
                borderRadius: '0.375rem',
                cursor: 'pointer',
                fontSize: '1rem'
              }}
            >
              ✗ Add Error Notification
            </button>
            <button 
              onClick={() => addNotification('warning')}
              style={{
                padding: '0.75rem 1rem',
                backgroundColor: '#f59e0b',
                color: 'white',
                border: 'none',
                borderRadius: '0.375rem',
                cursor: 'pointer',
                fontSize: '1rem'
              }}
            >
              ⚠ Add Warning Notification
            </button>
            <button 
              onClick={() => addNotification('info')}
              style={{
                padding: '0.75rem 1rem',
                backgroundColor: '#3b82f6',
                color: 'white',
                border: 'none',
                borderRadius: '0.375rem',
                cursor: 'pointer',
                fontSize: '1rem'
              }}
            >
              ℹ Add Info Notification
            </button>
          </div>

          <div style={{ marginTop: '1rem', padding: '1rem', backgroundColor: '#f3f4f6', borderRadius: '0.375rem' }}>
            <p style={{ margin: 0, fontSize: '0.875rem' }}>
              <strong>Total Notifications:</strong> {notifications.length}
            </p>
            <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.875rem' }}>
              <strong>Visible:</strong> {Math.min(notifications.length, 5)} of {notifications.length}
            </p>
          </div>
        </div>

        {/* Notification Component */}
        <div>
          <h2 style={{ marginBottom: '1rem' }}>Notification Display</h2>
          <Notification 
            notifications={notifications}
            title="Station Activity Log"
            maxVisible={5}
            onClear={clearNotifications}
          />
        </div>
      </div>

      {/* Documentation */}
      <div style={{ 
        marginTop: '3rem', 
        padding: '1.5rem', 
        backgroundColor: '#f9fafb', 
        borderRadius: '0.5rem',
        border: '1px solid #e5e7eb'
      }}>
        <h2 style={{ marginTop: 0 }}>Component Features</h2>
        <ul style={{ lineHeight: '1.8' }}>
          <li>✅ <strong>Console-style display</strong> - Monospace font, line-by-line stacking</li>
          <li>✅ <strong>Auto-scroll behavior</strong> - New notifications appear at top</li>
          <li>✅ <strong>Scrollable history</strong> - Shows 5 lines, scroll for more</li>
          <li>✅ <strong>Type indicators</strong> - Color + icon for each notification type</li>
          <li>✅ <strong>Timestamp display</strong> - HH:MM:SS format for each notification</li>
          <li>✅ <strong>Station context</strong> - Shows which station triggered the notification</li>
          <li>✅ <strong>Clear function</strong> - Remove all notifications at once</li>
          <li>✅ <strong>Slide-in animation</strong> - Smooth entrance for new notifications</li>
        </ul>

        <h3>Usage in Dashboards</h3>
        <pre style={{ 
          backgroundColor: '#1f2937', 
          color: '#f3f4f6', 
          padding: '1rem', 
          borderRadius: '0.375rem',
          overflow: 'auto',
          fontSize: '0.875rem'
        }}>
{`<DashboardLayout
  layout="compact"
  statsCards={renderStatsCards()}
  primaryContent={renderPrimaryContent()}
  notifications={
    <Notification 
      notifications={notifications}
      title="Station Activity"
      maxVisible={5}
      onClear={clearNotifications}
    />
  }
  ordersSection={renderOrdersSection()}
/>`}
        </pre>

        <p style={{ marginTop: '1rem', fontSize: '0.875rem', color: '#666' }}>
          See <strong>docs/NotificationComponent.md</strong> for complete documentation
        </p>
      </div>
    </div>
  );
}

export default NotificationTestPage;
