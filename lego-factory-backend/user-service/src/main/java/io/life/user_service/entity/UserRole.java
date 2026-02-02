package io.life.user_service.entity;

import java.util.Collections;
import java.util.Set;

/**
 * Enumerates supported application roles with workstation access rules.
 * 
 * Each role defines:
 * - accessibleWorkstations: Which workstations this role can view/interact with
 * - primaryWorkstation: The default workstation for dashboard routing (0 = system/all)
 * 
 * Workstation IDs:
 * - WS-1: Injection Molding (Manufacturing)
 * - WS-2: Parts Pre-Production (Manufacturing)
 * - WS-3: Part Finishing (Manufacturing)
 * - WS-4: Gear Assembly (Assembly)
 * - WS-5: Motor Assembly (Assembly)
 * - WS-6: Final Assembly (Assembly)
 * - WS-7: Plant Warehouse (Customer Fulfillment)
 * - WS-8: Modules Supermarket (Internal Warehouse)
 * - WS-9: Parts Supply Warehouse (Raw Materials)
 * - 0: System-wide access (Admin, Viewer, Planning)
 */
public enum UserRole {
    ADMIN(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0),
    PLANT_WAREHOUSE(Set.of(7), 7),
    MODULES_SUPERMARKET(Set.of(8), 8),
    PRODUCTION_PLANNING(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0),
    PRODUCTION_CONTROL(Set.of(1, 2, 3, 9), 0),
    ASSEMBLY_CONTROL(Set.of(4, 5, 6, 8), 0),
    PARTS_SUPPLY(Set.of(9), 9),
    MANUFACTURING(Set.of(1, 2, 3), 0),  // Specific WS assigned per user
    VIEWER(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0);

    private final Set<Integer> accessibleWorkstations;
    private final int primaryWorkstation;

    UserRole(Set<Integer> accessibleWorkstations, int primaryWorkstation) {
        this.accessibleWorkstations = Collections.unmodifiableSet(accessibleWorkstations);
        this.primaryWorkstation = primaryWorkstation;
    }

    /**
     * Check if this role can access a specific workstation.
     * 
     * @param workstationId The workstation to check
     * @return true if the role has access to this workstation
     */
    public boolean canAccessWorkstation(int workstationId) {
        return accessibleWorkstations.contains(workstationId);
    }

    /**
     * Get all workstations this role can access.
     * 
     * @return Immutable set of accessible workstation IDs
     */
    public Set<Integer> getAccessibleWorkstations() {
        return accessibleWorkstations;
    }

    /**
     * Get the primary/default workstation for this role.
     * Used for dashboard routing when user has no explicit workstationId.
     * 
     * @return Primary workstation ID (0 = system-wide/no specific workstation)
     */
    public int getPrimaryWorkstation() {
        return primaryWorkstation;
    }

    /**
     * Check if this role has system-wide access (can see all workstations).
     * 
     * @return true if role can access all 9 workstations
     */
    public boolean hasSystemWideAccess() {
        return accessibleWorkstations.size() == 9;
    }
}
