import React from "react";
import OrderCard from "./OrderCard";

export default function CustomerOrderCard({ order, children }) {
  return <OrderCard order={order} type="customer">{children}</OrderCard>;
}
