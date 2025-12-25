/**
 * Dashboard Helper Utilities
 * Shared functions used across multiple dashboard components
 */

/**
 * Product name mapping with acronyms
 * Maps itemId/productId to display name
 * Must match IDs from masterdata-service DataInitializer
 */
export const PRODUCT_NAMES = {
  1: { name: "Technic Truck Yellow" },
  2: { name: "Technic Truck Red" },
  3: { name: "Creator House" },
  4: { name: "Friends Cafe" },
};

/**
 * Generate acronym from product name
 * Takes first letter of each word (e.g., "Drill A" -> "DA", "Drill Variant B" -> "DVB")
 */
export const generateAcronym = (productName) => {
  if (!productName) return "?";
  return productName
    .split(" ")
    .map((word) => word.charAt(0).toUpperCase())
    .join("");
};

/**
 * Get product display name with dynamically generated acronym
 */
export const getProductDisplayName = (itemId, itemType) => {
  if (itemType && itemType !== "PRODUCT") {
    return generateAcronym(itemType);
  }
  const product = PRODUCT_NAMES[itemId];
  if (product) {
    return generateAcronym(product.name);
  }
  return `#${itemId}`;
};

/**
 * Get inventory status color based on quantity
 * Green: > 20, Yellow: 6-20, Red: 1-5, Dark Red: 0
 */
export const getInventoryStatusColor = (quantity) => {
  if (quantity > 20) return "#10b981"; // Green - In stock
  if (quantity > 5) return "#f59e0b"; // Yellow - Medium stock
  if (quantity > 0) return "#ef4444"; // Red - Low stock
  return "#991b1b"; // Dark red - Out of stock
};

/**
 * Standard status color mapping
 */
export const STATUS_COLORS = {
  PENDING: '#f59e0b',
  PROCESSING: '#3b82f6',
  COMPLETED: '#10b981',
  CANCELLED: '#6b7280',
  CONFIRMED: '#6366f1',
  ASSIGNED: '#8b5cf6',
  IN_PROGRESS: '#3b82f6',
  FULFILLED: '#10b981',
};

/**
 * Format date/time for display
 */
export const formatDateTime = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString();
};

/**
 * Format date only
 */
export const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleDateString();
};
