import { useState, useEffect, useRef } from 'react';
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
 * - Current time indicator
 * - Mini navigation controls
 * 
 * @param {Array} scheduledTasks - Array of task objects with workstation, duration, status, start/end times
 * @param {Function} onTaskClick - Handler when a task bar is clicked
 * @param {string} title - Optional title override
 * @param {boolean} showTitle - Whether to show the internal title (default: true, set false when inside Card with title)
 * @param {boolean} showCurrentTime - Show current time indicator (default: true)
 * @param {boolean} showMiniControls - Show mini zoom/pan controls (default: true)
 */
function CompactScheduleTimeline({ 
  scheduledTasks = [], 
  onTaskClick, 
  title = "Production Schedule Timeline", 
  showTitle = true,
  showCurrentTime = true,
  showMiniControls = true
}) {
  const [viewStart, setViewStart] = useState(null);
  const [viewEnd, setViewEnd] = useState(null);
  const [hoveredTask, setHoveredTask] = useState(null);
  const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });
  const [tooltipContent, setTooltipContent] = useState(null);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [zoomLevel, setZoomLevel] = useState(1);
  const containerRef = useRef(null);

  // Current time update
  useEffect(() => {
    if (!showCurrentTime) return;
    const interval = setInterval(() => setCurrentTime(new Date()), 60000);
    return () => clearInterval(interval);
  }, [showCurrentTime]);

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

  // Zoom handlers
  const handleZoomIn = () => {
    setZoomLevel(prev => Math.min(prev * 1.5, 3));
    if (viewStart && viewEnd) {
      const center = new Date((viewStart.getTime() + viewEnd.getTime()) / 2);
      const duration = viewEnd - viewStart;
      const newDuration = duration / 1.5;
      setViewStart(new Date(center.getTime() - newDuration / 2));
      setViewEnd(new Date(center.getTime() + newDuration / 2));
    }
  };

  const handleZoomOut = () => {
    setZoomLevel(prev => Math.max(prev / 1.5, 0.5));
    if (viewStart && viewEnd) {
      const center = new Date((viewStart.getTime() + viewEnd.getTime()) / 2);
      const duration = viewEnd - viewStart;
      const newDuration = duration * 1.5;
      setViewStart(new Date(center.getTime() - newDuration / 2));
      setViewEnd(new Date(center.getTime() + newDuration / 2));
    }
  };

  const scrollToNow = () => {
    if (!viewStart || !viewEnd) return;
    const duration = viewEnd - viewStart;
    const now = new Date();
    setViewStart(new Date(now.getTime() - duration / 2));
    setViewEnd(new Date(now.getTime() + duration / 2));
  };

  const getCurrentTimePosition = () => {
    if (!viewStart || !viewEnd || !showCurrentTime) return null;
    const totalDuration = viewEnd - viewStart;
    const offset = currentTime - viewStart;
    const percentage = (offset / totalDuration) * 100;
    if (percentage < 0 || percentage > 100) return null;
    return percentage;
  };

  // Group tasks by workstation - ALWAYS show all 5 manufacturing/assembly workstations
  const groupTasksByWorkstation = () => {
    // Initialize with 5 manufacturing/assembly workstations only
    const allWorkstations = [
      'WS-1: Injection Molding',
      'WS-2: Parts Pre-Production',
      'WS-3: Part Finishing',
      'WS-4: Gear Assembly',
      'WS-5: Motor Assembly'
    ];
    
    // Map backend workstation names/IDs to display format
    const normalizeWorkstationName = (task) => {
      const wsId = task.workstationId;
      const wsName = task.workstationName;
      
      // Handle workstation ID (e.g., 1, "WS-1", "1")
      if (wsId) {
        const id = typeof wsId === 'string' ? parseInt(wsId.replace('WS-', '')) : wsId;
        const mapping = {
          1: 'WS-1: Injection Molding',
          2: 'WS-2: Parts Pre-Production',
          3: 'WS-3: Part Finishing',
          4: 'WS-4: Gear Assembly',
          5: 'WS-5: Motor Assembly'
        };
        if (mapping[id]) return mapping[id];
      }
      
      // Handle backend workstation name variations
      if (wsName) {
        if (wsName.includes('Injection') || wsName.includes('Molding')) return 'WS-1: Injection Molding';
        if (wsName.includes('Pre-Production') || wsName.includes('PreProduction')) return 'WS-2: Parts Pre-Production';
        if (wsName.includes('Finishing')) return 'WS-3: Part Finishing';
        if (wsName.includes('Gear')) return 'WS-4: Gear Assembly';
        if (wsName.includes('Motor')) return 'WS-5: Motor Assembly';
      }
      
      return null; // Skip non-manufacturing/assembly workstations
    };
    
    const grouped = {};
    
    // Initialize all workstations with empty arrays
    allWorkstations.forEach(ws => {
      grouped[ws] = [];
    });
    
    // Add tasks to appropriate workstations
    scheduledTasks.forEach(task => {
      const normalizedWs = normalizeWorkstationName(task);
      if (normalizedWs && grouped[normalizedWs]) {
        grouped[normalizedWs].push(task);
      }
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
  const currentTimePosition = getCurrentTimePosition();

  if (!viewStart || !viewEnd) {
    return (
      <div style={{ padding: '1rem', textAlign: 'center', color: '#6b7280', fontSize: '0.875rem' }}>
        Initializing timeline...
      </div>
    );
  }

  return (
    <div className="compact-schedule-timeline" ref={containerRef} style={{ 
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
          flexShrink: 0,
          gap: '0.5rem'
        }}>
          <h3 style={{ 
            margin: 0, 
            fontSize: '0.875rem', 
            fontWeight: 'bold', 
            color: '#1f2937' 
          }}>
            📅 {title}
          </h3>
          
          {/* Mini Controls */}
          {showMiniControls && (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.25rem',
              background: '#f3f4f6',
              borderRadius: '0.25rem',
              padding: '0.125rem'
            }}>
              <button
                onClick={handleZoomOut}
                disabled={zoomLevel <= 0.5}
                style={{
                  border: 'none',
                  background: 'transparent',
                  cursor: zoomLevel <= 0.5 ? 'not-allowed' : 'pointer',
                  opacity: zoomLevel <= 0.5 ? 0.4 : 1,
                  fontSize: '0.75rem',
                  padding: '0.125rem 0.25rem',
                  borderRadius: '0.125rem'
                }}
                title="Zoom Out"
              >−</button>
              <button
                onClick={scrollToNow}
                style={{
                  border: 'none',
                  background: 'transparent',
                  cursor: 'pointer',
                  fontSize: '0.65rem',
                  padding: '0.125rem 0.25rem',
                  borderRadius: '0.125rem'
                }}
                title="Jump to Now"
              >📍</button>
              <button
                onClick={handleZoomIn}
                disabled={zoomLevel >= 3}
                style={{
                  border: 'none',
                  background: 'transparent',
                  cursor: zoomLevel >= 3 ? 'not-allowed' : 'pointer',
                  opacity: zoomLevel >= 3 ? 0.4 : 1,
                  fontSize: '0.75rem',
                  padding: '0.125rem 0.25rem',
                  borderRadius: '0.125rem'
                }}
                title="Zoom In"
              >+</button>
            </div>
          )}
          
          <span style={{ 
            fontSize: '0.7rem', 
            color: '#6b7280',
            whiteSpace: 'nowrap'
          }}>
            {viewStart.toLocaleDateString()} - {viewEnd.toLocaleDateString()}
          </span>
        </div>
      )}

      {/* Mini Controls Bar - Shows when title is hidden (controls go in parent Card header) */}
      {!showTitle && showMiniControls && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingBottom: '0.25rem',
          marginBottom: '0.125rem',
          borderBottom: '1px solid #e5e7eb',
          flexShrink: 0
        }}>
          <span style={{ 
            fontSize: '0.6rem', 
            color: '#6b7280',
            whiteSpace: 'nowrap'
          }}>
            📅 {viewStart.toLocaleDateString()} - {viewEnd.toLocaleDateString()}
          </span>
          
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.25rem',
            background: '#f3f4f6',
            borderRadius: '0.25rem',
            padding: '0.125rem'
          }}>
            <button
              onClick={handleZoomOut}
              disabled={zoomLevel <= 0.5}
              style={{
                border: 'none',
                background: 'transparent',
                cursor: zoomLevel <= 0.5 ? 'not-allowed' : 'pointer',
                opacity: zoomLevel <= 0.5 ? 0.4 : 1,
                fontSize: '0.75rem',
                padding: '0.125rem 0.25rem',
                borderRadius: '0.125rem'
              }}
              title="Zoom Out"
            >−</button>
            <button
              onClick={scrollToNow}
              style={{
                border: 'none',
                background: 'transparent',
                cursor: 'pointer',
                fontSize: '0.65rem',
                padding: '0.125rem 0.25rem',
                borderRadius: '0.125rem'
              }}
              title="Jump to Now"
            >📍</button>
            <button
              onClick={handleZoomIn}
              disabled={zoomLevel >= 3}
              style={{
                border: 'none',
                background: 'transparent',
                cursor: zoomLevel >= 3 ? 'not-allowed' : 'pointer',
                opacity: zoomLevel >= 3 ? 0.4 : 1,
                fontSize: '0.75rem',
                padding: '0.125rem 0.25rem',
                borderRadius: '0.125rem'
              }}
              title="Zoom In"
            >+</button>
          </div>
        </div>
      )}

      {/* Status Legend - Matches GanttChart */}
      <div style={{
        display: 'flex',
        gap: '0.375rem',
        padding: '0.125rem 0',
        fontSize: '0.6rem',
        flexShrink: 0,
        flexWrap: 'wrap'
      }}>
        {['PENDING', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map(status => {
          const colors = getStatusColor(status);
          return (
            <div key={status} style={{ display: 'flex', alignItems: 'center', gap: '0.125rem' }}>
              <div style={{ 
                width: '10px', 
                height: '10px', 
                backgroundColor: colors.base,
                border: `1px solid ${colors.border}`,
                borderRadius: '2px'
              }} />
              <span style={{ color: '#374151' }}>{status.replace('_', ' ')}</span>
            </div>
          );
        })}
      </div>

      {/* Timeline Container - Compact height for 5 Manufacturing/Assembly Workstations */}
      <div style={{ 
        minHeight: '210px', // Compact: 5 workstations × 36px + time markers 16px + padding 14px
        maxHeight: '210px',
        position: 'relative',
        overflowY: 'hidden', // No vertical scrolling needed
        overflowX: 'visible',
        paddingTop: '0.25rem'
      }}>
        {/* Current Time Indicator */}
        {currentTimePosition !== null && (
          <div style={{
            position: 'absolute',
            left: `${currentTimePosition}%`,
            top: 0,
            bottom: 0,
            width: '2px',
            background: '#ef4444',
            zIndex: 50,
            pointerEvents: 'none'
          }}>
            <div style={{
              position: 'absolute',
              top: '-1px',
              left: '-3px',
              width: '8px',
              height: '8px',
              background: '#ef4444',
              borderRadius: '50%',
              boxShadow: '0 0 0 2px rgba(239, 68, 68, 0.2)'
            }} />
            <div style={{
              position: 'absolute',
              top: '-14px',
              left: '50%',
              transform: 'translateX(-50%)',
              background: '#ef4444',
              color: 'white',
              fontSize: '0.5rem',
              fontWeight: '600',
              padding: '1px 3px',
              borderRadius: '2px',
              whiteSpace: 'nowrap'
            }}>
              {formatTime(currentTime)}
            </div>
          </div>
        )}
        
        {/* Time markers - Compact */}
        <div style={{
          position: 'relative',
          height: '16px',
          borderBottom: '1px solid #d1d5db',
          marginBottom: '0.125rem',
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
                  height: '3px',
                  marginBottom: '1px'
                }} />
                <span style={{ 
                  fontSize: '0.6rem', 
                  color: '#6b7280',
                  whiteSpace: 'nowrap'
                }}>
                  {formatTime(marker)}
                </span>
              </div>
            );
          })}
        </div>

        {/* Workstation rows - Always show all 5 workstations with fixed heights */}
        <div>
          {Object.entries(groupedTasks).map(([workstation, tasks]) => (
            <div 
              key={workstation}
              style={{
                display: 'flex',
                marginBottom: '0.125rem',
                minHeight: '36px', // Compact fixed height per workstation row
                maxHeight: '36px',
                borderBottom: '1px solid #f3f4f6'
              }}
            >
                {/* Workstation label - Compact */}
                <div style={{
                  width: '100px',
                  flexShrink: 0,
                  paddingRight: '0.25rem',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  fontSize: '0.65rem'
                }}>
                  <span style={{ fontWeight: '600', color: '#1f2937', lineHeight: '1.1' }}>
                    {workstation}
                  </span>
                  <span style={{ fontSize: '0.6rem', color: '#6b7280' }}>
                    {tasks.length} task{tasks.length !== 1 ? 's' : ''}
                  </span>
                </div>

                {/* Timeline track */}
                <div style={{
                  flex: 1,
                  position: 'relative',
                  backgroundColor: '#fafafa',
                  borderRadius: '3px',
                  minHeight: '28px',
                  maxHeight: '28px'
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
                          border: `1px solid ${colors.border}`,
                          borderRadius: '2px',
                          color: colors.text,
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                          transform: isHovered ? 'translateY(-1px)' : 'translateY(0)',
                          boxShadow: isHovered ? '0 2px 4px rgba(0,0,0,0.15)' : '0 1px 2px rgba(0,0,0,0.1)',
                          zIndex: isHovered ? 10 : idx,
                          opacity: colors.opacity || 1,
                          fontSize: '0.55rem',
                          padding: '0.125rem',
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
                          width: '5px',
                          height: '5px',
                          borderRadius: '50%',
                          backgroundColor: colors.text,
                          marginRight: '0.125rem',
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
  showTitle: PropTypes.bool,
  showCurrentTime: PropTypes.bool,
  showMiniControls: PropTypes.bool
};

export default CompactScheduleTimeline;
