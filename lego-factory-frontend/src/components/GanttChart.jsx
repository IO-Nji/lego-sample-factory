import { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import '../styles/GanttChart.css';

/**
 * GanttChart Component
 * 
 * Visualizes production schedules in a timeline format showing:
 * - Tasks assigned to different workstations
 * - Duration and time allocation
 * - Dependencies between tasks
 * - Current progress status
 * 
 * @param {Array} scheduledTasks - Array of task objects with workstation, duration, start/end times
 * @param {Function} onTaskClick - Handler when a task bar is clicked
 * @param {Function} onTaskDrag - Handler for drag-and-drop rescheduling
 * @param {boolean} editable - Whether tasks can be dragged/edited
 */
function GanttChart({ scheduledTasks = [], onTaskClick, onTaskDrag, editable = false }) {
  const [timeScale, setTimeScale] = useState('hours'); // 'hours', 'days', 'weeks'
  const [viewStart, setViewStart] = useState(null);
  const [viewEnd, setViewEnd] = useState(null);
  const [hoveredTask, setHoveredTask] = useState(null);
  const chartRef = useRef(null);

  // Initialize view range based on tasks
  useEffect(() => {
    if (scheduledTasks.length > 0) {
      const startTimes = scheduledTasks
        .map(task => new Date(task.startTime || task.scheduledStartTime))
        .filter(date => !isNaN(date));
      
      const endTimes = scheduledTasks
        .map(task => new Date(task.endTime || task.scheduledEndTime))
        .filter(date => !isNaN(date));

      if (startTimes.length > 0 && endTimes.length > 0) {
        const minStart = new Date(Math.min(...startTimes));
        const maxEnd = new Date(Math.max(...endTimes));
        
        // Add padding (1 hour before and after)
        minStart.setHours(minStart.getHours() - 1);
        maxEnd.setHours(maxEnd.getHours() + 1);
        
        setViewStart(minStart);
        setViewEnd(maxEnd);
      }
    } else {
      // Default to current day
      const now = new Date();
      const start = new Date(now);
      start.setHours(8, 0, 0, 0);
      const end = new Date(now);
      end.setHours(18, 0, 0, 0);
      setViewStart(start);
      setViewEnd(end);
    }
  }, [scheduledTasks]);

  // Group tasks by workstation
  const groupTasksByWorkstation = () => {
    const grouped = {};
    scheduledTasks.forEach(task => {
      const ws = task.workstationName || task.workstationId || 'Unassigned';
      if (!grouped[ws]) {
        grouped[ws] = [];
      }
      grouped[ws].push(task);
    });
    return grouped;
  };

  // Calculate task position and width as percentages
  const calculateTaskPosition = (task) => {
    if (!viewStart || !viewEnd) return { left: 0, width: 0 };

    const taskStart = new Date(task.startTime || task.scheduledStartTime);
    const taskEnd = new Date(task.endTime || task.scheduledEndTime);
    
    if (isNaN(taskStart) || isNaN(taskEnd)) return { left: 0, width: 0 };

    const totalDuration = viewEnd - viewStart;
    const taskStartOffset = taskStart - viewStart;
    const taskDuration = taskEnd - taskStart;

    const left = (taskStartOffset / totalDuration) * 100;
    const width = (taskDuration / totalDuration) * 100;

    return {
      left: Math.max(0, Math.min(100, left)),
      width: Math.max(0.5, Math.min(100 - left, width))
    };
  };

  // Generate time markers
  const generateTimeMarkers = () => {
    if (!viewStart || !viewEnd) return [];
    
    const markers = [];
    const totalHours = (viewEnd - viewStart) / (1000 * 60 * 60);
    const interval = totalHours > 24 ? 4 : totalHours > 12 ? 2 : 1;

    let current = new Date(viewStart);
    while (current <= viewEnd) {
      markers.push(new Date(current));
      current.setHours(current.getHours() + interval);
    }

    return markers;
  };

  // Get status color
  const getStatusColor = (status) => {
    const statusColors = {
      PENDING: '#f59e0b',
      SCHEDULED: '#3b82f6',
      IN_PROGRESS: '#10b981',
      COMPLETED: '#6b7280',
      CANCELLED: '#ef4444'
    };
    return statusColors[status] || '#9ca3af';
  };

  // Format time for display
  const formatTime = (date) => {
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false 
    });
  };

  const groupedTasks = groupTasksByWorkstation();
  const timeMarkers = generateTimeMarkers();

  if (!viewStart || !viewEnd) {
    return (
      <div className="gantt-chart-empty">
        <p>Initializing timeline...</p>
      </div>
    );
  }

  return (
    <div className="gantt-chart-container" ref={chartRef}>
      {/* Header */}
      <div className="gantt-header">
        <h3 className="gantt-title">Production Schedule Timeline</h3>
        <div className="gantt-controls">
          <select 
            value={timeScale} 
            onChange={(e) => setTimeScale(e.target.value)}
            className="gantt-scale-selector"
          >
            <option value="hours">Hourly View</option>
            <option value="days">Daily View</option>
            <option value="weeks">Weekly View</option>
          </select>
          <span className="gantt-date-range">
            {viewStart.toLocaleDateString()} - {viewEnd.toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Timeline */}
      <div className="gantt-timeline">
        {/* Time markers */}
        <div className="gantt-time-axis">
          {timeMarkers.map((marker, idx) => (
            <div 
              key={idx} 
              className="time-marker"
              style={{ left: `${((marker - viewStart) / (viewEnd - viewStart)) * 100}%` }}
            >
              <span className="time-label">{formatTime(marker)}</span>
            </div>
          ))}
        </div>

        {/* Workstation rows */}
        <div className="gantt-rows">
          {Object.keys(groupedTasks).length === 0 ? (
            <div className="gantt-empty-state">
              <p>No scheduled tasks to display</p>
              <p className="gantt-empty-hint">Tasks will appear here when scheduled</p>
            </div>
          ) : (
            Object.entries(groupedTasks).map(([workstation, tasks]) => (
              <div key={workstation} className="gantt-row">
                <div className="gantt-row-label">
                  <span className="workstation-name">{workstation}</span>
                  <span className="task-count">{tasks.length} tasks</span>
                </div>
                <div className="gantt-row-timeline">
                  {tasks.map((task, idx) => {
                    const position = calculateTaskPosition(task);
                    return (
                      <div
                        key={task.id || idx}
                        className={`gantt-task ${editable ? 'editable' : ''} ${hoveredTask === task.id ? 'hovered' : ''}`}
                        style={{
                          left: `${position.left}%`,
                          width: `${position.width}%`,
                          backgroundColor: getStatusColor(task.status)
                        }}
                        onClick={() => onTaskClick && onTaskClick(task)}
                        onMouseEnter={() => setHoveredTask(task.id)}
                        onMouseLeave={() => setHoveredTask(null)}
                        draggable={editable}
                        onDragEnd={(e) => editable && onTaskDrag && onTaskDrag(task, e)}
                      >
                        <span className="task-label">
                          {task.taskType || task.operationType || 'Task'} 
                          {task.duration && ` (${task.duration}min)`}
                        </span>
                        
                        {/* Tooltip on hover */}
                        {hoveredTask === task.id && (
                          <div className="gantt-tooltip">
                            <div className="tooltip-row">
                              <strong>Task:</strong> {task.taskType || task.operationType}
                            </div>
                            <div className="tooltip-row">
                              <strong>Start:</strong> {formatTime(new Date(task.startTime || task.scheduledStartTime))}
                            </div>
                            <div className="tooltip-row">
                              <strong>End:</strong> {formatTime(new Date(task.endTime || task.scheduledEndTime))}
                            </div>
                            <div className="tooltip-row">
                              <strong>Duration:</strong> {task.duration} min
                            </div>
                            <div className="tooltip-row">
                              <strong>Status:</strong> {task.status}
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Legend */}
      <div className="gantt-legend">
        <span className="legend-title">Status:</span>
        {['PENDING', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map(status => (
          <div key={status} className="legend-item">
            <div 
              className="legend-color" 
              style={{ backgroundColor: getStatusColor(status) }}
            />
            <span>{status.replace('_', ' ')}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

GanttChart.propTypes = {
  scheduledTasks: PropTypes.array,
  onTaskClick: PropTypes.func,
  onTaskDrag: PropTypes.func,
  editable: PropTypes.bool
};

export default GanttChart;
