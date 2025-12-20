import React from "react";
import OrderCard from "./OrderCard";

export default function SupplyOrderCard({ order, children }) {
  return <OrderCard order={order} type="supply">{children}</OrderCard>;
}
