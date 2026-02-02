package io.life.user_service.service;

import io.life.user_service.entity.User;
import io.life.user_service.entity.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * WorkstationAccessService
 * 
 * Centralized service for workstation access validation and routing.
 * Leverages the enhanced UserRole enum to determine which workstations
 * a user can access based on their role and assigned workstation.
 * 
 * WORKSTATION MAPPING:
 * - WS-1: Injection Molding (Manufacturing)
 * - WS-2: Parts Pre-Production (Manufacturing)
 * - WS-3: Part Finishing (Manufacturing)
 * - WS-4: Gear Assembly (Assembly)
 * - WS-5: Motor Assembly (Assembly)
 * - WS-6: Final Assembly (Assembly)
 * - WS-7: Plant Warehouse (Customer Fulfillment)
 * - WS-8: Modules Supermarket (Internal Warehouse)
 * - WS-9: Parts Supply Warehouse (Raw Materials)
 * 
 * ACCESS RULES:
 * - ADMIN: All workstations (1-9)
 * - PLANT_WAREHOUSE: WS-7 only
 * - MODULES_SUPERMARKET: WS-8 only
 * - PRODUCTION_PLANNING: All workstations (planning view)
 * - PRODUCTION_CONTROL: WS-1, WS-2, WS-3, WS-9
 * - ASSEMBLY_CONTROL: WS-4, WS-5, WS-6, WS-8
 * - PARTS_SUPPLY: WS-9 only
 * - MANUFACTURING: WS-1, WS-2, WS-3 (specific WS assigned per user)
 * - VIEWER: All workstations (read-only)
 */
@Service
@Slf4j
public class WorkstationAccessService {

    /**
     * Check if a user can access a specific workstation.
     * 
     * @param user The user to check
     * @param workstationId The workstation ID to validate access for
     * @return true if the user can access the workstation
     */
    public boolean canAccessWorkstation(User user, int workstationId) {
        if (user == null || user.getRole() == null) {
            log.warn("Access check failed: user or role is null");
            return false;
        }

        UserRole role = user.getRole();
        
        // First check role-based access
        if (!role.canAccessWorkstation(workstationId)) {
            log.debug("User {} with role {} cannot access workstation {}", 
                    user.getUsername(), role, workstationId);
            return false;
        }

        // For MANUFACTURING role, also check if this is their assigned workstation
        if (role == UserRole.MANUFACTURING) {
            Long userWorkstationId = user.getWorkstationId();
            if (userWorkstationId != null && userWorkstationId.intValue() != workstationId) {
                log.debug("Manufacturing user {} assigned to WS-{} cannot access WS-{}", 
                        user.getUsername(), userWorkstationId, workstationId);
                return false;
            }
        }

        return true;
    }

    /**
     * Get all workstations a user can access.
     * 
     * @param user The user
     * @return Set of accessible workstation IDs
     */
    public Set<Integer> getAccessibleWorkstations(User user) {
        if (user == null || user.getRole() == null) {
            return Set.of();
        }

        UserRole role = user.getRole();
        
        // For MANUFACTURING role with assigned workstation, return only that workstation
        if (role == UserRole.MANUFACTURING && user.getWorkstationId() != null) {
            return Set.of(user.getWorkstationId().intValue());
        }

        return role.getAccessibleWorkstations();
    }

    /**
     * Get the primary/default workstation for a user.
     * Used for dashboard routing when user has no explicit workstationId.
     * 
     * @param user The user
     * @return Primary workstation ID (0 = system-wide/no specific workstation)
     */
    public int getPrimaryWorkstation(User user) {
        if (user == null) {
            return 0;
        }

        // If user has an assigned workstation, use that
        Long userWorkstationId = user.getWorkstationId();
        if (userWorkstationId != null && userWorkstationId > 0) {
            return userWorkstationId.intValue();
        }

        // Fall back to role's primary workstation
        if (user.getRole() != null) {
            return user.getRole().getPrimaryWorkstation();
        }

        return 0;
    }

    /**
     * Determine if a user has system-wide access (can see all workstations).
     * 
     * @param user The user
     * @return true if user can access all 9 workstations
     */
    public boolean hasSystemWideAccess(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().hasSystemWideAccess();
    }

    /**
     * Validate that a user can be assigned to a specific workstation.
     * Only certain roles can be assigned to workstations.
     * 
     * @param role The user's role
     * @param workstationId The workstation to assign
     * @throws IllegalArgumentException if the assignment is invalid
     */
    public void validateWorkstationAssignment(UserRole role, Integer workstationId) {
        if (workstationId == null || workstationId == 0) {
            return; // No assignment is always valid
        }

        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null for workstation assignment");
        }

        if (!role.canAccessWorkstation(workstationId)) {
            throw new IllegalArgumentException(
                    String.format("Role %s cannot be assigned to workstation %d. Allowed workstations: %s",
                            role, workstationId, role.getAccessibleWorkstations()));
        }
    }

    /**
     * Get a user-friendly description of what workstations a role can access.
     * Useful for UI display and error messages.
     * 
     * @param role The role
     * @return Human-readable workstation access description
     */
    public String getAccessDescription(UserRole role) {
        if (role == null) {
            return "No access";
        }

        if (role.hasSystemWideAccess()) {
            return "All workstations (WS-1 to WS-9)";
        }

        Set<Integer> workstations = role.getAccessibleWorkstations();
        if (workstations.isEmpty()) {
            return "No workstations";
        }

        StringBuilder sb = new StringBuilder();
        for (Integer ws : workstations) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("WS-").append(ws);
        }
        return sb.toString();
    }
}
