import { useState, useEffect } from 'react';
import api from '../api/api';
import GanttChart from '../components/GanttChart';
import { Button } from '../components';
import '../styles/ManualScheduler.css';

/**
 * Manual Scheduler Page
 * 
 * Allows production planners to:
 * - View scheduled production tasks in Gantt chart
 * - Manually adjust task schedules
 * - Assign tasks to workstations
 * - Modify task durations and priorities
 * - Visualize conflicts and resource allocation
 */
function ManualSchedulerPage() {
  const [scheduledOrders, setScheduledOrders] = useState([]);
  const [productionOrders, setProductionOrders] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Manual adjustment form state
  const [adjustmentForm, setAdjustmentForm] = useState({
    workstationId: '',
    scheduledStartTime: '',
    duration: '',
    priority: 'NORMAL'
  });

  // Fetch scheduled orders from SimAL
  const fetchScheduledOrders = async () => {
    setLoading(true);
    try {
      const response = await api.get('/simal/scheduled-orders');
      setScheduledOrders(response.data || []);
    } catch (err) {
      console.error('Failed to fetch scheduled orders:', err);
      setError('Failed to load scheduled orders');
    } finally {
      setLoading(false);
    }
  };

  // Fetch production orders
  const fetchProductionOrders = async () => {
    try {
      const response = await api.get('/production-orders');
      setProductionOrders(response.data || []);
    } catch (err) {
      console.error('Failed to fetch production orders:', err);
    }
  };

  useEffect(() => {
    fetchScheduledOrders();
    fetchProductionOrders();
    
    // Refresh every 30 seconds
    const interval = setInterval(() => {
      fetchScheduledOrders();
      fetchProductionOrders();
    }, 30000);
    
    return () => clearInterval(interval);
  }, []);

  // Convert scheduled orders to tasks for Gantt chart
  const tasksForGantt = scheduledOrders.flatMap(order => 
    (order.scheduledTasks || []).map(task => ({
      ...task,
      id: task.taskId,
      workstationName: task.workstation,
      startTime: task.startTime,
      endTime: task.endTime,
      status: order.status || 'SCHEDULED'
    }))
  );

  // Handle task click in Gantt chart
  const handleTaskClick = (task) => {
    setSelectedTask(task);
    setAdjustmentForm({
      workstationId: task.workstationId || '',
      scheduledStartTime: task.startTime || '',
      duration: task.duration || '',
      priority: 'NORMAL'
    });
    setError(null);
    setSuccess(null);
  };

  // Handle form input changes
  const handleFormChange = (field, value) => {
    setAdjustmentForm(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Submit manual schedule adjustment
  const handleSubmitAdjustment = async () => {
    if (!selectedTask) {
      setError('No task selected');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      // Call backend API to update schedule
      await api.put(`/simal/tasks/${selectedTask.taskId}/reschedule`, {
        workstationId: adjustmentForm.workstationId,
        scheduledStartTime: adjustmentForm.scheduledStartTime,
        duration: parseInt(adjustmentForm.duration),
        priority: adjustmentForm.priority
      });

      setSuccess(`Task ${selectedTask.taskId} rescheduled successfully`);
      setSelectedTask(null);
      fetchScheduledOrders(); // Refresh
    } catch (err) {
      setError('Failed to reschedule task: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  // Clear selection
  const handleClearSelection = () => {
    setSelectedTask(null);
    setAdjustmentForm({
      workstationId: '',
      scheduledStartTime: '',
      duration: '',
      priority: 'NORMAL'
    });
    setError(null);
    setSuccess(null);
  };

  return (
    <div className="manual-scheduler-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Manual Production Scheduler</h1>
          <p className="page-subtitle">
            Visualize and manually adjust production task schedules
          </p>
        </div>
        <Button 
          variant="secondary" 
          onClick={fetchScheduledOrders}
          disabled={loading}
        >
          {loading ? 'Refreshing...' : '🔄 Refresh'}
        </Button>
      </div>

      {/* Notifications */}
      {error && (
        <div className="notification error">
          <span>❌ {error}</span>
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}
      {success && (
        <div className="notification success">
          <span>✅ {success}</span>
          <button onClick={() => setSuccess(null)}>✕</button>
        </div>
      )}

      {/* Gantt Chart Section */}
      <div className="scheduler-section">
        <GanttChart
          scheduledTasks={tasksForGantt}
          onTaskClick={handleTaskClick}
          editable={true}
        />
      </div>

      {/* Manual Adjustment Panel */}
      <div className="scheduler-section">
        <div className="adjustment-panel">
          <div className="panel-header">
            <h2>Manual Schedule Adjustment</h2>
            {selectedTask && (
              <Button 
                variant="ghost" 
                size="small" 
                onClick={handleClearSelection}
              >
                Clear Selection
              </Button>
            )}
          </div>

          {selectedTask ? (
            <div className="adjustment-form">
              {/* Selected Task Info */}
              <div className="selected-task-info">
                <h3>Selected Task: {selectedTask.taskType || selectedTask.operationType}</h3>
                <div className="task-details-grid">
                  <div className="detail-item">
                    <span className="label">Task ID:</span>
                    <span className="value">{selectedTask.taskId}</span>
                  </div>
                  <div className="detail-item">
                    <span className="label">Current Workstation:</span>
                    <span className="value">{selectedTask.workstation}</span>
                  </div>
                  <div className="detail-item">
                    <span className="label">Current Start:</span>
                    <span className="value">
                      {new Date(selectedTask.startTime).toLocaleString()}
                    </span>
                  </div>
                  <div className="detail-item">
                    <span className="label">Current Duration:</span>
                    <span className="value">{selectedTask.duration} min</span>
                  </div>
                  <div className="detail-item">
                    <span className="label">Status:</span>
                    <span className={`status-badge ${selectedTask.status.toLowerCase()}`}>
                      {selectedTask.status}
                    </span>
                  </div>
                </div>
              </div>

              {/* Adjustment Form */}
              <div className="form-grid">
                <div className="form-group">
                  <label htmlFor="workstation">New Workstation ID</label>
                  <input
                    id="workstation"
                    type="text"
                    value={adjustmentForm.workstationId}
                    onChange={(e) => handleFormChange('workstationId', e.target.value)}
                    placeholder="Enter workstation ID"
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="startTime">New Start Time</label>
                  <input
                    id="startTime"
                    type="datetime-local"
                    value={adjustmentForm.scheduledStartTime}
                    onChange={(e) => handleFormChange('scheduledStartTime', e.target.value)}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="duration">New Duration (minutes)</label>
                  <input
                    id="duration"
                    type="number"
                    value={adjustmentForm.duration}
                    onChange={(e) => handleFormChange('duration', e.target.value)}
                    placeholder="Duration in minutes"
                    min="1"
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="priority">Priority</label>
                  <select
                    id="priority"
                    value={adjustmentForm.priority}
                    onChange={(e) => handleFormChange('priority', e.target.value)}
                    className="form-input"
                  >
                    <option value="LOW">Low</option>
                    <option value="NORMAL">Normal</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="form-actions">
                <Button 
                  variant="primary" 
                  onClick={handleSubmitAdjustment}
                  disabled={loading || !adjustmentForm.workstationId || !adjustmentForm.scheduledStartTime}
                >
                  {loading ? 'Applying...' : '✅ Apply Changes'}
                </Button>
                <Button 
                  variant="secondary" 
                  onClick={handleClearSelection}
                  disabled={loading}
                >
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <div className="no-selection">
              <p>Click on a task in the Gantt chart above to adjust its schedule</p>
              <p className="hint">You can also drag tasks to reschedule them visually</p>
            </div>
          )}
        </div>
      </div>

      {/* Unscheduled Orders Section */}
      <div className="scheduler-section">
        <div className="unscheduled-orders">
          <h2>Unscheduled Production Orders</h2>
          <div className="orders-list">
            {productionOrders
              .filter(order => order.status === 'CREATED' || order.status === 'SUBMITTED')
              .length === 0 ? (
              <div className="empty-state">
                <p>No unscheduled orders</p>
              </div>
            ) : (
              productionOrders
                .filter(order => order.status === 'CREATED' || order.status === 'SUBMITTED')
                .map(order => (
                  <div key={order.id} className="order-card">
                    <div className="order-header">
                      <span className="order-number">#{order.productionOrderNumber}</span>
                      <span className={`order-status ${order.status.toLowerCase()}`}>
                        {order.status}
                      </span>
                    </div>
                    <div className="order-details">
                      {order.priority && (
                        <span className={`priority-badge ${order.priority.toLowerCase()}`}>
                          {order.priority}
                        </span>
                      )}
                      {order.dueDate && (
                        <span className="due-date">
                          Due: {new Date(order.dueDate).toLocaleDateString()}
                        </span>
                      )}
                    </div>
                  </div>
                ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ManualSchedulerPage;
