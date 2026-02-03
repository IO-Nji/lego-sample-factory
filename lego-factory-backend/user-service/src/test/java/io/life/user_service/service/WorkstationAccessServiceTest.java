package io.life.user_service.service;

import io.life.user_service.entity.User;
import io.life.user_service.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WorkstationAccessService.
 * 
 * Tests workstation access validation based on user roles and assignments.
 * No mocking required as this service has no dependencies.
 */
@DisplayName("WorkstationAccessService Tests")
class WorkstationAccessServiceTest {

    private WorkstationAccessService workstationAccessService;

    @BeforeEach
    void setUp() {
        workstationAccessService = new WorkstationAccessService();
    }

    /**
     * Creates a User with null role for testing edge cases.
     * Uses reflection since User constructor requires non-null role.
     */
    private User createUserWithNullRole(String username) {
        User user = new User(username, "hash", UserRole.ADMIN, null);
        ReflectionTestUtils.setField(user, "role", null);
        return user;
    }

    // ==================== canAccessWorkstation Tests ====================
    
    @Nested
    @DisplayName("canAccessWorkstation()")
    class CanAccessWorkstationTests {

        @Test
        @DisplayName("Should return false when user is null")
        void shouldReturnFalseWhenUserIsNull() {
            boolean result = workstationAccessService.canAccessWorkstation(null, 7);
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user role is null")
        void shouldReturnFalseWhenRoleIsNull() {
            User user = createUserWithNullRole("test");
            
            boolean result = workstationAccessService.canAccessWorkstation(user, 7);
            
            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9})
        @DisplayName("ADMIN should access all workstations")
        void adminShouldAccessAllWorkstations(int workstationId) {
            User admin = new User("admin", "hash", UserRole.ADMIN, null);
            
            boolean result = workstationAccessService.canAccessWorkstation(admin, workstationId);
            
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9})
        @DisplayName("PRODUCTION_PLANNING should access all workstations")
        void productionPlanningShouldAccessAllWorkstations(int workstationId) {
            User planner = new User("planner", "hash", UserRole.PRODUCTION_PLANNING, null);
            
            boolean result = workstationAccessService.canAccessWorkstation(planner, workstationId);
            
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9})
        @DisplayName("VIEWER should access all workstations")
        void viewerShouldAccessAllWorkstations(int workstationId) {
            User viewer = new User("viewer", "hash", UserRole.VIEWER, null);
            
            boolean result = workstationAccessService.canAccessWorkstation(viewer, workstationId);
            
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("PLANT_WAREHOUSE should only access WS-7")
        void plantWarehouseShouldOnlyAccessWS7() {
            User warehouseOp = new User("warehouse", "hash", UserRole.PLANT_WAREHOUSE, null);
            
            // Should access WS-7
            assertThat(workstationAccessService.canAccessWorkstation(warehouseOp, 7)).isTrue();
            
            // Should NOT access other workstations
            assertThat(workstationAccessService.canAccessWorkstation(warehouseOp, 1)).isFalse();
            assertThat(workstationAccessService.canAccessWorkstation(warehouseOp, 6)).isFalse();
            assertThat(workstationAccessService.canAccessWorkstation(warehouseOp, 8)).isFalse();
        }

        @Test
        @DisplayName("MODULES_SUPERMARKET should only access WS-8")
        void modulesSupermarketShouldOnlyAccessWS8() {
            User modulesOp = new User("modules", "hash", UserRole.MODULES_SUPERMARKET, null);
            
            assertThat(workstationAccessService.canAccessWorkstation(modulesOp, 8)).isTrue();
            assertThat(workstationAccessService.canAccessWorkstation(modulesOp, 7)).isFalse();
            assertThat(workstationAccessService.canAccessWorkstation(modulesOp, 9)).isFalse();
        }

        @Test
        @DisplayName("PARTS_SUPPLY should only access WS-9")
        void partsSupplyShouldOnlyAccessWS9() {
            User partsOp = new User("parts", "hash", UserRole.PARTS_SUPPLY, null);
            
            assertThat(workstationAccessService.canAccessWorkstation(partsOp, 9)).isTrue();
            assertThat(workstationAccessService.canAccessWorkstation(partsOp, 7)).isFalse();
            assertThat(workstationAccessService.canAccessWorkstation(partsOp, 8)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 9})
        @DisplayName("PRODUCTION_CONTROL should access WS-1, 2, 3, 9")
        void productionControlShouldAccessManufacturingAndSupply(int workstationId) {
            User prodControl = new User("prodcontrol", "hash", UserRole.PRODUCTION_CONTROL, null);
            
            assertThat(workstationAccessService.canAccessWorkstation(prodControl, workstationId)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {4, 5, 6, 7, 8})
        @DisplayName("PRODUCTION_CONTROL should NOT access WS-4, 5, 6, 7, 8")
        void productionControlShouldNotAccessAssemblyOrWarehouses(int workstationId) {
            User prodControl = new User("prodcontrol", "hash", UserRole.PRODUCTION_CONTROL, null);
            
            assertThat(workstationAccessService.canAccessWorkstation(prodControl, workstationId)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(ints = {4, 5, 6, 8})
        @DisplayName("ASSEMBLY_CONTROL should access WS-4, 5, 6, 8")
        void assemblyControlShouldAccessAssemblyStations(int workstationId) {
            User assemblyControl = new User("assembly", "hash", UserRole.ASSEMBLY_CONTROL, null);
            
            assertThat(workstationAccessService.canAccessWorkstation(assemblyControl, workstationId)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 7, 9})
        @DisplayName("ASSEMBLY_CONTROL should NOT access WS-1, 2, 3, 7, 9")
        void assemblyControlShouldNotAccessManufacturing(int workstationId) {
            User assemblyControl = new User("assembly", "hash", UserRole.ASSEMBLY_CONTROL, null);
            
            assertThat(workstationAccessService.canAccessWorkstation(assemblyControl, workstationId)).isFalse();
        }

        @Test
        @DisplayName("MANUFACTURING user should access only assigned workstation")
        void manufacturingUserShouldAccessOnlyAssignedWorkstation() {
            User mfgUser = new User("mfg", "hash", UserRole.MANUFACTURING, 1L);
            
            // Should access assigned WS-1
            assertThat(workstationAccessService.canAccessWorkstation(mfgUser, 1)).isTrue();
            
            // Should NOT access other manufacturing stations
            assertThat(workstationAccessService.canAccessWorkstation(mfgUser, 2)).isFalse();
            assertThat(workstationAccessService.canAccessWorkstation(mfgUser, 3)).isFalse();
        }

        @Test
        @DisplayName("MANUFACTURING user without assignment should access all manufacturing workstations")
        void manufacturingUserWithoutAssignmentShouldAccessAllManufacturing() {
            User mfgUser = new User("mfg", "hash", UserRole.MANUFACTURING, null);
            
            // Without assignment, role-based access (WS-1,2,3)
            assertThat(workstationAccessService.canAccessWorkstation(mfgUser, 1)).isTrue();
            assertThat(workstationAccessService.canAccessWorkstation(mfgUser, 2)).isTrue();
            assertThat(workstationAccessService.canAccessWorkstation(mfgUser, 3)).isTrue();
            
            // But not other stations
            assertThat(workstationAccessService.canAccessWorkstation(mfgUser, 4)).isFalse();
        }
    }

    // ==================== getAccessibleWorkstations Tests ====================
    
    @Nested
    @DisplayName("getAccessibleWorkstations()")
    class GetAccessibleWorkstationsTests {

        @Test
        @DisplayName("Should return empty set when user is null")
        void shouldReturnEmptySetWhenUserIsNull() {
            Set<Integer> result = workstationAccessService.getAccessibleWorkstations(null);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set when role is null")
        void shouldReturnEmptySetWhenRoleIsNull() {
            User user = createUserWithNullRole("test");
            
            Set<Integer> result = workstationAccessService.getAccessibleWorkstations(user);
            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ADMIN should have access to all 9 workstations")
        void adminShouldHaveAllWorkstations() {
            User admin = new User("admin", "hash", UserRole.ADMIN, null);
            
            Set<Integer> result = workstationAccessService.getAccessibleWorkstations(admin);
            
            assertThat(result).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9);
        }

        @Test
        @DisplayName("PLANT_WAREHOUSE should have only WS-7")
        void plantWarehouseShouldHaveOnlyWS7() {
            User warehouseOp = new User("warehouse", "hash", UserRole.PLANT_WAREHOUSE, null);
            
            Set<Integer> result = workstationAccessService.getAccessibleWorkstations(warehouseOp);
            
            assertThat(result).containsExactly(7);
        }

        @Test
        @DisplayName("MANUFACTURING user with assignment should have only assigned workstation")
        void manufacturingWithAssignmentShouldHaveOnlyAssigned() {
            User mfgUser = new User("mfg", "hash", UserRole.MANUFACTURING, 2L);
            
            Set<Integer> result = workstationAccessService.getAccessibleWorkstations(mfgUser);
            
            assertThat(result).containsExactly(2);
        }

        @Test
        @DisplayName("MANUFACTURING user without assignment should have all manufacturing workstations")
        void manufacturingWithoutAssignmentShouldHaveAll() {
            User mfgUser = new User("mfg", "hash", UserRole.MANUFACTURING, null);
            
            Set<Integer> result = workstationAccessService.getAccessibleWorkstations(mfgUser);
            
            assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
        }
    }

    // ==================== getPrimaryWorkstation Tests ====================
    
    @Nested
    @DisplayName("getPrimaryWorkstation()")
    class GetPrimaryWorkstationTests {

        @Test
        @DisplayName("Should return 0 when user is null")
        void shouldReturnZeroWhenUserIsNull() {
            int result = workstationAccessService.getPrimaryWorkstation(null);
            
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should return user's assigned workstation when set")
        void shouldReturnAssignedWorkstation() {
            User user = new User("user", "hash", UserRole.MANUFACTURING, 3L);
            
            int result = workstationAccessService.getPrimaryWorkstation(user);
            
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return role's primary workstation when user has no assignment")
        void shouldReturnRolePrimaryWhenNoAssignment() {
            User user = new User("warehouse", "hash", UserRole.PLANT_WAREHOUSE, null);
            
            int result = workstationAccessService.getPrimaryWorkstation(user);
            
            assertThat(result).isEqualTo(7);
        }

        @Test
        @DisplayName("MODULES_SUPERMARKET should have primary WS-8")
        void modulesSupermarketShouldHavePrimaryWS8() {
            User user = new User("modules", "hash", UserRole.MODULES_SUPERMARKET, null);
            
            int result = workstationAccessService.getPrimaryWorkstation(user);
            
            assertThat(result).isEqualTo(8);
        }

        @Test
        @DisplayName("PARTS_SUPPLY should have primary WS-9")
        void partsSupplyShouldHavePrimaryWS9() {
            User user = new User("parts", "hash", UserRole.PARTS_SUPPLY, null);
            
            int result = workstationAccessService.getPrimaryWorkstation(user);
            
            assertThat(result).isEqualTo(9);
        }

        @Test
        @DisplayName("ADMIN should have primary 0 (system-wide)")
        void adminShouldHavePrimaryZero() {
            User user = new User("admin", "hash", UserRole.ADMIN, null);
            
            int result = workstationAccessService.getPrimaryWorkstation(user);
            
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should return 0 when role is null")
        void shouldReturnZeroWhenRoleIsNull() {
            User user = createUserWithNullRole("test");
            
            int result = workstationAccessService.getPrimaryWorkstation(user);
            
            assertThat(result).isZero();
        }
    }

    // ==================== hasSystemWideAccess Tests ====================
    
    @Nested
    @DisplayName("hasSystemWideAccess()")
    class HasSystemWideAccessTests {

        @Test
        @DisplayName("Should return false when user is null")
        void shouldReturnFalseWhenUserIsNull() {
            boolean result = workstationAccessService.hasSystemWideAccess(null);
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when role is null")
        void shouldReturnFalseWhenRoleIsNullForSystemWide() {
            User user = createUserWithNullRole("test");
            
            boolean result = workstationAccessService.hasSystemWideAccess(user);
            
            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = UserRole.class, names = {"ADMIN", "PRODUCTION_PLANNING", "VIEWER"})
        @DisplayName("Roles with all workstations should have system-wide access")
        void rolesWithAllWorkstationsShouldHaveSystemWideAccess(UserRole role) {
            User user = new User("test", "hash", role, null);
            
            boolean result = workstationAccessService.hasSystemWideAccess(user);
            
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = UserRole.class, names = {"PLANT_WAREHOUSE", "MODULES_SUPERMARKET", "PARTS_SUPPLY", 
                                                      "PRODUCTION_CONTROL", "ASSEMBLY_CONTROL", "MANUFACTURING"})
        @DisplayName("Roles with limited workstations should NOT have system-wide access")
        void rolesWithLimitedWorkstationsShouldNotHaveSystemWideAccess(UserRole role) {
            User user = new User("test", "hash", role, null);
            
            boolean result = workstationAccessService.hasSystemWideAccess(user);
            
            assertThat(result).isFalse();
        }
    }

    // ==================== validateWorkstationAssignment Tests ====================
    
    @Nested
    @DisplayName("validateWorkstationAssignment()")
    class ValidateWorkstationAssignmentTests {

        @Test
        @DisplayName("Should accept null workstation assignment")
        void shouldAcceptNullWorkstationAssignment() {
            assertThatCode(() -> 
                workstationAccessService.validateWorkstationAssignment(UserRole.MANUFACTURING, null)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept zero workstation assignment")
        void shouldAcceptZeroWorkstationAssignment() {
            assertThatCode(() -> 
                workstationAccessService.validateWorkstationAssignment(UserRole.MANUFACTURING, 0)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when role is null with workstation assignment")
        void shouldThrowWhenRoleIsNullWithWorkstationAssignment() {
            assertThatThrownBy(() -> 
                workstationAccessService.validateWorkstationAssignment(null, 1)
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role cannot be null");
        }

        @Test
        @DisplayName("Should accept valid MANUFACTURING assignment to WS-1")
        void shouldAcceptValidManufacturingAssignment() {
            assertThatCode(() -> 
                workstationAccessService.validateWorkstationAssignment(UserRole.MANUFACTURING, 1)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject MANUFACTURING assignment to non-manufacturing station")
        void shouldRejectInvalidManufacturingAssignment() {
            assertThatThrownBy(() -> 
                workstationAccessService.validateWorkstationAssignment(UserRole.MANUFACTURING, 7)
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MANUFACTURING")
                .hasMessageContaining("cannot be assigned to workstation 7");
        }

        @Test
        @DisplayName("Should accept PLANT_WAREHOUSE assignment to WS-7")
        void shouldAcceptValidPlantWarehouseAssignment() {
            assertThatCode(() -> 
                workstationAccessService.validateWorkstationAssignment(UserRole.PLANT_WAREHOUSE, 7)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject PLANT_WAREHOUSE assignment to WS-8")
        void shouldRejectInvalidPlantWarehouseAssignment() {
            assertThatThrownBy(() -> 
                workstationAccessService.validateWorkstationAssignment(UserRole.PLANT_WAREHOUSE, 8)
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PLANT_WAREHOUSE");
        }

        @Test
        @DisplayName("ADMIN can be assigned to any workstation")
        void adminCanBeAssignedToAnyWorkstation() {
            for (int ws = 1; ws <= 9; ws++) {
                final int workstation = ws;
                assertThatCode(() -> 
                    workstationAccessService.validateWorkstationAssignment(UserRole.ADMIN, workstation)
                ).doesNotThrowAnyException();
            }
        }
    }

    // ==================== getAccessDescription Tests ====================
    
    @Nested
    @DisplayName("getAccessDescription()")
    class GetAccessDescriptionTests {

        @Test
        @DisplayName("Should return 'No access' when role is null")
        void shouldReturnNoAccessWhenRoleIsNull() {
            String result = workstationAccessService.getAccessDescription(null);
            
            assertThat(result).isEqualTo("No access");
        }

        @Test
        @DisplayName("ADMIN should have 'All workstations' description")
        void adminShouldHaveAllWorkstationsDescription() {
            String result = workstationAccessService.getAccessDescription(UserRole.ADMIN);
            
            assertThat(result).isEqualTo("All workstations (WS-1 to WS-9)");
        }

        @Test
        @DisplayName("PLANT_WAREHOUSE should have 'WS-7' description")
        void plantWarehouseShouldHaveWS7Description() {
            String result = workstationAccessService.getAccessDescription(UserRole.PLANT_WAREHOUSE);
            
            assertThat(result).isEqualTo("WS-7");
        }

        @Test
        @DisplayName("PRODUCTION_CONTROL should list all accessible workstations")
        void productionControlShouldListAccessibleWorkstations() {
            String result = workstationAccessService.getAccessDescription(UserRole.PRODUCTION_CONTROL);
            
            assertThat(result).contains("WS-1", "WS-2", "WS-3", "WS-9");
        }
    }
}
