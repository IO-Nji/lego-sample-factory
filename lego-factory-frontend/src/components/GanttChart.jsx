import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
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
 * @param {Function} onTaskDragEnd - Handler for drag-and-drop rescheduling
 * @param {boolean} editable - Whether tasks can be dragged/edited
 */
function GanttChart({ scheduledTasks = [], onTaskClick, onTaskDragEnd, editable = false }) {
  const [timeScale, setTimeScale] = useState('hours'); // 'hours', 'days', 'weeks'
  const [viewStart, setViewStart] = useState(null);
  const [viewEnd, setViewEnd] = useState(null);
  const [hoveredTask, setHoveredTask] = useState(null);
  const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });
  const [tooltipContent, setTooltipContent] = useState(null);
  const [draggedTask, setDraggedTask] = useState(null);
  const [dragStartX, setDragStartX] = useState(0);
  const [dragCurrentX, setDragCurrentX] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
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

  // Drag-and-drop handlers
  const handleDragStart = (task, e) => {
    if (!editable) return;
    setDraggedTask(task);
    setDragStartX(e.clientX);
    setDragCurrentX(e.clientX);
    setIsDragging(true);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDrag = (e) => {
    if (!isDragging || !draggedTask) return;
    if (e.clientX === 0) return; // Ignore invalid drag events
    setDragCurrentX(e.clientX);
  };

  const handleDragEnd = (e) => {
    if (!draggedTask || !chartRef.current || !viewStart || !viewEnd) {
      resetDrag();
      return;
    }

    // Calculate time offset based on drag distance
    const chartRect = chartRef.current.getBoundingClientRect();
    const timelineElement = chartRef.current.querySelector('.gantt-row-timeline');
    if (!timelineElement) {
      resetDrag();
      return;
    }

    const timelineRect = timelineElement.getBoundingClientRect();
    const dragDistance = dragCurrentX - dragStartX;
    const timelineWidth = timelineRect.width;
    const totalDuration = viewEnd - viewStart;
    
    // Calculate time offset in milliseconds
    const timeOffset = (dragDistance / timelineWidth) * totalDuration;
    
    // Calculate new start time
    const originalStartTime = new Date(draggedTask.startTime || draggedTask.scheduledStartTime);
    const newStartTime = new Date(originalStartTime.getTime() + timeOffset);
    
    // Only trigger callback if moved more than 5 minutes
    const minutesMoved = Math.abs(timeOffset / (1000 * 60));
    if (minutesMoved > 5 && onTaskDragEnd) {
      onTaskDragEnd(draggedTask, newStartTime);
    }

    resetDrag();
  };

  const resetDrag = () => {
    setDraggedTask(null);
    setDragStartX(0);
    setDragCurrentX(0);
    setIsDragging(false);
  };

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

  // Get status-based color (matches workstation order status)
  // Maps actual production status to visual feedback colors
  const getStatusColor = (status) => {
    const statusColors = {
      PENDING: {
        base: '#f59e0b', // Orange - task not yet started
        border: '#d97706',
        text: '#ffffff'
      },
      SCHEDULED: {
        base: '#3b82f6', // Blue - task scheduled but not confirmed
        border: '#2563eb',
        text: '#ffffff'
      },
      IN_PROGRESS: {
        base: '#10b981', // Green - active work at workstation
        border: '#059669',
        text: '#ffffff'
      },
      COMPLETED: {
        base: '#6b7280', // Gray - task finished
        border: '#4b5563',
        text: '#ffffff'
      },
      CANCELLED: {
        base: '#ef4444', // Red - task cancelled
        border: '#dc2626',
        text: '#ffffff'
      }
    };

    return statusColors[status] || statusColors.PENDING;
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
  
  // Calculate dynamic height based on number of workstations
  // Ensures all workstations are visible without scrolling
  const workstationCount = Object.keys(groupedTasks).length;
  const rowHeight = 60; // Base height per workstation row (matches .gantt-row min-height)
  const headerHeight = 100; // Header + time axis height
  const legendHeight = 50; // Legend at bottom
  const minHeight = 300; // Minimum chart height
  const maxHeight = 800; // Maximum chart height to prevent excessive vertical space
  
  const calculatedHeight = headerHeight + (workstationCount * rowHeight) + legendHeight;
  const timelineHeight = Math.max(minHeight, Math.min(maxHeight, calculatedHeight));

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
      <div className="gantt-timeline" style={{ minHeight: `${timelineHeight}px` }}>
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
                    const isBeingDragged = draggedTask?.id === task.id || draggedTask?.taskId === task.taskId;
                    const statusColors = getStatusColor(task.status);
                    
                    return (
                      <div
                        key={task.id || idx}
                        className={`gantt-task ${editable ? 'editable' : ''} ${hoveredTask === task.id ? 'hovered' : ''} ${isBeingDragged ? 'dragging' : ''}`}
                        style={{
                          left: `${position.left}%`,
                          width: `${position.width}%`,
                          backgroundColor: statusColors.base,
                          borderLeft: `4px solid ${statusColors.border}`,
                          color: statusColors.text,
                          cursor: editable ? 'grab' : 'pointer',
                          opacity: isBeingDragged ? 0.6 : 1
                        }}
                        onClick={() => onTaskClick && onTaskClick(task)}
                        onMouseEnter={(e) => {
                          setHoveredTask(task.id);
                          setTooltipPosition({ x: e.clientX, y: e.clientY });
                          setTooltipContent({
                            taskName: task.taskType || task.operationType || task.itemName || 'Task',
                            status: task.status || 'PENDING',
                            statusClass: (task.status || 'PENDING').toLowerCase().replace('_', '-'),
                            workstation: task.workstationName || task.workstationId || 'N/A',
                            duration: task.duration,
                            startTime: formatTime(new Date(task.startTime || task.scheduledStartTime)),
                            endTime: formatTime(new Date(task.endTime || task.scheduledEndTime)),
                            priority: task.priority,
                            manuallyAdjusted: task.manuallyAdjusted,
                            adjustedBy: task.adjustedBy,
                            adjustmentReason: task.adjustmentReason
                          });
                        }}
                        onMouseMove={(e) => {
                          if (hoveredTask === task.id) {
                            setTooltipPosition({ x: e.clientX, y: e.clientY });
                          }
                        }}
                        onMouseLeave={() => {
                          setHoveredTask(null);
                          setTooltipContent(null);
                        }}
                        draggable={editable}
                        onDragStart={(e) => handleDragStart(task, e)}
                        onDrag={handleDrag}
                        onDragEnd={handleDragEnd}
                      >
                        <span className="task-label">
                          {task.manuallyAdjusted && <span className="manual-adjustment-badge" title="Manually adjusted">✏️</span>}
                          {task.taskType || task.operationType || task.itemName || 'Task'} 
                          {task.duration && ` (${task.duration}min)`}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Portal-rendered tooltip for proper z-index handling */}
      {tooltipContent && createPortal(
        <div 
          className="gantt-tooltip visible"
          style={{
            position: 'fixed',
            top: tooltipPosition.y + 15,
            left: tooltipPosition.x + 10,
            zIndex: 999999,
            pointerEvents: 'none'
          }}
        >
          <div className="gantt-tooltip-header">
            <span className="gantt-tooltip-icon">📋</span>
            <span className="gantt-tooltip-title">{tooltipContent.taskName}</span>
            <span className={`gantt-tooltip-status ${tooltipContent.statusClass}`}>
              {tooltipContent.status.replace('_', ' ')}
            </span>
          </div>
          <div className="gantt-tooltip-content">
            <span className="gantt-tooltip-label">Workstation</span>
            <span className="gantt-tooltip-value">{tooltipContent.workstation}</span>
            
            <span className="gantt-tooltip-label">Start</span>
            <span className="gantt-tooltip-value">{tooltipContent.startTime}</span>
            
            <span className="gantt-tooltip-label">End</span>
            <span className="gantt-tooltip-value">{tooltipContent.endTime}</span>
            
            <span className="gantt-tooltip-label">Duration</span>
            <span className="gantt-tooltip-value">{tooltipContent.duration} min</span>
            
            {tooltipContent.priority && (
              <>
                <span className="gantt-tooltip-label">Priority</span>
                <span className="gantt-tooltip-value">{tooltipContent.priority}</span>
              </>
            )}
            
            {tooltipContent.manuallyAdjusted && (
              <div className="tooltip-row manual-adjustment-info">
                ✏️ Manually adjusted
                {tooltipContent.adjustedBy && ` by ${tooltipContent.adjustedBy}`}
                {tooltipContent.adjustmentReason && ` - ${tooltipContent.adjustmentReason}`}
              </div>
            )}
          </div>
        </div>,
        document.body
      )}

      {/* Legend */}
      <div className="gantt-legend">
        <span className="legend-title">Status:</span>
        {['PENDING', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map(status => {
          const colors = getStatusColor(status);
          return (
            <div key={status} className="legend-item">
              <div 
                className="legend-color" 
                style={{ 
                  backgroundColor: colors.base,
                  borderLeft: `4px solid ${colors.border}`
                }}
              />
              <span>{status.replace('_', ' ')}</span>
            </div>
          );
        })}
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
