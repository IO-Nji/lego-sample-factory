import React from 'react';
import '../styles/Chart.css';

/**
 * StatCard - Reusable component for displaying key metrics
 * @param {string} title - Card title
 * @param {string|number} value - Main metric value
 * @param {string} icon - Icon/emoji for the card
 * @param {string} color - Accent color (primary, success, warning, danger, info)
 * @param {number} threshold - Optional threshold for dynamic coloring
 * @param {string} thresholdType - Type of threshold check ('high' or 'low')
 * @param {number} trend - Optional trend percentage (positive/negative)
 */
function StatCard({ title, value, icon, color = 'primary', threshold, thresholdType = 'high', trend }) {
  const colorClass = `stat-card-${color}`;
  
  // Dynamic value coloring based on threshold
  let valueColorClass = '';
  if (threshold !== undefined && value !== undefined && value !== null) {
    // For 'low' threshold: trigger danger when value is AT OR BELOW threshold
    // For 'high' threshold: trigger warning when value is AT OR ABOVE threshold
    if (thresholdType === 'low' && value <= threshold) {
      valueColorClass = 'stat-value-danger'; // Low stock alert (red pulse)
    } else if (thresholdType === 'high' && value >= threshold) {
      valueColorClass = 'stat-value-warning'; // High pending orders alert (orange)
    }
  }
  
  return (
    <div className={`stat-card ${colorClass}`}>
      <div className={`stat-value ${valueColorClass}`}>{value}</div>
      <div className="stat-title">{title}</div>
      <div className="stat-icon">{icon}</div>
      {trend !== undefined && (
        <div className={`stat-trend ${trend >= 0 ? 'trend-up' : 'trend-down'}`}>
          {trend >= 0 ? '↑' : '↓'} {Math.abs(trend)}%
        </div>
      )}
    </div>
  );
}

export default StatCard;
