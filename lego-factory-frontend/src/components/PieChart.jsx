import React from 'react';
import '../styles/Chart.css';
import '../styles/DashboardLayout.css'; // Import for .component-title class

/**
 * PieChart - Reusable pie chart component using CSS conic-gradient
 * @param {Array} data - Array of {label, value, color}
 * @param {string} title - Chart title
 * @param {number} size - Chart diameter in pixels (default 200)
 * @param {string} layout - 'vertical' (default) or 'horizontal' (chart and legend side-by-side)
 */
function PieChart({ data, title, size = 200, layout = 'vertical' }) {
  const total = data.reduce((sum, item) => sum + item.value, 0);
  
  if (total === 0) {
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

  return (
    <div className="chart-container">
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
    </div>
  );
}

export default PieChart;
