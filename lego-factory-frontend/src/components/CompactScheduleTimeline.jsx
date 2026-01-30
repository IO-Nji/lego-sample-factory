import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import PropTypes from 'prop-types';
import '../styles/GanttChart.css';

/**
 * CompactScheduleTimeline Component
 * 
 * Compact version of GanttChart optimized for dashboard display
 * Features:
 * - Status-based task coloring (matches GanttChart legend)
 * - Visual distinction for overlapping tasks
 * - Fits in activity log area (300px height)
 * - Enhanced legibility with high contrast colors
 * 
 * @param {Array} scheduledTasks - Array of task objects with workstation, duration, status, start/end times
 * @param {Function} onTaskClick - Handler when a task bar is clicked
 * @param {string} title - Optional title override
 * @param {boolean} showTitle - Whether to show the internal title (default: true, set false when inside Card with title)
 */
function CompactScheduleTimeline({ scheduledTasks = [], onTaskClick, title = "Production Schedule Timeline", showTitle = true }) {
  const [viewStart, setViewStart] = useState(null);
  const [viewEnd, setViewEnd] = useState(null);
  const [hoveredTask, setHoveredTask] = useState(null);
  const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });
  const [tooltipContent, setTooltipContent] = useState(null);

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
        
        // Add padding (30 minutes before and after)
        minStart.setMinutes(minStart.getMinutes() - 30);
        maxEnd.setMinutes(maxEnd.getMinutes() + 30);
        
        setViewStart(minStart);
        setViewEnd(maxEnd);
      }
    } else {
      // Default to current day (8 AM - 6 PM)
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

  // Generate time markers (compact - fewer markers)
  const generateTimeMarkers = () => {
    if (!viewStart || !viewEnd) return [];
    
    const markers = [];
    const totalHours = (viewEnd - viewStart) / (1000 * 60 * 60);
    // Use larger intervals for compact view
    const interval = totalHours > 24 ? 6 : totalHours > 12 ? 3 : 2;

    let current = new Date(viewStart);
    while (current <= viewEnd) {
      markers.push(new Date(current));
      current.setHours(current.getHours() + interval);
    }

    return markers;
  };

  // Get status-based color (matches GanttChart legend)
  const getStatusColor = (status) => {
    const statusColors = {
      PENDING: {
        base: '#f59e0b', // Orange
        border: '#d97706',
        text: '#ffffff'
      },
      SCHEDULED: {
        base: '#3b82f6', // Blue
        border: '#2563eb',
        text: '#ffffff'
      },
      IN_PROGRESS: {
        base: '#10b981', // Green
        border: '#059669',
        text: '#ffffff'
      },
      COMPLETED: {
        base: '#6b7280', // Gray
        border: '#4b5563',
        text: '#ffffff'
      },
      CANCELLED: {
        base: '#ef4444', // Red
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

  if (!viewStart || !viewEnd) {
    return (
      <div style={{ padding: '1rem', textAlign: 'center', color: '#6b7280', fontSize: '0.875rem' }}>
        Initializing timeline...
      </div>
    );
  }

  return (
    <div className="compact-schedule-timeline" style={{ 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      fontSize: '0.75rem',
      overflow: 'hidden'
    }}>
      {/* Header - Compact (only show if showTitle is true) */}
      {showTitle && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingBottom: '0.5rem',
          borderBottom: '2px solid #e5e7eb',
          flexShrink: 0
        }}>
          <h3 style={{ 
            margin: 0, 
            fontSize: '0.875rem', 
            fontWeight: 'bold', 
            color: '#1f2937' 
          }}>
            📅 {title}
          </h3>
          <span style={{ 
            fontSize: '0.7rem', 
            color: '#6b7280',
            whiteSpace: 'nowrap'
          }}>
            {viewStart.toLocaleDateString()} - {viewEnd.toLocaleDateString()}
          </span>
        </div>
      )}

      {/* Status Legend - Matches GanttChart */}
      <div style={{
        display: 'flex',
        gap: '0.5rem',
        padding: '0.25rem 0',
        fontSize: '0.65rem',
        flexShrink: 0,
        flexWrap: 'wrap'
      }}>
        {['PENDING', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map(status => {
          const colors = getStatusColor(status);
          return (
            <div key={status} style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <div style={{ 
                width: '12px', 
                height: '12px', 
                backgroundColor: colors.base,
                border: `1px solid ${colors.border}`,
                borderRadius: '2px'
              }} />
              <span style={{ color: '#374151' }}>{status.replace('_', ' ')}</span>
            </div>
          );
        })}
      </div>

      {/* Timeline Container - Scrollable */}
      <div style={{ 
        flex: 1,
        position: 'relative',
        overflowY: 'auto',
        overflowX: 'visible',
        paddingTop: '0.5rem'
      }}>
        {/* Time markers - Compact */}
        <div style={{
          position: 'relative',
          height: '20px',
          borderBottom: '1px solid #d1d5db',
          marginBottom: '0.25rem',
          flexShrink: 0
        }}>
          {timeMarkers.map((marker, idx) => {
            const position = ((marker - viewStart) / (viewEnd - viewStart)) * 100;
            return (
              <div 
                key={idx}
                style={{
                  position: 'absolute',
                  left: `${position}%`,
                  transform: 'translateX(-50%)',
                  top: 0
                }}
              >
                <div style={{ 
                  borderLeft: '1px solid #d1d5db', 
                  height: '4px',
                  marginBottom: '2px'
                }} />
                <span style={{ 
                  fontSize: '0.65rem', 
                  color: '#6b7280',
                  whiteSpace: 'nowrap'
                }}>
                  {formatTime(marker)}
                </span>
              </div>
            );
          })}
        </div>

        {/* Workstation rows - Compact */}
        {Object.keys(groupedTasks).length === 0 ? (
          <div style={{ 
            padding: '2rem', 
            textAlign: 'center', 
            color: '#9ca3af',
            fontSize: '0.8rem'
          }}>
            <p style={{ margin: 0, fontWeight: 'bold' }}>No scheduled tasks</p>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.7rem' }}>Tasks will appear when scheduled</p>
          </div>
        ) : (
          <div>
            {Object.entries(groupedTasks).map(([workstation, tasks]) => (
              <div 
                key={workstation}
                style={{
                  display: 'flex',
                  marginBottom: '0.25rem',
                  minHeight: '32px',
                  borderBottom: '1px solid #f3f4f6'
                }}
              >
                {/* Workstation label - Compact */}
                <div style={{
                  width: '120px',
                  flexShrink: 0,
                  paddingRight: '0.5rem',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  fontSize: '0.7rem'
                }}>
                  <span style={{ fontWeight: '600', color: '#1f2937', lineHeight: '1.2' }}>
                    {workstation}
                  </span>
                  <span style={{ fontSize: '0.65rem', color: '#6b7280' }}>
                    {tasks.length} task{tasks.length !== 1 ? 's' : ''}
                  </span>
                </div>

                {/* Timeline track */}
                <div style={{
                  flex: 1,
                  position: 'relative',
                  backgroundColor: '#fafafa',
                  borderRadius: '3px',
                  minHeight: '32px'
                }}>
                  {tasks.map((task, idx) => {
                    const position = calculateTaskPosition(task);
                    const colors = getStatusColor(task.status || 'PENDING');
                    const isHovered = hoveredTask === (task.id || idx);
                    const taskName = task.taskType || task.operationType || task.itemName || 'Task';
                    const status = task.status || 'PENDING';
                    const statusClass = status.toLowerCase().replace('_', '-');
                    
                    return (
                      <div
                        key={task.id || idx}
                        style={{
                          position: 'absolute',
                          left: `${position.left}%`,
                          width: `${position.width}%`,
                          top: `${(idx % 2) * 50}%`, // Alternate rows for overlapping tasks
                          height: '45%',
                          backgroundColor: colors.base,
                          border: `2px solid ${colors.border}`,
                          borderRadius: '3px',
                          color: colors.text,
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                          transform: isHovered ? 'translateY(-2px)' : 'translateY(0)',
                          boxShadow: isHovered ? '0 4px 6px rgba(0,0,0,0.2)' : '0 1px 3px rgba(0,0,0,0.1)',
                          zIndex: isHovered ? 10 : idx,
                          opacity: colors.opacity || 1,
                          fontSize: '0.65rem',
                          padding: '0.25rem',
                          display: 'flex',
                          alignItems: 'center',
                          overflow: 'visible',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}
                        onClick={() => onTaskClick && onTaskClick(task)}
                        onMouseEnter={(e) => {
                          setHoveredTask(task.id || idx);
                          setTooltipPosition({ x: e.clientX, y: e.clientY });
                          setTooltipContent({
                            taskName,
                            status,
                            statusClass,
                            workstation: task.workstationName || task.workstationId || 'N/A',
                            duration: task.duration,
                            startTime: formatTime(new Date(task.startTime || task.scheduledStartTime)),
                            endTime: formatTime(new Date(task.endTime || task.scheduledEndTime)),
                            priority: task.priority,
                            manuallyAdjusted: task.manuallyAdjusted
                          });
                        }}
                        onMouseMove={(e) => {
                          if (isHovered) {
                            setTooltipPosition({ x: e.clientX, y: e.clientY });
                          }
                        }}
                        onMouseLeave={() => {
                          setHoveredTask(null);
                          setTooltipContent(null);
                        }}
                      >
                        {/* Status indicator dot */}
                        <span style={{
                          width: '6px',
                          height: '6px',
                          borderRadius: '50%',
                          backgroundColor: colors.text,
                          marginRight: '0.25rem',
                          flexShrink: 0
                        }} />
                        <span style={{ 
                          fontWeight: '600',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis'
                        }}>
                          {task.manuallyAdjusted && '✏️ '}
                          {task.taskType || task.operationType || task.itemName || 'Task'}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
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
            
            <span className="gantt-tooltip-label">Duration</span>
            <span className="gantt-tooltip-value">{tooltipContent.duration} min</span>
            
            <span className="gantt-tooltip-label">Start</span>
            <span className="gantt-tooltip-value">{tooltipContent.startTime}</span>
            
            <span className="gantt-tooltip-label">End</span>
            <span className="gantt-tooltip-value">{tooltipContent.endTime}</span>
            
            {tooltipContent.priority && (
              <>
                <span className="gantt-tooltip-label">Priority</span>
                <span className="gantt-tooltip-value">{tooltipContent.priority}</span>
              </>
            )}
            
            {tooltipContent.manuallyAdjusted && (
              <div className="tooltip-row manual-adjustment-info">
                ✏️ Manually adjusted
              </div>
            )}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}

CompactScheduleTimeline.propTypes = {
  scheduledTasks: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    taskId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    workstationName: PropTypes.string,
    workstationId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    taskType: PropTypes.string,
    operationType: PropTypes.string,
    itemName: PropTypes.string,
    status: PropTypes.oneOf(['PENDING', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']),
    priority: PropTypes.oneOf(['LOW', 'NORMAL', 'HIGH', 'URGENT']),
    duration: PropTypes.number,
    startTime: PropTypes.string,
    scheduledStartTime: PropTypes.string,
    endTime: PropTypes.string,
    scheduledEndTime: PropTypes.string,
    manuallyAdjusted: PropTypes.bool
  })),
  onTaskClick: PropTypes.func,
  title: PropTypes.string,
  showTitle: PropTypes.bool
};

export default CompactScheduleTimeline;
