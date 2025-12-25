import React from 'react';
import '../styles/Chart.css';

/**
 * BarChart - Reusable horizontal/vertical bar chart component
 * @param {Array} data - Array of {label, value, color}
 * @param {string} title - Chart title
 * @param {string} orientation - 'horizontal' or 'vertical' (default horizontal)
 * @param {number} maxValue - Maximum value for scale (auto-calculated if not provided)
 */
function BarChart({ data, title, orientation = 'horizontal', maxValue }) {
  const max = maxValue || Math.max(...data.map(d => d.value), 1);
  
  if (data.length === 0) {
    return (
      <div className="chart-container">
        {title && <h3 className="chart-title">{title}</h3>}
        <div className="chart-empty">No data available</div>
      </div>
    );
  }

  return (
    <div className="chart-container">
      {title && <h3 className="chart-title">{title}</h3>}
      <div className={`bar-chart bar-chart-${orientation}`}>
        {data.map((item, index) => {
          const percentage = (item.value / max) * 100;
          return (
            <div key={index} className="bar-item">
              <div className="bar-label">{item.label}</div>
              <div className="bar-wrapper">
                <div 
                  className="bar-fill" 
                  style={{ 
                    [orientation === 'horizontal' ? 'width' : 'height']: `${percentage}%`,
                    backgroundColor: item.color || 'var(--color-primary)'
                  }}
                >
                  <span className="bar-value">{item.value}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default BarChart;
