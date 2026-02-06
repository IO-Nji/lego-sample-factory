import { useState, useEffect, useCallback } from 'react';
import api from '../api/api';

/**
 * Custom hook for managing inventory display with masterdata lookup
 * 
 * @param {string} itemType - Type of items: 'PRODUCT', 'MODULE', or 'PART'
 * @param {number} workstationId - ID of the workstation (optional, for auto-fetching inventory)
 * @returns {object} - Inventory management state and functions
 * 
 * @example
 * // In PartsSupplyWarehouseDashboard
 * const { 
 *   inventory, 
 *   masterdata, 
 *   loading, 
 *   error,
 *   fetchInventory, 
 *   getItemName 
 * } = useInventoryDisplay('PART', 9);
 * 
 * <InventoryTable
 *   inventory={inventory}
 *   getItemName={getItemName}
 * />
 */
export const useInventoryDisplay = (itemType, workstationId = null) => {
  const [inventory, setInventory] = useState([]);
  const [masterdata, setMasterdata] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Determine API endpoint based on item type
  const getMasterdataEndpoint = useCallback(() => {
    switch (itemType) {
      case 'PRODUCT':
        return '/masterdata/products';
      case 'MODULE':
        return '/masterdata/modules';
      case 'PART':
        return '/masterdata/parts';
      default:
        throw new Error(`Unknown item type: ${itemType}`);
    }
  }, [itemType]);

  // Fetch masterdata (products, modules, or parts)
  const fetchMasterdata = useCallback(async () => {
    try {
      const endpoint = getMasterdataEndpoint();
      console.log(`[useInventoryDisplay] Fetching ${itemType} from ${endpoint}...`);
      const response = await api.get(endpoint);
      console.log(`[useInventoryDisplay] Received ${response.data?.length || 0} ${itemType} items:`, response.data);
      
      if (!response.data || response.data.length === 0) {
        console.warn(`[useInventoryDisplay] WARNING: No ${itemType} data received from ${endpoint}`);
        console.warn('[useInventoryDisplay] Check if masterdata-service database is seeded properly');
      }
      
      setMasterdata(response.data || []);
      return response.data || [];
    } catch (err) {
      console.error(`[useInventoryDisplay] ERROR fetching ${itemType} masterdata from ${getMasterdataEndpoint()}:`, err);
      console.error('[useInventoryDisplay] Full error details:', {
        message: err.message,
        response: err.response?.data,
        status: err.response?.status,
        endpoint: getMasterdataEndpoint()
      });
      const errorMsg = err.response?.data?.message || err.message || `Failed to load ${itemType.toLowerCase()} data`;
      setError(errorMsg);
      setMasterdata([]); // Set empty array on error
      return [];
    }
  }, [itemType, getMasterdataEndpoint]);

  // Fetch inventory for a specific workstation
  const fetchInventory = useCallback(async (wsId = workstationId) => {
    if (!wsId) {
      return [];
    }

    setLoading(true);
    setError(null);
    try {
      const response = await api.get(`/stock/workstation/${wsId}`);
      const inventoryData = response.data || [];
      
      // Filter by itemType if needed
      const filteredInventory = inventoryData.filter(item => item.itemType === itemType);
      
      setInventory(filteredInventory);
      return filteredInventory;
    } catch (err) {
      console.error(`[useInventoryDisplay] Error fetching inventory for WS-${wsId}:`, err);
      setError(err.response?.data?.message || 'Failed to load inventory');
      return [];
    } finally {
      setLoading(false);
    }
  }, [workstationId, itemType]);

  // Get item name from masterdata by itemId
  // Supports both: getItemName(item) where item has itemId, or getItemName(itemId, itemType)
  const getItemName = useCallback((itemOrId, itemTypeArg) => {
    // Handle different calling conventions
    let itemId;
    if (typeof itemOrId === 'object' && itemOrId !== null) {
      // Called with item object: getItemName({ itemId: 1, itemType: 'PRODUCT' })
      itemId = itemOrId.itemId;
    } else {
      // Called with itemId directly: getItemName(1, 'PRODUCT')
      itemId = itemOrId;
    }

    if (!itemId) {
      console.warn(`[useInventoryDisplay.getItemName] No itemId provided, itemOrId:`, itemOrId);
      return 'Unknown Item';
    }

    // Debug logging
    if (masterdata.length === 0) {
      console.warn(`[useInventoryDisplay.getItemName] Masterdata not loaded yet for ${itemType}, returning placeholder for itemId ${itemId}`);
    }

    const masterdataItem = masterdata.find(m => m.id === itemId);
    
    if (masterdataItem?.name) {
      return masterdataItem.name;
    }

    // Fallback display names
    switch (itemType) {
      case 'PRODUCT':
        return `Product #${itemId}`;
      case 'MODULE':
        return `Module #${itemId}`;
      case 'PART':
        return `Part #${itemId}`;
      default:
        return `Item #${itemId}`;
    }
  }, [masterdata, itemType]);

  // Get item name with description (more verbose version)
  // Supports both: getItemNameWithDescription(item) or getItemNameWithDescription(itemId, itemType)
  const getItemNameWithDescription = useCallback((itemOrId, itemTypeArg) => {
    // Handle different calling conventions
    let itemId;
    if (typeof itemOrId === 'object' && itemOrId !== null) {
      itemId = itemOrId.itemId;
    } else {
      itemId = itemOrId;
    }

    if (!itemId) {
      return 'Unknown Item';
    }

    const masterdataItem = masterdata.find(m => m.id === itemId);
    
    if (masterdataItem) {
      const name = masterdataItem.name || `Item #${itemId}`;
      const description = masterdataItem.description;
      return description ? `${name} (${description})` : name;
    }

    return getItemName(itemId, itemTypeArg);
  }, [masterdata, getItemName]);

  // Get stock quantity for a specific item
  // Returns the quantity in stock, or null if inventory not loaded yet
  const getStockLevel = useCallback((itemId, itemTypeParam = itemType) => {
    if (!itemId) return null;
    // If inventory hasn't been loaded yet, return null to indicate "unknown"
    if (!inventory || inventory.length === 0) return null;
    const invItem = inventory.find(inv => inv.itemId === itemId && inv.itemType === itemTypeParam);
    // If item found, return its quantity; if not found but inventory is loaded, item has 0 stock
    return invItem ? invItem.quantity : 0;
  }, [inventory, itemType]);

  // Auto-fetch masterdata on mount
  useEffect(() => {
    fetchMasterdata();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run once on mount

  // Auto-fetch inventory if workstationId provided
  // This will trigger when workstationId becomes available (e.g., after session loads)
  useEffect(() => {
    if (workstationId) {
      fetchInventory(workstationId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workstationId]); // Only re-fetch if workstationId changes

  return {
    inventory,
    masterdata,
    loading,
    error,
    fetchInventory,
    fetchMasterdata,
    getItemName,
    getItemNameWithDescription,
    getStockLevel,
    setInventory, // Allow manual updates
    setMasterdata, // Allow manual updates
  };
};

export default useInventoryDisplay;
