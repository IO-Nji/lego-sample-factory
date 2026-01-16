import React from "react";
import "../styles/OrderCard.css";

export default function OrderCard({ order, type = "customer", children }) {
  // Border color by type
  const borderColors = {
    customer: "#3b82f6", // blue
    production: "#8b5cf6", // purple - distinct from other order types
    warehouse: "#f59e0b", // yellow
    assembly: "#10b981", // green
    supply: "#6366f1", // indigo
    default: "#d1d5db", // gray
  };
  const borderColor = borderColors[type] || borderColors.default;

  return (
    <div className="order-card" style={{ borderLeft: `4px solid ${borderColor}` }}>
      <div className="order-card-header">
        <span className="order-card-id">Order #{order.orderNumber || order.id}</span>
        <span className={`order-card-status status-badge status-${(order.status || "unknown").toLowerCase()}`}>{order.status || "UNKNOWN"}</span>
      </div>
      <div className="order-card-body">
        <div className="order-card-row">
          <span>Items:</span>
          <span>{order.quantity || order.items?.length || order.orderItems?.length || 0}</span>
        </div>
        <div className="order-card-row">
          <span>Priority:</span>
          <span>{order.priority || "NORMAL"}</span>
        </div>
        <div className="order-card-row">
          <span>Created:</span>
          <span>{order.createdAt ? new Date(order.createdAt).toLocaleDateString() : (order.orderDate ? new Date(order.orderDate).toLocaleDateString() : "-")}</span>
        </div>
        {children}
      </div>
    </div>
  );
}
