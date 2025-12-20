import React from "react";
import OrderCard from "./OrderCard";

export default function WarehouseOrderCard({ order, children }) {
  return <OrderCard order={order} type="warehouse">{children}</OrderCard>;
}
