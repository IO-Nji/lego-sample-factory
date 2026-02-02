import React from 'react';
import '../styles/Chart.css';
import '../styles/DashboardLayout.css'; // Import for .component-title class

/**
 * PieChart - Reusable pie chart component using CSS conic-gradient
 * @param {Array} data - Array of {label, value, color}
 * @param {string} title - Chart title (optional, omit when using inside Card with title prop)
 * @param {number} size - Chart diameter in pixels (default 200)
 * @param {string} layout - 'vertical' (default), 'horizontal' (side-by-side), or 'compact' (small chart with inline legend)
 * @param {boolean} noContainer - If true, skips the .chart-container wrapper (use when inside Card)
 */
function PieChart({ data, title, size = 200, layout = 'vertical', noContainer = false }) {
  const total = data.reduce((sum, item) => sum + item.value, 0);
  
  if (total === 0) {
    if (noContainer) {
      return (
        <>
          {title && <h3 className="component-title">{title}</h3>}
          <div className="chart-empty">No data available</div>
        </>
      );
    }
    return (
      <div className="chart-container">
        {title && <h3 className="component-title">{title}</h3>}
        <div className="chart-empty">No data available</div>
      </div>
    );
  }

  // Calculate percentages and conic-gradient stops
  let currentAngle = 0;
  const gradientStops = data.map(item => {
    const percentage = (item.value / total) * 100;
    const startAngle = currentAngle;
    currentAngle += percentage;
    return `${item.color} ${startAngle}% ${currentAngle}%`;
  }).join(', ');

  const isHorizontal = layout === 'horizontal';
  const isCompact = layout === 'compact';

  // Compact layout: smaller chart with legend items in a wrapped row
  if (isCompact) {
    const compactSize = size || 80;
    const content = (
      <>
        {title && <h3 className="component-title">{title}</h3>}
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          gap: '0.75rem',
          width: 'fit-content',
          minHeight: 0
        }}>
          <div 
            className="pie-chart" 
            style={{ 
              width: compactSize, 
              height: compactSize,
              background: `conic-gradient(${gradientStops})`,
              flexShrink: 0
            }}
          >
            <div className="pie-center"></div>
          </div>
          <div style={{ 
            display: 'flex', 
            flexDirection: 'column',
            gap: '0.1rem',
            minWidth: '90px',
            maxWidth: '110px',
            overflow: 'hidden'
          }}>
            {data.map((item, index) => (
              <div key={index} style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: '0.5rem',
                fontSize: '0.68rem',
                whiteSpace: 'nowrap',
                overflow: 'hidden'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', overflow: 'hidden' }}>
                  <span style={{ 
                    width: '0.45rem', 
                    height: '0.45rem', 
                    borderRadius: '2px', 
                    backgroundColor: item.color,
                    flexShrink: 0
                  }}></span>
                  <span style={{ 
                    fontWeight: 500,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  }}>{item.label}</span>
                </div>
                <span style={{ 
                  color: 'var(--color-text-secondary)',
                  fontWeight: 600,
                  flexShrink: 0
                }}>{item.value}</span>
              </div>
            ))}
          </div>
        </div>
      </>
    );

    if (noContainer) {
      return content;
    }
    return <div className="chart-container">{content}</div>;
  }

  const content = (
    <>
      {title && <h3 className="component-title">{title}</h3>}
      <div className="pie-chart-wrapper" style={isHorizontal ? { display: 'flex', alignItems: 'center', gap: '1.5rem' } : {}}>
        <div 
          className="pie-chart" 
          style={{ 
            width: size, 
            height: size,
            background: `conic-gradient(${gradientStops})`,
            flexShrink: 0
          }}
        >
          <div className="pie-center"></div>
        </div>
        <div className="chart-legend" style={isHorizontal ? { flex: 1, minWidth: 0 } : {}}>
          {data.map((item, index) => (
            <div key={index} className="legend-item">
              <span className="legend-color" style={{ backgroundColor: item.color }}></span>
              <span className="legend-label">{item.label}</span>
              <span className="legend-value">{item.value} ({((item.value / total) * 100).toFixed(0)}%)</span>
            </div>
          ))}
        </div>
      </div>
    </>
  );

  if (noContainer) {
    return content;
  }

  return (
    <div className="chart-container">
      {content}
    </div>
  );
}

export default PieChart;
