import React from 'react';
import '../styles/Chart.css';

/**
 * StatCard - Reusable component for displaying key metrics
 * @param {string} title - Card title
 * @param {string|number} value - Main metric value
 * @param {string} icon - Icon/emoji for the card
 * @param {string} color - Accent color (primary, success, warning, danger, info)
 * @param {string} subtitle - Optional subtitle text
 * @param {number} trend - Optional trend percentage (positive/negative)
 */
function StatCard({ title, value, icon, color = 'primary', subtitle, trend }) {
  const colorClass = `stat-card-${color}`;
  
  return (
    <div className={`stat-card ${colorClass}`}>
      <div className="stat-icon">{icon}</div>
      <div className="stat-content">
        <div className="stat-value">{value}</div>
        <div className="stat-title">{title}</div>
        {subtitle && <div className="stat-subtitle">{subtitle}</div>}
        {trend !== undefined && (
          <div className={`stat-trend ${trend >= 0 ? 'trend-up' : 'trend-down'}`}>
            {trend >= 0 ? '↑' : '↓'} {Math.abs(trend)}%
          </div>
        )}
      </div>
    </div>
  );
}

export default StatCard;
