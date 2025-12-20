import React from "react";
import OrderCard from "./OrderCard";

export default function ProductionOrderCard({ order, children }) {
  return <OrderCard order={order} type="production">{children}</OrderCard>;
}
