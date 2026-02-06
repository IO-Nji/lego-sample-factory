/**
 * Order Components - Unified exports
 */

// Unified Order Card (new)
export { default as UnifiedOrderCard, ORDER_TYPES } from './UnifiedOrderCard';
export * from './orderCardConfig';

// New Order Grid and Card Variants
export { default as OrdersGrid, VIEW_MODES, DEFAULT_FILTER_OPTIONS, DEFAULT_SORT_OPTIONS } from './OrdersGrid';
export { GridCard, CompactCard, ListRow } from './OrderCardVariants';

// Legacy cards (for backward compatibility during migration)
export { default as BaseOrderCard } from './BaseOrderCard';
export { default as CustomerOrderCard } from './CustomerOrderCard';
export { default as WarehouseOrderCard } from './WarehouseOrderCard';
export { default as FinalAssemblyOrderCard } from './FinalAssemblyOrderCard';
export { default as WorkstationOrderCard } from './WorkstationOrderCard';
export { default as SupplyOrderCard } from './SupplyOrderCard';
export { default as ProductionOrderCard } from './ProductionOrderCard';
export { default as ProductionControlOrderCard } from './ProductionControlOrderCard';
export { default as AssemblyControlOrderCard } from './AssemblyControlOrderCard';
