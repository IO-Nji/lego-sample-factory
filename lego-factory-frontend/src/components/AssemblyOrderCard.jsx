import React from "react";
import OrderCard from "./OrderCard";

export default function AssemblyOrderCard({ order, children }) {
  return <OrderCard order={order} type="assembly">{children}</OrderCard>;
}
