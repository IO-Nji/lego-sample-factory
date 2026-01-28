package io.life.user_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.life.user_service.entity.UserRole;
import io.life.user_service.service.UserService;

/**
 * Comprehensive user initialization for LEGO Factory system.
 * Creates all required users for development and testing.
 * 
 * This replaces SQL-based user initialization for better control
 * and consistency with password encoding.
 */
@Component
public class UserInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInitializer.class);
    private static final String DEFAULT_PASSWORD = "password";

    private final UserService userService;

    public UserInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("Starting comprehensive user initialization...");
        
        // Create admin user
        createUserIfNotExists("lego_admin", DEFAULT_PASSWORD, UserRole.ADMIN, null);
        
        // Create plant warehouse users
        createUserIfNotExists("warehouse_operator", DEFAULT_PASSWORD, UserRole.PLANT_WAREHOUSE, 7L);
        
        // Create modules supermarket user
        createUserIfNotExists("modules_supermarket", DEFAULT_PASSWORD, UserRole.MODULES_SUPERMARKET, 8L);
        
        // Create planning users
        createUserIfNotExists("production_planning", DEFAULT_PASSWORD, UserRole.PRODUCTION_PLANNING, null);
        
        // Create control users (coordination roles, no specific workstation)
        createUserIfNotExists("production_control", DEFAULT_PASSWORD, UserRole.PRODUCTION_CONTROL, null);
        
        // Create assembly control users (coordination role, no specific workstation)
        createUserIfNotExists("assembly_control", DEFAULT_PASSWORD, UserRole.ASSEMBLY_CONTROL, null);
        createUserIfNotExists("gear_assembly", DEFAULT_PASSWORD, UserRole.ASSEMBLY_CONTROL, 4L);
        createUserIfNotExists("motor_assembly", DEFAULT_PASSWORD, UserRole.ASSEMBLY_CONTROL, 5L);
        createUserIfNotExists("final_assembly", DEFAULT_PASSWORD, UserRole.ASSEMBLY_CONTROL, 6L);
        
        // Create manufacturing users
        createUserIfNotExists("injection_molding", DEFAULT_PASSWORD, UserRole.MANUFACTURING, 1L);
        createUserIfNotExists("parts_preproduction", DEFAULT_PASSWORD, UserRole.MANUFACTURING, 2L);
        createUserIfNotExists("part_finishing", DEFAULT_PASSWORD, UserRole.MANUFACTURING, 3L);
        
        // Create parts supply user
        createUserIfNotExists("parts_supply", DEFAULT_PASSWORD, UserRole.PARTS_SUPPLY, 9L);
        
        // Create viewer user
        createUserIfNotExists("viewer_user", DEFAULT_PASSWORD, UserRole.VIEWER, null);
        
        LOGGER.info("User initialization completed successfully.");
    }

    private void createUserIfNotExists(String username, String password, UserRole role, Long workstationId) {
        if (userService.findByUsername(username).isEmpty()) {
            LOGGER.debug("Creating user: {} with role: {} at workstation: {}", username, role, workstationId);
            userService.registerUser(username, password, role, workstationId);
        } else {
            LOGGER.debug("User {} already exists, skipping creation.", username);
        }
    }
}